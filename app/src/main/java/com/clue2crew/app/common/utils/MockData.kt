package com.clue2crew.app.common.utils

import com.clue2crew.app.common.models.FamilyMember
import com.clue2crew.app.common.models.FamilyGroup

object MockData {
    val mom = FamilyMember(
        id = "1",
        name = "Mom",
        isConnected = true,
        isLive = true,
        distance = "250 m",
        lastUpdated = "10:32 AM",
        direction = "NORTH-EAST"
    )

    val dad = FamilyMember(
        id = "2",
        name = "Dad",
        isConnected = false,
        isLive = false,
        distance = "1.2 km",
        lastUpdated = "10:25 AM"
    )

    val sister = FamilyMember(
        id = "3",
        name = "Sister",
        isConnected = true,
        isLive = true,
        distance = "500 m",
        lastUpdated = "10:31 AM"
    )

    val familyGroup = FamilyGroup(
        name = "My Family Group",
        members = listOf(mom, dad, sister)
    )
}
