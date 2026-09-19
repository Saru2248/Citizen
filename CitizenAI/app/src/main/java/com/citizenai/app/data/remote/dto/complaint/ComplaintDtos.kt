package com.citizenai.app.data.remote.dto.complaint

import com.google.gson.annotations.SerializedName

data class ComplaintDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("_id") val mongoId: String? = null,
    @SerializedName("complaintId") val complaintId: String? = null,
    @SerializedName("citizenId") val citizenId: String? = null,
    @SerializedName("citizenName") val citizenName: String? = null,
    @SerializedName("issueType") val issueType: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("photoUrl") val photoUrl: String? = null,
    @SerializedName("afterImageUrl") val afterImageUrl: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("priority") val priority: String? = null,
    @SerializedName("department") val department: String? = null,
    @SerializedName("departmentId") val departmentId: String? = null,
    @SerializedName("assignedWorkerId") val assignedWorkerId: String? = null,
    @SerializedName("assignedWorkerName") val assignedWorkerName: String? = null,
    @SerializedName("aiConfidence") val aiConfidence: Float? = null,
    @SerializedName("reportedAt") val reportedAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("deadline") val deadline: String? = null,
    @SerializedName("resolvedAt") val resolvedAt: String? = null,
    @SerializedName("workerNotes") val workerNotes: String? = null,
    @SerializedName("progressPercentage") val progressPercentage: Int? = null,
    @SerializedName("progressNote") val progressNote: String? = null,
    @SerializedName("distanceKm") val distanceKm: Double? = null
)

data class TimelineEventDto(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("completed") val completed: Boolean,
    @SerializedName("actorName") val actorName: String?,
    @SerializedName("actorRole") val actorRole: String?
)

data class CommentDto(
    @SerializedName("id") val id: String,
    @SerializedName("complaintId") val complaintId: String,
    @SerializedName("authorId") val authorId: String,
    @SerializedName("authorName") val authorName: String,
    @SerializedName("authorRole") val authorRole: String,
    @SerializedName("message") val message: String,
    @SerializedName("postedAt") val postedAt: String
)

data class VerifyResolutionRequest(
    @SerializedName("resolved") val resolved: Boolean,
    @SerializedName("reason") val reason: String?
)
