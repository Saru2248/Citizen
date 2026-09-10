package com.citizenai.worker.domain.model

data class WorkerTask(
    val id: String, // MongoDB Complaint _id or complaintId
    val complaintId: String, // e.g. "CIV-1028"
    val title: String,
    val category: String,
    val description: String,
    val priority: String = "MEDIUM",
    val status: TaskStatus = TaskStatus.WORKER_ASSIGNED,
    val statusRaw: String = "WORKER_ASSIGNED",
    val progressPercentage: Int = 0,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val beforePhotoUrl: String? = null,
    val afterPhotoUrl: String? = null,
    val workerNotes: String? = null,
    val progressNote: String? = null,
    val department: String? = null,
    val assignedWorkerId: String? = null,
    val assignedWorkerName: String? = null,
    val citizenName: String? = null,
    val citizenPhone: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val statusHistory: List<StatusHistoryItem> = emptyList()
)
