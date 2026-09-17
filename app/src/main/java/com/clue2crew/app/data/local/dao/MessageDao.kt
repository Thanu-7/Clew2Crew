package com.clue2crew.app.data.local.dao

import androidx.room.*
import com.clue2crew.app.data.local.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE familyId = :familyId ORDER BY timestamp ASC")
    fun getMessagesByFamily(familyId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE familyId = :familyId")
    suspend fun deleteMessagesByFamily(familyId: String)
}
