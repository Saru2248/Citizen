package com.citizenai.worker.presentation.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.citizenai.worker.presentation.components.StatusBadge
import com.citizenai.worker.presentation.theme.*
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkReportScreen(
    onBackClick: () -> Unit,
    viewModel: WorkReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val report = uiState.report

    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var previewImageTitle by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Field Work Report", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CivicGreen)
            }
        } else if (report == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = uiState.errorMessage ?: "Work report unavailable", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Report Header Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "REF: ${report.complaintId}",
                                    fontWeight = FontWeight.Bold,
                                    color = CivicGreenLight,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp
                                )
                                StatusBadge(status = report.finalStatus)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = report.issueCategory,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = report.description,
                                fontSize = 13.sp,
                                color = TextOnDarkSecondary
                            )
                        }
                    }
                }

                // Evidence Photos Section Header
                item {
                    Text(
                        text = "Work Completion Proof",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                }

                // 1. BEFORE WORK PHOTO CARD
                item {
                    val beforeRole = report.beforeUploadedByRole ?: "Citizen Evidence"
                    val beforeTime = report.beforeUploadedAt ?: report.reportedDate
                    EvidencePhotoCard(
                        title = "BEFORE WORK — Citizen Evidence",
                        roleLabel = if (beforeRole.equals("citizen", ignoreCase = true)) "Uploaded by Citizen" else beforeRole,
                        timestamp = beforeTime,
                        photoUrl = report.beforePhotoUrl,
                        emptyMessage = "Citizen has not uploaded a before photo.",
                        accentColor = DeepCivicBlue,
                        onPhotoClick = { url, title ->
                            previewImageUrl = url
                            previewImageTitle = title
                        }
                    )
                }

                // 2. AFTER WORK PHOTO CARD
                item {
                    val afterRole = report.afterUploadedByRole ?: "Worker Evidence"
                    val afterTime = report.afterUploadedAt ?: report.completionDate
                    EvidencePhotoCard(
                        title = "AFTER WORK — Worker Evidence",
                        roleLabel = if (afterRole.equals("worker", ignoreCase = true)) "Uploaded by Worker" else afterRole,
                        timestamp = afterTime,
                        photoUrl = report.afterPhotoUrl,
                        emptyMessage = "Worker has not uploaded an after photo yet.",
                        accentColor = CivicGreen,
                        onPhotoClick = { url, title ->
                            previewImageUrl = url
                            previewImageTitle = title
                        }
                    )
                }

                // Worker Completion Notes
                if (!report.workerNotes.isNullOrEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Worker Completion Notes:", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = report.workerNotes, color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Execution History Timeline Header
                item {
                    Text("Execution Timeline", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                }

                items(report.statusHistory) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CivicGreen)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = item.status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                                val desc = item.message ?: item.note
                                if (!desc.isNullOrEmpty()) {
                                    Text(text = desc, color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    // Full-screen Image Preview Dialog
    if (previewImageUrl != null) {
        FullscreenImageDialog(
            imageUrl = previewImageUrl!!,
            title = previewImageTitle ?: "Evidence Photo",
            onDismiss = {
                previewImageUrl = null
                previewImageTitle = null
            }
        )
    }
}

@Composable
private fun EvidencePhotoCard(
    title: String,
    roleLabel: String,
    timestamp: String?,
    photoUrl: String?,
    emptyMessage: String,
    accentColor: Color,
    onPhotoClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 14.sp
            )

            val formattedTime = formatTimestamp(timestamp)
            val subtitle = if (!formattedTime.isNullOrBlank()) {
                "$roleLabel • $formattedTime"
            } else {
                roleLabel
            }

            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            if (!photoUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceVariantLight)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { onPhotoClick(photoUrl, title) }
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = accentColor,
                                    strokeWidth = 2.5.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = PriorityCritical,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Unable to load image. Please try again.",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    )

                    // Zoom indicator badge overlay
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tap to enlarge",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceVariantLight)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        color = TextTertiary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FullscreenImageDialog(
    imageUrl: String,
    title: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar with title and close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Image in Fit mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = CivicGreen,
                                    strokeWidth = 3.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Unable to load image. Please try again.",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(isoString: String?): String? {
    if (isoString.isNullOrBlank()) return null
    return try {
        val instant = Instant.parse(isoString)
        val zoneId = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
            .withZone(zoneId)
        formatter.format(instant)
    } catch (_: Exception) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(isoString)
            if (date != null) {
                val outSdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                outSdf.format(date)
            } else {
                isoString
            }
        } catch (_: Exception) {
            isoString
        }
    }
}
