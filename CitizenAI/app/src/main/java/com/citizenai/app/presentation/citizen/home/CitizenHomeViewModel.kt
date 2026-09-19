package com.citizenai.app.presentation.citizen.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.app.data.datastore.UserPreferencesDataStore
import com.citizenai.app.domain.model.Complaint
import com.citizenai.app.domain.usecase.complaint.GetMyComplaintsUseCase
import com.citizenai.app.util.LocationHelper
import com.citizenai.app.util.LocationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CitizenHomeUiState(
    val userName: String = "",
    val totalReports: Int = 0,
    val resolvedReports: Int = 0,
    val pendingReports: Int = 0,
    val recentComplaints: List<Complaint> = emptyList(),
    val currentLocationAddress: String = "Detecting location...",
    val isFetchingLocation: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class CitizenHomeViewModel @Inject constructor(
    private val getMyComplaintsUseCase: GetMyComplaintsUseCase,
    private val dataStore: UserPreferencesDataStore,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(CitizenHomeUiState())
    val uiState: StateFlow<CitizenHomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun retry() {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val name = dataStore.getUserName().firstOrNull() ?: "Citizen"

            getMyComplaintsUseCase().fold(
                onSuccess = { complaints ->
                    _uiState.value = _uiState.value.copy(
                        userName = name,
                        totalReports = complaints.size,
                        resolvedReports = complaints.count {
                            it.status == com.citizenai.app.domain.model.ComplaintStatus.RESOLVED ||
                            it.status == com.citizenai.app.domain.model.ComplaintStatus.COMPLETED
                        },
                        pendingReports = complaints.count {
                            it.status !in listOf(
                                com.citizenai.app.domain.model.ComplaintStatus.RESOLVED,
                                com.citizenai.app.domain.model.ComplaintStatus.COMPLETED,
                                com.citizenai.app.domain.model.ComplaintStatus.REJECTED,
                                com.citizenai.app.domain.model.ComplaintStatus.CANCELLED
                            )
                        },
                        recentComplaints = complaints.take(5),
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = err.message ?: "Failed to connect to server",
                        userName = name
                    )
                }
            )
        }
    }

    fun fetchLocation(context: Context) {
        if (!locationHelper.hasLocationPermission(context)) {
            _uiState.value = _uiState.value.copy(
                currentLocationAddress = "Tap to enable location access"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFetchingLocation = true)
            when (val result = locationHelper.getCurrentLocation(context)) {
                is LocationResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        currentLocationAddress = result.address,
                        isFetchingLocation = false
                    )
                }
                is LocationResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        currentLocationAddress = "Location unavailable. Tap to retry",
                        isFetchingLocation = false
                    )
                }
            }
        }
    }
}
