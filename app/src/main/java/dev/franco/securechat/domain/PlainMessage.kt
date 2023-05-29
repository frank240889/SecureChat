package dev.franco.securechat.domain

const val FROM = "from"
const val MESSAGE = "message"
const val DATE = "date"
const val SELF = "self"
data class PlainMessage(
    val from: String,
    val message: String,
    val date: String,
    val self: Boolean,
)
