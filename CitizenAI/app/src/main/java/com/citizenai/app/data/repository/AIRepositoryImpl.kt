package com.citizenai.app.data.repository

import com.citizenai.app.domain.model.AIAnalysisResult
import com.citizenai.app.domain.model.IssueCategory
import com.citizenai.app.domain.repository.AIRepository
import kotlinx.coroutines.delay
import java.io.File
import javax.inject.Inject

/**
 * AIRepositoryImpl — client-side / mock AI analysis pipeline.
 *
 * NOTE (STEP 5): AI image classification is currently handled via this structured
 * local analysis module. If cloud AI inference is required later, it can be attached
 * via a Firebase Cloud Function or external Vision API.
 */
class AIRepositoryImpl @Inject constructor() : AIRepository {

    override suspend fun analyzeImage(imageFile: File): Result<AIAnalysisResult> {
        return getMockAnalysisResult(imageFile)
    }

    private suspend fun getMockAnalysisResult(imageFile: File): Result<AIAnalysisResult> {
        delay(2000)

        val fileName = imageFile.name.lowercase()
        val (issueType, category, dept, desc) = when {
            fileName.contains("water") || fileName.contains("leak") -> Quadruple(
                "Major Pipeline Leakage", IssueCategory.WATER_LEAKAGE, "Water Supply", "Clean drinking water leaking rapidly on public street."
            )
            fileName.contains("light") || fileName.contains("dark") -> Quadruple(
                "Non-Functional Streetlight", IssueCategory.STREETLIGHT, "Electrical Department", "Streetlight is out, creating dark conditions at night."
            )
            fileName.contains("garbage") || fileName.contains("trash") -> Quadruple(
                "Overflowing Garbage Dumpster", IssueCategory.GARBAGE, "Solid Waste Management", "Uncollected garbage accumulating in residential area."
            )
            else -> Quadruple(
                "Pothole", IssueCategory.POTHOLE, "Road Maintenance", "Large pothole detected on the road surface. Requires immediate attention to prevent vehicle damage."
            )
        }

        val mockResult = AIAnalysisResult(
            issueType = issueType,
            severity = "HIGH",
            department = dept,
            confidence = 0.94f,
            suggestedCategory = category,
            suggestedDescription = desc
        )

        return Result.success(mockResult)
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
