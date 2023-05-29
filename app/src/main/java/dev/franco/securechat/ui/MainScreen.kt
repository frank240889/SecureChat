package dev.franco.securechat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.franco.securechat.R
import dev.franco.securechat.ui.chats.ChatContent
import dev.franco.securechat.ui.chats.ChatsViewModel
import dev.franco.securechat.ui.start.DeviceInformationViewModel
import dev.franco.securechat.ui.start.DeviceInformationViewState
import dev.franco.securechat.ui.start.StartContent

@Composable
internal fun MainScreen(
    chatsViewModel: ChatsViewModel,
    deviceInformationViewModel: DeviceInformationViewModel,
) {
    val state by deviceInformationViewModel.viewState.collectAsState()
    val loading by deviceInformationViewModel.viewLoading.collectAsState()

    LaunchedEffect(Unit) {
        deviceInformationViewModel.registerDeviceOnNetwork()
    }
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(24.dp)
                    .size(64.dp)
                    .align(Alignment.BottomCenter),
            )
        }

        when (state) {
            is DeviceInformationViewState.CreatingKeys -> {
                Text(
                    stringResource(R.string.creating_keys),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center),
                )
            }

            is DeviceInformationViewState.KeysCreated -> {
                Text(
                    stringResource(R.string.keys_created),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center),
                )
            }

            is DeviceInformationViewState.RegisteringServiceOnNetwork -> {
                Text(
                    stringResource(R.string.registering_service_on_network),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center),
                )
            }

            is DeviceInformationViewState.ServiceRegisteredOnNetwork -> {
                Text(
                    stringResource(R.string.registered_service_on_network),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center),
                )
            }

            is DeviceInformationViewState.StartingServer -> {
                Text(
                    stringResource(R.string.starting_server),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center),
                )
            }

            is DeviceInformationViewState.ServerStarted -> {
                StartContent(
                    buttonEnabled = !loading,
                ) {
                    deviceInformationViewModel.discoverDevice()
                }
            }

            is DeviceInformationViewState.SearchingDevice -> {
                Text(
                    stringResource(R.string.searching_devices),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center),
                )
            }

            is DeviceInformationViewState.DeviceFound -> {
                Text(
                    stringResource(R.string.device_found),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center),
                )
            }

            is DeviceInformationViewState.DeviceNotFound -> {
                StartContent(
                    buttonEnabled = !loading,
                ) {
                    deviceInformationViewModel.discoverDevice()
                }
                /*Toast.makeText(
                    context,
                    stringResource(R.string.device_not_found),
                    Toast.LENGTH_SHORT,
                )
                    .show()*/
            }

            is DeviceInformationViewState.ConnectingToDevice -> {
                Text(
                    stringResource(R.string.connecting_to_device),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center),
                )
            }

            is DeviceInformationViewState.ConnectedToDevice -> {
                Text(
                    stringResource(R.string.connected_to_device),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center),
                )
            }

            is DeviceInformationViewState.SharingPublicKey -> {
                Text(
                    stringResource(R.string.sharing_public_key),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center),
                )
            }

            is DeviceInformationViewState.PublicKeyShared -> {
                Text(
                    stringResource(R.string.shared_public_key),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center),
                )
            }

            is DeviceInformationViewState.ConnectionToDeviceLost -> {
                Text(
                    stringResource(R.string.remove_device_connection_lost),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center),
                )
            }

            is DeviceInformationViewState.Error -> {
                StartContent(
                    message = (state as DeviceInformationViewState.Error).message,
                    buttonEnabled = !loading,
                ) {
                    deviceInformationViewModel.discoverDevice()
                }
            }

            is DeviceInformationViewState.ConnectionEstablishedSuccessfully -> {
                ChatContent(chatsViewModel)
            }

            is DeviceInformationViewState.Idle -> Unit
        }
    }
}
