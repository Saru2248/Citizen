package com.citizenai.app.domain.model

/**
 * User domain model — represents an authenticated user (citizen, worker, or admin).
 * Intentionally separate from Room Entity and API DTO to maintain clean boundaries.
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val role: UserRole,
    val adminLevel: AdminLevel? = null,   // Only set when role == ADMIN
    val departmentId: String? = null,
    val avatarUrl: String? = null,
    // Worker-specific
    val workerId: String? = null,
    val department: String? = null,
    // Statistics
    val totalReports: Int = 0,
    val resolvedReports: Int = 0,
    val pendingReports: Int = 0,
    val tasksCompleted: Int = 0,
    val tasksInProgress: Int = 0,
    val avgCompletionTimeHours: Double? = null
)

enum class UserRole {
    CITIZEN,
    WORKER,
    ADMIN
}

/**
 * Admin hierarchy levels — all authenticated as ADMIN role,
 * but with different permission scopes enforced server-side.
 */
enum class AdminLevel {
    SUPERVISOR,
    DEPARTMENT_ADMIN,
    SUPER_ADMIN
}
