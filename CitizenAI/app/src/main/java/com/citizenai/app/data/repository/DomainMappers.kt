package com.citizenai.app.data.repository

import com.citizenai.app.data.local.entity.ComplaintEntity
import com.citizenai.app.data.remote.dto.ai.AIAnalysisResponse
import com.citizenai.app.data.remote.dto.auth.UserDto
import com.citizenai.app.data.remote.dto.complaint.ComplaintDto
import com.citizenai.app.data.remote.dto.complaint.CommentDto
import com.citizenai.app.data.remote.dto.complaint.TimelineEventDto
import com.citizenai.app.data.remote.dto.notification.NotificationDto
import com.citizenai.app.data.remote.dto.worker.ProgressUpdateDto
import com.citizenai.app.data.remote.dto.worker.WorkerMonitorDto
import com.citizenai.app.data.remote.dto.worker.WorkerTaskDto
import com.citizenai.app.domain.model.*
import java.time.Instant

/** Extension functions mapping API DTOs → Domain models. */

fun UserDto.toDomain(): User = User(
    id         = id,
    name       = name,
    email      = email,
    phone      = phone ?: "",
    role       = when (role.uppercase()) {
        "WORKER" -> UserRole.WORKER
        "ADMIN", "SUPERVISOR", "DEPARTMENT_ADMIN", "SUPER_ADMIN" -> UserRole.ADMIN
        else     -> UserRole.CITIZEN
    },
    adminLevel = adminLevel?.let {
        runCatching {
            when (it.uppercase()) {
                "SUPER_ADMIN"      -> AdminLevel.SUPER_ADMIN
                "DEPARTMENT_ADMIN" -> AdminLevel.DEPARTMENT_ADMIN
                "SUPERVISOR"       -> AdminLevel.SUPERVISOR
                else               -> null
            }
        }.getOrNull()
    } ?: run {
        // Infer adminLevel from role string if adminLevel field is missing
        when (role.uppercase()) {
            "SUPER_ADMIN"      -> AdminLevel.SUPER_ADMIN
            "DEPARTMENT_ADMIN" -> AdminLevel.DEPARTMENT_ADMIN
            "SUPERVISOR"       -> AdminLevel.SUPERVISOR
            else               -> null
        }
    },
    departmentId           = departmentId,
    avatarUrl              = avatarUrl,
    workerId               = workerId,
    department             = department,
    totalReports           = totalReports ?: 0,
    resolvedReports        = resolvedReports ?: 0,
    pendingReports         = pendingReports ?: 0,
    tasksCompleted         = tasksCompleted ?: 0,
    tasksInProgress        = tasksInProgress ?: 0,
    avgCompletionTimeHours = avgCompletionTimeHours
)

fun ComplaintDto.toDomain(): Complaint {
    val effectiveId = id ?: mongoId ?: complaintId ?: "unknown"
    val effectiveComplaintId = complaintId ?: effectiveId
    val reportedInstant = (reportedAt ?: createdAt)?.parseInstant() ?: Instant.now()
    val updatedInstant = (updatedAt ?: createdAt)?.parseInstant() ?: reportedInstant
    return Complaint(
        id                  = effectiveId,
        complaintId         = effectiveComplaintId,
        citizenId           = citizenId ?: "",
        citizenName         = citizenName ?: "Citizen",
        issueType           = issueType ?: category ?: "Civic Issue",
        category            = IssueCategory.fromString(category ?: ""),
        description         = description ?: "",
        imageUrl            = resolveFullImageUrl(imageUrl ?: photoUrl),
        afterImageUrl       = resolveFullImageUrl(afterImageUrl),
        latitude            = latitude ?: 0.0,
        longitude           = longitude ?: 0.0,
        address             = address ?: "",
        status              = runCatching { ComplaintStatus.valueOf((status ?: "REPORTED").uppercase()) }.getOrDefault(ComplaintStatus.REPORTED),
        priority            = runCatching { Priority.valueOf((priority ?: "NORMAL").uppercase()) }.getOrDefault(Priority.NORMAL),
        department          = department ?: "Public Works",
        departmentId        = departmentId,
        assignedWorkerId    = assignedWorkerId,
        assignedWorkerName  = assignedWorkerName,
        aiConfidence        = aiConfidence,
        reportedAt          = reportedInstant,
        updatedAt           = updatedInstant,
        deadline            = deadline?.parseInstant(),
        resolvedAt          = resolvedAt?.parseInstant(),
        workerNotes         = workerNotes,
        progressPercentage  = progressPercentage ?: 0,
        progressNote        = progressNote,
        distanceKm          = distanceKm
    )
}

