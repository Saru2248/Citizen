package com.citizenai.app.presentation.admin.complaints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.app.domain.model.Complaint
import com.citizenai.app.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminComplaintListUiState(
    val complaints: List<Complaint> = emptyList(),
    val filteredComplaints: List<Complaint> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedStatusFilter: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class AdminComplaintListViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val firestoreService: com.citizenai.app.data.remote.firebase.FirestoreService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminComplaintListUiState())
    val uiState: StateFlow<AdminComplaintListUiState> = _uiState.asStateFlow()

    init {
        observeLiveComplaints()
    }

    private fun observeLiveComplaints() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            firestoreService.observeComplaints().collect { complaints ->
                val statusFilter = _uiState.value.selectedStatusFilter
                val filteredByStatus = if (!statusFilter.isNullOrBlank()) {
                    complaints.filter { it.status.name.equals(statusFilter, ignoreCase = true) }
                } else complaints

                _uiState.value = _uiState.value.copy(
                    complaints = complaints,
                    filteredComplaints = applyLocalFilter(filteredByStatus, _uiState.value.searchQuery),
                    isLoading = false
                )
            }
        }
    }

    fun load(statusFilter: String? = null) {
        observeLiveComplaints()
    }


    fun onStatusFilterChanged(status: String?) {
        _uiState.value = _uiState.value.copy(selectedStatusFilter = status)
        load(status)
    }

    fun onSearchQueryChanged(query: String) {
        val filtered = applyLocalFilter(_uiState.value.complaints, query)
        _uiState.value = _uiState.value.copy(
            searchQuery        = query,
            filteredComplaints = filtered
        )
    }

    private fun applyLocalFilter(complaints: List<Complaint>, query: String): List<Complaint> {
        if (query.isBlank()) return complaints
        val q = query.lowercase()
        return complaints.filter {
            it.complaintId.lowercase().contains(q) ||
            it.issueType.lowercase().contains(q) ||
            it.address.lowercase().contains(q) ||
            it.citizenName.lowercase().contains(q) ||
            it.department.lowercase().contains(q)
        }
    }
}
