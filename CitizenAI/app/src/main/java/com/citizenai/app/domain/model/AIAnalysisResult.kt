package com.citizenai.app.domain.model

/**
 * AIAnalysisResult — result from AI image analysis endpoint.
 * Received from backend POST /api/ai/analyze (Gemini Vision).
 */
data class AIAnalysisResult(
    val issueType: String,          // e.g. "Pothole"
    val severity: String,           // "LOW" | "NORMAL" | "HIGH" | "CRITICAL"
    val department: String,         // e.g. "Road Maintenance"
    val confidence: Float,          // 0.0-1.0
    val suggestedCategory: IssueCategory,
    val suggestedDescription: String = "", // AI-generated description hint
    val detected: Boolean = true,
    val imageHash: String? = null
)
