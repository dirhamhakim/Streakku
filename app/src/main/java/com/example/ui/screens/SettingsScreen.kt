package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.model.AppSettings
import com.example.domain.model.BackupPayload
import com.example.domain.model.ThemeMode
import com.example.domain.time.IndonesianDateFormatter
import com.example.ui.BackupDialogState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    today: LocalDate,
    backupState: BackupDialogState,
    onSelectTheme: (ThemeMode) -> Unit,
    onSetReminderEnabled: (Boolean) -> Unit,
    onSetReminderTime: (Int, Int) -> Unit,
    onCreateBackupJson: suspend () -> Pair<String, BackupPayload>,
    onExportSuccess: (Int, Int) -> Unit,
    onExportError: (String) -> Unit,
    onPrepareImportJson: (String) -> Unit,
    onImportReadError: (String) -> Unit,
    onConfirmImport: () -> Unit,
    onDismissImportDialog: () -> Unit,
    onDismissBackupError: () -> Unit,
    onConsumeFeedback: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val permissionDeniedText = stringResource(R.string.settings_reminder_permission_denied)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onSetReminderEnabled(true)
        } else {
            onSetReminderEnabled(false)
            scope.launch {
                snackbarHostState.showSnackbar(permissionDeniedText)
            }
        }
    }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val (jsonText, payload) = onCreateBackupJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonText.toByteArray(Charsets.UTF_8))
                        out.flush()
                    } ?: error("Tidak dapat membuka file tujuan.")
                }
                onExportSuccess(payload.habits.size, payload.entries.size)
            } catch (e: Exception) {
                onExportError(e.message ?: "kesalahan penyimpanan")
            }
        }
    }

    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val rawText = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).readText()
                    } ?: error("Tidak dapat membaca file yang dipilih.")
                }
                onPrepareImportJson(rawText)
            } catch (e: Exception) {
                onImportReadError(e.message ?: "kesalahan membaca file")
            }
        }
    }

    LaunchedEffect(backupState.feedbackMessage) {
        val message = backupState.feedbackMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onConsumeFeedback()
        }
    }

    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = Color.White,
        uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
        uncheckedBorderColor = Color.Transparent
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Appearance / Theme
                SettingsSectionCard(title = stringResource(R.string.settings_section_appearance)) {
                    ThemeOptionRow(
                        label = stringResource(R.string.settings_theme_system),
                        selected = settings.themeMode == ThemeMode.SYSTEM,
                        testTag = "theme_option_system",
                        onClick = { onSelectTheme(ThemeMode.SYSTEM) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ThemeOptionRow(
                        label = stringResource(R.string.settings_theme_light),
                        selected = settings.themeMode == ThemeMode.LIGHT,
                        testTag = "theme_option_light",
                        onClick = { onSelectTheme(ThemeMode.LIGHT) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ThemeOptionRow(
                        label = stringResource(R.string.settings_theme_dark),
                        selected = settings.themeMode == ThemeMode.DARK,
                        testTag = "theme_option_dark",
                        onClick = { onSelectTheme(ThemeMode.DARK) }
                    )
                }

                // Daily reminder
                SettingsSectionCard(title = stringResource(R.string.settings_section_reminder)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_reminder_toggle),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.settings_reminder_desc),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.reminderEnabled,
                            onCheckedChange = { wantEnabled ->
                                if (!wantEnabled) {
                                    onSetReminderEnabled(false)
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            onSetReminderEnabled(true)
                                        } else {
                                            notificationPermissionLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        }
                                    } else {
                                        onSetReminderEnabled(true)
                                    }
                                }
                            },
                            colors = switchColors,
                            modifier = Modifier.testTag("settings_reminder_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showTimePickerDialog = true }
                            .padding(horizontal = 4.dp)
                            .testTag("settings_reminder_time_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.settings_reminder_time),
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = IndonesianDateFormatter.formatTime(
                                    settings.reminderHour,
                                    settings.reminderMinute
                                ),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Backup JSON
                SettingsSectionCard(title = stringResource(R.string.settings_section_backup)) {
                    SettingsActionRow(
                        icon = Icons.Outlined.Upload,
                        title = stringResource(R.string.settings_export_title),
                        subtitle = stringResource(R.string.settings_export_desc),
                        testTag = "settings_export_button",
                        onClick = {
                            val suggestedName = IndonesianDateFormatter.formatBackupFilename(today)
                            exportDocumentLauncher.launch(suggestedName)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsActionRow(
                        icon = Icons.Outlined.Download,
                        title = stringResource(R.string.settings_import_title),
                        subtitle = stringResource(R.string.settings_import_desc),
                        testTag = "settings_import_button",
                        onClick = {
                            importDocumentLauncher.launch(
                                arrayOf("application/json", "text/plain", "*/*")
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = settings.reminderHour,
            initialMinute = settings.reminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text(stringResource(R.string.settings_reminder_time)) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSetReminderTime(timePickerState.hour, timePickerState.minute)
                        showTimePickerDialog = false
                    },
                    modifier = Modifier.testTag("confirm_reminder_time_button")
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val pendingPayload = backupState.pendingImportPayload
    if (pendingPayload != null) {
        AlertDialog(
            onDismissRequest = onDismissImportDialog,
            title = { Text(stringResource(R.string.settings_import_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.settings_import_dialog_message,
                        pendingPayload.habits.size,
                        pendingPayload.entries.size
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmImport,
                    modifier = Modifier.testTag("confirm_import_backup_button")
                ) {
                    Text(stringResource(R.string.settings_import_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissImportDialog) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val errorMessage = backupState.errorMessage
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissBackupError,
            title = { Text("Gagal Memproses Cadangan") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = onDismissBackupError) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
