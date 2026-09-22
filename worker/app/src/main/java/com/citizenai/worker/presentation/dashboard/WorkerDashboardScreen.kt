package com.citizenai.worker.presentation.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.citizenai.worker.data.remote.SocketStatus
import com.citizenai.worker.domain.model.TaskStatus
import com.citizenai.worker.domain.model.WorkerTask
import com.citizenai.worker.presentation.components.PriorityBadge
import com.citizenai.worker.presentation.components.StatusBadge
import com.citizenai.worker.presentation.theme.*

@Composable
fun WorkerDashboardScreen(
    onTaskClick: (String) -> Unit,
    viewModel: WorkerDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
    ) {
        // ─── Header Section (Matching CitizenHomeScreen Header Gradient) ─────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(DeepCivicBlue, CivicBlueMedium)
                    )
                )
                .padding(horizontal = 20.dp)
                .padding(top = 52.dp, bottom = 24.dp)
        ) {
            Column {
                // Top row: greeting + department + socket connection bell
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val greeting = when (java.time.LocalTime.now().hour) {
                            in 0..11 -> "Good Morning,"
                            in 12..17 -> "Good Afternoon,"
                            else -> "Good Evening,"
                        }
                        Text(
                            text = greeting,
                            fontSize = 14.sp,
                            color = TextOnDarkSecondary
                        )
                        Text(
                            text = uiState.user?.name ?: "Field Worker",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Work,
                                contentDescription = null,
                                tint = CivicGreenLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = uiState.user?.department ?: "Public Works Department",
                                fontSize = 12.sp,
                                color = TextOnDarkSecondary
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    // Socket connection & refresh status surface
                    Surface(
                        onClick = { viewModel.refreshDashboardData() },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            val dotColor = when (uiState.socketStatus) {
                                SocketStatus.CONNECTED -> StatusResolved
                                SocketStatus.CONNECTING, SocketStatus.RECONNECTING -> StatusPending
                                else -> PriorityCritical
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(9.dp)
                                    .background(dotColor, CircleShape)
                                    .border(1.5.dp, DeepCivicBlue, CircleShape)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ─── Field Metrics Impact Card (Real MongoDB Data) ───────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WORKER PERFORMANCE METRICS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CivicGreenLight,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Live DB",
                                fontSize = 10.sp,
                                color = TextOnDarkSecondary
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Top Row: Total Logins & Total Assigned
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ImpactStat(
                                value = "${uiState.totalLogins}",
                                label = "Total Logins",
                                valueColor = Color.White
                            )
                            VerticalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.height(44.dp))
                            ImpactStat(
                                value = "${uiState.totalAssignedComplaints}",
                                label = "Total Assigned",
                                valueColor = CivicGreenLight
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        Spacer(Modifier.height(14.dp))

                        // Bottom Row: New, Pending, In Progress, Completed
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ImpactStatSmall(
                                value = "${uiState.newComplaints}",
                                label = "New",
                                valueColor = StatusNew
                            )
                            ImpactStatSmall(
                                value = "${uiState.pendingComplaints}",
                                label = "Pending",
                                valueColor = StatusPending
                            )
                            ImpactStatSmall(
                                value = "${uiState.inProgressComplaints}",
                                label = "In Progress",
                                valueColor = StatusInProgress
                            )
                            ImpactStatSmall(
                                value = "${uiState.completedComplaints}",
                                label = "Completed",
                                valueColor = StatusResolved
                            )
                        }
                    }
                }
            }
        }

        // ─── Recent Complaints Section (Phase 4 Metric #7) ─────────────────
        if (uiState.recentComplaints.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Complaints",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${uiState.recentComplaints.size} Recent",
                    fontSize = 12.sp,
                    color = TextTertiary,
                    fontWeight = FontWeight.Medium
                )
            }

            uiState.recentComplaints.forEach { task ->
                TaskCard(task = task, onClick = { onTaskClick(task.id) })
            }

            Spacer(Modifier.height(12.dp))
        }

        // ─── Active Tasks Header ──────────────────────────────────────────────
        val activeTasks = uiState.tasks.filter {
            it.status != TaskStatus.COMPLETED &&
            it.status != TaskStatus.VERIFICATION_REQUIRED &&
            it.status != TaskStatus.RESOLVED &&
            it.status != TaskStatus.CANCELLED &&
            it.status != TaskStatus.REJECTED
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Assigned Field Work",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Surface(
                color = CivicGreenContainer,
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text = "${activeTasks.size} Active",
                    color = OnCivicGreenContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // ─── Tasks List ──────────────────────────────────────────────────────
        if (uiState.isLoading && uiState.tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CivicGreen)
            }
        } else if (activeTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CivicGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "All Tasks Completed!",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "You have no pending field assignments right now.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            activeTasks.forEach { task ->
                TaskCard(task = task, onClick = { onTaskClick(task.id) })
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ImpactStat(
    value: String,
    label: String,
    valueColor: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextOnDarkSecondary
        )
    }
}

@Composable
private fun ImpactStatSmall(
    value: String,
    label: String,
    valueColor: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextOnDarkSecondary
        )
    }
}

@Composable
fun TaskCard(
    task: WorkerTask,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail container matching ComplaintCard
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariantLight),
                contentAlignment = Alignment.Center
            ) {
                if (!task.beforePhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = task.beforePhotoUrl,
                        contentDescription = "Task Before Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = when (task.category.uppercase()) {
                            "POTHOLE", "ROAD_DAMAGE" -> Icons.Default.Construction
                            "GARBAGE", "SANITATION" -> Icons.Default.Delete
                            "STREETLIGHT", "ELECTRICAL" -> Icons.Default.Lightbulb
                            "WATER_LEAKAGE", "WATER" -> Icons.Default.WaterDrop
                            else -> Icons.Default.Build
                        },
                        contentDescription = null,
                        tint = CivicGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    PriorityBadge(priority = task.priority)
                }

                if (!task.address.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = task.address,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(status = task.status.displayName)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = task.complaintId,
                            fontSize = 11.sp,
                            color = TextTertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "${task.progressPercentage}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CivicGreen
                    )
                }

                Spacer(Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { task.progressPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = CivicGreen,
                    trackColor = SurfaceVariantLight
                )
            }

            Spacer(Modifier.width(4.dp))

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View Task Details",
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
