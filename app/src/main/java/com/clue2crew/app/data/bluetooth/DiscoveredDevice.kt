package com.clue2crew.app.data.bluetooth

data class DiscoveredDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val connectionState: String = "Disconnected",
    val lastMessage: String? = null
)
