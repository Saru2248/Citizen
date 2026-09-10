package com.citizenai.app.domain.model

import java.time.Instant

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val complaintId: String?,
    val isRead: Boolean,
    val createdAt: Instant
)

enum class NotificationType {
    COMPLAINT_ASSIGNED,
    WORKER_ASSIGNED,
    WORK_STARTED,
    WORK_COMPLETED,
    VERIFICATION_REQUIRED,
    COMPLAINT_RESOLVED,
    COMPLAINT_REOPENED,
    GENERAL
}
