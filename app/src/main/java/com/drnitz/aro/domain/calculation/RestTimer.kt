package com.drnitz.aro.domain.calculation

import java.util.Locale

/**
 * Immutable state model for a gym rest timer.
 *
 * Designed to be pure Kotlin (complying with [DomainPurityTest]), deterministic,
 * and resilient across Android system events or UI recompositions by tracking
 * the target completion timestamp rather than incrementing seconds in a loose loop.
 */
data class RestTimerSnapshot(
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean,
    val isPaused: Boolean,
    val isFinished: Boolean,
    val targetEndElapsedMillis: Long = 0L,
    val pausedRemainingSeconds: Int = 0,
) {
    val progress: Float
        get() = if (totalSeconds > 0) {
            ((totalSeconds - remainingSeconds).toFloat() / totalSeconds).coerceIn(0f, 1f)
        } else {
            1f
        }

    val formattedRemaining: String
        get() {
            val safe = remainingSeconds.coerceAtLeast(0)
            val minutes = safe / 60
            val secs = safe % 60
            return String.format(Locale.US, "%02d:%02d", minutes, secs)
        }
}

object RestTimerEngine {
    val PRESET_DURATIONS_SECONDS = listOf(30, 45, 60, 90, 120, 180, 300)

    fun createInitial(defaultSeconds: Int = 90): RestTimerSnapshot =
        RestTimerSnapshot(
            totalSeconds = defaultSeconds,
            remainingSeconds = defaultSeconds,
            isRunning = false,
            isPaused = false,
            isFinished = false,
        )

    fun start(seconds: Int, currentElapsedMillis: Long): RestTimerSnapshot {
        val safeSeconds = seconds.coerceAtLeast(1)
        return RestTimerSnapshot(
            totalSeconds = safeSeconds,
            remainingSeconds = safeSeconds,
            isRunning = true,
            isPaused = false,
            isFinished = false,
            targetEndElapsedMillis = currentElapsedMillis + (safeSeconds * 1000L),
            pausedRemainingSeconds = 0,
        )
    }

    fun tick(snapshot: RestTimerSnapshot, currentElapsedMillis: Long): RestTimerSnapshot {
        if (!snapshot.isRunning || snapshot.isPaused) return snapshot
        val remainingMillis = snapshot.targetEndElapsedMillis - currentElapsedMillis
        val remainingSecs = kotlin.math.ceil(remainingMillis / 1000.0).toInt()
        return if (remainingSecs <= 0) {
            snapshot.copy(
                remainingSeconds = 0,
                isRunning = false,
                isPaused = false,
                isFinished = true,
            )
        } else {
            snapshot.copy(
                remainingSeconds = remainingSecs.coerceAtMost(snapshot.totalSeconds),
                isFinished = false,
            )
        }
    }

    fun pause(snapshot: RestTimerSnapshot, currentElapsedMillis: Long): RestTimerSnapshot {
        if (!snapshot.isRunning || snapshot.isPaused) return snapshot
        val remainingMillis = (snapshot.targetEndElapsedMillis - currentElapsedMillis).coerceAtLeast(0)
        val remainingSecs = kotlin.math.ceil(remainingMillis / 1000.0).toInt().coerceAtLeast(0)
        return snapshot.copy(
            remainingSeconds = remainingSecs,
            isRunning = false,
            isPaused = true,
            pausedRemainingSeconds = remainingSecs,
        )
    }

    fun resume(snapshot: RestTimerSnapshot, currentElapsedMillis: Long): RestTimerSnapshot {
        if (!snapshot.isPaused) return snapshot
        val resumeSecs = snapshot.pausedRemainingSeconds.coerceAtLeast(1)
        return snapshot.copy(
            remainingSeconds = resumeSecs,
            isRunning = true,
            isPaused = false,
            targetEndElapsedMillis = currentElapsedMillis + (resumeSecs * 1000L),
            pausedRemainingSeconds = 0,
        )
    }

    fun adjustTime(
        snapshot: RestTimerSnapshot,
        deltaSeconds: Int,
        currentElapsedMillis: Long,
    ): RestTimerSnapshot {
        if (snapshot.isFinished) {
            return if (deltaSeconds > 0) {
                start(deltaSeconds, currentElapsedMillis)
            } else {
                snapshot
            }
        }
        val newRemaining = (snapshot.remainingSeconds + deltaSeconds).coerceAtLeast(0)
        if (newRemaining == 0) {
            return snapshot.copy(
                remainingSeconds = 0,
                isRunning = false,
                isPaused = false,
                isFinished = true,
            )
        }
        val newTotal = maxOf(snapshot.totalSeconds, newRemaining)
        return if (snapshot.isRunning) {
            snapshot.copy(
                totalSeconds = newTotal,
                remainingSeconds = newRemaining,
                targetEndElapsedMillis = currentElapsedMillis + (newRemaining * 1000L),
            )
        } else if (snapshot.isPaused) {
            snapshot.copy(
                totalSeconds = newTotal,
                remainingSeconds = newRemaining,
                pausedRemainingSeconds = newRemaining,
            )
        } else {
            snapshot.copy(
                totalSeconds = newTotal,
                remainingSeconds = newRemaining,
            )
        }
    }

    fun skip(snapshot: RestTimerSnapshot): RestTimerSnapshot =
        snapshot.copy(
            remainingSeconds = 0,
            isRunning = false,
            isPaused = false,
            isFinished = true,
        )

    fun reset(snapshot: RestTimerSnapshot, defaultSeconds: Int? = null): RestTimerSnapshot {
        val secs = defaultSeconds ?: snapshot.totalSeconds
        return createInitial(secs)
    }
}
