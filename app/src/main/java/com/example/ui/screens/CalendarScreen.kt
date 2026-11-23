package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.logic.StreakCalculator
import com.example.domain.model.EditLockReason
import com.example.domain.model.EffectiveStatus
import com.example.domain.model.EntryStatus
import com.example.domain.model.Habit
import com.example.domain.model.HabitEntry
import com.example.domain.model.HabitStats
import com.example.domain.time.IndonesianDateFormatter
import com.example.ui.CalendarUiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HabitColorDot
import com.example.ui.theme.StreakkuThemeTokens
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onSelectHabit: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToday: () -> Unit,
    onSetEntryStatus: (Habit, LocalDate, EntryStatus?) -> Unit,
    onAddHabit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDateForSheet by remember { mutableStateOf<LocalDate?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (uiState.selectedHabit == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                EmptyStateCard(
                    icon = Icons.Outlined.CalendarMonth,
                    title = stringResource(R.string.calendar_empty_title),
                    subtitle = stringResource(R.string.calendar_empty_subtitle),
                    actionLabel = stringResource(R.string.today_empty_cta),
                    actionTestTag = "calendar_empty_add_button",
                    onAction = onAddHabit
                )
            }
            return@Box
        }

        val habit = uiState.selectedHabit

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .testTag("calendar_screen_content"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 2.dp,
                bottom = 112.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "habit_selector") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calendar_habit_selector")
                ) {
                    items(uiState.habits, key = { it.id }) { item ->
                        val isSelected = item.id == habit.id
                        HabitSelectorPill(
                            label = item.name,
                            selected = isSelected,
                            testTag = "calendar_habit_chip_${item.id}",
                            onClick = { onSelectHabit(item.id) }
                        )
                    }
                }
            }

            item(key = "month_calendar_card") {
                MonthlyCalendarCard(
                    habit = habit,
                    displayedMonth = uiState.displayedMonth,
                    today = uiState.today,
                    entriesByDate = uiState.entriesByDate,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onJumpToday = onJumpToday,
                    onDateClick = { date -> selectedDateForSheet = date }
                )
            }

            uiState.stats?.let { stats ->
                item(key = "calendar_stats") {
                    CalendarStatsGrid(stats = stats)
                }
            }
        }

        val sheetDate = selectedDateForSheet
        if (sheetDate != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val lockReason = StreakCalculator.editLockReason(habit, sheetDate, uiState.today)
            val effective = StreakCalculator.effectiveStatus(
                habit = habit,
                entriesByDate = uiState.entriesByDate,
                date = sheetDate,
                today = uiState.today
            )

            ModalBottomSheet(
                onDismissRequest = { selectedDateForSheet = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                DateStatusBottomSheetContent(
                    habit = habit,
                    date = sheetDate,
                    currentEffectiveStatus = effective,
                    lockReason = lockReason,
                    onPickStatus = { newStatus ->
                        onSetEntryStatus(habit, sheetDate, newStatus)
                        selectedDateForSheet = null
                    }
                )
            }
        }
    }
}

