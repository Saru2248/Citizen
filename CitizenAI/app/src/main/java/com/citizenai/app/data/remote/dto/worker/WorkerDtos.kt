package com.citizenai.app.data.remote.dto.worker

import com.google.gson.annotations.SerializedName

data class WorkerTaskDto(
    @SerializedName("id") val id: String,
    @SerializedName("complaintId") val complaintId: String,
    @SerializedName("issueType") val issueType: String,
    @SerializedName("category") val category: String,
    @SerializedName("address") val address: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("priority") val priority: String,
    @SerializedName("status") val status: String,
    @SerializedName("beforeImageUrl") val beforeImageUrl: String?,
    @SerializedName("afterImageUrl") val afterImageUrl: String?,
    @SerializedName("citizenDescription") val citizenDescription: String,
    @SerializedName("workerNotes") val workerNotes: String?,
    @SerializedName("assignedAt") val assignedAt: String,
    @SerializedName("acceptedAt") val acceptedAt: String?,
    @SerializedName("startedAt") val startedAt: String?,
    @SerializedName("completedAt") val completedAt: String?,
    @SerializedName("deadline") val deadline: String?,
    @SerializedName("progressPercentage") val progressPercentage: Int?,
    @SerializedName("assignedBy") val assignedBy: String?,
    @SerializedName("assignedByName") val assignedByName: String?,
    @SerializedName("distanceKm") val distanceKm: Double?
)

data class UpdateTaskStatusRequest(
    @SerializedName("status") val status: String
)

data class CompleteTaskRequest(
    @SerializedName("notes") val notes: String
)

data class RejectTaskRequest(
    @SerializedName("reason") val reason: String
)

data class ProgressUpdateDto(
    @SerializedName("id") val id: String,
    @SerializedName("complaintId") val complaintId: String,
    @SerializedName("taskId") val taskId: String,
    @SerializedName("workerId") val workerId: String,
    @SerializedName("workerName") val workerName: String,
    @SerializedName("progressPercentage") val progressPercentage: Int,
    @SerializedName("note") val note: String,
    @SerializedName("photoUrl") val photoUrl: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("createdAt") val createdAt: String
)

/** Worker monitoring data for Admin */
data class WorkerMonitorDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("workerId") val workerId: String?,
    @SerializedName("department") val department: String?,
    @SerializedName("availability") val availability: String,   // AVAILABLE, BUSY, OFFLINE, ON_LEAVE
    @SerializedName("activeTaskCount") val activeTaskCount: Int,
    @SerializedName("currentComplaintId") val currentComplaintId: String?,
    @SerializedName("currentIssueType") val currentIssueType: String?,
    @SerializedName("currentTaskStatus") val currentTaskStatus: String?,
    @SerializedName("progressPercentage") val progressPercentage: Int?,
    @SerializedName("lastUpdateAt") val lastUpdateAt: String?,
    @SerializedName("lastNote") val lastNote: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("tasksCompletedToday") val tasksCompletedToday: Int,
    @SerializedName("tasksDelayed") val tasksDelayed: Int
)
