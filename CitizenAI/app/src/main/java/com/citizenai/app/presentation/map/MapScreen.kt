package com.citizenai.app.presentation.map

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citizenai.app.domain.model.Complaint
import com.citizenai.app.ui.components.PriorityBadge
import com.citizenai.app.ui.components.StatusBadge
import com.citizenai.app.ui.theme.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

import androidx.compose.ui.res.stringResource
import com.citizenai.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToComplaintDetail: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isListView by remember { mutableStateOf(false) }

    // Default camera position — Aurangabad, Maharashtra
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(19.8762, 75.3433), 13f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.issue_map_title), fontWeight = FontWeight.Bold, color = TextPrimary) },
                actions = {
                    IconButton(onClick = { isListView = !isListView }) {
                        Icon(
                            imageVector = if (isListView) Icons.Default.Map else Icons.Default.FormatListBulleted,
                            contentDescription = if (isListView) "Map View" else "List View",
                            tint = CivicGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        },
        floatingActionButton = {
            if (!isListView) {
                FloatingActionButton(
                    onClick = {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(19.8762, 75.3433), 14f)
                    },
                    containerColor = SurfaceLight,
                    contentColor = CivicGreen,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Recenter Map")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.filterOptions) { filter ->
                    FilterChip(
                        selected = uiState.selectedFilter == filter,
                        onClick = { viewModel.onFilterSelected(filter) },
                        label = { Text(filter, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CivicGreenContainer,
                            selectedLabelColor = CivicGreen
                        )
                    )
                }
            }

            if (isListView) {
                // Interactive List Fallback
                if (uiState.filteredComplaints.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_complaints_filter), color = TextTertiary)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        uiState.filteredComplaints.forEach { complaint ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToComplaintDetail(complaint.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = PriorityCritical,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(complaint.issueType, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                        Text(complaint.address, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
                                        Spacer(Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            StatusBadge(complaint.status)
                                            PriorityBadge(complaint.priority)
                                        }
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary)
                                }
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    // Google Map
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = false),
                        uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = false)
                    ) {
                        uiState.filteredComplaints.forEach { complaint ->
                            Marker(
                                state = MarkerState(position = LatLng(complaint.latitude, complaint.longitude)),
                                title = complaint.issueType,
                                snippet = complaint.address,
                                icon = BitmapDescriptorFactory.defaultMarker(
                                    when (complaint.priority.name) {
                                        "CRITICAL" -> BitmapDescriptorFactory.HUE_RED
                                        "HIGH" -> BitmapDescriptorFactory.HUE_ORANGE
                                        "NORMAL" -> BitmapDescriptorFactory.HUE_YELLOW
                                        else -> BitmapDescriptorFactory.HUE_GREEN
                                    }
                                ),
                                onClick = {
                                    viewModel.onComplaintSelected(complaint)
                                    false
                                }
                            )
                        }
                    }

                    // Selected complaint info card overlay
                    uiState.selectedComplaint?.let { complaint ->
                        ComplaintMapInfoCard(
                            complaint = complaint,
                            onViewDetails = { onNavigateToComplaintDetail(complaint.id) },
                            onDismiss = { viewModel.onComplaintSelected(null) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComplaintMapInfoCard(
    complaint: Complaint,
    onViewDetails: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(complaint.issueType, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(complaint.address, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, "Dismiss", tint = TextTertiary)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(complaint.status)
                PriorityBadge(complaint.priority)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)
            ) {
                Text(stringResource(R.string.view_details), color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
