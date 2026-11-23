package com.example.domain.repository

import com.example.domain.model.AppSettings
import com.example.domain.model.BackupImportSummary
import com.example.domain.model.BackupPayload
import com.example.domain.model.ChangeLogRecord
import com.example.domain.model.EntryStatus
import com.example.domain.model.Habit
import com.example.domain.model.HabitEntry
import com.example.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Abstract data repository interfaces separated from Room/DataStore implementations
 * so future sync or external backup providers can be plugged in cleanly.
 */
interface HabitRepository {
    fun observeAllHabits(): Flow<List<Habit>>
    fun observeAllEntries(): Flow<List<HabitEntry>>
    fun observeRecentChangeLogs(limit: Int = 50): Flow<List<ChangeLogRecord>>

    suspend fun getHabitById(habitId: String): Habit?
    suspend fun getAllHabitsSnapshot(): List<Habit>
    suspend fun getAllEntriesSnapshot(): List<HabitEntry>

    suspend fun saveHabit(habit: Habit)
    suspend fun setHabitArchived(habitId: String, archived: Boolean)
    suspend fun deleteHabit(habitId: String)

    /**
     * Sets entry status for (habitId, date).
     * Passing null deletes the entry row (representing "Belum diisi").
     */
    suspend fun setEntryStatus(
        habitId: String,
        date: LocalDate,
        status: EntryStatus?,
        timestampMillis: Long = System.currentTimeMillis()
    )

    suspend fun mergeBackup(payload: BackupPayload): BackupImportSummary
}

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun getSettingsSnapshot(): AppSettings
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setReminderEnabled(enabled: Boolean)
    suspend fun setReminderTime(hour: Int, minute: Int)
}