@Composable
private fun HabitSelectorPill(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = bg,
            border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
            modifier = Modifier.height(36.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    ),
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun MonthlyCalendarCard(
    habit: Habit,
    displayedMonth: YearMonth,
    today: LocalDate,
    entriesByDate: Map<LocalDate, HabitEntry>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToday: () -> Unit,
    onDateClick: (LocalDate) -> Unit
) {
    val weeks = remember(displayedMonth) { buildCalendarWeeks(displayedMonth) }
    val statusColors = StreakkuThemeTokens.statusColors
    val isCurrentMonth = displayedMonth == YearMonth.from(today)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.testTag("calendar_prev_month_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.calendar_prev_month),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = IndonesianDateFormatter.formatMonthYear(displayedMonth),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!isCurrentMonth) {
                        TextButton(
                            onClick = onJumpToday,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("calendar_jump_today_button")
                        ) {
                            Text(
                                text = stringResource(R.string.calendar_today_action),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.testTag("calendar_next_month_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.calendar_next_month),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                IndonesianDateFormatter.weekdayHeaders().forEachIndexed { idx, label ->
                    val isoDay = idx + 1
                    val activeWeekday = habit.activeWeekdays.contains(isoDay)
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (activeWeekday) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            statusColors.inactiveText
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            weeks.forEach { weekDates ->
                val statuses = weekDates.map { date ->
                    date?.let {
                        StreakCalculator.effectiveStatus(habit, entriesByDate, it, today)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    // Translucent 4dp green streak connector lines aligned through the status dots
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        val cellWidth = size.width / 7f
                        val dotCenterY = size.height - 11.dp.toPx()
                        val strokeWidthPx = 4.dp.toPx()

                        for (col in 0 until 6) {
                            val leftDate = weekDates[col]
                            val rightDate = weekDates[col + 1]
                            if (leftDate != null && rightDate != null &&
                                statuses[col] == EffectiveStatus.DONE &&
                                statuses[col + 1] == EffectiveStatus.DONE
                            ) {
                                val startX = cellWidth * col + cellWidth * 0.5f
                                val endX = cellWidth * (col + 1) + cellWidth * 0.5f
                                drawLine(
                                    color = statusColors.doneStreakLine.copy(alpha = 0.35f),
                                    start = Offset(startX, dotCenterY),
                                    end = Offset(endX, dotCenterY),
                                    strokeWidth = strokeWidthPx,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekDates.forEachIndexed { colIndex, date ->
                            if (date == null) {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                )
                            } else {
                                val status = statuses[colIndex] ?: EffectiveStatus.EMPTY
                                val isFuture = date.isAfter(today)
                                CalendarDayCell(
                                    date = date,
                                    isToday = date == today,
                                    isFuture = isFuture,
                                    status = status,
                                    onClick = { onDateClick(date) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CalendarLegendRow()
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    isToday: Boolean,
    isFuture: Boolean,
    status: EffectiveStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = StreakkuThemeTokens.statusColors

    val statusText = when (status) {
        EffectiveStatus.DONE -> stringResource(R.string.status_done)
        EffectiveStatus.FAILED -> stringResource(R.string.status_failed)
        EffectiveStatus.EMPTY -> stringResource(R.string.status_empty)
        EffectiveStatus.INACTIVE -> stringResource(R.string.status_inactive)
    }

    val dayTextColor = when {
        status == EffectiveStatus.INACTIVE -> statusColors.inactiveText
        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    val dotColor: Color? = when {
        isFuture -> null
        status == EffectiveStatus.DONE -> statusColors.done
        status == EffectiveStatus.FAILED -> statusColors.failed
        status == EffectiveStatus.EMPTY -> statusColors.empty
        status == EffectiveStatus.INACTIVE -> null
        else -> null
    }

    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("calendar_day_$date")
            .semantics {
                contentDescription = "${date.dayOfMonth}: $statusText"
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // Today green ring around the date number (matching SVG r=19 stroke=2)
        if (isToday) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(38.dp)
                    .border(2.dp, statusColors.done, CircleShape)
            )
        }

        // Date number
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .height(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                ),
                color = dayTextColor
            )
        }

        // Status dot seated cleanly at bottom center (matching SVG r=5)
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
private fun CalendarLegendRow() {
    val colors = StreakkuThemeTokens.statusColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(dotColor = colors.done, label = stringResource(R.string.status_done))
        LegendItem(dotColor = colors.failed, label = stringResource(R.string.status_failed))
        LegendItem(dotColor = colors.empty, label = stringResource(R.string.status_empty))
    }
}

@Composable
private fun LegendItem(
    dotColor: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalendarStatsGrid(stats: HabitStats) {
    val statusColors = StreakkuThemeTokens.statusColors

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMetricCard(
                title = stringResource(R.string.calendar_stat_current_streak),
                value = stringResource(R.string.streak_days_format, stats.currentStreak),
                valueColor = statusColors.done,
                subtitle = null,
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_current_streak")
            )
            val longestRange = stats.longestStreak?.let {
                IndonesianDateFormatter.formatDateRange(it.startDate, it.endDate)
            }
            StatMetricCard(
                title = stringResource(R.string.calendar_stat_longest_streak),
                value = stringResource(R.string.streak_days_format, stats.longestStreak?.length ?: 0),
                valueColor = MaterialTheme.colorScheme.onSurface,
                subtitle = longestRange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_longest_streak")
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMetricCard(
                title = stringResource(R.string.calendar_stat_total_done),
                value = stringResource(R.string.streak_days_format, stats.totalDoneDays),
                valueColor = MaterialTheme.colorScheme.onSurface,
                subtitle = null,
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_total_done")
            )
            val pct30 = IndonesianDateFormatter.formatPercent(stats.consistency30Days)
            val pctAll = IndonesianDateFormatter.formatPercent(stats.consistencyAllTime)
            StatMetricCard(
                title = stringResource(R.string.calendar_stat_consistency),
                value = pct30,
                valueColor = MaterialTheme.colorScheme.onSurface,
                subtitle = stringResource(R.string.calendar_consistency_detail, pct30, pctAll),
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_consistency")
            )
        }
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    valueColor: Color,
    subtitle: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = valueColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DateStatusBottomSheetContent(
    habit: Habit,
    date: LocalDate,
    currentEffectiveStatus: EffectiveStatus,
    lockReason: EditLockReason,
    onPickStatus: (EntryStatus?) -> Unit
) {
    val colors = StreakkuThemeTokens.statusColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HabitColorDot(colorHex = habit.colorHex, size = 10.dp)
            Text(
                text = habit.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = IndonesianDateFormatter.formatFullDayFullMonth(date),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (lockReason != EditLockReason.NONE) {
            val message = when (lockReason) {
                EditLockReason.FUTURE_DATE -> stringResource(R.string.calendar_lock_future)
                EditLockReason.INACTIVE_WEEKDAY -> stringResource(R.string.calendar_lock_inactive)
                EditLockReason.BACKFILL_LIMIT -> stringResource(
                    R.string.calendar_lock_backfill,
                    habit.backfillLimitDays ?: 0
                )
                EditLockReason.NONE -> ""
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_sheet_locked_banner"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return
        }

        Spacer(modifier = Modifier.height(4.dp))

        StatusChoiceRow(
            label = stringResource(R.string.status_done),
            icon = Icons.Filled.Check,
            accentColor = colors.done,
            containerColor = colors.doneContainer,
            contentColor = colors.onDoneContainer,
            selected = currentEffectiveStatus == EffectiveStatus.DONE,
            testTag = "sheet_pick_done",
            onClick = { onPickStatus(EntryStatus.DONE) }
        )

        StatusChoiceRow(
            label = stringResource(R.string.status_failed),
            icon = Icons.Filled.Close,
            accentColor = colors.failed,
            containerColor = colors.failedContainer,
            contentColor = colors.onFailedContainer,
            selected = currentEffectiveStatus == EffectiveStatus.FAILED,
            testTag = "sheet_pick_failed",
            onClick = { onPickStatus(EntryStatus.FAILED) }
        )

        StatusChoiceRow(
            label = stringResource(R.string.status_empty),
            icon = Icons.Outlined.RadioButtonUnchecked,
            accentColor = colors.empty,
            containerColor = colors.emptyContainer,
            contentColor = colors.onEmptyContainer,
            selected = currentEffectiveStatus == EffectiveStatus.EMPTY,
            testTag = "sheet_pick_empty",
            onClick = { onPickStatus(null) }
        )
    }
}

@Composable
private fun StatusChoiceRow(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    contentColor: Color,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) containerColor else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (selected) 1f else 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) contentColor else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun buildCalendarWeeks(yearMonth: YearMonth): List<List<LocalDate?>> {
    val firstDay = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val leadingBlanks = firstDay.dayOfWeek.value - 1

    val cells = mutableListOf<LocalDate?>()
    repeat(leadingBlanks) { cells.add(null) }
    for (day in 1..daysInMonth) {
        cells.add(yearMonth.atDay(day))
    }
    while (cells.size % 7 != 0) {
        cells.add(null)
    }
    return cells.chunked(7)
}
