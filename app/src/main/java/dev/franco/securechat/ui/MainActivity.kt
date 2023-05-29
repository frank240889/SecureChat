package dev.franco.securechat.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import dev.franco.comm.DiscoveryService
import dev.franco.comm.RegisterService
import dev.franco.securechat.background.ClientService
import dev.franco.securechat.background.ServerService
import dev.franco.securechat.ui.chats.ChatsViewModel
import dev.franco.securechat.ui.start.DeviceInformationViewModel
import dev.franco.securechat.ui.theme.SecureChatTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val chatsViewModel: ChatsViewModel by viewModels()
    private val deviceInformationViewModel: DeviceInformationViewModel by viewModels()

    private val discoveryFilters = listOf(
        IntentFilter(DiscoveryService.INTENT_FILTER_DISCOVERY_STARTED),
        IntentFilter(DiscoveryService.INTENT_FILTER_SERVICE_FOUND),
        IntentFilter(DiscoveryService.INTENT_FILTER_SERVICE_LOST),
        IntentFilter(DiscoveryService.INTENT_FILTER_DISCOVERY_TIMEOUT),
        IntentFilter(DiscoveryService.INTENT_FILTER_START_DISCOVERY_FAILED),
        IntentFilter(DiscoveryService.INTENT_FILTER_DISCOVERY_STOPPED),
        IntentFilter(DiscoveryService.INTENT_FILTER_STOP_DISCOVERY_FAILED),
        IntentFilter(DiscoveryService.INTENT_FILTER_FAILED_RESOLVED),
        IntentFilter(DiscoveryService.INTENT_FILTER_INVALID_SERVICE_TYPE),
        IntentFilter(DiscoveryService.INTENT_FILTER_SERVICE_RESOLVED),
    )

    private val communicationFilters = listOf(
        IntentFilter(ClientService.INTENT_FILTER_COMM_SERVICE_CONNECTED),
        IntentFilter(ClientService.INTENT_FILTER_COMM_SERVICE_CONNECTION_ERROR),
        IntentFilter(ClientService.INTENT_FILTER_PUBLIC_KEY_SHARED),
        IntentFilter(ClientService.INTENT_FILTER_PUBLIC_KEY_SHARING),
        IntentFilter(ClientService.INTENT_FILTER_CONNECTION_ACCEPTED),
        IntentFilter(ClientService.INTENT_FILTER_ACCEPTING_CONNECTION),
    )

    private val serverFilters = listOf(
        IntentFilter(ServerService.INTENT_FILTER_COMM_SERVER_CREATED),
        IntentFilter(ServerService.INTENT_FILTER_COMM_SERVER_STARTED),
        IntentFilter(ServerService.INTENT_FILTER_COMM_SERVER_CLIENT_CONNECTION_ERROR),
    )

    private val registerServiceFilters = listOf(
        IntentFilter(RegisterService.START_SERVICE_REGISTRATION),
        IntentFilter(RegisterService.SERVICE_REGISTERED),
        IntentFilter(RegisterService.SERVICE_UNREGISTERED),
        IntentFilter(RegisterService.SERVICE_REGISTER_FAILED),
        IntentFilter(RegisterService.SERVICE_UNREGISTER_FAILED),
    )

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            processDiscoveryResult(intent)
        }
    }

    private val communicationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            processCommunicationResult(intent)
        }
    }

    private val serverReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            processServerResult(intent)
        }
    }

    private val registerServiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            processRegisterServiceResult(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerFilters(discoveryFilters, discoveryReceiver)
        registerFilters(communicationFilters, communicationReceiver)
        registerFilters(serverFilters, serverReceiver)
        registerFilters(registerServiceFilters, registerServiceReceiver)
        setContent {
            SecureChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainScreen(
                        chatsViewModel = chatsViewModel,
                        deviceInformationViewModel = deviceInformationViewModel,
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterFilters(discoveryReceiver)
        unregisterFilters(communicationReceiver)
        unregisterFilters(serverReceiver)
        unregisterFilters(registerServiceReceiver)
    }

    private fun processDiscoveryResult(result: Intent?) {
        val resultMap = DiscoveryService.extractDiscoveryData(result)
        deviceInformationViewModel.processDiscoveryInformation(resultMap)
    }

    private fun processCommunicationResult(intent: Intent?) {
        val action = intent?.action
        deviceInformationViewModel.processClientCommunicationEvent(action)
    }

    private fun processServerResult(intent: Intent?) {
        val action = intent?.action
        Log.e("processServerResult", action.toString())
        deviceInformationViewModel.processServerInitializationEvent(action)
    }

    private fun processRegisterServiceResult(intent: Intent?) {
        val action = intent?.action
        val ip = intent?.getStringExtra(RegisterService.IP)
        val port = intent?.getIntExtra(RegisterService.PORT, -1)
        deviceInformationViewModel.processRegisterServiceEvent(action, ip, port)
    }

    private fun registerFilters(filters: List<IntentFilter>, broadcastReceiver: BroadcastReceiver) {
        filters.forEach {
            registerReceiver(broadcastReceiver, it)
        }
    }

    private fun unregisterFilters(broadcastReceiver: BroadcastReceiver) {
        unregisterReceiver(broadcastReceiver)
    }
}
