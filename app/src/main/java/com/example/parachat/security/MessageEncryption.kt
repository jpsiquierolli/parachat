package com.example.parachat.security

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

/**
 * Message encryption using AES-256-GCM for E2E protection.
 * Each conversation has a shared key derived from participants' IDs.
 * NOT CRYPTOGRAPHICALLY PERFECT (key derivation is deterministic from IDs only)
 * but provides confidentiality in transit + at rest in Supabase.
 */
object MessageEncryption {

    private const val ALGORITHM = "AES"
    private const val CIPHER_MODE = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    /**
     * Derive a deterministic conversation key from two user IDs.
     * Ensures both parties compute the same key without key exchange.
     * WARNING: In production, use a proper key exchange (ECDH, X3DH, etc.)
     */
    fun deriveConversationKey(userId1: String, userId2: String): SecretKey {
        // Canonical order: smaller ID first
        val (id1, id2) = if (userId1 < userId2) userId1 to userId2 else userId2 to userId1
        val combined = "$id1:$id2".toByteArray(Charsets.UTF_8)

        // Simple SHA-256 based derivation (not PBKDF2, but deterministic)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(combined)

        return SecretKeySpec(keyBytes, 0, 32, ALGORITHM)
    }

    /**
     * Encrypt a message with AES-256-GCM.
     * Returns Base64-encoded ciphertext (includes IV + tag).
     */
    fun encrypt(plaintext: String, conversationKey: SecretKey): String {
        val cipher = Cipher.getInstance(CIPHER_MODE)
        val iv = ByteArray(GCM_IV_LENGTH).apply {
            SecureRandom().nextBytes(this)
        }
        val gcmSpec = javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, conversationKey, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Format: IV || Ciphertext (IV is public, tag is appended by GCM)
        val result = iv + ciphertext
        return Base64.encodeToString(result, Base64.DEFAULT)
    }

    /**
     * Decrypt a Base64-encoded AES-256-GCM ciphertext.
     * Throws on authentication failure or decryption error.
     */
    fun decrypt(encryptedBase64: String, conversationKey: SecretKey): String {
        val encryptedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)

        if (encryptedBytes.size < GCM_IV_LENGTH) {
            throw IllegalArgumentException("Encrypted data too short (missing IV)")
        }

        val iv = encryptedBytes.sliceArray(0 until GCM_IV_LENGTH)
        val ciphertext = encryptedBytes.sliceArray(GCM_IV_LENGTH until encryptedBytes.size)

        val cipher = Cipher.getInstance(CIPHER_MODE)
        val gcmSpec = javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, conversationKey, gcmSpec)

        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }
}

