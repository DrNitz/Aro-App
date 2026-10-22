package com.drnitz.aro.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ActiveFastTest {

    private val start = Instant.parse("2026-10-18T06:00:00Z")

    private fun fastAt(hours: Long, target: Int = 16) =
        ActiveFast.of(
            id = "f",
            startedAt = start,
            targetHours = target,
            now = start.plusSeconds(hours * 3600),
        )

    @Test
    fun `elapsed time is derived from the start instant, never accumulated`() {
        assertEquals(0L, fastAt(0).elapsedMinutes)
        assertEquals(60L, fastAt(1).elapsedMinutes)
        assertEquals(16 * 60L, fastAt(16).elapsedMinutes)
    }

    @Test
    fun `a clock that has not moved never reports a negative fast`() {
        val backdated = ActiveFast.of("f", start, 16, start.minusSeconds(3600))
        assertEquals(0L, backdated.elapsedMinutes)
    }

    @Test
    fun `progress runs from zero to one and clamps beyond the target`() {
        assertEquals(0f, fastAt(0).progress, 0.0001f)
        assertEquals(0.5f, fastAt(8).progress, 0.0001f)
        assertEquals(1f, fastAt(16).progress, 0.0001f)
        assertEquals(1f, fastAt(40).progress, 0.0001f)
    }

    @Test
    fun `a zero target does not divide by zero`() {
        val fast = ActiveFast.of("f", start, 0, start.plusSeconds(3600))
        assertEquals(0f, fast.progress, 0.0001f)
    }

    @Test
    fun `elapsed text is zero-padded`() {
        assertEquals("0h 05m", fastAt(0).copy(elapsedMinutes = 5).elapsedText())
        assertEquals("16h 30m", fastAt(16).copy(elapsedMinutes = 16 * 60 + 30).elapsedText())
    }

    @Test
    fun `remaining text never goes negative`() {
        assertEquals("16h 00m left", fastAt(0).remainingText())
        assertEquals("0h 00m left", fastAt(20).remainingText())
    }

    @Test
    fun `metabolic stages advance with elapsed time`() {
        assertEquals("Digestive rest", fastAt(1).metabolicStage())
        assertEquals("Insulin drop", fastAt(6).metabolicStage())
        assertEquals("Fat oxidation", fastAt(14).metabolicStage())
        assertEquals("Ketosis / autophagy", fastAt(18).metabolicStage())
    }

    @Test
    fun `a fast survives a process restart because it is derived not counted`() {
        // Same persisted start, two "now" values: elapsed is always start -> now.
        val reopened = ActiveFast.of("f", start, 16, Instant.parse("2026-10-18T22:00:00Z"))
        assertEquals(16 * 60L, reopened.elapsedMinutes)
        assertTrue(reopened.progress >= 1f)
    }
}