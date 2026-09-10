package com.citizenai.worker.presentation.report

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.citizenai.worker.presentation.components.StatusBadge
import com.citizenai.worker.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkReportScreen(
    onBackClick: () -> Unit,
    viewModel: WorkReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val report = uiState.report

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

                // Before / After Photo Comparison Cards
                item {
                    Text("Work Completion Proof", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Before Photo
                        Column(modifier = Modifier.weight(1f)) {
                            Text("BEFORE WORK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceVariantLight)
                            ) {
                                if (!report.beforePhotoUrl.isNullOrEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(report.beforePhotoUrl),
                                        contentDescription = "Before Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No Photo", color = TextTertiary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // After Photo
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AFTER WORK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CivicGreen)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceVariantLight)
                                    .border(1.5.dp, CivicGreen, RoundedCornerShape(12.dp))
                            ) {
                                if (!report.afterPhotoUrl.isNullOrEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(report.afterPhotoUrl),
                                        contentDescription = "After Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No Photo", color = TextTertiary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
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
}
