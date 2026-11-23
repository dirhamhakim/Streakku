package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupJsonCodec
import com.example.domain.logic.StreakCalculator
import com.example.domain.model.AppSettings
import com.example.domain.model.BackupParseResult
import com.example.domain.model.BackupPayload
import com.example.domain.model.ChangeLogRecord
import com.example.domain.model.EffectiveStatus
import com.example.domain.model.EntryStatus
import com.example.domain.model.Habit
import com.example.domain.model.HabitEntry
import com.example.domain.model.HabitStats
import com.example.domain.model.StreakRecord
import com.example.domain.model.ThemeMode
import com.example.domain.repository.HabitRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.time.DeviceDateTicker
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class TodayHabitCardUi(
    val habit: Habit,
    val todayStatus: EffectiveStatus,
    val currentStreak: Int
)

data class TodayUiState(
    val today: LocalDate = LocalDate.now(),
    val scheduledHabits: List<TodayHabitCardUi> = emptyList(),
    val offDayHabits: List<TodayHabitCardUi> = emptyList(),
    val archivedHabits: List<Habit> = emptyList(),
    val completedTodayCount: Int = 0,
    val totalScheduledTodayCount: Int = 0,
    val isLoaded: Boolean = false
)

data class CalendarUiState(
    val today: LocalDate = LocalDate.now(),
    val habits: List<Habit> = emptyList(),
    val selectedHabit: Habit? = null,
    val displayedMonth: YearMonth = YearMonth.now(),
    val entriesByDate: Map<LocalDate, HabitEntry> = emptyMap(),
    val stats: HabitStats? = null
)

data class HistoryUiState(
    val today: LocalDate = LocalDate.now(),
    val habits: List<Habit> = emptyList(),
    val selectedHabitFilterId: String? = null,
    val streaks: List<StreakRecord> = emptyList(),
    val recentLogs: List<ChangeLogRecord> = emptyList()
)

data class BackupDialogState(
    val pendingImportPayload: BackupPayload? = null,
    val errorMessage: String? = null,
    val feedbackMessage: String? = null
)

