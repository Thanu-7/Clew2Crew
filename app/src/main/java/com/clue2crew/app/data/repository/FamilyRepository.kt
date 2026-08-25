package com.clue2crew.app.data.repository

import com.clue2crew.app.data.local.dao.FamilyDao
import com.clue2crew.app.data.local.dao.FamilyMemberDao
import com.clue2crew.app.data.local.entities.FamilyEntity
import com.clue2crew.app.data.local.entities.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

class FamilyRepository(
    private val familyDao: FamilyDao,
    private val familyMemberDao: FamilyMemberDao
) {
    val family: Flow<FamilyEntity?> = familyDao.getFamily()

    fun getMembers(familyId: String): Flow<List<FamilyMemberEntity>> = 
        familyMemberDao.getMembersByFamilyId(familyId)

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
        // Optionally delete all members too, though cascading or manual cleanup is better
    }

    suspend fun updateMemberLocation(memberId: String, latitude: Double?, longitude: Double?, status: String) {
        familyMemberDao.updateMemberLocation(memberId, latitude, longitude, status)
    }
}