fun ComplaintDto.toEntity(): ComplaintEntity {
    val effectiveId = id ?: mongoId ?: complaintId ?: "unknown"
    val effectiveComplaintId = complaintId ?: effectiveId
    val reportedInstant = (reportedAt ?: createdAt)?.parseInstant() ?: Instant.now()
    val updatedInstant = (updatedAt ?: createdAt)?.parseInstant() ?: reportedInstant
    return ComplaintEntity(
        id                 = effectiveId,
        complaintId        = effectiveComplaintId,
        citizenId          = citizenId ?: "",
        citizenName        = citizenName ?: "Citizen",
        issueType          = issueType ?: category ?: "Civic Issue",
        category           = category ?: "OTHER",
        description        = description ?: "",
        imageUrl           = imageUrl ?: photoUrl,
        afterImageUrl      = afterImageUrl,
        latitude           = latitude ?: 0.0,
        longitude          = longitude ?: 0.0,
        address            = address ?: "",
        status             = status ?: "SUBMITTED",
        priority           = priority ?: "NORMAL",
        department         = department ?: "Public Works",
        assignedWorkerId   = assignedWorkerId,
        assignedWorkerName = assignedWorkerName,
        aiConfidence       = aiConfidence,
        reportedAt         = reportedInstant.toEpochMilli(),
        updatedAt          = updatedInstant.toEpochMilli(),
        resolvedAt         = resolvedAt?.parseInstant()?.toEpochMilli(),
        workerNotes        = workerNotes
    )
}

fun ComplaintEntity.toDomain(): Complaint = Complaint(
    id                 = id,
    complaintId        = complaintId,
    citizenId          = citizenId,
    citizenName        = citizenName,
    issueType          = issueType,
    category           = IssueCategory.fromString(category),
    description        = description,
    imageUrl           = imageUrl,
    afterImageUrl      = afterImageUrl,
    latitude           = latitude,
    longitude          = longitude,
    address            = address,
    status             = runCatching { ComplaintStatus.valueOf(status.uppercase()) }.getOrDefault(ComplaintStatus.PENDING),
    priority           = runCatching { Priority.valueOf(priority.uppercase()) }.getOrDefault(Priority.NORMAL),
    department         = department,
    assignedWorkerId   = assignedWorkerId,
    assignedWorkerName = assignedWorkerName,
    aiConfidence       = aiConfidence,
    reportedAt         = Instant.ofEpochMilli(reportedAt),
    updatedAt          = Instant.ofEpochMilli(updatedAt),
    resolvedAt         = resolvedAt?.let { Instant.ofEpochMilli(it) },
    workerNotes        = workerNotes
)

fun Complaint.toEntity(): ComplaintEntity = ComplaintEntity(
    id                 = id,
    complaintId        = complaintId,
    citizenId          = citizenId,
    citizenName        = citizenName,
    issueType          = issueType,
    category           = category.name,
    description        = description,
    imageUrl           = imageUrl,
    afterImageUrl      = afterImageUrl,
    latitude           = latitude,
    longitude          = longitude,
    address            = address,
    status             = status.name,
    priority           = priority.name,
    department         = department,
    assignedWorkerId   = assignedWorkerId,
    assignedWorkerName = assignedWorkerName,
    aiConfidence       = aiConfidence,
    reportedAt         = reportedAt.toEpochMilli(),
    updatedAt          = updatedAt.toEpochMilli(),
    resolvedAt         = resolvedAt?.toEpochMilli(),
    workerNotes        = workerNotes
)

fun TimelineEventDto.toDomain(): TimelineEvent = TimelineEvent(
    id          = id,
    status      = runCatching { ComplaintStatus.valueOf(status.uppercase()) }.getOrDefault(ComplaintStatus.REPORTED),
    title       = title,
    description = description,
    timestamp   = timestamp?.parseInstant(),
    completed   = completed,
    actorName   = actorName,
    actorRole   = actorRole?.let {
        runCatching { UserRole.valueOf(it.uppercase()) }.getOrNull()
    }
)

