package com.drnitz.aro.domain.calculation

import com.drnitz.aro.domain.model.SetEntry
import com.drnitz.aro.domain.model.WorkoutLog
import kotlin.math.roundToInt

/** Which published formula produced a 1RM estimate. Surfaced in the logger UI. */
enum class OneRepMaxMethod(val label: String) {
    DIRECT_SINGLE("Single rep — measured directly"),
    BRZYCKI("Brzycki (reps 2-12)"),
    EPLEY("Epley (reps 13-20)"),
}

/**
 * A 1RM estimate plus an honest statement of how much to trust it.
 *
 * The web app extrapolated Brzycki to 30 reps and then hard-clamped to 1.5x weight, so
 * a 30-rep set reported **5.16x** the load and a 31-rep set reported **1.5x** — a 3.4x
 * cliff between two adjacent inputs. Aro refuses to extrapolate past
 * [OneRepMaxCalculator.MAX_EXTRAPOLATION_REPS] instead of inventing a number, and
 * marks [isReliable] = false for the Epley range so the UI can caveat it.
 */
data class OneRepMaxEstimate(
    val oneRepMaxKg: Double,
    val method: OneRepMaxMethod,
    val isReliable: Boolean,
    /** Why the estimate is soft, or null when it is exact. */
    val caveat: String? = null,
)

/**
 * Estimated one-rep max and session tonnage.
 *
 * Brzycki: `1RM = weight / (1.0278 - 0.0278 x reps)`, valid to ~12 reps.
 * Epley:   `1RM = weight x (1 + reps / 30)`, valid to ~20 reps.
 */
object OneRepMaxCalculator {

    /** Beyond this, no published formula is defensible, so we return null. */
    const val MAX_EXTRAPOLATION_REPS = 20

    const val BRZYCKI_MAX_REPS = 12
    const val BRZYCKI_DIVISOR_BASE = 1.0278
    const val BRZYCKI_DIVISOR_SLOPE = 0.0278
    const val EPLEY_DIVISOR = 30.0

    /**
     * Returns null — never a fabricated number — when the load or reps are non-positive,
     * or when reps exceed [MAX_EXTRAPOLATION_REPS].
     */
    fun estimate(weightKg: Double, reps: Int): OneRepMaxEstimate? {
        if (weightKg <= 0.0 || reps <= 0) return null
        if (reps > MAX_EXTRAPOLATION_REPS) return null

        if (reps == 1) {
            return OneRepMaxEstimate(weightKg, OneRepMaxMethod.DIRECT_SINGLE, isReliable = true)
        }

        if (reps <= BRZYCKI_MAX_REPS) {
            val divisor = BRZYCKI_DIVISOR_BASE - (BRZYCKI_DIVISOR_SLOPE * reps)
            if (divisor <= 0.0) return null // unreachable for reps <= 12, kept as a guard
            val estimate = weightKg / divisor
            return OneRepMaxEstimate(
                oneRepMaxKg = (estimate * 10).roundToInt() / 10.0,
                method = OneRepMaxMethod.BRZYCKI,
                isReliable = true,
            )
        }

        val estimate = weightKg * (1 + reps / EPLEY_DIVISOR)
        return OneRepMaxEstimate(
            oneRepMaxKg = (estimate * 10).roundToInt() / 10.0,
            method = OneRepMaxMethod.EPLEY,
            isReliable = false,
            caveat = "High-rep set: Epley extrapolation past 12 reps overestimates by roughly 10-20%.",
        )
    }

    /** Best estimate across a session's working sets — the heaviest single set wins. */
    fun bestOf(sets: List<SetEntry>): OneRepMaxEstimate? =
        sets.filter { it.isValid }
            .mapNotNull { estimate(it.weightKg, it.reps) }
            .maxByOrNull { it.oneRepMaxKg }

    /** Session tonnage, the volume metric used by Task 11's logger. */
    fun tonnageKg(sets: List<SetEntry>): Double = sets.filter { it.isValid }.sumOf { it.volumeKg }

    /**
     * Inverse of [estimate]: how many reps of [weightKg] fit inside a known 1RM.
     * Uses the Epley rearrangement `reps = 30 x (1RM/weight - 1)`, floored at 1 and
     * null when the load already exceeds the 1RM.
     */
    fun estimatedReps(oneRepMaxKg: Double, weightKg: Double): Int? {
        if (oneRepMaxKg <= 0.0 || weightKg <= 0.0) return null
        if (weightKg > oneRepMaxKg) return null
        return ((EPLEY_DIVISOR * (oneRepMaxKg / weightKg - 1)).roundToInt()).coerceAtLeast(1)
    }

    /**
     * Standard percentage table for a training UI, expressed as a fraction of 1RM.
     * Covers reps 1-12, the range where the estimate is defensible.
     */
    fun repMaxPercentages(): Map<Int, Double> = (1..BRZYCKI_MAX_REPS).associateWith { reps ->
        when (reps) {
            1 -> 1.00
            2 -> 0.95
            3 -> 0.93
            4 -> 0.91
            5 -> 0.89
            6 -> 0.87
            7 -> 0.85
            8 -> 0.83
            9 -> 0.81
            10 -> 0.79
            else -> 0.77
        }
    }
}

/**
 * Calorie cost of resistance training.
 *
 * Aro uses a volume-based estimate of **0.065 kcal per kg lifted** (the value the web
 * app's code actually used, kept here so both clients agree) rather than the 0.08 its
 * comment claimed. Applied to total tonnage this yields realistic magnitudes: a 5000 kg
 * session burns ~325 kcal.
 *
 * This ignores body weight and duration by construction — a genuine limitation, since a
 * light and a heavy lifter doing the same volume do not expend the same energy. It is
 * labelled as an estimate in the UI rather than presented as MET-derived truth.
 */
const val KCAL_PER_KG_TONNAGE = 0.065

/** Calorie estimate for a logged strength session. */
fun WorkoutLog.estimatedCaloriesBurned(): Int =
    if (isStrength) (totalVolumeKg * KCAL_PER_KG_TONNAGE).roundToInt() else 0