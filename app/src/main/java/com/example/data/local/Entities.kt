package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.ChangeLogStatus
import com.example.domain.model.EntryStatus
import com.example.domain.model.Habit
import com.example.domain.model.HabitEntry
import java.time.LocalDate

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String,
    val createdDate: String, // ISO YYYY-MM-DD
    val archived: Boolean,
    val sortOrder: Int,
    val autoFail: Boolean,
    val backfillLimitDays: Int?,
    val activeWeekdaysCsv: String // e.g. "1,2,3,4,5,6,7"
)

@Entity(
    tableName = "entries",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["updatedAt"])
    ]
)
data class EntryEntity(
    val habitId: String,
    val date: String, // ISO YYYY-MM-DD
    val status: String, // "DONE" or "FAILED"
    val updatedAt: Long
)

@Entity(
    tableName = "change_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["changedAt"])
    ]
)
data class ChangeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: String,
    val date: String, // ISO YYYY-MM-DD
    val newStatus: String, // "DONE", "FAILED", "EMPTY"
    val changedAt: Long
)

data class ChangeLogWithHabitTuple(
    val id: Long,
    val habitId: String,
    val habitName: String,
    val habitColorHex: String,
    val date: String,
    val newStatus: String,
    val changedAt: Long
)

fun HabitEntity.toDomain(): Habit {
    val parsedWeekdays = activeWeekdaysCsv
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..7 }
        .toSet()
        .ifEmpty { Habit.ALL_WEEKDAYS }

    return Habit(
        id = id,
        name = name,
        colorHex = colorHex,
        createdDate = LocalDate.parse(createdDate),
        archived = archived,
        sortOrder = sortOrder,
        autoFail = autoFail,
        backfillLimitDays = backfillLimitDays,
        activeWeekdays = parsedWeekdays
    )
}

fun Habit.toEntity(): HabitEntity {
    val sortedCsv = activeWeekdays
        .filter { it in 1..7 }
        .ifEmpty { Habit.ALL_WEEKDAYS }
        .sorted()
        .joinToString(",")

    return HabitEntity(
        id = id,
        name = name,
        colorHex = colorHex,
        createdDate = createdDate.toString(),
        archived = archived,
        sortOrder = sortOrder,
        autoFail = autoFail,
        backfillLimitDays = backfillLimitDays,
        activeWeekdaysCsv = sortedCsv
    )
}

fun EntryEntity.toDomain(): HabitEntry = HabitEntry(
    habitId = habitId,
    date = LocalDate.parse(date),
    status = EntryStatus.valueOf(status),
    updatedAt = updatedAt
)

fun HabitEntry.toEntity(): EntryEntity = EntryEntity(
    habitId = habitId,
    date = date.toString(),
    status = status.name,
    updatedAt = updatedAt
)

fun ChangeLogWithHabitTuple.toDomain(): com.example.domain.model.ChangeLogRecord =
    com.example.domain.model.ChangeLogRecord(
        id = id,
        habitId = habitId,
        habitName = habitName,
        habitColorHex = habitColorHex,
        date = LocalDate.parse(date),
        newStatus = runCatching { ChangeLogStatus.valueOf(newStatus) }
            .getOrDefault(ChangeLogStatus.EMPTY),
        changedAt = changedAt
    )
