package com.citizenai.worker.presentation.action

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
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
import coil.compose.rememberAsyncImagePainter
import com.citizenai.worker.presentation.theme.*
import java.io.File
import java.io.FileOutputStream

@Composable
fun ProgressDialog(
    onDismiss: () -> Unit,
    onSubmit: (progressPercentage: Int, note: String, photoFile: File?) -> Unit
) {
    val context = LocalContext.current
    var sliderValue by remember { mutableStateOf(50f) }
    var noteText by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var photoFile by remember { mutableStateOf<File?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPhotoUri = it
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val tempFile = File.createTempFile("wip_photo_", ".jpg", context.cacheDir)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceLight,
        title = {
            Text(
                text = "Submit Progress Update",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Completion Level:", color = TextSecondary, fontSize = 14.sp)
                    Text(
                        text = "${sliderValue.toInt()}%",
                        color = CivicGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 10f..95f,
                    steps = 16,
                    colors = SliderDefaults.colors(
                        thumbColor = CivicGreen,
                        activeTrackColor = CivicGreen,
                        inactiveTrackColor = SurfaceVariantLight
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Progress Note", color = TextSecondary) },
                    placeholder = { Text("e.g. Surface preparation completed", color = TextTertiary) },
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

                Spacer(modifier = Modifier.height(16.dp))

                Text("Work Photo (Optional):", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (selectedPhotoUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedPhotoUri),
                            contentDescription = "Progress Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CivicGreen)
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = CivicGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Attach WIP Photo", color = CivicGreen)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(sliderValue.toInt(), noteText.ifBlank { "Progress update" }, photoFile)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CivicGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("SUBMIT UPDATE", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}
