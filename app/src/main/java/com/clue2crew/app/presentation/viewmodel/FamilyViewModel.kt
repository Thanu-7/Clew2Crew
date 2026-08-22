package com.clue2crew.app.presentation.viewmodel

import android.app.Application
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

    val family: StateFlow<FamilyEntity?>
    @OptIn(ExperimentalCoroutinesApi::class)
    val members: StateFlow<List<FamilyMemberEntity>>

    private val _currentDeviceLocation = MutableStateFlow<Location?>(null)
    val currentDeviceLocation: StateFlow<Location?> = _currentDeviceLocation.asStateFlow()

    init {
        val database = Clue2CrewDatabase.getDatabase(application)
        repository = FamilyRepository(database.familyDao(), database.familyMemberDao())
        
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
        viewModelScope.launch {
            val newFamily = FamilyEntity(
                familyId = UUID.randomUUID().toString(),
                familyName = name,
                createdAt = System.currentTimeMillis()
            )
            repository.createFamily(newFamily)
        }
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
                longitude = longitude
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
        val firstMember = members.value.firstOrNull() ?: return
        updateLocation(firstMember.memberId, latitude, longitude)
    }

    fun updateCurrentDeviceLocation(location: Location) {
        _currentDeviceLocation.value = location
    }
}
