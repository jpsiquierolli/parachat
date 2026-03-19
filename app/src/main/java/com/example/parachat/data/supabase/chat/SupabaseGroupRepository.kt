package com.example.parachat.data.supabase.chat

import com.example.parachat.data.supabase.SupabaseSchemaGuard
import com.example.parachat.domain.chat.Group
import com.example.parachat.domain.chat.GroupRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import javax.inject.Inject

class SupabaseGroupRepository @Inject constructor(
    private val supabase: SupabaseClient
) : GroupRepository {

    private val groupsTable = "groups"
    private val conversationsTable = "conversations"

    private fun logTableMissingOnce() {
        android.util.Log.e(
            "SupabaseGroupRepo",
            "Supabase table '$groupsTable' is missing. Run the SQL migration scripts in parachat/supabase/migrations."
        )
    }

    override suspend fun createGroup(group: Group): String {
        val groupId = if (group.id.isBlank()) java.util.UUID.randomUUID().toString() else group.id
        val payload = group.copy(id = groupId)
        if (!SupabaseSchemaGuard.isTableAvailable(groupsTable)) {
            throw IllegalStateException("Supabase table '$groupsTable' is missing")
        }

        try {
            supabase.postgrest[groupsTable].upsert(payload)
            createGroupConversationRows(payload)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseGroupRepo", "Error creating group", e)
            if (SupabaseSchemaGuard.markMissingTableIfNeeded(groupsTable, e)) {
                logTableMissingOnce()
            }
            throw e
        }
        return groupId
    }

    private suspend fun createGroupConversationRows(group: Group) {
        if (!SupabaseSchemaGuard.isTableAvailable(conversationsTable)) return

        val members = (group.members + group.creatorId)
            .filter { it.isNotBlank() }
            .distinct()

        members.forEach { ownerId ->
            val rowId = "${ownerId}__group__${group.id}"
            val payload = mapOf(
                "id" to rowId,
                "owner_id" to ownerId,
                "other_user_id" to group.id,
                "title" to group.name,
                "last_message_preview" to "",
                "last_message_timestamp" to group.createdAt,
                "unread_count" to 0,
                "is_group" to true,
                "participants" to members,
                "pinned_message_id" to null
            )

            try {
                supabase.postgrest[conversationsTable].upsert(payload)
            } catch (e: Exception) {
                android.util.Log.e("SupabaseGroupRepo", "Error creating conversation row for group", e)
            }
        }
    }

    override suspend fun updateGroup(group: Group) {
        if (!SupabaseSchemaGuard.isTableAvailable(groupsTable)) {
            return
        }

        try {
            supabase.postgrest[groupsTable].upsert(group)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseGroupRepo", "Error updating group", e)
            if (SupabaseSchemaGuard.markMissingTableIfNeeded(groupsTable, e)) {
                logTableMissingOnce()
            }
        }
    }

    override suspend fun addMember(groupId: String, userId: String) {
        val group = loadGroup(groupId) ?: return
        if (group.members.contains(userId)) return
        updateGroup(group.copy(members = group.members + userId))
    }

    override suspend fun removeMember(groupId: String, userId: String) {
        val group = loadGroup(groupId) ?: return
        if (!group.members.contains(userId)) return
        updateGroup(group.copy(members = group.members.filterNot { it == userId }))
    }

    override fun observeGroups(userId: String): Flow<List<Group>> = flow {
        if (!SupabaseSchemaGuard.isTableAvailable(groupsTable)) {
            emit(emptyList())
            return@flow
        }

        while (coroutineContext.isActive) {
            try {
                val groups = supabase.postgrest[groupsTable].select {
                    order("created_at", Order.DESCENDING)
                }.decodeList<Group>()

                val visibleGroups = groups
                    .filter { it.creatorId == userId || it.members.contains(userId) }
                    .sortedBy { it.name.lowercase() }

                emit(visibleGroups)
            } catch (e: Exception) {
                android.util.Log.e("SupabaseGroupRepo", "Error loading groups", e)
                if (SupabaseSchemaGuard.markMissingTableIfNeeded(groupsTable, e)) {
                    logTableMissingOnce()
                }
                emit(emptyList())
            }

            delay(GROUPS_REFRESH_INTERVAL_MS)
        }
    }

    override fun observeGroup(groupId: String): Flow<Group?> = flow {
        emit(loadGroup(groupId))
    }

    private suspend fun loadGroup(groupId: String): Group? {
        if (!SupabaseSchemaGuard.isTableAvailable(groupsTable)) {
            return null
        }

        return try {
            supabase.postgrest[groupsTable].select {
                filter { eq("id", groupId) }
            }.decodeSingleOrNull<Group>()
        } catch (e: Exception) {
            android.util.Log.e("SupabaseGroupRepo", "Error loading group", e)
            if (SupabaseSchemaGuard.markMissingTableIfNeeded(groupsTable, e)) {
                logTableMissingOnce()
            }
            null
        }
    }

    companion object {
        private const val GROUPS_REFRESH_INTERVAL_MS = 2_000L
    }
}
