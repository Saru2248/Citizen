package com.citizenai.worker.presentation.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.worker.domain.model.TaskStatus
import com.citizenai.worker.domain.model.WorkerTask
import com.citizenai.worker.domain.repository.WorkerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class TaskDetailUiState(
    val taskId: String = "",
    val task: WorkerTask? = null,
    val isLoading: Boolean = false,
    val isActionProcessing: Boolean = false,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null,
    val showProgressDialog: Boolean = false,
    val showCompleteDialog: Boolean = false
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val workerRepository: WorkerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String = savedStateHandle.get<String>("taskId") ?: ""

    private val _uiState = MutableStateFlow(TaskDetailUiState(taskId = taskId))
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        if (taskId.isNotEmpty()) {
            loadTaskDetails()
        }
    }

    fun loadTaskDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val res = workerRepository.getTaskById(taskId)
            res.fold(
                onSuccess = { task ->
                    _uiState.value = _uiState.value.copy(isLoading = false, task = task)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Failed to load task details"
                    )
                }
            )
        }
    }

    fun acceptTask() {
        if (_uiState.value.isActionProcessing) return // Duplicate submission protection
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionProcessing = true, errorMessage = null)
            val res = workerRepository.acceptTask(taskId)
            res.fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(
                        isActionProcessing = false,
                        task = updated,
                        actionSuccessMessage = "Task accepted successfully"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isActionProcessing = false,
                        errorMessage = err.message ?: "Failed to accept task"
                    )
                }
            )
        }
    }

    fun startWork(beforeImageFile: File? = null) {
        if (_uiState.value.isActionProcessing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionProcessing = true, errorMessage = null)
            val res = workerRepository.startWork(taskId, beforeImageFile)
            res.fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(
                        isActionProcessing = false,
                        task = updated,
                        actionSuccessMessage = "Work started"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isActionProcessing = false,
                        errorMessage = err.message ?: "Failed to start work"
                    )
                }
            )
        }
    }

    fun openProgressDialog() {
        _uiState.value = _uiState.value.copy(showProgressDialog = true)
    }

    fun closeProgressDialog() {
        _uiState.value = _uiState.value.copy(showProgressDialog = false)
    }

    fun submitProgress(progressPercentage: Int, note: String, photoFile: File?) {
        if (_uiState.value.isActionProcessing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionProcessing = true, errorMessage = null, showProgressDialog = false)
            val res = workerRepository.submitProgress(taskId, progressPercentage, note, photoFile)
            res.fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(
                        isActionProcessing = false,
                        task = updated,
                        actionSuccessMessage = "Progress updated to $progressPercentage%"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isActionProcessing = false,
                        errorMessage = err.message ?: "Failed to update progress"
                    )
                }
            )
        }
    }

    fun openCompleteDialog() {
        _uiState.value = _uiState.value.copy(showCompleteDialog = true)
    }

    fun closeCompleteDialog() {
        _uiState.value = _uiState.value.copy(showCompleteDialog = false)
    }

    fun completeTask(notes: String, afterImageFile: File) {
        if (_uiState.value.isActionProcessing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionProcessing = true, errorMessage = null, showCompleteDialog = false)
            val res = workerRepository.completeTask(taskId, notes, afterImageFile)
            res.fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(
                        isActionProcessing = false,
                        task = updated,
                        actionSuccessMessage = "Task marked as Completed!"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isActionProcessing = false,
                        errorMessage = err.message ?: "Failed to complete task"
                    )
                }
            )
        }
    }
}
