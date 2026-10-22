package com.drnitz.aro.domain.calculation

import com.drnitz.aro.domain.Fixtures
import com.drnitz.aro.domain.assertClose
import com.drnitz.aro.domain.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetTest {

    // ---- Formula ----------------------------------------------------------

    @Test
    fun `8 MET at 80kg for 45 minutes burns 480 kcal`() {
        assertEquals(480, caloriesBurned(met = 8.0, bodyWeightKg = 80.0, durationMinutes = 45))
    }

    @Test
    fun `one MET at one kilogram for one hour is one kcal`() {
        assertEquals(1, caloriesBurned(met = 1.0, bodyWeightKg = 1.0, durationMinutes = 60))
    }

    @Test
    fun `burn scales linearly with body weight and duration`() {
        val base = caloriesBurned(8.0, 80.0, 60)
        assertEquals(base * 2, caloriesBurned(8.0, 160.0, 60))
        assertEquals(base / 2, caloriesBurned(8.0, 80.0, 30))
    }

    @Test
    fun `missing or impossible inputs degrade to zero instead of crashing`() {
        assertEquals(0, caloriesBurned(0.0, 80.0, 45))
        assertEquals(0, caloriesBurned(8.0, 0.0, 45))
        assertEquals(0, caloriesBurned(8.0, 80.0, 0))
        assertEquals(0, caloriesBurned(-8.0, 80.0, 45))
    }

    // ---- Catalog ----------------------------------------------------------

    @Test
    fun `catalog ships 26 activities with unique ids`() {
        assertEquals(26, MetCatalog.all.size)
        assertEquals(MetCatalog.all.size, MetCatalog.all.map { it.id }.distinct().size)
    }

    @Test
    fun `every activity has a plausible MET and a category`() {
        for (activity in MetCatalog.all) {
            assertTrue("${activity.id} MET ${activity.met}", activity.met in 2.0..12.0)
            assertTrue(activity.category.isNotBlank())
            assertTrue(activity.name.isNotBlank())
        }
    }

    @Test
    fun `categories are distinct and browsable`() {
        assertEquals(MetCatalog.all.map { it.category }.distinct().size, MetCatalog.categories.size)
        assertTrue(MetCatalog.categories.contains("Running"))
        assertEquals(3, MetCatalog.inCategory("Running").size)
        assertEquals(0, MetCatalog.inCategory("Quidditch").size)
    }

    @Test
    fun `lookup by id returns null for an unknown activity`() {
        assertNotNull(MetCatalog.byId("running-6mph"))
        assertNull(MetCatalog.byId("levitating"))
    }

    @Test
    fun `catalog preserves the web app's compendium values`() {
        assertClose(9.8, MetCatalog.byId("running-6mph")!!.met)
        assertClose(11.8, MetCatalog.byId("jump-rope")!!.met)
        assertClose(3.8, MetCatalog.byId("weight-training-moderate")!!.met)
        assertClose(2.5, MetCatalog.byId("yoga-hatha")!!.met)
    }

    // ---- Workout logs -----------------------------------------------------

    @Test
    fun `cardio log burns against the weight snapshotted at log time`() {
        assertEquals(480, Fixtures.cardio(met = 8.0, minutes = 45, bodyWeightKg = 80.0).caloriesBurned)
    }

    @Test
    fun `cardio log without a weight snapshot burns nothing rather than guessing`() {
        assertEquals(0, Fixtures.cardio(bodyWeightKg = null).caloriesBurned)
    }

    @Test
    fun `strength log falls back to volume based burn`() {
        val log = Fixtures.strength(100.0 to 5, 100.0 to 5, 100.0 to 5)
        assertEquals(WorkoutType.STRENGTH, log.type)
        assertEquals(98, log.caloriesBurned)
    }

    @Test
    fun `an empty day burns nothing`() {
        assertEquals(0, Fixtures.day().caloriesBurned())
    }

    @Test
    fun `day burn sums every workout`() {
        val day = Fixtures.day(Fixtures.oatmeal())
            .copy(workouts = listOf(Fixtures.cardio(), Fixtures.strength(100.0 to 5, 100.0 to 5, 100.0 to 5)))
        assertEquals(480 + 98, day.caloriesBurned())
    }

    @Test
    fun `day burn uses the fallback weight for legacy logs without a snapshot`() {
        val day = Fixtures.day().copy(workouts = listOf(Fixtures.cardio(bodyWeightKg = null)))
        assertEquals(0, day.caloriesBurned())
        assertEquals(480, day.caloriesBurned(fallbackBodyWeightKg = 80.0))
    }

    // ---- Diary level ------------------------------------------------------

    @Test
    fun `day energy balance subtracts burned calories from the target`() {
        // 100 g oatmeal = 71 kcal, 150 g chicken breast = 372 kcal
        val day = Fixtures.day(Fixtures.oatmeal(), Fixtures.chickenBreast())
            .copy(workouts = listOf(Fixtures.cardio()))
        val balance = day.energyBalance(targetCalories = 2200)
        assertEquals(443, balance.caloriesEaten)
        assertEquals(480, balance.caloriesBurned)
        assertEquals(2200 - 443 + 480, balance.remaining)
    }

    @Test
    fun `fasted day with a workout leaves the target plus the burn remaining`() {
        val day = Fixtures.day().copy(workouts = listOf(Fixtures.cardio()))
        assertEquals(2200 + 480, day.energyBalance(2200).remaining)
    }

    @Test
    fun `water progress is guarded against a zero goal`() {
        val day = Fixtures.day().copy(waterMl = 750)
        assertClose(0.25, day.waterProgress(3000))
        assertEquals(0.0, day.waterProgress(0), 0.0001)
    }
}