fun CommentDto.toDomain(): Comment = Comment(
    id         = id,
    complaintId = complaintId,
    authorId   = authorId,
    authorName = authorName,
    authorRole = when (authorRole.uppercase()) {
        "WORKER" -> UserRole.WORKER
        "ADMIN"  -> UserRole.ADMIN
        else     -> UserRole.CITIZEN
    },
    message    = message,
    postedAt   = postedAt.parseInstant()
)

fun AIAnalysisResponse.toDomain(): AIAnalysisResult = AIAnalysisResult(
    issueType           = issueType,
    severity            = severity,
    department          = department,
    confidence          = confidence,
    suggestedCategory   = IssueCategory.fromString(issueType),
    suggestedDescription = suggestedDescription ?: ""
)

fun WorkerTaskDto.toDomain(): WorkerTask = WorkerTask(
    id                  = id,
    complaintId         = complaintId,
    issueType           = issueType,
    category            = IssueCategory.fromString(category),
    address             = address,
    latitude            = latitude,
    longitude           = longitude,
    priority            = runCatching { Priority.valueOf(priority.uppercase()) }.getOrDefault(Priority.NORMAL),
    status              = runCatching { TaskStatus.valueOf(status.uppercase()) }.getOrDefault(TaskStatus.PENDING),
    beforeImageUrl      = beforeImageUrl,
    afterImageUrl       = afterImageUrl,
    citizenDescription  = citizenDescription,
    workerNotes         = workerNotes,
    assignedAt          = assignedAt.parseInstant(),
    acceptedAt          = acceptedAt?.parseInstant(),
    startedAt           = startedAt?.parseInstant(),
    completedAt         = completedAt?.parseInstant(),
    deadline            = deadline?.parseInstant(),
    progressPercentage  = progressPercentage ?: 0,
    assignedBy          = assignedBy,
    assignedByName      = assignedByName,
    distanceKm          = distanceKm
)

fun ProgressUpdateDto.toDomain(): ProgressUpdate = ProgressUpdate(
    id                 = id,
    complaintId        = complaintId,
    taskId             = taskId,
    workerId           = workerId,
    workerName         = workerName,
    progressPercentage = progressPercentage,
    note               = note,
    photoUrl           = photoUrl,
    latitude           = latitude,
    longitude          = longitude,
    createdAt          = createdAt.parseInstant()
)

fun NotificationDto.toDomain(): Notification = Notification(
    id          = id,
    title       = title,
    message     = message,
    type        = runCatching { NotificationType.valueOf(type.uppercase()) }.getOrDefault(NotificationType.GENERAL),
    complaintId = complaintId,
    isRead      = isRead,
    createdAt   = createdAt.parseInstant()
)

fun WorkerMonitorDto.toDomain(): WorkerMonitor = WorkerMonitor(
    id                  = id,
    name                = name,
    workerId            = workerId,
    department          = department,
    availability        = runCatching {
        WorkerAvailability.valueOf(availability.uppercase())
    }.getOrDefault(WorkerAvailability.OFFLINE),
    activeTaskCount     = activeTaskCount,
    currentComplaintId  = currentComplaintId,
    currentIssueType    = currentIssueType,
    currentTaskStatus   = currentTaskStatus,
    progressPercentage  = progressPercentage,
    lastUpdateAt        = lastUpdateAt?.parseInstant(),
    lastNote            = lastNote,
    latitude            = latitude,
    longitude           = longitude,
    tasksCompletedToday = tasksCompletedToday,
    tasksDelayed        = tasksDelayed
)

/** Parse ISO 8601 string to Instant, falling back to now on failure. */
private fun String.parseInstant(): Instant = try {
    Instant.parse(this)
} catch (e: Exception) {
    Instant.now()
}

fun com.citizenai.app.data.remote.dto.admin.AuditLogDto.toDomain(): com.citizenai.app.domain.model.AuditLog =
    com.citizenai.app.domain.model.AuditLog(
        id         = id,
        userId     = userId,
        userName   = userName,
        userRole   = userRole,
        action     = action,
        entityType = entityType,
        entityId   = entityId,
        oldValue   = oldValue,
        newValue   = newValue,
        timestamp  = createdAt.parseInstant()
    )

private fun resolveFullImageUrl(rawUrl: String?): String? {
    if (rawUrl.isNullOrBlank()) return null
    if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
        return rawUrl
    }
    val baseUrl = com.citizenai.app.BuildConfig.API_BASE_URL.replace("/api/", "").replace("/api", "")
    return "$baseUrl/${rawUrl.removePrefix("/")}"
}


