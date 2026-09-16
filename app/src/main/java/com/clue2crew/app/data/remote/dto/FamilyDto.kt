package com.clue2crew.app.data.remote.dto

data class FamilyDto(
    val familyId: String,
    val familyName: String,
    val pairingCode: String,
    val createdAt: Long,
    val lastUpdated: Long
)
