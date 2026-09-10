package com.citizenai.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.citizenai.app.presentation.admin.AdminScaffold
import com.citizenai.app.presentation.admin.complaints.AdminComplaintDetailScreen
import com.citizenai.app.presentation.auth.LoginScreen
import com.citizenai.app.presentation.auth.RegisterScreen
import com.citizenai.app.presentation.citizen.CitizenScaffold
import com.citizenai.app.presentation.complaints.detail.ComplaintDetailScreen
import com.citizenai.app.presentation.report.analysis.AIAnalysisScreen
import com.citizenai.app.presentation.report.capture.ReportCaptureScreen
import com.citizenai.app.presentation.report.details.ReportDetailsScreen
import com.citizenai.app.presentation.report.success.ReportSuccessScreen
import com.citizenai.app.presentation.splash.SplashScreen
import com.citizenai.app.presentation.worker.WorkerScaffold
import com.citizenai.app.presentation.worker.task.WorkerTaskDetailsScreen

/**
 * AppNavHost — root Navigation Compose graph.
 * Decides routing based on authentication state and user role.
 * Role-specific routes are protected; navigating to citizen routes as a worker redirects.
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = Routes.SPLASH
    ) {
        // ─── Splash ───────────────────────────────────────────────────────────

        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToCitizenHome = {
                    navController.navigate(Routes.CITIZEN_HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToWorkerHome = {
                    navController.navigate(Routes.WORKER_HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToAdminHome = {
                    navController.navigate(Routes.ADMIN_HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // ─── Auth ─────────────────────────────────────────────────────────────

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginAsCitizen = {
                    navController.navigate(Routes.CITIZEN_HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onLoginAsWorker = {
                    navController.navigate(Routes.WORKER_HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onLoginAsAdmin = {
                    navController.navigate(Routes.ADMIN_HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(Routes.CITIZEN_HOME) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ─── Citizen (Scaffold handles its own bottom nav) ───────────────────

        composable(Routes.CITIZEN_HOME) {
            CitizenScaffold(
                onNavigateToReport = {
                    navController.navigate(Routes.REPORT_CAPTURE)
                },
                onNavigateToComplaintDetail = { complaintId ->
                    navController.navigate(Routes.complaintDetail(complaintId))
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ─── Report Flow ──────────────────────────────────────────────────────

        composable(Routes.REPORT_CAPTURE) {
            ReportCaptureScreen(
                onImageCaptured = {
                    navController.navigate(Routes.REPORT_ANALYSIS)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.REPORT_ANALYSIS) {
            AIAnalysisScreen(
                onAnalysisComplete = {
                    navController.navigate(Routes.REPORT_DETAILS)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.REPORT_DETAILS) {
            ReportDetailsScreen(
                onSubmitSuccess = { complaintId ->
                    navController.navigate(Routes.reportSuccess(complaintId)) {
                        popUpTo(Routes.REPORT_CAPTURE) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route     = Routes.REPORT_SUCCESS,
            arguments = listOf(navArgument("complaintId") { type = NavType.StringType })
        ) { backStack ->
            val complaintId = backStack.arguments?.getString("complaintId") ?: ""
            ReportSuccessScreen(
                complaintId  = complaintId,
                onTrackReport = {
                    navController.navigate(Routes.complaintDetail(complaintId)) {
                        popUpTo(Routes.CITIZEN_HOME)
                    }
                },
                onBackToHome = {
                    navController.navigate(Routes.CITIZEN_HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ─── Complaint Detail (accessible from both citizen and worker) ───────

        composable(
            route     = Routes.COMPLAINT_DETAIL,
            arguments = listOf(navArgument("complaintId") { type = NavType.StringType })
        ) { backStack ->
            val complaintId = backStack.arguments?.getString("complaintId") ?: ""
            ComplaintDetailScreen(
                complaintId     = complaintId,
                onNavigateBack  = { navController.popBackStack() }
            )
        }

        // ─── Worker ───────────────────────────────────────────────────────────

        composable(Routes.WORKER_HOME) {
            WorkerScaffold(
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate(Routes.workerTask(taskId))
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route     = Routes.WORKER_TASK_DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStack ->
            val taskId = backStack.arguments?.getString("taskId") ?: ""
            WorkerTaskDetailsScreen(
                taskId          = taskId,
                onNavigateBack  = { navController.popBackStack() },
                onTaskCompleted = { navController.popBackStack() }
            )
        }

        // ─── Admin ────────────────────────────────────────────────────────────

        composable(Routes.ADMIN_HOME) {
            AdminScaffold(
                onNavigateToComplaintDetail = { complaintId ->
                    navController.navigate(Routes.adminComplaintDetail(complaintId))
                },
                onNavigateToWorkerDetail = { workerId ->
                    navController.navigate(Routes.adminWorkerDetail(workerId))
                },
                onNavigateToAddWorker = {
                    navController.navigate(Routes.ADMIN_ADD_WORKER)
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route     = Routes.ADMIN_COMPLAINT_DETAIL,
            arguments = listOf(navArgument("complaintId") { type = NavType.StringType })
        ) { backStack ->
            val complaintId = backStack.arguments?.getString("complaintId") ?: ""
            AdminComplaintDetailScreen(
                complaintId    = complaintId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADMIN_ADD_WORKER) {
            com.citizenai.app.presentation.admin.workers.AdminAddWorkerScreen(
                onNavigateBack = { navController.popBackStack() },
                onWorkerAdded = { navController.popBackStack() }
            )
        }
    }
}

