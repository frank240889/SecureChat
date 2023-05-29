package dev.franco.securechat.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.franco.securechat.data.database.entity.LocalMessage

@Database(entities = [LocalMessage::class], version = 1)
@TypeConverters(Converters::class)
abstract class ChatDataBase : RoomDatabase() {
    companion object {
        private var INSTANCE: ChatDataBase? = null

        @Synchronized
        fun getDatabase(@ApplicationContext context: Context): ChatDataBase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context,
                    ChatDataBase::class.java,
                    "chats",
                )
                    .build()
            }
            return INSTANCE!!
        }
    }

    abstract fun messageDao(): MessageDao
}
