package com.clue2crew.app.data.repository

import android.content.Context
import androidx.work.*
import com.clue2crew.app.data.local.dao.FamilyDao
import com.clue2crew.app.data.local.dao.FamilyMemberDao
import com.clue2crew.app.data.local.entities.FamilyEntity
import com.clue2crew.app.data.local.entities.FamilyMemberEntity
import com.clue2crew.app.data.sync.SyncWorker
import kotlinx.coroutines.flow.Flow

class FamilyRepository(
    private val familyDao: FamilyDao,
    private val familyMemberDao: FamilyMemberDao,
    private val context: Context
) {
    val family: Flow<FamilyEntity?> = familyDao.getFamily()

    fun getMembers(familyId: String): Flow<List<FamilyMemberEntity>> = 
        familyMemberDao.getMembersByFamilyId(familyId)

    suspend fun createFamily(family: FamilyEntity) {
        familyDao.insertFamily(family)
        scheduleSync()
    }

    suspend fun getFamilyByPairingCode(pairingCode: String): FamilyEntity? {
        return familyDao.getFamilyByPairingCode(pairingCode)
    }

    suspend fun addMember(member: FamilyMemberEntity) {
        familyMemberDao.insertMember(member)
        scheduleSync()
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
        scheduleSync()
    }

    fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "FamilySync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
