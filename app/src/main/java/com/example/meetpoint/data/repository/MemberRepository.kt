package com.example.meetpoint.data.repository

import com.example.meetpoint.data.local.db.MemberDao
import com.example.meetpoint.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MemberRepository @Inject constructor(
    private val dao: MemberDao
) {
    fun getAllMembers(): Flow<List<MemberEntity>> = dao.getAllMembers()
    suspend fun insert(member: MemberEntity) = dao.insert(member)
    suspend fun update(member: MemberEntity) = dao.update(member)
    suspend fun delete(member: MemberEntity) = dao.delete(member)
}
