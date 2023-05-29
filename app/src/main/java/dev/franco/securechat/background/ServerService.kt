package dev.franco.securechat.background

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.franco.securechat.background.ClientService.Companion.MESSAGE_NODE
import dev.franco.securechat.background.ClientService.Companion.SIGNATURE_NODE
import dev.franco.securechat.data.database.entity.LocalMessage
import dev.franco.securechat.data.source.local.DeviceConnectionRepository
import dev.franco.securechat.data.source.local.MessagesRepository
import dev.franco.securechat.di.IoDispatcher
import dev.franco.securechat.di.ServerDeviceRepository
import dev.franco.securechat.domain.DATE
import dev.franco.securechat.domain.FROM
import dev.franco.securechat.domain.MESSAGE
import dev.franco.securechat.domain.SELF
import dev.franco.security.CLIENT_PUBLIC_KEY
import dev.franco.security.EncryptionManager
import dev.franco.security.Storage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class ServerService : LifecycleService() {

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var messagesRepository: MessagesRepository

    @ServerDeviceRepository
    @Inject
    lateinit var serverConnectionRepository: DeviceConnectionRepository

    @Volatile
    @Inject
    lateinit var encryptionManager: EncryptionManager

    @Inject
    lateinit var storage: Storage

    private lateinit var serverSocket: ServerSocket

    private var clientSocket: Socket? = null

    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch {
            serverConnectionRepository
                .readDeviceConnectionData()
                .collect { info ->
                    info?.let {
                        startServer(it.second)
                    }
                }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        closeServer()
    }

    private fun startServer(port: Int) {
        lifecycleScope.launch(ioDispatcher) {
            if (::serverSocket.isInitialized && (serverSocket.isBound || !serverSocket.isClosed)) {
                serverSocket.close()
            }
            sendBroadcast(Intent(INTENT_FILTER_COMM_SERVER_CREATED))
            serverSocket = ServerSocket(port)
            sendBroadcast(Intent(INTENT_FILTER_COMM_SERVER_STARTED))
            connectToClient()
        }
    }

    private fun acceptClientConnection(): Socket? {
        return try {
            val clientSocket = serverSocket.accept()
            clientSocket
        } catch (e: IOException) {
            sendBroadcast(Intent(INTENT_FILTER_COMM_SERVER_CLIENT_CONNECTION_ERROR))
            null
        }
    }

    private fun connectToClient() {
        lifecycleScope.launch(ioDispatcher) {
            // while (true) {
            clientSocket = acceptClientConnection()

            if (clientSocket != null) {
                sendBroadcast(Intent(ClientService.INTENT_FILTER_ACCEPTING_CONNECTION))
                sendBroadcast(Intent(ClientService.INTENT_FILTER_CONNECTION_ACCEPTED))
                launch {
                    receiveDataFromClient()
                }
                sendServerPublicKey()
            } else {
                sendBroadcast(Intent(INTENT_FILTER_COMM_SERVER_CLIENT_CONNECTION_ERROR))
            }
            // }
        }
    }

    private suspend fun receiveDataFromClient() {
        val reader = DataInputStream(clientSocket?.getInputStream())
        while (true) {
            try {
                val length = reader.readInt()
                val contentBuffer = ByteArray(length)
                reader.readFully(contentBuffer)
                val clientData = String(contentBuffer, Charsets.UTF_8)
                processClientData(clientData)
                Log.e("ServerService", "Recibiendo datos del cliente: $clientData")
            } catch (e: IOException) {
                sendBroadcast(Intent(INTENT_FILTER_COMM_SERVER_CLIENT_CONNECTION_ERROR))
                e.printStackTrace()
                break
            }
        }
    }

    private suspend fun sendDataToClient(data: String) {
        withContext(ioDispatcher) {
            try {
                val outputStream = DataOutputStream(clientSocket?.getOutputStream())
                val jsonData = data.toByteArray(Charsets.UTF_8)
                outputStream.writeInt(jsonData.size)
                outputStream.write(jsonData)
                outputStream.flush()
                Log.e("ServerService", "Enviando datos al cliente: $data")
            } catch (e: IOException) {
                sendBroadcast(Intent(INTENT_FILTER_COMM_SERVER_CLIENT_CONNECTION_ERROR))
                e.printStackTrace()
            }
        }
    }

    private suspend fun processClientData(data: String) {
        Log.e("ServerConnectionService", "processClientData: $data")
        val jsonData = JSONObject(data)
        val nodeData = JSONObject(jsonData.getString(ClientService.DATA_NODE))
        Log.e("ServerConnectionService", "message node: $nodeData")
        return when {
            nodeData.has(SERVER_RESPONSE_PUBLIC_KEY_RECEIVED) -> {
                processPublicKeyReceivedResponse(
                    nodeData.getBoolean(SERVER_RESPONSE_PUBLIC_KEY_RECEIVED),
                )
            }

            nodeData.has(CLIENT_PUBLIC_KEY) -> {
                saveClientPublicKey(nodeData.getString(CLIENT_PUBLIC_KEY))
            }

            nodeData.has(MESSAGE_NODE) -> {
                saveMessage(nodeData.getString(MESSAGE_NODE), nodeData.getString(SIGNATURE_NODE))
            }

            else -> {
                sendBroadcast(Intent(ClientService.INTENT_FILTER_CONNECTION_ACCEPTED))
                responseConnectionAccepted()
            }
        }
    }

    private suspend fun sendLastUnsentOwnMessage() {
        lifecycleScope.launch(ioDispatcher) {
            messagesRepository
                .readMessages()
                .collect { messages ->
                    messages
                        .filter { message ->
                            message.self && !message.sent
                        }
                        .forEach { message ->
                            val msg = createMessageData(message)
                            sendDataToClient(msg)
                            updateLocalMessage(message)
                        }
                }
        }
    }

    private fun createMessageData(message: LocalMessage): String {
        val messageString = JSONObject().apply {
            put(FROM, message.from)
            put(MESSAGE, message.message)
            put(DATE, message.date)
            // In the other side, this message doesn't belong to receiver, for that
            // reason we sent self as false
            put(SELF, false)
        }
        val encryptedMessage = encryptionManager
            .encryptMessage(messageString.toString(), storage.getString(CLIENT_PUBLIC_KEY)!!)
        val signature = encryptionManager.signMessage(messageString.toString())

        val root = JSONObject().apply {
            put(
                ClientService.DATA_NODE,
                JSONObject().apply {
                    put(MESSAGE_NODE, Base64.encodeToString(encryptedMessage, Base64.NO_WRAP))
                    put(SIGNATURE_NODE, Base64.encodeToString(signature, Base64.NO_WRAP))
                },
            )
        }
        return root.toString()
    }

    private suspend fun updateLocalMessage(lastMessage: LocalMessage) {
        val updatedSentMessage = lastMessage.copy(sent = true)
        messagesRepository.updateMessage(updatedSentMessage)
    }

    private suspend fun saveMessage(messageData: String, signatureData: String) {
        val decoded = Base64.decode(messageData, Base64.NO_WRAP)
        val decrypted = encryptionManager.decryptMessage(decoded)
        val signature = Base64.decode(signatureData, Base64.NO_WRAP)
        val clientPublicKey = storage.getString(CLIENT_PUBLIC_KEY)!!
        val signatureIsValid = encryptionManager
            .verifySignature(decrypted, signature, clientPublicKey)
        Log.e("ClientService", "signature is valid: $signatureIsValid")
        val messageNode = JSONObject(decrypted)
        val from = try {
            messageNode.getString(FROM)
        } catch (e: JSONException) {
            ""
        }
        val message = try {
            messageNode.getString(MESSAGE)
        } catch (e: JSONException) {
            ""
        }
        val date = try {
            messageNode.getLong(DATE)
        } catch (e: JSONException) {
            Date().time
        }
        val self = try {
            messageNode.getBoolean(SELF)
        } catch (e: JSONException) {
            false
        }
        messagesRepository.createMessage(
            LocalMessage(
                uid = UUID.randomUUID().toString(),
                from = from,
                message = message,
                date = date,
                self = self,
                sent = false,
            ),
        )
    }

    private suspend fun saveClientPublicKey(publicKey: String) {
        Log.e("ServerConnectionService", "Saving client public key: $publicKey")
        val data = JSONObject().apply {
            put(
                ClientService.DATA_NODE,
                JSONObject().apply {
                    put(SERVER_RESPONSE_PUBLIC_KEY_RECEIVED, true)
                },
            )
        }
        storage.putString(CLIENT_PUBLIC_KEY, publicKey)
        sendLastUnsentOwnMessage()
        sendDataToClient(data.toString())
    }

    private fun processMessageReceivedTamperedResponse() {
        sendBroadcast(Intent(ClientService.INTENT_FILTER_MESSAGE_TAMPERED))
    }

    private fun closeServer() {
        lifecycleScope.launch(ioDispatcher) {
            try {
                serverSocket.close()
            } catch (e: IOException) {
                sendBroadcast(Intent(INTENT_FILTER_COMM_SERVER_CLIENT_CONNECTION_ERROR))
            }
        }
    }

    private fun sendServerPublicKey() {
        lifecycleScope.launch(ioDispatcher) {
            val publicKey = encryptionManager.publicKey
            val data = JSONObject().apply {
                put(
                    ClientService.DATA_NODE,
                    JSONObject().apply {
                        put(CLIENT_PUBLIC_KEY, publicKey)
                    },
                )
            }
            sendBroadcast(Intent(ClientService.INTENT_FILTER_PUBLIC_KEY_SHARING))
            sendDataToClient(data.toString())
        }
    }

    private suspend fun responseConnectionAccepted() {
        val data = JSONObject().apply {
            put(
                ClientService.DATA_NODE,
                JSONObject().apply {
                    put(SERVER_RESPONSE_CONNECTION_ACCEPTED, true)
                },
            )
        }.toString()
        sendDataToClient(data)
    }

    private fun processPublicKeyReceivedResponse(serverResponsePublicKey: Boolean) {
        sendBroadcast(
            Intent(
                if (serverResponsePublicKey) {
                    ClientService.INTENT_FILTER_PUBLIC_KEY_SHARED
                } else {
                    ClientService.INTENT_FILTER_PUBLIC_KEY_SHARED_ERROR
                },
            ),
        )
    }

    // For testing
    inner class LocalBinder : Binder() {
        fun getService(): ServerService = this@ServerService
    }

    companion object {
        const val INTENT_FILTER_COMM_SERVER_CREATED = "intent_filter_comm_server_created"
        const val INTENT_FILTER_COMM_SERVER_STARTED = "intent_filter_comm_server_started"
        const val INTENT_FILTER_COMM_SERVER_CLIENT_CONNECTION_ERROR =
            "intent_filter_comm_server_connection_error"
        const val SERVER_RESPONSE_MESSAGE_TAMPERED = "server_response_message_tampered"
        const val SERVER_RESPONSE_MESSAGE_OK = "server_response_message_ok"
        const val SERVER_RESPONSE_CONNECTION_ACCEPTED = "server_response_connection_accepted"
        const val SERVER_RESPONSE_PUBLIC_KEY_RECEIVED = "server_response_public_key_received"

        fun startServer(context: Context) {
            context.startService(
                Intent(context, ServerService::class.java),
            )
        }

        fun stopServer(context: Context) {
            context.stopService(Intent(context, ServerService::class.java))
        }
    }
}
