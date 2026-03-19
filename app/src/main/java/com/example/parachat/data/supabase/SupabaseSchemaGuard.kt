package com.example.parachat.data.supabase

import java.util.concurrent.ConcurrentHashMap

object SupabaseSchemaGuard {
    private val missingTables = ConcurrentHashMap.newKeySet<String>()

    fun isTableAvailable(table: String): Boolean = !missingTables.contains(table)

    fun markMissingTableIfNeeded(table: String, throwable: Throwable): Boolean {
        val message = throwable.message.orEmpty()
        val isMissing = message.contains("Could not find the table 'public.$table'", ignoreCase = true) ||
            message.contains("relation \"$table\" does not exist", ignoreCase = true)

        if (isMissing) {
            missingTables.add(table)
        }

        return isMissing
    }
}

