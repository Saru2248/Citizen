package com.citizenai.app.domain.usecase.ai

import com.citizenai.app.domain.model.AIAnalysisResult
import com.citizenai.app.domain.repository.AIRepository
import java.io.File
import javax.inject.Inject

class AnalyzeImageUseCase @Inject constructor(
    private val aiRepository: AIRepository
) {
    suspend operator fun invoke(imageFile: File): Result<AIAnalysisResult> {
        if (!imageFile.exists()) {
            return Result.failure(IllegalArgumentException("Image file not found"))
        }
        if (imageFile.length() > 10 * 1024 * 1024) {
            return Result.failure(IllegalArgumentException("Image file is too large (max 10MB)"))
        }
        return aiRepository.analyzeImage(imageFile)
    }
}
