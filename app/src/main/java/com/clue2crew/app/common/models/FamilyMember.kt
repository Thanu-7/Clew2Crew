package com.clue2crew.app.common.models

data class FamilyMember(
    val id: String,
    val name: String,
    val isConnected: Boolean,
    val isLive: Boolean,
    val distance: String,
    val lastUpdated: String,
    val direction: String = ""
)
