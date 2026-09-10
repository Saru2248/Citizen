package com.citizenai.app.navigation

/**
 * All navigation routes for the Citizen AI application.
 * Routes are organized by feature area and role.
 * String constants prevent typos in navigation calls.
 */
object Routes {
    // Auth flow
    const val SPLASH   = "splash"
    const val LOGIN    = "login"
    const val REGISTER = "register"

    // Citizen
    const val CITIZEN_HOME    = "citizen_home"
    const val REPORT_CAPTURE  = "report_capture"
    const val REPORT_ANALYSIS = "report_analysis"
    const val REPORT_DETAILS  = "report_details"
    const val REPORT_SUCCESS  = "report_success/{complaintId}"
    const val MAP             = "map"
    const val COMPLAINTS_LIST = "complaints"
    const val COMPLAINT_DETAIL = "complaint_detail/{complaintId}"
    const val NOTIFICATIONS   = "notifications"
    const val CITIZEN_PROFILE = "citizen_profile"

    // Worker
    const val WORKER_HOME        = "worker_home"
    const val WORKER_TASK_DETAIL = "worker_task/{taskId}"
    const val WORKER_HISTORY     = "worker_history"
    const val WORKER_PROFILE     = "worker_profile"

    // Admin
    const val ADMIN_HOME             = "admin_home"
    const val ADMIN_COMPLAINT_DETAIL = "admin_complaint/{complaintId}"
    const val ADMIN_WORKER_DETAIL    = "admin_worker/{workerId}"
    const val ADMIN_ADD_WORKER       = "admin_add_worker"

    // Helper functions to build routes with args
    fun reportSuccess(complaintId: String)        = "report_success/$complaintId"
    fun complaintDetail(complaintId: String)      = "complaint_detail/$complaintId"
    fun workerTask(taskId: String)                = "worker_task/$taskId"
    fun adminComplaintDetail(complaintId: String) = "admin_complaint/$complaintId"
    fun adminWorkerDetail(workerId: String)       = "admin_worker/$workerId"
}
