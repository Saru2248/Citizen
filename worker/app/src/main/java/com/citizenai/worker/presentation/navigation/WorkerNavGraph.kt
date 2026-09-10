package com.citizenai.worker.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.citizenai.worker.presentation.auth.WorkerLoginScreen
import com.citizenai.worker.presentation.dashboard.WorkerDashboardScreen
import com.citizenai.worker.presentation.profile.WorkerProfileScreen
import com.citizenai.worker.presentation.report.WorkReportScreen
import com.citizenai.worker.presentation.tasks.AssignedTasksScreen
import com.citizenai.worker.presentation.tasks.TaskDetailScreen

@Composable
fun WorkerNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Tasks.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Login.route) {
                WorkerLoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                WorkerDashboardScreen(
                    onTaskClick = { taskId ->
                        navController.navigate(Screen.TaskDetail.createRoute(taskId))
                    }
                )
            }

            composable(Screen.Tasks.route) {
                AssignedTasksScreen(
                    onTaskClick = { taskId ->
                        navController.navigate(Screen.TaskDetail.createRoute(taskId))
                    }
                )
            }

            composable(Screen.TaskDetail.route) {
                TaskDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onViewReportClick = { taskId ->
                        navController.navigate(Screen.WorkReport.createRoute(taskId))
                    }
                )
            }

            composable(Screen.WorkReport.route) {
                WorkReportScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Profile.route) {
                WorkerProfileScreen(
                    onLoggedOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
