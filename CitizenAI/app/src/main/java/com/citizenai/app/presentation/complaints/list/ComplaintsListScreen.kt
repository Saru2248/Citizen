package com.citizenai.app.presentation.complaints.list

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citizenai.app.presentation.citizen.home.ComplaintCard
import com.citizenai.app.ui.theme.*

import androidx.compose.ui.res.stringResource
import com.citizenai.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintsListScreen(
    onNavigateToComplaintDetail: (String) -> Unit,
    viewModel: ComplaintsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = listOf(
        stringResource(R.string.filter_all),
        stringResource(R.string.stat_pending_label),
        stringResource(R.string.stat_in_progress_label),
        stringResource(R.string.stat_resolved)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_reports_title), fontWeight = FontWeight.Bold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = SurfaceLight,
                contentColor = CivicGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                        color = CivicGreen,
                        width = 40.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.onTabSelected(index) },
                        text = {
                            Text(
                                title,
                                fontSize = 13.sp,
                                fontWeight = if (uiState.selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = CivicGreen,
                        unselectedContentColor = TextSecondary
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CivicGreen)
                }
            } else {
                val filtered = viewModel.getFilteredComplaints(uiState.selectedTab)
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, null, tint = TextTertiary, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.no_reports_category), color = TextTertiary)
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(filtered, key = { it.id }) { complaint ->
                            ComplaintCard(
                                complaint = complaint,
                                onClick = { onNavigateToComplaintDetail(complaint.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
