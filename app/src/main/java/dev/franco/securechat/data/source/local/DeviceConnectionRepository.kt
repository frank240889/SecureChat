package dev.franco.securechat.data.source.local

import kotlinx.coroutines.flow.Flow

interface DeviceConnectionRepository {
    suspend fun readDeviceConnectionData(): Flow<Pair<String, Int>?>
    fun createDeviceConnectionData(serverData: Pair<String, Int>)
}
