package com.citizenai.app.domain.model

import java.time.Instant

/**
 * WorkerMonitor — Admin view of a worker's current state.
 * Used in Admin monitoring screens (Live Ops, Worker Management).
 */
data class WorkerMonitor(
    val id: String,
    val name: String,
    val workerId: String?,
    val department: String?,
    val availability: WorkerAvailability,
    val activeTaskCount: Int,
    val currentComplaintId: String?,
    val currentIssueType: String?,
    val currentTaskStatus: String?,
    val progressPercentage: Int?,
    val lastUpdateAt: Instant?,
    val lastNote: String?,
    val latitude: Double?,
    val longitude: Double?,
    val tasksCompletedToday: Int,
    val tasksDelayed: Int
) {
    /** Minutes since last update, null if never updated */
    fun minutesSinceLastUpdate(): Long? {
        return lastUpdateAt?.let {
            val diff = Instant.now().epochSecond - it.epochSecond
            diff / 60
        }
    }

    fun isDelayed(thresholdMinutes: Long = 30): Boolean {
        return minutesSinceLastUpdate()?.let { it > thresholdMinutes } ?: false
    }
}

enum class WorkerAvailability {
    AVAILABLE,  // No active tasks
    BUSY,       // Has active tasks
    OFFLINE,    // Not logged in / no recent activity
    ON_LEAVE,   // Leave marked by admin
    DISABLED;   // Account disabled

    fun displayLabel(): String = when (this) {
        AVAILABLE -> "Available"
        BUSY      -> "On Task"
        OFFLINE   -> "Offline"
        ON_LEAVE  -> "On Leave"
        DISABLED  -> "Disabled"
    }
}
