package dev.franco.securechat.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.franco.securechat.data.database.entity.LocalMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM local_message ORDER BY date ASC")
    fun readMessages(): Flow<List<LocalMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createMessage(localMessage: LocalMessage)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateMessage(localMessage: LocalMessage): Int
}
