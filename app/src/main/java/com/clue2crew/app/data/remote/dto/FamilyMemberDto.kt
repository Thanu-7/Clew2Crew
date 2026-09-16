package com.clue2crew.app.data.remote.dto

data class FamilyMemberDto(
    val memberId: String,
    val familyId: String,
    val name: String,
    val deviceId: String,
    val status: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isMe: Boolean = false,
    val lastUpdated: Long
)
