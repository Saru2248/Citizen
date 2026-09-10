package com.citizenai.app.presentation.admin.complaints

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.citizenai.app.domain.model.Priority
import com.citizenai.app.domain.model.WorkerAvailability
import com.citizenai.app.domain.model.WorkerMonitor
import com.citizenai.app.ui.components.PriorityBadge
import com.citizenai.app.ui.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
private val dateFormatter  = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminComplaintDetailScreen(
    complaintId: String,
    onNavigateBack: () -> Unit,
    viewModel: AdminComplaintDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(complaintId) { viewModel.loadComplaint(complaintId) }

    // Success snackbar
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessages()
        }
    }

    // Worker Selection Dialog
    if (uiState.showWorkerDialog) {
        WorkerSelectionDialog(
            workers   = uiState.availableWorkers,
            onSelect  = { worker -> viewModel.assignWorker(worker.id) },
            onDismiss = viewModel::hideWorkerAssignment
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.complaint?.complaintId ?: "Complaint Detail",
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadComplaint(complaintId) }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = CivicGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CivicGreen)
            }
            return@Scaffold
        }

        val complaint = uiState.complaint ?: run {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Complaint not found", color = TextSecondary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // ── Success Banner ────────────────────────────────────────────────
            uiState.successMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = StatusResolved.copy(0.1f)),
                    border   = BorderStroke(1.dp, StatusResolved.copy(0.4f)),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = StatusResolved)
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = StatusResolved, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── Complaint Header Card ─────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = DarkCardSurface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.Top
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                complaint.complaintId,
                                fontSize   = 12.sp,
                                color      = CivicGreenLight,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                complaint.issueType,
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                        }
                        PriorityBadge(complaint.priority)
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = TextOnDarkSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(complaint.address, fontSize = 13.sp, color = TextOnDarkSecondary)
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(0.12f)) {
                            Text(
                                complaint.status.displayLabel(),
                                fontSize   = 11.sp,
                                color      = CivicGreenLight,
                                fontWeight = FontWeight.Medium,
                                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Dept: ${complaint.department}",
                            fontSize = 12.sp,
                            color    = TextOnDarkSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Admin Actions ─────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick  = viewModel::showWorkerAssignment,
                    enabled  = !uiState.isAssigning,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = CivicGreen)
                ) {
                    if (uiState.isAssigning) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (complaint.assignedWorkerId != null) "Reassign" else "Assign Worker",
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Worker Assignment Info ─────────────────────────────────────────
            if (complaint.assignedWorkerName != null) {
                InfoSection("Assigned Worker") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Engineering, null, tint = CivicGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(complaint.assignedWorkerName, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Worker ID: ${complaint.assignedWorkerId ?: "—"}", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                    if (complaint.progressPercentage > 0) {
                        Spacer(Modifier.height(10.dp))
                        Text("Progress: ${complaint.progressPercentage}%", fontSize = 13.sp, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress    = { complaint.progressPercentage / 100f },
                            modifier    = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color       = CivicGreen,
                            trackColor  = CivicGreenLight
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Progress Updates ──────────────────────────────────────────────
            if (uiState.progressUpdates.isNotEmpty()) {
                InfoSection("Progress Updates") {
                    uiState.progressUpdates.forEach { update ->
                        ProgressUpdateCard(update)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Citizen Photo ─────────────────────────────────────────────────
            if (complaint.imageUrl != null) {
                Text("Issue Photo", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model           = complaint.imageUrl,
                    contentDescription = "Issue photo",
                    contentScale    = ContentScale.Crop,
                    modifier        = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── Description ───────────────────────────────────────────────────
            InfoSection("Citizen's Report") {
                Text(complaint.description, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Reported by: ${complaint.citizenName}",
                    fontSize = 12.sp,
                    color    = TextTertiary
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Timeline ─────────────────────────────────────────────────────
            if (uiState.timeline.isNotEmpty()) {
                Text("Timeline", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                uiState.timeline.forEach { event ->
                    TimelineEventRow(event)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 12.sp, color = TextTertiary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ProgressUpdateCard(update: com.citizenai.app.domain.model.ProgressUpdate) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CivicGreen.copy(0.12f)
            ) {
                Text(
                    "${update.progressPercentage}%",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = CivicGreen,
                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(update.workerName, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(
                update.createdAt.atZone(ZoneId.systemDefault()).format(timeFormatter),
                fontSize = 11.sp,
                color    = TextTertiary
            )
        }
        if (update.note.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(update.note, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
        }
        if (update.photoUrl != null) {
            Spacer(Modifier.height(6.dp))
            AsyncImage(
                model              = update.photoUrl,
                contentDescription = "Progress photo",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Divider)
    }
}

@Composable
private fun TimelineEventRow(event: com.citizenai.app.domain.model.TimelineEvent) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Indicator
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = when {
                    event.completed -> CivicGreen
                    event.isCurrent -> CivicBlueMedium
                    else            -> Divider
                },
                modifier = Modifier.size(12.dp)
            ) {}
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.title,
                fontWeight = if (event.isCurrent) FontWeight.Bold else FontWeight.Normal,
                fontSize   = 14.sp,
                color      = if (event.completed || event.isCurrent) TextPrimary else TextTertiary
            )
            event.timestamp?.let { ts ->
                Text(
                    ts.atZone(ZoneId.systemDefault()).format(dateFormatter),
                    fontSize = 11.sp,
                    color    = TextTertiary
                )
            }
            event.actorName?.let {
                Text(
                    "by $it",
                    fontSize = 11.sp,
                    color    = TextTertiary
                )
            }
        }
    }
}

@Composable
fun WorkerSelectionDialog(
    workers: List<WorkerMonitor>,
    onSelect: (WorkerMonitor) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Worker", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            if (workers.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(color = CivicGreen)
                    Spacer(Modifier.height(8.dp))
                    Text("Loading workers…", color = TextSecondary)
                }
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    workers.forEach { worker ->
                        WorkerOptionCard(
                            worker   = worker,
                            onSelect = { onSelect(worker) }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceLight,
        shape           = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun WorkerOptionCard(worker: WorkerMonitor, onSelect: () -> Unit) {
    val availabilityColor = when (worker.availability) {
        WorkerAvailability.AVAILABLE -> StatusResolved
        WorkerAvailability.BUSY      -> PriorityHigh
        else                         -> TextTertiary
    }

    Card(
        onClick   = onSelect,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = BackgroundLight),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, null, tint = DeepCivicBlue, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(worker.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                Text(
                    "${worker.department ?: "—"} · ${worker.activeTaskCount} active tasks",
                    fontSize = 12.sp,
                    color    = TextSecondary
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = availabilityColor.copy(0.12f)
            ) {
                Text(
                    worker.availability.displayLabel(),
                    fontSize   = 11.sp,
                    color      = availabilityColor,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
