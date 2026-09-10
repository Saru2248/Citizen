package com.citizenai.app.presentation.admin.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citizenai.app.R
import com.citizenai.app.domain.model.Complaint
import com.citizenai.app.domain.model.Priority
import com.citizenai.app.ui.components.PriorityBadge
import com.citizenai.app.ui.theme.*

@Composable
fun AdminDashboardScreen(
    modifier: Modifier = Modifier,
    onNavigateToComplaintDetail: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(DeepCivicBlue, CivicBlueMedium)))
                .padding(horizontal = 20.dp)
                .padding(top = 52.dp, bottom = 28.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            stringResource(R.string.admin_dashboard_label),
                            fontSize = 12.sp,
                            color = TextOnDarkSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            uiState.adminName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(0.12f), CircleShape)
                    ) {
                        Icon(Icons.Default.Logout, stringResource(R.string.logout), tint = Color.White)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Stats strip
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    uiState.stats?.let { stats ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AdminStatChip(
                                "${stats.totalComplaints}",
                                stringResource(R.string.stat_total),
                                Color.White,
                                Modifier.weight(1f)
                            )
                            AdminStatChip(
                                "${stats.pendingComplaints}",
                                stringResource(R.string.stat_pending_label),
                                PriorityHigh,
                                Modifier.weight(1f)
                            )
                            AdminStatChip(
                                "${stats.inProgressComplaints}",
                                stringResource(R.string.stat_in_progress_label),
                                StatusInProgress,
                                Modifier.weight(1f)
                            )
                            AdminStatChip(
                                "${stats.resolvedComplaints}",
                                stringResource(R.string.stat_resolved),
                                StatusResolved,
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Alert Cards ───────────────────────────────────────────────────────
        uiState.stats?.let { stats ->
            if (stats.criticalComplaints > 0 || stats.delayedTasks > 0) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (stats.criticalComplaints > 0) {
                        AlertCard(
                            icon    = Icons.Default.Warning,
                            color   = PriorityCritical,
                            title   = stringResource(R.string.critical_complaints_fmt, stats.criticalComplaints),
                            message = stringResource(R.string.critical_complaints_desc)
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    if (stats.delayedTasks > 0) {
                        AlertCard(
                            icon    = Icons.Default.Schedule,
                            color   = PriorityHigh,
                            title   = stringResource(R.string.delayed_tasks_fmt, stats.delayedTasks),
                            message = stringResource(R.string.delayed_tasks_desc)
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Worker Summary ────────────────────────────────────────────────────
        uiState.stats?.let { stats ->
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.People,
                        null,
                        tint   = DeepCivicBlue,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.active_workers_fmt, stats.activeWorkers),
                            fontWeight = FontWeight.Bold,
                            color      = TextPrimary
                        )
                        Text(
                            stringResource(R.string.total_workers_fmt, stats.totalWorkers),
                            fontSize = 12.sp,
                            color    = TextSecondary
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = TextTertiary)
                }
            }
        }

        // ── Recent High Priority Complaints ───────────────────────────────────
        if (uiState.recentHighPriority.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.high_priority_complaints),
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
                color      = TextPrimary,
                modifier   = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(10.dp))

            uiState.recentHighPriority.forEach { complaint ->
                AdminComplaintCard(
                    complaint = complaint,
                    onClick   = { onNavigateToComplaintDetail(complaint.id) }
                )
            }
        }

        // ── Error State ───────────────────────────────────────────────────────
        uiState.error?.let { error ->
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors   = CardDefaults.cardColors(containerColor = PriorityCriticalContainer),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, null, tint = PriorityCritical)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            stringResource(R.string.connection_error),
                            fontWeight = FontWeight.SemiBold,
                            color      = PriorityCritical
                        )
                        Text(error, fontSize = 12.sp, color = PriorityCritical)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AdminStatChip(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f)),
        border   = BorderStroke(1.dp, Color.White.copy(0.18f))
    ) {
        Column(
            modifier              = Modifier.padding(10.dp),
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = TextOnDarkSecondary)
        }
    }
}

@Composable
private fun AlertCard(
    icon: ImageVector,
    color: Color,
    title: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = color.copy(0.08f)),
        border   = BorderStroke(1.dp, color.copy(0.3f))
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, color = color, fontSize = 14.sp)
                Text(message, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun AdminComplaintCard(
    complaint: Complaint,
    onClick: () -> Unit
) {
    Card(
        onClick    = onClick,
        modifier   = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape      = RoundedCornerShape(14.dp),
        colors     = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation  = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        complaint.complaintId,
                        fontSize     = 11.sp,
                        color        = CivicGreen,
                        fontWeight   = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        complaint.issueType,
                        fontWeight   = FontWeight.SemiBold,
                        fontSize     = 15.sp,
                        color        = TextPrimary
                    )
                }
                PriorityBadge(complaint.priority)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    null,
                    tint     = TextTertiary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    complaint.address,
                    fontSize = 12.sp,
                    color    = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(complaint.status.displayLabel())
                Spacer(Modifier.width(8.dp))
                if (complaint.assignedWorkerName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            null,
                            tint     = TextTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            complaint.assignedWorkerName,
                            fontSize = 11.sp,
                            color    = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = StatusInProgress.copy(0.12f)
    ) {
        Text(
            label,
            fontSize   = 11.sp,
            color      = StatusInProgress,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
