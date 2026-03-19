package com.example.parachat.data.supabase.storage

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class MediaStorageRepository(
    private val client: SupabaseClient,
    private val bucketName: String = "chat-media"
) {
    private val bucket = client.storage.from(bucketName)

    suspend fun uploadBytes(
        ownerId: String,
        bytes: ByteArray,
        extension: String,
        mimeType: String
    ): String = withContext(Dispatchers.IO) {
        val objectPath = "$ownerId/${UUID.randomUUID()}.$extension"
        bucket.upload(objectPath, bytes) {
            upsert = true
            contentType = io.ktor.http.ContentType.parse(mimeType)
        }
        bucket.publicUrl(objectPath)
    }
}
