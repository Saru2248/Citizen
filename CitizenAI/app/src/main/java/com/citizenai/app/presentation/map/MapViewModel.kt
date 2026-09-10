package com.citizenai.app.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.app.domain.model.Complaint
import com.citizenai.app.domain.model.IssueCategory
import com.citizenai.app.domain.repository.ComplaintRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val complaints: List<Complaint> = emptyList(),
    val filteredComplaints: List<Complaint> = emptyList(),
    val selectedFilter: String = "All",
    val isLoading: Boolean = true,
    val selectedComplaint: Complaint? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val complaintRepository: ComplaintRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    val filterOptions = listOf("All", "Potholes", "Garbage", "Streetlight", "Water Leakage", "Drainage")

    init { loadComplaints() }

    private fun loadComplaints() {
        viewModelScope.launch {
            complaintRepository.getAllComplaintsForMap().fold(
                onSuccess = { complaints ->
                    _uiState.value = _uiState.value.copy(
                        complaints = complaints,
                        filteredComplaints = complaints,
                        isLoading = false
                    )
                },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false) }
            )
        }
    }

    fun onFilterSelected(filter: String) {
        val filtered = if (filter == "All") {
            _uiState.value.complaints
        } else {
            val cat = when (filter) {
                "Potholes" -> IssueCategory.POTHOLE
                "Garbage" -> IssueCategory.GARBAGE
                "Streetlight" -> IssueCategory.STREETLIGHT
                "Water Leakage" -> IssueCategory.WATER_LEAKAGE
                "Drainage" -> IssueCategory.DRAINAGE
                else -> null
            }
            if (cat != null) _uiState.value.complaints.filter { it.category == cat }
            else _uiState.value.complaints
        }
        _uiState.value = _uiState.value.copy(selectedFilter = filter, filteredComplaints = filtered)
    }

    fun onComplaintSelected(complaint: Complaint?) {
        _uiState.value = _uiState.value.copy(selectedComplaint = complaint)
    }
}
