package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    val IST_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    private fun isoFormat(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = IST_TIMEZONE
    }

    private fun prettyFormat(): SimpleDateFormat = SimpleDateFormat("EEEE, d MMM yyyy", Locale.US).apply {
        timeZone = IST_TIMEZONE
    }

    private fun shortDayFormat(): SimpleDateFormat = SimpleDateFormat("EEE", Locale.US).apply {
        timeZone = IST_TIMEZONE
    }

    private fun timeFormat(): SimpleDateFormat = SimpleDateFormat("hh:mm a", Locale.US).apply {
        timeZone = IST_TIMEZONE
    }

    fun todayIso(): String = isoFormat().format(Date())

    fun offsetDateIso(baseIso: String, daysOffset: Int): String {
        val cal = Calendar.getInstance(IST_TIMEZONE)
        val parsed = runCatching { isoFormat().parse(baseIso) }.getOrNull() ?: Date()
        cal.time = parsed
        cal.add(Calendar.DAY_OF_YEAR, daysOffset)
        return isoFormat().format(cal.time)
    }

    fun yesterdayIso(baseIso: String = todayIso()): String = offsetDateIso(baseIso, -1)

    fun lastNDaysIso(n: Int, endIso: String = todayIso()): List<String> =
        (n - 1 downTo 0).map { offsetDateIso(endIso, -it) }

    fun formatPrettyDate(iso: String): String {
        val date = runCatching { isoFormat().parse(iso) }.getOrNull() ?: Date()
        return prettyFormat().format(date)
    }

    fun formatShortDay(iso: String): String {
        val date = runCatching { isoFormat().parse(iso) }.getOrNull() ?: Date()
        return shortDayFormat().format(date)
    }

    fun formatTime(epochMs: Long): String = timeFormat().format(Date(epochMs))

    fun currentGreeting(): String {
        val hour = Calendar.getInstance(IST_TIMEZONE).get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 5 -> "Burning the midnight oil"
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            hour < 21 -> "Good evening"
            else -> "Wind down strong"
        }
    }

    fun dayOfWeekBit(iso: String): Int {
        val cal = Calendar.getInstance(IST_TIMEZONE)
        val parsed = runCatching { isoFormat().parse(iso) }.getOrNull() ?: Date()
        cal.time = parsed
        // Calendar.MONDAY = 2 .. SUNDAY = 1 -> map Mon=0..Sun=6
        val idx = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> 6
        }
        return 1 shl idx
    }

    fun isFutureIso(iso: String, today: String = todayIso()): Boolean = iso > today

    fun daysBetweenInclusive(startIso: String, endIso: String): Int {
        val s = runCatching { isoFormat().parse(startIso)?.time }.getOrNull() ?: return 1
        val e = runCatching { isoFormat().parse(endIso)?.time }.getOrNull() ?: return 1
        val diff = ((e - s) / (24L * 60L * 60L * 1000L)).toInt() + 1
        return diff.coerceAtLeast(1)
    }

    fun dateRange(startIso: String, endIso: String, maxDays: Int = 90): List<String> {
        val total = daysBetweenInclusive(startIso, endIso).coerceAtMost(maxDays)
        return (0 until total).map { offsetDateIso(startIso, it) }
    }
}
