package com.example.parachat.data

import com.example.parachat.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseProvider {
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL
    private val supabaseKey: String = BuildConfig.SUPABASE_KEY

    val client = createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseKey
    ) {
        install(Postgrest)
        install(Realtime)
        install(Storage)
        
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
        })
    }

    init {
        val looksLikePlaceholder = supabaseKey.isBlank() ||
            supabaseUrl.isBlank() ||
            supabaseUrl.contains("example", ignoreCase = true) ||
            supabaseKey.contains("replace", ignoreCase = true) ||
            supabaseKey.contains("dummy", ignoreCase = true)

        if (looksLikePlaceholder) {
            android.util.Log.e(
                "SupabaseProvider",
                "WARNING: Supabase credentials look like placeholders. Configure SUPABASE_URL and SUPABASE_KEY in gradle.properties or environment variables."
            )
        }
    }
}
