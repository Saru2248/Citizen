package com.citizenai.app.presentation.complaints.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.app.domain.model.Complaint
import com.citizenai.app.domain.model.ComplaintStatus
import com.citizenai.app.domain.usecase.complaint.GetMyComplaintsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComplaintsListUiState(
    val allComplaints: List<Complaint> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedTab: Int = 0
)

@HiltViewModel
class ComplaintsListViewModel @Inject constructor(
    private val getMyComplaintsUseCase: GetMyComplaintsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComplaintsListUiState())
    val uiState: StateFlow<ComplaintsListUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getMyComplaintsUseCase().fold(
                onSuccess = { complaints ->
                    _uiState.value = _uiState.value.copy(allComplaints = complaints, isLoading = false)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = err.message)
                }
            )
        }
    }

    fun onTabSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun getFilteredComplaints(tabIndex: Int): List<Complaint> {
        val all = _uiState.value.allComplaints
        return when (tabIndex) {
            0 -> all
            1 -> all.filter { it.status == ComplaintStatus.PENDING || it.status == ComplaintStatus.ASSIGNED }
            2 -> all.filter { it.status == ComplaintStatus.IN_PROGRESS || it.status == ComplaintStatus.CITIZEN_VERIFICATION }
            3 -> all.filter { it.status == ComplaintStatus.RESOLVED || it.status == ComplaintStatus.COMPLETED }
            else -> all
        }
    }
}
