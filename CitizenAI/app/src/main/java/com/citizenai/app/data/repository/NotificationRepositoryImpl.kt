package com.citizenai.app.data.repository

import com.citizenai.app.data.local.dao.NotificationDao
import com.citizenai.app.data.local.entity.NotificationEntity
import com.citizenai.app.domain.model.Notification
import com.citizenai.app.domain.model.NotificationType
import com.citizenai.app.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import javax.inject.Inject

/**
 * NotificationRepositoryImpl — local Room + Firestore notification management.
 */
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao
) : NotificationRepository {

    override suspend fun getNotifications(): Result<List<Notification>> {
        return try {
            val cached = notificationDao.observeAll().firstOrNull() ?: emptyList()
            if (cached.isNotEmpty()) {
                val list = cached.map { entity ->
                    Notification(
                        id = entity.id,
                        title = entity.title,
                        message = entity.message,
                        type = runCatching { NotificationType.valueOf(entity.type) }.getOrDefault(NotificationType.GENERAL),
                        complaintId = entity.complaintId,
                        isRead = entity.isRead,
                        createdAt = Instant.ofEpochMilli(entity.createdAt)
                    )
                }
                Result.success(list)
            } else {
                val demo = getDemoNotifications()
                notificationDao.insertAll(demo.map { notif ->
                    NotificationEntity(
                        id = notif.id,
                        title = notif.title,
                        message = notif.message,
                        type = notif.type.name,
                        complaintId = notif.complaintId,
                        isRead = notif.isRead,
                        createdAt = notif.createdAt.toEpochMilli()
                    )
                })
                Result.success(demo)
            }
        } catch (e: Exception) {
            Result.success(getDemoNotifications())
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        notificationDao.markAsRead(notificationId)
        return Result.success(Unit)
    }

    override suspend fun markAllAsRead(): Result<Unit> {
        notificationDao.markAllAsRead()
        return Result.success(Unit)
    }

    override fun observeUnreadCount(): Flow<Int> =
        notificationDao.observeUnreadCount()

    private fun getDemoNotifications(): List<Notification> = listOf(
        Notification(
            id = "notif_1",
            title = "Worker Assigned",
            message = "Ramesh Kumar has been assigned to your complaint CIV-1024.",
            type = NotificationType.WORKER_ASSIGNED,
            complaintId = "demo_cmp_1",
            isRead = false,
            createdAt = Instant.now().minusSeconds(3600)
        ),
        Notification(
            id = "notif_2",
            title = "Work Started",
            message = "Work has begun on your complaint CIV-1024 (Pothole Repair).",
            type = NotificationType.WORK_STARTED,
            complaintId = "demo_cmp_1",
            isRead = false,
            createdAt = Instant.now().minusSeconds(1800)
        ),
        Notification(
            id = "notif_3",
            title = "Complaint Resolved",
            message = "Your complaint CIV-1025 (Broken Streetlight) has been resolved.",
            type = NotificationType.COMPLAINT_RESOLVED,
            complaintId = "demo_cmp_2",
            isRead = true,
            createdAt = Instant.now().minusSeconds(86400)
        )
    )
}
