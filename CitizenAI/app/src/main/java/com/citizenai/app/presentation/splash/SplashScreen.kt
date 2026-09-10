package com.citizenai.app.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citizenai.app.ui.theme.*

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToCitizenHome: () -> Unit,
    onNavigateToWorkerHome: () -> Unit,
    onNavigateToAdminHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    // Route when destination is determined
    LaunchedEffect(destination) {
        when (destination) {
            is SplashDestination.Login       -> onNavigateToLogin()
            is SplashDestination.CitizenHome -> onNavigateToCitizenHome()
            is SplashDestination.WorkerHome  -> onNavigateToWorkerHome()
            is SplashDestination.AdminHome   -> onNavigateToAdminHome()
            else -> Unit
        }
    }

    // ─── Animations ───────────────────────────────────────────────────────────

    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")

    // Scale pulse on the icon
    val iconScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "icon_scale"
    )

    // Fade-in for text
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "text_alpha"
    )

    LaunchedEffect(Unit) { visible = true }

    // Pulsing ring animation
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )

    // ─── UI ───────────────────────────────────────────────────────────────────

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DeepCivicBlue, CivicBlueMedium, DeepCivicBlue)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(alpha)
        ) {
            // Logo container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                // Outer pulsing ring
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(ringScale)
                        .background(
                            color = CivicGreen.copy(alpha = 0.15f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                // Main icon card with official logo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(105.dp)
                        .scale(iconScale)
                        .background(
                            color = Color.White,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        )
                        .padding(14.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.citizenai.app.R.drawable.app_logo),
                        contentDescription = "Citizen AI Logo",
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App name
            Text(
                text = "Citizen AI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Smart Public Problem Reporting\n& Resolution Platform",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextOnDarkSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Loading indicator
            androidx.compose.material3.CircularProgressIndicator(
                color = CivicGreenLight,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp)
            )
        }

        // Version text at bottom
        Text(
            text = "v1.0.0",
            fontSize = 12.sp,
            color = TextOnDarkSecondary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(alpha)
        )
    }
}
