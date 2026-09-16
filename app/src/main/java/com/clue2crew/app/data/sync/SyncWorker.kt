package com.clue2crew.app.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.clue2crew.app.data.local.database.Clue2CrewDatabase
import com.clue2crew.app.data.local.entities.SyncStatus
import com.clue2crew.app.data.remote.RetrofitClient
import com.clue2crew.app.data.remote.dto.FamilyDto
import com.clue2crew.app.data.remote.dto.FamilyMemberDto

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = Clue2CrewDatabase.getDatabase(applicationContext)
        val familyDao = database.familyDao()
        val memberDao = database.familyMemberDao()
        val apiService = RetrofitClient.instance

        var hasError = false

        // Sync pending families
        val pendingFamilies = familyDao.getPendingFamilies()
        for (family in pendingFamilies) {
            try {
                val dto = FamilyDto(
                    familyId = family.familyId,
                    familyName = family.familyName,
                    pairingCode = family.pairingCode,
                    createdAt = family.createdAt,
                    lastUpdated = family.lastUpdated
                )
                val response = apiService.createFamily(dto) // Or update if it already exists on server
                if (response.isSuccessful) {
                    familyDao.updateSyncStatus(family.familyId, SyncStatus.SYNCED.name)
                } else {
                    familyDao.updateSyncStatus(family.familyId, SyncStatus.FAILED.name)
                    hasError = true
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Error syncing family ${family.familyId}", e)
                familyDao.updateSyncStatus(family.familyId, SyncStatus.FAILED.name)
                hasError = true
            }
        }

        // Sync pending members
        val pendingMembers = memberDao.getPendingMembers()
        for (member in pendingMembers) {
            try {
                val dto = FamilyMemberDto(
                    memberId = member.memberId,
                    familyId = member.familyId,
                    name = member.name,
                    deviceId = member.deviceId,
                    status = member.status,
                    latitude = member.latitude,
                    longitude = member.longitude,
                    isMe = member.isMe,
                    lastUpdated = member.lastUpdated
                )
                val response = apiService.addMember(dto)
                if (response.isSuccessful) {
                    memberDao.updateSyncStatus(member.memberId, SyncStatus.SYNCED.name)
                } else {
                    memberDao.updateSyncStatus(member.memberId, SyncStatus.FAILED.name)
                    hasError = true
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Error syncing member ${member.memberId}", e)
                memberDao.updateSyncStatus(member.memberId, SyncStatus.FAILED.name)
                hasError = true
            }
        }

        return if (hasError) Result.retry() else Result.success()
    }
}
