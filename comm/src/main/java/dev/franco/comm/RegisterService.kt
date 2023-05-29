package dev.franco.comm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.RegistrationListener
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Binder
import android.text.format.Formatter
import android.util.Log
import java.io.IOException
import java.net.ServerSocket

class RegisterService : Service() {
    private var nsdManager: NsdManager? = null

    // For testing
    var serviceStarted = false
    var serviceStopped = false

    private var registrationListener: RegistrationListener? = null
    private var port: Int = DEFAULT_PORT

    override fun onBind(intent: Intent?) = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        serviceStarted = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val serviceName = getServiceName(intent)
        if (serviceName == null) {
            Log.e("serviceName", "null")
            sendBroadcast(Intent(SERVICE_NAME_EMPTY))
        } else {
            Log.e("serviceName", serviceName)
            startServiceRegistration(serviceName)
        }
        return START_NOT_STICKY
    }

    override fun stopService(name: Intent?): Boolean {
        serviceStopped = true
        return super.stopService(name)
    }

    private fun startServiceRegistration(serviceName: String) {
        registrationListener = provideRegistrationListener()
        port = getPort()
        val serviceInfo = NsdServiceInfo()
        serviceInfo.serviceName = serviceName
        serviceInfo.serviceType = DEFAULT_SERVICE_TYPE
        serviceInfo.port = port
        nsdManager!!.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            registrationListener,
        )
    }

    private fun getPort(): Int {
        return try {
            val socket = ServerSocket(0)
            val p = socket.localPort
            socket.close()
            p
        } catch (e: IOException) {
            DEFAULT_PORT
        }
    }

    private fun getServiceName(intent: Intent?): String? {
        return intent?.getStringExtra(NAME)
    }

    private fun getSelfWiFiIp(): String {
        val wifiMgr = getSystemService(WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiMgr.connectionInfo
        val ip = wifiInfo.ipAddress
        val formattedIp = Formatter.formatIpAddress(ip)
        Log.e("getSelfWiFiIp", formattedIp)
        return formattedIp
    }

    private fun provideRegistrationListener(): RegistrationListener {
        return object : RegistrationListener {
            override fun onRegistrationFailed(service: NsdServiceInfo?, errorCode: Int) {
                Log.e("onRegistrationFailed", "$service - $errorCode")
                sendBroadcast(Intent(SERVICE_REGISTER_FAILED))
            }

            override fun onUnregistrationFailed(service: NsdServiceInfo?, errorCode: Int) {
                Log.e("onUnregistrationFailed", "$service - $errorCode")
                sendBroadcast(Intent(SERVICE_UNREGISTER_FAILED))
            }

            override fun onServiceRegistered(service: NsdServiceInfo?) {
                Log.e("onServiceRegistered", "$service ON PORT: $port")
                sendBroadcast(
                    Intent(SERVICE_REGISTERED).apply {
                        putExtra(IP, getSelfWiFiIp())
                        putExtra(PORT, port)
                    },
                )
            }

            override fun onServiceUnregistered(service: NsdServiceInfo?) {
                Log.e("onServiceUnregistered", "$service")
                sendBroadcast(Intent(SERVICE_UNREGISTERED))
            }
        }
    }

    companion object {
        const val IP = "ip"
        const val PORT = "port"
        const val SERVICE_NAME_EMPTY = "service_name_empty"
        const val SERVICE_REGISTERED = "service_registered"
        const val START_SERVICE_REGISTRATION = "start_service_registration"
        const val SERVICE_UNREGISTERED = "service_unregistered"
        const val SERVICE_REGISTER_FAILED = "service_register_failed"
        const val SERVICE_UNREGISTER_FAILED = "service_unregister_failed"
        const val NAME = "name"

        private const val DEFAULT_SERVICE_TYPE = "_nsdchat._tcp."
        private const val DEFAULT_PORT = 9999

        /**
         * Entry point to start discovery task.
         */
        fun startRegisterService(context: Context, serviceName: String) {
            val intent = Intent(context, RegisterService::class.java).apply {
                putExtra(NAME, serviceName)
            }
            context.startService(intent)
        }

        /**
         * Entry point to stop discovery task.
         */
        fun stopRegisterService(context: Context) {
            val result = context.stopService(
                Intent(context, RegisterService::class.java),
            )
        }
    }

    // For testing
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods.
        fun getService(): RegisterService = this@RegisterService
    }
}
