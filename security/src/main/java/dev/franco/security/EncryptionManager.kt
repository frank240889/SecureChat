package dev.franco.security

const val CLIENT_PUBLIC_KEY = "client_public_key"
const val SERVER_PUBLIC_KEY = "server_public_key"
interface EncryptionManager {
    val publicKey: String
    val privateKey: String

    fun encryptMessage(message: String, publicKeyString: String): ByteArray
    fun decryptMessage(encryptedMessage: ByteArray): String
    fun signMessage(message: String): ByteArray
    fun verifySignature(message: String, signature: ByteArray, publicKeyString: String): Boolean
}
