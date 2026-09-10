package com.citizenai.app.presentation.admin.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.citizenai.app.domain.model.WorkerAvailability
import com.citizenai.app.domain.model.WorkerMonitor
import com.citizenai.app.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.citizenai.app.ui.theme.*

data class AdminWorkerListUiState(
    val workers: List<WorkerMonitor> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AdminWorkerListViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminWorkerListUiState())
    val uiState: StateFlow<AdminWorkerListUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            adminRepository.getAllWorkers().fold(
                onSuccess = { workers ->
                    _uiState.value = AdminWorkerListUiState(
                        workers   = workers.sortedBy { it.availability.ordinal },
                        isLoading = false
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = err.message)
                }
            )
        }
    }
}

@Composable
fun AdminWorkerListScreen(
    modifier: Modifier = Modifier,
    onNavigateToWorkerDetail: (String) -> Unit,
    onNavigateToAddWorker: () -> Unit = {},
    viewModel: AdminWorkerListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddWorker,
                containerColor = CivicGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Worker")
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .background(BackgroundLight)
        ) {
            // App bar
            Surface(color = SurfaceLight, shadowElevation = 2.dp) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Workers",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                        modifier   = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = CivicGreen)
                    }
                }
            }


        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CivicGreen)
            }
            uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.error!!, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::load, colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)) {
                        Text("Retry")
                    }
                }
            }
            else -> LazyColumn(
                contentPadding      = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.workers, key = { it.id }) { worker ->
                    WorkerCard(
                        worker  = worker,
                        onClick = { onNavigateToWorkerDetail(worker.id) }
                    )
                }
                }
            }
        }
    }
}



@Composable
fun WorkerCard(worker: WorkerMonitor, onClick: () -> Unit) {
    val availColor = when (worker.availability) {
        WorkerAvailability.AVAILABLE -> StatusResolved
        WorkerAvailability.BUSY      -> PriorityHigh
        WorkerAvailability.ON_LEAVE  -> CivicBlueMedium
        WorkerAvailability.OFFLINE   -> TextTertiary
        WorkerAvailability.DISABLED  -> PriorityCritical
    }

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            Surface(
                shape    = CircleShape,
                color    = DeepCivicBlue.copy(0.12f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        worker.name.firstOrNull()?.toString() ?: "W",
                        fontWeight = FontWeight.Bold,
                        color      = DeepCivicBlue,
                        fontSize   = 18.sp
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(worker.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                Text(
                    "${worker.department ?: "—"} · ${worker.workerId ?: ""}",
                    fontSize = 12.sp,
                    color    = TextSecondary
                )
                if (worker.currentIssueType != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Task: ${worker.currentIssueType}",
                        fontSize = 12.sp,
                        color    = TextTertiary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = availColor.copy(0.12f)
                ) {
                    Text(
                        worker.availability.displayLabel(),
                        fontSize   = 11.sp,
                        color      = availColor,
                        fontWeight = FontWeight.Medium,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("${worker.activeTaskCount} tasks", fontSize = 11.sp, color = TextTertiary)
            }
        }
    }
}
