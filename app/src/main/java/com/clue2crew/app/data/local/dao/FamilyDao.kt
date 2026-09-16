package com.clue2crew.app.data.local.dao

import androidx.room.*
import com.clue2crew.app.data.local.entities.FamilyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {
    @Query("SELECT * FROM families LIMIT 1")
    fun getFamily(): Flow<FamilyEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamily(family: FamilyEntity)

    @Query("DELETE FROM families")
    suspend fun deleteAllFamilies()

    @Query("SELECT * FROM families WHERE pairingCode = :pairingCode LIMIT 1")
    suspend fun getFamilyByPairingCode(pairingCode: String): FamilyEntity?

    @Query("SELECT * FROM families WHERE syncStatus = 'PENDING' OR syncStatus = 'FAILED'")
    suspend fun getPendingFamilies(): List<FamilyEntity>

    @Query("UPDATE families SET syncStatus = :status WHERE familyId = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
