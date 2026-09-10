package com.citizenai.app.presentation.worker.task

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.app.domain.model.ProgressUpdate
import com.citizenai.app.domain.model.TaskStatus
import com.citizenai.app.domain.model.WorkerTask
import com.citizenai.app.domain.usecase.worker.*
import com.citizenai.app.domain.repository.WorkerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class WorkerTaskUiState(
    val task: WorkerTask? = null,
    val progressUpdates: List<ProgressUpdate> = emptyList(),
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val isCompleting: Boolean = false,
    val isSubmittingProgress: Boolean = false,
    val showRejectDialog: Boolean = false,
    val showProgressDialog: Boolean = false,
    val afterImageUri: Uri? = null,
    val afterImageFile: File? = null,
    val progressImageUri: Uri? = null,
    val progressImageFile: File? = null,
    val workNotes: String = "",
    val progressNote: String = "",
    val progressPercentage: Int = 50,
    val error: String? = null,
    val isCompleted: Boolean = false
)

@HiltViewModel
class WorkerTaskViewModel @Inject constructor(
    private val updateTaskStatusUseCase: UpdateTaskStatusUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val workerRepository: WorkerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerTaskUiState())
    val uiState: StateFlow<WorkerTaskUiState> = _uiState.asStateFlow()

    fun loadTask(taskId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            workerRepository.getTaskById(taskId).fold(
                onSuccess = { task ->
                    // Load progress updates for complaint
                    val progressResult = workerRepository.getProgressUpdates(task.complaintId)
                    _uiState.value = _uiState.value.copy(
                        task            = task,
                        progressUpdates = progressResult.getOrElse { emptyList() },
                        isLoading       = false
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = err.message)
                }
            )
        }
    }

    fun acceptTask() {
        val taskId = _uiState.value.task?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true)
            workerRepository.acceptTask(taskId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        task       = _uiState.value.task?.copy(status = TaskStatus.ACCEPTED)
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(isUpdating = false, error = err.message)
                }
            )
        }
    }

    fun showRejectDialog() {
        _uiState.value = _uiState.value.copy(showRejectDialog = true)
    }

    fun hideRejectDialog() {
        _uiState.value = _uiState.value.copy(showRejectDialog = false)
    }

    fun rejectTask(reason: String) {
        val taskId = _uiState.value.task?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true, showRejectDialog = false)
            workerRepository.rejectTask(taskId, reason).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isUpdating  = false,
                        isCompleted = true // Navigate back on rejection
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(isUpdating = false, error = err.message)
                }
            )
        }
    }

    fun startTask() {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true)
            updateTaskStatusUseCase(task.id, TaskStatus.IN_PROGRESS).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        task       = task.copy(status = TaskStatus.IN_PROGRESS)
                    )
                },
                onFailure = { err -> _uiState.value = _uiState.value.copy(isUpdating = false, error = err.message) }
            )
        }
    }

    fun showProgressDialog() {
        _uiState.value = _uiState.value.copy(showProgressDialog = true)
    }

    fun hideProgressDialog() {
        _uiState.value = _uiState.value.copy(showProgressDialog = false)
    }

    fun onProgressPercentageChanged(percentage: Int) {
        _uiState.value = _uiState.value.copy(progressPercentage = percentage)
    }

    fun onProgressNoteChanged(note: String) {
        _uiState.value = _uiState.value.copy(progressNote = note)
    }

    fun onProgressImageSelected(uri: Uri, file: File?) {
        _uiState.value = _uiState.value.copy(progressImageUri = uri, progressImageFile = file)
    }

    fun submitProgressUpdate() {
        val taskId = _uiState.value.task?.id ?: return
        val percentage = _uiState.value.progressPercentage
        val note = _uiState.value.progressNote
        val file = _uiState.value.progressImageFile

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingProgress = true, error = null)
            workerRepository.submitProgress(
                taskId     = taskId,
                percentage = percentage,
                note       = note,
                photoFile  = file,
                latitude   = _uiState.value.task?.latitude,
                longitude  = _uiState.value.task?.longitude
            ).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingProgress = false,
                        showProgressDialog   = false,
                        progressNote         = "",
                        progressImageUri     = null,
                        progressImageFile    = null,
                        task                 = _uiState.value.task?.copy(progressPercentage = percentage)
                    )
                    // Reload updates
                    loadTask(taskId)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(isSubmittingProgress = false, error = err.message)
                }
            )
        }
    }

    fun onAfterImageSelected(uri: Uri, file: File?) {
        _uiState.value = _uiState.value.copy(afterImageUri = uri, afterImageFile = file)
    }

    fun onWorkNotesChanged(notes: String) {
        _uiState.value = _uiState.value.copy(workNotes = notes)
    }

    fun completeTask() {
        val task = _uiState.value.task ?: return
        val afterFile = _uiState.value.afterImageFile ?: run {
            _uiState.value = _uiState.value.copy(error = "Please upload an after photo before completing.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCompleting = true, error = null)
            completeTaskUseCase(task.id, afterFile, _uiState.value.workNotes).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isCompleting = false, isCompleted = true) },
                onFailure = { err -> _uiState.value = _uiState.value.copy(isCompleting = false, error = err.message) }
            )
        }
    }
}
