package com.citizenai.worker.presentation.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.citizenai.worker.domain.model.TaskStatus
import com.citizenai.worker.presentation.dashboard.TaskCard
import com.citizenai.worker.presentation.dashboard.WorkerDashboardViewModel
import com.citizenai.worker.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignedTasksScreen(
    onTaskClick: (String) -> Unit,
    viewModel: WorkerDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: New, 2: Pending, 3: In Progress, 4: Completed

    val filteredTasks = remember(uiState.tasks, searchQuery, selectedTab) {
        uiState.tasks.filter { task ->
            val matchesQuery = searchQuery.isBlank() ||
                    task.complaintId.contains(searchQuery, ignoreCase = true) ||
                    task.title.contains(searchQuery, ignoreCase = true) ||
                    task.category.contains(searchQuery, ignoreCase = true) ||
                    (task.address?.contains(searchQuery, ignoreCase = true) == true)

            val matchesTab = when (selectedTab) {
                1 -> task.status == TaskStatus.WORKER_ASSIGNED && task.progressPercentage == 0
                2 -> task.status == TaskStatus.WORKER_ASSIGNED && task.progressPercentage > 0
                3 -> task.status == TaskStatus.WORK_STARTED || task.status == TaskStatus.IN_PROGRESS
                4 -> task.status == TaskStatus.COMPLETED || task.status == TaskStatus.VERIFICATION_REQUIRED || task.status == TaskStatus.RESOLVED
                else -> true
            }

            matchesQuery && matchesTab
        }
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            Surface(color = SurfaceLight, shadowElevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Assigned Field Work",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by ID, Title, Address...", color = TextTertiary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CivicGreen,
                            focusedLabelColor = CivicGreen,
                            cursorColor = CivicGreen
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrollable Tab Row (All, New, Pending, In Progress, Completed)
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = SurfaceLight,
                        contentColor = CivicGreen,
                        edgePadding = 0.dp
                    ) {
                        val newCount = uiState.tasks.count { it.status == TaskStatus.WORKER_ASSIGNED && it.progressPercentage == 0 }
                        val pendingCount = uiState.tasks.count { it.status == TaskStatus.WORKER_ASSIGNED && it.progressPercentage > 0 }
                        val inProgCount = uiState.tasks.count { it.status == TaskStatus.WORK_STARTED || it.status == TaskStatus.IN_PROGRESS }
                        val compCount = uiState.tasks.count { it.status == TaskStatus.COMPLETED || it.status == TaskStatus.VERIFICATION_REQUIRED || it.status == TaskStatus.RESOLVED }

                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                            Text("All (${uiState.tasks.size})", modifier = Modifier.padding(vertical = 10.dp), color = if (selectedTab == 0) CivicGreen else TextSecondary, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                            Text("New ($newCount)", modifier = Modifier.padding(vertical = 10.dp), color = if (selectedTab == 1) CivicGreen else TextSecondary, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                            Text("Pending ($pendingCount)", modifier = Modifier.padding(vertical = 10.dp), color = if (selectedTab == 2) CivicGreen else TextSecondary, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                        }
                        Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) {
                            Text("In Progress ($inProgCount)", modifier = Modifier.padding(vertical = 10.dp), color = if (selectedTab == 3) CivicGreen else TextSecondary, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal)
                        }
                        Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }) {
                            Text("Completed ($compCount)", modifier = Modifier.padding(vertical = 10.dp), color = if (selectedTab == 4) CivicGreen else TextSecondary, fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No matching field tasks found", color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(task = task, onClick = { onTaskClick(task.id) })
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
