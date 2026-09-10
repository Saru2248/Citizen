package com.citizenai.app.presentation.report.details

import android.Manifest
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.citizenai.app.domain.model.IssueCategory
import com.citizenai.app.presentation.report.capture.ReportFlowState
import com.citizenai.app.presentation.report.capture.StepProgressBar
import com.citizenai.app.ui.theme.*
import com.google.accompanist.permissions.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import java.io.File
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailsScreen(
    onSubmitSuccess: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ReportDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var categoryExpanded by remember { mutableStateOf(false) }

    // Location permission
    val locationPermission = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.submittedComplaintId.collectLatest { id -> onSubmitSuccess(id) }
    }

    LaunchedEffect(locationPermission.allPermissionsGranted) {
        if (locationPermission.allPermissionsGranted && uiState.latitude == 0.0) {
            viewModel.fetchCurrentLocation(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Location & Details", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Step 3 of 3", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            StepProgressBar(currentStep = 3, totalSteps = 3)
            Spacer(Modifier.height(24.dp))

            // ─── Attached Photo Section ───────────────────────────────────────
            val attachedImageUri = ReportFlowState.imageUri
            val attachedImagePath = ReportFlowState.imagePath
            if (attachedImageUri != null || attachedImagePath.isNotBlank()) {
                SectionHeader(icon = Icons.Default.Image, title = "Attached Photo")
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    AsyncImage(
                        model = attachedImageUri ?: File(attachedImagePath),
                        contentDescription = "Attached issue photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            // ─── Location Section ─────────────────────────────────────────────

            SectionHeader(icon = Icons.Default.LocationOn, title = "Location")
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Map location picker card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(SurfaceVariantLight, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (locationPermission.allPermissionsGranted) {
                                    viewModel.fetchCurrentLocation(context)
                                } else {
                                    locationPermission.launchMultiplePermissionRequest()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.latitude != 0.0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Default.LocationOn, null, tint = PriorityCritical, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "📍 ${String.format("%.4f", uiState.latitude)}, ${String.format("%.4f", uiState.longitude)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(uiState.address, fontSize = 12.sp, color = TextSecondary, maxLines = 2)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Map, null, tint = CivicGreen, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(4.dp))
                                Text("Tap map to auto-detect your location", color = CivicGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (locationPermission.allPermissionsGranted) {
                                viewModel.fetchCurrentLocation(context)
                            } else {
                                locationPermission.launchMultiplePermissionRequest()
                            }
                        },
                        enabled = !uiState.isLoadingLocation,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)
                    ) {
                        if (uiState.isLoadingLocation) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Getting location…", color = Color.White, fontSize = 14.sp)
                        } else {
                            Icon(Icons.Default.MyLocation, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Use Current Location", color = Color.White, fontSize = 14.sp)
                        }
                    }

                    uiState.locationError?.let { error ->
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = PriorityCritical, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ─── Category ─────────────────────────────────────────────────────

            SectionHeader(icon = Icons.Default.Category, title = "Category")
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.category.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Issue Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CivicGreen,
                        focusedLabelColor = CivicGreen
                    )
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    IssueCategory.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.displayName) },
                            onClick = {
                                viewModel.onCategoryChange(cat)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ─── Description ──────────────────────────────────────────────────

            SectionHeader(icon = Icons.Default.Description, title = "Description")
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Describe the issue in detail…") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CivicGreen,
                    focusedLabelColor = CivicGreen,
                    cursorColor = CivicGreen
                )
            )

            uiState.submitError?.let { error ->
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = PriorityCriticalContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(error, modifier = Modifier.padding(10.dp), color = PriorityCritical, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(32.dp))

            // ─── Submit Button ────────────────────────────────────────────────

            Button(
                onClick = viewModel::submitReport,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CivicGreen),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Submitting report…", color = Color.White, fontSize = 16.sp)
                } else {
                    Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Submit Report", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = CivicGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
