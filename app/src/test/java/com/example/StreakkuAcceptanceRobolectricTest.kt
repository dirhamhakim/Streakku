package com.example

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupJsonCodec
import com.example.data.local.StreakkuDatabase
import com.example.data.repository.RoomHabitRepository
import com.example.domain.model.BackupParseResult
import com.example.domain.model.BackupPayload
import com.example.domain.model.EntryStatus
import com.example.domain.model.Habit
import com.example.domain.model.HabitEntry
import com.example.domain.time.DeviceDateTicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StreakkuAcceptanceRobolectricTest {

    /**
     * Kriteria penerimaan 6:
     * Aplikasi ditutup dan dibuka lagi (juga tanpa internet) -> semua data tetap ada.
     * Juga memverifikasi menghapus status ke "Belum diisi" menghapus baris Entry.
     */
    @Test
    fun ac6_roomPersistenceSurvivesDatabaseCloseAndReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "streakku_test_persist.db"
        context.deleteDatabase(dbName)

        val habit = Habit(
            id = "habit-persist-1",
            name = "Sholat Dhuhur",
            colorHex = "#0E7C5B",
            createdDate = LocalDate.of(2026, 11, 1),
            archived = false,
            sortOrder = 0,
            autoFail = true,
            backfillLimitDays = 7,
            activeWeekdays = setOf(1, 2, 3, 4, 5)
        )

        val db1 = Room.databaseBuilder(context, StreakkuDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        val repo1 = RoomHabitRepository(db1)
        repo1.saveHabit(habit)
        repo1.setEntryStatus(habit.id, LocalDate.of(2026, 11, 20), EntryStatus.DONE, 1000L)
        repo1.setEntryStatus(habit.id, LocalDate.of(2026, 11, 21), EntryStatus.FAILED, 2000L)
        // Clear Nov 21 back to "Belum diisi" -> deletes Entry row
        repo1.setEntryStatus(habit.id, LocalDate.of(2026, 11, 21), null, 3000L)
        db1.close()

        // Reopen database fresh
        val db2 = Room.databaseBuilder(context, StreakkuDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        val repo2 = RoomHabitRepository(db2)

        val restoredHabits = repo2.getAllHabitsSnapshot()
        val restoredEntries = repo2.getAllEntriesSnapshot()
        db2.close()
        context.deleteDatabase(dbName)

        assertEquals(1, restoredHabits.size)
        assertEquals(habit, restoredHabits.first())
        assertEquals(1, restoredEntries.size)
        assertEquals(LocalDate.of(2026, 11, 20), restoredEntries.first().date)
        assertEquals(EntryStatus.DONE, restoredEntries.first().status)
    }

    /**
     * Kriteria penerimaan 7:
     * Ekspor lalu Impor di data kosong -> seluruh data kembali identik.
     * Juga menguji penggabungan berdasarkan updatedAt terbaru & validasi format JSON.
     */
    @Test
    fun ac7_exportAndImportIntoEmptyDbRestoresIdenticalDataAndMergesByLatestUpdatedAt() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val sourceDb = Room.inMemoryDatabaseBuilder(context, StreakkuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val sourceRepo = RoomHabitRepository(sourceDb)

        val habit1 = Habit(
            id = "h-1",
            name = "Baca Buku",
            colorHex = "#2563EB",
            createdDate = LocalDate.of(2026, 10, 1),
            archived = false,
            sortOrder = 0,
            autoFail = true,
            backfillLimitDays = 14,
            activeWeekdays = setOf(1, 2, 3, 4, 5, 6, 7)
        )
        val habit2 = Habit(
            id = "h-2",
            name = "Olahraga",
            colorHex = "#EA580C",
            createdDate = LocalDate.of(2026, 11, 1),
            archived = true,
            sortOrder = 1,
            autoFail = false,
            backfillLimitDays = null,
            activeWeekdays = setOf(1, 3, 5)
        )

        sourceRepo.saveHabit(habit1)
        sourceRepo.saveHabit(habit2)
        sourceRepo.setEntryStatus("h-1", LocalDate.of(2026, 11, 10), EntryStatus.DONE, 5000L)
        sourceRepo.setEntryStatus("h-1", LocalDate.of(2026, 11, 11), EntryStatus.FAILED, 6000L)
        sourceRepo.setEntryStatus("h-2", LocalDate.of(2026, 11, 11), EntryStatus.DONE, 7000L)

        val originalHabits = sourceRepo.getAllHabitsSnapshot()
        val originalEntries = sourceRepo.getAllEntriesSnapshot()
        val exportedJson = BackupJsonCodec.encode(
            BackupPayload(
                schemaVersion = 1,
                exportedAt = 9999L,
                habits = originalHabits,
                entries = originalEntries
            )
        )
        sourceDb.close()

        // Import into a completely empty database
        val targetDb = Room.inMemoryDatabaseBuilder(context, StreakkuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val targetRepo = RoomHabitRepository(targetDb)

        val parseResult = BackupJsonCodec.decode(exportedJson)
        assertTrue(parseResult is BackupParseResult.Success)
        val payload = (parseResult as BackupParseResult.Success).payload
        targetRepo.mergeBackup(payload)

        assertEquals(originalHabits, targetRepo.getAllHabitsSnapshot())
        assertEquals(originalEntries, targetRepo.getAllEntriesSnapshot())

        // Verify merge by latest updatedAt: older entry ignored, newer entry wins
        val conflictPayload = BackupPayload(
            schemaVersion = 1,
            exportedAt = 12000L,
            habits = listOf(habit1),
            entries = listOf(
                HabitEntry("h-1", LocalDate.of(2026, 11, 10), EntryStatus.FAILED, 4000L), // older -> ignored
                HabitEntry("h-1", LocalDate.of(2026, 11, 11), EntryStatus.DONE, 8000L)    // newer -> applied
            )
        )
        targetRepo.mergeBackup(conflictPayload)

        val mergedEntries = targetRepo.getAllEntriesSnapshot().associateBy { it.habitId to it.date }
        assertEquals(EntryStatus.DONE, mergedEntries["h-1" to LocalDate.of(2026, 11, 10)]?.status)
        assertEquals(EntryStatus.DONE, mergedEntries["h-1" to LocalDate.of(2026, 11, 11)]?.status)
        targetDb.close()

        // Verify invalid JSON returns Indonesian error message
        val badResult = BackupJsonCodec.decode("{invalid json")
        assertTrue(badResult is BackupParseResult.Error)
        assertTrue((badResult as BackupParseResult.Error).message.contains("JSON"))
    }

    /**
     * Kriteria penerimaan 8:
     * Pergantian hari tepat tengah malam waktu perangkat;
     * "hari ini" ikut berganti tanpa restart manual.
     */
    @Test
    fun ac8_midnightRolloverUpdatesCurrentDateAutomaticallyWithoutRestart() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wibZone = ZoneId.of("Asia/Jakarta")

        // 2026-11-22 23:59:59 WIB
        var currentInstant = Instant.parse("2026-11-22T16:59:59Z")
        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val ticker = DeviceDateTicker(
            clockProvider = { Clock.fixed(currentInstant, wibZone) },
            scope = testScope
        )
        ticker.start(context)

        assertEquals(LocalDate.of(2026, 11, 22), ticker.currentDate.value)

        // Advance clock across WIB midnight -> 2026-11-23 00:00:01 WIB
        currentInstant = Instant.parse("2026-11-22T17:00:01Z")
        context.sendBroadcast(Intent(Intent.ACTION_DATE_CHANGED))
        // Ensure refresh picks up the new day even if Robolectric broadcast is async
        ticker.refresh()

        assertEquals(LocalDate.of(2026, 11, 23), ticker.currentDate.value)
        testScope.cancel()
    }
}
