package com.citizenai.app.presentation.admin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.app.data.datastore.UserPreferencesDataStore
import com.citizenai.app.domain.model.Complaint
import com.citizenai.app.domain.repository.AdminRepository
import com.citizenai.app.domain.repository.AdminStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminDashboardUiState(
    val adminName: String = "",
    val stats: AdminStats? = null,
    val recentHighPriority: List<Complaint> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val dataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val name = dataStore.getUserName().firstOrNull() ?: "Admin"

            // Load stats and recent complaints in parallel
            val statsResult = adminRepository.getAdminStats()
            val complaintsResult = adminRepository.getAllComplaints(
                priority = "CRITICAL,HIGH"
            )

            val stats = statsResult.getOrNull()
            val criticalComplaints = complaintsResult.getOrNull()
                ?.sortedByDescending { it.reportedAt }
                ?.take(5)
                ?: emptyList()

            val error = if (statsResult.isFailure && complaintsResult.isFailure) {
                statsResult.exceptionOrNull()?.message
            } else null

            _uiState.value = AdminDashboardUiState(
                adminName          = name,
                stats              = stats,
                recentHighPriority = criticalComplaints,
                isLoading          = false,
                error              = error
            )
        }
    }
}
