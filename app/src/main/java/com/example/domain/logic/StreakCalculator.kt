package com.example.domain.logic

import com.example.domain.model.EditLockReason
import com.example.domain.model.EffectiveStatus
import com.example.domain.model.EntryStatus
import com.example.domain.model.Habit
import com.example.domain.model.HabitEntry
import com.example.domain.model.HabitStats
import com.example.domain.model.StreakRecord
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object StreakCalculator {

    fun isActiveDay(habit: Habit, date: LocalDate): Boolean {
        val active = habit.activeWeekdays.ifEmpty { Habit.ALL_WEEKDAYS }
        return active.contains(date.dayOfWeek.value)
    }

    fun computeStartDate(
        habit: Habit,
        entriesByDate: Map<LocalDate, HabitEntry>
    ): LocalDate {
        val oldestEntry = entriesByDate.keys.minOrNull()
        return if (oldestEntry != null && oldestEntry.isBefore(habit.createdDate)) {
            oldestEntry
        } else {
            habit.createdDate
        }
    }

    fun effectiveStatus(
        habit: Habit,
        entriesByDate: Map<LocalDate, HabitEntry>,
        date: LocalDate,
        today: LocalDate
    ): EffectiveStatus {
        if (!isActiveDay(habit, date)) {
            return EffectiveStatus.INACTIVE
        }
        val entry = entriesByDate[date]
        if (entry != null) {
            return when (entry.status) {
                EntryStatus.DONE -> EffectiveStatus.DONE
                EntryStatus.FAILED -> EffectiveStatus.FAILED
            }
        }
        val startDate = computeStartDate(habit, entriesByDate)
        if (habit.autoFail && date.isBefore(today) && !date.isBefore(startDate)) {
            return EffectiveStatus.FAILED
        }
        return EffectiveStatus.EMPTY
    }

    fun editLockReason(
        habit: Habit,
        date: LocalDate,
        today: LocalDate
    ): EditLockReason {
        if (date.isAfter(today)) {
            return EditLockReason.FUTURE_DATE
        }
        if (!isActiveDay(habit, date)) {
            return EditLockReason.INACTIVE_WEEKDAY
        }
        val limit = habit.backfillLimitDays
        if (limit != null) {
            val daysAgo = ChronoUnit.DAYS.between(date, today)
            if (daysAgo > limit.toLong()) {
                return EditLockReason.BACKFILL_LIMIT
            }
        }
        return EditLockReason.NONE
    }

    fun canEditDate(
        habit: Habit,
        date: LocalDate,
        today: LocalDate
    ): Boolean = editLockReason(habit, date, today) == EditLockReason.NONE

    /**
     * Siklus ketuk di layar Hari Ini:
     * Belum diisi -> Sudah -> Gagal -> Belum diisi (null = hapus Entry).
     */
    fun nextTodayEntryStatus(current: EffectiveStatus): EntryStatus? = when (current) {
        EffectiveStatus.EMPTY -> EntryStatus.DONE
        EffectiveStatus.DONE -> EntryStatus.FAILED
        EffectiveStatus.FAILED -> null
        EffectiveStatus.INACTIVE -> null
    }

    fun computeCurrentStreak(
        habit: Habit,
        entriesByDate: Map<LocalDate, HabitEntry>,
        today: LocalDate
    ): Int {
        val startDate = computeStartDate(habit, entriesByDate)
        if (startDate.isAfter(today)) return 0

        var streak = 0
        var cursor = today

        // Evaluate today first: if EMPTY or INACTIVE, skip today without breaking streak.
        if (isActiveDay(habit, today)) {
            when (effectiveStatus(habit, entriesByDate, today, today)) {
                EffectiveStatus.DONE -> streak = 1
                EffectiveStatus.FAILED -> return 0
                EffectiveStatus.EMPTY, EffectiveStatus.INACTIVE -> {
                    // Skip today; streak can continue from the previous active day.
                }
            }
        }
        cursor = today.minusDays(1)

        while (!cursor.isBefore(startDate)) {
            if (!isActiveDay(habit, cursor)) {
                cursor = cursor.minusDays(1)
                continue
            }
            val status = effectiveStatus(habit, entriesByDate, cursor, today)
            if (status == EffectiveStatus.DONE) {
                streak++
                cursor = cursor.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    fun computeAllStreaks(
        habit: Habit,
        entriesByDate: Map<LocalDate, HabitEntry>,
        today: LocalDate
    ): List<StreakRecord> {
        val startDate = computeStartDate(habit, entriesByDate)
        if (startDate.isAfter(today)) return emptyList()

        val result = mutableListOf<StreakRecord>()
        var runStart: LocalDate? = null
        var runEnd: LocalDate? = null
        var runLength = 0

        var cursor = startDate
        while (!cursor.isAfter(today)) {
            if (!isActiveDay(habit, cursor)) {
                cursor = cursor.plusDays(1)
                continue
            }
            val status = effectiveStatus(habit, entriesByDate, cursor, today)
            when {
                status == EffectiveStatus.DONE -> {
                    if (runStart == null) {
                        runStart = cursor
                    }
                    runEnd = cursor
                    runLength++
                }
                cursor == today && status == EffectiveStatus.EMPTY -> {
                    // Today being unfilled does not break an ongoing streak.
                }
                else -> {
                    if (runStart != null && runEnd != null && runLength > 0) {
                        result.add(
                            StreakRecord(
                                habitId = habit.id,
                                habitName = habit.name,
                                habitColorHex = habit.colorHex,
                                startDate = runStart,
                                endDate = runEnd,
                                length = runLength,
                                isOngoing = false
                            )
                        )
                        runStart = null
                        runEnd = null
                        runLength = 0
                    }
                }
            }
            cursor = cursor.plusDays(1)
        }

        if (runStart != null && runEnd != null && runLength > 0) {
            result.add(
                StreakRecord(
                    habitId = habit.id,
                    habitName = habit.name,
                    habitColorHex = habit.colorHex,
                    startDate = runStart,
                    endDate = runEnd,
                    length = runLength,
                    isOngoing = true
                )
            )
        }

        return result.sortedWith(
            compareByDescending<StreakRecord> { it.length }
                .thenByDescending { it.endDate }
        )
    }

    fun computeStats(
        habit: Habit,
        entries: List<HabitEntry>,
        today: LocalDate
    ): HabitStats {
        val entriesByDate = entries.associateBy { it.date }
        val startDate = computeStartDate(habit, entriesByDate)
        val currentStreak = computeCurrentStreak(habit, entriesByDate, today)
        val allStreaks = computeAllStreaks(habit, entriesByDate, today)
        val longestStreak = allStreaks.firstOrNull()

        if (startDate.isAfter(today)) {
            return HabitStats(
                currentStreak = 0,
                longestStreak = null,
                allStreaks = emptyList(),
                totalDoneDays = 0,
                done30Days = 0,
                eligible30Days = 0,
                consistency30Days = null,
                doneAllTime = 0,
                eligibleAllTime = 0,
                consistencyAllTime = null
            )
        }

        val thirtyDaysStart = maxOf(startDate, today.minusDays(29))
        var doneAllTime = 0
        var eligibleAllTime = 0
        var done30Days = 0
        var eligible30Days = 0

        var cursor = startDate
        while (!cursor.isAfter(today)) {
            if (isActiveDay(habit, cursor)) {
                val status = effectiveStatus(habit, entriesByDate, cursor, today)
                val skipTodayUnfilled = cursor == today && status == EffectiveStatus.EMPTY
                if (!skipTodayUnfilled) {
                    eligibleAllTime++
                    val isDone = status == EffectiveStatus.DONE
                    if (isDone) doneAllTime++

                    if (!cursor.isBefore(thirtyDaysStart)) {
                        eligible30Days++
                        if (isDone) done30Days++
                    }
                }
            }
            cursor = cursor.plusDays(1)
        }

        val consistencyAllTime = if (eligibleAllTime > 0) {
            (doneAllTime.toDouble() / eligibleAllTime.toDouble()) * 100.0
        } else {
            null
        }
        val consistency30Days = if (eligible30Days > 0) {
            (done30Days.toDouble() / eligible30Days.toDouble()) * 100.0
        } else {
            null
        }

        return HabitStats(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            allStreaks = allStreaks,
            totalDoneDays = doneAllTime,
            done30Days = done30Days,
            eligible30Days = eligible30Days,
            consistency30Days = consistency30Days,
            doneAllTime = doneAllTime,
            eligibleAllTime = eligibleAllTime,
            consistencyAllTime = consistencyAllTime
        )
    }
}
