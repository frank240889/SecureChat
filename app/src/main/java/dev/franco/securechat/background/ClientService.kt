package dev.franco.securechat.background

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Base64
import android.util.Base64.NO_WRAP
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.franco.securechat.background.ServerService.Companion.SERVER_RESPONSE_CONNECTION_ACCEPTED
import dev.franco.securechat.background.ServerService.Companion.SERVER_RESPONSE_PUBLIC_KEY_RECEIVED
import dev.franco.securechat.data.database.entity.LocalMessage
import dev.franco.securechat.data.source.local.DeviceConnectionRepository
import dev.franco.securechat.data.source.local.MessagesRepository
import dev.franco.securechat.di.ClientDeviceRepository
import dev.franco.securechat.di.IoDispatcher
import dev.franco.securechat.domain.DATE
import dev.franco.securechat.domain.FROM
import dev.franco.securechat.domain.MESSAGE
import dev.franco.securechat.domain.SELF
import dev.franco.security.CLIENT_PUBLIC_KEY
import dev.franco.security.EncryptionManager
import dev.franco.security.SERVER_PUBLIC_KEY
import dev.franco.security.Storage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class ClientService : LifecycleService() {

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var messagesRepository: MessagesRepository

    @ClientDeviceRepository
    @Inject
    lateinit var clientConnectionRepository: DeviceConnectionRepository

    @Inject
    lateinit var encryptionManager: EncryptionManager

    @Inject
    lateinit var storage: Storage

    private lateinit var socket: Socket

    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch(ioDispatcher) {
            clientConnectionRepository
                .readDeviceConnectionData()
                .collect { info ->
                    info?.let {
                        connectToServer(it.first, it.second)
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
        closeClient()
    }

    private fun connectToServer(ip: String, port: Int) {
        lifecycleScope.launch(ioDispatcher) {
            try {
                socket = Socket(ip, port)
                sendBroadcast(Intent(INTENT_FILTER_COMM_SERVICE_CONNECTED))
                launch {
                    receiveDataFromServer()
                }
                sendClientPublicKey()
            } catch (e: IOException) {
                e.printStackTrace()
                sendBroadcast(Intent(INTENT_FILTER_COMM_SERVICE_CONNECTION_ERROR))
            }
        }
    }

    private suspend fun receiveDataFromServer() {
        val reader = DataInputStream(socket.getInputStream())
        while (true) {
            try {
                val length = reader.readInt()
                val contentBuffer = ByteArray(length)
                reader.readFully(contentBuffer)
                val serverData = String(contentBuffer, Charsets.UTF_8)
                processServerData(serverData)
                Log.e("ClientService", "Recibiendo datos del servidor: $serverData")
            } catch (e: IOException) {
                sendBroadcast(Intent(ServerService.INTENT_FILTER_COMM_SERVER_CLIENT_CONNECTION_ERROR))
                e.printStackTrace()
                break
            }
        }
    }

    private suspend fun sendDataToServer(data: String) {
        withContext(ioDispatcher) {
            try {
                val outputStream = DataOutputStream(socket.getOutputStream())
                val jsonData = data.toByteArray(Charsets.UTF_8)
                outputStream.writeInt(jsonData.size)
                outputStream.write(jsonData)
                outputStream.flush()
                Log.e("ClientService", "Enviando datos al servidor: $data")
            } catch (e: IOException) {
                sendBroadcast(Intent(INTENT_FILTER_COMM_SERVICE_CONNECTION_ERROR))
                e.printStackTrace()
            }
        }
    }

    private suspend fun processServerData(data: String) {
        val jsonData = JSONObject(data)
        val nodeData = JSONObject(jsonData.getString(DATA_NODE))
        return when {
            nodeData.has(SERVER_RESPONSE_PUBLIC_KEY_RECEIVED) -> {
                processPublicKeyReceivedResponse(
                    nodeData.getBoolean(SERVER_RESPONSE_PUBLIC_KEY_RECEIVED),
                )
            }

            nodeData.has(CLIENT_PUBLIC_KEY) -> {
                saveServerPublicKey(nodeData.getString(CLIENT_PUBLIC_KEY))
            }

            /*nodeData.has(SERVER_RESPONSE_MESSAGE_TAMPERED) -> {
                processMessageReceivedTamperedResponse()
                null
            }*/

            /*
            nodeData.has(SERVER_RESPONSE_MESSAGE_OK) -> {
                Log.e("ClientConnectionService", "Message OK")
                null
            }*/

            nodeData.has(MESSAGE_NODE) -> {
                saveMessage(nodeData.getString(MESSAGE_NODE), nodeData.getString(SIGNATURE_NODE))
            }

            nodeData.has(SERVER_RESPONSE_CONNECTION_ACCEPTED) -> {
                sendBroadcast(Intent(INTENT_FILTER_CONNECTION_ACCEPTED))
            }

            else -> Unit
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
                            sendDataToServer(msg)
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
            .encryptMessage(messageString.toString(), storage.getString(SERVER_PUBLIC_KEY)!!)
        val signature = encryptionManager.signMessage(messageString.toString())

        val root = JSONObject().apply {
            put(
                ClientService.DATA_NODE,
                JSONObject().apply {
                    put(MESSAGE_NODE, Base64.encodeToString(encryptedMessage, NO_WRAP))
                    put(SIGNATURE_NODE, Base64.encodeToString(signature, NO_WRAP))
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
        val decoded = Base64.decode(messageData, NO_WRAP)
        val decrypted = encryptionManager.decryptMessage(decoded)
        val signature = Base64.decode(signatureData, NO_WRAP)
        val clientPublicKey = storage.getString(SERVER_PUBLIC_KEY)!!
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

    private suspend fun saveServerPublicKey(publicKey: String) {
        Log.e("ServerConnectionService", "Saving server public key: $publicKey")
        val data = JSONObject().apply {
            put(
                DATA_NODE,
                JSONObject().apply {
                    put(SERVER_RESPONSE_PUBLIC_KEY_RECEIVED, true)
                },
            )
        }.toString()
        storage.putString(SERVER_PUBLIC_KEY, publicKey)
        sendLastUnsentOwnMessage()
        sendDataToServer(data)
    }

    private fun processMessageReceivedTamperedResponse() {
        sendBroadcast(Intent(INTENT_FILTER_MESSAGE_TAMPERED))
    }

    private fun closeClient() {
        lifecycleScope.launch(ioDispatcher) {
            try {
                socket.close()
            } catch (e: IOException) {
                e.printStackTrace()
                sendBroadcast(Intent(ServerService.INTENT_FILTER_COMM_SERVER_CLIENT_CONNECTION_ERROR))
            }
        }
    }

    private fun sendClientPublicKey() {
        lifecycleScope.launch(ioDispatcher) {
            val publicKey = encryptionManager.publicKey
            val jsonObject = JSONObject().apply {
                put(
                    DATA_NODE,
                    JSONObject().apply {
                        put(CLIENT_PUBLIC_KEY, publicKey)
                    },
                )
            }
            sendBroadcast(Intent(INTENT_FILTER_PUBLIC_KEY_SHARING))
            sendDataToServer(jsonObject.toString())
        }
    }

    private fun processPublicKeyReceivedResponse(serverResponsePublicKey: Boolean) {
        sendBroadcast(
            Intent(
                if (serverResponsePublicKey) {
                    INTENT_FILTER_PUBLIC_KEY_SHARED
                } else {
                    INTENT_FILTER_PUBLIC_KEY_SHARED_ERROR
                },
            ),
        )
    }

    inner class LocalBinder : Binder() {
        fun getService(): ClientService = this@ClientService
    }

    companion object {
        const val SEND_PUBLIC_KEY = "send_public_key"
        const val INTENT_FILTER_COMM_SERVICE_CONNECTED = "intent_filter_comm_service_connected"
        const val INTENT_FILTER_COMM_SERVICE_CONNECTION_ERROR =
            "intent_filter_comm_service_connection_error"
        const val INTENT_FILTER_PUBLIC_KEY_SHARED = "intent_filter_public_key_shared"
        const val INTENT_FILTER_PUBLIC_KEY_SHARED_ERROR = "intent_filter_public_key_shared_error"
        const val INTENT_FILTER_PUBLIC_KEY_SHARING = "intent_filter_public_key_sharing"
        const val INTENT_FILTER_MESSAGE_TAMPERED = "intent_filter_message_tampered"
        const val INTENT_FILTER_CONNECTION_ACCEPTED = "intent_filter_connection_accepted"
        const val INTENT_FILTER_ACCEPTING_CONNECTION = "intent_filter_accepting_connection"
        const val CLIENT_RESPONSE_MESSAGE = "client_response_message"

        const val DATA_NODE = "data"
        const val SIGNATURE_NODE = "signature"
        const val MESSAGE_NODE = "message"

        fun startClient(context: Context) {
            context.startService(
                Intent(context, ClientService::class.java),
            )
        }

        fun sendPublicKey(context: Context) {
            context.startService(
                Intent(context, ClientService::class.java).apply {
                    action = SEND_PUBLIC_KEY
                },
            )
        }

        fun stopClient(context: Context) {
            context.stopService(Intent(context, ClientService::class.java))
        }
    }
}
