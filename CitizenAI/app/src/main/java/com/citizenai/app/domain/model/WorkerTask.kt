package com.citizenai.app.domain.model

import java.time.Instant

/**
 * WorkerTask — a complaint assignment visible to a Worker.
 * Mirrors the Complaint model but includes worker-specific fields.
 *
 * Status flow: PENDING → ACCEPTED / REJECTED → IN_PROGRESS → COMPLETED / REOPENED
 */
data class WorkerTask(
    val id: String,
    val complaintId: String,       // e.g. "CIV-1024"
    val issueType: String,
    val category: IssueCategory,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val priority: Priority,
    val status: TaskStatus,
    val beforeImageUrl: String?,
    val afterImageUrl: String?,
    val citizenDescription: String,
    val workerNotes: String?,
    val assignedAt: Instant,
    val acceptedAt: Instant? = null,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val deadline: Instant? = null,
    val progressPercentage: Int = 0,
    val assignedBy: String? = null,
    val assignedByName: String? = null,
    val distanceKm: Double?
)

enum class TaskStatus {
    PENDING,       // Assigned but not yet accepted by worker
    ACCEPTED,      // Worker accepted
    REJECTED,      // Worker rejected (reassignment needed)
    IN_PROGRESS,   // Worker started work
    COMPLETED,     // Worker submitted completion
    REOPENED;      // Citizen rejected completion — back to active

    fun displayLabel(): String = when (this) {
        PENDING    -> "Pending Acceptance"
        ACCEPTED   -> "Accepted"
        REJECTED   -> "Rejected"
        IN_PROGRESS -> "In Progress"
        COMPLETED  -> "Completed"
        REOPENED   -> "Reopened"
    }
}
