package com.clue2crew.app.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clue2crew.app.data.local.database.Clue2CrewDatabase
import com.clue2crew.app.data.local.entities.FamilyEntity
import com.clue2crew.app.data.local.entities.FamilyMemberEntity
import com.clue2crew.app.data.repository.FamilyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class FamilyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FamilyRepository
    private val prefs = application.getSharedPreferences("clue2crew_prefs", Context.MODE_PRIVATE)

    val family: StateFlow<FamilyEntity?>
    @OptIn(ExperimentalCoroutinesApi::class)
    val members: StateFlow<List<FamilyMemberEntity>>

    private val _currentDeviceLocation = MutableStateFlow<Location?>(null)
    val currentDeviceLocation: StateFlow<Location?> = _currentDeviceLocation.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
    }

    fun createFamily(name: String) {
        if (name.isBlank()) {
            _error.value = "Family name cannot be empty."
            return
        }

        val familyId = UUID.randomUUID().toString()
        val pairingCode = generatePairingCode()
        val myId = prefs.getString("my_member_id", "") ?: ""

        viewModelScope.launch {
            try {
                val newFamily = FamilyEntity(
                    familyId = familyId,
                    familyName = name.trim(),
                    pairingCode = pairingCode,
                    createdAt = System.currentTimeMillis()
                )
                repository.createFamily(newFamily)

                // Register current device as first member
                val me = FamilyMemberEntity(
                    memberId = myId,
                    familyId = familyId,
                    name = "Me",
                    deviceId = android.os.Build.MODEL,
                    status = "Connected",
                    isMe = true
                )
                repository.addMember(me)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to create family: ${e.localizedMessage}"
            }
        }
    }

    fun joinFamily(pairingCode: String) {
        val code = pairingCode.trim().uppercase()
        if (code.isBlank()) {
            _error.value = "Please enter a pairing code."
            return
        }

        val myId = prefs.getString("my_member_id", "") ?: ""
        viewModelScope.launch {
            try {
                val targetFamily = repository.getFamilyByPairingCode(code)
                if (targetFamily != null) {
                    // Check if already a member
                    val existingMember = repository.getMember(myId, targetFamily.familyId)
                    if (existingMember != null) {
                        _error.value = "You are already a member of this family."
                        return@launch
                    }

                    val me = FamilyMemberEntity(
                        memberId = myId,
                        familyId = targetFamily.familyId,
                        name = "Me (Joined)",
                        deviceId = android.os.Build.MODEL,
                        status = "Connected",
                        isMe = true
                    )
                    repository.addMember(me)
                    _error.value = null
                } else {
                    _error.value = "Invalid pairing code. Family not found."
                }
            } catch (e: Exception) {
                _error.value = "Failed to join family: ${e.localizedMessage}"
            }
        }
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
        updateLocation(myId, latitude, longitude)
    }

    fun updateCurrentDeviceLocation(location: Location) {
        _currentDeviceLocation.value = location
    }
}
