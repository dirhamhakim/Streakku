package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY archived ASC, sortOrder ASC, createdDate ASC, name ASC")
    fun observeAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY archived ASC, sortOrder ASC, createdDate ASC, name ASC")
    suspend fun getAllHabitsSnapshot(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: String): HabitEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM habits")
    suspend fun getMaxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity)

    @Query("UPDATE habits SET archived = :archived WHERE id = :habitId")
    suspend fun setArchived(habitId: String, archived: Boolean)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: String)
}

@Dao
interface EntryDao {

    @Query("SELECT * FROM entries")
    fun observeAllEntries(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries ORDER BY habitId ASC, date ASC")
    suspend fun getAllEntriesSnapshot(): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getEntry(habitId: String, date: String): EntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE habitId = :habitId AND date = :date")
    suspend fun deleteEntry(habitId: String, date: String)
}

@Dao
interface ChangeLogDao {

    @Query(
        """
        SELECT c.id AS id,
               c.habitId AS habitId,
               h.name AS habitName,
               h.colorHex AS habitColorHex,
               c.date AS date,
               c.newStatus AS newStatus,
               c.changedAt AS changedAt
        FROM change_logs c
        INNER JOIN habits h ON h.id = c.habitId
        ORDER BY c.changedAt DESC, c.id DESC
        LIMIT :limit
        """
    )
    fun observeRecentLogs(limit: Int): Flow<List<ChangeLogWithHabitTuple>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: ChangeLogEntity)

    @Query("SELECT COUNT(*) FROM change_logs WHERE habitId = :habitId AND date = :date AND changedAt = :changedAt")
    suspend fun countMatchingLog(habitId: String, date: String, changedAt: Long): Int
}
