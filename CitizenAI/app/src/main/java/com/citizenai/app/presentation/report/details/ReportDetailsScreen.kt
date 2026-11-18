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
        if (locationPermission.allPermissionsGranted && uiState.address.isBlank() && uiState.locationSource != "MANUAL") {
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

            // ─── Location Selection Section ─────────────────────────────────────

            SectionHeader(icon = Icons.Default.LocationOn, title = "Location Selection")
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Selected Location Status Display
                    if (uiState.address.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.locationSource == "MANUAL") Color(0xFFFFFBEB) else Color(0xFFF0FDF4)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (uiState.locationSource == "MANUAL") Color(0xFFFDE68A) else Color(0xFFBBF7D0)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (uiState.locationSource == "MANUAL") "📍 Manual Location" else "🛰️ Current GPS Location",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (uiState.locationSource == "MANUAL") Color(0xFFB45309) else Color(0xFF15803D)
                                    )
                                    if (uiState.latitude != null && uiState.longitude != null && uiState.latitude != 0.0 && uiState.longitude != 0.0) {
                                        Text(
                                            text = "${String.format("%.4f", uiState.latitude)}, ${String.format("%.4f", uiState.longitude)}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    } else {
                                        Text(
                                            text = "Address only",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = uiState.address,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .background(SurfaceVariantLight, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LocationOff, null, tint = TextSecondary, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.height(4.dp))
                                Text("No location selected yet", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    // Dual Options: Option A (Current GPS Location) & Option B (Manual Location Entry)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option A: Use Current Location
                        Button(
                            onClick = {
                                if (locationPermission.allPermissionsGranted) {
                                    viewModel.fetchCurrentLocation(context)
                                } else {
                                    locationPermission.launchMultiplePermissionRequest()
                                }
                            },
                            enabled = !uiState.isLoadingLocation,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.locationSource == "GPS" && uiState.address.isNotBlank()) CivicGreen else Color(0xFF0F766E)
                            )
                        ) {
                            if (uiState.isLoadingLocation) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.MyLocation, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.locationSource == "GPS" && uiState.address.isNotBlank()) "Refresh GPS" else "Current GPS",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Option B: Enter Location Manually
                        OutlinedButton(
                            onClick = { viewModel.openManualLocationDialog() },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, CivicGreen)
                        ) {
                            Icon(Icons.Default.EditLocationAlt, null, tint = CivicGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Enter Manually",
                                color = CivicGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    uiState.locationError?.let { error ->
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = PriorityCritical, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // ─── Manual Location Entry Dialog ─────────────────────────────────
            if (uiState.showManualLocationDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissManualLocationDialog() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EditLocationAlt, contentDescription = null, tint = CivicGreen)
                            Spacer(Modifier.width(8.dp))
                            Text("Enter Complaint Location", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Enter the actual complaint location if GPS is unavailable or inaccurate:",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            OutlinedTextField(
                                value = uiState.manualStreet,
                                onValueChange = viewModel::updateManualStreet,
                                label = { Text("Street / Road Name *") },
                                placeholder = { Text("e.g. MG Road, Near Market") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = uiState.manualArea,
                                onValueChange = viewModel::updateManualArea,
                                label = { Text("Area / Locality *") },
                                placeholder = { Text("e.g. Shivaji Nagar, Ward 4") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = uiState.manualLandmark,
                                onValueChange = viewModel::updateManualLandmark,
                                label = { Text("Landmark") },
                                placeholder = { Text("e.g. Opposite City Hospital") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = uiState.manualCity,
                                onValueChange = viewModel::updateManualCity,
                                label = { Text("City *") },
                                placeholder = { Text("e.g. Pune / Mumbai") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = uiState.manualPincode,
                                onValueChange = viewModel::updateManualPincode,
                                label = { Text("PIN Code (Optional)") },
                                placeholder = { Text("e.g. 411005") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.saveManualLocation(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = CivicGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Location", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.dismissManualLocationDialog() }
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                )
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
