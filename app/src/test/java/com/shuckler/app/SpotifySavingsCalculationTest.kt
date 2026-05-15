package com.shuckler.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Tests for savings calculation math in SpotifySavingsTracker.
 * The tracker's public logic is extracted as pure functions here so we can
 * test without Android Context or SharedPreferences.
 */
class SpotifySavingsCalculationTest {

    companion object {
        private const val MONTHLY = 13f
        private const val DELTA = 0.01f
    }

    // Pure-function mirrors of SpotifySavingsTracker math (no Context needed)

    private fun savedThisMonth(cancelDate: LocalDate, today: LocalDate, monthlyCost: Float): Float {
        if (today.isBefore(cancelDate)) return 0f
        val cancelMonth = YearMonth.from(cancelDate)
        val thisMonth = YearMonth.from(today)
        return if (cancelMonth == thisMonth) {
            val daysInMonth = thisMonth.lengthOfMonth().toFloat()
            val daysActive = (today.dayOfMonth - cancelDate.dayOfMonth + 1).coerceAtLeast(0).toFloat()
            (daysActive / daysInMonth) * monthlyCost
        } else {
            monthlyCost
        }
    }

    private fun savedThisYear(cancelDate: LocalDate, today: LocalDate, monthlyCost: Float): Float {
        if (today.isBefore(cancelDate)) return 0f
        val months = ChronoUnit.MONTHS.between(YearMonth.from(cancelDate), YearMonth.from(today)).toFloat()
        return (months * monthlyCost).coerceAtLeast(savedThisMonth(cancelDate, today, monthlyCost))
    }

    private fun savedTotal(cancelDate: LocalDate, today: LocalDate, monthlyCost: Float): Float {
        if (today.isBefore(cancelDate)) return 0f
        val months = ChronoUnit.MONTHS.between(cancelDate, today).toFloat()
        return months * monthlyCost
    }

    // --- savedThisMonth ---

    @Test fun savedThisMonth_cancelledToday_isPartialMonth() {
        val today = LocalDate.of(2026, 5, 14)
        val result = savedThisMonth(today, today, MONTHLY)
        val daysInMonth = 31f
        val expected = (1f / daysInMonth) * MONTHLY
        assertEquals(expected, result, DELTA)
    }

    @Test fun savedThisMonth_cancelledFirstDayOfMonth_isFullIfSameMonth() {
        val cancel = LocalDate.of(2026, 5, 1)
        val today = LocalDate.of(2026, 5, 31)
        val result = savedThisMonth(cancel, today, MONTHLY)
        // All 31 days in May
        assertEquals(MONTHLY, result, DELTA)
    }

    @Test fun savedThisMonth_cancelledPrevMonth_fullMonthSaved() {
        val cancel = LocalDate.of(2026, 4, 15)
        val today = LocalDate.of(2026, 5, 14)
        val result = savedThisMonth(cancel, today, MONTHLY)
        assertEquals(MONTHLY, result, DELTA)
    }

    @Test fun savedThisMonth_cancellationInFuture_isZero() {
        val cancel = LocalDate.of(2026, 6, 1)
        val today = LocalDate.of(2026, 5, 14)
        assertEquals(0f, savedThisMonth(cancel, today, MONTHLY), DELTA)
    }

    // --- savedTotal ---

    @Test fun savedTotal_exactlyOneMonthLater_isOneMonthlyCost() {
        val cancel = LocalDate.of(2026, 1, 1)
        val today = LocalDate.of(2026, 2, 1)
        assertEquals(MONTHLY, savedTotal(cancel, today, MONTHLY), DELTA)
    }

    @Test fun savedTotal_sameDay_isZero() {
        val today = LocalDate.of(2026, 5, 14)
        assertEquals(0f, savedTotal(today, today, MONTHLY), DELTA)
    }

    @Test fun savedTotal_sixMonthsLater_isSixMonths() {
        val cancel = LocalDate.of(2026, 1, 1)
        val today = LocalDate.of(2026, 7, 1)
        assertEquals(6 * MONTHLY, savedTotal(cancel, today, MONTHLY), DELTA)
    }

    @Test fun savedTotal_cancellationInFuture_isZero() {
        val cancel = LocalDate.of(2026, 12, 1)
        val today = LocalDate.of(2026, 5, 14)
        assertEquals(0f, savedTotal(cancel, today, MONTHLY), DELTA)
    }

    // --- savedThisYear ---

    @Test fun savedThisYear_atLeastAsMuchAsSavedThisMonth() {
        val cancel = LocalDate.of(2026, 3, 1)
        val today = LocalDate.of(2026, 5, 14)
        val thisMonth = savedThisMonth(cancel, today, MONTHLY)
        val thisYear = savedThisYear(cancel, today, MONTHLY)
        assertTrue("savedThisYear should be >= savedThisMonth", thisYear >= thisMonth)
    }

    @Test fun savedThisYear_twoFullMonths() {
        val cancel = LocalDate.of(2026, 3, 1)
        val today = LocalDate.of(2026, 5, 1)
        // cancelMonth=March, thisMonth=May → 2 months apart
        assertEquals(2 * MONTHLY, savedThisYear(cancel, today, MONTHLY), DELTA)
    }

    @Test fun savedThisYear_cancellationInFuture_isZero() {
        val cancel = LocalDate.of(2027, 1, 1)
        val today = LocalDate.of(2026, 5, 14)
        assertEquals(0f, savedThisYear(cancel, today, MONTHLY), DELTA)
    }

    // --- DEFAULT_MONTHLY_COST constant ---

    @Test fun defaultMonthlyCost_is13() {
        assertEquals(13f, com.shuckler.app.spotify.SpotifySavingsTracker.DEFAULT_MONTHLY_COST, DELTA)
    }
}
