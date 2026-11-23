package com.example.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.local.StreakkuDatabase
import com.example.data.preferences.DataStoreSettingsRepository
import com.example.data.repository.RoomHabitRepository
import com.example.domain.logic.StreakCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val UNIQUE_WORK_NAME = "streakku_daily_reminder_work"
    const val CHANNEL_ID = "streakku_daily_reminder"
    const val NOTIFICATION_ID = 1001

    fun ensureNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun calculateDelayMillis(
        hour: Int,
        minute: Int,
        now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
    ): Long {
        var nextTrigger = now
            .withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)

        if (!nextTrigger.isAfter(now)) {
            nextTrigger = nextTrigger.plusDays(1)
        }
        return Duration.between(now, nextTrigger).toMillis().coerceAtLeast(1_000L)
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        ensureNotificationChannel(context)
        val delayMillis = calculateDelayMillis(hour, minute)
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settingsRepo = DataStoreSettingsRepository(applicationContext)
        val settings = settingsRepo.getSettingsSnapshot()
        if (!settings.reminderEnabled) {
            return Result.success()
        }

        val db = StreakkuDatabase.getInstance(applicationContext)
        val habitRepo = RoomHabitRepository(db)
        val today = LocalDate.now(ZoneId.systemDefault())

        val activeHabitsToday = habitRepo.getAllHabitsSnapshot()
            .filter { !it.archived && StreakCalculator.isActiveDay(it, today) }

        val entriesTodayHabitIds = habitRepo.getAllEntriesSnapshot()
            .asSequence()
            .filter { it.date == today }
            .map { it.habitId }
            .toSet()

        val unfilledCount = activeHabitsToday.count { it.id !in entriesTodayHabitIds }

        if (unfilledCount > 0) {
            showReminderNotification(unfilledCount)
        }

        // Schedule next day's reminder using device local time
        ReminderScheduler.scheduleDailyReminder(
            context = applicationContext,
            hour = settings.reminderHour,
            minute = settings.reminderMinute
        )

        return Result.success()
    }

    private fun showReminderNotification(unfilledCount: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ReminderScheduler.ensureNotificationChannel(applicationContext)

        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_streak)
            .setContentTitle(applicationContext.getString(R.string.notification_reminder_title))
            .setContentText(
                applicationContext.getString(R.string.notification_reminder_body, unfilledCount)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(ReminderScheduler.NOTIFICATION_ID, notification)
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settingsRepo = DataStoreSettingsRepository(context.applicationContext)
                    val settings = settingsRepo.getSettingsSnapshot()
                    if (settings.reminderEnabled) {
                        ReminderScheduler.scheduleDailyReminder(
                            context = context.applicationContext,
                            hour = settings.reminderHour,
                            minute = settings.reminderMinute
                        )
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
