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
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String = "",
    val locationSource: String = "GPS",
    val isLoadingLocation: Boolean = false,
    val isSubmitting: Boolean = false,
    val locationError: String? = null,
    val submitError: String? = null,
    val showManualLocationDialog: Boolean = false,
    val manualArea: String = "",
    val manualStreet: String = "",
    val manualLandmark: String = "",
    val manualCity: String = "",
    val manualPincode: String = ""
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
        // Pre-fill from AI result and FlowState
        val aiState = ReportFlowState
        _uiState.value = _uiState.value.copy(
            description = aiState.suggestedDescription,
            category = IssueCategory.fromString(aiState.issueType),
            latitude = if (aiState.latitude != 0.0) aiState.latitude else null,
            longitude = if (aiState.longitude != 0.0) aiState.longitude else null,
            address = aiState.address,
            locationSource = aiState.locationSource.ifBlank { "GPS" },
            manualArea = aiState.manualArea,
            manualStreet = aiState.manualStreet,
            manualLandmark = aiState.manualLandmark,
            manualCity = aiState.manualCity,
            manualPincode = aiState.manualPincode
        )
    }

    fun onDescriptionChange(v: String) = update { copy(description = v, submitError = null) }
    fun onCategoryChange(v: IssueCategory) = update { copy(category = v) }

    fun openManualLocationDialog() {
        val flow = ReportFlowState
        update {
            copy(
                showManualLocationDialog = true,
                manualArea = if (manualArea.isNotBlank()) manualArea else flow.manualArea,
                manualStreet = if (manualStreet.isNotBlank()) manualStreet else flow.manualStreet,
                manualLandmark = if (manualLandmark.isNotBlank()) manualLandmark else flow.manualLandmark,
                manualCity = if (manualCity.isNotBlank()) manualCity else flow.manualCity,
                manualPincode = if (manualPincode.isNotBlank()) manualPincode else flow.manualPincode,
                locationError = null
            )
        }
    }

    fun dismissManualLocationDialog() {
        update { copy(showManualLocationDialog = false) }
    }

    fun updateManualArea(v: String) = update { copy(manualArea = v) }
    fun updateManualStreet(v: String) = update { copy(manualStreet = v) }
    fun updateManualLandmark(v: String) = update { copy(manualLandmark = v) }
    fun updateManualCity(v: String) = update { copy(manualCity = v) }
    fun updateManualPincode(v: String) = update { copy(manualPincode = v) }

    fun saveManualLocation(context: Context) {
        val s = _uiState.value
        val area = s.manualArea.trim()
        val street = s.manualStreet.trim()
        val landmark = s.manualLandmark.trim()
        val city = s.manualCity.trim()
        val pincode = s.manualPincode.trim()

        val parts = mutableListOf<String>()
        if (street.isNotBlank()) parts.add(street)
        if (landmark.isNotBlank()) parts.add(landmark)
        if (area.isNotBlank()) parts.add(area)
        if (city.isNotBlank()) parts.add(city)
        if (pincode.isNotBlank()) parts.add(pincode)

        if (parts.isEmpty()) {
            update { copy(locationError = "Please enter complaint location or address.") }
            return
        }

        val fullAddress = parts.joinToString(", ")

        ReportFlowState.locationSource = "MANUAL"
        ReportFlowState.address = fullAddress
        ReportFlowState.manualArea = area
        ReportFlowState.manualStreet = street
        ReportFlowState.manualLandmark = landmark
        ReportFlowState.manualCity = city
        ReportFlowState.manualPincode = pincode

        // Attempt forward geocoding if available, without blocking or faking
        val coords = locationHelper.geocodeAddress(context, fullAddress)
        val lat = coords?.first
        val lng = coords?.second

        ReportFlowState.latitude = lat ?: 0.0
        ReportFlowState.longitude = lng ?: 0.0

        update {
            copy(
                address = fullAddress,
                latitude = lat,
                longitude = lng,
                locationSource = "MANUAL",
                showManualLocationDialog = false,
                locationError = null
            )
        }
    }

    fun fetchCurrentLocation(context: Context) {
        viewModelScope.launch {
            update { copy(isLoadingLocation = true, locationError = null) }
            try {
                when (val result = locationHelper.getCurrentLocation(context)) {
                    is LocationResult.Success -> {
                        ReportFlowState.locationSource = "GPS"
                        ReportFlowState.latitude = result.latitude
                        ReportFlowState.longitude = result.longitude
                        ReportFlowState.address = result.address
                        update {
                            copy(
                                latitude = result.latitude,
                                longitude = result.longitude,
                                address = result.address,
                                locationSource = "GPS",
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
        val locSource = state.locationSource.ifBlank { flow.locationSource.ifBlank { "GPS" } }
        val addr = state.address.ifBlank { flow.address }

        if (addr.isBlank()) {
            update { copy(submitError = "Please provide or select a location for this complaint.") }
            return
        }

        if (locSource.equals("GPS", ignoreCase = true)) {
            val curLat = state.latitude ?: flow.latitude.takeIf { it != 0.0 }
            val curLon = state.longitude ?: flow.longitude.takeIf { it != 0.0 }
            if (curLat == null || curLon == null) {
                update { copy(submitError = "Please wait for GPS location or enter the address manually.") }
                return
            }
        }

        viewModelScope.launch {
            update { copy(isSubmitting = true, submitError = null) }
            try {
                val lat = state.latitude ?: if (flow.latitude != 0.0) flow.latitude else null
                val lon = state.longitude ?: if (flow.longitude != 0.0) flow.longitude else null

                val result = submitComplaintUseCase(
                    imagePath = flow.imagePath,
                    category = state.category,
                    description = state.description,
                    latitude = lat,
                    longitude = lon,
                    address = addr,
                    issueType = flow.issueType.ifBlank { state.category.displayName },
                    priority = flow.severity.ifBlank { "NORMAL" },
                    department = flow.department.ifBlank { "Public Works" },
                    locationSource = locSource
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
