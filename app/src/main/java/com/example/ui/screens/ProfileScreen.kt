package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.NewsAndVideoRepository
import com.example.data.ThemeMode
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.AppleGroupedRow
import com.example.ui.components.GlassCard
import com.example.ui.components.LifeOsBadgeIcon
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.components.UserAvatarCircle
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ProfileScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    snackbarHostState: SnackbarHostState,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAvatarOptions by remember { mutableStateOf(false) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    var cropZoom by remember { mutableFloatStateOf(1f) }
    var showRemoveAvatarConfirm by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            cropZoom = 1f
            pendingCropUri = uri
        }
    }

    // Camera capture launcher (via FileProvider)
    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val captured = tempCameraUri
        if (success && captured != null) {
            cropZoom = 1f
            pendingCropUri = captured
        }
    }

    val launchCamera: () -> Unit = {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        tempCameraUri = uri
        cameraCaptureLauncher.launch(uri)
    }

    // Runtime CAMERA permission launcher (requested ONLY when user taps Take Photo)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Camera permission denied. You can still choose a photo from Gallery.")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("profile_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LifeOsBadgeIcon(type = LifeOsBadgeType.PROFILE, size = 44.dp)
                    Column {
                        Text("Profile & Settings", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Account, Notifications, Appearance & Cloud Sync",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Profile Identity & Circular Avatar Card
            item {
                GlassCard(accentTint = MaterialTheme.colorScheme.primary) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UserAvatarCircle(
                            avatarPath = state.profile.avatarLocalPath,
                            displayName = state.profile.fullName.ifBlank { state.displayName },
                            size = 92.dp,
                            onClick = { showAvatarOptions = true },
                            modifier = Modifier.testTag("profile_avatar_circle")
                        )
                        Text(
                            text = state.profile.fullName.ifBlank { state.displayName },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.profile.username.isNotBlank()) {
                            Text(
                                text = "@${state.profile.username}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (state.email.isNotBlank()) {
                            Text(
                                text = state.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (state.profile.bio.isNotBlank()) {
                            Text(
                                text = state.profile.bio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { showAvatarOptions = true },
                                modifier = Modifier.testTag("profile_change_photo_button")
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Change Photo")
                            }
                            OutlinedButton(
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier.testTag("profile_edit_info_button")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Edit Profile")
                            }
                        }
                    }
                }
            }

            // Automatic Dark / Light / System Theme Selector
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Appearance Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Follows Android system appearance automatically by default.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                ThemeMode.SYSTEM to "System Default",
                                ThemeMode.LIGHT to "Light Mode",
                                ThemeMode.DARK to "Dark Mode"
                            ).forEach { (mode, label) ->
                                FilterChip(
                                    selected = state.preferences.themeMode == mode,
                                    onClick = { viewModel.updateThemeMode(mode) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Apple Grouped Settings & Automation Shortcuts
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Automation & Workspace",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        AppleGroupedRow(
                            badgeType = LifeOsBadgeType.NOTIFICATIONS,
                            title = "Smart Notification Center",
                            subtitle = if (state.notificationPrefs.pauseAll) "Paused • Tap to resume alerts" else "13 smart categories • Quiet hours & history",
                            onClick = { onNavigate("notifications") }
                        )
                        AppleGroupedRow(
                            badgeType = LifeOsBadgeType.AI,
                            title = "LIFE OS AI Personal Assistant",
                            subtitle = "Powered by Firebase AI Logic (Gemini)",
                            onClick = { onNavigate("ai_assistant") }
                        )
                        AppleGroupedRow(
                            badgeType = LifeOsBadgeType.MEMORIES,
                            title = "Private Memories",
                            subtitle = "${state.memories.size} personal photo memories",
                            onClick = { onNavigate("memories") }
                        )
                        AppleGroupedRow(
                            badgeType = LifeOsBadgeType.NOTES,
                            title = "Personal & Code Notes",
                            subtitle = "${state.notes.size} organized notes across 8 folders",
                            onClick = { onNavigate("notes") }
                        )
                        AppleGroupedRow(
                            badgeType = LifeOsBadgeType.STEPS,
                            title = "Automatic Step Counter",
                            subtitle = "${state.stepSensorStatus.todaySensorSteps} sensor steps today",
                            onClick = { onNavigate("steps") }
                        )
                        AppleGroupedRow(
                            badgeType = LifeOsBadgeType.ACHIEVEMENTS,
                            title = "Backup & Export",
                            subtitle = "Firestore cloud sync + JSON & CSV export",
                            onClick = { onNavigate("backup") }
                        )
                        AppleGroupedRow(
                            badgeType = LifeOsBadgeType.SETTINGS,
                            title = "Score Weights & Targets",
                            subtitle = "Customize widgets, goals & daily weights",
                            showDivider = false,
                            onClick = { onNavigate("settings") }
                        )
                    }
                }
            }

            // Honest Security & Integration Status Card
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Text("Privacy & Service Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text("• Firebase Auth + Cloud Firestore + Room Offline Sync: Active", style = MaterialTheme.typography.bodySmall)
                        Text("• Firebase AI Logic (Gemini) + App Check: Active", style = MaterialTheme.typography.bodySmall)
                        Text("• Live Tech News RSS Feeds: Active (no API key needed)", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "• YouTube Data API v3: " + if (NewsAndVideoRepository.hasYouTubeApiKey()) "Configured" else "Optional — add YOUTUBE_API_KEY in AI Studio Secrets for in-app live search (verified YouTube portals active now).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Profile photo is stored securely in app-private storage (zero broad storage permissions).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Sign Out & Delete Data Actions
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.weight(1f).testTag("profile_sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                    OutlinedButton(
                        onClick = { showDeleteAllConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear Data")
                    }
                }
            }
        }
    }

    // Avatar Source Picker Dialog
    if (showAvatarOptions) {
        AlertDialog(
            onDismissRequest = { showAvatarOptions = false },
            title = { Text("Profile Picture") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            showAvatarOptions = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Choose from Gallery")
                    }
                    FilledTonalButton(
                        onClick = {
                            showAvatarOptions = false
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED
                            if (granted) launchCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Take Photo with Camera")
                    }
                    if (state.profile.avatarLocalPath.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                showAvatarOptions = false
                                showRemoveAvatarConfirm = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Remove Photo (Restore Default)")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAvatarOptions = false }) { Text("Close") } }
        )
    }

    // Preview & Crop Zoom Dialog
    pendingCropUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingCropUri = null },
            title = { Text("Preview & Crop Avatar") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(160.dp)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Avatar preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                    Text("Crop Zoom: ${"%.1f".format(cropZoom)}x", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = cropZoom,
                        onValueChange = { cropZoom = it },
                        valueRange = 1f..2.2f
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val chosen = uri
                    val zoom = cropZoom
                    pendingCropUri = null
                    scope.launch {
                        val res = viewModel.saveProfileAvatar(chosen, zoom)
                        snackbarHostState.showSnackbar(
                            if (res.isSuccess) "Profile picture updated!" else "Could not save photo: ${res.exceptionOrNull()?.message}"
                        )
                    }
                }) { Text("Save Avatar") }
            },
            dismissButton = { TextButton(onClick = { pendingCropUri = null }) { Text("Cancel") } }
        )
    }

    // Remove Avatar Confirmation
    if (showRemoveAvatarConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveAvatarConfirm = false },
            title = { Text("Remove Profile Picture?") },
            text = { Text("This will delete your custom profile photo and restore the default illustrated avatar.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.removeProfileAvatar()
                    showRemoveAvatarConfirm = false
                    scope.launch { snackbarHostState.showSnackbar("Restored default avatar") }
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { showRemoveAvatarConfirm = false }) { Text("Cancel") } }
        )
    }

    // Edit Name / Username / Bio Dialog
    if (showEditProfileDialog) {
        var fullName by remember { mutableStateOf(state.profile.fullName.ifBlank { state.displayName }) }
        var username by remember { mutableStateOf(state.profile.username) }
        var bio by remember { mutableStateOf(state.profile.bio) }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, singleLine = true)
                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, singleLine = true)
                    OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Short Bio") }, maxLines = 3)
                }
            },
            confirmButton = {
                Button(
                    enabled = fullName.isNotBlank(),
                    onClick = {
                        viewModel.updateProfile {
                            it.copy(fullName = fullName.trim(), username = username.trim(), bio = bio.trim())
                        }
                        showEditProfileDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text("Clear All Account Data?") },
            text = { Text("This permanently deletes your tasks, habits, finance records, learning progress, and schedules from this device and Firestore.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showDeleteAllConfirm = false
                        scope.launch {
                            viewModel.deleteAllUserData()
                            snackbarHostState.showSnackbar("All user data cleared.")
                        }
                    }
                ) { Text("Delete Everything") }
            },
            dismissButton = { TextButton(onClick = { showDeleteAllConfirm = false }) { Text("Cancel") } }
        )
    }
}
