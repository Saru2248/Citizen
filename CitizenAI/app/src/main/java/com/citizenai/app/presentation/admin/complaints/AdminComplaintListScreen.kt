package com.citizenai.app.presentation.admin.complaints

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.stringResource
import com.citizenai.app.R
import com.citizenai.app.presentation.admin.dashboard.AdminComplaintCard
import com.citizenai.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminComplaintListScreen(
    modifier: Modifier = Modifier,
    onNavigateToComplaintDetail: (String) -> Unit,
    viewModel: AdminComplaintListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val statusFilterList = listOf(
        null to stringResource(R.string.filter_all),
        "REPORTED" to stringResource(R.string.status_reported),
        "IN_PROGRESS" to stringResource(R.string.status_in_progress),
        "WORK_COMPLETED" to stringResource(R.string.status_work_completed),
        "RESOLVED" to stringResource(R.string.status_resolved),
        "REOPENED" to stringResource(R.string.status_reopened)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // ── App Bar ───────────────────────────────────────────────────────────
        Surface(
            color = SurfaceLight,
            shadowElevation = 2.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.complaints_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.load(uiState.selectedStatusFilter) }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = CivicGreen)
                    }
                }

                // Search bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text(stringResource(R.string.search_complaints_hint), color = TextTertiary) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = TextTertiary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CivicGreen,
                        unfocusedBorderColor = Divider
                    )
                )

                // Filter chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(statusFilterList) { (status, label) ->
                        FilterChip(
                            selected = uiState.selectedStatusFilter == status,
                            onClick = { viewModel.onStatusFilterChanged(status) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CivicGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // ── Content ───────────────────────────────────────────────────────────
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CivicGreen)
                }
            }
            uiState.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.WifiOff,
                            null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(uiState.error!!, color = TextSecondary)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.load(uiState.selectedStatusFilter) },
                            colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)
                        ) {
                            Text(stringResource(R.string.retry_upper))
                        }
                    }
                }
            }
            uiState.filteredComplaints.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inbox,
                            null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.no_complaints_found), color = TextSecondary)
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding        = PaddingValues(vertical = 8.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.filteredComplaints, key = { it.id }) { complaint ->
                        AdminComplaintCard(
                            complaint = complaint,
                            onClick   = { onNavigateToComplaintDetail(complaint.id) }
                        )
                    }
                }
            }
        }
    }
}
