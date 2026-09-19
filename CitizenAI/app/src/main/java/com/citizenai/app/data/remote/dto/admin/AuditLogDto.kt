package com.citizenai.app.data.remote.dto.admin

import com.google.gson.annotations.SerializedName

/** DTO for audit log entry from backend */
data class AuditLogDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("userName") val userName: String,
    @SerializedName("userRole") val userRole: String,
    @SerializedName("action") val action: String,
    @SerializedName("entityType") val entityType: String,
    @SerializedName("entityId") val entityId: String,
    @SerializedName("oldValue") val oldValue: String? = null,
    @SerializedName("newValue") val newValue: String? = null,
    @SerializedName("createdAt") val createdAt: String
)
