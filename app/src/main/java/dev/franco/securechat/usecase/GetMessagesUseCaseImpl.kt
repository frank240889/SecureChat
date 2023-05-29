package dev.franco.securechat.usecase

import dev.franco.securechat.data.source.local.MessagesRepository
import dev.franco.securechat.domain.PlainMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject

class GetMessagesUseCaseImpl @Inject constructor(
    private val messagesRepository: MessagesRepository,
) : GetMessagesUseCase {
    override suspend fun getMessages(): Flow<ReadMessagesResult> {
        return messagesRepository
            .readMessages()
            .map { messages ->
                if (messages.isEmpty()) {
                    ReadMessagesResult.OnEmpty
                } else {
                    val plainMessages = messages.map { secureMessage ->
                        PlainMessage(
                            from = secureMessage.from,
                            message = secureMessage.message,
                            date = Date(secureMessage.date).toString(),
                            self = secureMessage.self,
                        )
                    }
                    ReadMessagesResult.OnSuccess(plainMessages)
                }
            }
    }
}
