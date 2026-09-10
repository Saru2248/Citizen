package com.citizenai.app.presentation.report.capture

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.citizenai.app.ui.theme.*
import com.google.accompanist.permissions.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReportCaptureScreen(
    onImageCaptured: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ReportCaptureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCameraPreview by remember { mutableStateOf(false) }

    // Camera permission
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Gallery launcher (Android 13+ photo picker)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Copy to a cache file for upload
            val file = copyUriToFile(context, it)
            viewModel.onGalleryImageSelected(it, file)
        }
    }

    // Camera output URI and temp file tracker
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraLaunch by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null && currentPhotoFile != null) {
            viewModel.onImageCaptured(cameraImageUri!!, currentPhotoFile!!)
        }
    }

    fun launchCamera() {
        try {
            val imageFile = createImageFile(context)
            currentPhotoFile = imageFile
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            viewModel.onError("Unable to launch camera: ${e.localizedMessage}")
        }
    }

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted && pendingCameraLaunch) {
            pendingCameraLaunch = false
            launchCamera()
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Column {
                        Text("Report Issue", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Step 1 of 3", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, "Cancel", tint = TextPrimary)
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
            // Progress bar
            StepProgressBar(currentStep = 1, totalSteps = 3)
            Spacer(Modifier.height(24.dp))

            if (uiState.capturedImageUri != null) {
                // ─── Preview Mode ──────────────────────────────────────────────
                Text("Image Preview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = uiState.capturedImageUri,
                        contentDescription = "Captured issue",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Remove button
                    IconButton(
                        onClick = viewModel::removeImage,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "Remove image", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = viewModel::removeImage,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, TextTertiary)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retake", color = TextSecondary)
                    }
                    Button(
                        onClick = onImageCaptured,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)
                    ) {
                        Text("Continue", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                // ─── Capture Options ───────────────────────────────────────────
                Text("Capture the Issue", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Take a clear photo of the problem",
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(Modifier.height(48.dp))

                // Camera option
                CaptureOptionCard(
                    icon = Icons.Default.CameraAlt,
                    title = "Camera",
                    subtitle = "Take a photo right now",
                    backgroundColor = CivicGreenContainer,
                    iconColor = CivicGreen
                ) {
                    if (cameraPermissionState.status.isGranted) {
                        launchCamera()
                    } else {
                        pendingCameraLaunch = true
                        cameraPermissionState.launchPermissionRequest()
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Gallery option
                CaptureOptionCard(
                    icon = Icons.Default.Image,
                    title = "Gallery",
                    subtitle = "Choose from your photos",
                    backgroundColor = CivicBlueContainer,
                    iconColor = DeepCivicBlue
                ) {
                    galleryLauncher.launch("image/*")
                }

                // Permission denial message
                if (cameraPermissionState.status.shouldShowRationale) {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PriorityHighContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Camera permission is required to capture issues. Please grant permission in Settings.",
                            modifier = Modifier.padding(12.dp),
                            color = PriorityHigh,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Error
            uiState.error?.let { error ->
                Spacer(Modifier.height(12.dp))
                Text(error, color = PriorityCritical, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CaptureOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(backgroundColor, RoundedCornerShape(14.dp))
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary)
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextTertiary)
        }
    }
}

@Composable
fun StepProgressBar(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(totalSteps) { index ->
            val isCompleted = index < currentStep - 1
            val isCurrent = index == currentStep - 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        when {
                            isCompleted || isCurrent -> CivicGreen
                            else -> Divider
                        }
                    )
            )
        }
    }
}

// Utilities
private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile("CITIZEN_AI_${timeStamp}_", ".jpg", storageDir)
}

private fun copyUriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "gallery_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { inputStream.copyTo(it) }
        file
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmallTopAppBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit,
    colors: TopAppBarColors
) {
    TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        colors = colors
    )
}
