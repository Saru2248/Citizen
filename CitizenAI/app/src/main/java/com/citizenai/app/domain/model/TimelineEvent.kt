package com.citizenai.app.domain.model

import java.time.Instant

/**
 * TimelineEvent — one step in a complaint's progress timeline.
 * Rendered as a vertical timeline on ComplaintDetailScreen.
 *
 * IMPORTANT: Timeline events must come from the backend database.
 * The buildTimeline() helper below is only used as a UI fallback
 * when the backend is unavailable. Production data must come from
 * /api/complaints/{id}/timeline
 */
data class TimelineEvent(
    val id: String,
    val status: ComplaintStatus,
    val title: String,
    val description: String,
    val timestamp: Instant?,
    val completed: Boolean,
    val isCurrent: Boolean = false,
    val actorName: String? = null,   // Who triggered this event
    val actorRole: UserRole? = null
)

/**
 * Produces the canonical timeline for UI rendering.
 * Real timeline event timestamps are populated from backend statusHistory.
 */
fun buildTimelineFallback(currentStatus: ComplaintStatus): List<TimelineEvent> {
    val canonicalStages = listOf(
        ComplaintStatus.REPORTED to "Complaint Reported",
        ComplaintStatus.DEPARTMENT_ASSIGNED to "Department Assigned",
        ComplaintStatus.WORKER_ASSIGNED to "Worker Assigned",
        ComplaintStatus.WORK_STARTED to "Work Started",
        ComplaintStatus.IN_PROGRESS to "Work In Progress",
        ComplaintStatus.WORK_COMPLETED to "Work Completed"
    )

    val currentOrdinal = stageOrdinal(currentStatus)

    return canonicalStages.map { (status, title) ->
        val stageOrd = stageOrdinal(status)
        val isCompleted = stageOrd <= currentOrdinal
        val isCurrent = status == currentStatus || stageOrd == currentOrdinal

        TimelineEvent(
            id = status.name,
            status = status,
            title = title,
            description = timelineDescription(status),
            timestamp = null,
            completed = isCompleted,
            isCurrent = isCurrent
        )
    }
}

public fun stageOrdinal(status: ComplaintStatus): Int = when (status) {
    ComplaintStatus.REPORTED, ComplaintStatus.PENDING                      -> 1
    ComplaintStatus.AI_ANALYZED, ComplaintStatus.UNDER_REVIEW                             -> 1
    ComplaintStatus.DEPARTMENT_ASSIGNED, ComplaintStatus.ASSIGNED                         -> 2
    ComplaintStatus.WORKER_ASSIGNED, ComplaintStatus.WORKER_ACCEPTED                       -> 3
    ComplaintStatus.WORK_STARTED                                                          -> 4
    ComplaintStatus.IN_PROGRESS, ComplaintStatus.PROGRESS_UPDATE                           -> 5
    ComplaintStatus.WORK_COMPLETED, ComplaintStatus.COMPLETED, ComplaintStatus.RESOLVED   -> 6
    ComplaintStatus.ADMIN_REVIEW, ComplaintStatus.CITIZEN_VERIFICATION                    -> 6
    ComplaintStatus.REOPENED                                                              -> 5
    ComplaintStatus.REJECTED, ComplaintStatus.CANCELLED                                   -> 6
}

private fun timelineTitle(status: ComplaintStatus) = when (status) {
    ComplaintStatus.REPORTED, ComplaintStatus.PENDING -> "Complaint Reported"
    ComplaintStatus.AI_ANALYZED         -> "AI Analysis Complete"
    ComplaintStatus.UNDER_REVIEW        -> "Under Admin Review"
    ComplaintStatus.DEPARTMENT_ASSIGNED, ComplaintStatus.ASSIGNED -> "Department Assigned"
    ComplaintStatus.WORKER_ASSIGNED, ComplaintStatus.WORKER_ACCEPTED -> "Worker Assigned"
    ComplaintStatus.WORK_STARTED        -> "Work Started"
    ComplaintStatus.IN_PROGRESS, ComplaintStatus.PROGRESS_UPDATE -> "Work In Progress"
    ComplaintStatus.WORK_COMPLETED, ComplaintStatus.COMPLETED, ComplaintStatus.RESOLVED -> "Work Completed"
    ComplaintStatus.ADMIN_REVIEW        -> "Admin Review"
    ComplaintStatus.CITIZEN_VERIFICATION -> "Verification Required"
    ComplaintStatus.REOPENED            -> "Complaint Reopened"
    ComplaintStatus.REJECTED            -> "Complaint Rejected"
    ComplaintStatus.CANCELLED           -> "Complaint Cancelled"
}

private fun timelineDescription(status: ComplaintStatus) = when (status) {
    ComplaintStatus.REPORTED, ComplaintStatus.PENDING -> "Your complaint has been submitted successfully."
    ComplaintStatus.AI_ANALYZED         -> "AI has analyzed and classified the issue."
    ComplaintStatus.UNDER_REVIEW        -> "Admin is reviewing your complaint."
    ComplaintStatus.DEPARTMENT_ASSIGNED, ComplaintStatus.ASSIGNED -> "The complaint has been sent to the relevant department."
    ComplaintStatus.WORKER_ASSIGNED, ComplaintStatus.WORKER_ACCEPTED -> "A field worker has been assigned to your complaint."
    ComplaintStatus.WORK_STARTED        -> "The worker has started work at the location."
    ComplaintStatus.IN_PROGRESS, ComplaintStatus.PROGRESS_UPDATE -> "Work is currently in progress."
    ComplaintStatus.WORK_COMPLETED, ComplaintStatus.COMPLETED, ComplaintStatus.RESOLVED -> "The worker has completed the work."
    ComplaintStatus.ADMIN_REVIEW        -> "Admin is reviewing the completed work."
    ComplaintStatus.CITIZEN_VERIFICATION -> "Work is done. Please verify if the issue is resolved."
    ComplaintStatus.REOPENED            -> "The complaint has been reopened for further action."
    ComplaintStatus.REJECTED            -> "The complaint was rejected."
    ComplaintStatus.CANCELLED           -> "The complaint was cancelled."
}

/** Alias kept for backward compatibility with existing call sites. */
fun buildTimeline(currentStatus: ComplaintStatus): List<TimelineEvent> =
    buildTimelineFallback(currentStatus)
