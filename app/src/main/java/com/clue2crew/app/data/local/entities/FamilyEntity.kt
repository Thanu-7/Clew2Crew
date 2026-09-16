package com.clue2crew.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "families")
data class FamilyEntity(
    @PrimaryKey val familyId: String,
    val familyName: String,
    val pairingCode: String,
    val createdAt: Long
)
