package com.citizenai.app.presentation.report.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CaptureUiState(
    val capturedImageUri: Uri? = null,
    val capturedImageFile: File? = null,
    val showCamera: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReportCaptureViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    init {
        if (ReportFlowState.imageUri != null) {
            _uiState.value = _uiState.value.copy(
                capturedImageUri = ReportFlowState.imageUri,
                capturedImageFile = ReportFlowState.imagePath.takeIf { it.isNotBlank() }?.let { File(it) }
            )
        }
    }

    fun onImageCaptured(uri: Uri, file: File) {
        _uiState.value = _uiState.value.copy(
            capturedImageUri = uri,
            capturedImageFile = file,
            showCamera = false,
            error = null
        )
        // Store image path in a shared ViewModel (ReportFlowViewModel) — see below
        ReportFlowState.imagePath = file.absolutePath
        ReportFlowState.imageUri = uri
    }

    fun onGalleryImageSelected(uri: Uri, file: File?) {
        _uiState.value = _uiState.value.copy(
            capturedImageUri = uri,
            capturedImageFile = file,
            error = null
        )
        ReportFlowState.imageUri = uri
        file?.let { ReportFlowState.imagePath = it.absolutePath }
    }

    fun removeImage() {
        _uiState.value = _uiState.value.copy(capturedImageUri = null, capturedImageFile = null)
        ReportFlowState.imagePath = ""
        ReportFlowState.imageUri = null
    }

    fun onError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }
}

/**
 * ReportFlowState — simple object to share state across 3 report steps.
 * In a production app, use a shared SavedStateHandle ViewModel or navigation args.
 */
object ReportFlowState {
    var imagePath: String = ""
    var imageUri: Uri? = null
    var issueType: String = ""
    var severity: String = ""
    var department: String = ""
    var confidence: Float = 0f
    var suggestedDescription: String = ""
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var address: String = ""

    fun clear() {
        imagePath = ""; imageUri = null; issueType = ""; severity = ""
        department = ""; confidence = 0f; suggestedDescription = ""
        latitude = 0.0; longitude = 0.0; address = ""
    }
}
