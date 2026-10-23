package com.drnitz.aro.ui.intake

import com.drnitz.aro.domain.model.FastingSession
import com.drnitz.aro.domain.model.WaterLog
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class IntakeHistoryStatsTest {

    private val today = LocalDate.of(2026, 10, 23)

    private fun water(id: String, daysAgo: Long, amountMl: Int = 250) = WaterLog(
        id = id,
        date = today.minusDays(daysAgo),
        amountMl = amountMl,
        loggedAt = Instant.parse("2026-10-23T08:00:00Z"),
    )

    private fun fast(
        id: String,
        targetHours: Int,
        start: String,
        end: String?,
    ) = FastingSession(
        id = id,
        date = today,
        targetHours = targetHours,
        startedAt = Instant.parse(start),
        endedAt = end?.let(Instant::parse),
        createdAt = Instant.parse(start),
    )

    @Test
    fun `water counts are scoped to today, the last seven days and the last thirty`() {
        val state = IntakeHistoryUiState.Success(
            today = today,
            waterLogs = listOf(
                water("today-1", 0),
                water("today-2", 0, 500),
                water("week-1", 6),
                water("week-2", 7),
                water("month-1", 29),
                water("older", 30),
            ),
            fastingSessions = emptyList(),
        )

        assertEquals(2, state.todayWaterLogs.size)
        assertEquals(3, state.last7DaysWaterLogs.size)
        assertEquals(5, state.last30DaysWaterLogs.size)
        assertEquals(1750, state.totalWaterMl)
    }

    @Test
    fun `days are grouped newest first with their own entries`() {
        val state = IntakeHistoryUiState.Success(
            today = today,
            waterLogs = listOf(
                water("a", 0),
                water("b", 2),
                water("c", 0),
            ),
            fastingSessions = emptyList(),
        )

        val groups = state.waterLogsByDay
        assertEquals(listOf(today, today.minusDays(2)), groups.map { it.first })
        assertEquals(2, groups.first().second.size)
        assertEquals(1, groups.last().second.size)
    }

    @Test
    fun `fasting summary counts completed sessions and averages only those`() {
        val state = IntakeHistoryUiState.Success(
            today = today,
            waterLogs = emptyList(),
            fastingSessions = listOf(
                fast("met", 16, "2026-10-20T18:00:00Z", "2026-10-21T10:00:00Z"),
                fast("short", 16, "2026-10-21T18:00:00Z", "2026-10-22T02:00:00Z"),
                fast("active", 16, "2026-10-23T06:00:00Z", null),
            ),
        )

        assertEquals(2, state.completedFasts.size)
        assertEquals(1, state.targetMetCount)
        assertEquals(720L, state.averageCompletedMinutes) // (960 + 480) / 2
        assertEquals(1440L, state.totalCompletedMinutes)
    }

    @Test
    fun `an empty history has zeroed summaries and no groups`() {
        val state = IntakeHistoryUiState.Success(
            today = today,
            waterLogs = emptyList(),
            fastingSessions = emptyList(),
        )

        assertEquals(0, state.totalWaterMl)
        assertEquals(0, state.todayWaterLogs.size)
        assertEquals(0, state.last7DaysWaterLogs.size)
        assertEquals(0, state.last30DaysWaterLogs.size)
        assertEquals(0, state.waterLogsByDay.size)
        assertEquals(0, state.completedFasts.size)
        assertEquals(0, state.targetMetCount)
        assertEquals(0L, state.averageCompletedMinutes)
        assertEquals(0L, state.totalCompletedMinutes)
    }
}
