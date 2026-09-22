package com.citizenai.worker.data.repository

import android.util.Log
import com.citizenai.worker.BuildConfig
import com.citizenai.worker.data.local.SessionManager
import com.citizenai.worker.data.local.WorkerTaskDao
import com.citizenai.worker.data.local.WorkerTaskEntity
import com.citizenai.worker.data.remote.WorkerApiService
import com.citizenai.worker.data.remote.dto.ComplaintDto
import com.citizenai.worker.data.remote.dto.WorkerDashboardDto
import com.citizenai.worker.domain.model.StatusHistoryItem
import com.citizenai.worker.domain.model.TaskStatus
import com.citizenai.worker.domain.model.WorkReport
import com.citizenai.worker.domain.model.WorkerTask
import com.citizenai.worker.domain.model.WorkerUser
import com.citizenai.worker.domain.repository.WorkerRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerRepositoryImpl @Inject constructor(
    private val apiService: WorkerApiService,
    private val taskDao: WorkerTaskDao,
    private val sessionManager: SessionManager,
    private val gson: Gson
) : WorkerRepository {

    override fun getTasksFlow(): Flow<List<WorkerTask>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain(gson) }
        }
    }

    private fun resolveImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        val base = BuildConfig.API_BASE_URL.removeSuffix("/api/").removeSuffix("/")

        val isLocalHost = trimmed.contains("localhost") ||
                trimmed.contains("127.0.0.1") ||
                trimmed.contains("10.0.2.2") ||
                trimmed.contains(":8000") ||
                trimmed.contains(":5000")

        if ((trimmed.startsWith("http://") || trimmed.startsWith("https://")) && !isLocalHost && !trimmed.contains("/uploads/")) {
            Log.d("WorkerRepository", "[IMAGE URL] Using remote URL: $trimmed")
            return trimmed
        }

        if (trimmed.contains("/uploads/")) {
            val filename = trimmed.substringAfter("/uploads/").trimStart('/')
            val resolved = "$base/uploads/$filename"
            Log.d("WorkerRepository", "[IMAGE URL] Resolved local upload: $resolved")
            return resolved
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }

        val clean = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        val resolved = "$base$clean"
        Log.d("WorkerRepository", "[IMAGE URL] Resolved relative path: $resolved")
        return resolved
    }

    private fun mapDtoToTask(dto: ComplaintDto): WorkerTask {
        val historyList = dto.statusHistory?.map {
            StatusHistoryItem(
                status = it.status ?: "",
                message = it.message,
                note = it.note,
                updatedBy = it.updatedBy,
                actorType = it.actorType,
                actorId = it.actorId,
                timestamp = it.timestamp
            )
        } ?: emptyList()

        val rawStatus = dto.status ?: "WORKER_ASSIGNED"

        return WorkerTask(
            id = dto.id ?: dto.complaintId ?: "",
            complaintId = dto.complaintId ?: dto.id ?: "",
            title = dto.title ?: dto.category ?: "Assigned Work",
            category = dto.category ?: "General",
            description = dto.description ?: "",
            priority = dto.priority ?: "MEDIUM",
            status = TaskStatus.fromString(rawStatus),
            statusRaw = rawStatus,
            progressPercentage = dto.progressPercentage ?: 0,
            address = dto.address,
            latitude = dto.latitude,
            longitude = dto.longitude,
            beforePhotoUrl = resolveImageUrl(dto.imageUrl),
            afterPhotoUrl = resolveImageUrl(dto.afterImageUrl ?: dto.completionPhotoUrl),
            workerNotes = dto.workerNotes,
            progressNote = dto.progressNote,
            department = dto.department,
            assignedWorkerId = dto.assignedWorkerId,
            assignedWorkerName = dto.assignedWorkerName,
            citizenName = dto.citizenName,
            citizenPhone = dto.citizenPhone,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            statusHistory = historyList
        )
    }

    override suspend fun fetchWorkerTasks(): Result<List<WorkerTask>> {
        return try {
            val response = apiService.getWorkerTasks()
            val body = response.body()
            val dtoList = body?.data ?: body?.tasks ?: body?.complaints
            if (response.isSuccessful && dtoList != null) {
                val tasks = dtoList.map { mapDtoToTask(it) }

                // Cache active tasks in Room without clearing previous entries
                taskDao.insertTasks(tasks.map { WorkerTaskEntity.fromDomain(it, gson) })

                Log.i("WorkerRepository", "[WORKER TASK] Loaded: ${tasks.size} active tasks")
                Result.success(tasks)
            } else {
                val cached = runCatching { taskDao.getAllTasksList() }.getOrDefault(emptyList())
                if (cached.isNotEmpty()) {
                    Log.i("WorkerRepository", "Serving ${cached.size} cached tasks after HTTP ${response.code()}")
                    Result.success(cached.map { it.toDomain(gson) })
                } else {
                    Result.failure(Exception("Failed to fetch tasks: ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            val cached = runCatching { taskDao.getAllTasksList() }.getOrDefault(emptyList())
            if (cached.isNotEmpty()) {
                Log.i("WorkerRepository", "Serving ${cached.size} cached tasks after network failure: ${e.message}")
                Result.success(cached.map { it.toDomain(gson) })
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun fetchTaskHistory(): Result<List<WorkerTask>> {
        return try {
            val response = apiService.getTaskHistory()
            val body = response.body()
            val dtoList = body?.data ?: body?.tasks ?: body?.complaints
            if (response.isSuccessful && dtoList != null) {
                val tasks = dtoList.map { mapDtoToTask(it) }

                // Merge completed/historical tasks into Room cache safely
                taskDao.insertTasks(tasks.map { WorkerTaskEntity.fromDomain(it, gson) })

                Log.i("WorkerRepository", "[WORKER TASK HISTORY] Loaded: ${tasks.size} history tasks")
                Result.success(tasks)
            } else {
                val cached = runCatching { taskDao.getAllTasksList() }.getOrDefault(emptyList())
                val historyCached = cached.filter {
                    it.status == "COMPLETED" || it.status == "RESOLVED" || it.status == "VERIFICATION_REQUIRED"
                }
                if (historyCached.isNotEmpty()) {
                    Result.success(historyCached.map { it.toDomain(gson) })
                } else {
                    Result.failure(Exception("Failed to fetch task history: ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            val cached = runCatching { taskDao.getAllTasksList() }.getOrDefault(emptyList())
            val historyCached = cached.filter {
                it.status == "COMPLETED" || it.status == "RESOLVED" || it.status == "VERIFICATION_REQUIRED"
            }
            if (historyCached.isNotEmpty()) {
                Result.success(historyCached.map { it.toDomain(gson) })
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getTaskById(taskId: String): Result<WorkerTask> {
        return try {
            val response = apiService.getTaskById(taskId)
            if (response.isSuccessful && response.body() != null) {
                val task = mapDtoToTask(response.body()!!)
                taskDao.insertTask(WorkerTaskEntity.fromDomain(task, gson))
                Result.success(task)
            } else {
                // Fallback to Room cache
                val cached = taskDao.getTaskById(taskId)
                if (cached != null) {
                    Result.success(cached.toDomain(gson))
                } else {
                    Result.failure(Exception("Task not found"))
                }
            }
        } catch (e: Exception) {
            val cached = taskDao.getTaskById(taskId)
            if (cached != null) {
                Result.success(cached.toDomain(gson))
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getTaskReport(taskId: String): Result<WorkReport> {
        return try {
            val response = apiService.getTaskReport(taskId)
            if (response.isSuccessful && response.body()?.data != null) {
                val d = response.body()!!.data!!
                val history = d.statusHistory?.map {
                    StatusHistoryItem(
                        status = it.status ?: "",
                        message = it.message,
                        note = it.note,
                        updatedBy = it.updatedBy,
                        actorType = it.actorType,
                        actorId = it.actorId,
                        timestamp = it.timestamp
                    )
                } ?: emptyList()

                val rawBeforeUrl = d.beforePhotoUrl ?: d.evidence?.before?.publicUrl ?: d.evidence?.before?.url
                val rawAfterUrl = d.afterPhotoUrl ?: d.evidence?.after?.publicUrl ?: d.evidence?.after?.url

                val report = WorkReport(
                    complaintId = d.complaintId ?: taskId,
                    issueCategory = d.issueCategory ?: "Issue",
                    description = d.description ?: "",
                    locationAddress = d.location?.address,
                    latitude = d.location?.latitude,
                    longitude = d.location?.longitude,
                    department = d.department,
                    assignedWorker = d.assignedWorker,
                    reportedDate = d.reportedDate,
                    startedAt = d.startedAt,
                    completionDate = d.completionDate,
                    beforePhotoUrl = resolveImageUrl(rawBeforeUrl),
                    afterPhotoUrl = resolveImageUrl(rawAfterUrl),
                    beforeUploadedAt = d.beforeUploadedAt ?: d.evidence?.before?.uploadedAt,
                    beforeUploadedByRole = d.beforeUploadedByRole ?: d.evidence?.before?.uploadedByRole,
                    afterUploadedAt = d.afterUploadedAt ?: d.evidence?.after?.uploadedAt,
                    afterUploadedByRole = d.afterUploadedByRole ?: d.evidence?.after?.uploadedByRole,
                    workerNotes = d.workerNotes,
                    statusHistory = history,
                    finalStatus = d.finalStatus ?: "COMPLETED"
                )
                Result.success(report)
            } else {
                Result.failure(Exception("Failed to load report"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptTask(taskId: String): Result<WorkerTask> {
        return try {
            val response = apiService.acceptTask(taskId)
            val body = response.body()?.data ?: response.body()?.tasks ?: response.body()?.complaints
            if (response.isSuccessful && body != null) {
                val task = mapDtoToTask(body)
                taskDao.insertTask(WorkerTaskEntity.fromDomain(task, gson))
                Log.i("WorkerRepository", "[WORKER STATUS] Updated: Task $taskId accepted")
                Result.success(task)
            } else {
                Result.failure(Exception("Accept task failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectTask(taskId: String, reason: String): Result<Boolean> {
        return try {
            val response = apiService.rejectTask(taskId, mapOf("reason" to reason))
            if (response.isSuccessful) {
                Log.i("WorkerRepository", "[WORKER STATUS] Updated: Task $taskId rejected")
                Result.success(true)
            } else {
                Result.failure(Exception("Reject failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startWork(taskId: String, beforeImageFile: File?): Result<WorkerTask> {
        return try {
            val response = apiService.startTask(taskId)
            val body = response.body()?.data ?: response.body()?.tasks ?: response.body()?.complaints
            if (response.isSuccessful && body != null) {
                val task = mapDtoToTask(body)
                taskDao.insertTask(WorkerTaskEntity.fromDomain(task, gson))
                Log.i("WorkerRepository", "[WORKER STATUS] Updated: Task $taskId started")
                Result.success(task)
            } else {
                Result.failure(Exception("Start work failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun submitProgress(
        taskId: String,
        progressPercentage: Int,
        note: String,
        photoFile: File?
    ): Result<WorkerTask> {
        return try {
            val pctBody = progressPercentage.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val noteBody = note.toRequestBody("text/plain".toMediaTypeOrNull())

            val photoPart = photoFile?.let { file ->
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("photo", file.name, requestFile)
            }

            val response = apiService.submitProgress(taskId, pctBody, noteBody, photoPart)
            val body = response.body()?.data ?: response.body()?.tasks ?: response.body()?.complaints
            if (response.isSuccessful && body != null) {
                val task = mapDtoToTask(body)
                taskDao.insertTask(WorkerTaskEntity.fromDomain(task, gson))
                Log.i("WorkerRepository", "[WORKER STATUS] Updated: Task $taskId progress updated to $progressPercentage%")
                Result.success(task)
            } else {
                Result.failure(Exception("Submit progress failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeTask(
        taskId: String,
        notes: String,
        afterImageFile: File
    ): Result<WorkerTask> {
        return try {
            val notesBody = notes.toRequestBody("text/plain".toMediaTypeOrNull())
            val requestFile = afterImageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val afterPart = MultipartBody.Part.createFormData("afterImage", afterImageFile.name, requestFile)

            val response = apiService.completeTask(taskId, notesBody, afterPart)
            val body = response.body()?.data ?: response.body()?.tasks ?: response.body()?.complaints
            if (response.isSuccessful && body != null) {
                val task = mapDtoToTask(body)
                taskDao.insertTask(WorkerTaskEntity.fromDomain(task, gson))
                Log.i("WorkerRepository", "[WORKER STATUS] Updated: Task $taskId completed")
                Result.success(task)
            } else {
                Result.failure(Exception("Complete task failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchWorkerProfile(): Result<WorkerUser> {
        return try {
            val response = apiService.getWorkerProfile()
            if (response.isSuccessful && response.body()?.data != null) {
                val u = response.body()!!.data!!
                val user = WorkerUser(
                    id = u.id ?: "",
                    workerId = u.workerId,
                    name = u.name ?: "Worker",
                    email = u.email,
                    phone = u.phone,
                    mobileNumber = u.mobileNumber ?: u.phone,
                    role = u.role ?: "WORKER",
                    firebaseUid = u.firebaseUid,
                    department = u.department,
                    departmentId = u.departmentId,
                    employeeId = u.employeeId,
                    accountStatus = u.accountStatus ?: "ACTIVE",
                    isActive = u.isActive ?: true,
                    lastOtpVerifiedAt = u.lastOtpVerifiedAt,
                    totalLogins = u.totalLogins ?: 0,
                    tasksCompleted = u.tasksCompleted ?: 0,
                    tasksInProgress = u.tasksInProgress ?: 0
                )
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to fetch worker profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchWorkerDashboard(): Result<WorkerDashboardDto> {
        return try {
            val response = apiService.getWorkerDashboard()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to fetch worker dashboard stats: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
