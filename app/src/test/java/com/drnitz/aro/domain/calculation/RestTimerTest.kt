package com.drnitz.aro.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestTimerTest {

    @Test
    fun `initial snapshot is not running and has correct default duration`() {
        val snapshot = RestTimerEngine.createInitial(90)
        assertEquals(90, snapshot.totalSeconds)
        assertEquals(90, snapshot.remainingSeconds)
        assertFalse(snapshot.isRunning)
        assertFalse(snapshot.isPaused)
        assertFalse(snapshot.isFinished)
        assertEquals("01:30", snapshot.formattedRemaining)
        assertEquals(0f, snapshot.progress, 0.001f)
    }

    @Test
    fun `starting timer computes deterministic target end millis`() {
        val startMillis = 100_000L
        val snapshot = RestTimerEngine.start(seconds = 60, currentElapsedMillis = startMillis)
        assertTrue(snapshot.isRunning)
        assertFalse(snapshot.isPaused)
        assertFalse(snapshot.isFinished)
        assertEquals(60, snapshot.totalSeconds)
        assertEquals(60, snapshot.remainingSeconds)
        assertEquals(160_000L, snapshot.targetEndElapsedMillis)
        assertEquals("01:00", snapshot.formattedRemaining)
    }

    @Test
    fun `tick decrements remaining seconds accurately`() {
        val startMillis = 100_000L
        var snapshot = RestTimerEngine.start(seconds = 60, currentElapsedMillis = startMillis)

        // 15 seconds elapsed
        snapshot = RestTimerEngine.tick(snapshot, currentElapsedMillis = startMillis + 15_000L)
        assertEquals(45, snapshot.remainingSeconds)
        assertEquals("00:45", snapshot.formattedRemaining)
        assertEquals(0.25f, snapshot.progress, 0.01f)
        assertTrue(snapshot.isRunning)
        assertFalse(snapshot.isFinished)

        // 50 seconds elapsed
        snapshot = RestTimerEngine.tick(snapshot, currentElapsedMillis = startMillis + 50_000L)
        assertEquals(10, snapshot.remainingSeconds)
        assertEquals("00:10", snapshot.formattedRemaining)

        // 60 seconds elapsed -> Finished!
        snapshot = RestTimerEngine.tick(snapshot, currentElapsedMillis = startMillis + 60_000L)
        assertEquals(0, snapshot.remainingSeconds)
        assertFalse(snapshot.isRunning)
        assertTrue(snapshot.isFinished)
        assertEquals("00:00", snapshot.formattedRemaining)
        assertEquals(1f, snapshot.progress, 0.001f)
    }

    @Test
    fun `pause freezes remaining seconds and resume restores target end millis`() {
        val startMillis = 200_000L
        var snapshot = RestTimerEngine.start(seconds = 90, currentElapsedMillis = startMillis)

        // Advance 30s -> 60s remaining
        snapshot = RestTimerEngine.tick(snapshot, currentElapsedMillis = startMillis + 30_000L)
        assertEquals(60, snapshot.remainingSeconds)

        // Pause
        snapshot = RestTimerEngine.pause(snapshot, currentElapsedMillis = startMillis + 30_000L)
        assertFalse(snapshot.isRunning)
        assertTrue(snapshot.isPaused)
        assertEquals(60, snapshot.pausedRemainingSeconds)
        assertEquals(60, snapshot.remainingSeconds)

        // Even if tick is called while paused, remaining seconds do not change
        snapshot = RestTimerEngine.tick(snapshot, currentElapsedMillis = startMillis + 60_000L)
        assertEquals(60, snapshot.remainingSeconds)

        // Resume at a later time (e.g. 50s later)
        val resumeMillis = startMillis + 80_000L
        snapshot = RestTimerEngine.resume(snapshot, currentElapsedMillis = resumeMillis)
        assertTrue(snapshot.isRunning)
        assertFalse(snapshot.isPaused)
        assertEquals(60, snapshot.remainingSeconds)
        assertEquals(resumeMillis + 60_000L, snapshot.targetEndElapsedMillis)

        // Advance 20s after resume -> 40s remaining
        snapshot = RestTimerEngine.tick(snapshot, currentElapsedMillis = resumeMillis + 20_000L)
        assertEquals(40, snapshot.remainingSeconds)
    }

    @Test
    fun `adjust time adds or subtracts seconds dynamically`() {
        val startMillis = 50_000L
        var snapshot = RestTimerEngine.start(seconds = 60, currentElapsedMillis = startMillis)

        // Add 30 seconds
        snapshot = RestTimerEngine.adjustTime(snapshot, deltaSeconds = 30, currentElapsedMillis = startMillis)
        assertEquals(90, snapshot.remainingSeconds)
        assertEquals(90, snapshot.totalSeconds)
        assertEquals(startMillis + 90_000L, snapshot.targetEndElapsedMillis)

        // Subtract 15 seconds
        snapshot = RestTimerEngine.adjustTime(snapshot, deltaSeconds = -15, currentElapsedMillis = startMillis)
        assertEquals(75, snapshot.remainingSeconds)
        assertEquals(startMillis + 75_000L, snapshot.targetEndElapsedMillis)
    }

    @Test
    fun `skip marks timer as finished immediately`() {
        val startMillis = 10_000L
        var snapshot = RestTimerEngine.start(seconds = 120, currentElapsedMillis = startMillis)
        snapshot = RestTimerEngine.skip(snapshot)
        assertEquals(0, snapshot.remainingSeconds)
        assertFalse(snapshot.isRunning)
        assertTrue(snapshot.isFinished)
    }

    @Test
    fun `reset restores initial state`() {
        val startMillis = 10_000L
        var snapshot = RestTimerEngine.start(seconds = 120, currentElapsedMillis = startMillis)
        snapshot = RestTimerEngine.reset(snapshot, defaultSeconds = 60)
        assertEquals(60, snapshot.totalSeconds)
        assertEquals(60, snapshot.remainingSeconds)
        assertFalse(snapshot.isRunning)
        assertFalse(snapshot.isFinished)
    }
}
