package com.citizenai.app.presentation.worker.task

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.citizenai.app.domain.model.TaskStatus
import com.citizenai.app.ui.components.PriorityBadge
import com.citizenai.app.ui.theme.*
import java.io.File

import androidx.compose.ui.res.stringResource
import com.citizenai.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerTaskDetailsScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    onTaskCompleted: () -> Unit,
    viewModel: WorkerTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(taskId) { viewModel.loadTask(taskId) }
    LaunchedEffect(uiState.isCompleted) { if (uiState.isCompleted) onTaskCompleted() }

    // After Image Launcher
    val afterGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val file = try {
                val inputStream = context.contentResolver.openInputStream(it) ?: return@let
                val f = File(context.cacheDir, "after_${System.currentTimeMillis()}.jpg")
                f.outputStream().use { o -> inputStream.copyTo(o) }
                f
            } catch (e: Exception) { null }
            viewModel.onAfterImageSelected(it, file)
        }
    }

    // Progress Image Launcher
    val progressGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val file = try {
                val inputStream = context.contentResolver.openInputStream(it) ?: return@let
                val f = File(context.cacheDir, "progress_${System.currentTimeMillis()}.jpg")
                f.outputStream().use { o -> inputStream.copyTo(o) }
                f
            } catch (e: Exception) { null }
            viewModel.onProgressImageSelected(it, file)
        }
    }

    // Reject Dialog
    if (uiState.showRejectDialog) {
        var rejectReason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = viewModel::hideRejectDialog,
            title = { Text(stringResource(R.string.reject_task), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.reject_task_reason_prompt), fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        placeholder = { Text(stringResource(R.string.reject_reason_placeholder)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.rejectTask(rejectReason) },
                    enabled = rejectReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PriorityCritical)
                ) {
                    Text(stringResource(R.string.confirm_reject))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideRejectDialog) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Progress Update Dialog
    if (uiState.showProgressDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideProgressDialog,
            title = { Text(stringResource(R.string.update_work_progress), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.progress_percentage_fmt, uiState.progressPercentage), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(25, 50, 75, 100).forEach { pct ->
                            FilterChip(
                                selected = uiState.progressPercentage == pct,
                                onClick = { viewModel.onProgressPercentageChanged(pct) },
                                label = { Text("$pct%") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CivicGreen, selectedLabelColor = Color.White)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.progress_note), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = uiState.progressNote,
                        onValueChange = viewModel::onProgressNoteChanged,
                        placeholder = { Text(stringResource(R.string.progress_note_placeholder)) },
                        modifier = Modifier.fillMaxWidth().height(90.dp)
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.progress_photo_optional), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    if (uiState.progressImageUri != null) {
                        AsyncImage(
                            model = uiState.progressImageUri,
                            contentDescription = "Progress photo",
                            modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        OutlinedButton(
                            onClick = { progressGalleryLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddAPhoto, null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.add_photo))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::submitProgressUpdate,
                    enabled = !uiState.isSubmittingProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)
                ) {
                    if (uiState.isSubmittingProgress) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    else Text(stringResource(R.string.submit_update))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideProgressDialog) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.task_details_title), fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary) }
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

        val task = uiState.task ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // Header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(task.complaintId, fontSize = 13.sp, color = CivicGreenLight, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(task.issueType, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        PriorityBadge(task.priority)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = TextOnDarkSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(task.address, fontSize = 13.sp, color = TextOnDarkSecondary)
                    }
                    task.distanceKm?.let {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NearMe, null, tint = TextOnDarkSecondary, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${String.format("%.1f", it)} ${stringResource(R.string.km_away)}", fontSize = 12.sp, color = TextOnDarkSecondary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Before Photo
            if (task.beforeImageUrl != null) {
                Text("Before Photo", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = task.beforeImageUrl,
                    contentDescription = "Before photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.height(16.dp))
            }

            // Citizen description
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(stringResource(R.string.citizens_report), fontSize = 12.sp, color = TextTertiary, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(task.citizenDescription, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Action based on status
            when (task.status) {
                TaskStatus.PENDING -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = viewModel::showRejectDialog,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, PriorityCritical)
                        ) {
                            Text(stringResource(R.string.reject), color = PriorityCritical)
                        }
                        Button(
                            onClick = viewModel::acceptTask,
                            enabled = !uiState.isUpdating,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)
                        ) {
                            if (uiState.isUpdating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                            else Text(stringResource(R.string.accept_task))
                        }
                    }
                }
                TaskStatus.ACCEPTED -> {
                    Button(
                        onClick = viewModel::startTask,
                        enabled = !uiState.isUpdating,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusInProgress)
                    ) {
                        if (uiState.isUpdating) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        else {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.start_work), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
                TaskStatus.IN_PROGRESS -> {
                    // Update Progress Button
                    OutlinedButton(
                        onClick = viewModel::showProgressDialog,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, CivicGreen)
                    ) {
                        Icon(Icons.Default.Update, null, tint = CivicGreen)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.post_progress_update_fmt, task.progressPercentage), color = CivicGreen, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.completion_report), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))

                    // After photo upload
                    Text(stringResource(R.string.select_completion_photo), fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    if (uiState.afterImageUri != null) {
                        AsyncImage(
                            model = uiState.afterImageUri,
                            contentDescription = "After photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(14.dp))
                        )
                        TextButton(onClick = { afterGalleryLauncher.launch("image/*") }) {
                            Text(stringResource(R.string.change_photo), color = CivicGreen)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { afterGalleryLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddAPhoto, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.select_completion_photo))
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.final_work_notes), fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.workNotes,
                        onValueChange = viewModel::onWorkNotesChanged,
                        placeholder = { Text(stringResource(R.string.work_notes_placeholder), color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CivicGreen, cursorColor = CivicGreen)
                    )

                    uiState.error?.let { Text(it, color = PriorityCritical, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = viewModel::completeTask,
                        enabled = !uiState.isCompleting && uiState.afterImageUri != null,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)
                    ) {
                        if (uiState.isCompleting) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.submitting_dots), color = Color.White)
                        } else {
                            Icon(Icons.Default.DoneAll, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.submit_completion_report), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
                else -> {
                    Text(stringResource(R.string.task_completed_review), color = StatusResolved, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
