package dev.franco.securechat.data.source.local

import dev.franco.securechat.data.database.entity.LocalMessage
import kotlinx.coroutines.flow.Flow

interface MessagesRepository {
    suspend fun readMessages(): Flow<List<LocalMessage>>
    suspend fun createMessage(message: LocalMessage)
    suspend fun updateMessage(message: LocalMessage): Int
}
