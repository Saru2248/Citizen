package com.citizenai.worker.domain.repository

import com.citizenai.worker.data.remote.dto.WorkerDashboardDto
import com.citizenai.worker.domain.model.WorkReport
import com.citizenai.worker.domain.model.WorkerTask
import com.citizenai.worker.domain.model.WorkerUser
import kotlinx.coroutines.flow.Flow
import java.io.File

interface WorkerRepository {
    fun getTasksFlow(): Flow<List<WorkerTask>>
    suspend fun fetchWorkerTasks(): Result<List<WorkerTask>>
    suspend fun fetchTaskHistory(): Result<List<WorkerTask>>
    suspend fun fetchWorkerDashboard(): Result<WorkerDashboardDto>
    suspend fun getTaskById(taskId: String): Result<WorkerTask>
    suspend fun getTaskReport(taskId: String): Result<WorkReport>
    suspend fun acceptTask(taskId: String): Result<WorkerTask>
    suspend fun rejectTask(taskId: String, reason: String): Result<Boolean>
    suspend fun startWork(taskId: String, beforeImageFile: File? = null): Result<WorkerTask>
    suspend fun submitProgress(taskId: String, progressPercentage: Int, note: String, photoFile: File? = null): Result<WorkerTask>
    suspend fun completeTask(taskId: String, notes: String, afterImageFile: File): Result<WorkerTask>
    suspend fun fetchWorkerProfile(): Result<WorkerUser>
}
