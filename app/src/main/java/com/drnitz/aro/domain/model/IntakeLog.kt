package com.drnitz.aro.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * One logged drink.
 *
 * The daily `days.water_ml` column remains the fast dashboard aggregate, while this
 * record keeps the individual event: when it happened and how much was drunk. That is
 * what makes "how many times did I drink today?" answerable after the dialog closes.
 */
data class WaterLog(
    val id: String,
    val date: LocalDate,
    val amountMl: Int,
    val loggedAt: Instant,
    val source: WaterLogSource = WaterLogSource.MANUAL,
) {
    /** A single aggregate row created from pre-v8 hydration totals. */
    val isBackfilled: Boolean get() = source == WaterLogSource.BACKFILL
}

/**
 * Where a water entry came from.
 *
 * [BACKFILL] marks the one-time migration of an old per-day total. [CORRECTION] is used
 * when a caller replaces a day's total directly instead of adding a drink.
 */
enum class WaterLogSource {
    MANUAL,
    BACKFILL,
    CORRECTION;

    companion object {
        /** Unknown future values fall back to a manual entry rather than crashing. */
        fun fromStorage(raw: String?): WaterLogSource =
            entries.firstOrNull { it.name == raw } ?: MANUAL
    }
}

/**
 * One fasting window, active while [endedAt] is null.
 *
 * Durations are derived from the absolute timestamps, never stored, so a session stays
 * correct across process death and clock changes in the same way [ActiveFast] does.
 */
data class FastingSession(
    val id: String,
    val date: LocalDate,
    val targetHours: Int,
    val startedAt: Instant,
    val endedAt: Instant?,
    val createdAt: Instant,
    /** Optional context: hunger, energy, mood, electrolytes, etc. */
    val note: String = "",
) {
    val isActive: Boolean get() = endedAt == null

    fun durationMinutes(now: Instant = Instant.now()): Long =
        Duration.between(startedAt, endedAt ?: now).toMinutes().coerceAtLeast(0)

    fun metTarget(now: Instant = Instant.now()): Boolean =
        durationMinutes(now) >= targetHours * 60L
}
