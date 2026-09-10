package com.citizenai.app.presentation.worker.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.app.data.datastore.UserPreferencesDataStore
import com.citizenai.app.domain.model.*
import com.citizenai.app.domain.usecase.worker.GetWorkerTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkerHomeUiState(
    val workerName: String = "",
    val activeTasks: List<WorkerTask> = emptyList(),
    val criticalCount: Int = 0,
    val highCount: Int = 0,
    val normalCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class WorkerHomeViewModel @Inject constructor(
    private val getWorkerTasksUseCase: GetWorkerTasksUseCase,
    private val dataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerHomeUiState())
    val uiState: StateFlow<WorkerHomeUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            val name = dataStore.getUserName().firstOrNull() ?: "Worker"
            _uiState.value = _uiState.value.copy(isLoading = true, workerName = name)
            getWorkerTasksUseCase().fold(
                onSuccess = { tasks ->
                    val active = tasks.filter { it.status != TaskStatus.COMPLETED }
                    _uiState.value = WorkerHomeUiState(
                        workerName = name,
                        activeTasks = active,
                        criticalCount = active.count { it.priority == Priority.CRITICAL },
                        highCount = active.count { it.priority == Priority.HIGH },
                        normalCount = active.count { it.priority == Priority.NORMAL || it.priority == Priority.LOW },
                        isLoading = false
                    )
                },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false) }
            )
        }
    }
}
