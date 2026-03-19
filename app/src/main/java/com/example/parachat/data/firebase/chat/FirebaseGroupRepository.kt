package com.example.parachat.data.firebase.chat

import com.example.parachat.domain.chat.Conversation
import com.example.parachat.domain.chat.Group
import com.example.parachat.domain.chat.GroupRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseGroupRepository(
    private val database: FirebaseDatabase
) : GroupRepository {

    private val groupsRef = database.getReference("groups")
    private val conversationsRef = database.getReference("conversations")

    override suspend fun createGroup(group: Group): String {
        val ref = groupsRef.push()
        val id = ref.key ?: ""
        val normalizedMembers = (group.members + group.creatorId)
            .filter { it.isNotBlank() }
            .distinct()
        val groupWithId = group.copy(id = id, members = normalizedMembers)
        ref.setValue(groupWithId).await()
        upsertGroupConversationForMembers(
            groupId = id,
            groupName = groupWithId.name,
            members = normalizedMembers,
            createdAt = groupWithId.createdAt
        )
        return id
    }

    override suspend fun updateGroup(group: Group) {
        val previousSnapshot = groupsRef.child(group.id).get().await()
        val previousMembers = previousSnapshot.child("members").children
            .mapNotNull { it.getValue(String::class.java) }
            .toSet()

        val normalizedMembers = (group.members + group.creatorId)
            .filter { it.isNotBlank() }
            .distinct()
        groupsRef.child(group.id).setValue(group.copy(members = normalizedMembers)).await()

        upsertGroupConversationForMembers(
            groupId = group.id,
            groupName = group.name,
            members = normalizedMembers,
            createdAt = group.createdAt
        )

        val removedMembers = previousMembers - normalizedMembers.toSet()
        removedMembers.forEach { memberId ->
            conversationsRef.child(memberId).child(group.id).removeValue().await()
        }
    }

    override suspend fun addMember(groupId: String, userId: String) {
        val snapshot = groupsRef.child(groupId).child("members").get().await()
        val currentMembers = snapshot.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
        if (!currentMembers.contains(userId)) {
            currentMembers.add(userId)
            groupsRef.child(groupId).child("members").setValue(currentMembers).await()

            val groupSnapshot = groupsRef.child(groupId).get().await()
            val groupName = groupSnapshot.child("name").getValue(String::class.java).orEmpty().ifBlank { "Grupo" }
            val creatorId = groupSnapshot.child("creatorId").getValue(String::class.java).orEmpty()
            val members = (currentMembers + creatorId)
                .filter { it.isNotBlank() }
                .distinct()
            val createdAt = groupSnapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

            upsertGroupConversationForMembers(
                groupId = groupId,
                groupName = groupName,
                members = members,
                createdAt = createdAt
            )
        }
    }

    override suspend fun removeMember(groupId: String, userId: String) {
        val snapshot = groupsRef.child(groupId).child("members").get().await()
        val currentMembers = snapshot.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
        if (currentMembers.contains(userId)) {
            currentMembers.remove(userId)
            groupsRef.child(groupId).child("members").setValue(currentMembers).await()
            conversationsRef.child(userId).child(groupId).removeValue().await()

            val groupSnapshot = groupsRef.child(groupId).get().await()
            val groupName = groupSnapshot.child("name").getValue(String::class.java).orEmpty().ifBlank { "Grupo" }
            val creatorId = groupSnapshot.child("creatorId").getValue(String::class.java).orEmpty()
            val members = (currentMembers + creatorId)
                .filter { it.isNotBlank() }
                .distinct()
            val createdAt = groupSnapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

            upsertGroupConversationForMembers(
                groupId = groupId,
                groupName = groupName,
                members = members,
                createdAt = createdAt
            )
        }
    }

    override fun observeGroups(userId: String): Flow<List<Group>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groups = snapshot.children.mapNotNull { it.getValue(Group::class.java) }
                    .filter { it.members.contains(userId) || it.creatorId == userId }
                trySend(groups)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w(
                    "FirebaseGroupRepo",
                    "observeGroups cancelled for $userId: ${error.code} ${error.message}"
                )
                trySend(emptyList())
            }
        }
        groupsRef.addValueEventListener(listener)
        awaitClose { groupsRef.removeEventListener(listener) }
    }

    override fun observeGroup(groupId: String): Flow<Group?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Group::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w(
                    "FirebaseGroupRepo",
                    "observeGroup cancelled for $groupId: ${error.code} ${error.message}"
                )
                trySend(null)
            }
        }
        groupsRef.child(groupId).addValueEventListener(listener)
        awaitClose { groupsRef.child(groupId).removeEventListener(listener) }
    }

    private suspend fun upsertGroupConversationForMembers(
        groupId: String,
        groupName: String,
        members: List<String>,
        createdAt: Long
    ) {
        val title = groupName.ifBlank { "Grupo" }
        members.forEach { memberId ->
            val ref = conversationsRef.child(memberId).child(groupId)
            val current = ref.get().await().getValue(Conversation::class.java)
            val conversation = Conversation(
                id = current?.id ?: "group_$groupId",
                otherUserId = groupId,
                title = title,
                lastMessagePreview = current?.lastMessagePreview.orEmpty(),
                lastMessageTimestamp = current?.lastMessageTimestamp ?: createdAt,
                unreadCount = current?.unreadCount ?: 0,
                isGroup = true,
                participants = members,
                pinnedMessageId = current?.pinnedMessageId
            )
            ref.setValue(conversation).await()
        }
    }
}
