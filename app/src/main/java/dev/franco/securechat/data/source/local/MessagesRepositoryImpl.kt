package dev.franco.securechat.data.source.local

import dev.franco.securechat.data.database.MessageDao
import dev.franco.securechat.data.database.entity.LocalMessage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MessagesRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
) : MessagesRepository {

    override suspend fun readMessages(): Flow<List<LocalMessage>> {
        return messageDao.readMessages()
    }

    override suspend fun createMessage(message: LocalMessage) {
        messageDao.createMessage(message)
    }

    override suspend fun updateMessage(message: LocalMessage): Int {
        return messageDao.updateMessage(message)
    }
}
