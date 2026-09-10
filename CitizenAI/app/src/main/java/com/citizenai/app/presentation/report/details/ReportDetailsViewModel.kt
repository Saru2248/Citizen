package com.citizenai.app.presentation.report.details

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.app.domain.model.IssueCategory
import com.citizenai.app.domain.usecase.complaint.SubmitComplaintUseCase
import com.citizenai.app.presentation.report.capture.ReportFlowState
import com.citizenai.app.util.LocationHelper
import com.citizenai.app.util.LocationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportDetailsUiState(
    val description: String = "",
    val category: IssueCategory = IssueCategory.POTHOLE,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val isLoadingLocation: Boolean = false,
    val isSubmitting: Boolean = false,
    val locationError: String? = null,
    val submitError: String? = null
)

@HiltViewModel
class ReportDetailsViewModel @Inject constructor(
    private val submitComplaintUseCase: SubmitComplaintUseCase,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportDetailsUiState())
    val uiState: StateFlow<ReportDetailsUiState> = _uiState.asStateFlow()

    private val _submittedComplaintId = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val submittedComplaintId: SharedFlow<String> = _submittedComplaintId.asSharedFlow()

    init {
        // Pre-fill from AI result
        val aiState = ReportFlowState
        _uiState.value = _uiState.value.copy(
            description = aiState.suggestedDescription,
            category = IssueCategory.fromString(aiState.issueType)
        )
    }

    fun onDescriptionChange(v: String) = update { copy(description = v, submitError = null) }
    fun onCategoryChange(v: IssueCategory) = update { copy(category = v) }

    fun fetchCurrentLocation(context: Context) {
        viewModelScope.launch {
            update { copy(isLoadingLocation = true, locationError = null) }
            try {
                when (val result = locationHelper.getCurrentLocation(context)) {
                    is LocationResult.Success -> {
                        ReportFlowState.latitude = result.latitude
                        ReportFlowState.longitude = result.longitude
                        ReportFlowState.address = result.address
                        update {
                            copy(
                                latitude = result.latitude,
                                longitude = result.longitude,
                                address = result.address,
                                locationError = null
                            )
                        }
                    }
                    is LocationResult.Error -> {
                        update { copy(locationError = result.message) }
                    }
                }
            } catch (e: Exception) {
                update { copy(locationError = e.message ?: "Failed to acquire location") }
            } finally {
                update { copy(isLoadingLocation = false) }
            }
        }
    }

    fun submitReport() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val flow = ReportFlowState
        viewModelScope.launch {
            update { copy(isSubmitting = true, submitError = null) }
            try {
                // Use stored location if available
                val lat = if (state.latitude != 0.0) state.latitude else flow.latitude
                val lon = if (state.longitude != 0.0) state.longitude else flow.longitude
                val addr = state.address.ifBlank { flow.address }

                val result = submitComplaintUseCase(
                    imagePath = flow.imagePath,
                    category = state.category,
                    description = state.description,
                    latitude = lat,
                    longitude = lon,
                    address = addr,
                    issueType = flow.issueType.ifBlank { state.category.displayName },
                    priority = flow.severity.ifBlank { "NORMAL" },
                    department = flow.department.ifBlank { "Public Works" }
                )

                result.fold(
                    onSuccess = { complaint ->
                        update { copy(isSubmitting = false, submitError = null) }
                        ReportFlowState.clear()
                        _submittedComplaintId.tryEmit(complaint.complaintId)
                    },
                    onFailure = { err ->
                        val msg = err.message?.takeIf { it.isNotBlank() }
                            ?: "Failed to submit report. Please check backend connectivity."
                        update { copy(isSubmitting = false, submitError = msg) }
                    }
                )
            } catch (e: Exception) {
                val msg = e.message?.takeIf { it.isNotBlank() }
                    ?: "An unexpected error occurred while submitting report."
                update { copy(isSubmitting = false, submitError = msg) }
            } finally {
                update { copy(isSubmitting = false) }
            }
        }
    }

    private fun update(block: ReportDetailsUiState.() -> ReportDetailsUiState) {
        _uiState.value = _uiState.value.block()
    }
}
