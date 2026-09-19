package com.citizenai.app.data.remote.dto.admin

import com.google.gson.annotations.SerializedName

data class AssignWorkerRequest(
    @SerializedName("workerId") val workerId: String,
    @SerializedName("deadline") val deadline: String?,      // ISO 8601
    @SerializedName("priority") val priority: String?,
    @SerializedName("note") val note: String?
)

data class AdminStatsDto(
    @SerializedName("totalComplaints") val totalComplaints: Int,
    @SerializedName("pendingComplaints") val pendingComplaints: Int,
    @SerializedName("inProgressComplaints") val inProgressComplaints: Int,
    @SerializedName("resolvedComplaints") val resolvedComplaints: Int,
    @SerializedName("criticalComplaints") val criticalComplaints: Int,
    @SerializedName("delayedTasks") val delayedTasks: Int,
    @SerializedName("activeWorkers") val activeWorkers: Int,
    @SerializedName("totalWorkers") val totalWorkers: Int
)

data class AdminCommentRequest(
    @SerializedName("message") val message: String,
    @SerializedName("visibility") val visibility: String = "ALL"  // ALL, CITIZEN_ONLY, ADMIN_ONLY
)

data class EscalateRequest(
    @SerializedName("reason") val reason: String,
    @SerializedName("escalateTo") val escalateTo: String   // DEPARTMENT_ADMIN, SUPER_ADMIN
)
