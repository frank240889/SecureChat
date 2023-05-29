package dev.franco.securechat.usecase

import dev.franco.securechat.data.database.entity.LocalMessage
import dev.franco.securechat.data.source.local.MessagesRepository
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class SendMessageUseCaseImpl @Inject constructor(
    private val messagesRepository: MessagesRepository,
) : SendMessageUseCase {
    override suspend fun send(message: String, from: String): SendMessageResult {
        return if (message.isBlank()) {
            SendMessageResult.OnEmpty
        } else {
            val encryptedMessage = cipher(message)
            val signedMessage = sign(encryptedMessage)
            messagesRepository.createMessage(
                LocalMessage(
                    uid = UUID.randomUUID().toString(),
                    from = from,
                    message = signedMessage,
                    date = Date().time,
                    sent = false,
                    self = true,
                ),
            )
            SendMessageResult.OnSuccess
        }
    }

    private fun cipher(value: String): String {
        return value
    }

    private fun sign(value: String): String {
        return value
    }
}
