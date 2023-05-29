package dev.franco.securechat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.franco.securechat.background.ConnectionManager
import dev.franco.securechat.di.ClientServiceManager
import dev.franco.securechat.di.ServerServiceManager
import java.util.UUID
import javax.inject.Inject

val EXPOSED_SERVICE_NAME = "NsdChat${UUID.randomUUID()}"
const val SEARCH_SERVICE_TYPE = "_nsdchat._tcp."

@HiltAndroidApp
class SecureChatApplication : Application() {
    @ClientServiceManager
    @Inject
    lateinit var clientConnectionManager: ConnectionManager

    @ServerServiceManager
    @Inject
    lateinit var serverConnectionManager: ConnectionManager

    override fun onCreate() {
        super.onCreate()
        startServer()
        startClient()
    }

    private fun startServer() {
        serverConnectionManager.startCommunication()
    }

    private fun startClient() {
        clientConnectionManager.startCommunication()
    }
}
