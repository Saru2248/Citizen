package com.citizenai.worker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuthResponseDto(
    @SerializedName("token") val token: String? = null,
    @SerializedName("user") val user: UserDto? = null,
    @SerializedName("data") val data: UserDto? = null,
    @SerializedName("success") val success: Boolean? = true,
    @SerializedName("message") val message: String? = null
)

data class WorkerLoginRequestDto(
    @SerializedName("identifier") val identifier: String,
    @SerializedName("password") val password: String
)

data class LoginRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class UserDto(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("workerId") val workerId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("mobileNumber") val mobileNumber: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("firebaseUid") val firebaseUid: String? = null,
    @SerializedName("department") val department: String? = null,
    @SerializedName("departmentId") val departmentId: String? = null,
    @SerializedName("employeeId") val employeeId: String? = null,
    @SerializedName("accountStatus") val accountStatus: String? = null,
    @SerializedName("isActive") val isActive: Boolean? = true,
    @SerializedName("lastOtpVerifiedAt") val lastOtpVerifiedAt: String? = null,
    @SerializedName("totalLogins") val totalLogins: Int? = 0,
    @SerializedName("tasksCompleted") val tasksCompleted: Int? = 0,
    @SerializedName("tasksInProgress") val tasksInProgress: Int? = 0
)

data class ValidateWorkerRequestDto(
    @SerializedName("workerId") val workerId: String,
    @SerializedName("mobileNumber") val mobileNumber: String
)

data class VerifyWorkerOtpRequestDto(
    @SerializedName("idToken") val idToken: String? = null,
    @SerializedName("workerId") val workerId: String? = null,
    @SerializedName("phone") val phone: String? = null
)

data class ApiResponseDto<T>(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null,
    @SerializedName("tasks") val tasks: T? = null,
    @SerializedName("complaints") val complaints: T? = null
)

data class ComplaintDto(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("complaintId") val complaintId: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("priority") val priority: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("progressPercentage") val progressPercentage: Int? = 0,
    @SerializedName("address") val address: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("afterImageUrl") val afterImageUrl: String? = null,
    @SerializedName("completionPhotoUrl") val completionPhotoUrl: String? = null,
    @SerializedName("workerNotes") val workerNotes: String? = null,
    @SerializedName("progressNote") val progressNote: String? = null,
    @SerializedName("department") val department: String? = null,
    @SerializedName("assignedWorkerId") val assignedWorkerId: String? = null,
    @SerializedName("assignedWorkerName") val assignedWorkerName: String? = null,
    @SerializedName("citizenName") val citizenName: String? = null,
    @SerializedName("citizenPhone") val citizenPhone: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("statusHistory") val statusHistory: List<StatusHistoryDto>? = null
)

data class StatusHistoryDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("note") val note: String? = null,
    @SerializedName("updatedBy") val updatedBy: String? = null,
    @SerializedName("actorType") val actorType: String? = null,
    @SerializedName("actorId") val actorId: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)

data class WorkReportDto(
    @SerializedName("complaintId") val complaintId: String? = null,
    @SerializedName("issueCategory") val issueCategory: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("location") val location: LocationDto? = null,
    @SerializedName("department") val department: String? = null,
    @SerializedName("assignedWorker") val assignedWorker: String? = null,
    @SerializedName("reportedDate") val reportedDate: String? = null,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("completionDate") val completionDate: String? = null,
    @SerializedName("beforePhotoUrl") val beforePhotoUrl: String? = null,
    @SerializedName("afterPhotoUrl") val afterPhotoUrl: String? = null,
    @SerializedName("workerNotes") val workerNotes: String? = null,
    @SerializedName("statusHistory") val statusHistory: List<StatusHistoryDto>? = null,
    @SerializedName("finalStatus") val finalStatus: String? = null
)

data class LocationDto(
    @SerializedName("address") val address: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)

data class WorkerDashboardDto(
    @SerializedName("workerId") val workerId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("totalLogins") val totalLogins: Int = 0,
    @SerializedName("totalAssignedComplaints") val totalAssignedComplaints: Int = 0,
    @SerializedName("newComplaints") val newComplaints: Int = 0,
    @SerializedName("pendingComplaints") val pendingComplaints: Int = 0,
    @SerializedName("inProgressComplaints") val inProgressComplaints: Int = 0,
    @SerializedName("completedComplaints") val completedComplaints: Int = 0,
    @SerializedName("recentComplaints") val recentComplaints: List<ComplaintDto>? = emptyList()
)
