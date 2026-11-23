package com.example.domain.logic

import com.example.domain.model.BackupPayload
import com.example.domain.model.Habit
import com.example.domain.model.HabitEntry
import java.time.LocalDate

data class BackupMergePlan(
    val habitsToUpsert: List<Habit>,
    val entriesToUpsert: List<HabitEntry>
)

/**
 * Pure domain merge rules for backup import:
 * - Upserts incoming habits by id.
 * - For each incoming entry referencing a known habit id, inserts if absent,
 *   or replaces existing entry strictly when incoming.updatedAt > existing.updatedAt.
 */
object BackupMergeEngine {

    fun planMerge(
        existingHabitIds: Set<String>,
        existingEntries: Map<Pair<String, LocalDate>, HabitEntry>,
        payload: BackupPayload
    ): BackupMergePlan {
        val knownHabitIds = existingHabitIds.toMutableSet()
        val habitsToUpsert = mutableListOf<Habit>()
        for (habit in payload.habits) {
            habitsToUpsert.add(habit)
            knownHabitIds.add(habit.id)
        }

        val latestEntries = existingEntries.toMutableMap()
        val entriesToUpsert = mutableListOf<HabitEntry>()
        for (entry in payload.entries) {
            if (entry.habitId !in knownHabitIds) continue
            val key = entry.habitId to entry.date
            val existing = latestEntries[key]
            if (existing == null || entry.updatedAt > existing.updatedAt) {
                latestEntries[key] = entry
                entriesToUpsert.add(entry)
            }
        }

        return BackupMergePlan(
            habitsToUpsert = habitsToUpsert,
            entriesToUpsert = entriesToUpsert
        )
    }
}
