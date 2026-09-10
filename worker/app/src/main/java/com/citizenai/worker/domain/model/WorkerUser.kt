package com.citizenai.worker.domain.model

data class WorkerUser(
    val id: String,
    val workerId: String? = null,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val mobileNumber: String? = null,
    val role: String = "WORKER",
    val firebaseUid: String? = null,
    val department: String? = null,
    val departmentId: String? = null,
    val employeeId: String? = null,
    val accountStatus: String = "ACTIVE",
    val isActive: Boolean = true,
    val lastOtpVerifiedAt: String? = null,
    val totalLogins: Int = 0,
    val tasksCompleted: Int = 0,
    val tasksInProgress: Int = 0,
    val token: String? = null
)
