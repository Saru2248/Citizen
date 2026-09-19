package com.citizenai.app.data.remote.dto.ai

import com.google.gson.annotations.SerializedName

data class AIAnalysisResponse(
    @SerializedName("issueType") val issueType: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("department") val department: String,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("suggestedDescription") val suggestedDescription: String?
)
