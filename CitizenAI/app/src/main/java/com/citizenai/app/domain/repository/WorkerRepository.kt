package com.citizenai.app.domain.repository

import com.citizenai.app.domain.model.ProgressUpdate
import com.citizenai.app.domain.model.WorkerTask
import com.citizenai.app.domain.model.TaskStatus
import java.io.File

interface WorkerRepository {
    suspend fun getWorkerTasks(): Result<List<WorkerTask>>
    suspend fun getTaskById(taskId: String): Result<WorkerTask>
    suspend fun acceptTask(taskId: String): Result<Unit>
    suspend fun rejectTask(taskId: String, reason: String): Result<Unit>
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Result<Unit>
    suspend fun submitProgress(
        taskId: String,
        percentage: Int,
        note: String,
        photoFile: File?,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit>
    suspend fun completeTask(taskId: String, afterImageFile: File, notes: String): Result<Unit>
    suspend fun getTaskHistory(): Result<List<WorkerTask>>
    suspend fun getProgressUpdates(complaintId: String): Result<List<ProgressUpdate>>
}
