package com.citizenai.app.domain.model

import java.time.Instant

/**
 * ProgressUpdate — a worker's in-progress report for an active task.
 *
 * Workers submit updates with percentage, optional photo, note, and location.
 * These are displayed to both the Admin (monitoring) and Citizen (complaint tracking).
 */
data class ProgressUpdate(
    val id: String,
    val complaintId: String,
    val taskId: String,
    val workerId: String,
    val workerName: String,
    val progressPercentage: Int,   // 0, 25, 50, 75, 100
    val note: String,
    val photoUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Instant
)

/** Photo types uploaded during the complaint lifecycle. */
enum class WorkPhotoType {
    BEFORE,     // Citizen-submitted photo of the issue
    PROGRESS,   // Worker progress photo during work
    COMPLETION  // Worker's final after-work photo
}
