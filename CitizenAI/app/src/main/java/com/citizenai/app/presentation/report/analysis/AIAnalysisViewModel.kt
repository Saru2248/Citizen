package com.citizenai.app.presentation.report.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.app.domain.model.AIAnalysisResult
import com.citizenai.app.domain.usecase.ai.AnalyzeImageUseCase
import com.citizenai.app.presentation.report.capture.ReportFlowState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class AnalysisStage(val label: String, val completed: Boolean)

data class AIAnalysisUiState(
    val stages: List<AnalysisStage> = listOf(
        AnalysisStage("Detecting issue type", false),
        AnalysisStage("Checking severity", false),
        AnalysisStage("Finding responsible department", false)
    ),
    val result: AIAnalysisResult? = null,
    val isAnalyzing: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AIAnalysisViewModel @Inject constructor(
    private val analyzeImageUseCase: AnalyzeImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIAnalysisUiState())
    val uiState: StateFlow<AIAnalysisUiState> = _uiState.asStateFlow()

    init {
        startAnalysis()
    }

    fun retryAnalysis() {
        _uiState.value = AIAnalysisUiState(
            stages = listOf(
                AnalysisStage("Detecting issue type", false),
                AnalysisStage("Checking severity", false),
                AnalysisStage("Finding responsible department", false)
            ),
            isAnalyzing = true,
            error = null
        )
        startAnalysis()
    }

    private fun startAnalysis() {
        viewModelScope.launch {
            val imagePath = ReportFlowState.imagePath
            val imageFile = if (imagePath.isNotBlank()) File(imagePath) else null

            // Animate stages progressively
            delay(800)
            completeStage(0)
            delay(700)
            completeStage(1)
            delay(700)
            completeStage(2)

            // Now call the AI
            val result = if (imageFile != null && imageFile.exists()) {
                analyzeImageUseCase(imageFile)
            } else {
                Result.failure(IllegalArgumentException("No image captured. Please go back and capture an image."))
            }

            result.fold(
                onSuccess = { aiResult ->
                    if (!aiResult.detected) {
                        _uiState.value = _uiState.value.copy(
                            isAnalyzing = false,
                            error = "The uploaded photo is unclear or does not clearly depict a civic infrastructure issue. Please retake or select a clearer photo."
                        )
                    } else {
                        // Persist result for the details step
                        ReportFlowState.issueType = aiResult.issueType
                        ReportFlowState.severity = aiResult.severity
                        ReportFlowState.department = aiResult.department
                        ReportFlowState.confidence = aiResult.confidence
                        ReportFlowState.suggestedDescription = aiResult.suggestedDescription

                        _uiState.value = _uiState.value.copy(
                            result = aiResult,
                            isAnalyzing = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        error = err.message ?: "Failed to analyze image."
                    )
                }
            )
        }
    }

    private fun completeStage(index: Int) {
        val stages = _uiState.value.stages.toMutableList()
        stages[index] = stages[index].copy(completed = true)
        _uiState.value = _uiState.value.copy(stages = stages)
    }
}
