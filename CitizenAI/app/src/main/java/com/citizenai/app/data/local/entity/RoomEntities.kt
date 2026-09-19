package com.citizenai.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey val id: String,
    val complaintId: String,
    val citizenId: String,
    val citizenName: String,
    val issueType: String,
    val category: String,
    val description: String,
    val imageUrl: String?,
    val afterImageUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val status: String,
    val priority: String,
    val department: String,
    val assignedWorkerId: String?,
    val assignedWorkerName: String?,
    val aiConfidence: Float?,
    val reportedAt: Long,       // Epoch millis
    val updatedAt: Long,
    val resolvedAt: Long?,
    val workerNotes: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val type: String,
    val complaintId: String?,
    val isRead: Boolean,
    val createdAt: Long,
    val cachedAt: Long = System.currentTimeMillis()
)
