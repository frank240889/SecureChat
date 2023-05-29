package dev.franco.securechat.ui.start

sealed class DeviceInformationViewState {
    object CreatingKeys : DeviceInformationViewState()
    object KeysCreated : DeviceInformationViewState()
    object SharingPublicKey : DeviceInformationViewState()
    object PublicKeyShared : DeviceInformationViewState()
    object RegisteringServiceOnNetwork : DeviceInformationViewState()
    object ServiceRegisteredOnNetwork : DeviceInformationViewState()
    object StartingServer : DeviceInformationViewState()
    object ServerStarted : DeviceInformationViewState()
    object SearchingDevice : DeviceInformationViewState()
    object DeviceFound : DeviceInformationViewState()
    object DeviceNotFound : DeviceInformationViewState()
    object ConnectingToDevice : DeviceInformationViewState()
    object ConnectedToDevice : DeviceInformationViewState()
    object ConnectionToDeviceLost : DeviceInformationViewState()
    class Error(val message: String) : DeviceInformationViewState()
    object ConnectionEstablishedSuccessfully : DeviceInformationViewState()
    object Idle : DeviceInformationViewState()
}
