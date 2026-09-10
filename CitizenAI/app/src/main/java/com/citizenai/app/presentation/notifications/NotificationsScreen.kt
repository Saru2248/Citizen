package com.citizenai.app.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citizenai.app.domain.model.Notification
import com.citizenai.app.domain.model.NotificationType
import com.citizenai.app.ui.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import androidx.compose.ui.res.stringResource
import com.citizenai.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm").withZone(ZoneId.systemDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title), fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
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

        if (uiState.notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsNone, null, tint = TextTertiary, modifier = Modifier.size(72.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.no_notifications_yet), color = TextTertiary, fontSize = 16.sp)
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(uiState.notifications, key = { it.id }) { notification ->
                NotificationRow(
                    notification = notification,
                    formatter = formatter,
                    onClick = { viewModel.markAsRead(notification.id) }
                )
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: Notification,
    formatter: DateTimeFormatter,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) SurfaceLight else CivicGreenContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(if (notification.isRead) 1.dp else 2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(notificationColor(notification.type).copy(0.15f))
            ) {
                Icon(
                    notificationIcon(notification.type),
                    null,
                    tint = notificationColor(notification.type),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(notification.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                Spacer(Modifier.height(3.dp))
                Text(notification.message, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(formatter.format(notification.createdAt), fontSize = 11.sp, color = TextTertiary)
            }
            if (!notification.isRead) {
                Box(Modifier.size(8.dp).background(CivicGreen, CircleShape))
            }
        }
    }
}

private fun notificationIcon(type: NotificationType) = when (type) {
    NotificationType.WORKER_ASSIGNED -> Icons.Default.Engineering
    NotificationType.WORK_STARTED -> Icons.Default.PlayArrow
    NotificationType.WORK_COMPLETED -> Icons.Default.CheckCircle
    NotificationType.VERIFICATION_REQUIRED -> Icons.Default.HelpOutline
    NotificationType.COMPLAINT_RESOLVED -> Icons.Default.DoneAll
    NotificationType.COMPLAINT_REOPENED -> Icons.Default.Refresh
    else -> Icons.Default.Notifications
}

private fun notificationColor(type: NotificationType) = when (type) {
    NotificationType.COMPLAINT_RESOLVED -> StatusResolved
    NotificationType.VERIFICATION_REQUIRED -> StatusPending
    NotificationType.COMPLAINT_REOPENED -> PriorityCritical
    else -> StatusInProgress
}
