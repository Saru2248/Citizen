package com.citizenai.app.presentation.complaints.detail

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.citizenai.app.domain.model.*
import com.citizenai.app.ui.components.PriorityBadge
import com.citizenai.app.ui.components.StatusBadge
import com.citizenai.app.ui.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintDetailScreen(
    complaintId: String,
    onNavigateBack: () -> Unit,
    viewModel: ComplaintDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showVerifyDialog by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }

    LaunchedEffect(complaintId) {
        viewModel.loadComplaint(complaintId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complaint Details", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
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

        val complaint = uiState.complaint ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ─── Header Card ───────────────────────────────────────────────────

            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                                complaint.complaintId,
                                fontSize = 13.sp,
                                color = CivicGreenLight,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(complaint.issueType, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        StatusBadge(complaint.status)
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = TextOnDarkSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(complaint.address, fontSize = 13.sp, color = TextOnDarkSecondary, maxLines = 2)
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, tint = TextOnDarkSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault())
                        Text(formatter.format(complaint.reportedAt), fontSize = 12.sp, color = TextOnDarkSecondary)
                        Spacer(Modifier.width(12.dp))
                        PriorityBadge(complaint.priority)
                    }
                }
            }

            // ─── Assigned Worker & Department Card ───────────────────────────
            val assignedWorker = complaint.assignedWorkerName
            val dept = complaint.department
            if (!assignedWorker.isNullOrBlank() || !dept.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, tint = CivicGreen, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Assignment Details", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        }
                        Spacer(Modifier.height(10.dp))
                        if (!dept.isNullOrBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Department", fontSize = 13.sp, color = TextSecondary)
                                Text(dept, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                        }
                        if (!assignedWorker.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Assigned Worker", fontSize = 13.sp, color = TextSecondary)
                                Text(assignedWorker, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CivicGreen)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ─── Before Photo ──────────────────────────────────────────────────

            if (complaint.imageUrl != null || complaint.afterImageUrl != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (complaint.imageUrl != null) {
                        PhotoCard(url = complaint.imageUrl, label = "Before", modifier = Modifier.weight(1f))
                    }
                    if (complaint.afterImageUrl != null) {
                        PhotoCard(url = complaint.afterImageUrl, label = "After", modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ─── Timeline ──────────────────────────────────────────────────────

            if (uiState.timeline.isNotEmpty()) {
                SectionCard(title = "Progress Timeline", icon = Icons.Default.Timeline) {
                    uiState.timeline.forEachIndexed { index, event ->
                        TimelineItemRow(
                            event = event,
                            isLast = index == uiState.timeline.size - 1
                        )
                    }
                }
            }

            // ─── Citizen Verification ──────────────────────────────────────────

            if (complaint.status == ComplaintStatus.CITIZEN_VERIFICATION && !uiState.verifySuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PriorityHighContainer),
                    border = BorderStroke(1.dp, PriorityHigh.copy(0.3f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Has this issue been resolved?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.verifyResolution(true) },
                            enabled = !uiState.isVerifying,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)
                        ) {
                            Text("YES, ISSUE RESOLVED", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showVerifyDialog = true },
                            enabled = !uiState.isVerifying,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, PriorityCritical)
                        ) {
                            Text("NO, STILL NOT RESOLVED", color = PriorityCritical, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ─── Comments ──────────────────────────────────────────────────────

            SectionCard(title = "Comments (${uiState.comments.size})", icon = Icons.Default.Comment) {
                uiState.comments.forEach { comment ->
                    CommentRow(comment = comment)
                    Spacer(Modifier.height(12.dp))
                }

                // Add comment input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.commentText,
                        onValueChange = viewModel::onCommentChange,
                        placeholder = { Text("Add a comment…", color = TextTertiary) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CivicGreen, cursorColor = CivicGreen
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = viewModel::postComment,
                        enabled = uiState.commentText.isNotBlank() && !uiState.isPostingComment,
                        modifier = Modifier
                            .size(44.dp)
                            .background(CivicGreen, CircleShape)
                    ) {
                        Icon(Icons.Default.Send, "Post comment", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Rejection reason dialog
    if (showVerifyDialog) {
        AlertDialog(
            onDismissRequest = { showVerifyDialog = false },
            title = { Text("Why is it not resolved?") },
            text = {
                OutlinedTextField(
                    value = rejectionReason,
                    onValueChange = { rejectionReason = it },
                    placeholder = { Text("Explain the issue…") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.verifyResolution(false, rejectionReason)
                        showVerifyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PriorityCritical)
                ) { Text("Submit", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showVerifyDialog = false }) { Text("Cancel") }
            }
        )
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
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                Icon(icon, null, tint = CivicGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 15.sp)
            }
            content()
        }
    }
}

@Composable
private fun TimelineItemRow(event: TimelineEvent, isLast: Boolean) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm").withZone(ZoneId.systemDefault())
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = when {
                            event.completed -> CivicGreenContainer
                            event.isCurrent -> CivicGreen
                            else -> SurfaceVariantLight
                        },
                        shape = CircleShape
                    )
                    .border(1.5.dp, when {
                        event.completed -> CivicGreen
                        event.isCurrent -> CivicGreen
                        else -> Divider
                    }, CircleShape)
            ) {
                if (event.completed) {
                    Icon(Icons.Default.Check, null, tint = CivicGreen, modifier = Modifier.size(14.dp))
                } else if (event.isCurrent) {
                    Box(Modifier.size(8.dp).background(Color.White, CircleShape))
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(if (event.completed) CivicGreen else Divider)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 20.dp)) {
            Text(
                event.title,
                fontWeight = if (event.isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (event.completed || event.isCurrent) TextPrimary else TextTertiary
            )
            Text(
                event.description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
            event.timestamp?.let { ts ->
                Text(formatter.format(ts), fontSize = 11.sp, color = TextTertiary, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun CommentRow(comment: Comment) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm").withZone(ZoneId.systemDefault())
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(SurfaceVariantLight, CircleShape)
        ) {
            Text(
                comment.authorName.firstOrNull()?.toString() ?: "?",
                fontWeight = FontWeight.Bold,
                color = DeepCivicBlue
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(comment.authorName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                Text(formatter.format(comment.postedAt), fontSize = 11.sp, color = TextTertiary)
            }
            Spacer(Modifier.height(3.dp))
            Text(comment.message, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
        }
    }
}
