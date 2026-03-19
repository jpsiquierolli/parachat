package com.example.parachat.domain.chat

import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    suspend fun createGroup(group: Group): String
    suspend fun updateGroup(group: Group)
    suspend fun addMember(groupId: String, userId: String)
    suspend fun removeMember(groupId: String, userId: String)
    fun observeGroups(userId: String): Flow<List<Group>>
    fun observeGroup(groupId: String): Flow<Group?>
}
