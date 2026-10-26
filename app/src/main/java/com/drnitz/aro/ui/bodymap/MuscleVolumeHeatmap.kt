package com.drnitz.aro.ui.bodymap

import com.drnitz.aro.domain.model.Exercise
import com.drnitz.aro.domain.model.WorkoutLog
import kotlin.math.min

/**
 * Accumulated volume metrics for a specific muscle group over a time window (e.g. trailing 7 days).
 */
data class MuscleVolumeStat(
    val muscle: Muscle,
    val sets: Double = 0.0,
    val volumeKg: Double = 0.0,
    val intensity: Float = 0f,
    val workoutCount: Int = 0,
)

/**
 * Maps logged workouts to muscle group volume landmarks and heatmap intensities.
 */
object MuscleVolumeCalculator {

    /**
     * Calculates weekly volume and intensity for each muscle group based on logged workouts.
     *
     * @param workouts List of workouts (typically spanning the trailing 7 days).
     * @param exerciseLookup Optional lookup to resolve muscle metadata if not present directly on [WorkoutLog].
     */
    fun calculateMuscleVolume(
        workouts: List<WorkoutLog>,
        exerciseLookup: ((String) -> Exercise?)? = null,
    ): Map<Muscle, MuscleVolumeStat> {
        val setMap = mutableMapOf<Muscle, Double>()
        val volumeMap = mutableMapOf<Muscle, Double>()
        val countMap = mutableMapOf<Muscle, Int>()

        for (workout in workouts) {
            val primaryMuscles = mutableSetOf<Muscle>()
            val secondaryMuscles = mutableSetOf<Muscle>()

            // 1. Direct muscle string from workout log
            if (!workout.muscle.isNullOrBlank()) {
                workout.muscle.split(",", ";").forEach { mStr ->
                    Muscle.fromExerciseMuscle(mStr)?.let { primaryMuscles.add(it) }
                }
            }

            // 2. Lookup in exercise library if needed
            if (primaryMuscles.isEmpty() && exerciseLookup != null) {
                val exercise = exerciseLookup(workout.exerciseName)
                if (exercise != null) {
                    exercise.primaryMuscles.forEach { mStr ->
                        Muscle.fromExerciseMuscle(mStr)?.let { primaryMuscles.add(it) }
                    }
                    exercise.secondaryMuscles.forEach { mStr ->
                        Muscle.fromExerciseMuscle(mStr)?.let { secondaryMuscles.add(it) }
                    }
                }
            }

            // 3. Fallback name heuristics if still empty
            if (primaryMuscles.isEmpty()) {
                val lowerName = workout.exerciseName.lowercase()
                when {
                    lowerName.contains("bench") || lowerName.contains("chest") || lowerName.contains("push up") || lowerName.contains("push-up") -> primaryMuscles.add(Muscle.Chest)
                    lowerName.contains("squat") || lowerName.contains("leg press") || lowerName.contains("lunge") -> primaryMuscles.add(Muscle.Quadriceps)
                    lowerName.contains("deadlift") -> {
                        primaryMuscles.add(Muscle.Hamstring)
                        primaryMuscles.add(Muscle.LowerBack)
                    }
                    lowerName.contains("pull up") || lowerName.contains("pull-up") || lowerName.contains("lat") || lowerName.contains("row") -> primaryMuscles.add(Muscle.UpperBack)
                    lowerName.contains("shoulder") || lowerName.contains("overhead") || lowerName.contains("military") || lowerName.contains("lateral raise") -> primaryMuscles.add(Muscle.Deltoids)
                    lowerName.contains("curl") || lowerName.contains("bicep") -> primaryMuscles.add(Muscle.Biceps)
                    lowerName.contains("tricep") || lowerName.contains("dip") || lowerName.contains("skull crusher") -> primaryMuscles.add(Muscle.Triceps)
                    lowerName.contains("crunch") || lowerName.contains("plank") || lowerName.contains("ab ") -> primaryMuscles.add(Muscle.Abs)
                    lowerName.contains("calf") || lowerName.contains("calves") -> primaryMuscles.add(Muscle.Calves)
                }
            }

            // Resistance set count: working sets or sets size (fallback 3)
            val setCount = when {
                workout.workingSets.isNotEmpty() -> workout.workingSets.size.toDouble()
                workout.sets.isNotEmpty() -> workout.sets.size.toDouble()
                workout.isStrength -> 3.0
                workout.isCardio -> 2.0 // Light 2-set activation for aerobic sessions
                else -> 1.0
            }

            val volumeKg = if (workout.isStrength) workout.totalVolumeKg else 0.0

            // If cardio, attribute light activation to legs if no muscles found
            if (workout.isCardio && primaryMuscles.isEmpty()) {
                primaryMuscles.add(Muscle.Quadriceps)
                primaryMuscles.add(Muscle.Calves)
            }

            primaryMuscles.forEach { muscle ->
                setMap[muscle] = (setMap[muscle] ?: 0.0) + setCount
                volumeMap[muscle] = (volumeMap[muscle] ?: 0.0) + volumeKg
                countMap[muscle] = (countMap[muscle] ?: 0) + 1
            }

            secondaryMuscles.forEach { muscle ->
                if (!primaryMuscles.contains(muscle)) {
                    setMap[muscle] = (setMap[muscle] ?: 0.0) + (setCount * 0.5)
                    volumeMap[muscle] = (volumeMap[muscle] ?: 0.0) + (volumeKg * 0.5)
                    countMap[muscle] = (countMap[muscle] ?: 0) + 1
                }
            }
        }

        // Compute intensity for all muscles
        return Muscle.entries.filter { !it.isCosmeticPart }.associateWith { muscle ->
            val directSets = setMap[muscle] ?: 0.0
            val directVol = volumeMap[muscle] ?: 0.0
            val directCount = countMap[muscle] ?: 0

            val intensity = setsToIntensity(directSets)
            MuscleVolumeStat(
                muscle = muscle,
                sets = directSets,
                volumeKg = directVol,
                intensity = intensity,
                workoutCount = directCount,
            )
        }
    }

    /**
     * Converts a set count into a 0.0 to 1.0 heatmap intensity.
     *
     * Based on Renaissance Periodization volume landmarks:
     * - 0 sets: 0.0 (rested)
     * - 1-4 sets: 0.15 - 0.35 (light stimulus)
     * - 5-10 sets: 0.35 - 0.60 (maintenance / moderate)
     * - 11-18 sets: 0.60 - 0.85 (optimal adaptive volume)
     * - 19+ sets: 0.85 - 1.00 (maximum recoverable volume)
     */
    fun setsToIntensity(sets: Double): Float {
        if (sets <= 0.0) return 0f
        return when {
            sets <= 4.0 -> (0.15f + (sets / 4.0) * 0.20f).toFloat()
            sets <= 10.0 -> (0.35f + ((sets - 4.0) / 6.0) * 0.25f).toFloat()
            sets <= 18.0 -> (0.60f + ((sets - 10.0) / 8.0) * 0.25f).toFloat()
            else -> (0.85f + min(1.0, (sets - 18.0) / 7.0) * 0.15f).toFloat().coerceIn(0f, 1f)
        }
    }

    /**
     * Generates a list of [MuscleIntensity] suitable for [heatmapHighlights].
     */
    fun toMuscleIntensities(volumeStats: Map<Muscle, MuscleVolumeStat>): List<MuscleIntensity> =
        volumeStats.values
            .filter { it.intensity > 0f }
            .map { stat -> MuscleIntensity(muscle = stat.muscle, intensity = stat.intensity) }
}
