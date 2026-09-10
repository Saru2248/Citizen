package com.citizenai.app.presentation.worker

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.citizenai.app.R
import com.citizenai.app.presentation.worker.home.WorkerHomeScreen
import com.citizenai.app.presentation.worker.history.WorkerHistoryScreen
import com.citizenai.app.presentation.worker.profile.WorkerProfileScreen
import com.citizenai.app.ui.theme.*

private sealed class WorkerNavItem(val route: String, val icon: ImageVector) {
    object Home    : WorkerNavItem("worker_home_tab",    Icons.Default.Home)
    object History : WorkerNavItem("worker_history_tab", Icons.Default.History)
    object Profile : WorkerNavItem("worker_profile_tab", Icons.Default.Person)
}

/** Returns the localised label for this worker nav item. Must be called inside a Composable. */
@Composable
private fun WorkerNavItem.label(): String = when (this) {
    WorkerNavItem.Home    -> stringResource(R.string.nav_home)
    WorkerNavItem.History -> stringResource(R.string.nav_history)
    WorkerNavItem.Profile -> stringResource(R.string.nav_profile)
}

@Composable
fun WorkerScaffold(
    onNavigateToTaskDetail: (String) -> Unit,
    onLogout: () -> Unit
) {
    val innerNav = rememberNavController()
    val navBackStack by innerNav.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    val items = listOf(WorkerNavItem.Home, WorkerNavItem.History, WorkerNavItem.Profile)

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = SurfaceLight, tonalElevation = 8.dp) {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            innerNav.navigate(item.route) {
                                popUpTo(WorkerNavItem.Home.route) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, item.label(), modifier = Modifier.size(22.dp)) },
                        label = { Text(item.label(), fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CivicGreen,
                            selectedTextColor = CivicGreen,
                            unselectedIconColor = TextTertiary,
                            indicatorColor = CivicGreenContainer
                        )
                    )
                }
            }
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        NavHost(
            navController = innerNav,
            startDestination = WorkerNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(WorkerNavItem.Home.route) {
                WorkerHomeScreen(onNavigateToTask = onNavigateToTaskDetail)
            }
            composable(WorkerNavItem.History.route) {
                WorkerHistoryScreen()
            }
            composable(WorkerNavItem.Profile.route) {
                WorkerProfileScreen(onLogout = onLogout)
            }
        }
    }
}
