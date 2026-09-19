package com.citizenai.app.data.remote.dto.notification

import com.google.gson.annotations.SerializedName

data class NotificationDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("type") val type: String,
    @SerializedName("complaintId") val complaintId: String?,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("createdAt") val createdAt: String
)
