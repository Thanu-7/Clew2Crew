package com.clue2crew.app.data.repository

import com.clue2crew.app.data.local.dao.FamilyDao
import com.clue2crew.app.data.local.dao.FamilyMemberDao
import com.clue2crew.app.data.local.dao.MessageDao
import com.clue2crew.app.data.local.entities.FamilyEntity
import com.clue2crew.app.data.local.entities.FamilyMemberEntity
import com.clue2crew.app.data.local.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

class FamilyRepository(
    private val familyDao: FamilyDao,
    private val familyMemberDao: FamilyMemberDao,
    private val messageDao: MessageDao
) {
    val family: Flow<FamilyEntity?> = familyDao.getFamily()

    fun getMembers(familyId: String): Flow<List<FamilyMemberEntity>> = 
        familyMemberDao.getMembersByFamilyId(familyId)

    fun getMessages(familyId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesByFamily(familyId)

    suspend fun saveMessage(message: MessageEntity) {
        messageDao.insertMessage(message)
    }

    suspend fun createFamily(family: FamilyEntity) {
        familyDao.insertFamily(family)
    }

    suspend fun getFamilyByPairingCode(pairingCode: String): FamilyEntity? {
        return familyDao.getFamilyByPairingCode(pairingCode)
    }

    suspend fun addMember(member: FamilyMemberEntity) {
        familyMemberDao.insertMember(member)
    }

    suspend fun getMember(memberId: String, familyId: String): FamilyMemberEntity? {
        return familyMemberDao.getMember(memberId, familyId)
    }

    suspend fun removeMember(member: FamilyMemberEntity) {
        familyMemberDao.deleteMember(member)
    }

    suspend fun deleteFamily() {
        familyDao.deleteAllFamilies()
    }

    suspend fun updateMemberLocation(memberId: String, latitude: Double?, longitude: Double?, status: String) {
        familyMemberDao.updateMemberLocation(memberId, latitude, longitude, status)
    }
}
