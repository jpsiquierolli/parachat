package com.example.parachat.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseProvider {
    // TODO: Replace with your actual Supabase URL and Anon Key
    private const val SUPABASE_URL = "https://hfnphqthphorkhqcucee.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_06hFbleNTt7kgljTzb6IoQ_UAlSb2hI"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Realtime)
        install(Storage)
        
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
        })
    }
}
