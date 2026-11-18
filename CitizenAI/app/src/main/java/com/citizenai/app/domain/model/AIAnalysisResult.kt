package com.citizenai.app.domain.model

/**
 * AIAnalysisResult — result from AI image analysis endpoint.
 * Received from backend POST /api/ai/analyze (Hugging Face Vision AI).
 */
data class AIAnalysisResult(
    val issueType: String,                 // e.g. "Pothole", "Garbage Accumulation"
    val severity: String,                  // "LOW" | "NORMAL" | "HIGH" | "CRITICAL"
    val department: String,                // e.g. "Road Maintenance", "Sanitation"
    val confidence: Float? = null,         // 0.0-1.0 or null if uncalibrated
    val suggestedCategory: IssueCategory,
    val suggestedDescription: String = "", // AI-generated description hint
    val detected: Boolean = true,
    val needsHumanReview: Boolean = false,
    val imageHash: String? = null
)
