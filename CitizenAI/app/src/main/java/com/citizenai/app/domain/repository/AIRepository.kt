package com.citizenai.app.domain.repository

import com.citizenai.app.domain.model.AIAnalysisResult
import java.io.File

interface AIRepository {
    /**
     * Analyze an image and return AI classification result.
     * Sends image to POST /api/ai/analyze.
     * Falls back to development mock if backend is unavailable.
     */
    suspend fun analyzeImage(imageFile: File): Result<AIAnalysisResult>
}
