package dev.franco.securechat.ui.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.franco.comm.DiscoveryDevice
import dev.franco.comm.DiscoveryService
import dev.franco.comm.DiscoveryService.Companion.ACTION
import dev.franco.comm.DiscoveryService.Companion.HOST_IP
import dev.franco.comm.DiscoveryService.Companion.PORT
import dev.franco.comm.RegisterService
import dev.franco.comm.RegistrationService
import dev.franco.securechat.EXPOSED_SERVICE_NAME
import dev.franco.securechat.SEARCH_SERVICE_TYPE
import dev.franco.securechat.background.ClientService
import dev.franco.securechat.background.ServerService
import dev.franco.securechat.data.source.local.DeviceConnectionRepository
import dev.franco.securechat.di.ClientDeviceRepository
import dev.franco.securechat.di.ServerDeviceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceInformationViewModel @Inject constructor(
    @ClientDeviceRepository private val clientConnectionRepository: DeviceConnectionRepository,
    @ServerDeviceRepository private val serverConnectionRepository: DeviceConnectionRepository,
    private val discoveryDevice: DiscoveryDevice,
    private val registrationService: RegistrationService,
) : ViewModel() {

    private val _viewState =
        MutableStateFlow<DeviceInformationViewState>(DeviceInformationViewState.Idle)
    val viewState = _viewState.asStateFlow()

    private val _viewLoading = MutableStateFlow(false)
    val viewLoading = _viewLoading.asStateFlow()

    fun registerDeviceOnNetwork() {
        registerServiceOnNetwork()
    }

    private fun registerServiceOnNetwork() {
        registrationService.startRegistration(EXPOSED_SERVICE_NAME)
    }

    fun discoverDevice() {
        _viewLoading.value = true
        _viewState.value = DeviceInformationViewState.SearchingDevice
        discoveryDevice.startSearch(SEARCH_SERVICE_TYPE, EXPOSED_SERVICE_NAME)
    }

    private fun stopDiscoverDevice() {
        discoveryDevice.stopSearch()
    }

    fun processRegisterServiceEvent(action: String?, ip: String?, port: Int?) {
        when (action) {
            RegisterService.START_SERVICE_REGISTRATION -> {
                _viewLoading.value = true
                _viewState.value = DeviceInformationViewState.RegisteringServiceOnNetwork
            }

            RegisterService.SERVICE_REGISTERED -> {
                _viewLoading.value = false
                _viewState.value = DeviceInformationViewState.ServiceRegisteredOnNetwork
                serverConnectionRepository.createDeviceConnectionData(Pair(ip!!, port!!))
            }

            RegisterService.SERVICE_UNREGISTERED -> {
                _viewLoading.value = false
            }

            RegisterService.SERVICE_REGISTER_FAILED -> {
                _viewLoading.value = false
                _viewState.value = DeviceInformationViewState.Error(action)
            }

            RegisterService.SERVICE_UNREGISTER_FAILED -> {
                _viewLoading.value = false
                _viewState.value = DeviceInformationViewState.Error(action)
            }
        }
    }

    fun processDiscoveryInformation(map: Map<String, String?>) {
        when (val action = map[ACTION]) {
            DiscoveryService.INTENT_FILTER_SERVICE_LOST -> {
                _viewLoading.value = true
                _viewState.value = DeviceInformationViewState.ConnectionToDeviceLost
                discoverDevice()
            }

            DiscoveryService.INTENT_FILTER_START_DISCOVERY_FAILED,
            DiscoveryService.INTENT_FILTER_FAILED_RESOLVED,
            -> {
                _viewLoading.value = false
                _viewState.value = DeviceInformationViewState.Error(action)
                stopDiscoverDevice()
            }

            DiscoveryService.INTENT_FILTER_DISCOVERY_TIMEOUT -> {
                _viewLoading.value = false
                _viewState.value = DeviceInformationViewState.DeviceNotFound
                stopDiscoverDevice()
            }

            DiscoveryService.INTENT_FILTER_SERVICE_FOUND -> {
                _viewLoading.value = true
                _viewState.value = DeviceInformationViewState.DeviceFound
            }

            DiscoveryService.INTENT_FILTER_DISCOVERY_STOPPED -> {
                _viewLoading.value = false
                _viewState.value = DeviceInformationViewState.DeviceNotFound
            }

            DiscoveryService.INTENT_FILTER_SERVICE_RESOLVED -> {
                _viewLoading.value = false
                discoveryDevice.stopSearch()
                clientConnectionRepository.createDeviceConnectionData(
                    Pair(map[HOST_IP].orEmpty(), map[PORT]?.toIntOrNull() ?: -1),
                )
            }
        }
    }

    fun processServerInitializationEvent(action: String?) {
        when (action) {
            ServerService.INTENT_FILTER_COMM_SERVER_CREATED -> {
                _viewLoading.value = true
                _viewState.value = DeviceInformationViewState.StartingServer
            }

            ServerService.INTENT_FILTER_COMM_SERVER_STARTED -> {
                _viewLoading.value = false
                _viewState.value = DeviceInformationViewState.ServerStarted
            }

            ServerService.INTENT_FILTER_COMM_SERVER_CLIENT_CONNECTION_ERROR -> {
                _viewLoading.value = false
                _viewState.value = DeviceInformationViewState.Error(action)
            }
        }
    }

    fun processClientCommunicationEvent(action: String?) {
        when (action) {
            ClientService.INTENT_FILTER_COMM_SERVICE_CONNECTED -> {
                _viewLoading.value = true
                _viewState.value = DeviceInformationViewState.ConnectedToDevice
                // goToChat()
            }

            ClientService.INTENT_FILTER_COMM_SERVICE_CONNECTION_ERROR -> {
                _viewLoading.value = false
                _viewState.value = DeviceInformationViewState.Error(action)
            }

            ClientService.INTENT_FILTER_PUBLIC_KEY_SHARING -> {
                _viewLoading.value = true
                _viewState.value = DeviceInformationViewState.SharingPublicKey
            }

            ClientService.INTENT_FILTER_PUBLIC_KEY_SHARED -> {
                _viewLoading.value = false
                _viewState.value = DeviceInformationViewState.PublicKeyShared
                stopDiscoverDevice()
                goToChat()
            }

            ClientService.INTENT_FILTER_ACCEPTING_CONNECTION -> {
                _viewLoading.value = true
            }

            ClientService.INTENT_FILTER_CONNECTION_ACCEPTED -> {
                _viewLoading.value = false
            }
        }
    }

    private fun goToChat() {
        viewModelScope.launch {
            delay(500)
            _viewLoading.value = false
            _viewState.value = DeviceInformationViewState.ConnectionEstablishedSuccessfully
        }
    }
}
