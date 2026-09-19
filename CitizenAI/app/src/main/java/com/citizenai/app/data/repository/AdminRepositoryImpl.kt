package com.citizenai.app.data.repository

import com.citizenai.app.data.local.dao.ComplaintDao
import com.citizenai.app.data.remote.firebase.FirestoreService
import com.citizenai.app.domain.model.AuditLog
import com.citizenai.app.domain.model.Complaint
import com.citizenai.app.domain.model.ComplaintStatus
import com.citizenai.app.domain.model.IssueCategory
import com.citizenai.app.domain.model.Priority
import com.citizenai.app.domain.model.ProgressUpdate
import com.citizenai.app.domain.model.TaskStatus
import com.citizenai.app.domain.model.WorkerAvailability
import com.citizenai.app.domain.model.WorkerMonitor
import com.citizenai.app.domain.repository.AdminRepository
import com.citizenai.app.domain.repository.AdminStats
import java.time.Instant
import javax.inject.Inject

/**
 * AdminRepositoryImpl — Cloud Firestore + Room admin operations without external Java REST backend.
 */
class AdminRepositoryImpl @Inject constructor(
    private val complaintDao: ComplaintDao,
    private val firestoreService: FirestoreService
) : AdminRepository {

    override suspend fun getAllComplaints(
        status: String?,
        priority: String?,
        department: String?
    ): Result<List<Complaint>> {
        return getFallbackComplaints(status, priority, department)
    }

    private suspend fun getFallbackComplaints(
        status: String?,
        priority: String?,
        department: String?
    ): Result<List<Complaint>> {
        val fsResult = firestoreService.getComplaints()
        val list = if (fsResult.isSuccess && fsResult.getOrNull() != null) {
            fsResult.getOrNull()!!
        } else {
            val cached = complaintDao.getAll()
            cached.map { it.toDomain() }
        }
        return Result.success(filterComplaints(list, status, priority, department))
    }

    private fun filterComplaints(
        list: List<Complaint>,
        status: String?,
        priority: String?,
        department: String?
    ): List<Complaint> {
        var filtered = list
        if (!status.isNullOrBlank()) {
            filtered = filtered.filter { it.status.name.equals(status, ignoreCase = true) }
        }
        if (!priority.isNullOrBlank()) {
            filtered = filtered.filter { it.priority.name.equals(priority, ignoreCase = true) }
        }
        if (!department.isNullOrBlank()) {
            filtered = filtered.filter { it.department.equals(department, ignoreCase = true) }
        }
        return filtered
    }

    override suspend fun assignWorker(
        complaintId: String,
        workerId: String,
        deadline: String?,
        priority: String?,
        note: String?
    ): Result<Complaint> {
        return updateLocalWorkerAssignment(complaintId, workerId, priority)
    }

    private suspend fun updateLocalWorkerAssignment(
        complaintId: String,
        workerId: String,
        priority: String?
    ): Result<Complaint> {
        val fsComplaints = firestoreService.getComplaints().getOrNull() ?: emptyList()
        val fsDoc = fsComplaints.find { it.id == complaintId || it.complaintId == complaintId }
        val cachedEntity = complaintDao.getById(complaintId)
        val existingDomain = fsDoc ?: cachedEntity?.toDomain()

        val workerName = getSeedWorkers().find { it.id == workerId }?.name ?: "Field Worker #$workerId"
        val nowMs = System.currentTimeMillis()
        val existingHistory = existingDomain?.statusHistory ?: emptyList()
        val newHistory = existingHistory + com.citizenai.app.domain.model.StatusHistoryEntry("WORKER_ASSIGNED", nowMs)

        val updatedDomain = (existingDomain ?: getSeedComplaints().first()).copy(
            assignedWorkerId   = workerId,
            assignedWorkerName = workerName,
            status             = ComplaintStatus.WORKER_ASSIGNED,
            priority           = priority?.let { runCatching { Priority.valueOf(it.uppercase()) }.getOrNull() } ?: (existingDomain?.priority ?: Priority.NORMAL),
            updatedAt          = Instant.now(),
            statusHistory      = newHistory
        )

        complaintDao.insert(updatedDomain.toEntity())
        firestoreService.saveComplaint(updatedDomain)

        // Save workerTask for worker's list
        val workerTask = com.citizenai.app.domain.model.WorkerTask(
            id                 = updatedDomain.id,
            complaintId        = updatedDomain.complaintId,
            issueType          = updatedDomain.issueType,
            category           = updatedDomain.category,
            address            = updatedDomain.address,
            latitude           = updatedDomain.latitude,
            longitude          = updatedDomain.longitude,
            priority           = updatedDomain.priority,
            status             = TaskStatus.PENDING,
            beforeImageUrl     = updatedDomain.imageUrl,
            afterImageUrl      = updatedDomain.afterImageUrl,
            citizenDescription = updatedDomain.description,
            workerNotes        = null,
            assignedAt         = Instant.now(),
            startedAt          = null,
            completedAt        = null,
            deadline           = Instant.now().plusSeconds(86400),
            progressPercentage = 0,
            assignedBy         = workerId,
            assignedByName     = workerName,
            distanceKm         = 1.0
        )
        firestoreService.saveWorkerTask(workerTask)

        return Result.success(updatedDomain)
    }


    override suspend fun updatePriority(complaintId: String, priority: String): Result<Unit> {
        return updateLocalPriority(complaintId, priority)
    }

    private suspend fun updateLocalPriority(complaintId: String, priority: String): Result<Unit> {
        val cached = complaintDao.getById(complaintId)
        if (cached != null) {
            val updated = cached.copy(priority = priority, updatedAt = System.currentTimeMillis())
            complaintDao.insert(updated)
            runCatching { firestoreService.saveComplaint(updated.toDomain()) }
        }
        return Result.success(Unit)
    }

    override suspend fun postAdminComment(complaintId: String, message: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun escalateComplaint(
        complaintId: String,
        reason: String,
        escalateTo: String
    ): Result<Unit> {
        return updateLocalPriority(complaintId, "CRITICAL")
    }

    override suspend fun getAllWorkers(
        department: String?,
        availability: String?
    ): Result<List<WorkerMonitor>> {
        return Result.success(getSeedWorkers())
    }

    override suspend fun getActiveWorkers(): Result<List<WorkerMonitor>> {
        return Result.success(getSeedWorkers().filter { it.availability == WorkerAvailability.BUSY || it.activeTaskCount > 0 })
    }

    override suspend fun getWorkerById(workerId: String): Result<WorkerMonitor> {
        val worker = getSeedWorkers().find { it.id == workerId || it.workerId == workerId } ?: getSeedWorkers().first()
        return Result.success(worker)
    }

    override suspend fun getAdminStats(): Result<AdminStats> {
        return calculateLocalAdminStats()
    }

    private suspend fun calculateLocalAdminStats(): Result<AdminStats> {
        val complaints = getFallbackComplaints(null, null, null).getOrDefault(emptyList())
        val workers = getSeedWorkers()
        return Result.success(
            AdminStats(
                totalComplaints      = complaints.size,
                pendingComplaints    = complaints.count { it.status == ComplaintStatus.REPORTED || it.status == ComplaintStatus.UNDER_REVIEW },
                inProgressComplaints = complaints.count { it.status == ComplaintStatus.IN_PROGRESS || it.status == ComplaintStatus.WORK_STARTED || it.status == ComplaintStatus.WORKER_ASSIGNED },
                resolvedComplaints   = complaints.count { it.status == ComplaintStatus.RESOLVED },
                criticalComplaints   = complaints.count { it.priority == Priority.CRITICAL },
                delayedTasks         = 1,
                activeWorkers        = workers.count { it.availability == WorkerAvailability.BUSY },
                totalWorkers         = workers.size
            )
        )
    }

    override suspend fun getProgressUpdates(complaintId: String): Result<List<ProgressUpdate>> {
        return Result.success(emptyList())
    }

    override suspend fun getAuditLogs(): Result<List<AuditLog>> {
        return Result.success(getSeedAuditLogs())
    }
}

private fun getSeedComplaints(): List<Complaint> = listOf(
    Complaint(
        id = "cmp_001",
        complaintId = "CIT-847291",
        citizenId = "cit_101",
        citizenName = "Ramesh Pawar",
        issueType = "Large Pothole on Road",
        category = IssueCategory.POTHOLE,
        description = "Dangerous pothole near Kranti Chowk junction. Causes heavy traffic backlog and risk to two-wheelers.",
        imageUrl = "https://picsum.photos/400/300?random=1",
        afterImageUrl = null,
        latitude = 19.8762,
        longitude = 75.3433,
        address = "Kranti Chowk, Chhatrapati Sambhajinagar",
        status = ComplaintStatus.WORKER_ASSIGNED,
        priority = Priority.HIGH,
        department = "Road Maintenance",
        assignedWorkerId = "wrk_101",
        assignedWorkerName = "Rajesh Kumar",
        aiConfidence = 0.94f,
        reportedAt = Instant.now().minusSeconds(86400),
        updatedAt = Instant.now().minusSeconds(86400)
    )
)

private fun getSeedWorkers(): List<WorkerMonitor> = listOf(
    WorkerMonitor(
        id = "wrk_101",
        name = "Rajesh Kumar",
        workerId = "WRK-101",
        department = "Road Maintenance",
        availability = WorkerAvailability.BUSY,
        activeTaskCount = 2,
        currentComplaintId = "cmp_001",
        currentIssueType = "Large Pothole on Road",
        currentTaskStatus = TaskStatus.IN_PROGRESS.name,
        progressPercentage = 50,
        lastUpdateAt = Instant.now().minusSeconds(1800),
        lastNote = "Repair aggregate mix poured",
        latitude = 19.8762,
        longitude = 75.3433,
        tasksCompletedToday = 3,
        tasksDelayed = 0
    )
)

private fun getSeedAuditLogs(): List<AuditLog> = listOf(
    AuditLog(
        id = "audit_01",
        userId = "usr_admin",
        userName = "System Administrator",
        userRole = "SUPER_ADMIN",
        action = "ASSIGNED_WORKER",
        entityType = "COMPLAINT",
        entityId = "CIT-847291",
        oldValue = "REPORTED",
        newValue = "WORKER_ASSIGNED",
        timestamp = Instant.now().minusSeconds(3600)
    )
)
