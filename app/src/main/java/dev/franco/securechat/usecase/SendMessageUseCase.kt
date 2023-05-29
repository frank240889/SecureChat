package dev.franco.securechat.usecase

interface SendMessageUseCase {
    suspend fun send(message: String, from: String): SendMessageResult
}

sealed class SendMessageResult {
    object OnSuccess : SendMessageResult()
    object OnEmpty : SendMessageResult()
}