class StreakkuViewModel(
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val dateTicker: DeviceDateTicker,
    private val appContext: Context
) : ViewModel() {

    val todayDate: StateFlow<LocalDate> = dateTicker.currentDate

    val settingsState: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    private val _selectedCalendarHabitId = MutableStateFlow<String?>(null)
    private val _displayedMonth = MutableStateFlow(YearMonth.from(dateTicker.currentDate.value))
    private val _selectedHistoryHabitId = MutableStateFlow<String?>(null)

    private val _backupDialogState = MutableStateFlow(BackupDialogState())
    val backupDialogState: StateFlow<BackupDialogState> = _backupDialogState.asStateFlow()

    val allHabits: StateFlow<List<Habit>> = habitRepository.observeAllHabits().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val allEntriesFlow = habitRepository.observeAllEntries()
    private val recentLogsFlow = habitRepository.observeRecentChangeLogs(limit = 60)

    val todayUiState: StateFlow<TodayUiState> = combine(
        habitRepository.observeAllHabits(),
        allEntriesFlow,
        dateTicker.currentDate
    ) { habits, entries, today ->
        val entriesByHabit = entries.groupBy { it.habitId }
        val activeHabits = habits.filter { !it.archived }
        val archivedHabits = habits.filter { it.archived }

        val scheduled = mutableListOf<TodayHabitCardUi>()
        val offDay = mutableListOf<TodayHabitCardUi>()

        for (habit in activeHabits) {
            val map = entriesByHabit[habit.id].orEmpty().associateBy { it.date }
            val status = StreakCalculator.effectiveStatus(habit, map, today, today)
            val streak = StreakCalculator.computeCurrentStreak(habit, map, today)
            val card = TodayHabitCardUi(
                habit = habit,
                todayStatus = status,
                currentStreak = streak
            )
            if (status == EffectiveStatus.INACTIVE) {
                offDay.add(card)
            } else {
                scheduled.add(card)
            }
        }

        val completedCount = scheduled.count { it.todayStatus == EffectiveStatus.DONE }
        TodayUiState(
            today = today,
            scheduledHabits = scheduled,
            offDayHabits = offDay,
            archivedHabits = archivedHabits,
            completedTodayCount = completedCount,
            totalScheduledTodayCount = scheduled.size,
            isLoaded = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(today = dateTicker.currentDate.value)
    )

    val calendarUiState: StateFlow<CalendarUiState> = combine(
        habitRepository.observeAllHabits(),
        allEntriesFlow,
        dateTicker.currentDate,
        _selectedCalendarHabitId,
        _displayedMonth
    ) { habits, entries, today, selectedId, month ->
        val activeHabits = habits.filter { !it.archived }
        val selectable = activeHabits.ifEmpty { habits }
        val chosen = selectable.firstOrNull { it.id == selectedId } ?: selectable.firstOrNull()
        val chosenEntries = if (chosen != null) {
            entries.filter { it.habitId == chosen.id }
        } else {
            emptyList()
        }
        val entriesByDate = chosenEntries.associateBy { it.date }
        val stats = chosen?.let { StreakCalculator.computeStats(it, chosenEntries, today) }

        CalendarUiState(
            today = today,
            habits = selectable,
            selectedHabit = chosen,
            displayedMonth = month,
            entriesByDate = entriesByDate,
            stats = stats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(
            today = dateTicker.currentDate.value,
            displayedMonth = YearMonth.from(dateTicker.currentDate.value)
        )
    )

    val historyUiState: StateFlow<HistoryUiState> = combine(
        habitRepository.observeAllHabits(),
        allEntriesFlow,
        recentLogsFlow,
        dateTicker.currentDate,
        _selectedHistoryHabitId
    ) { habits, entries, logs, today, filterHabitId ->
        val entriesByHabit = entries.groupBy { it.habitId }
        val targetHabits = if (filterHabitId != null) {
            habits.filter { it.id == filterHabitId }
        } else {
            habits
        }

        val streaks = targetHabits
            .flatMap { habit ->
                val map = entriesByHabit[habit.id].orEmpty().associateBy { it.date }
                StreakCalculator.computeAllStreaks(habit, map, today)
            }
            .sortedWith(
                compareByDescending<StreakRecord> { it.length }
                    .thenByDescending { it.isOngoing }
                    .thenByDescending { it.endDate }
            )

        val filteredLogs = if (filterHabitId != null) {
            logs.filter { it.habitId == filterHabitId }
        } else {
            logs
        }

        HistoryUiState(
            today = today,
            habits = habits,
            selectedHabitFilterId = filterHabitId,
            streaks = streaks,
            recentLogs = filteredLogs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(today = dateTicker.currentDate.value)
    )

    fun refreshTodayDate() {
        dateTicker.refresh()
    }

    fun cycleTodayStatus(card: TodayHabitCardUi) {
        val today = dateTicker.currentDate.value
        if (!StreakCalculator.canEditDate(card.habit, today, today)) return
        val nextStatus = StreakCalculator.nextTodayEntryStatus(card.todayStatus)
        viewModelScope.launch {
            habitRepository.setEntryStatus(
                habitId = card.habit.id,
                date = today,
                status = nextStatus
            )
        }
    }

    fun setEntryStatus(habit: Habit, date: LocalDate, status: EntryStatus?) {
        val today = dateTicker.currentDate.value
        if (!StreakCalculator.canEditDate(habit, date, today)) return
        viewModelScope.launch {
            habitRepository.setEntryStatus(
                habitId = habit.id,
                date = date,
                status = status
            )
        }
    }

    suspend fun loadHabitById(habitId: String): Habit? = habitRepository.getHabitById(habitId)

    fun saveHabit(
        existingHabit: Habit?,
        name: String,
        colorHex: String,
        autoFail: Boolean,
        backfillLimitDays: Int?,
        activeWeekdays: Set<Int>,
        onSaved: () -> Unit
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        val sanitizedDays = activeWeekdays.filter { it in 1..7 }.toSet().ifEmpty { Habit.ALL_WEEKDAYS }

        val habitToSave = if (existingHabit != null) {
            existingHabit.copy(
                name = trimmedName,
                colorHex = colorHex,
                autoFail = autoFail,
                backfillLimitDays = backfillLimitDays,
                activeWeekdays = sanitizedDays
            )
        } else {
            Habit(
                id = UUID.randomUUID().toString(),
                name = trimmedName,
                colorHex = colorHex,
                createdDate = dateTicker.currentDate.value,
                archived = false,
                sortOrder = 0,
                autoFail = autoFail,
                backfillLimitDays = backfillLimitDays,
                activeWeekdays = sanitizedDays
            )
        }

        viewModelScope.launch {
            habitRepository.saveHabit(habitToSave)
            onSaved()
        }
    }

    fun setHabitArchived(habitId: String, archived: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            habitRepository.setHabitArchived(habitId, archived)
            onDone()
        }
    }

    fun deleteHabit(habitId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habitId)
            onDone()
        }
    }

    fun selectCalendarHabit(habitId: String) {
        _selectedCalendarHabitId.value = habitId
    }

    fun previousCalendarMonth() {
        _displayedMonth.update { it.minusMonths(1) }
    }

    fun nextCalendarMonth() {
        _displayedMonth.update { it.plusMonths(1) }
    }

    fun jumpCalendarToToday() {
        _displayedMonth.value = YearMonth.from(dateTicker.currentDate.value)
    }

    fun selectHistoryFilter(habitId: String?) {
        _selectedHistoryHabitId.value = habitId
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(enabled)
            val current = settingsRepository.getSettingsSnapshot()
            if (enabled) {
                ReminderScheduler.scheduleDailyReminder(
                    context = appContext,
                    hour = current.reminderHour,
                    minute = current.reminderMinute
                )
            } else {
                ReminderScheduler.cancelDailyReminder(appContext)
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(hour, minute)
            val current = settingsRepository.getSettingsSnapshot()
            if (current.reminderEnabled) {
                ReminderScheduler.scheduleDailyReminder(
                    context = appContext,
                    hour = hour,
                    minute = minute
                )
            }
        }
    }

    suspend fun createBackupJson(): Pair<String, BackupPayload> {
        val habits = habitRepository.getAllHabitsSnapshot()
        val entries = habitRepository.getAllEntriesSnapshot()
        val payload = BackupPayload(
            schemaVersion = 1,
            exportedAt = System.currentTimeMillis(),
            habits = habits,
            entries = entries
        )
        return BackupJsonCodec.encode(payload) to payload
    }

    fun notifyExportSuccess(habitCount: Int, entryCount: Int) {
        _backupDialogState.update {
            it.copy(
                feedbackMessage = "Cadangan berhasil diekspor ($habitCount kegiatan, $entryCount catatan)."
            )
        }
    }

    fun notifyExportError(reason: String) {
        _backupDialogState.update {
            it.copy(errorMessage = "Gagal mengekspor cadangan: $reason")
        }
    }

    fun prepareImportFromJson(rawJson: String) {
        when (val parsed = BackupJsonCodec.decode(rawJson)) {
            is BackupParseResult.Success -> {
                _backupDialogState.update {
                    it.copy(pendingImportPayload = parsed.payload, errorMessage = null)
                }
            }
            is BackupParseResult.Error -> {
                _backupDialogState.update {
                    it.copy(pendingImportPayload = null, errorMessage = parsed.message)
                }
            }
        }
    }

    fun notifyImportReadError(reason: String) {
        _backupDialogState.update {
            it.copy(errorMessage = "Gagal membaca file cadangan: $reason")
        }
    }

    fun confirmPendingImport() {
        val payload = _backupDialogState.value.pendingImportPayload ?: return
        _backupDialogState.update { it.copy(pendingImportPayload = null) }
        viewModelScope.launch {
            try {
                val summary = habitRepository.mergeBackup(payload)
                _backupDialogState.update {
                    it.copy(
                        feedbackMessage = "Cadangan berhasil diimpor (${summary.habitCount} kegiatan, ${summary.entryCount} catatan diperbarui)."
                    )
                }
            } catch (e: Exception) {
                _backupDialogState.update {
                    it.copy(errorMessage = "Gagal mengimpor data ke database: ${e.message ?: "kesalahan tidak diketahui"}")
                }
            }
        }
    }

    fun dismissPendingImport() {
        _backupDialogState.update { it.copy(pendingImportPayload = null) }
    }

    fun dismissBackupError() {
        _backupDialogState.update { it.copy(errorMessage = null) }
    }

    fun consumeFeedbackMessage() {
        _backupDialogState.update { it.copy(feedbackMessage = null) }
    }

    companion object {
        fun provideFactory(
            habitRepository: HabitRepository,
            settingsRepository: SettingsRepository,
            dateTicker: DeviceDateTicker,
            appContext: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StreakkuViewModel(
                    habitRepository = habitRepository,
                    settingsRepository = settingsRepository,
                    dateTicker = dateTicker,
                    appContext = appContext
                ) as T
            }
        }
    }
}
