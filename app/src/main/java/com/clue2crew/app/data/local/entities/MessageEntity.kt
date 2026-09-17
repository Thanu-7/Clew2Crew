package com.clue2crew.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val familyId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val isMe: Boolean
)
