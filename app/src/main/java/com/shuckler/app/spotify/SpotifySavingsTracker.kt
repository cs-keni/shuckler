package com.shuckler.app.spotify

import android.content.Context
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class SpotifySavingsTracker(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var monthlyCost: Float
        get() = prefs.getFloat(KEY_MONTHLY_COST, DEFAULT_MONTHLY_COST)
        set(value) { prefs.edit().putFloat(KEY_MONTHLY_COST, value).apply() }

    var cancellationDateEpochDay: Long
        get() = prefs.getLong(KEY_CANCELLATION_DATE, -1L)
        set(value) { prefs.edit().putLong(KEY_CANCELLATION_DATE, value).apply() }

    val hasCancellationDate: Boolean
        get() = cancellationDateEpochDay >= 0

    fun setCancelledToday() {
        cancellationDateEpochDay = LocalDate.now().toEpochDay()
    }

    fun getCancellationDate(): LocalDate? {
        val epoch = cancellationDateEpochDay
        return if (epoch >= 0) LocalDate.ofEpochDay(epoch) else null
    }

    fun savedThisMonth(): Float {
        val cancelDate = getCancellationDate() ?: return 0f
        val today = LocalDate.now()
        if (today.isBefore(cancelDate)) return 0f
        val cancelMonth = YearMonth.from(cancelDate)
        val thisMonth = YearMonth.from(today)
        return if (cancelMonth == thisMonth) {
            // Partial month: days remaining from cancel date to end of month
            val daysInMonth = thisMonth.lengthOfMonth().toFloat()
            val daysActive = (today.dayOfMonth - cancelDate.dayOfMonth + 1).coerceAtLeast(0).toFloat()
            (daysActive / daysInMonth) * monthlyCost
        } else {
            monthlyCost
        }
    }

    fun savedThisYear(): Float {
        val cancelDate = getCancellationDate() ?: return 0f
        val today = LocalDate.now()
        if (today.isBefore(cancelDate)) return 0f
        val months = ChronoUnit.MONTHS.between(
            YearMonth.from(cancelDate),
            YearMonth.from(today)
        ).toFloat()
        return (months * monthlyCost).coerceAtLeast(savedThisMonth())
    }

    fun savedTotal(): Float {
        val cancelDate = getCancellationDate() ?: return 0f
        val today = LocalDate.now()
        if (today.isBefore(cancelDate)) return 0f
        val months = ChronoUnit.MONTHS.between(cancelDate, today).toFloat()
        return months * monthlyCost
    }

    companion object {
        private const val PREFS_NAME = "spotify_savings"
        private const val KEY_MONTHLY_COST = "monthly_cost"
        private const val KEY_CANCELLATION_DATE = "cancellation_date"
        const val DEFAULT_MONTHLY_COST = 13f
    }
}
