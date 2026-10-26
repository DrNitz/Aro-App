package com.drnitz.aro.ui.bodymap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import com.drnitz.aro.domain.model.Exercise
import com.drnitz.aro.domain.model.SetEntry
import com.drnitz.aro.domain.model.WorkoutLog
import com.drnitz.aro.domain.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class BodyMapTest {

    @Test
    fun `muscle enum maps to Aro exercise filter strings accurately`() {
        assertEquals(listOf("chest"), Muscle.Chest.toExerciseFilterMuscles())
        assertEquals(listOf("chest"), Muscle.UpperChest.toExerciseFilterMuscles())
        assertEquals(listOf("chest"), Muscle.LowerChest.toExerciseFilterMuscles())

        assertEquals(listOf("abdominals"), Muscle.Abs.toExerciseFilterMuscles())
        assertEquals(listOf("abdominals"), Muscle.UpperAbs.toExerciseFilterMuscles())
        assertEquals(listOf("abdominals"), Muscle.Obliques.toExerciseFilterMuscles())

        assertEquals(listOf("biceps"), Muscle.Biceps.toExerciseFilterMuscles())
        assertEquals(listOf("triceps"), Muscle.Triceps.toExerciseFilterMuscles())
        assertEquals(listOf("forearms"), Muscle.Forearm.toExerciseFilterMuscles())

        assertTrue(Muscle.Deltoids.toExerciseFilterMuscles().contains("shoulders"))
        assertTrue(Muscle.FrontDeltoid.toExerciseFilterMuscles().contains("shoulders"))
        assertTrue(Muscle.RotatorCuff.toExerciseFilterMuscles().contains("supraspinatus"))

        assertEquals(listOf("quadriceps"), Muscle.Quadriceps.toExerciseFilterMuscles())
        assertEquals(listOf("hamstrings"), Muscle.Hamstring.toExerciseFilterMuscles())
        assertTrue(Muscle.Gluteal.toExerciseFilterMuscles().contains("glutes"))
        assertEquals(listOf("calves"), Muscle.Calves.toExerciseFilterMuscles())

        assertTrue(Muscle.UpperBack.toExerciseFilterMuscles().contains("lats"))
        assertEquals(listOf("lower back"), Muscle.LowerBack.toExerciseFilterMuscles())
        assertEquals(listOf("traps"), Muscle.Trapezius.toExerciseFilterMuscles())
        assertEquals(listOf("adductors"), Muscle.Adductors.toExerciseFilterMuscles())
        assertEquals(listOf("neck"), Muscle.Neck.toExerciseFilterMuscles())
    }

    @Test
    fun `fromExerciseMuscle parses all canonical exercise library muscle names`() {
        assertEquals(Muscle.Chest, Muscle.fromExerciseMuscle("chest"))
        assertEquals(Muscle.Abs, Muscle.fromExerciseMuscle("abdominals"))
        assertEquals(Muscle.Abs, Muscle.fromExerciseMuscle("abs"))
        assertEquals(Muscle.Biceps, Muscle.fromExerciseMuscle("biceps"))
        assertEquals(Muscle.Triceps, Muscle.fromExerciseMuscle("triceps"))
        assertEquals(Muscle.Forearm, Muscle.fromExerciseMuscle("forearms"))
        assertEquals(Muscle.Deltoids, Muscle.fromExerciseMuscle("shoulders"))
        assertEquals(Muscle.RotatorCuff, Muscle.fromExerciseMuscle("supraspinatus"))
        assertEquals(Muscle.Quadriceps, Muscle.fromExerciseMuscle("quadriceps"))
        assertEquals(Muscle.Hamstring, Muscle.fromExerciseMuscle("hamstrings"))
        assertEquals(Muscle.Gluteal, Muscle.fromExerciseMuscle("glutes"))
        assertEquals(Muscle.Gluteal, Muscle.fromExerciseMuscle("abductors"))
        assertEquals(Muscle.Calves, Muscle.fromExerciseMuscle("calves"))
        assertEquals(Muscle.UpperBack, Muscle.fromExerciseMuscle("lats"))
        assertEquals(Muscle.UpperBack, Muscle.fromExerciseMuscle("middle back"))
        assertEquals(Muscle.LowerBack, Muscle.fromExerciseMuscle("lower back"))
        assertEquals(Muscle.Trapezius, Muscle.fromExerciseMuscle("traps"))
        assertEquals(Muscle.Adductors, Muscle.fromExerciseMuscle("adductors"))
        assertEquals(Muscle.Neck, Muscle.fromExerciseMuscle("neck"))
        assertEquals(Muscle.Serratus, Muscle.fromExerciseMuscle("serratus anterior"))

        assertNull(Muscle.fromExerciseMuscle("unknown_nonexistent_muscle"))
    }

    @Test
    fun `muscle hierarchy and parent group logic is consistent`() {
        assertEquals(Muscle.Chest, Muscle.UpperChest.parentGroup)
        assertEquals(Muscle.Chest, Muscle.LowerChest.parentGroup)
        assertEquals(Muscle.Quadriceps, Muscle.InnerQuad.parentGroup)
        assertEquals(Muscle.Abs, Muscle.UpperAbs.parentGroup)
        assertEquals(Muscle.Trapezius, Muscle.UpperTrapezius.parentGroup)

        assertTrue(Muscle.UpperChest.isSubGroup)
        assertFalse(Muscle.Chest.isSubGroup)

        assertTrue(Muscle.Neck.isAlwaysVisibleSubGroup)
        assertTrue(Muscle.Adductors.isAlwaysVisibleSubGroup)
        assertTrue(Muscle.Ankles.isAlwaysVisibleSubGroup)
        assertFalse(Muscle.UpperAbs.isAlwaysVisibleSubGroup)
    }

    @Test
    fun `body path datasets are loaded and populated for both genders and sides`() {
        val maleFront = BodyPathProvider.paths(BodyGender.Male, BodySide.Front)
        val maleBack = BodyPathProvider.paths(BodyGender.Male, BodySide.Back)
        val femaleFront = BodyPathProvider.paths(BodyGender.Female, BodySide.Front)
        val femaleBack = BodyPathProvider.paths(BodyGender.Female, BodySide.Back)

        assertTrue("Male Front parts must be non-empty", maleFront.isNotEmpty())
        assertTrue("Male Back parts must be non-empty", maleBack.isNotEmpty())
        assertTrue("Female Front parts must be non-empty", femaleFront.isNotEmpty())
        assertTrue("Female Back parts must be non-empty", femaleBack.isNotEmpty())

        assertTrue(maleFront.any { it.slug == BodySlug.Chest })
        assertTrue(maleFront.any { it.slug == BodySlug.Abs })
        assertTrue(maleBack.any { it.slug == BodySlug.UpperBack })
        assertTrue(maleBack.any { it.slug == BodySlug.Gluteal })

        val mfVb = BodyPathProvider.viewBox(BodyGender.Male, BodySide.Front)
        val mbVb = BodyPathProvider.viewBox(BodyGender.Male, BodySide.Back)
        assertEquals(727f, mfVb.width)
        assertEquals(1280f, mfVb.height)
        assertEquals(718f, mbVb.originX)
    }

    @Test
    fun `heatmap highlights color interpolation and thresholding calculate correctly`() {
        val scale = HeatmapColorScale.Workout

        // Baseline (0%) should match the first color (muted gray 0xFFC7C7C7)
        val lowColor = scale.colorFor(0f)
        assertEquals(Color(0xFFC7C7C7), lowColor)

        // Max (100%) should match the last color (red 0xFFFF3B30)
        val highColor = scale.colorFor(1f)
        assertEquals(Color(0xFFFF3B30), highColor)

        val intensities = listOf(
            MuscleIntensity(Muscle.Chest, 0.9f),
            MuscleIntensity(Muscle.Biceps, 0.4f),
            MuscleIntensity(Muscle.Calves, 0.1f),
        )

        val highlightsAll = heatmapHighlights(intensities, scale)
        assertEquals(3, highlightsAll.size)
        assertNotNull(highlightsAll[Muscle.Chest])
        assertNotNull(highlightsAll[Muscle.Biceps])
        assertNotNull(highlightsAll[Muscle.Calves])

        // With threshold of 0.3f, Calves (0.1f) must be excluded
        val filtered = heatmapHighlights(intensities, scale, threshold = 0.3f)
        assertEquals(2, filtered.size)
        assertTrue(filtered.containsKey(Muscle.Chest))
        assertTrue(filtered.containsKey(Muscle.Biceps))
        assertFalse(filtered.containsKey(Muscle.Calves))
    }

    @Test
    fun `color interpolation curves apply properly`() {
        assertEquals(0.5f, ColorInterpolation.Linear.apply(0.5f), 0.001f)
        assertEquals(0.25f, ColorInterpolation.EaseIn.apply(0.5f), 0.001f)
        assertEquals(0.75f, ColorInterpolation.EaseOut.apply(0.5f), 0.001f)

        val step5 = ColorInterpolation.Step(5)
        assertEquals(0.4f, step5.apply(0.45f), 0.001f)
    }

    @Test
    fun `setsToIntensity maps volume landmarks correctly based on Renaissance Periodization standards`() {
        assertEquals(0f, MuscleVolumeCalculator.setsToIntensity(0.0), 0.001f)
        assertEquals(0.25f, MuscleVolumeCalculator.setsToIntensity(2.0), 0.01f)
        assertEquals(0.35f, MuscleVolumeCalculator.setsToIntensity(4.0), 0.001f)
        assertEquals(0.60f, MuscleVolumeCalculator.setsToIntensity(10.0), 0.001f)
        assertEquals(0.85f, MuscleVolumeCalculator.setsToIntensity(18.0), 0.001f)
        assertEquals(1.0f, MuscleVolumeCalculator.setsToIntensity(25.0), 0.001f)
        assertEquals(1.0f, MuscleVolumeCalculator.setsToIntensity(35.0), 0.001f)
    }

    @Test
    fun `calculateMuscleVolume accurately maps workouts with explicit muscle tags, working sets, and total volume`() {
        val workouts = listOf(
            WorkoutLog(
                id = "w1",
                type = WorkoutType.STRENGTH,
                exerciseName = "Barbell Flat Bench Press",
                loggedAt = Instant.now(),
                met = 6.0,
                durationMinutes = 45,
                muscle = "chest",
                sets = listOf(
                    SetEntry(weightKg = 100.0, reps = 10),
                    SetEntry(weightKg = 100.0, reps = 10),
                    SetEntry(weightKg = 100.0, reps = 10),
                ),
            ),
        )

        val volumeStats = MuscleVolumeCalculator.calculateMuscleVolume(workouts)
        val chestStat = volumeStats[Muscle.Chest]

        assertNotNull(chestStat)
        assertEquals(3.0, chestStat!!.sets, 0.001)
        assertEquals(3000.0, chestStat.volumeKg, 0.001)
        assertEquals(1, chestStat.workoutCount)
        assertTrue("Chest intensity should be light stimulus (>0.25)", chestStat.intensity >= 0.25f)
    }

    @Test
    fun `calculateMuscleVolume handles secondary muscles with half set volume`() {
        val workouts = listOf(
            WorkoutLog(
                id = "w2",
                type = WorkoutType.STRENGTH,
                exerciseName = "Close Grip Bench Press",
                loggedAt = Instant.now(),
                met = 6.0,
                durationMinutes = 35,
                sets = listOf(
                    SetEntry(weightKg = 80.0, reps = 10),
                    SetEntry(weightKg = 80.0, reps = 10),
                    SetEntry(weightKg = 80.0, reps = 10),
                    SetEntry(weightKg = 80.0, reps = 10),
                ),
            ),
        )

        val exercises = mapOf(
            "Close Grip Bench Press" to Exercise(
                id = "ex-cgbp",
                name = "Close Grip Bench Press",
                category = "strength",
                primaryMuscles = listOf("triceps"),
                secondaryMuscles = listOf("chest"),
            ),
        )

        val volumeStats = MuscleVolumeCalculator.calculateMuscleVolume(
            workouts = workouts,
            exerciseLookup = { name -> exercises[name] },
        )

        val tricepsStat = volumeStats[Muscle.Triceps]
        val chestStat = volumeStats[Muscle.Chest]

        assertNotNull(tricepsStat)
        assertNotNull(chestStat)
        assertEquals(4.0, tricepsStat!!.sets, 0.001)
        assertEquals(3200.0, tricepsStat.volumeKg, 0.001)

        assertEquals(2.0, chestStat!!.sets, 0.001)
        assertEquals(1600.0, chestStat.volumeKg, 0.001)
    }

    @Test
    fun `calculateMuscleVolume falls back to exercise name heuristics when muscle tag is missing`() {
        val workouts = listOf(
            WorkoutLog(
                id = "w3",
                type = WorkoutType.STRENGTH,
                exerciseName = "Barbell Squat",
                loggedAt = Instant.now(),
                met = 6.0,
                durationMinutes = 40,
                sets = listOf(
                    SetEntry(weightKg = 120.0, reps = 8),
                    SetEntry(weightKg = 120.0, reps = 8),
                ),
            ),
            WorkoutLog(
                id = "w4",
                type = WorkoutType.STRENGTH,
                exerciseName = "Dumbbell Bicep Curl",
                loggedAt = Instant.now(),
                met = 4.0,
                durationMinutes = 20,
                sets = listOf(
                    SetEntry(weightKg = 15.0, reps = 12),
                    SetEntry(weightKg = 15.0, reps = 12),
                    SetEntry(weightKg = 15.0, reps = 12),
                ),
            ),
        )

        val volumeStats = MuscleVolumeCalculator.calculateMuscleVolume(workouts)

        val quadsStat = volumeStats[Muscle.Quadriceps]
        val bicepsStat = volumeStats[Muscle.Biceps]

        assertNotNull(quadsStat)
        assertEquals(2.0, quadsStat!!.sets, 0.001)

        assertNotNull(bicepsStat)
        assertEquals(3.0, bicepsStat!!.sets, 0.001)
    }

    @Test
    fun `toMuscleIntensities filters out muscles with zero intensity`() {
        val workouts = listOf(
            WorkoutLog(
                id = "w5",
                type = WorkoutType.STRENGTH,
                exerciseName = "Pull Up",
                loggedAt = Instant.now(),
                met = 5.0,
                durationMinutes = 20,
                muscle = "lats",
                sets = listOf(
                    SetEntry(weightKg = 75.0, reps = 8),
                ),
            ),
        )

        val volumeStats = MuscleVolumeCalculator.calculateMuscleVolume(workouts)
        val activeIntensities = MuscleVolumeCalculator.toMuscleIntensities(volumeStats)

        assertEquals(1, activeIntensities.size)
        assertEquals(Muscle.UpperBack, activeIntensities[0].muscle)
        assertTrue(activeIntensities[0].intensity > 0f)
    }

    @Test
    fun `zoom and pan coordinate inversion calculates exact unzoomed canvas points`() {
        val canvasSize = Size(360f, 640f)
        val center = canvasSize.center

        // Case 1: scale 1.0f, no pan. Screen point equals unzoomed point.
        val tap1 = Offset(180f, 320f)
        val unzoomed1 = center + (tap1 - Offset.Zero - center) / 1.0f
        assertEquals(180f, unzoomed1.x, 0.001f)
        assertEquals(320f, unzoomed1.y, 0.001f)

        // Case 2: scale 2.0f, pivot center, pan (20, -40).
        // A point originally at (230, 320) is 50 units right of center.
        // After 2x scale: 180 + 50 * 2 = 280.
        // After pan (+20, -40): screen tap is at (300, 280).
        val tap2 = Offset(300f, 280f)
        val userScale = 2.0f
        val userPan = Offset(20f, -40f)

        val unzoomed2 = center + (tap2 - userPan - center) / userScale
        assertEquals(230f, unzoomed2.x, 0.001f)
        assertEquals(320f, unzoomed2.y, 0.001f)
    }

    @Test
    fun `BodyGeometry of viewBox fits within canvas size preserves aspect ratio`() {
        val viewBox = BodyViewBox(originX = 0f, originY = 0f, width = 727f, height = 1280f)
        val canvasSize = Size(360f, 720f)
        val geo = BodyGeometry.of(viewBox, canvasSize)

        // Aspect ratio: 360 / 727 ≈ 0.495, 720 / 1280 = 0.5625 -> scale should be width-limited
        assertEquals(360f / 727f, geo.scale, 0.001f)
        assertEquals(0f, geo.offsetX, 0.001f)
        assertTrue("offsetY should center figure vertically", geo.offsetY > 0f)
    }
}
