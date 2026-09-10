package com.citizenai.worker.presentation.tasks

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.citizenai.worker.domain.model.StatusHistoryItem
import com.citizenai.worker.domain.model.TaskStatus
import com.citizenai.worker.presentation.action.CompleteTaskDialog
import com.citizenai.worker.presentation.action.ProgressDialog
import com.citizenai.worker.presentation.components.PriorityBadge
import com.citizenai.worker.presentation.components.StatusBadge
import com.citizenai.worker.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onBackClick: () -> Unit,
    onViewReportClick: (String) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val task = uiState.task

    if (uiState.showProgressDialog) {
        ProgressDialog(
            onDismiss = { viewModel.closeProgressDialog() },
            onSubmit = { pct, note, file -> viewModel.submitProgress(pct, note, file) }
        )
    }

    if (uiState.showCompleteDialog) {
        CompleteTaskDialog(
            onDismiss = { viewModel.closeCompleteDialog() },
            onSubmit = { notes, file -> viewModel.completeTask(notes, file) }
        )
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = task?.complaintId ?: "Task Details",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        }
    ) { padding ->
        if (uiState.isLoading && task == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CivicGreen)
            }
        } else if (task == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Task details not found", color = TextSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Success / Error Banner
                    uiState.errorMessage?.let { err ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PriorityCriticalContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, null, tint = PriorityCritical, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(err, color = PriorityCritical, fontSize = 13.sp)
                            }
                        }
                    }

                    uiState.actionSuccessMessage?.let { msg ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CivicGreenContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = CivicGreenDark, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(msg, color = OnCivicGreenContainer, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // ─── Header Card (Matching ComplaintDetailScreen Header Card) ─────
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCardSurface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.complaintId,
                                        fontSize = 13.sp,
                                        color = CivicGreenLight,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = task.title,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                StatusBadge(status = task.status.displayName)
                            }

                            Spacer(Modifier.height(12.dp))

                            if (!task.address.isNullOrEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, tint = TextOnDarkSecondary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(task.address, fontSize = 13.sp, color = TextOnDarkSecondary, maxLines = 2)
                                }
                                Spacer(Modifier.height(8.dp))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Category, null, tint = TextOnDarkSecondary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(task.category, fontSize = 12.sp, color = TextOnDarkSecondary)
                                Spacer(Modifier.width(12.dp))
                                PriorityBadge(priority = task.priority)
                            }
                        }
                    }

                    // ─── Task Description Card ──────────────────────────────────────
                    SectionCard(title = "Task Description", icon = Icons.Default.Description) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, lineHeight = 22.sp)
                        )
                    }

                    // ─── Before / After Photo Comparison Cards ──────────────────────
                    if (!task.beforePhotoUrl.isNullOrEmpty() || !task.afterPhotoUrl.isNullOrEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!task.beforePhotoUrl.isNullOrEmpty()) {
                                PhotoCard(url = task.beforePhotoUrl, label = "Before (Citizen)", modifier = Modifier.weight(1f))
                            }
                            if (!task.afterPhotoUrl.isNullOrEmpty()) {
                                PhotoCard(url = task.afterPhotoUrl, label = "After (Worker)", modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // ─── Location & Map Direction Button ─────────────────────────────
                    if (!task.address.isNullOrEmpty()) {
                        SectionCard(title = "Location & Map", icon = Icons.Default.Map) {
                            Text(task.address, fontSize = 13.sp, color = TextSecondary)
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    val geoUri = if (task.latitude != null && task.longitude != null) {
                                        "geo:${task.latitude},${task.longitude}?q=${task.latitude},${task.longitude}(${task.title})"
                                    } else {
                                        "geo:0,0?q=${Uri.encode(task.address)}"
                                    }
                                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
                                    context.startActivity(mapIntent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, CivicGreen)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, tint = CivicGreen, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("OPEN IN GOOGLE MAPS", color = CivicGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    // ─── Status Progress Timeline (Matching Citizen Timeline) ────────
                    SectionCard(title = "Progress Timeline", icon = Icons.Default.Timeline) {
                        if (task.statusHistory.isEmpty()) {
                            Text("No timeline events recorded yet.", fontSize = 13.sp, color = TextTertiary)
                        } else {
                            task.statusHistory.forEachIndexed { index, event ->
                                TimelineItemRow(
                                    item = event,
                                    isLast = index == task.statusHistory.size - 1
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }

                // ─── Bottom Action Controls ──────────────────────────────────────────
                Surface(
                    color = SurfaceLight,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (task.status) {
                            TaskStatus.WORKER_ASSIGNED -> {
                                Button(
                                    onClick = { viewModel.acceptTask() },
                                    enabled = !uiState.isActionProcessing,
                                    colors = ButtonDefaults.buttonColors(containerColor = CivicGreen),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("ACCEPT TASK", fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Button(
                                    onClick = { viewModel.startWork() },
                                    enabled = !uiState.isActionProcessing,
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepCivicBlue),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("START WORK", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            TaskStatus.WORK_STARTED, TaskStatus.IN_PROGRESS -> {
                                OutlinedButton(
                                    onClick = { viewModel.openProgressDialog() },
                                    enabled = !uiState.isActionProcessing,
                                    border = BorderStroke(1.5.dp, CivicGreen),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("UPDATE PROGRESS", fontWeight = FontWeight.Bold, color = CivicGreen, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = { viewModel.openCompleteDialog() },
                                    enabled = !uiState.isActionProcessing,
                                    colors = ButtonDefaults.buttonColors(containerColor = CivicGreen),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("COMPLETE TASK", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                }
                            }

                            TaskStatus.COMPLETED, TaskStatus.VERIFICATION_REQUIRED, TaskStatus.RESOLVED -> {
                                Button(
                                    onClick = { onViewReportClick(task.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CivicGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Assessment, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("VIEW WORK REPORT", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            else -> {
                                Button(
                                    onClick = { viewModel.startWork() },
                                    enabled = !uiState.isActionProcessing,
                                    colors = ButtonDefaults.buttonColors(containerColor = CivicGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("START WORK", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(icon, contentDescription = null, tint = CivicGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 15.sp)
            }
            content()
        }
    }
}

@Composable
private fun PhotoCard(url: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
        AsyncImage(
            model = url,
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceVariantLight)
        )
    }
}

@Composable
private fun TimelineItemRow(item: StatusHistoryItem, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .background(CivicGreenContainer, CircleShape)
                    .border(1.5.dp, CivicGreen, CircleShape)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = CivicGreenDark, modifier = Modifier.size(14.dp))
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(CivicGreen)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 16.dp)) {
            Text(
                text = item.status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )
            val desc = item.message ?: item.note
            if (!desc.isNullOrBlank()) {
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
            item.timestamp?.let { ts ->
                Text(ts, fontSize = 11.sp, color = TextTertiary, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
