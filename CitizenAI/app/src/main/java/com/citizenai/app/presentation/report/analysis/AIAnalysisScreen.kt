package com.citizenai.app.presentation.report.analysis

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citizenai.app.presentation.report.capture.StepProgressBar
import com.citizenai.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAnalysisScreen(
    onAnalysisComplete: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AIAnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Analysis", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Step 2 of 3", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StepProgressBar(currentStep = 2, totalSteps = 3)
            Spacer(Modifier.height(32.dp))

            if (uiState.isAnalyzing) {
                // ─── Analyzing State ───────────────────────────────────────────
                AIAnalyzingAnimation()
                Spacer(Modifier.height(32.dp))
                Text(
                    "AI is analyzing your image…",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(24.dp))

                // Stage indicators
                uiState.stages.forEach { stage ->
                    AnalysisStageRow(stage = stage)
                    Spacer(Modifier.height(12.dp))
                }
            } else if (uiState.result != null) {
                // ─── Result State ──────────────────────────────────────────────
                val result = uiState.result!!

                // Success animation
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .background(StatusResolvedContainer, CircleShape)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusResolved,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("AI Analysis Complete", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(24.dp))

                // Result card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "AI ANALYSIS RESULT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CivicGreen,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.height(16.dp))

                        AIResultRow("Issue", result.issueType, Icons.Default.Report)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Divider)
                        AIResultRow(
                            "Severity",
                            result.severity.let {
                                it.lowercase().replaceFirstChar { c -> c.uppercase() }
                            },
                            Icons.Default.PriorityHigh,
                            valueColor = when (result.severity.uppercase()) {
                                "CRITICAL" -> PriorityCritical
                                "HIGH" -> PriorityHigh
                                "LOW" -> PriorityNormal
                                else -> TextPrimary
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Divider)
                        AIResultRow("Department", result.department, Icons.Default.Business)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Divider)
                        AIResultRow(
                            "Confidence",
                            "${(result.confidence * 100).toInt()}%",
                            Icons.Default.Analytics,
                            valueColor = CivicGreen
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = onAnalysisComplete,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)
                ) {
                    Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null, tint = Color.White)
                }
            } else if (uiState.error != null) {
                Icon(Icons.Default.Error, null, tint = PriorityCritical, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Analysis Failed", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                Text(uiState.error!!, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onNavigateBack, colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)) {
                    Text("Try Again", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AIAnalyzingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_scan")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(120.dp).scale(scale)
    ) {
        // Outer ring
        CircularProgressIndicator(
            progress = { 0.75f },
            modifier = Modifier.fillMaxSize().rotate(rotation),
            color = CivicGreen,
            strokeWidth = 3.dp,
            trackColor = CivicGreenContainer
        )
        // Center AI icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = Brush.radialGradient(listOf(DeepCivicBlue, CivicBlueMedium)),
                    shape = CircleShape
                )
        ) {
            Icon(Icons.Default.Psychology, null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun AnalysisStageRow(stage: AnalysisStage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = stage.completed,
            label = "stage_icon"
        ) { completed ->
            if (completed) {
                Icon(Icons.Default.CheckCircle, null, tint = CivicGreen, modifier = Modifier.size(20.dp))
            } else {
                CircularProgressIndicator(
                    color = CivicGreen,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = stage.label,
            color = if (stage.completed) TextPrimary else TextSecondary,
            fontWeight = if (stage.completed) FontWeight.Medium else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AIResultRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}
