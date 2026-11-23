package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.ChangeLogRecord
import com.example.domain.model.ChangeLogStatus
import com.example.domain.model.StreakRecord
import com.example.domain.time.IndonesianDateFormatter
import com.example.ui.HistoryUiState
import com.example.ui.theme.StreakkuThemeTokens
import java.time.LocalDate

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onSelectHabitFilter: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val showingAllHabits = uiState.selectedHabitFilterId == null

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .testTag("history_screen_list"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 2.dp,
                bottom = 112.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (uiState.habits.isNotEmpty()) {
                item(key = "history_filter_pills") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_habit_filter_row")
                    ) {
                        item(key = "filter_all") {
                            HistoryFilterPill(
                                label = stringResource(R.string.history_filter_all),
                                selected = uiState.selectedHabitFilterId == null,
                                testTag = "history_filter_all",
                                onClick = { onSelectHabitFilter(null) }
                            )
                        }
                        items(uiState.habits, key = { it.id }) { habit ->
                            HistoryFilterPill(
                                label = habit.name,
                                selected = uiState.selectedHabitFilterId == habit.id,
                                testTag = "history_filter_${habit.id}",
                                onClick = { onSelectHabitFilter(habit.id) }
                            )
                        }
                    }
                }
            }

            item(key = "section_streaks_header") {
                Text(
                    text = stringResource(R.string.history_streaks_section),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }

            if (uiState.streaks.isEmpty()) {
                item(key = "streaks_empty") {
                    InlineEmptyBox(text = stringResource(R.string.history_streaks_empty))
                }
            } else {
                items(
                    items = uiState.streaks,
                    key = { "${it.habitId}_${it.startDate}_${it.endDate}" }
                ) { streak ->
                    StreakHistoryCard(
                        streak = streak,
                        showHabitName = showingAllHabits
                    )
                }
            }

            item(key = "section_logs_header") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.history_logs_section),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            if (uiState.recentLogs.isEmpty()) {
                item(key = "logs_empty") {
                    InlineEmptyBox(text = stringResource(R.string.history_logs_empty))
                }
            } else {
                items(
                    items = uiState.recentLogs,
                    key = { "log_${it.id}" }
                ) { log ->
                    ChangeLogCard(
                        log = log,
                        today = uiState.today
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterPill(
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
private fun StreakHistoryCard(
    streak: StreakRecord,
    showHabitName: Boolean
) {
    val dateRangeText = IndonesianDateFormatter.formatStreakRange(
        start = streak.startDate,
        end = streak.endDate,
        isOngoing = streak.isOngoing
    )
    val subtitleText = if (showHabitName) {
        "${streak.habitName} · $dateRangeText"
    } else {
        dateRangeText
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("streak_record_${streak.habitId}_${streak.startDate}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left green pill badge with bold streak number
            Surface(
                shape = RoundedCornerShape(17.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .width(46.dp)
                    .height(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = streak.length.toString(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.streak_days_format, streak.length),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (streak.isOngoing) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = stringResource(R.string.history_ongoing_badge),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangeLogCard(
    log: ChangeLogRecord,
    today: LocalDate
) {
    val statusColors = StreakkuThemeTokens.statusColors
    val dotColor = when (log.newStatus) {
        ChangeLogStatus.DONE -> statusColors.done
        ChangeLogStatus.FAILED -> statusColors.failed
        ChangeLogStatus.EMPTY -> statusColors.empty
    }
    val statusLabel = when (log.newStatus) {
        ChangeLogStatus.DONE -> stringResource(R.string.status_done)
        ChangeLogStatus.FAILED -> stringResource(R.string.status_failed)
        ChangeLogStatus.EMPTY -> stringResource(R.string.status_empty)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("change_log_${log.id}"),
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
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${log.habitName} · ${IndonesianDateFormatter.formatDayShortMonth(log.date)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = IndonesianDateFormatter.formatRelativeTimestamp(log.changedAt, today),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.history_log_changed_to, statusLabel),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun InlineEmptyBox(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
