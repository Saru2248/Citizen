package com.citizenai.worker.presentation.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.worker.domain.model.WorkReport
import com.citizenai.worker.domain.repository.WorkerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkReportUiState(
    val taskId: String = "",
    val report: WorkReport? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class WorkReportViewModel @Inject constructor(
    private val workerRepository: WorkerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String = savedStateHandle.get<String>("taskId") ?: ""

    private val _uiState = MutableStateFlow(WorkReportUiState(taskId = taskId))
    val uiState: StateFlow<WorkReportUiState> = _uiState.asStateFlow()

    init {
        if (taskId.isNotEmpty()) {
            loadReport()
        }
    }

    fun loadReport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val res = workerRepository.getTaskReport(taskId)
            res.fold(
                onSuccess = { report ->
                    _uiState.value = _uiState.value.copy(isLoading = false, report = report)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Failed to load report"
                    )
                }
            )
        }
    }
}
