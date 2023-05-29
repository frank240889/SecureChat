package dev.franco.securechat.data.source.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class ClientConnectionRepository @Inject constructor() : DeviceConnectionRepository {

    private val store = MutableStateFlow<Pair<String, Int>?>(null)

    override suspend fun readDeviceConnectionData(): Flow<Pair<String, Int>?> = store

    override fun createDeviceConnectionData(serverData: Pair<String, Int>) {
        store.value = serverData
    }
}
