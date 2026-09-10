package com.citizenai.app.presentation.citizen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.citizenai.app.R
import com.citizenai.app.presentation.citizen.home.CitizenHomeScreen
import com.citizenai.app.presentation.complaints.list.ComplaintsListScreen
import com.citizenai.app.presentation.map.MapScreen
import com.citizenai.app.presentation.notifications.NotificationsScreen
import com.citizenai.app.presentation.profile.ProfileScreen
import com.citizenai.app.ui.theme.*

private sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector
) {
    object Home    : BottomNavItem("home",    Icons.Default.Home)
    object Map     : BottomNavItem("map",     Icons.Default.Map)
    object Report  : BottomNavItem("report",  Icons.Default.AddCircle)
    object Reports : BottomNavItem("reports", Icons.Default.List)
    object Profile : BottomNavItem("profile", Icons.Default.Person)
}

/** Returns the localised label for this nav item. Must be called inside a Composable. */
@Composable
private fun BottomNavItem.label(): String = when (this) {
    BottomNavItem.Home    -> stringResource(R.string.nav_home)
    BottomNavItem.Map     -> stringResource(R.string.nav_map)
    BottomNavItem.Report  -> stringResource(R.string.nav_report)
    BottomNavItem.Reports -> stringResource(R.string.nav_reports)
    BottomNavItem.Profile -> stringResource(R.string.nav_profile)
}

/**
 * CitizenScaffold — manages the citizen bottom navigation and its inner screens.
 * Report action navigates out to the full report flow (handled by parent NavHost).
 */
@Composable
fun CitizenScaffold(
    onNavigateToReport: () -> Unit,
    onNavigateToComplaintDetail: (String) -> Unit,
    onLogout: () -> Unit
) {
    val innerNavController = rememberNavController()
    val navBackStack by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Map,
        BottomNavItem.Report,
        BottomNavItem.Reports,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceLight,
                tonalElevation = 8.dp
            ) {
                items.forEach { item ->
                    if (item is BottomNavItem.Report) {
                        // Center prominent action button
                        NavigationBarItem(
                            selected = false,
                            onClick = onNavigateToReport,
                            icon = {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .shadow(8.dp, CircleShape)
                                        .background(CivicGreen, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = item.label(),
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    item.label(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CivicGreen
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                innerNavController.navigate(item.route) {
                                    popUpTo(BottomNavItem.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label(),
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text(item.label(), fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CivicGreen,
                                selectedTextColor = CivicGreen,
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary,
                                indicatorColor = CivicGreenContainer
                            )
                        )
                    }
                }
            }
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                CitizenHomeScreen(
                    onNavigateToReport = onNavigateToReport,
                    onNavigateToComplaintDetail = onNavigateToComplaintDetail,
                    onNavigateToNotifications = {
                        innerNavController.navigate("notifications")
                    }
                )
            }
            composable("notifications") {
                NotificationsScreen(
                    onNavigateBack = { innerNavController.popBackStack() }
                )
            }
            composable(BottomNavItem.Map.route) {
                MapScreen(
                    onNavigateToComplaintDetail = onNavigateToComplaintDetail
                )
            }
            composable(BottomNavItem.Reports.route) {
                ComplaintsListScreen(
                    onNavigateToComplaintDetail = onNavigateToComplaintDetail
                )
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(onLogout = onLogout)
            }
        }
    }
}
