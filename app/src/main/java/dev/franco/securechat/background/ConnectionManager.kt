package dev.franco.securechat.background

interface ConnectionManager {
    fun startCommunication()
    fun stopCommunication()
    fun sendPublicKey() { }
}
