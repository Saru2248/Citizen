package com.citizenai.app.presentation.worker.profile

import android.widget.Toast
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citizenai.app.R
import com.citizenai.app.presentation.profile.ProfileViewModel
import com.citizenai.app.util.LanguageManager
import com.citizenai.app.ui.theme.*

@Composable
fun WorkerProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    var editName by remember(userName) { mutableStateOf(userName) }
    var editEmail by remember(userEmail) { mutableStateOf(userEmail) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(DeepCivicBlue, CivicBlueMedium)))
                .padding(20.dp)
                .padding(top = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp).background(CivicGreen, CircleShape)
                ) {
                    Icon(Icons.Default.Engineering, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    userName.ifBlank { stringResource(R.string.worker_user) },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    stringResource(R.string.worker_subtitle),
                    fontSize = 13.sp,
                    color = CivicGreenLight
                )
                Text(
                    userEmail.ifBlank { "helpdesk@csc.gov.in" },
                    fontSize = 13.sp,
                    color = TextOnDarkSecondary
                )
            }
        }

        // Stats
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("47", stringResource(R.string.stat_completed))
                StatItem("3",  stringResource(R.string.stat_in_progress))
                StatItem("4.2h", stringResource(R.string.stat_avg_time))
            }
        }

        Spacer(Modifier.height(8.dp))

        // Worker options
        WorkerMenuItem(Icons.Default.Person, stringResource(R.string.menu_edit_profile)) {
            editName = userName
            editEmail = userEmail
            showEditDialog = true
        }

        WorkerMenuItem(
            icon = Icons.Default.Language,
            label = stringResource(R.string.menu_language),
            trailingLabel = LanguageManager.LANGUAGE_DISPLAY[currentLanguage]
        ) {
            showLanguageDialog = true
        }

        WorkerMenuItem(Icons.Default.HeadsetMic, stringResource(R.string.menu_field_helpline)) {
            showHelpDialog = true
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Divider)
        Spacer(Modifier.height(12.dp))

        WorkerMenuItem(
            icon       = Icons.Default.Logout,
            label      = stringResource(R.string.logout),
            tintColor  = PriorityCritical,
            labelColor = PriorityCritical
        ) {
            showLogoutDialog = true
        }

        Spacer(Modifier.height(32.dp))
    }

    // Edit Profile Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.edit_profile), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(R.string.worker_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text(stringResource(R.string.email_address_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(editName.trim(), editEmail.trim())
                        Toast.makeText(context, context.getString(R.string.profile_saved), Toast.LENGTH_SHORT).show()
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)
                ) { Text(stringResource(R.string.save), color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Field Helpline Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.field_helpline_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.field_helpline_number),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        stringResource(R.string.field_helpline_email),
                        fontWeight = FontWeight.SemiBold,
                        color = DeepCivicBlue
                    )
                    Text(
                        stringResource(R.string.field_helpline_office),
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Text(
                    stringResource(R.string.language_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LanguageManager.SUPPORTED_LANGUAGES.forEach { code ->
                        val displayName = LanguageManager.LANGUAGE_DISPLAY[code] ?: code
                        val isSelected  = code == currentLanguage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLanguageDialog = false
                                    viewModel.setLanguage(code)
                                    Toast.makeText(context, context.getString(R.string.language_selected), Toast.LENGTH_SHORT).show()
                                }
                                .background(
                                    if (isSelected) CivicGreenContainer else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = if (isSelected) CivicGreen else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text       = displayName,
                                fontSize   = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color      = if (isSelected) CivicGreen else TextPrimary,
                                modifier   = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CivicGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text  = { Text(stringResource(R.string.logout_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.logout(onLogout) },
                    colors = ButtonDefaults.buttonColors(containerColor = PriorityCritical)
                ) { Text(stringResource(R.string.logout), color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun WorkerMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tintColor: Color = TextSecondary,
    labelColor: Color = TextPrimary,
    trailingLabel: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tintColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = labelColor, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (trailingLabel != null) {
            Text(trailingLabel, fontSize = 14.sp, color = CivicGreen, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CivicGreen)
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}
