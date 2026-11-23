package com.example

import com.example.data.backup.BackupJsonCodec
import com.example.domain.model.BackupParseResult
import com.example.domain.model.BackupPayload
import com.example.domain.model.EntryStatus
import com.example.domain.model.Habit
import com.example.domain.model.HabitEntry
import com.example.domain.time.DeviceDateTicker
import com.example.domain.time.IndonesianDateFormatter
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class BackupAndTickerTest {

    @Test
    fun backupJsonRoundTripPreservesAllHabitAndEntryFields() {
        val habits = listOf(
            Habit(
                id = "habit-1",
                name = "Sholat Dhuhur",
                colorHex = "#0E7C5B",
                createdDate = LocalDate.of(2026, 11, 1),
                archived = false,
                sortOrder = 0,
                autoFail = true,
                backfillLimitDays = 7,
                activeWeekdays = setOf(1, 2, 3, 4, 5)
            ),
            Habit(
                id = "habit-2",
                name = "Baca Buku",
                colorHex = "#2563EB",
                createdDate = LocalDate.of(2026, 10, 15),
                archived = true,
                sortOrder = 1,
                autoFail = false,
                backfillLimitDays = null,
                activeWeekdays = Habit.ALL_WEEKDAYS
            )
        )
        val entries = listOf(
            HabitEntry("habit-1", LocalDate.of(2026, 11, 20), EntryStatus.DONE, 1000L),
            HabitEntry("habit-1", LocalDate.of(2026, 11, 21), EntryStatus.FAILED, 2000L),
            HabitEntry("habit-2", LocalDate.of(2026, 11, 22), EntryStatus.DONE, 3000L)
        )

        val payload = BackupPayload(
            schemaVersion = 1,
            exportedAt = 9999L,
            habits = habits,
            entries = entries
        )

        val encoded = BackupJsonCodec.encode(payload)
        val decoded = BackupJsonCodec.decode(encoded)

        assertTrue(decoded is BackupParseResult.Success)
        val restored = (decoded as BackupParseResult.Success).payload
        assertEquals(habits, restored.habits)
        assertEquals(entries, restored.entries)
    }

    @Test
    fun backupJsonRejectsInvalidFormatWithIndonesianError() {
        val invalidJson = BackupJsonCodec.decode("bukan json")
        assertTrue(invalidJson is BackupParseResult.Error)
        assertTrue((invalidJson as BackupParseResult.Error).message.contains("Format JSON tidak valid"))

        val missingEntries = BackupJsonCodec.decode("""{"schemaVersion":1,"habits":[]}""")
        assertTrue(missingEntries is BackupParseResult.Error)
        assertTrue((missingEntries as BackupParseResult.Error).message.contains("entries"))
    }

    @Test
    fun deviceDateTickerRollsOverAtExactDeviceMidnightWib() {
        val wib = ZoneId.of("Asia/Jakarta")
        // 2026-11-22 23:59:59 WIB = 2026-11-22T16:59:59Z
        var nowInstant = Instant.parse("2026-11-22T16:59:59Z")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val ticker = DeviceDateTicker(
            clockProvider = { Clock.fixed(nowInstant, wib) },
            scope = scope
        )

        assertEquals(LocalDate.of(2026, 11, 22), ticker.currentDate.value)
        val millisToMidnight = ticker.millisUntilNextMidnight()
        assertTrue(millisToMidnight in 1000L..1200L)

        // Cross midnight -> 2026-11-23 00:00:00 WIB
        nowInstant = Instant.parse("2026-11-22T17:00:00Z")
        ticker.refresh()

        assertEquals(LocalDate.of(2026, 11, 23), ticker.currentDate.value)
        scope.cancel()
    }

    @Test
    fun backupFilenameAndReminderDelayUseDeviceLocalTime() {
        assertEquals(
            "streakku-backup-20261122.json",
            IndonesianDateFormatter.formatBackupFilename(LocalDate.of(2026, 11, 22))
        )

        val wib = ZoneId.of("Asia/Jakarta")
        val now = ZonedDateTime.of(2026, 11, 22, 19, 30, 0, 0, wib)
        // Target 20:00 today -> 30 minutes away
        val delaySameDay = ReminderScheduler.calculateDelayMillis(20, 0, now)
        assertEquals(30 * 60 * 1000L, delaySameDay)

        // Target 19:00 -> already passed, so tomorrow 19:00 -> 23h 30m away
        val delayNextDay = ReminderScheduler.calculateDelayMillis(19, 0, now)
        assertEquals((23 * 60 + 30) * 60 * 1000L, delayNextDay)
    }

    @Test
    fun backupMergeResolvesEntryConflictsByLatestUpdatedAt() {
        val h1 = Habit(
            id = "h-1",
            name = "Baca Buku",
            colorHex = "#2563EB",
            createdDate = LocalDate.of(2026, 11, 1)
        )
        val existingEntries = mapOf(
            ("h-1" to LocalDate.of(2026, 11, 10)) to HabitEntry("h-1", LocalDate.of(2026, 11, 10), EntryStatus.DONE, 5000L),
            ("h-1" to LocalDate.of(2026, 11, 11)) to HabitEntry("h-1", LocalDate.of(2026, 11, 11), EntryStatus.FAILED, 6000L)
        )
        val incoming = BackupPayload(
            schemaVersion = 1,
            exportedAt = 9000L,
            habits = listOf(h1),
            entries = listOf(
                HabitEntry("h-1", LocalDate.of(2026, 11, 10), EntryStatus.FAILED, 4000L), // older -> ignored
                HabitEntry("h-1", LocalDate.of(2026, 11, 11), EntryStatus.DONE, 8000L),   // newer -> wins
                HabitEntry("h-1", LocalDate.of(2026, 11, 12), EntryStatus.DONE, 7000L)    // new -> inserted
            )
        )

        val plan = com.example.domain.logic.BackupMergeEngine.planMerge(
            existingHabitIds = setOf("h-1"),
            existingEntries = existingEntries,
            payload = incoming
        )

        assertEquals(1, plan.habitsToUpsert.size)
        assertEquals(2, plan.entriesToUpsert.size)
        assertEquals(
            mapOf(
                LocalDate.of(2026, 11, 11) to EntryStatus.DONE,
                LocalDate.of(2026, 11, 12) to EntryStatus.DONE
            ),
            plan.entriesToUpsert.associate { it.date to it.status }
        )
    }
}
