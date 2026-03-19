package com.example.parachat.data.firebase.chat

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.example.parachat.domain.chat.Group

class FirebaseGroupRepository(
    private val database: FirebaseDatabase
) {
    private val groupsRef = database.getReference("groups")

    suspend fun createGroup(group: Group): String {
        val newGroupRef = groupsRef.push()
        val groupId = newGroupRef.key ?: throw Exception("Failed to get group ID")
        val payload = group.copy(id = groupId)
        newGroupRef.setValue(payload).await()
        return groupId
    }

    suspend fun updateGroup(group: Group) {
        groupsRef.child(group.id).setValue(group).await()
    }

    fun observeGroups(userId: String): Flow<List<Group>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groups = snapshot.children.mapNotNull { it.getValue(Group::class.java) }
                    .filter { it.members.contains(userId) }
                trySend(groups)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        groupsRef.addValueEventListener(listener)
        awaitClose { groupsRef.removeEventListener(listener) }
    }

    fun observeGroup(groupId: String): Flow<Group?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Group::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        groupsRef.child(groupId).addValueEventListener(listener)
        awaitClose { groupsRef.child(groupId).removeEventListener(listener) }
    }

    suspend fun addMember(groupId: String, userId: String) {
        val snapshot = groupsRef.child(groupId).get().await()
        val group = snapshot.getValue(Group::class.java) ?: return
        if (!group.members.contains(userId)) {
            val newMembers = group.members + userId
            groupsRef.child(groupId).child("members").setValue(newMembers).await()
        }
    }

    suspend fun removeMember(groupId: String, userId: String) {
        val snapshot = groupsRef.child(groupId).get().await()
        val group = snapshot.getValue(Group::class.java) ?: return
        val newMembers = group.members.filter { it != userId }
        groupsRef.child(groupId).child("members").setValue(newMembers).await()
    }
}
