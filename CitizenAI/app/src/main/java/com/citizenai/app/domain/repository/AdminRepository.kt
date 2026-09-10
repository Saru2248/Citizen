package com.citizenai.app.domain.repository

import com.citizenai.app.domain.model.Complaint
import com.citizenai.app.domain.model.ProgressUpdate
import com.citizenai.app.domain.model.WorkerMonitor

interface AdminRepository {
    /** Get all complaints with optional filters */
    suspend fun getAllComplaints(
        status: String? = null,
        priority: String? = null,
        department: String? = null
    ): Result<List<Complaint>>

    /** Assign a worker to a complaint */
    suspend fun assignWorker(
        complaintId: String,
        workerId: String,
        deadline: String? = null,
        priority: String? = null,
        note: String? = null
    ): Result<Complaint>

    /** Change complaint priority */
    suspend fun updatePriority(complaintId: String, priority: String): Result<Unit>

    /** Post admin comment on complaint */
    suspend fun postAdminComment(complaintId: String, message: String): Result<Unit>

    /** Escalate complaint to higher authority */
    suspend fun escalateComplaint(
        complaintId: String,
        reason: String,
        escalateTo: String
    ): Result<Unit>

    /** Get all workers with optional filters */
    suspend fun getAllWorkers(
        department: String? = null,
        availability: String? = null
    ): Result<List<WorkerMonitor>>

    /** Get currently active workers (BUSY status with active tasks) */
    suspend fun getActiveWorkers(): Result<List<WorkerMonitor>>

    /** Get a specific worker's monitoring info */
    suspend fun getWorkerById(workerId: String): Result<WorkerMonitor>

    /** Get admin statistics dashboard */
    suspend fun getAdminStats(): Result<AdminStats>

    /** Get progress updates for a complaint */
    suspend fun getProgressUpdates(complaintId: String): Result<List<ProgressUpdate>>

    /** Get system audit logs */
    suspend fun getAuditLogs(): Result<List<com.citizenai.app.domain.model.AuditLog>>
}

/**
 * Admin dashboard statistics — calculated from real database records.
 */
data class AdminStats(
    val totalComplaints: Int,
    val pendingComplaints: Int,
    val inProgressComplaints: Int,
    val resolvedComplaints: Int,
    val criticalComplaints: Int,
    val delayedTasks: Int,
    val activeWorkers: Int,
    val totalWorkers: Int
)
