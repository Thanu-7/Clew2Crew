package com.clue2crew.app.data.local.dao

import androidx.room.*
import com.clue2crew.app.data.local.entities.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members WHERE familyId = :familyId")
    fun getMembersByFamilyId(familyId: String): Flow<List<FamilyMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMemberEntity)

    @Delete
    suspend fun deleteMember(member: FamilyMemberEntity)

    @Query("DELETE FROM family_members WHERE familyId = :familyId")
    suspend fun deleteMembersByFamilyId(familyId: String)

    @Query("UPDATE family_members SET latitude = :lat, longitude = :lng, status = :status, syncStatus = 'PENDING', lastUpdated = :lastUpdated WHERE memberId = :id")
    suspend fun updateMemberLocation(id: String, lat: Double?, lng: Double?, status: String, lastUpdated: Long = System.currentTimeMillis())

    @Query("SELECT * FROM family_members WHERE memberId = :memberId AND familyId = :familyId LIMIT 1")
    suspend fun getMember(memberId: String, familyId: String): FamilyMemberEntity?

    @Query("SELECT * FROM family_members WHERE syncStatus = 'PENDING' OR syncStatus = 'FAILED'")
    suspend fun getPendingMembers(): List<FamilyMemberEntity>

    @Query("UPDATE family_members SET syncStatus = :status WHERE memberId = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
