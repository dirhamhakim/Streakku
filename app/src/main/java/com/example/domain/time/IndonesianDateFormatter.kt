package com.example.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object IndonesianDateFormatter {

    private val dayNamesFull = listOf(
        "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"
    )

    private val dayNamesShort = listOf(
        "Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"
    )

    private val dayInitials = listOf(
        "S", "S", "R", "K", "J", "S", "M"
    )

    private val monthNamesFull = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    private val monthNamesShort = listOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    )

    private val timeDotFormatter = DateTimeFormatter.ofPattern("HH.mm", Locale.US)

    fun dayShort(isoDayOfWeek: Int): String =
        dayNamesShort[(isoDayOfWeek - 1).coerceIn(0, 6)]

    fun dayInitial(isoDayOfWeek: Int): String =
        dayInitials[(isoDayOfWeek - 1).coerceIn(0, 6)]

    fun dayFull(isoDayOfWeek: Int): String =
        dayNamesFull[(isoDayOfWeek - 1).coerceIn(0, 6)]

    fun weekdayHeaders(): List<String> = dayNamesShort

    /** Contoh: "Minggu, 22 Nov 2026" */
    fun formatFullDayShortMonth(date: LocalDate): String {
        val day = dayFull(date.dayOfWeek.value)
        val month = monthNamesShort[date.monthValue - 1]
        return "$day, ${date.dayOfMonth} $month ${date.year}"
    }

    /** Contoh: "Senin, 23 November 2026" */
    fun formatFullDayFullMonth(date: LocalDate): String {
        val day = dayFull(date.dayOfWeek.value)
        val month = monthNamesFull[date.monthValue - 1]
        return "$day, ${date.dayOfMonth} $month ${date.year}"
    }

    /** Contoh: "November 2026" */
    fun formatMonthYear(yearMonth: YearMonth): String {
        val month = monthNamesFull[yearMonth.monthValue - 1]
        return "$month ${yearMonth.year}"
    }

    /** Contoh: "22 Nov" */
    fun formatDayShortMonth(date: LocalDate): String {
        val month = monthNamesShort[date.monthValue - 1]
        return "${date.dayOfMonth} $month"
    }

    /** Contoh: "1 Nov 2026" */
    fun formatShortDate(date: LocalDate): String {
        val month = monthNamesShort[date.monthValue - 1]
        return "${date.dayOfMonth} $month ${date.year}"
    }

    /** Contoh: "7 Nov – 14 Nov 2026" atau "16 Nov – sekarang" */
    fun formatStreakRange(start: LocalDate, end: LocalDate, isOngoing: Boolean): String {
        if (isOngoing) {
            return "${formatDayShortMonth(start)} – sekarang"
        }
        return formatDateRange(start, end)
    }

    /** Contoh: "7 Nov – 14 Nov 2026" atau "29 Des 2025 – 2 Jan 2026" */
    fun formatDateRange(start: LocalDate, end: LocalDate): String {
        if (start == end) return formatShortDate(start)
        return when {
            start.year == end.year -> {
                "${formatDayShortMonth(start)} – ${formatDayShortMonth(end)} ${end.year}"
            }
            else -> "${formatShortDate(start)} – ${formatShortDate(end)}"
        }
    }

    /** Contoh: "Hari ini 08.12", "Kemarin 21.30", atau "20 Nov 19.02" */
    fun formatRelativeTimestamp(
        epochMillis: Long,
        today: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val date = zdt.toLocalDate()
        val time = zdt.format(timeDotFormatter)
        return when (date) {
            today -> "Hari ini $time"
            today.minusDays(1) -> "Kemarin $time"
            else -> "${formatDayShortMonth(date)} $time"
        }
    }

    fun formatTime(hour: Int, minute: Int): String =
        String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))

    fun formatBackupFilename(date: LocalDate): String =
        String.format(
            Locale.US,
            "streakku-backup-%04d%02d%02d.json",
            date.year,
            date.monthValue,
            date.dayOfMonth
        )

    fun formatPercent(value: Double?): String {
        if (value == null) return "–"
        val rounded = Math.round(value).toInt()
        return "$rounded%"
    }
}
