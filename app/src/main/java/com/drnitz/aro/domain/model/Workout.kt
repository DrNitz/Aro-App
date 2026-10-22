package com.drnitz.aro.domain.model

import java.time.Instant

/** Resistance training vs. steady-state cardio. */
enum class WorkoutType(val label: String) {
    STRENGTH("Resistance"),
    CARDIO("Cardio"),
}

/** One working set: the load lifted and the repetitions completed. */
data class SetEntry(
    val weightKg: Double,
    val reps: Int,
) {
    val isValid: Boolean get() = weightKg > 0.0 && reps > 0

    /** Tonnage contributed by this set (load × reps). */
    val volumeKg: Double get() = if (isValid) weightKg * reps else 0.0
}

/**
 * A logged workout.
 *
 * Deliberately a flat model rather than a sealed STRENGTH/CARDIO hierarchy: Task 5
 * persists this straight to a Room `WorkoutEntity` plus a child `WorkoutSetEntity`,
 * and the `type` discriminator is a plain column.
 *
 * [bodyWeightKg] snapshots the user's weight at log time so historical burn estimates
 * do not silently change when the profile is later updated — the web app recalculated
 * them against the *current* weight, which quietly rewrote past days.
 *
 * No formulas live here: tonnage, 1RM and calorie burn are in
 * `com.drnitz.aro.domain.calculation`.
 */
data class WorkoutLog(
    val id: String,
    val type: WorkoutType,
    val exerciseName: String,
    val loggedAt: Instant,
    /** Metabolic equivalent, from the Compendium of Physical Activities. */
    val met: Double,
    val durationMinutes: Int,
    /** Optional GPS/step-estimated or manually entered distance for cardio. */
    val distanceKm: Double? = null,
    /** Optional average speed for cardio, in kilometres per hour. */
    val averageSpeedKph: Double? = null,
    /** Optional phone step-counter reading captured during a walk or run. */
    val steps: Int? = null,
    /** Body weight at time of logging; drives every calorie estimate. */
    val bodyWeightKg: Double? = null,
    /** Resistance sets; empty for cardio. */
    val sets: List<SetEntry> = emptyList(),
    val category: String = "",
    val muscle: String? = null,
    val equipment: String? = null,
) {
    val isStrength: Boolean get() = type == WorkoutType.STRENGTH
    val isCardio: Boolean get() = type == WorkoutType.CARDIO

    /** Total tonnage across all working sets. Zero for cardio. */
    val totalVolumeKg: Double get() = if (isStrength) sets.sumOf { it.volumeKg } else 0.0

    val workingSets: List<SetEntry> get() = sets.filter { it.isValid }

    /** Heaviest single working set, used for at-a-glance PR display. */
    val topSetKg: Double get() = sets.maxOfOrNull { it.weightKg } ?: 0.0
}