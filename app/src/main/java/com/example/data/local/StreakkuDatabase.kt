package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HabitEntity::class,
        EntryEntity::class,
        ChangeLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StreakkuDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun entryDao(): EntryDao
    abstract fun changeLogDao(): ChangeLogDao

    companion object {
        private const val DATABASE_NAME = "streakku.db"

        @Volatile
        private var instance: StreakkuDatabase? = null

        fun getInstance(context: Context): StreakkuDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StreakkuDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
        }
    }
}
