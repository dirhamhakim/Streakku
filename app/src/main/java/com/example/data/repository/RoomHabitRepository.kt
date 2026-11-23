package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.ChangeLogEntity
import com.example.data.local.EntryEntity
import com.example.data.local.StreakkuDatabase
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.domain.model.BackupImportSummary
import com.example.domain.model.BackupPayload
import com.example.domain.model.ChangeLogRecord
import com.example.domain.model.ChangeLogStatus
import com.example.domain.model.EntryStatus
import com.example.domain.model.Habit
import com.example.domain.model.HabitEntry
import com.example.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class RoomHabitRepository(
    private val database: StreakkuDatabase
) : HabitRepository {

    private val habitDao = database.habitDao()
    private val entryDao = database.entryDao()
    private val changeLogDao = database.changeLogDao()

    override fun observeAllHabits(): Flow<List<Habit>> =
        habitDao.observeAllHabits().map { list -> list.map { it.toDomain() } }

    override fun observeAllEntries(): Flow<List<HabitEntry>> =
        entryDao.observeAllEntries().map { list -> list.map { it.toDomain() } }

    override fun observeRecentChangeLogs(limit: Int): Flow<List<ChangeLogRecord>> =
        changeLogDao.observeRecentLogs(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun getHabitById(habitId: String): Habit? =
        habitDao.getHabitById(habitId)?.toDomain()

    override suspend fun getAllHabitsSnapshot(): List<Habit> =
        habitDao.getAllHabitsSnapshot().map { it.toDomain() }

    override suspend fun getAllEntriesSnapshot(): List<HabitEntry> =
        entryDao.getAllEntriesSnapshot().map { it.toDomain() }

    override suspend fun saveHabit(habit: Habit) {
        val existing = habitDao.getHabitById(habit.id)
        val sortOrder = existing?.sortOrder ?: (habitDao.getMaxSortOrder() + 1)
        habitDao.upsertHabit(habit.copy(sortOrder = sortOrder).toEntity())
    }

    override suspend fun setHabitArchived(habitId: String, archived: Boolean) {
        habitDao.setArchived(habitId, archived)
    }

    override suspend fun deleteHabit(habitId: String) {
        habitDao.deleteHabitById(habitId)
    }

    override suspend fun setEntryStatus(
        habitId: String,
        date: LocalDate,
        status: EntryStatus?,
        timestampMillis: Long
    ) {
        val dateText = date.toString()
        database.withTransaction {
            if (status == null) {
                entryDao.deleteEntry(habitId, dateText)
                changeLogDao.insertLog(
                    ChangeLogEntity(
                        habitId = habitId,
                        date = dateText,
                        newStatus = ChangeLogStatus.EMPTY.name,
                        changedAt = timestampMillis
                    )
                )
            } else {
                entryDao.upsertEntry(
                    EntryEntity(
                        habitId = habitId,
                        date = dateText,
                        status = status.name,
                        updatedAt = timestampMillis
                    )
                )
                changeLogDao.insertLog(
                    ChangeLogEntity(
                        habitId = habitId,
                        date = dateText,
                        newStatus = status.name,
                        changedAt = timestampMillis
                    )
                )
            }
        }
    }

    override suspend fun mergeBackup(payload: BackupPayload): BackupImportSummary {
        var appliedHabits = 0
        var appliedEntries = 0

        database.withTransaction {
            val existingHabitIds = habitDao.getAllHabitsSnapshot().map { it.id }.toSet()
            val existingEntries = entryDao.getAllEntriesSnapshot()
                .map { it.toDomain() }
                .associateBy { it.habitId to it.date }

            val plan = com.example.domain.logic.BackupMergeEngine.planMerge(
                existingHabitIds = existingHabitIds,
                existingEntries = existingEntries,
                payload = payload
            )

            for (habit in plan.habitsToUpsert) {
                habitDao.upsertHabit(habit.toEntity())
                appliedHabits++
            }

            for (entry in plan.entriesToUpsert) {
                val dateText = entry.date.toString()
                entryDao.upsertEntry(entry.toEntity())
                appliedEntries++
                val logExists = changeLogDao.countMatchingLog(
                    habitId = entry.habitId,
                    date = dateText,
                    changedAt = entry.updatedAt
                ) > 0
                if (!logExists) {
                    changeLogDao.insertLog(
                        ChangeLogEntity(
                            habitId = entry.habitId,
                            date = dateText,
                            newStatus = entry.status.name,
                            changedAt = entry.updatedAt
                        )
                    )
                }
            }
        }

        return BackupImportSummary(
            habitCount = appliedHabits,
            entryCount = appliedEntries
        )
    }
}
