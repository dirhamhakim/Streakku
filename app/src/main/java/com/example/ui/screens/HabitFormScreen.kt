package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.Habit
import com.example.domain.time.IndonesianDateFormatter
import com.example.ui.theme.HabitColorPalette
import com.example.ui.theme.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitFormScreen(
    habitId: String?,
    loadHabit: suspend (String) -> Habit?,
    onSave: (
        existingHabit: Habit?,
        name: String,
        colorHex: String,
        autoFail: Boolean,
        backfillLimitDays: Int?,
        activeWeekdays: Set<Int>
    ) -> Unit,
    onToggleArchive: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var existingHabit by remember(habitId) { mutableStateOf<Habit?>(null) }
    var initialized by remember(habitId) { mutableStateOf(habitId == null) }

    var name by remember(habitId) { mutableStateOf("") }
    var nameError by remember(habitId) { mutableStateOf(false) }
    var selectedColor by remember(habitId) { mutableStateOf(HabitColorPalette.first()) }
    var autoFail by remember(habitId) { mutableStateOf(false) }
    var backfillEnabled by remember(habitId) { mutableStateOf(false) }
    var backfillText by remember(habitId) { mutableStateOf("7") }
    var backfillError by remember(habitId) { mutableStateOf(false) }
    var activeWeekdays by remember(habitId) { mutableStateOf(Habit.ALL_WEEKDAYS) }
    var showMinWeekdayWarning by remember(habitId) { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(habitId) {
        if (habitId != null) {
            val loaded = loadHabit(habitId)
            if (loaded != null) {
                existingHabit = loaded
                name = loaded.name
                selectedColor = loaded.colorHex
                autoFail = loaded.autoFail
                backfillEnabled = loaded.backfillLimitDays != null
                backfillText = loaded.backfillLimitDays?.toString() ?: "7"
                activeWeekdays = loaded.activeWeekdays
            }
            initialized = true
        }
    }

    val isEditMode = existingHabit != null
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
                        text = stringResource(
                            if (isEditMode) R.string.form_title_edit else R.string.form_title_create
                        ),
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
                        modifier = Modifier.testTag("form_back_button")
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (!initialized) return@Scaffold

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
                // 1. Nama kegiatan
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormSectionLabel(text = stringResource(R.string.form_name_label))
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (nameError && it.isNotBlank()) nameError = false
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.form_name_placeholder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        isError = nameError,
                        supportingText = if (nameError) {
                            { Text(stringResource(R.string.form_name_error_empty)) }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            errorContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("habit_name_input")
                    )
                }

                // 2. Warna (6 circular swatches with outer ring when selected)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormSectionLabel(text = stringResource(R.string.form_color_label))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HabitColorPalette.forEachIndexed { index, hex ->
                            val swatchColor = parseHexColor(hex)
                            val isSelected = selectedColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable { selectedColor = hex }
                                    .testTag("color_swatch_$index")
                                    .semantics { contentDescription = "Warna ${index + 1}" },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .border(
                                                width = 2.dp,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                shape = CircleShape
                                            )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                )
                            }
                        }
                    }
                }

                // 3. Aturan opsional (3 separate clean cards matching SVG mockup)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormSectionLabel(text = stringResource(R.string.form_optional_rules_title))

                    // Card A: Otomatis gagal
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.form_auto_fail_title),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.form_auto_fail_desc),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoFail,
                                onCheckedChange = { autoFail = it },
                                colors = switchColors,
                                modifier = Modifier.testTag("auto_fail_switch")
                            )
                        }
                    }

                    // Card B: Batas waktu input
                    val parsedDaysPreview = backfillText.toIntOrNull()
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.form_backfill_title),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (!backfillEnabled) {
                                            stringResource(R.string.form_backfill_unlimited_desc)
                                        } else if (parsedDaysPreview != null && parsedDaysPreview >= 0) {
                                            stringResource(
                                                R.string.form_backfill_limited_desc,
                                                parsedDaysPreview
                                            )
                                        } else {
                                            stringResource(R.string.form_backfill_input_label)
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = backfillEnabled,
                                    onCheckedChange = { enabled ->
                                        backfillEnabled = enabled
                                        if (!enabled) {
                                            backfillError = false
                                        } else if (backfillText.isBlank()) {
                                            backfillText = "7"
                                        }
                                    },
                                    colors = switchColors,
                                    modifier = Modifier.testTag("backfill_limit_switch")
                                )
                            }

                            AnimatedVisibility(visible = backfillEnabled) {
                                OutlinedTextField(
                                    value = backfillText,
                                    onValueChange = { input ->
                                        backfillText = input.filter { it.isDigit() }
                                        backfillError = false
                                    },
                                    label = { Text(stringResource(R.string.form_backfill_input_label)) },
                                    isError = backfillError,
                                    supportingText = if (backfillError) {
                                        { Text(stringResource(R.string.form_backfill_error)) }
                                    } else null,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("backfill_limit_input")
                                )
                            }
                        }
                    }

                    // Card C: Hari aktif (7 circular weekday buttons S S R K J S M)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = stringResource(R.string.form_active_weekdays_title),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.form_active_weekdays_helper),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (1..7).forEach { isoDay ->
                                    val selected = activeWeekdays.contains(isoDay)
                                    val fullDayName = IndonesianDateFormatter.dayFull(isoDay)
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                if (selected) {
                                                    if (activeWeekdays.size <= 1) {
                                                        showMinWeekdayWarning = true
                                                    } else {
                                                        activeWeekdays = activeWeekdays - isoDay
                                                        showMinWeekdayWarning = false
                                                    }
                                                } else {
                                                    activeWeekdays = activeWeekdays + isoDay
                                                    showMinWeekdayWarning = false
                                                }
                                            }
                                            .testTag("weekday_chip_$isoDay")
                                            .semantics {
                                                contentDescription = "$fullDayName ${if (selected) "aktif" else "nonaktif"}"
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (selected) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = IndonesianDateFormatter.dayInitial(isoDay),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (showMinWeekdayWarning) {
                                Text(
                                    text = stringResource(R.string.form_active_weekdays_min_error),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Primary Save pill button
                Button(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isEmpty()) {
                            nameError = true
                            return@Button
                        }
                        val parsedBackfill = if (!backfillEnabled) {
                            null
                        } else {
                            val parsed = backfillText.toIntOrNull()
                            if (parsed == null || parsed < 0) {
                                backfillError = true
                                return@Button
                            }
                            parsed
                        }
                        onSave(
                            existingHabit,
                            trimmed,
                            selectedColor,
                            autoFail,
                            parsedBackfill,
                            activeWeekdays
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_habit_button"),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(
                            if (isEditMode) R.string.form_save_edit else R.string.form_save_create
                        ),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Edit mode centered subtle text actions: Archive & Delete
                val currentHabit = existingHabit
                if (currentHabit != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TextButton(
                            onClick = {
                                onToggleArchive(currentHabit.id, !currentHabit.archived)
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("archive_habit_button")
                        ) {
                            Text(
                                text = stringResource(
                                    if (currentHabit.archived) R.string.form_unarchive
                                    else R.string.form_archive
                                ),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("delete_habit_button")
                        ) {
                            Text(
                                text = stringResource(R.string.form_delete),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    val habitToDelete = existingHabit
    if (showDeleteDialog && habitToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.form_delete_dialog_title)) },
            text = {
                Text(stringResource(R.string.form_delete_dialog_message, habitToDelete.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(habitToDelete.id)
                    },
                    modifier = Modifier.testTag("confirm_delete_habit_button")
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
