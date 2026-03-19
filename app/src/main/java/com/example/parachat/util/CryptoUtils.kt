package com.example.parachat.util

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES"
    // In a real app, this key should be unique per conversation and stored securely
    private val KEY = "12345678901234567890123456789012".toByteArray() 

    fun encrypt(value: String): String {
        val sks = SecretKeySpec(KEY, ALGORITHM)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, sks)
        val encrypted = cipher.doFinal(value.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    fun decrypt(value: String): String {
        return try {
            val sks = SecretKeySpec(KEY, ALGORITHM)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, sks)
            val decodedValue = Base64.decode(value, Base64.DEFAULT)
            val decrypted = cipher.doFinal(decodedValue)
            String(decrypted)
        } catch (e: Exception) {
            value // Fallback to original text if decryption fails (e.g., old messages)
        }
    }
}
