package com.drnitz.aro.domain.calculation

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Six-week grid used by the Diary calendar. Adjacent-month days are included so the
 * month's height never jumps between five- and six-week months.
 */
fun monthGridDates(
    month: YearMonth,
    weekStart: DayOfWeek,
    weeks: Int = 6,
): List<LocalDate> {
    val first = month.atDay(1)
    val offset = (first.dayOfWeek.value - weekStart.value + 7) % 7
    val gridStart = first.minusDays(offset.toLong())
    return (0 until weeks * 7).map { gridStart.plusDays(it.toLong()) }
}

/** The seven weekday headers in display order. */
fun weekDaysInOrder(weekStart: DayOfWeek): List<DayOfWeek> =
    (0 until 7).map { weekStart.plus(it.toLong()) }
