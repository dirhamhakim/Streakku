package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.EffectiveStatus
import com.example.domain.model.Habit
import com.example.ui.TodayHabitCardUi
import com.example.ui.TodayUiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HabitColorDot
import com.example.ui.theme.StreakkuThemeTokens
import com.example.ui.theme.parseHexColor

@Composable
fun TodayScreen(
    uiState: TodayUiState,
    onCycleStatus: (TodayHabitCardUi) -> Unit,
    onEditHabit: (String) -> Unit,
    onAddHabit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showArchived by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .testTag("today_habit_list"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 4.dp,
                bottom = 112.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "today_summary_card") {
                TodayGreenBanner(
                    completed = uiState.completedTodayCount,
                    total = uiState.totalScheduledTodayCount
                )
            }

            if (uiState.isLoaded && uiState.scheduledHabits.isEmpty() && uiState.offDayHabits.isEmpty()) {
                item(key = "today_empty_state") {
                    Spacer(modifier = Modifier.height(6.dp))
                    EmptyStateCard(
                        icon = Icons.Outlined.AddTask,
                        title = stringResource(R.string.today_empty_title),
                        subtitle = stringResource(R.string.today_empty_subtitle),
                        actionLabel = stringResource(R.string.today_empty_cta),
                        actionTestTag = "today_empty_add_button",
                        onAction = onAddHabit
                    )
                }
            }

            items(
                items = uiState.scheduledHabits,
                key = { it.habit.id }
            ) { card ->
                TodayHabitCard(
                    card = card,
                    isScheduledToday = true,
                    onToggleStatus = { onCycleStatus(card) },
                    onOpenEdit = { onEditHabit(card.habit.id) }
                )
            }

            if (uiState.scheduledHabits.isNotEmpty()) {
                item(key = "today_tap_hint") {
                    Text(
                        text = stringResource(R.string.today_tap_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )
                }
            }

            if (uiState.offDayHabits.isNotEmpty()) {
                item(key = "offday_header") {
                    Text(
                        text = stringResource(R.string.today_inactive_section),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp, start = 4.dp)
                    )
                }
                items(
                    items = uiState.offDayHabits,
                    key = { "off_${it.habit.id}" }
                ) { card ->
                    TodayHabitCard(
                        card = card,
                        isScheduledToday = false,
                        onToggleStatus = {},
                        onOpenEdit = { onEditHabit(card.habit.id) }
                    )
                }
            }

            if (uiState.archivedHabits.isNotEmpty()) {
                item(key = "archived_toggle") {
                    ArchivedSectionHeader(
                        count = uiState.archivedHabits.size,
                        expanded = showArchived,
                        onToggle = { showArchived = !showArchived }
                    )
                }
                if (showArchived) {
                    items(
                        items = uiState.archivedHabits,
                        key = { "arch_${it.id}" }
                    ) { archivedHabit ->
                        ArchivedHabitRow(
                            habit = archivedHabit,
                            onClick = { onEditHabit(archivedHabit.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayGreenBanner(
    completed: Int,
    total: Int
) {
    val statusColors = StreakkuThemeTokens.statusColors
    val targetProgress = if (total > 0) (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "todayProgress"
    )

    val subtitleText = when {
        total == 0 -> stringResource(R.string.today_subtitle_start)
        completed >= total -> stringResource(R.string.today_subtitle_all_done)
        completed > 0 -> stringResource(R.string.today_subtitle_in_progress)
        else -> stringResource(R.string.today_subtitle_start)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_summary_banner"),
        shape = RoundedCornerShape(20.dp),
        color = statusColors.done
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = if (total > 0) {
                    stringResource(R.string.today_summary_format, completed, total)
                } else {
                    stringResource(R.string.today_summary_none_scheduled)
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColors.bannerSubtitle
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColors.bannerProgressTrack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
private fun TodayHabitCard(
    card: TodayHabitCardUi,
    isScheduledToday: Boolean,
    onToggleStatus: () -> Unit,
    onOpenEdit: () -> Unit
) {
    val habitColor = parseHexColor(card.habit.colorHex, MaterialTheme.colorScheme.primary)
    val statusColors = StreakkuThemeTokens.statusColors

    val circleFill by animateColorAsState(
        targetValue = when (card.todayStatus) {
            EffectiveStatus.DONE -> statusColors.done
            EffectiveStatus.FAILED -> statusColors.failed
            EffectiveStatus.EMPTY, EffectiveStatus.INACTIVE -> Color.Transparent
        },
        label = "statusCircleFill"
    )

    val statusTextForA11y = when (card.todayStatus) {
        EffectiveStatus.DONE -> stringResource(R.string.status_done)
        EffectiveStatus.FAILED -> stringResource(R.string.status_failed)
        EffectiveStatus.EMPTY -> stringResource(R.string.status_empty)
        EffectiveStatus.INACTIVE -> stringResource(R.string.status_inactive)
    }

    val streakSubtitle = if (card.currentStreak > 0) {
        stringResource(R.string.today_streak_label, card.currentStreak)
    } else {
        stringResource(R.string.today_no_streak_label)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_card_${card.habit.id}")
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenEdit),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp, bottom = 15.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left flush vertical color accent strip matching SVG mockup
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                    .background(habitColor)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = card.habit.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (isScheduledToday) {
                        streakSubtitle
                    } else {
                        "${stringResource(R.string.status_inactive)} · $streakSubtitle"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 48dp accessible circular status button (44dp visual circle matching SVG r=22)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .then(
                        if (isScheduledToday) {
                            Modifier.clickable(onClick = onToggleStatus)
                        } else {
                            Modifier
                        }
                    )
                    .testTag("today_status_button_${card.habit.id}")
                    .semantics { contentDescription = "${card.habit.name}: $statusTextForA11y" },
                contentAlignment = Alignment.Center
            ) {
                when (card.todayStatus) {
                    EffectiveStatus.DONE -> {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(circleFill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    EffectiveStatus.FAILED -> {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(circleFill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    EffectiveStatus.EMPTY -> {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .border(
                                    width = 2.5.dp,
                                    color = statusColors.emptyRing,
                                    shape = CircleShape
                                )
                        )
                    }
                    EffectiveStatus.INACTIVE -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Libur",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedSectionHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .testTag("archived_section_toggle"),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Archive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.today_archived_section, count),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ArchivedHabitRow(
    habit: Habit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("archived_habit_${habit.id}"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HabitColorDot(colorHex = habit.colorHex, size = 10.dp)
            Text(
                text = habit.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.cd_edit_habit),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
