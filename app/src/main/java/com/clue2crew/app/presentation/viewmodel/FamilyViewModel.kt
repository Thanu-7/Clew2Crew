package com.clue2crew.app.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clue2crew.app.data.bluetooth.BleManager
import com.clue2crew.app.data.bluetooth.DiscoveredDevice
import com.clue2crew.app.data.local.database.Clue2CrewDatabase
import com.clue2crew.app.data.local.entities.FamilyEntity
import com.clue2crew.app.data.local.entities.FamilyMemberEntity
import com.clue2crew.app.data.repository.FamilyRepository
import com.clue2crew.app.data.security.AuthProtocol
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class FamilyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FamilyRepository
    private val prefs = application.getSharedPreferences("clue2crew_prefs", Context.MODE_PRIVATE)

    private val bleManager = BleManager(application)

    val family: StateFlow<FamilyEntity?>
    @OptIn(ExperimentalCoroutinesApi::class)
    val members: StateFlow<List<FamilyMemberEntity>>

    // BLE Flows
    val isBleAdvertising: StateFlow<Boolean> = bleManager.isAdvertising
    val isBleScanning: StateFlow<Boolean> = bleManager.isScanning
    val discoveredBleDevices: StateFlow<List<DiscoveredDevice>> = bleManager.discoveredDevices
    val bleConnectionState: StateFlow<String> = bleManager.connectionState
    val lastBleMessage: StateFlow<String?> = bleManager.lastReceivedMessage
    val bleStatusMessage: StateFlow<String> = bleManager.statusMessage
    val isBleAuthenticated: StateFlow<Boolean> = bleManager.isAuthenticated
    val authenticatedMemberId: StateFlow<String?> = bleManager.authenticatedMemberId
    val incomingBleLocation: StateFlow<AuthProtocol.LocationData?> = bleManager.incomingLocation

    private val _currentDeviceLocation = MutableStateFlow<Location?>(null)
    val currentDeviceLocation: StateFlow<Location?> = _currentDeviceLocation.asStateFlow()

    private var lastKnownLocation: Location? = null

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _isJoiningInProgress = MutableStateFlow(false)
    val isJoiningInProgress: StateFlow<Boolean> = _isJoiningInProgress.asStateFlow()

    private val _userName = MutableStateFlow(prefs.getString("my_user_name", "Me") ?: "Me")
    val userName: StateFlow<String> = _userName.asStateFlow()

    init {
        val database = Clue2CrewDatabase.getDatabase(application)
        repository = FamilyRepository(database.familyDao(), database.familyMemberDao())

        // Ensure stable device identity
        if (prefs.getString("my_member_id", null) == null) {
            prefs.edit().putString("my_member_id", UUID.randomUUID().toString()).apply()
        }
        
        family = repository.family.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        @OptIn(ExperimentalCoroutinesApi::class)
        members = family.flatMapLatest { f ->
            if (f != null) {
                repository.getMembers(f.familyId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Monitor incoming BLE location updates
        viewModelScope.launch {
            bleManager.incomingLocation.collect { locData ->
                locData?.let {
                    updateLocation(it.memberId, it.latitude, it.longitude, "Connected")
                }
            }
        }

        // Periodic location broadcasting over BLE when authenticated
        viewModelScope.launch {
            while (true) {
                if (bleManager.isAuthenticated.value) {
                    lastKnownLocation?.let { loc ->
                        bleManager.sendLocation(loc.latitude, loc.longitude, loc.accuracy)
                    }
                }
                delay(3000) // Broadcast location every 3 seconds during active session
            }
        }

        // Handle successful BLE pairing/joining
        viewModelScope.launch {
            bleManager.pairedFamily.collect { authResult ->
                if (authResult != null && family.value == null) {
                    val myId = prefs.getString("my_member_id", "") ?: ""
                    val savedUserName = prefs.getString("my_user_name", "Me") ?: "Me"
                    try {
                        val newFamily = FamilyEntity(
                            familyId = authResult.familyId,
                            familyName = authResult.familyName,
                            pairingCode = bleManager.isScanning.value.let { if (it) prefs.getString("temp_pairing_code", "") ?: "" else "" },
                            createdAt = System.currentTimeMillis()
                        )
                        repository.createFamily(newFamily)

                        val me = FamilyMemberEntity(
                            memberId = myId,
                            familyId = authResult.familyId,
                            name = savedUserName,
                            deviceId = android.os.Build.MODEL,
                            status = "Connected",
                            latitude = lastKnownLocation?.latitude,
                            longitude = lastKnownLocation?.longitude,
                            isMe = true
                        )
                        repository.addMember(me)
                        
                        _isJoiningInProgress.value = false
                        _error.value = null
                    } catch (e: Exception) {
                        Log.e("FamilyViewModel", "Failed to save paired family", e)
                    }
                }
            }
        }

        // Automatically start BLE services when family is present
        viewModelScope.launch {
            family.collect { f ->
                if (f != null) {
                    startBleAdvertising()
                    startBleScan(60000) // Scan for 1 minute
                } else {
                    stopBleAdvertising()
                    stopBleScan()
                }
            }
        }

        // Auto-connect to discovered Clew2Crew devices
        viewModelScope.launch {
            discoveredBleDevices.collect { devices ->
                if (family.value != null && !isBleAuthenticated.value && bleConnectionState.value == "Disconnected") {
                    val target = devices.firstOrNull() // Try the first one found
                    target?.let { 
                        connectBleDevice(it.address)
                    }
                }
            }
        }
    }

    fun createFamily(familyName: String, userNameInput: String = "") {
        val trimmedFamilyName = familyName.trim()
        if (trimmedFamilyName.isBlank()) {
            _error.value = "Family name cannot be empty."
            return
        }

        val cleanUserName = if (userNameInput.isBlank()) {
            prefs.getString("my_user_name", "Me") ?: "Me"
        } else {
            userNameInput.trim()
        }

        prefs.edit().putString("my_user_name", cleanUserName).apply()
        _userName.value = cleanUserName

        val familyId = UUID.randomUUID().toString()
        val pairingCode = generatePairingCode()
        val myId = prefs.getString("my_member_id", "") ?: ""

        viewModelScope.launch {
            try {
                val newFamily = FamilyEntity(
                    familyId = familyId,
                    familyName = trimmedFamilyName,
                    pairingCode = pairingCode,
                    createdAt = System.currentTimeMillis()
                )
                repository.createFamily(newFamily)

                // Register current device as first member
                val me = FamilyMemberEntity(
                    memberId = myId,
                    familyId = familyId,
                    name = cleanUserName,
                    deviceId = android.os.Build.MODEL,
                    status = "Connected",
                    latitude = lastKnownLocation?.latitude,
                    longitude = lastKnownLocation?.longitude,
                    isMe = true
                )
                repository.addMember(me)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to create family: ${e.localizedMessage}"
            }
        }
    }

    fun initiateJoinProcess(pairingCode: String, userNameInput: String = "") {
        val code = pairingCode.trim().uppercase()
        if (code.isBlank()) {
            _error.value = "Please enter a pairing code."
            return
        }

        val cleanUserName = if (userNameInput.isBlank()) {
            prefs.getString("my_user_name", "Me") ?: "Me"
        } else {
            userNameInput.trim()
        }

        prefs.edit().putString("my_user_name", cleanUserName).apply()
        _userName.value = cleanUserName

        val myId = prefs.getString("my_member_id", "") ?: ""
        
        _isJoiningInProgress.value = true
        _statusMessage.value = "Searching for family with code $code..."
        prefs.edit().putString("temp_pairing_code", code).apply()
        
        bleManager.setCredentials(code, myId)
        startBleScan(30000) // Scan for 30 seconds
    }

    private fun generatePairingCode(): String {
        // Human-friendly characters: Avoid 0, O, I, 1, L
        val allowedChars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        return (1..6)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun clearError() {
        _error.value = null
    }

    fun addMember(name: String, latitude: Double? = null, longitude: Double? = null) {
        val currentFamilyId = family.value?.familyId ?: return
        viewModelScope.launch {
            val newMember = FamilyMemberEntity(
                memberId = UUID.randomUUID().toString(),
                familyId = currentFamilyId,
                name = name,
                deviceId = "MOCK_DEVICE_${UUID.randomUUID().toString().take(4)}",
                status = "Connected",
                latitude = latitude,
                longitude = longitude,
                isMe = false
            )
            repository.addMember(newMember)
        }
    }

    fun removeMember(member: FamilyMemberEntity) {
        viewModelScope.launch {
            repository.removeMember(member)
        }
    }

    fun deleteFamily() {
        viewModelScope.launch {
            repository.deleteFamily()
        }
    }

    fun updateLocation(memberId: String, latitude: Double?, longitude: Double?, status: String = "Connected") {
        viewModelScope.launch {
            repository.updateMemberLocation(memberId, latitude, longitude, status)
        }
    }

    fun updateFirstMemberLocation(latitude: Double, longitude: Double) {
        val myId = prefs.getString("my_member_id", "") ?: ""
        if (family.value != null) {
            updateLocation(myId, latitude, longitude)
        }
    }

    fun updateCurrentDeviceLocation(location: Location) {
        _currentDeviceLocation.value = location
        lastKnownLocation = location
    }

    fun toggleDemoMode() {
        _isDemoMode.value = !_isDemoMode.value
        if (_isDemoMode.value) {
            startDemoUpdates()
        }
    }

    private fun startDemoUpdates() {
        viewModelScope.launch {
            while (_isDemoMode.value) {
                val currentFam = family.value ?: break
                val otherMembers = members.value.filter { !it.isMe }
                
                if (otherMembers.isEmpty()) {
                    // Add a mock member if none exists
                    addMember("Demo Member", 12.9716, 77.5946)
                }

                otherMembers.forEach { member ->
                    // Simulate slight movement
                    val newLat = (member.latitude ?: 12.9716) + (Math.random() - 0.5) * 0.001
                    val newLon = (member.longitude ?: 77.5946) + (Math.random() - 0.5) * 0.001
                    updateLocation(member.memberId, newLat, newLon, "Connected")
                }
                delay(3000)
            }
        }
    }

    // BLE Delegations
    fun startBleAdvertising() {
        val f = family.value
        val pairingCode = f?.pairingCode ?: "DEFAULT"
        val myId = prefs.getString("my_member_id", "") ?: ""
        bleManager.setCredentials(pairingCode, myId, f?.familyId ?: "", f?.familyName ?: "")
        bleManager.startAdvertising()
    }

    fun stopBleAdvertising() = bleManager.stopAdvertising()
    fun startBleScan(durationMs: Long = 10000L) = bleManager.startScan(durationMs)
    fun stopBleScan() = bleManager.stopScan()

    fun connectBleDevice(address: String) {
        val pairingCode = family.value?.pairingCode ?: "DEFAULT"
        val myId = prefs.getString("my_member_id", "") ?: ""
        bleManager.connectToDevice(address, pairingCode, myId)
    }

    fun disconnectBle() = bleManager.disconnectGatt()

    fun sendEmergencyAlert() = bleManager.sendEmergencyAlert()

    override fun onCleared() {
        super.onCleared()
        bleManager.cleanup()
    }
}
