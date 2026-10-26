package com.citizenai.app.data.remote.dto.ai

import com.google.gson.annotations.SerializedName

data class AIAnalysisResponse(
    @SerializedName("success") val success: Boolean? = true,
    @SerializedName("detected") val detected: Boolean? = true,
    @SerializedName("issue") val issue: String? = null,
    @SerializedName("issueType") val issueType: String? = null,
    @SerializedName("severity") val severity: String? = null,
    @SerializedName("responsibleDepartment") val responsibleDepartment: String? = null,
    @SerializedName("department") val department: String? = null,
    @SerializedName("confidence") val confidence: Float? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("suggestedDescription") val suggestedDescription: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("imageHash") val imageHash: String? = null
)
