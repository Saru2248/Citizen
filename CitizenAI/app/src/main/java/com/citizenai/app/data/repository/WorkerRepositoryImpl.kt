package com.citizenai.app.data.repository

import com.citizenai.app.data.local.dao.ComplaintDao
import com.citizenai.app.data.remote.firebase.FirebaseStorageService
import com.citizenai.app.data.remote.firebase.FirestoreService
import com.citizenai.app.domain.model.IssueCategory
import com.citizenai.app.domain.model.Priority
import com.citizenai.app.domain.model.ProgressUpdate
import com.citizenai.app.domain.model.TaskStatus
import com.citizenai.app.domain.model.WorkerTask
import com.citizenai.app.domain.repository.WorkerRepository
import java.io.File
import java.time.Instant
import javax.inject.Inject

/**
 * WorkerRepositoryImpl — Firestore + Room worker task management without external Java REST backend.
 */
class WorkerRepositoryImpl @Inject constructor(
    private val complaintDao: ComplaintDao,
    private val firestoreService: FirestoreService,
    private val storageService: FirebaseStorageService,
    private val firebaseAuth: com.google.firebase.auth.FirebaseAuth
) : WorkerRepository {

    override suspend fun getWorkerTasks(): Result<List<WorkerTask>> {
        return getFallbackWorkerTasks()
    }

    private suspend fun getFallbackWorkerTasks(): Result<List<WorkerTask>> {
        val currentWorkerId = firebaseAuth.currentUser?.uid ?: ""
        val fsComplaints = firestoreService.getComplaints().getOrNull() ?: emptyList()
        val workerComplaints = fsComplaints.filter {
            it.assignedWorkerId == currentWorkerId || (currentWorkerId.isNotBlank() && it.assignedWorkerId == currentWorkerId)
        }

        if (workerComplaints.isNotEmpty()) {
            val converted = workerComplaints.map { complaint ->
                WorkerTask(
                    id                 = complaint.id,
                    complaintId        = complaint.complaintId,
                    issueType          = complaint.issueType,
                    category           = complaint.category,
                    address            = complaint.address,
                    latitude           = complaint.latitude,
                    longitude          = complaint.longitude,
                    priority           = complaint.priority,
                    status             = runCatching { TaskStatus.valueOf(complaint.status.name) }.getOrDefault(TaskStatus.PENDING),
                    beforeImageUrl     = complaint.imageUrl,
                    afterImageUrl      = complaint.afterImageUrl,
                    citizenDescription = complaint.description,
                    workerNotes        = complaint.workerNotes,
                    assignedAt         = complaint.reportedAt,
                    startedAt          = null,
                    completedAt        = complaint.resolvedAt,
                    deadline           = complaint.reportedAt.plusSeconds(86400),
                    progressPercentage = if (complaint.status.name in listOf("COMPLETED", "RESOLVED")) 100 else 25,
                    assignedBy         = "Admin",
                    assignedByName     = "System Administrator",
                    distanceKm         = 1.2
                )
            }
            return Result.success(converted)
        }

        val cachedComplaints = complaintDao.getAll().filter {
            it.assignedWorkerId == currentWorkerId
        }
        val converted = cachedComplaints.map { entity ->
            WorkerTask(
                id                 = entity.id,
                complaintId        = entity.complaintId,
                issueType          = entity.issueType,
                category           = runCatching { IssueCategory.valueOf(entity.category) }.getOrDefault(IssueCategory.OTHER),
                address            = entity.address,
                latitude           = entity.latitude,
                longitude          = entity.longitude,
                priority           = runCatching { Priority.valueOf(entity.priority) }.getOrDefault(Priority.NORMAL),
                status             = runCatching { TaskStatus.valueOf(entity.status) }.getOrDefault(TaskStatus.PENDING),
                beforeImageUrl     = entity.imageUrl,
                afterImageUrl      = entity.afterImageUrl,
                citizenDescription = entity.description,
                workerNotes        = entity.workerNotes,
                assignedAt         = Instant.ofEpochMilli(entity.reportedAt),
                startedAt          = null,
                completedAt        = entity.resolvedAt?.let { Instant.ofEpochMilli(it) },
                deadline           = Instant.ofEpochMilli(entity.reportedAt).plusSeconds(86400),
                progressPercentage = if (entity.status == "COMPLETED" || entity.status == "RESOLVED") 100 else 25,
                assignedBy         = "Admin",
                assignedByName     = "System Administrator",
                distanceKm         = 1.2
            )
        }
        return Result.success(converted)
    }

    override suspend fun getTaskById(taskId: String): Result<WorkerTask> {
        val tasks = getWorkerTasks().getOrDefault(emptyList())
        val task = tasks.find { it.id == taskId || it.complaintId == taskId }
        return if (task != null) Result.success(task)
        else Result.failure(Exception("Task not found"))
    }

    override suspend fun acceptTask(taskId: String): Result<Unit> {
        val taskRes = getTaskById(taskId)
        if (taskRes.isSuccess) {
            val updated = taskRes.getOrThrow().copy(
                status = TaskStatus.ACCEPTED,
                startedAt = Instant.now()
            )
            updateLocalTask(updated)
        }
        return Result.success(Unit)
    }

    override suspend fun rejectTask(taskId: String, reason: String): Result<Unit> {
        val taskRes = getTaskById(taskId)
        if (taskRes.isSuccess) {
            val updated = taskRes.getOrThrow().copy(
                status = TaskStatus.REJECTED,
                workerNotes = "Rejected: $reason"
            )
            updateLocalTask(updated)
        }
        return Result.success(Unit)
    }

    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Result<Unit> {
        val taskRes = getTaskById(taskId)
        if (taskRes.isSuccess) {
            val updated = taskRes.getOrThrow().copy(status = status)
            updateLocalTask(updated)
        }
        return Result.success(Unit)
    }

    override suspend fun submitProgress(
        taskId: String,
        percentage: Int,
        note: String,
        photoFile: File?,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit> {
        val taskRes = getTaskById(taskId)
        if (taskRes.isSuccess) {
            var photoUrl = taskRes.getOrThrow().afterImageUrl
            if (photoFile != null && photoFile.exists()) {
                val uploadRes = storageService.uploadImage(photoFile, "tasks/$taskId/progress_${System.currentTimeMillis()}.jpg")
                if (uploadRes.isSuccess) {
                    photoUrl = uploadRes.getOrThrow()
                }
            }
            val updated = taskRes.getOrThrow().copy(
                status = TaskStatus.IN_PROGRESS,
                progressPercentage = percentage,
                workerNotes = note,
                afterImageUrl = photoUrl ?: taskRes.getOrThrow().afterImageUrl
            )
            updateLocalTask(updated)
        }
        return Result.success(Unit)
    }

    override suspend fun completeTask(taskId: String, afterImageFile: File, notes: String): Result<Unit> {
        var afterImageUrl: String? = null
        if (afterImageFile.exists()) {
            val uploadRes = storageService.uploadImage(afterImageFile, "tasks/$taskId/after_image.jpg")
            if (uploadRes.isSuccess) {
                afterImageUrl = uploadRes.getOrThrow()
            }
        }
        val taskRes = getTaskById(taskId)
        if (taskRes.isSuccess) {
            val updated = taskRes.getOrThrow().copy(
                status = TaskStatus.COMPLETED,
                afterImageUrl = afterImageUrl ?: taskRes.getOrThrow().afterImageUrl,
                workerNotes = notes,
                completedAt = Instant.now(),
                progressPercentage = 100
            )
            updateLocalTask(updated)
        }
        return Result.success(Unit)
    }

    override suspend fun getTaskHistory(): Result<List<WorkerTask>> {
        val tasks = getWorkerTasks().getOrDefault(emptyList())
        return Result.success(tasks.filter { it.status == TaskStatus.COMPLETED || it.status == TaskStatus.REJECTED })
    }

    override suspend fun getProgressUpdates(complaintId: String): Result<List<ProgressUpdate>> {
        return Result.success(emptyList())
    }

    private suspend fun updateLocalTask(task: WorkerTask) {
        firestoreService.saveWorkerTask(task)

        try {
            val fsComplaints = firestoreService.getComplaints().getOrNull() ?: emptyList()
            val existingDoc = fsComplaints.find { it.id == task.id || it.complaintId == task.id || it.complaintId == task.complaintId }
            val nowMs = System.currentTimeMillis()
            val mappedStatus = when (task.status) {
                TaskStatus.PENDING -> com.citizenai.app.domain.model.ComplaintStatus.ASSIGNED
                TaskStatus.ACCEPTED -> com.citizenai.app.domain.model.ComplaintStatus.WORKER_ACCEPTED
                TaskStatus.IN_PROGRESS -> com.citizenai.app.domain.model.ComplaintStatus.IN_PROGRESS
                TaskStatus.COMPLETED -> com.citizenai.app.domain.model.ComplaintStatus.COMPLETED
                TaskStatus.REJECTED, TaskStatus.REOPENED -> com.citizenai.app.domain.model.ComplaintStatus.REOPENED
            }


            if (existingDoc != null) {
                val updatedHistory = existingDoc.statusHistory + com.citizenai.app.domain.model.StatusHistoryEntry(mappedStatus.name, nowMs)
                val updatedComplaint = existingDoc.copy(
                    status = mappedStatus,
                    afterImageUrl = task.afterImageUrl ?: existingDoc.afterImageUrl,
                    workerNotes = task.workerNotes ?: existingDoc.workerNotes,
                    updatedAt = java.time.Instant.now(),
                    resolvedAt = if (task.status == TaskStatus.COMPLETED) java.time.Instant.now() else existingDoc.resolvedAt,
                    statusHistory = updatedHistory
                )
                complaintDao.insert(updatedComplaint.toEntity())
                firestoreService.saveComplaint(updatedComplaint)
            } else {
                val cached = complaintDao.getById(task.id)
                if (cached != null) {
                    val updatedEntity = cached.copy(
                        status = mappedStatus.name,
                        afterImageUrl = task.afterImageUrl ?: cached.afterImageUrl,
                        workerNotes = task.workerNotes ?: cached.workerNotes,
                        updatedAt = System.currentTimeMillis()
                    )
                    complaintDao.insert(updatedEntity)
                    firestoreService.saveComplaint(updatedEntity.toDomain())
                }
            }
        } catch (e: Exception) {
            // Log fallback error
        }
    }

}

private fun getSeedWorkerTasks(): List<WorkerTask> = listOf(
    WorkerTask(
        id = "wrk_task_1",
        complaintId = "CIT-847291",
        issueType = "Large Pothole on Road",
        category = IssueCategory.POTHOLE,
        address = "Kranti Chowk, Chhatrapati Sambhajinagar",
        latitude = 19.8762,
        longitude = 75.3433,
        priority = Priority.HIGH,
        status = TaskStatus.PENDING,
        beforeImageUrl = "https://picsum.photos/400/300?random=1",
        afterImageUrl = null,
        citizenDescription = "Dangerous pothole near Kranti Chowk junction.",
        workerNotes = null,
        assignedAt = Instant.now().minusSeconds(3600),
        startedAt = null,
        completedAt = null,
        deadline = Instant.now().plusSeconds(86400),
        progressPercentage = 0,
        assignedBy = "admin_01",
        assignedByName = "System Administrator",
        distanceKm = 0.8
    )
)
