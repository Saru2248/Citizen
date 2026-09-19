package com.citizenai.worker.presentation.action

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.citizenai.worker.presentation.theme.*
import java.io.File
import java.io.FileOutputStream

@Composable
fun CompleteTaskDialog(
    onDismiss: () -> Unit,
    onSubmit: (notes: String, afterImageFile: File) -> Unit
) {
    val context = LocalContext.current
    var notesText by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var photoFile by remember { mutableStateOf<File?>(null) }
    var cameraTempFile by remember { mutableStateOf<File?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPhotoUri = it
            validationError = null
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val tempFile = File.createTempFile("after_photo_", ".jpg", context.cacheDir)
                val outputStream = FileOutputStream(tempFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                photoFile = tempFile
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraTempFile != null) {
            photoFile = cameraTempFile
            selectedPhotoUri = Uri.fromFile(cameraTempFile)
            validationError = null
        }
    }

    val launchCamera = {
        try {
            val file = File.createTempFile("after_camera_", ".jpg", context.cacheDir)
            cameraTempFile = file
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            validationError = "Failed to launch camera: ${e.message}"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceLight,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CivicGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Complete Work Task",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                validationError?.let { err ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        color = PriorityCriticalContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = err,
                            color = PriorityCritical,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Text(
                    text = "Mandatory Completion Proof:",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (selectedPhotoUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, CivicGreen, RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedPhotoUri),
                            contentDescription = "After Work Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = {
                                selectedPhotoUri = null
                                photoFile = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(28.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove Photo", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { launchCamera() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CAMERA", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, CivicGreen)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = CivicGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GALLERY", fontWeight = FontWeight.Bold, color = CivicGreen, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it; validationError = null },
                    label = { Text("Completion Notes *", color = TextSecondary) },
                    placeholder = { Text("Describe repairs or work done", color = TextTertiary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CivicGreen,
                        focusedLabelColor = CivicGreen,
                        cursorColor = CivicGreen
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (photoFile == null) {
                        validationError = "Please attach mandatory After-Work photo proof."
                        return@Button
                    }
                    if (notesText.isBlank()) {
                        validationError = "Please enter completion notes."
                        return@Button
                    }
                    onSubmit(notesText.trim(), photoFile!!)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CivicGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("SUBMIT COMPLETION", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}
