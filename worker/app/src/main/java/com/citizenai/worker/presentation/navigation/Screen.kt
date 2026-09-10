package com.citizenai.worker.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Tasks : Screen("tasks")
    object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: String) = "task_detail/$taskId"
    }
    object WorkReport : Screen("work_report/{taskId}") {
        fun createRoute(taskId: String) = "work_report/$taskId"
    }
    object Profile : Screen("profile")
}
