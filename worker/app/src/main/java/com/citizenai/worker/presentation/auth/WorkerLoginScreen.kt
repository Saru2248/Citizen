package com.citizenai.worker.presentation.auth

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.citizenai.worker.R
import com.citizenai.worker.presentation.theme.*

@Composable
fun WorkerLoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: WorkerLoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
    ) {
        // ─── Top Gradient Header with CitizenAi Worker Logo ───────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(DeepCivicBlue, CivicBlueMedium)
                    )
                )
                .padding(top = 44.dp, bottom = 36.dp)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Official CitizenAi Worker Logo Badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .padding(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_worker_logo),
                        contentDescription = "CitizenAi Worker Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "CitizenAI Worker",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Field Operations & Municipal Services",
                    fontSize = 13.sp,
                    color = TextOnDarkSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ─── Overlapping Content Card ───────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset(y = (-20).dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {

                // Error Banner
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PriorityCriticalContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = PriorityCritical,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = PriorityCritical,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (uiState.step == LoginStep.ENTER_PHONE) {
                    // ══════════════════════════════════════════════════════
                    // SCREEN 1: Enter Worker ID & Mobile Number
                    // ══════════════════════════════════════════════════════
                    Text(
                        text = "Worker Verification",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Enter your assigned Worker ID and registered mobile number to receive an SMS OTP.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                    )

                    // Worker ID
                    OutlinedTextField(
                        value = uiState.workerIdInput,
                        onValueChange = { viewModel.onWorkerIdChanged(it) },
                        label = { Text("Worker ID", color = TextSecondary) },
                        placeholder = { Text("e.g. WRK-1001", color = TextTertiary) },
                        leadingIcon = {
                          Icon(Icons.Default.Badge, contentDescription = null, tint = TextSecondary)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CivicGreen,
                            focusedLabelColor = CivicGreen,
                            cursorColor = CivicGreen
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // Mobile Number with +91 Prefix
                    OutlinedTextField(
                        value = uiState.mobileNumberInput,
                        onValueChange = { viewModel.onMobileNumberChanged(it) },
                        label = { Text("Mobile Number", color = TextSecondary) },
                        placeholder = { Text("98765 43210", color = TextTertiary) },
                        prefix = {
                            Text(
                                text = "+91 ",
                                fontWeight = FontWeight.Bold,
                                color = DeepCivicBlue,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = TextSecondary)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (activity != null) viewModel.sendOtp(activity)
                        }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CivicGreen,
                            focusedLabelColor = CivicGreen,
                            cursorColor = CivicGreen
                        )
                    )

                    Spacer(Modifier.height(24.dp))

                    // Send OTP Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (activity != null) {
                                viewModel.sendOtp(activity)
                            }
                        },
                        enabled = !uiState.isLoading && uiState.mobileNumberInput.length >= 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)
                    ) {
                        if (uiState.isLoading) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "SENDING OTP…",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        } else {
                            Text(
                                text = "Send OTP",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                } else {
                    // ══════════════════════════════════════════════════════
                    // SCREEN 2: Verify Mobile Number (6-digit OTP)
                    // ══════════════════════════════════════════════════════
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.onBackToPhone() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = DeepCivicBlue
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Verify Mobile Number",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    Text(
                        text = "OTP sent to:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = uiState.maskedPhoneNumber.ifEmpty { "+91 XXXXX XXXXX" },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = DeepCivicBlue
                        ),
                        modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
                    )

                    // 6-digit OTP Box Input
                    OtpBoxesField(
                        otpValue = uiState.otpInput,
                        onOtpChange = { viewModel.onOtpChanged(it) },
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.verifyOtp()
                        }
                    )

                    Spacer(Modifier.height(24.dp))

                    // Verify OTP Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.verifyOtp()
                        },
                        enabled = !uiState.isLoading && uiState.otpInput.length == 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)
                    ) {
                        if (uiState.isLoading) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "VERIFYING OTP…",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        } else {
                            Text(
                                text = "Verify OTP",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Resend OTP Countdown
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!uiState.isResendEnabled) {
                            val secondsFormatted = String.format("%02d", uiState.resendCountdown)
                            Text(
                                text = "Resend OTP in 00:$secondsFormatted",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        } else {
                            TextButton(
                                onClick = {
                                    if (activity != null) viewModel.resendOtp(activity)
                                }
                            ) {
                                Text(
                                    text = "Resend OTP",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = CivicGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Authorized Municipal Worker Portal Only",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * 6-Box OTP Display & Input Field
 */
@Composable
private fun OtpBoxesField(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    onDone: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { focusRequester.requestFocus() },
        contentAlignment = Alignment.Center
    ) {
        // Hidden transparent text field to capture user typing and keyboard inputs
        BasicTextField(
            value = otpValue,
            onValueChange = { newValue ->
                val filtered = newValue.filter { it.isDigit() }.take(6)
                onOtpChange(filtered)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
            singleLine = true
        )

        // Visual 6 individual rounded boxes
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 6) {
                val char = otpValue.getOrNull(i)?.toString() ?: ""
                val isFocused = otpValue.length == i

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isFocused) SurfaceVariantLight else BackgroundLight)
                        .border(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = if (isFocused) CivicGreen else TextTertiary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (char.isNotEmpty()) char else "—",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (char.isNotEmpty()) DeepCivicBlue else TextTertiary.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

