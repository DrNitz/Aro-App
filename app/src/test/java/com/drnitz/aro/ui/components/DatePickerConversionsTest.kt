package com.drnitz.aro.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DatePickerConversionsTest {

    @Test
    fun `date picker millis represent UTC midnight and round trip`() {
        val date = LocalDate.of(2026, 10, 22)

        val millis = date.toDatePickerMillis()
        val utc = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC)

        assertEquals(0, utc.hour)
        assertEquals(0, utc.minute)
        assertEquals(0, utc.second)
        assertEquals(date, datePickerMillisToLocalDate(millis))
    }

    @Test
    fun `round trip keeps leap day and year boundaries`() {
        listOf(
            LocalDate.of(2024, 2, 29),
            LocalDate.of(2025, 12, 31),
            LocalDate.of(2026, 1, 1),
        ).forEach { date ->
            assertEquals(date, datePickerMillisToLocalDate(date.toDatePickerMillis()))
        }
    }

    @Test
    fun `late UTC millis still map to the intended calendar date`() {
        val date = LocalDate.of(2026, 10, 22)
        val almostMidnight = date.toDatePickerMillis() + 86_399_999L

        assertEquals(date, datePickerMillisToLocalDate(almostMidnight))
    }
}
