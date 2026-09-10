package com.citizenai.app.presentation.admin

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citizenai.app.R
import com.citizenai.app.presentation.admin.complaints.AdminComplaintListScreen
import com.citizenai.app.presentation.admin.dashboard.AdminDashboardScreen
import com.citizenai.app.presentation.admin.liveops.LiveOpsScreen
import com.citizenai.app.presentation.admin.workers.AdminWorkerListScreen
import com.citizenai.app.ui.theme.*

private enum class AdminTab(val icon: ImageVector) {
    DASHBOARD(Icons.Default.Dashboard),
    COMPLAINTS(Icons.Default.Assignment),
    WORKERS(Icons.Default.People),
    LIVE_OPS(Icons.Default.Sensors),
}

/** Returns the localised label for this admin tab. Must be called inside a Composable. */
@Composable
private fun AdminTab.label(): String = when (this) {
    AdminTab.DASHBOARD  -> stringResource(R.string.tab_dashboard)
    AdminTab.COMPLAINTS -> stringResource(R.string.tab_complaints)
    AdminTab.WORKERS    -> stringResource(R.string.tab_workers)
    AdminTab.LIVE_OPS   -> stringResource(R.string.tab_live_ops)
}

@Composable
fun AdminScaffold(
    onNavigateToComplaintDetail: (String) -> Unit,
    onNavigateToWorkerDetail: (String) -> Unit,
    onNavigateToAddWorker: () -> Unit = {},
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(AdminTab.DASHBOARD) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor  = SurfaceLight,
                tonalElevation  = 8.dp
            ) {
                AdminTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick  = { selectedTab = tab },
                        icon     = {
                            Icon(tab.icon, contentDescription = tab.label())
                        },
                        label    = {
                            Text(
                                tab.label(),
                                fontSize   = 10.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors   = NavigationBarItemDefaults.colors(
                            selectedIconColor   = CivicGreen,
                            selectedTextColor   = CivicGreen,
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary,
                            indicatorColor      = CivicGreenContainer
                        )
                    )
                }
            }
        },
        containerColor = BackgroundLight
    ) { padding ->
        when (selectedTab) {
            AdminTab.DASHBOARD ->
                AdminDashboardScreen(
                    modifier                    = Modifier.padding(padding),
                    onNavigateToComplaintDetail = onNavigateToComplaintDetail,
                    onLogout                    = onLogout
                )
            AdminTab.COMPLAINTS ->
                AdminComplaintListScreen(
                    modifier                    = Modifier.padding(padding),
                    onNavigateToComplaintDetail = onNavigateToComplaintDetail
                )
            AdminTab.WORKERS ->
                AdminWorkerListScreen(
                    modifier                 = Modifier.padding(padding),
                    onNavigateToWorkerDetail = onNavigateToWorkerDetail,
                    onNavigateToAddWorker    = onNavigateToAddWorker
                )
            AdminTab.LIVE_OPS ->
                LiveOpsScreen(
                    modifier                    = Modifier.padding(padding),
                    onNavigateToComplaintDetail = onNavigateToComplaintDetail
                )
        }
    }
}
