package com.citizenai.app.presentation.complaints.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.app.data.remote.websocket.RealTimeEvent
import com.citizenai.app.data.remote.websocket.SocketIOManager
import com.citizenai.app.domain.model.*
import com.citizenai.app.domain.usecase.complaint.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ComplaintDetailVM"

data class ComplaintDetailUiState(
    val complaint: Complaint? = null,
    val timeline: List<TimelineEvent> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val commentText: String = "",
    val isPostingComment: Boolean = false,
    val isVerifying: Boolean = false,
    val verifySuccess: Boolean = false
)

@HiltViewModel
class ComplaintDetailViewModel @Inject constructor(
    private val getComplaintDetailUseCase: GetComplaintDetailUseCase,
    private val verifyResolutionUseCase: VerifyResolutionUseCase,
    private val complaintRepository: com.citizenai.app.domain.repository.ComplaintRepository,
    private val firestoreService: com.citizenai.app.data.remote.firebase.FirestoreService,
    private val socketIOManager: SocketIOManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComplaintDetailUiState())
    val uiState: StateFlow<ComplaintDetailUiState> = _uiState.asStateFlow()

    private var targetComplaintId: String? = null

    fun loadComplaint(complaintId: String) {
        targetComplaintId = complaintId
        socketIOManager.connect()

        viewModelScope.launch {
            socketIOManager.events.collect { event ->
                when (event) {
                    is RealTimeEvent.ComplaintUpdated -> {
                        if (isTargetComplaint(event.complaintId)) {
                            Log.d(TAG, "[Socket.IO Citizen] complaint_updated received")
                            Log.d(TAG, "[Socket.IO Citizen] Complaint ID: ${event.complaintId}")
                            Log.d(TAG, "[Socket.IO Citizen] Status: ${event.newStatus}")
                            refreshComplaintData(complaintId)
                        }
                    }
                    is RealTimeEvent.WorkerAssigned -> {
                        if (isTargetComplaint(event.complaintId)) {
                            Log.d(TAG, "[Socket.IO Citizen] worker_assigned received")
                            Log.d(TAG, "[Socket.IO Citizen] Complaint ID: ${event.complaintId}")
                            refreshComplaintData(complaintId)
                        }
                    }
                    else -> {}
                }
            }
        }

        refreshComplaintData(complaintId)
    }

    private fun isTargetComplaint(id: String): Boolean {
        if (id.isBlank()) return true
        val current = _uiState.value.complaint
        val target = targetComplaintId
        return id == target || id == current?.id || id == current?.complaintId
    }

    private fun refreshComplaintData(complaintId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = _uiState.value.complaint == null)

            getComplaintDetailUseCase(complaintId).fold(
                onSuccess = { complaint ->
                    _uiState.value = _uiState.value.copy(
                        complaint = complaint,
                        isLoading = false
                    )
                    loadTimeline(complaintId, complaint.status)
                    loadComments(complaintId)
                },
                onFailure = { err ->
                    if (_uiState.value.complaint == null) {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = err.message)
                    }
                }
            )
        }
    }

    private suspend fun loadTimeline(id: String, fallbackStatus: ComplaintStatus) {
        complaintRepository.getComplaintTimeline(id).fold(
            onSuccess = { historyEvents ->
                val fullTimeline = buildTimeline(fallbackStatus).map { stage ->
                    val matched = historyEvents.lastOrNull { h ->
                        h.status == stage.status || stageOrdinal(h.status) == stageOrdinal(stage.status)
                    }
                    if (matched?.timestamp != null) {
                        stage.copy(
                            timestamp = matched.timestamp,
                            description = if (matched.description.isNotBlank()) matched.description else stage.description
                        )
                    } else {
                        stage
                    }
                }
                _uiState.value = _uiState.value.copy(timeline = fullTimeline)
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(timeline = buildTimeline(fallbackStatus))
            }
        )
    }

    private suspend fun loadComments(id: String) {
        complaintRepository.getComments(id).fold(
            onSuccess = { comments -> _uiState.value = _uiState.value.copy(comments = comments) },
            onFailure = {}
        )
    }

    fun onCommentChange(v: String) { _uiState.value = _uiState.value.copy(commentText = v) }

    fun postComment() {
        val state = _uiState.value
        if (state.commentText.isBlank() || state.complaint == null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPostingComment = true)
            complaintRepository.postComment(state.complaint.id, state.commentText).fold(
                onSuccess = { comment ->
                    _uiState.value = _uiState.value.copy(
                        comments = _uiState.value.comments + comment,
                        commentText = "",
                        isPostingComment = false
                    )
                },
                onFailure = { _uiState.value = _uiState.value.copy(isPostingComment = false) }
            )
        }
    }

    fun verifyResolution(isResolved: Boolean, reason: String? = null) {
        val complaint = _uiState.value.complaint ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifying = true)
            verifyResolutionUseCase(complaint.id, isResolved, reason).fold(
                onSuccess = {
                    val updatedStatus = if (isResolved) ComplaintStatus.RESOLVED else ComplaintStatus.REOPENED
                    val updatedComplaint = complaint.copy(status = updatedStatus)
                    _uiState.value = _uiState.value.copy(
                        complaint = updatedComplaint,
                        timeline = buildTimeline(updatedStatus),
                        isVerifying = false,
                        verifySuccess = true
                    )
                },
                onFailure = { _uiState.value = _uiState.value.copy(isVerifying = false) }
            )
        }
    }
}
