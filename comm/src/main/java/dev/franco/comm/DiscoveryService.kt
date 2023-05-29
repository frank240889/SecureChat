package dev.franco.comm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.DiscoveryListener
import android.net.nsd.NsdManager.FAILURE_INTERNAL_ERROR
import android.net.nsd.NsdManager.ResolveListener
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Binder
import android.text.format.Formatter
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DiscoveryService : Service() {

    private var nsdManager: NsdManager? = null
    private var serviceInfo: NsdServiceInfo? = null

    // For testing
    var serviceStarted = false
    var serviceStopped = false

    private var discoveryListener: DiscoveryListener? = null

    private var resolveListener: ResolveListener? = null
    private val timeOutJob: Job = Job()
    private val serviceCoroutineScope = CoroutineScope(timeOutJob)

    private lateinit var myIp: String
    private var serviceType: String = ""
    private var serviceName: String = ""

    override fun onBind(intent: Intent?) = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        serviceStarted = true
        myIp = getSelfWiFiIp()
        Log.e("my ip", myIp)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        nsdManager = getSystemService(NSD_SERVICE) as NsdManager
        serviceType = getServiceType(intent)
        serviceName = getServiceName(intent)
        discoveryListener = provideDiscoveryListener()
        discoverServices(serviceType, discoveryListener)
        return START_NOT_STICKY
    }

    override fun stopService(name: Intent?): Boolean {
        serviceStopped = true
        stopDiscoverServices(discoveryListener)
        resolveListener = null
        discoveryListener = null
        return super.stopService(name)
    }

    private fun getServiceType(intent: Intent?) = intent?.getStringExtra(SERVICE_TYPE).orEmpty()

    private fun getServiceName(intent: Intent?) = intent?.getStringExtra(SERVICE_NAME).orEmpty()

    private fun discoverServices(
        serviceType: String,
        listener: DiscoveryListener?,
        protocolType: Int = NsdManager.PROTOCOL_DNS_SD,
    ) {
        if (serviceType.isEmpty()) {
            Log.e("discoverServices", serviceType)
            sendBroadcast(
                Intent(INTENT_FILTER_INVALID_SERVICE_TYPE).apply {
                    putExtra(NSD_ERROR_CODE, INVALID_SERVICE.toString())
                },
            )
            stopDiscoveryService(this)
        } else {
            tryStartDiscovery(serviceType, protocolType, listener)
        }
    }

    private fun tryStartDiscovery(
        serviceType: String,
        protocolType: Int,
        listener: DiscoveryListener?,
    ) {
        try {
            listener?.let {
                Log.e("tryStartDiscovery", serviceType)
                nsdManager!!.discoverServices(serviceType, protocolType, it)
                dispatchTimeoutNsdDiscoveryAt()
            } ?: run {
            }
        } catch (_: IllegalArgumentException) {
            listener?.let {
                nsdManager!!.stopServiceDiscovery(it)
            }
            sendBroadcast(
                Intent(INTENT_FILTER_START_DISCOVERY_FAILED).apply {
                    putExtra(SERVICE_NAME, serviceInfo)
                    putExtra(NSD_ERROR_CODE, FAILURE_INTERNAL_ERROR.toString())
                },
            )
            stopDiscoveryService(this)
        }
    }

    private fun stopDiscoverServices(discoveryListener: DiscoveryListener?) {
        try {
            discoveryListener?.let {
                nsdManager?.stopServiceDiscovery(it)
            }
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun dispatchTimeoutNsdDiscoveryAt(timeout: Long = 30000L) {
        serviceCoroutineScope.launch {
            delay(timeout)
            sendBroadcastTimeout()
            Log.e("discoverServicesTimeOut", "discoverServicesTimeOut")
            stopDiscoveryService(this@DiscoveryService)
            cancel()
        }
    }

    private fun cancelTimeoutNsdDiscovery() {
        if (timeOutJob.isActive) {
            timeOutJob.cancel()
        }
    }

    private fun sendBroadcastTimeout() {
        sendBroadcast(
            Intent(INTENT_FILTER_DISCOVERY_TIMEOUT).apply {
                putExtra(SERVICE_TYPE, serviceInfo?.serviceType)
                putExtra(SERVICE_NAME, serviceInfo?.serviceName)
                putExtra(NSD_ERROR_CODE, SERVICE_DISCOVERY_TIMEOUT.toString())
            },
        )
    }

    private fun provideDiscoveryListener(): DiscoveryListener {
        return object : DiscoveryListener {
            override fun onDiscoveryStarted(service: String?) {
                Log.e("onDiscoveryStarted", service.toString())
                sendBroadcast(
                    Intent(INTENT_FILTER_DISCOVERY_STARTED).apply {
                        putExtra(SERVICE_NAME, service)
                    },
                )
            }

            override fun onDiscoveryStopped(service: String?) {
                Log.e("onDiscoveryStopped", service.toString())
                sendBroadcast(
                    Intent(INTENT_FILTER_DISCOVERY_STOPPED).apply {
                        putExtra(SERVICE_NAME, service)
                    },
                )
                cancelTimeoutNsdDiscovery()
                stopDiscoveryService(this@DiscoveryService)
            }

            override fun onStartDiscoveryFailed(service: String?, code: Int) {
                Log.e("onStartDiscoveryFailed", "service $service - code: $code")
                sendBroadcast(
                    Intent(INTENT_FILTER_START_DISCOVERY_FAILED).apply {
                        putExtra(SERVICE_NAME, service)
                        putExtra(NSD_ERROR_CODE, code.toString())
                    },
                )
                cancelTimeoutNsdDiscovery()
                stopDiscoveryService(this@DiscoveryService)
            }

            override fun onStopDiscoveryFailed(service: String?, code: Int) {
                Log.e("onStopDiscoveryFailed", "service $service - code: $code")
                sendBroadcast(
                    Intent(INTENT_FILTER_STOP_DISCOVERY_FAILED).apply {
                        putExtra(SERVICE_NAME, service)
                        putExtra(NSD_ERROR_CODE, code.toString())
                    },
                )
                cancelTimeoutNsdDiscovery()
                stopDiscoveryService(this@DiscoveryService)
            }

            override fun onServiceFound(service: NsdServiceInfo?) {
                Log.e("onServiceFound", "$service")
                if (service?.serviceType == this@DiscoveryService.serviceType &&
                    service.serviceName != this@DiscoveryService.serviceName &&
                    service.serviceName?.contains("NsdChat") == true
                ) {
                    Log.e("Filtered service", "$service")
                    serviceInfo = service
                    sendBroadcast(
                        Intent(INTENT_FILTER_SERVICE_FOUND).apply {
                            putExtra(SERVICE_TYPE, service.serviceType)
                            putExtra(SERVICE_NAME, service.serviceName)
                        },
                    )
                    stopDiscoverServices(discoveryListener)
                    resolveListener = provideResolveListener()
                    nsdManager?.resolveService(serviceInfo, resolveListener)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo?) {
                Log.e("onServiceLost", "service $service")
                sendBroadcast(
                    Intent(INTENT_FILTER_SERVICE_LOST).apply {
                        putExtra(HOST_IP, service?.host)
                        putExtra(PORT, service?.port)
                        putExtra(SERVICE_TYPE, service?.serviceType)
                        putExtra(SERVICE_NAME, service?.serviceName)
                    },
                )
                cancelTimeoutNsdDiscovery()
                stopDiscoveryService(this@DiscoveryService)
            }
        }
    }

    private fun provideResolveListener(): ResolveListener {
        return object : ResolveListener {
            override fun onResolveFailed(service: NsdServiceInfo?, code: Int) {
                Log.e("onResolveFailed", "service $service - code: $code")
                cancelTimeoutNsdDiscovery()
                if (service?.serviceType == this@DiscoveryService.serviceType &&
                    service.serviceName != this@DiscoveryService.serviceName &&
                    service.serviceName?.contains("NsdChat") == true
                ) {
                    Log.e("filtered onResolveFailed", "service $service - code: $code")
                    sendBroadcast(
                        Intent(INTENT_FILTER_FAILED_RESOLVED).apply {
                            putExtra(SERVICE_TYPE, service.serviceType)
                            putExtra(SERVICE_NAME, service.serviceName)
                            putExtra(NSD_ERROR_CODE, code.toString())
                        },
                    )
                }
            }

            override fun onServiceResolved(service: NsdServiceInfo?) {
                Log.e("onServiceResolved", "service $service")
                if (service?.serviceName != this@DiscoveryService.serviceName &&
                    service?.serviceName?.contains("NsdChat") == true
                ) {
                    Log.e("filtered onServiceResolved", "service $service")
                    Log.e("sendBroadcast", INTENT_FILTER_SERVICE_RESOLVED)
                    sendBroadcast(
                        Intent(INTENT_FILTER_SERVICE_RESOLVED).apply {
                            putExtra(HOST_IP, service.host?.hostAddress)
                            putExtra(PORT, service.port.toString())
                            putExtra(SERVICE_TYPE, service.serviceType)
                            putExtra(SERVICE_NAME, service.serviceName)
                        },
                    )
                    cancelTimeoutNsdDiscovery()
                    stopDiscoveryService(this@DiscoveryService)
                }
            }
        }
    }

    private fun getSelfWiFiIp(): String {
        val wifiMgr = getSystemService(WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiMgr.connectionInfo
        val ip = wifiInfo.ipAddress
        return Formatter.formatIpAddress(ip)
    }

    companion object {
        const val INTENT_FILTER_DISCOVERY_STARTED = "intent_filter_discovery_started"
        const val INTENT_FILTER_DISCOVERY_STOPPED = "intent_filter_discovery_stopped"
        const val INTENT_FILTER_START_DISCOVERY_FAILED = "intent_filter_start_discovery_failed"
        const val INTENT_FILTER_STOP_DISCOVERY_FAILED = "intent_filter_stop_discovery_failed"
        const val INTENT_FILTER_SERVICE_FOUND = "intent_filter_service_found"
        const val INTENT_FILTER_SERVICE_LOST = "intent_filter_service_lost"
        const val INTENT_FILTER_SERVICE_RESOLVED = "intent_filter_service_resolved"
        const val INTENT_FILTER_FAILED_RESOLVED = "intent_filter_failed_resolved"
        const val INTENT_FILTER_DISCOVERY_TIMEOUT = "intent_filter_discovery_timeout"

        const val INTENT_FILTER_INVALID_SERVICE_TYPE = "intent_filter_invalid_service_type"

        const val SERVICE_TYPE = "service_type"
        const val HOST_IP = "host_ip"
        const val PORT = "port"

        const val NSD_ERROR_CODE = "nsd_error_code"
        const val SERVICE_NAME = "service_name"

        const val INVALID_SERVICE = 10

        // Occur when request to NSD last so much for connection issues.
        const val SERVICE_DISCOVERY_TIMEOUT = 20
        const val ACTION = "action"

        fun startDiscoveryService(context: Context, serviceType: String, serviceName: String?) {
            val intent = Intent(context, DiscoveryService::class.java).apply {
                putExtra(SERVICE_TYPE, serviceType)
                putExtra(SERVICE_NAME, serviceName)
            }
            context.startService(intent)
        }

        fun stopDiscoveryService(context: Context) {
            context.stopService(
                Intent(context, DiscoveryService::class.java),
            )
        }

        fun extractDiscoveryData(intent: Intent?): MutableMap<String, String?> {
            return mutableMapOf(
                ACTION to intent?.action,
                HOST_IP to intent?.getStringExtra(HOST_IP),
                PORT to intent?.getStringExtra(PORT),
                SERVICE_NAME to intent?.getStringExtra(SERVICE_NAME),
                NSD_ERROR_CODE to intent?.getStringExtra(NSD_ERROR_CODE),
            )
        }
    }

    // For testing
    inner class LocalBinder : Binder() {
        fun getService(): DiscoveryService = this@DiscoveryService
    }
}
