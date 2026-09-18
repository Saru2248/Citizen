package com.citizenai.worker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.citizenai.worker.domain.model.StatusHistoryItem
import com.citizenai.worker.domain.model.TaskStatus
import com.citizenai.worker.domain.model.WorkerTask

@Entity(tableName = "worker_tasks")
data class WorkerTaskEntity(
    @PrimaryKey val id: String,
    val complaintId: String,
    val title: String,
    val category: String,
    val description: String,
    val priority: String,
    val status: String,
    val progressPercentage: Int,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val beforePhotoUrl: String?,
    val afterPhotoUrl: String?,
    val workerNotes: String?,
    val progressNote: String?,
    val department: String?,
    val assignedWorkerId: String?,
    val assignedWorkerName: String?,
    val citizenName: String?,
    val citizenPhone: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val statusHistoryJson: String?
) {
    fun toDomain(gson: com.google.gson.Gson): WorkerTask {
        val historyList = if (!statusHistoryJson.isNullOrEmpty()) {
            try {
                val itemType = object : com.google.gson.reflect.TypeToken<List<StatusHistoryItem>>() {}.type
                gson.fromJson<List<StatusHistoryItem>>(statusHistoryJson, itemType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()

        return WorkerTask(
            id = id,
            complaintId = complaintId,
            title = title,
            category = category,
            description = description,
            priority = priority,
            status = TaskStatus.fromString(status),
            statusRaw = status,
            progressPercentage = progressPercentage,
            address = address,
            latitude = latitude,
            longitude = longitude,
            beforePhotoUrl = beforePhotoUrl,
            afterPhotoUrl = afterPhotoUrl,
            workerNotes = workerNotes,
            progressNote = progressNote,
            department = department,
            assignedWorkerId = assignedWorkerId,
            assignedWorkerName = assignedWorkerName,
            citizenName = citizenName,
            citizenPhone = citizenPhone,
            createdAt = createdAt,
            updatedAt = updatedAt,
            statusHistory = historyList
        )
    }

    companion object {
        fun fromDomain(task: WorkerTask, gson: com.google.gson.Gson): WorkerTaskEntity {
            return WorkerTaskEntity(
                id = task.id,
                complaintId = task.complaintId,
                title = task.title,
                category = task.category,
                description = task.description,
                priority = task.priority,
                status = task.statusRaw,
                progressPercentage = task.progressPercentage,
                address = task.address,
                latitude = task.latitude,
                longitude = task.longitude,
                beforePhotoUrl = task.beforePhotoUrl,
                afterPhotoUrl = task.afterPhotoUrl,
                workerNotes = task.workerNotes,
                progressNote = task.progressNote,
                department = task.department,
                assignedWorkerId = task.assignedWorkerId,
                assignedWorkerName = task.assignedWorkerName,
                citizenName = task.citizenName,
                citizenPhone = task.citizenPhone,
                createdAt = task.createdAt,
                updatedAt = task.updatedAt,
                statusHistoryJson = gson.toJson(task.statusHistory)
            )
        }
    }
}
