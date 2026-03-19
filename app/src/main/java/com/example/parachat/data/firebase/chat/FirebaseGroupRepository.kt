package com.example.parachat.data.firebase.chat

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

    override suspend fun createGroup(group: Group): String {
        val ref = groupsRef.push()
        val id = ref.key ?: ""
        val normalizedMembers = (group.members + group.creatorId)
            .filter { it.isNotBlank() }
            .distinct()
        val groupWithId = group.copy(id = id, members = normalizedMembers)
        ref.setValue(groupWithId).await()
        return id
    }

    override suspend fun updateGroup(group: Group) {
        val normalizedMembers = (group.members + group.creatorId)
            .filter { it.isNotBlank() }
            .distinct()
        groupsRef.child(group.id).setValue(group.copy(members = normalizedMembers)).await()
    }

    override suspend fun addMember(groupId: String, userId: String) {
        val snapshot = groupsRef.child(groupId).child("members").get().await()
        val currentMembers = snapshot.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
        if (!currentMembers.contains(userId)) {
            currentMembers.add(userId)
            groupsRef.child(groupId).child("members").setValue(currentMembers).await()
        }
    }

    override suspend fun removeMember(groupId: String, userId: String) {
        val snapshot = groupsRef.child(groupId).child("members").get().await()
        val currentMembers = snapshot.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
        if (currentMembers.contains(userId)) {
            currentMembers.remove(userId)
            groupsRef.child(groupId).child("members").setValue(currentMembers).await()
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
}
