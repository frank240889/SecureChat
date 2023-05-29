package dev.franco.comm

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DiscoveryDeviceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : DiscoveryDevice {
    override fun startSearch(serviceType: String, serviceName: String) {
        DiscoveryService.startDiscoveryService(context, serviceType, serviceName)
    }

    override fun stopSearch() {
        DiscoveryService.stopDiscoveryService(context)
    }
}
