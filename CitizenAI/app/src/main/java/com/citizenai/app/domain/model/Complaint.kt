package com.citizenai.app.domain.model

import java.time.Instant

/**
 * Complaint — core domain model representing a citizen-reported civic issue.
 *
 * Full lifecycle state machine:
 * REPORTED → AI_ANALYZED → UNDER_REVIEW → DEPARTMENT_ASSIGNED →
 * WORKER_ASSIGNED → WORKER_ACCEPTED → WORK_STARTED → IN_PROGRESS →
 * WORK_COMPLETED → ADMIN_REVIEW → CITIZEN_VERIFICATION → RESOLVED
 *
 * Alternative exits: REOPENED, REJECTED, CANCELLED
 */
data class Complaint(
    val id: String,
    val complaintId: String,        // Display ID e.g. "CIV-1024"
    val citizenId: String,
    val citizenName: String,
    val issueType: String,          // e.g. "Pothole", "Garbage", "Broken Streetlight"
    val category: IssueCategory,
    val description: String,
    val imageUrl: String?,          // Before photo (citizen uploaded)
    val afterImageUrl: String?,     // After photo (worker uploaded)
    val latitude: Double,
    val longitude: Double,
    val address: String,            // Reverse geocoded address
    val status: ComplaintStatus,
    val priority: Priority,
    val department: String,
    val departmentId: String? = null,
    val assignedWorkerId: String?,
    val assignedWorkerName: String?,
    val aiConfidence: Float?,       // 0.0-1.0
    val reportedAt: Instant,
    val updatedAt: Instant,
    val deadline: Instant? = null,
    val resolvedAt: Instant? = null,
    val workerNotes: String? = null,
    val progressPercentage: Int = 0,
    val progressNote: String? = null,
    val distanceKm: Double? = null,  // Worker-specific: distance from worker's location
    val statusHistory: List<StatusHistoryEntry> = emptyList()
) {
    val completionPhotoUrl: String? get() = afterImageUrl
}

data class StatusHistoryEntry(
    val status: String = "",
    val timestamp: Long = System.currentTimeMillis()
)


/**
 * Full complaint lifecycle state machine.
 * Transitions are validated server-side.
 */
enum class ComplaintStatus {
    // Initial states
    REPORTED,
    AI_ANALYZED,
    UNDER_REVIEW,

    // Assignment states
    DEPARTMENT_ASSIGNED,
    WORKER_ASSIGNED,
    WORKER_ACCEPTED,

    // Work states
    WORK_STARTED,
    IN_PROGRESS,
    PROGRESS_UPDATE,
    WORK_COMPLETED,

    // Review/Verification
    ADMIN_REVIEW,
    CITIZEN_VERIFICATION,

    // Terminal states
    RESOLVED,
    REOPENED,
    REJECTED,
    CANCELLED,

    // Legacy aliases (backward compatibility with older API responses)
    PENDING,
    ASSIGNED,
    COMPLETED;

    /** Returns the canonical display label for this status. */
    fun displayLabel(): String = when (this) {
        REPORTED            -> "Reported"
        AI_ANALYZED         -> "AI Analyzed"
        UNDER_REVIEW        -> "Under Review"
        DEPARTMENT_ASSIGNED -> "Department Assigned"
        WORKER_ASSIGNED     -> "Worker Assigned"
        WORKER_ACCEPTED     -> "Worker Accepted"
        WORK_STARTED        -> "Work Started"
        IN_PROGRESS         -> "In Progress"
        PROGRESS_UPDATE     -> "Progress Update"
        WORK_COMPLETED      -> "Work Completed"
        ADMIN_REVIEW        -> "Admin Review"
        CITIZEN_VERIFICATION -> "Awaiting Verification"
        RESOLVED            -> "Resolved"
        REOPENED            -> "Reopened"
        REJECTED            -> "Rejected"
        CANCELLED           -> "Cancelled"
        PENDING             -> "Pending"
        ASSIGNED            -> "Assigned"
        COMPLETED           -> "Completed"
    }

    fun isTerminal(): Boolean = this in listOf(RESOLVED, REJECTED, CANCELLED, COMPLETED)
    fun isActive(): Boolean = !isTerminal()
}

enum class Priority {
    LOW,
    NORMAL,
    MEDIUM,
    HIGH,
    CRITICAL;

    fun displayLabel(): String = when (this) {
        LOW      -> "Low"
        NORMAL   -> "Normal"
        MEDIUM   -> "Medium"
        HIGH     -> "High"
        CRITICAL -> "Critical"
    }
}

enum class IssueCategory(val displayName: String) {
    POTHOLE("Pothole"),
    GARBAGE("Garbage"),
    STREETLIGHT("Streetlight"),
    WATER_LEAKAGE("Water Leakage"),
    DRAINAGE("Drainage"),
    ROAD_DAMAGE("Road Damage"),
    TRAFFIC_SIGNAL("Traffic Signal"),
    DAMAGED_SIGNAGE("Damaged Signage"),
    FALLEN_TREE("Fallen Tree"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): IssueCategory {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                it.displayName.equals(value, ignoreCase = true)
            } ?: OTHER
        }
    }
}
