package dev.franco.securechat.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_message")
data class LocalMessage(
    @PrimaryKey
    @ColumnInfo(name = "uid")
    val uid: String,
    @ColumnInfo(name = "from") val from: String,
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "sent") val sent: Boolean,
    @ColumnInfo(name = "self") val self: Boolean,
)
