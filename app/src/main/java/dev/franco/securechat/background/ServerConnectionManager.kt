package dev.franco.securechat.background

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ServerConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : ConnectionManager {
    override fun startCommunication() {
        ServerService.startServer(context)
    }

    override fun stopCommunication() {
        ServerService.stopServer(context)
    }
}
