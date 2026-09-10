package com.citizenai.worker.presentation.components

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
import com.citizenai.worker.presentation.theme.*

/**
 * StatusBadge — pill-shaped colored badge for worker tasks and complaint statuses.
 * Visually identical to CitizenAI StatusBadge.
 */
@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val normalized = status.uppercase().replace(" ", "_")
    val (backgroundColor, textColor) = when {
        normalized.contains("PENDING") || normalized == "REPORTED" || normalized == "SUBMITTED" -> StatusPendingContainer to StatusPending
        normalized.contains("PROGRESS") || normalized.contains("STARTED") || normalized == "WORK_STARTED" -> StatusInProgressContainer to StatusInProgress
        normalized.contains("COMPLETED") || normalized == "RESOLVED" -> StatusResolvedContainer to StatusResolved
        normalized.contains("ASSIGNED") || normalized == "WORKER_ASSIGNED" || normalized == "DEPARTMENT_ASSIGNED" -> CivicBlueContainer to CivicBlueMedium
        normalized.contains("VERIFICATION") || normalized == "VERIFICATION_REQUIRED" -> PriorityHighContainer to PriorityHigh
        normalized.contains("REJECTED") -> PriorityCriticalContainer to PriorityCritical
        else -> CivicBlueContainer to DeepCivicBlue
    }

    val displayLabel = when (normalized) {
        "SUBMITTED", "REPORTED" -> "Submitted"
        "DEPARTMENT_ASSIGNED" -> "Dept Assigned"
        "WORKER_ASSIGNED", "ASSIGNED" -> "Assigned"
        "WORK_STARTED", "STARTED" -> "Started"
        "IN_PROGRESS" -> "In Progress"
        "COMPLETED", "WORK_COMPLETED" -> "Completed"
        "VERIFICATION_REQUIRED" -> "Verification Pending"
        "RESOLVED" -> "Resolved"
        else -> status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }

    BadgePill(
        label = displayLabel,
        backgroundColor = backgroundColor,
        textColor = textColor,
        modifier = modifier
    )
}

@Composable
fun PriorityBadge(
    priority: String,
    modifier: Modifier = Modifier
) {
    val normalized = priority.uppercase()
    val (backgroundColor, textColor) = when (normalized) {
        "LOW", "NORMAL" -> PriorityNormalContainer to PriorityNormal
        "MEDIUM", "HIGH" -> PriorityHighContainer to PriorityHigh
        "CRITICAL", "URGENT" -> PriorityCriticalContainer to PriorityCritical
        else -> PriorityNormalContainer to PriorityNormal
    }

    BadgePill(
        label = normalized.lowercase().replaceFirstChar { it.uppercase() },
        backgroundColor = backgroundColor,
        textColor = textColor,
        modifier = modifier
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
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                fontSize = 11.sp
            )
        )
    }
}
