package dev.franco.securechat.background

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ClientConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : ConnectionManager {
    override fun startCommunication() {
        ClientService.startClient(context)
    }

    override fun stopCommunication() {
        ClientService.stopClient(context)
    }

    override fun sendPublicKey() {
        ClientService.sendPublicKey(context)
    }
}
