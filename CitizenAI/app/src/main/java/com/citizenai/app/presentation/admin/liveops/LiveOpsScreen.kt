package com.citizenai.app.presentation.admin.liveops

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.citizenai.app.domain.model.WorkerAvailability
import com.citizenai.app.domain.model.WorkerMonitor
import com.citizenai.app.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.citizenai.app.ui.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class LiveOpsUiState(
    val activeWorkers: List<WorkerMonitor> = emptyList(),
    val delayedWorkers: List<WorkerMonitor> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val lastRefreshedAt: String = ""
)

@HiltViewModel
class LiveOpsViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveOpsUiState())
    val uiState: StateFlow<LiveOpsUiState> = _uiState.asStateFlow()
    private val formatter = DateTimeFormatter.ofPattern("hh:mm:ss a")

    init {
        load()
        // Auto-refresh every 30 seconds for live monitoring
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                load(silent = true)
            }
        }
    }

    fun load(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.value = _uiState.value.copy(isLoading = true)
            adminRepository.getActiveWorkers().fold(
                onSuccess = { workers ->
                    val delayed = workers.filter { it.isDelayed(30) }
                    _uiState.value = LiveOpsUiState(
                        activeWorkers  = workers,
                        delayedWorkers = delayed,
                        isLoading      = false,
                        lastRefreshedAt = java.time.LocalTime.now().format(formatter)
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error     = err.message
                    )
                }
            )
        }
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

@Composable
fun LiveOpsScreen(
    modifier: Modifier = Modifier,
    onNavigateToComplaintDetail: (String) -> Unit,
    viewModel: LiveOpsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(DeepCivicBlue, CivicBlueMedium)))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Live Operations",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                        Text(
                            "Auto-refreshes every 30s",
                            fontSize = 11.sp,
                            color    = TextOnDarkSecondary
                        )
                    }
                    IconButton(
                        onClick  = { viewModel.load() },
                        modifier = Modifier.background(Color.White.copy(0.12f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                    }
                }
                if (uiState.lastRefreshedAt.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Last update: ${uiState.lastRefreshedAt}",
                        fontSize = 10.sp,
                        color    = TextOnDarkSecondary
                    )
                }

                // Summary stats
                if (!uiState.isLoading) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatChip(
                            "${uiState.activeWorkers.size}",
                            "Active Workers",
                            Color.White
                        )
                        StatChip(
                            "${uiState.delayedWorkers.size}",
                            "Delayed",
                            if (uiState.delayedWorkers.isEmpty()) StatusResolved else PriorityHigh
                        )
                    }
                }
            }
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CivicGreen)
            }
            uiState.activeWorkers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.People, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No active workers at this time", color = TextSecondary)
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Delayed workers first (alert section)
                if (uiState.delayedWorkers.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = PriorityHigh, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "DELAYED — No update in 30+ minutes",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 12.sp,
                                color      = PriorityHigh
                            )
                        }
                    }
                    items(uiState.delayedWorkers, key = { "delayed_${it.id}" }) { worker ->
                        LiveWorkerCard(
                            worker     = worker,
                            isDelayed  = true,
                            onComplaintClick = { id -> id?.let(onNavigateToComplaintDetail) }
                        )
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }

                item {
                    Text(
                        "ALL ACTIVE WORKERS",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextTertiary
                    )
                }
                items(uiState.activeWorkers, key = { it.id }) { worker ->
                    LiveWorkerCard(
                        worker     = worker,
                        isDelayed  = uiState.delayedWorkers.contains(worker),
                        onComplaintClick = { id -> id?.let(onNavigateToComplaintDetail) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveWorkerCard(
    worker: WorkerMonitor,
    isDelayed: Boolean,
    onComplaintClick: (String?) -> Unit
) {
    val borderColor = if (isDelayed) PriorityHigh else Color.Transparent
    val headerColor = if (isDelayed) PriorityHigh.copy(0.08f) else SurfaceLight

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = headerColor),
        elevation = CardDefaults.cardElevation(2.dp),
        border    = if (isDelayed) androidx.compose.foundation.BorderStroke(1.5.dp, PriorityHigh) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column {
                    Text(worker.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Text(
                        "${worker.department ?: "—"} · ID: ${worker.workerId ?: "—"}",
                        fontSize = 12.sp,
                        color    = TextSecondary
                    )
                }
                if (isDelayed) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PriorityHigh.copy(0.15f)
                    ) {
                        Text(
                            "DELAYED",
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color      = PriorityHigh,
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (worker.currentComplaintId != null) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            worker.currentIssueType ?: "Task",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp,
                            color      = TextPrimary
                        )
                        Text(
                            "Case: ${worker.currentComplaintId}",
                            fontSize = 12.sp,
                            color    = CivicGreen
                        )
                    }
                    TextButton(onClick = { onComplaintClick(worker.currentComplaintId) }) {
                        Text("View", color = CivicGreen, fontSize = 12.sp)
                    }
                }

                if ((worker.progressPercentage ?: 0) > 0) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${worker.progressPercentage}%", fontSize = 12.sp, color = CivicGreen, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress   = { (worker.progressPercentage ?: 0) / 100f },
                            modifier   = Modifier.weight(1f),
                            color      = CivicGreen,
                            trackColor = CivicGreenLight
                        )
                    }
                }
            }

            worker.lastUpdateAt?.let { ts ->
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = TextTertiary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Last update: ${ts.atZone(ZoneId.systemDefault()).format(timeFormatter)}",
                        fontSize = 11.sp,
                        color    = TextTertiary
                    )
                    worker.minutesSinceLastUpdate()?.let { mins ->
                        Text(
                            " (${mins}m ago)",
                            fontSize = 11.sp,
                            color    = if (isDelayed) PriorityHigh else TextTertiary
                        )
                    }
                }
            }

            worker.lastNote?.let { note ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "\"$note\"",
                    fontSize  = 12.sp,
                    color     = TextSecondary,
                    maxLines  = 2,
                    overflow  = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = TextOnDarkSecondary)
    }
}
