package com.citizenai.worker.presentation.theme

import androidx.compose.ui.graphics.Color

// ─── Brand Colors (Harmonized with CitizenAI) ────────────────────────────────

/** Primary — Civic Green */
val CivicGreen = Color(0xFF2D7A3A)
val CivicGreenLight = Color(0xFF4CAF50)
val CivicGreenDark = Color(0xFF1B5E20)
val CivicGreenContainer = Color(0xFFD4EDDA)
val OnCivicGreenContainer = Color(0xFF0A3D1A)

/** Secondary — Deep Civic Blue */
val DeepCivicBlue = Color(0xFF1A2B4A)
val CivicBlueLight = Color(0xFF2D4270)
val CivicBlueMedium = Color(0xFF3A5580)
val CivicBlueContainer = Color(0xFFD3DCF0)
val OnCivicBlueContainer = Color(0xFF0A1830)

// ─── Surface / Background ────────────────────────────────────────────────────

val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFEFF3F9)
val CardSurface = Color(0xFFFFFFFF)
val DarkCardSurface = Color(0xFF1A2B4A)

// ─── Text Colors ─────────────────────────────────────────────────────────────

val TextPrimary = Color(0xFF1E293B)
val TextSecondary = Color(0xFF64748B)
val TextTertiary = Color(0xFF94A3B8)
val TextOnDark = Color(0xFFFFFFFF)
val TextOnDarkSecondary = Color(0xFFB8C5D9)

// ─── Status Colors ────────────────────────────────────────────────────────────

/** New / Submitted — Sky Blue */
val StatusNew = Color(0xFF0EA5E9)
val StatusNewContainer = Color(0xFFE0F2FE)

/** Pending / Assigned — Amber */
val StatusPending = Color(0xFFF59E0B)
val StatusPendingContainer = Color(0xFFFFF8E1)

/** In Progress — Blue */
val StatusInProgress = Color(0xFF3B82F6)
val StatusInProgressContainer = Color(0xFFDCEEFE)

/** Resolved / Completed — Green */
val StatusResolved = Color(0xFF22C55E)
val StatusResolvedContainer = Color(0xFFDCFCE7)

/** High Priority / Worker Assigned — Orange */
val PriorityHigh = Color(0xFFF97316)
val PriorityHighContainer = Color(0xFFFFEDD5)

/** Critical Priority — Red */
val PriorityCritical = Color(0xFFEF4444)
val PriorityCriticalContainer = Color(0xFFFEE2E2)

/** Normal Priority — Muted */
val PriorityNormal = Color(0xFF64748B)
val PriorityNormalContainer = Color(0xFFF1F5F9)

/** Reopened — Purple */
val StatusReopened = Color(0xFF8B5CF6)
val StatusReopenedContainer = Color(0xFFEDE9FE)

// ─── Utility ──────────────────────────────────────────────────────────────────

val Divider = Color(0xFFE2E8F0)
val Shimmer = Color(0xFFE2E8F0)
val ShimmerHighlight = Color(0xFFF8FAFC)
val OverlayDark = Color(0x80000000)
val OverlayLight = Color(0x40FFFFFF)

// ─── Backward Compatibility Aliases for Worker Module ────────────────────────

val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkSurfaceVariant = Color(0xFF334155)
val PrimaryBlue = CivicGreen
val PrimaryBlueVariant = CivicGreenDark
val AccentGreen = StatusResolved
val AccentOrange = PriorityHigh
val AccentRed = PriorityCritical
val BorderColor = Divider
