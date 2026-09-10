package com.citizenai.app.presentation.profile

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
import com.citizenai.app.util.LanguageManager
import com.citizenai.app.ui.theme.*

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userName        by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail       by viewModel.userEmail.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val context         = LocalContext.current

    // Dialog states
    var showEditProfileDialog    by remember { mutableStateOf(false) }
    var showNotificationDialog   by remember { mutableStateOf(false) }
    var showPrivacyDialog        by remember { mutableStateOf(false) }
    var showHelpDialog           by remember { mutableStateOf(false) }
    var showAboutDialog          by remember { mutableStateOf(false) }
    var showLogoutDialog         by remember { mutableStateOf(false) }
    var showLanguageDialog       by remember { mutableStateOf(false) }

    // Edit Profile state
    var editName  by remember(userName)  { mutableStateOf(userName) }
    var editEmail by remember(userEmail) { mutableStateOf(userEmail) }

    // Notification Toggles
    var pushNotifications by remember { mutableStateOf(true) }
    var emailAlerts       by remember { mutableStateOf(true) }
    var statusUpdates     by remember { mutableStateOf(true) }
    var smsNotifications  by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
    ) {
        // ─── Header ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(DeepCivicBlue, CivicBlueMedium)))
                .padding(horizontal = 20.dp)
                .padding(top = 40.dp, bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .background(CivicGreen, CircleShape)
                ) {
                    Text(
                        userName.firstOrNull()?.toString()?.uppercase() ?: "C",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    userName.ifBlank { stringResource(R.string.citizen_user) },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    userEmail.ifBlank { "helpdesk@csc.gov.in" },
                    fontSize = 14.sp,
                    color = TextOnDarkSecondary
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ─── Menu Options ───────────────────────────────────────────────────
        ProfileMenuItem(
            icon    = Icons.Default.Person,
            label   = stringResource(R.string.menu_edit_profile),
            onClick = {
                editName  = userName
                editEmail = userEmail
                showEditProfileDialog = true
            }
        )

        ProfileMenuItem(
            icon    = Icons.Default.Notifications,
            label   = stringResource(R.string.menu_notification_settings),
            onClick = { showNotificationDialog = true }
        )

        ProfileMenuItem(
            icon    = Icons.Default.Security,
            label   = stringResource(R.string.menu_privacy_security),
            onClick = { showPrivacyDialog = true }
        )

        // ─── Language Option ─────────────────────────────────────────────────
        ProfileMenuItem(
            icon    = Icons.Default.Language,
            label   = stringResource(R.string.menu_language),
            onClick = { showLanguageDialog = true },
            trailingLabel = LanguageManager.LANGUAGE_DISPLAY[currentLanguage]
        )

        ProfileMenuItem(
            icon    = Icons.Default.Help,
            label   = stringResource(R.string.menu_help_support),
            onClick = { showHelpDialog = true }
        )

        ProfileMenuItem(
            icon    = Icons.Default.Info,
            label   = stringResource(R.string.menu_about),
            onClick = { showAboutDialog = true }
        )

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Divider)
        Spacer(Modifier.height(12.dp))

        // Logout
        ProfileMenuItem(
            icon       = Icons.Default.Logout,
            label      = stringResource(R.string.logout),
            onClick    = { showLogoutDialog = true },
            tintColor  = PriorityCritical,
            labelColor = PriorityCritical
        )

        Spacer(Modifier.height(32.dp))
    }

    // ─── 1. Edit Profile Dialog ──────────────────────────────────────────────
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text(stringResource(R.string.edit_profile), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value          = editName,
                        onValueChange  = { editName = it },
                        label          = { Text(stringResource(R.string.full_name_label)) },
                        leadingIcon    = { Icon(Icons.Default.Person, null, tint = CivicGreen) },
                        modifier       = Modifier.fillMaxWidth(),
                        shape          = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value          = editEmail,
                        onValueChange  = { editEmail = it },
                        label          = { Text(stringResource(R.string.email_address_label)) },
                        leadingIcon    = { Icon(Icons.Default.Email, null, tint = CivicGreen) },
                        modifier       = Modifier.fillMaxWidth(),
                        shape          = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank() && editEmail.isNotBlank()) {
                            viewModel.updateProfile(editName.trim(), editEmail.trim())
                            Toast.makeText(context, context.getString(R.string.profile_updated), Toast.LENGTH_SHORT).show()
                            showEditProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)
                ) {
                    Text(stringResource(R.string.save_changes), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ─── 2. Notification Settings Dialog ────────────────────────────────────
    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = { Text(stringResource(R.string.menu_notification_settings), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NotificationToggleItem(
                        stringResource(R.string.push_notifications),
                        stringResource(R.string.push_notifications_desc),
                        pushNotifications
                    ) { pushNotifications = it }
                    NotificationToggleItem(
                        stringResource(R.string.email_alerts),
                        stringResource(R.string.email_alerts_desc),
                        emailAlerts
                    ) { emailAlerts = it }
                    NotificationToggleItem(
                        stringResource(R.string.status_updates_label),
                        stringResource(R.string.status_updates_desc),
                        statusUpdates
                    ) { statusUpdates = it }
                    NotificationToggleItem(
                        stringResource(R.string.sms_notifications),
                        stringResource(R.string.sms_notifications_desc),
                        smsNotifications
                    ) { smsNotifications = it }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, context.getString(R.string.notification_saved), Toast.LENGTH_SHORT).show()
                        showNotificationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CivicGreen)
                ) {
                    Text(stringResource(R.string.done), color = Color.White)
                }
            }
        )
    }

    // ─── 3. Privacy & Security Dialog ───────────────────────────────────────
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(stringResource(R.string.menu_privacy_security), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SecurityStatusRow(
                        stringResource(R.string.data_encryption),
                        stringResource(R.string.data_encryption_status),
                        Icons.Default.Lock
                    )
                    SecurityStatusRow(
                        stringResource(R.string.location_access),
                        stringResource(R.string.location_access_status),
                        Icons.Default.LocationOn
                    )
                    SecurityStatusRow(
                        stringResource(R.string.camera_access),
                        stringResource(R.string.camera_access_status),
                        Icons.Default.CameraAlt
                    )

                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, context.getString(R.string.cache_cleared), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(10.dp),
                        border   = BorderStroke(1.dp, PriorityCritical)
                    ) {
                        Icon(Icons.Default.Delete, null, tint = PriorityCritical, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.clear_cache), color = PriorityCritical)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // ─── 4. Help & Support Dialog ────────────────────────────────────────────
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.menu_help_support), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CivicGreenContainer),
                        shape  = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(stringResource(R.string.helpline_title), fontSize = 12.sp, color = CivicGreen, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.helpline_number), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(stringResource(R.string.helpline_desc), fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight),
                        shape  = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(stringResource(R.string.email_helpdesk_label), fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("✉️ helpdesk@csc.gov.in", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DeepCivicBlue)
                        }
                    }

                    Text(stringResource(R.string.faq_title), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(stringResource(R.string.faq_ai), fontSize = 12.sp, color = TextSecondary)
                    Text(stringResource(R.string.faq_resolution_time), fontSize = 12.sp, color = TextSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // ─── 5. About Citizen AI Dialog ──────────────────────────────────────────
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.menu_about), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(10.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter        = androidx.compose.ui.res.painterResource(id = R.drawable.app_logo),
                            contentDescription = "Citizen AI Logo",
                            contentScale   = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier       = Modifier.fillMaxSize()
                        )
                    }
                    Text(stringResource(R.string.about_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Text(stringResource(R.string.about_version), fontSize = 12.sp, color = TextTertiary)
                    HorizontalDivider(color = Divider)
                    Text(
                        stringResource(R.string.about_description),
                        fontSize   = 12.sp,
                        color      = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // ─── 6. Logout Dialog ─────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text  = { Text(stringResource(R.string.logout_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.logout(onLogout) },
                    colors  = ButtonDefaults.buttonColors(containerColor = PriorityCritical)
                ) { Text(stringResource(R.string.logout), color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ─── 7. Language Selection Dialog ─────────────────────────────────────────
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
                                    // Toast shown in original language before Activity recreates
                                    Toast
                                        .makeText(context, context.getString(R.string.language_selected), Toast.LENGTH_SHORT)
                                        .show()
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
}

@Composable
private fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tintColor: Color = TextSecondary,
    labelColor: Color = TextPrimary,
    trailingLabel: String? = null
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
        Text(
            label,
            color    = labelColor,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        if (trailingLabel != null) {
            Text(
                text     = trailingLabel,
                fontSize = 13.sp,
                color    = CivicGreen,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(4.dp))
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun NotificationToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title,    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CivicGreen)
        )
    }
}

@Composable
private fun SecurityStatusRow(
    title: String,
    status: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = CivicGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title,  fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(status, fontSize = 11.sp, color = TextSecondary)
        }
        Icon(Icons.Default.CheckCircle, null, tint = CivicGreen, modifier = Modifier.size(16.dp))
    }
}
