package com.citizenai.app.presentation.citizen.home

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.citizenai.app.domain.model.Complaint
import com.citizenai.app.domain.model.ComplaintStatus
import com.citizenai.app.ui.components.PriorityBadge
import com.citizenai.app.ui.components.StatusBadge
import com.citizenai.app.ui.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CitizenHomeScreen(
    onNavigateToReport: () -> Unit,
    onNavigateToComplaintDetail: (String) -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    viewModel: CitizenHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fine || coarse) {
            viewModel.fetchLocation(context)
        } else {
            android.widget.Toast.makeText(context, "Location permission required to detect location", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED || androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.fetchLocation(context)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
    ) {
        // ─── Header Section (Dark Card) ───────────────────────────────────────

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
                // Top row: greeting + notification
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
                            text = if (uiState.isLoading) "Loading…" else uiState.userName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                permissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = CivicGreenLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = uiState.currentLocationAddress,
                                fontSize = 12.sp,
                                color = TextOnDarkSecondary
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    // ─── Prominent Citizen AI Notification Bell Icon ─────────────
                    Surface(
                        onClick = onNavigateToNotifications,
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
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            // Modern unread indicator badge dot
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(9.dp)
                                    .background(PriorityCritical, CircleShape)
                                    .border(1.5.dp, DeepCivicBlue, CircleShape)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ─── Civic Impact Card ─────────────────────────────────────────

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "YOUR CIVIC IMPACT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CivicGreenLight,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ImpactStat(
                                value = if (uiState.isLoading) "-" else "${uiState.totalReports}",
                                label = "Reports"
                            )
                            VerticalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.height(48.dp))
                            ImpactStat(
                                value = if (uiState.isLoading) "-" else "${uiState.resolvedReports}",
                                label = "Resolved",
                                valueColor = CivicGreenLight
                            )
                            VerticalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.height(48.dp))
                            ImpactStat(
                                value = if (uiState.isLoading) "-" else "${uiState.pendingReports}",
                                label = "Pending",
                                valueColor = StatusPending
                            )
                        }
                    }
                }
            }
        }

        // ─── Report New Issue Button ──────────────────────────────────────────

        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Button(
                onClick = onNavigateToReport,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CivicGreen),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Report New Issue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        // ─── Recent Reports ───────────────────────────────────────────────────

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Recent Reports",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            TextButton(onClick = { /* Navigate to all reports */ }) {
                Text("View All", color = CivicGreen, fontSize = 14.sp)
            }
        }

        if (uiState.isLoading) {
            // Shimmer placeholders
            repeat(3) {
                ShimmerComplaintCard()
            }
        } else if (uiState.recentComplaints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inbox,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No reports yet. Tap 'Report New Issue' to get started.",
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            uiState.recentComplaints.forEach { complaint ->
                ComplaintCard(
                    complaint = complaint,
                    onClick = { onNavigateToComplaintDetail(complaint.id) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
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
            fontSize = 28.sp,
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
fun ComplaintCard(
    complaint: Complaint,
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
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariantLight),
                contentAlignment = Alignment.Center
            ) {
                if (complaint.imageUrl != null) {
                    AsyncImage(
                        model = complaint.imageUrl,
                        contentDescription = "Issue image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = when (complaint.category.name) {
                            "POTHOLE", "ROAD_DAMAGE" -> Icons.Default.Construction
                            "GARBAGE" -> Icons.Default.Delete
                            "STREETLIGHT" -> Icons.Default.Lightbulb
                            "WATER_LEAKAGE" -> Icons.Default.WaterDrop
                            else -> Icons.Default.Report
                        },
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = complaint.issueType,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1
                )
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
                        text = complaint.address,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(complaint.status)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = complaint.complaintId,
                        fontSize = 11.sp,
                        color = TextTertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ShimmerComplaintCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Shimmer)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).background(Shimmer, RoundedCornerShape(4.dp)))
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth(0.9f).height(12.dp).background(Shimmer, RoundedCornerShape(4.dp)))
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.width(80.dp).height(22.dp).background(Shimmer, RoundedCornerShape(100.dp)))
            }
        }
    }
}
