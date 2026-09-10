package com.citizenai.worker.domain.model

enum class TaskStatus(val displayName: String) {
    SUBMITTED("Submitted"),
    DEPARTMENT_ASSIGNED("Department Assigned"),
    WORKER_ASSIGNED("Worker Assigned"),
    WORK_STARTED("Work Started"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    VERIFICATION_REQUIRED("Verification Required"),
    RESOLVED("Resolved"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    companion object {
        fun fromString(value: String?): TaskStatus {
            if (value == null) return WORKER_ASSIGNED
            return when (value.uppercase()) {
                "SUBMITTED" -> SUBMITTED
                "DEPARTMENT_ASSIGNED" -> DEPARTMENT_ASSIGNED
                "WORKER_ASSIGNED", "WORKER_ACCEPTED", "ACCEPTED" -> WORKER_ASSIGNED
                "WORK_STARTED" -> WORK_STARTED
                "IN_PROGRESS", "PROGRESS_UPDATE", "WIP" -> IN_PROGRESS
                "COMPLETED", "WORK_COMPLETED" -> COMPLETED
                "VERIFICATION_REQUIRED" -> VERIFICATION_REQUIRED
                "RESOLVED" -> RESOLVED
                "REJECTED" -> REJECTED
                "CANCELLED" -> CANCELLED
                else -> WORKER_ASSIGNED
            }
        }
    }
}
