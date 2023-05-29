package dev.franco.securechat.domain

data class PlainDeviceInformation(
    val ip: String,
    val port: Int,
    val self: Boolean
)
