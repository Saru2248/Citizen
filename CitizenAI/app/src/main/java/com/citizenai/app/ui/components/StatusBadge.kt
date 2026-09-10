package com.citizenai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citizenai.app.domain.model.ComplaintStatus
import com.citizenai.app.domain.model.Priority
import com.citizenai.app.ui.theme.*

/**
 * StatusBadge — pill-shaped colored badge for complaint statuses and priorities.
 * Used on complaint cards, detail screens, and worker task cards.
 */
@Composable
fun StatusBadge(
    status: ComplaintStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        ComplaintStatus.REPORTED,
        ComplaintStatus.PENDING            -> StatusPendingContainer to StatusPending
        ComplaintStatus.AI_ANALYZED,
        ComplaintStatus.UNDER_REVIEW       -> CivicBlueContainer to DeepCivicBlue
        ComplaintStatus.DEPARTMENT_ASSIGNED,
        ComplaintStatus.ASSIGNED           -> CivicBlueContainer to CivicBlueMedium
        ComplaintStatus.WORKER_ASSIGNED,
        ComplaintStatus.WORKER_ACCEPTED    -> PriorityHighContainer to PriorityHigh
        ComplaintStatus.WORK_STARTED,
        ComplaintStatus.IN_PROGRESS,
        ComplaintStatus.PROGRESS_UPDATE    -> StatusInProgressContainer to StatusInProgress
        ComplaintStatus.WORK_COMPLETED,
        ComplaintStatus.ADMIN_REVIEW       -> CivicGreenContainer to CivicGreenDark
        ComplaintStatus.CITIZEN_VERIFICATION -> PriorityHighContainer to PriorityHigh
        ComplaintStatus.RESOLVED,
        ComplaintStatus.COMPLETED          -> StatusResolvedContainer to StatusResolved
        ComplaintStatus.REOPENED           -> StatusReopenedContainer to StatusReopened
        ComplaintStatus.REJECTED           -> PriorityCriticalContainer to PriorityCritical
        ComplaintStatus.CANCELLED          -> PriorityNormalContainer to PriorityNormal
    }

    BadgePill(
        label           = status.displayLabel(),
        backgroundColor = backgroundColor,
        textColor       = textColor,
        modifier        = modifier
    )
}

@Composable
fun PriorityBadge(
    priority: Priority,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (priority) {
        Priority.LOW    -> PriorityNormalContainer to PriorityNormal
        Priority.NORMAL -> PriorityNormalContainer to PriorityNormal
        Priority.MEDIUM -> PriorityHighContainer to PriorityHigh
        Priority.HIGH   -> PriorityHighContainer to PriorityHigh
        Priority.CRITICAL -> PriorityCriticalContainer to PriorityCritical
    }

    BadgePill(
        label           = priority.displayLabel(),
        backgroundColor = backgroundColor,
        textColor       = textColor,
        modifier        = modifier
    )
}

@Composable
private fun BadgePill(
    label: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color      = textColor,
                fontSize   = 11.sp
            )
        )
    }
}
