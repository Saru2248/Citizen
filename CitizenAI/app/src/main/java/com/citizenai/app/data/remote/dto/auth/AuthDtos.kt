package com.citizenai.app.data.remote.dto.auth

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("phone") val phone: String
)

data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserDto
)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("role") val role: String,
    @SerializedName("adminLevel") val adminLevel: String?,
    @SerializedName("departmentId") val departmentId: String?,
    @SerializedName("avatarUrl") val avatarUrl: String?,
    @SerializedName("workerId") val workerId: String?,
    @SerializedName("department") val department: String?,
    @SerializedName("totalReports") val totalReports: Int?,
    @SerializedName("resolvedReports") val resolvedReports: Int?,
    @SerializedName("pendingReports") val pendingReports: Int?,
    @SerializedName("tasksCompleted") val tasksCompleted: Int?,
    @SerializedName("tasksInProgress") val tasksInProgress: Int?,
    @SerializedName("avgCompletionTimeHours") val avgCompletionTimeHours: Double?
)

data class FcmTokenRequest(
    @SerializedName("token") val token: String,
    @SerializedName("platform") val platform: String = "android"
)
