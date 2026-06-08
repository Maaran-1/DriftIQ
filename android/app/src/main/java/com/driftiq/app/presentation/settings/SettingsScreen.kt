package com.driftiq.app.presentation.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.driftiq.app.domain.repository.TwinRepository
import com.driftiq.app.presentation.home.DriftIQBottomNav
import com.driftiq.app.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val twinRepository: TwinRepository,
) : ViewModel() {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun resetBaseline() {
        viewModelScope.launch {
            twinRepository.resetTwin()
                .onSuccess { _message.value = "Baseline reset. Recalibration takes 7–14 days." }
                .onFailure { _message.value = "Reset failed. Please try again." }
        }
    }

    fun clearMessage() { _message.value = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val message by viewModel.message.collectAsState()
    var showResetDialog by remember { mutableStateOf(value = false) }

    if (message != null) {
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(3000.milliseconds)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary),
            )
        },
        bottomBar = { DriftIQBottomNav(navController, "settings") },
        snackbarHost = {
            message?.let {
                Snackbar(
                    containerColor = BackgroundCard,
                    contentColor = TextPrimary,
                    modifier = Modifier.padding(16.dp),
                ) { Text(it) }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Account section
            SettingsSectionHeader("Account")
            SettingsRow(icon = Icons.Default.Person, label = "Profile", subtitle = "View account details") {}
            SettingsRow(icon = Icons.Default.Lock, label = "Change Password", subtitle = null) {}

            Spacer(Modifier.height(8.dp))
            SettingsSectionHeader("Calibration")
            SettingsRow(
                icon = Icons.Default.Refresh,
                label = "Reset Baseline",
                subtitle = "Clears your behavioral baseline — takes 7–14 days to rebuild",
                labelColor = Warning,
            ) { showResetDialog = true }

            Spacer(Modifier.height(8.dp))
            SettingsSectionHeader("Notifications")
            var notificationsEnabled by remember { mutableStateOf(value = true) }
            SettingsToggleRow(
                icon = Icons.Default.Notifications,
                label = "Push Notifications",
                subtitle = "Drift alerts and weekly insights",
                checked = notificationsEnabled,
            ) { notificationsEnabled = it }

            Spacer(Modifier.height(8.dp))
            SettingsSectionHeader("Data & Privacy")
            SettingsRow(icon = Icons.Default.Download, label = "Export My Data", subtitle = "Download all your behavioral data") {}
            SettingsRow(icon = Icons.Default.PrivacyTip, label = "Privacy Policy", subtitle = null) {}
            SettingsRow(icon = Icons.Default.Delete, label = "Delete Account", subtitle = "Permanently delete account and data", labelColor = ErrorColor) {}

            Spacer(Modifier.height(8.dp))
            SettingsSectionHeader("About")
            SettingsRow(icon = Icons.Default.Info, label = "Version", subtitle = "1.0.0") {}
            SettingsRow(icon = Icons.Default.Star, label = "Rate DriftIQ", subtitle = null) {}
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                containerColor = BackgroundCard,
                title = { Text("Reset Baseline?", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "This will clear your current behavioral baseline. You will need 7–14 days to rebuild it. Your historical data will be preserved.",
                        color = TextSecondary, fontSize = 14.sp,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showResetDialog = false; viewModel.resetBaseline() }) {
                        Text("Reset", color = Warning, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        color = TextTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String?,
    labelColor: androidx.compose.ui.graphics.Color = TextPrimary,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = BackgroundCard,
        border = BorderStroke(1.dp, BackgroundElevated),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, null, tint = BrandSecondary, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = labelColor, fontSize = 14.sp)
                subtitle?.let { Text(it, color = TextTertiary, fontSize = 12.sp) }
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = BackgroundCard,
        border = BorderStroke(1.dp, BackgroundElevated),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, null, tint = BrandSecondary, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = TextPrimary, fontSize = 14.sp)
                subtitle?.let { Text(it, color = TextTertiary, fontSize = 12.sp) }
            }
            Switch(
                checked = checked, onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = BrandPrimary, uncheckedTrackColor = BackgroundElevated),
            )
        }
    }
}
