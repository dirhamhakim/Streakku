package com.example.domain.model

import java.time.LocalDate

enum class EntryStatus {
    DONE,
    FAILED
}

enum class EffectiveStatus {
    DONE,
    FAILED,
    EMPTY,
    INACTIVE
}

enum class ChangeLogStatus {
    DONE,
    FAILED,
    EMPTY
}

enum class EditLockReason {
    NONE,
    FUTURE_DATE,
    BACKFILL_LIMIT,
    INACTIVE_WEEKDAY
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class Habit(
    val id: String,
    val name: String,
    val colorHex: String,
    val createdDate: LocalDate,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
    val autoFail: Boolean = false,
    val backfillLimitDays: Int? = null,
    val activeWeekdays: Set<Int> = ALL_WEEKDAYS
) {
    companion object {
        val ALL_WEEKDAYS: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7)
    }
}

data class HabitEntry(
    val habitId: String,
    val date: LocalDate,
    val status: EntryStatus,
    val updatedAt: Long
)

data class ChangeLogRecord(
    val id: Long,
    val habitId: String,
    val habitName: String,
    val habitColorHex: String,
    val date: LocalDate,
    val newStatus: ChangeLogStatus,
    val changedAt: Long
)

data class StreakRecord(
    val habitId: String,
    val habitName: String,
    val habitColorHex: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val length: Int,
    val isOngoing: Boolean
)

data class HabitStats(
    val currentStreak: Int,
    val longestStreak: StreakRecord?,
    val allStreaks: List<StreakRecord>,
    val totalDoneDays: Int,
    val done30Days: Int,
    val eligible30Days: Int,
    val consistency30Days: Double?,
    val doneAllTime: Int,
    val eligibleAllTime: Int,
    val consistencyAllTime: Double?
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0
)

data class BackupPayload(
    val schemaVersion: Int = 1,
    val exportedAt: Long,
    val habits: List<Habit>,
    val entries: List<HabitEntry>
)

sealed interface BackupParseResult {
    data class Success(val payload: BackupPayload) : BackupParseResult
    data class Error(val message: String) : BackupParseResult
}

data class BackupImportSummary(
    val habitCount: Int,
    val entryCount: Int
)
