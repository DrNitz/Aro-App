package com.drnitz.aro.domain

import com.drnitz.aro.domain.calculation.monthGridDates
import com.drnitz.aro.domain.calculation.weekDaysInOrder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class MonthGridTest {

    @Test
    fun october2026MondayStart_hasSixRowsAndIncludesAdjacentDays() {
        val dates = monthGridDates(
            month = YearMonth.of(2026, 10),
            weekStart = DayOfWeek.MONDAY,
        )

        assertEquals(42, dates.size)
        assertEquals(LocalDate.of(2026, 9, 28), dates.first())
        assertEquals(LocalDate.of(2026, 11, 8), dates.last())
        assertEquals(3, dates.count { YearMonth.from(it) == YearMonth.of(2026, 9) })
        assertEquals(31, dates.count { YearMonth.from(it) == YearMonth.of(2026, 10) })
    }

    @Test
    fun sundayStart_usesLocaleWeekStart() {
        val dates = monthGridDates(
            month = YearMonth.of(2026, 10),
            weekStart = DayOfWeek.SUNDAY,
        )

        assertEquals(42, dates.size)
        assertEquals(DayOfWeek.SUNDAY, dates.first().dayOfWeek)
        assertEquals(LocalDate.of(2026, 9, 27), dates.first())
        assertEquals(LocalDate.of(2026, 11, 7), dates.last())
    }

    @Test
    fun weekDaysInOrder_rotatesToWeekStart() {
        assertEquals(
            listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
            ),
            weekDaysInOrder(DayOfWeek.MONDAY),
        )
        assertEquals(DayOfWeek.SUNDAY, weekDaysInOrder(DayOfWeek.SUNDAY).first())
    }
}
