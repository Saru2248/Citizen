package com.citizenai.worker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.worker.data.remote.SocketStatus
import com.citizenai.worker.data.remote.WorkerSocketManager
import com.citizenai.worker.domain.model.TaskStatus
import com.citizenai.worker.domain.model.WorkerTask
import com.citizenai.worker.domain.model.WorkerUser
import com.citizenai.worker.domain.repository.AuthRepository
import com.citizenai.worker.domain.repository.WorkerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val user: WorkerUser? = null,
    val tasks: List<WorkerTask> = emptyList(),
    val recentComplaints: List<WorkerTask> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val socketStatus: SocketStatus = SocketStatus.DISCONNECTED,
    // 7 Real MongoDB Metrics
    val totalLogins: Int = 0,
    val totalAssignedComplaints: Int = 0,
    val newComplaints: Int = 0,
    val pendingComplaints: Int = 0,
    val inProgressComplaints: Int = 0,
    val completedComplaints: Int = 0
)

@HiltViewModel
class WorkerDashboardViewModel @Inject constructor(
    private val workerRepository: WorkerRepository,
    private val authRepository: AuthRepository,
    private val socketManager: WorkerSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeUser()
        observeSocketStatus()
        observeTasksFlow()
        observeRealtimeEvents()
        refreshDashboardData()
    }

    private fun observeUser() {
        viewModelScope.launch {
            authRepository.workerUser.collectLatest { user ->
                _uiState.value = _uiState.value.copy(user = user)
                if (user != null) {
                    socketManager.connect(user.token, user.firebaseUid ?: user.id)
                }
            }
        }
    }

    private fun observeSocketStatus() {
        viewModelScope.launch {
            socketManager.status.collectLatest { status ->
                _uiState.value = _uiState.value.copy(socketStatus = status)
            }
        }
    }

    private fun observeTasksFlow() {
        viewModelScope.launch {
            workerRepository.getTasksFlow().collectLatest { tasks ->
                updateStats(tasks)
            }
        }
    }

    private fun observeRealtimeEvents() {
        viewModelScope.launch {
            socketManager.eventFlow.collectLatest { (event, data) ->
                // Real-time update trigger: refresh task list when complaint_updated or worker_task_assigned received
                if (event == "complaint_updated" || event == "worker_task_assigned" || event == "task_accepted") {
                    refreshDashboardData()
                }
            }
        }
    }

    fun refreshDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val dashboardRes = workerRepository.fetchWorkerDashboard()
            val tasksRes = workerRepository.fetchWorkerTasks()

            dashboardRes.fold(
                onSuccess = { d ->
                    val recent = (d.recentComplaints ?: emptyList()).map { mapDtoToTask(it) }
                    _uiState.value = _uiState.value.copy(
                        totalLogins = d.totalLogins,
                        totalAssignedComplaints = d.totalAssignedComplaints,
                        newComplaints = d.newComplaints,
                        pendingComplaints = d.pendingComplaints,
                        inProgressComplaints = d.inProgressComplaints,
                        completedComplaints = d.completedComplaints,
                        recentComplaints = recent
                    )
                },
                onFailure = { err ->
                    // Fallback to locally computed stats if dashboard endpoint offline
                    android.util.Log.w("WorkerDashboard", "Could not fetch dashboard endpoint: ${err.message}")
                }
            )

            tasksRes.fold(
                onSuccess = { tasks ->
                    _uiState.value = _uiState.value.copy(isLoading = false, tasks = tasks)
                    updateStats(tasks)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Failed to refresh dashboard data"
                    )
                }
            )
        }
    }

    private fun mapDtoToTask(dto: com.citizenai.worker.data.remote.dto.ComplaintDto): WorkerTask {
        val rawStatus = dto.status ?: "WORKER_ASSIGNED"
        val base = com.citizenai.worker.BuildConfig.API_BASE_URL.removeSuffix("/api/").removeSuffix("/")
        val resolveUrl = { u: String? ->
            if (u.isNullOrBlank()) null
            else if (u.startsWith("http://") || u.startsWith("https://")) u
            else "$base" + (if (u.startsWith("/")) u else "/$u")
        }
        return WorkerTask(
            id = dto.id ?: dto.complaintId ?: "",
            complaintId = dto.complaintId ?: dto.id ?: "",
            title = dto.title ?: dto.category ?: "Assigned Work",
            category = dto.category ?: "General",
            description = dto.description ?: "",
            priority = dto.priority ?: "MEDIUM",
            status = TaskStatus.fromString(rawStatus),
            statusRaw = rawStatus,
            progressPercentage = dto.progressPercentage ?: 0,
            address = dto.address,
            latitude = dto.latitude,
            longitude = dto.longitude,
            beforePhotoUrl = resolveUrl(dto.imageUrl),
            afterPhotoUrl = resolveUrl(dto.afterImageUrl ?: dto.completionPhotoUrl),
            workerNotes = dto.workerNotes,
            progressNote = dto.progressNote,
            department = dto.department,
            assignedWorkerId = dto.assignedWorkerId,
            assignedWorkerName = dto.assignedWorkerName,
            citizenName = dto.citizenName,
            citizenPhone = dto.citizenPhone,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            statusHistory = emptyList()
        )
    }

    private fun updateStats(tasks: List<WorkerTask>) {
        val current = _uiState.value
        // If backend dashboard returned 0 for totalAssignedComplaints or was not fetched, derive from tasks list
        val assigned = if (current.totalAssignedComplaints > 0) current.totalAssignedComplaints else tasks.size
        val newC = if (current.newComplaints > 0) current.newComplaints else tasks.count { it.status == TaskStatus.WORKER_ASSIGNED && it.progressPercentage == 0 }
        val pending = if (current.pendingComplaints > 0) current.pendingComplaints else tasks.count { it.status == TaskStatus.WORKER_ASSIGNED }
        val inProg = if (current.inProgressComplaints > 0) current.inProgressComplaints else tasks.count { it.status == TaskStatus.WORK_STARTED || it.status == TaskStatus.IN_PROGRESS }
        val comp = if (current.completedComplaints > 0) current.completedComplaints else tasks.count { it.status == TaskStatus.COMPLETED || it.status == TaskStatus.VERIFICATION_REQUIRED || it.status == TaskStatus.RESOLVED }

        _uiState.value = _uiState.value.copy(
            tasks = tasks,
            totalAssignedComplaints = assigned,
            newComplaints = newC,
            pendingComplaints = pending,
            inProgressComplaints = inProg,
            completedComplaints = comp
        )
    }
}
