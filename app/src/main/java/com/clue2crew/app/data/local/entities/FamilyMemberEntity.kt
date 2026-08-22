package com.clue2crew.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey val memberId: String,
    val familyId: String,
    val name: String,
    val deviceId: String,
    val status: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isMe: Boolean = false
)
