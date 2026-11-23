package com.example

import com.example.domain.logic.StreakCalculator
import com.example.domain.model.EditLockReason
import com.example.domain.model.EffectiveStatus
import com.example.domain.model.EntryStatus
import com.example.domain.model.Habit
import com.example.domain.model.HabitEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private fun habit(
        id: String = "h1",
        createdDate: LocalDate = LocalDate.of(2026, 11, 1),
        autoFail: Boolean = false,
        backfillLimitDays: Int? = null,
        activeWeekdays: Set<Int> = Habit.ALL_WEEKDAYS
    ) = Habit(
        id = id,
        name = "Olahraga",
        colorHex = "#0E7C5B",
        createdDate = createdDate,
        autoFail = autoFail,
        backfillLimitDays = backfillLimitDays,
        activeWeekdays = activeWeekdays
    )

    private fun entry(
        date: LocalDate,
        status: EntryStatus,
        habitId: String = "h1",
        updatedAt: Long = 1000L
    ) = HabitEntry(habitId = habitId, date = date, status = status, updatedAt = updatedAt)

    /**
     * Kriteria penerimaan 1:
     * DONE tgl 1-5, FAILED tgl 6, DONE tgl 7-8 (hari ini tgl 8)
     * -> streak saat ini 2, streak terpanjang 5.
     */
    @Test
    fun ac1_done1to5_failed6_done7to8_currentIs2_longestIs5() {
        val h = habit(createdDate = LocalDate.of(2026, 11, 1))
        val today = LocalDate.of(2026, 11, 8)
        val entries = listOf(
            entry(LocalDate.of(2026, 11, 1), EntryStatus.DONE),
            entry(LocalDate.of(2026, 11, 2), EntryStatus.DONE),
            entry(LocalDate.of(2026, 11, 3), EntryStatus.DONE),
            entry(LocalDate.of(2026, 11, 4), EntryStatus.DONE),
            entry(LocalDate.of(2026, 11, 5), EntryStatus.DONE),
            entry(LocalDate.of(2026, 11, 6), EntryStatus.FAILED),
            entry(LocalDate.of(2026, 11, 7), EntryStatus.DONE),
            entry(LocalDate.of(2026, 11, 8), EntryStatus.DONE)
        )

        val stats = StreakCalculator.computeStats(h, entries, today)

        assertEquals(2, stats.currentStreak)
        val longest = stats.longestStreak
        assertNotNull(longest)
        assertEquals(5, longest!!.length)
        assertEquals(LocalDate.of(2026, 11, 1), longest.startDate)
        assertEquals(LocalDate.of(2026, 11, 5), longest.endDate)
        assertFalse(longest.isOngoing)

        val ongoing = stats.allStreaks.first { it.isOngoing }
        assertEquals(2, ongoing.length)
        assertEquals(LocalDate.of(2026, 11, 7), ongoing.startDate)
        assertEquals(LocalDate.of(2026, 11, 8), ongoing.endDate)
        assertEquals(7, stats.totalDoneDays)
    }

    /**
     * Kriteria penerimaan 2:
     * Hari ini BELUM DIISI, kemarin dan sebelumnya DONE
     * -> streak saat ini tidak putus (menghitung dari kemarin),
     * dan hari ini yang belum diisi tidak masuk penyebut persentase.
     */
    @Test
    fun ac2_todayUnfilled_yesterdayDone_streakDoesNotBreak() {
        val h = habit(createdDate = LocalDate.of(2026, 11, 1))
        val today = LocalDate.of(2026, 11, 4)
        val entries = listOf(
            entry(LocalDate.of(2026, 11, 1), EntryStatus.DONE),
            entry(LocalDate.of(2026, 11, 2), EntryStatus.DONE),
            entry(LocalDate.of(2026, 11, 3), EntryStatus.DONE)
            // 2026-11-04 (today) is intentionally absent = EMPTY
        )

        val stats = StreakCalculator.computeStats(h, entries, today)

        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak?.length)
        assertTrue(stats.longestStreak?.isOngoing == true)
        assertEquals(3, stats.eligibleAllTime)
        assertEquals(100.0, stats.consistencyAllTime!!, 0.001)
    }

    /**
     * Kriteria penerimaan 3:
     * Tanggal lampau belum diisi dan autoFail mati -> abu-abu (EMPTY);
     * setelah autoFail dinyalakan -> merah (FAILED);
     * dimatikan lagi -> kembali abu-abu (EMPTY).
     */
    @Test
    fun ac3_autoFailToggleChangesPastUnfilledWithoutMutatingEntries() {
        val created = LocalDate.of(2026, 11, 1)
        val pastDate = LocalDate.of(2026, 11, 3)
        val today = LocalDate.of(2026, 11, 5)
        val entriesByDate = emptyMap<LocalDate, HabitEntry>()

        val hOff = habit(createdDate = created, autoFail = false)
        assertEquals(
            EffectiveStatus.EMPTY,
            StreakCalculator.effectiveStatus(hOff, entriesByDate, pastDate, today)
        )

        val hOn = hOff.copy(autoFail = true)
        assertEquals(
            EffectiveStatus.FAILED,
            StreakCalculator.effectiveStatus(hOn, entriesByDate, pastDate, today)
        )
        // Today itself must remain EMPTY even with autoFail enabled
        assertEquals(
            EffectiveStatus.EMPTY,
            StreakCalculator.effectiveStatus(hOn, entriesByDate, today, today)
        )

        val hOffAgain = hOn.copy(autoFail = false)
        assertEquals(
            EffectiveStatus.EMPTY,
            StreakCalculator.effectiveStatus(hOffAgain, entriesByDate, pastDate, today)
        )
    }

    /**
     * Kriteria penerimaan 4:
     * activeWeekdays = Senin-Jumat (1..5), DONE Jumat dan Senin berikutnya
     * -> streak tidak putus oleh Sabtu/Minggu.
     */
    @Test
    fun ac4_activeWeekdaysMonToFri_weekendDoesNotBreakStreak() {
        // 2026-11-20 = Friday (5), 21 = Saturday (6), 22 = Sunday (7), 23 = Monday (1)
        val friday = LocalDate.of(2026, 11, 20)
        val saturday = LocalDate.of(2026, 11, 21)
        val sunday = LocalDate.of(2026, 11, 22)
        val monday = LocalDate.of(2026, 11, 23)

        val h = habit(
            createdDate = friday,
            activeWeekdays = setOf(1, 2, 3, 4, 5)
        )
        val entries = listOf(
            entry(friday, EntryStatus.DONE),
            entry(monday, EntryStatus.DONE)
        )
        val map = entries.associateBy { it.date }

        assertEquals(EffectiveStatus.INACTIVE, StreakCalculator.effectiveStatus(h, map, saturday, monday))
        assertEquals(EffectiveStatus.INACTIVE, StreakCalculator.effectiveStatus(h, map, sunday, monday))

        val stats = StreakCalculator.computeStats(h, entries, monday)
        assertEquals(2, stats.currentStreak)
        assertEquals(2, stats.longestStreak?.length)
        assertEquals(friday, stats.longestStreak?.startDate)
        assertEquals(monday, stats.longestStreak?.endDate)
        assertEquals(2, stats.eligibleAllTime)
        assertEquals(100.0, stats.consistencyAllTime!!, 0.001)
    }

    /**
     * Kriteria penerimaan 5:
     * Mengisi tanggal 2 minggu lalu berhasil secara default;
     * dengan backfillLimitDays = 7 -> terkunci.
     * Tanggal masa depan selalu terkunci.
     */
    @Test
    fun ac5_backfillLimitAndFutureLock() {
        val today = LocalDate.of(2026, 11, 22)
        val twoWeeksAgo = today.minusDays(14)
        val sevenDaysAgo = today.minusDays(7)
        val eightDaysAgo = today.minusDays(8)
        val tomorrow = today.plusDays(1)

        val defaultHabit = habit(createdDate = today, backfillLimitDays = null)
        assertTrue(StreakCalculator.canEditDate(defaultHabit, twoWeeksAgo, today))
        assertFalse(StreakCalculator.canEditDate(defaultHabit, tomorrow, today))
        assertEquals(EditLockReason.FUTURE_DATE, StreakCalculator.editLockReason(defaultHabit, tomorrow, today))

        val limitedHabit = defaultHabit.copy(backfillLimitDays = 7)
        assertFalse(StreakCalculator.canEditDate(limitedHabit, twoWeeksAgo, today))
        assertEquals(EditLockReason.BACKFILL_LIMIT, StreakCalculator.editLockReason(limitedHabit, twoWeeksAgo, today))
        assertFalse(StreakCalculator.canEditDate(limitedHabit, eightDaysAgo, today))
        assertTrue(StreakCalculator.canEditDate(limitedHabit, sevenDaysAgo, today))
        assertTrue(StreakCalculator.canEditDate(limitedHabit, today, today))
    }

    @Test
    fun todayTapCycleMatchesPrdSpecification() {
        assertEquals(EntryStatus.DONE, StreakCalculator.nextTodayEntryStatus(EffectiveStatus.EMPTY))
        assertEquals(EntryStatus.FAILED, StreakCalculator.nextTodayEntryStatus(EffectiveStatus.DONE))
        assertNull(StreakCalculator.nextTodayEntryStatus(EffectiveStatus.FAILED))
    }
}
