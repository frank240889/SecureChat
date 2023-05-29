package dev.franco.comm

interface DiscoveryDevice {
    fun startSearch(serviceType: String, serviceName: String)
    fun stopSearch()
}
