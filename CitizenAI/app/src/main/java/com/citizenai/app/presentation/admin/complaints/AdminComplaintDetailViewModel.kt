package com.citizenai.app.presentation.admin.complaints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.app.domain.model.Complaint
import com.citizenai.app.domain.model.ProgressUpdate
import com.citizenai.app.domain.model.TimelineEvent
import com.citizenai.app.domain.model.WorkerMonitor
import com.citizenai.app.domain.model.buildTimeline
import com.citizenai.app.domain.repository.AdminRepository
import com.citizenai.app.domain.repository.ComplaintRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminComplaintDetailUiState(
    val complaint: Complaint? = null,
    val timeline: List<TimelineEvent> = emptyList(),
    val progressUpdates: List<ProgressUpdate> = emptyList(),
    val availableWorkers: List<WorkerMonitor> = emptyList(),
    val isLoading: Boolean = true,
    val isAssigning: Boolean = false,
    val showWorkerDialog: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AdminComplaintDetailViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val complaintRepository: ComplaintRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminComplaintDetailUiState())
    val uiState: StateFlow<AdminComplaintDetailUiState> = _uiState.asStateFlow()

    fun loadComplaint(complaintId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Load complaint + timeline + progress in parallel
            val complaintResult  = complaintRepository.getComplaintById(complaintId)
            val timelineResult   = complaintRepository.getComplaintTimeline(complaintId)
            val progressResult   = adminRepository.getProgressUpdates(complaintId)

            val complaint = complaintResult.getOrNull()
            _uiState.value = AdminComplaintDetailUiState(
                complaint       = complaint,
                timeline        = timelineResult.getOrElse {
                    complaint?.status?.let { buildTimeline(it) } ?: emptyList()
                },
                progressUpdates = progressResult.getOrElse { emptyList() },
                isLoading       = false,
                error           = if (complaintResult.isFailure) complaintResult.exceptionOrNull()?.message else null
            )
        }
    }

    fun showWorkerAssignment() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showWorkerDialog = true)
            // Load available workers
            adminRepository.getAllWorkers(availability = "AVAILABLE,BUSY").fold(
                onSuccess = { workers ->
                    _uiState.value = _uiState.value.copy(availableWorkers = workers)
                },
                onFailure = {}
            )
        }
    }

    fun hideWorkerAssignment() {
        _uiState.value = _uiState.value.copy(showWorkerDialog = false)
    }

    fun assignWorker(
        workerId: String,
        deadline: String? = null,
        priority: String? = null
    ) {
        val complaintId = _uiState.value.complaint?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAssigning = true, showWorkerDialog = false)
            adminRepository.assignWorker(
                complaintId = complaintId,
                workerId    = workerId,
                deadline    = deadline,
                priority    = priority
            ).fold(
                onSuccess = { updatedComplaint ->
                    _uiState.value = _uiState.value.copy(
                        complaint      = updatedComplaint,
                        isAssigning    = false,
                        successMessage = "Worker assigned successfully"
                    )
                    // Reload timeline
                    loadComplaint(complaintId)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isAssigning = false,
                        error       = "Assignment failed: ${err.message}"
                    )
                }
            )
        }
    }

    fun changePriority(priority: String) {
        val complaintId = _uiState.value.complaint?.id ?: return
        viewModelScope.launch {
            adminRepository.updatePriority(complaintId, priority).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(successMessage = "Priority updated")
                    loadComplaint(complaintId)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(error = err.message)
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}
