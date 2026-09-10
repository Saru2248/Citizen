package com.citizenai.app.presentation.worker.home

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citizenai.app.R
import com.citizenai.app.domain.model.WorkerTask
import com.citizenai.app.ui.components.PriorityBadge
import com.citizenai.app.ui.theme.*

@Composable
fun WorkerHomeScreen(
    onNavigateToTask: (String) -> Unit,
    viewModel: WorkerHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(DeepCivicBlue, CivicBlueMedium)))
                .padding(horizontal = 20.dp)
                .padding(top = 52.dp, bottom = 24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        val greeting = when (java.time.LocalTime.now().hour) {
                            in 0..11  -> stringResource(R.string.good_morning)
                            in 12..17 -> stringResource(R.string.good_afternoon)
                            else      -> stringResource(R.string.good_evening)
                        }
                        Text(greeting, fontSize = 14.sp, color = TextOnDarkSecondary)
                        Text(uiState.workerName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(44.dp).background(Color.White.copy(0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Notifications, null, tint = Color.White)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Today's Task Stats
                Text(
                    stringResource(R.string.todays_tasks_header),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CivicGreenLight,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TaskStatCard("${uiState.criticalCount}", stringResource(R.string.priority_critical_label), PriorityCritical,  modifier = Modifier.weight(1f))
                    TaskStatCard("${uiState.highCount}",     stringResource(R.string.priority_high_label),     PriorityHigh,       modifier = Modifier.weight(1f))
                    TaskStatCard("${uiState.normalCount}",   stringResource(R.string.priority_normal_label),   StatusInProgress,   modifier = Modifier.weight(1f))
                }
            }
        }

        // Your Tasks
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.your_tasks),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(12.dp))

        if (uiState.isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CivicGreen)
            }
        } else if (uiState.activeTasks.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircleOutline, null, tint = CivicGreenLight, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.no_pending_tasks), color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            uiState.activeTasks.forEach { task ->
                WorkerTaskCard(task = task, onStartTask = { onNavigateToTask(task.id) })
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TaskStatCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f)),
        border = BorderStroke(1.dp, Color.White.copy(0.15f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = TextOnDarkSecondary)
        }
    }
}

@Composable
fun WorkerTaskCard(task: WorkerTask, onStartTask: () -> Unit) {
    val kmAwayLabel = stringResource(R.string.km_away)
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.issueType, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Spacer(Modifier.height(3.dp))
                    Text(task.complaintId, fontSize = 12.sp, color = CivicGreen, fontWeight = FontWeight.SemiBold)
                }
                PriorityBadge(task.priority)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text(
                    task.address,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            task.distanceKm?.let { km ->
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NearMe, null, tint = TextTertiary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("${String.format("%.1f", km)} $kmAwayLabel", fontSize = 12.sp, color = TextTertiary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onStartTask,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)
            ) {
                Text(stringResource(R.string.view_task), color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
