package dev.franco.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.inject.Inject

const val ALIAS = "SecureChat"
const val ANDROID_KEYSTORE = "AndroidKeyStore"

class EncryptionManagerImpl @Inject constructor() : EncryptionManager {
    private val keyPair: KeyPair

    init {
        Log.e("EncryptionManager", this.toString())
        keyPair = getKeyPairFromKeyStore() ?: generateKeyPair()
    }

    override val publicKey: String
        get() = keyPair.public.encoded.toBase64String()

    override val privateKey: String
        get() = keyPair.private.encoded.toBase64String()

    override fun encryptMessage(message: String, publicKeyString: String): ByteArray {
        val publicKey = convertToPublicKey(publicKeyString)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(message.toByteArray())
    }

    override fun decryptMessage(encryptedMessage: ByteArray): String {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, keyPair.private)
        return String(cipher.doFinal(encryptedMessage))
    }

    override fun signMessage(message: String): ByteArray {
        val privateKey = getPrivateKeyFromKeyStore()
        val signatureInstance = Signature.getInstance("SHA256withRSA")
        signatureInstance.initSign(privateKey)
        signatureInstance.update(message.toByteArray())
        return signatureInstance.sign()
    }

    override fun verifySignature(
        message: String,
        signature: ByteArray,
        publicKeyString: String,
    ): Boolean {
        val publicKey = convertToPublicKey(publicKeyString)
        val signatureInstance = Signature.getInstance("SHA256withRSA")
        signatureInstance.initVerify(publicKey)
        signatureInstance.update(message.toByteArray())
        return signatureInstance.verify(signature)
    }

    private fun generateKeyPair(): KeyPair {
        val keyPairGenerator =
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
        val keyGenParameterSpec = KeyGenParameterSpec
            .Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT or
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
            )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .build()
        keyPairGenerator.initialize(keyGenParameterSpec)
        return keyPairGenerator.generateKeyPair()
    }

    private fun getKeyPairFromKeyStore(): KeyPair? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val privateKey = keyStore.getKey(ALIAS, null) as? PrivateKey?
        Log.e("EncryptionManager", "PrivateKey: ${privateKey?.encoded?.toBase64String()}")
        val publicKey = privateKey?.let { keyStore.getCertificate(ALIAS).publicKey }
        Log.e("EncryptionManager", "PublicKey: ${publicKey?.encoded?.toBase64String()}")
        return if (privateKey != null && publicKey != null) {
            KeyPair(publicKey, privateKey)
        } else {
            null
        }
    }

    private fun convertToPublicKey(publicKeyString: String): PublicKey {
        val publicKeyBytes = publicKeyString.base64ToByteArray()
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        return keyFactory.generatePublic(keySpec)
    }

    private fun getPrivateKeyFromKeyStore(): PrivateKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        return keyStore.getKey(ALIAS, null) as PrivateKey
    }
}

fun ByteArray.toBase64String(): String {
    return Base64.encodeToString(this, Base64.NO_WRAP)
}

fun String.base64ToByteArray(): ByteArray {
    return Base64.decode(this, Base64.NO_WRAP)
}
