package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppSettings
import com.example.domain.model.ThemeMode
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.streakkuDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "streakku_settings"
)

class DataStoreSettingsRepository(
    private val context: Context
) : SettingsRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    }

    override val settings: Flow<AppSettings> = context.streakkuDataStore.data.map { prefs ->
        val themeName = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeName) }.getOrDefault(ThemeMode.SYSTEM)
        AppSettings(
            themeMode = themeMode,
            reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: false,
            reminderHour = (prefs[Keys.REMINDER_HOUR] ?: 20).coerceIn(0, 23),
            reminderMinute = (prefs[Keys.REMINDER_MINUTE] ?: 0).coerceIn(0, 59)
        )
    }

    override suspend fun getSettingsSnapshot(): AppSettings = settings.first()

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.streakkuDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        context.streakkuDataStore.edit { prefs ->
            prefs[Keys.REMINDER_ENABLED] = enabled
        }
    }

    override suspend fun setReminderTime(hour: Int, minute: Int) {
        context.streakkuDataStore.edit { prefs ->
            prefs[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }
}
