package dev.franco.securechat.usecase

import dev.franco.securechat.domain.PlainMessage
import kotlinx.coroutines.flow.Flow

interface GetMessagesUseCase {
    suspend fun getMessages(): Flow<ReadMessagesResult>
}

sealed class ReadMessagesResult {
    class OnSuccess(val messages: List<PlainMessage>) : ReadMessagesResult()
    object OnEmpty : ReadMessagesResult()
}
