package com.drnitz.aro.data.db

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Resolves "which day is it" in exactly one place.
 *
 * Every read and write goes through this, which is what makes **yesterday vs today
 * isolation** hold: a day boundary is decided once, at the moment of use, from the same
 * clock, rather than each screen guessing. `LocalDate.now()` scattered across a codebase
 * is how a fasted session ends up filed against the wrong day after a midnight rollover
 * or a timezone change.
 *
 * Injectable so tests can pin the clock and assert that two consecutive days never mix.
 */
interface DateProvider {
    fun today(): LocalDate

    fun yesterday(): LocalDate = today().minusDays(1)

    /** The calendar day an instant falls in, in the provider's zone. */
    fun dayOf(instant: Instant): LocalDate = instant.atZone(zone()).toLocalDate()

    fun zone(): ZoneId = ZoneId.systemDefault()
}

class SystemDateProvider(
    private val clock: () -> Instant = { Instant.now() },
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : DateProvider {

    override fun today(): LocalDate = clock().atZone(zoneId).toLocalDate()

    override fun zone(): ZoneId = zoneId

    override fun dayOf(instant: Instant): LocalDate = instant.atZone(zoneId).toLocalDate()
}

/** A fixed clock, for deterministic tests. */
class FixedDateProvider(private val date: LocalDate) : DateProvider {
    override fun today(): LocalDate = date
    override fun zone(): ZoneId = ZoneId.of("UTC")
}

/**
 * A closed day range for chart and history queries.
 *
 * Expressed as epoch-day bounds so DAO queries stay integer comparisons on an indexed
 * column — no string formatting, no timezone-sensitive `BETWEEN` on timestamps (which
 * silently drops same-day rows whose time-of-day falls outside the bounds).
 */
data class DayRange(val startEpochDay: Long, val endEpochDay: Long) {

    init {
        require(startEpochDay <= endEpochDay) {
            "DayRange start ($startEpochDay) must not be after end ($endEpochDay)"
        }
    }

    val dayCount: Long get() = endEpochDay - startEpochDay + 1

    operator fun contains(epochDay: Long): Boolean = epochDay in startEpochDay..endEpochDay

    fun toLocalDateRange(): Pair<LocalDate, LocalDate> =
        LocalDate.ofEpochDay(startEpochDay) to LocalDate.ofEpochDay(endEpochDay)

    companion object {
        fun singleDay(date: LocalDate): DayRange =
            DayRange(date.toEpochDay(), date.toEpochDay())

        fun lastDays(endingOn: LocalDate, days: Long): DayRange =
            DayRange(endingOn.minusDays(days - 1).toEpochDay(), endingOn.toEpochDay())

        fun between(start: LocalDate, endInclusive: LocalDate): DayRange =
            DayRange(start.toEpochDay(), endInclusive.toEpochDay())
    }
}