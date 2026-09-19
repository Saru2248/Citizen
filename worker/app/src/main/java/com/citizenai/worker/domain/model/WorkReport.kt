package com.citizenai.worker.domain.model

data class WorkReport(
    val complaintId: String,
    val issueCategory: String,
    val description: String,
    val locationAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val department: String? = null,
    val assignedWorker: String? = null,
    val reportedDate: String? = null,
    val startedAt: String? = null,
    val completionDate: String? = null,
    val beforePhotoUrl: String? = null,
    val afterPhotoUrl: String? = null,
    val beforeUploadedAt: String? = null,
    val beforeUploadedByRole: String? = null,
    val afterUploadedAt: String? = null,
    val afterUploadedByRole: String? = null,
    val workerNotes: String? = null,
    val statusHistory: List<StatusHistoryItem> = emptyList(),
    val finalStatus: String = "COMPLETED"
)
