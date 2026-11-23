package com.example

import android.app.Application
import com.example.data.local.StreakkuDatabase
import com.example.data.preferences.DataStoreSettingsRepository
import com.example.data.repository.RoomHabitRepository
import com.example.domain.repository.HabitRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.time.DeviceDateTicker
import com.example.reminder.ReminderScheduler

class AppContainer(application: Application) {
    val database: StreakkuDatabase = StreakkuDatabase.getInstance(application)
    val habitRepository: HabitRepository = RoomHabitRepository(database)
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(application)
    val dateTicker: DeviceDateTicker = DeviceDateTicker().also { it.start(application) }
}

class StreakkuApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderScheduler.ensureNotificationChannel(this)
    }
}
