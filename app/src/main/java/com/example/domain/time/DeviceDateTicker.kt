package com.example.domain.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Tracks the device's current LocalDate in ZoneId.systemDefault() and automatically
 * rolls over at exact device midnight without requiring an app restart.
 */
class DeviceDateTicker(
    private val clockProvider: () -> Clock = { Clock.systemDefaultZone() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _currentDate = MutableStateFlow(todayNow())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    private var receiverRegistered = false

    private val dateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refresh()
        }
    }

    fun start(context: Context) {
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            ContextCompat.registerReceiver(
                context.applicationContext,
                dateChangeReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
        startMidnightLoop()
    }

    fun refresh() {
        val nowDate = todayNow()
        if (_currentDate.value != nowDate) {
            _currentDate.value = nowDate
        }
    }

    fun todayNow(): LocalDate {
        val clock = clockProvider()
        return LocalDate.now(clock)
    }

    fun millisUntilNextMidnight(): Long {
        val clock = clockProvider()
        val zone: ZoneId = clock.zone
        val now = ZonedDateTime.now(clock)
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zone)
        return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(50L) + 50L
    }

    private fun startMidnightLoop() {
        scope.launch {
            while (isActive) {
                val waitMillis = millisUntilNextMidnight()
                delay(waitMillis)
                refresh()
            }
        }
    }
}
