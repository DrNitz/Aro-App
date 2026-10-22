package com.drnitz.aro.domain.clinical

import com.drnitz.aro.domain.Fixtures
import com.drnitz.aro.domain.assertClose
import com.drnitz.aro.domain.model.MacroTargets
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissingMacroAuditorTest {

    private val targets = MacroTargets(calories = 2200, proteinG = 160, carbsG = 220, fatG = 70, fiberG = 31)

    @Test
    fun `an empty day reports nothing logged rather than zero intakes`() {
        val coverage = MissingMacroAuditor.audit(Fixtures.day(), targets)
        assertTrue(coverage.hasNothingLogged)
        assertEquals(0, coverage.totalEntries)
        for (macro in coverage.macros) {
            assertEquals(CoverageStatus.NO_LOG_ENTRIES, macro.status)
            assertTrue(macro.isMissing)
            assertEquals(null, macro.actual)
        }
        assertEquals(MealSlot.ORDERED, coverage.emptyMeals)
    }

    @Test
    fun `a day with only breakfast flags the other three meals as empty`() {
        val coverage = MissingMacroAuditor.audit(Fixtures.day(Fixtures.oatmeal()), targets)
        assertEquals(listOf(MealSlot.LUNCH, MealSlot.DINNER, MealSlot.SNACKS), coverage.emptyMeals)
        assertEquals(1, coverage.totalEntries)
    }

    @Test
    fun `a macro logged at exactly zero is absent not merely low`() {
        // Chicken breast reports 0 g carbohydrate: that is a real absence.
        val coverage = MissingMacroAuditor.auditMeal(MealSlot.LUNCH, listOf(Fixtures.chickenBreast()))
        assertTrue(coverage.missingMacros.contains(NutrientKey.CARBS))
        assertFalse(coverage.missingMacros.contains(NutrientKey.PROTEIN))
    }

    @Test
    fun `a macro the source never reported is unreported not absent`() {
        // Chicken breast carries no potassium entry at all.
        val coverage = MissingMacroAuditor.auditMeal(MealSlot.LUNCH, listOf(Fixtures.chickenBreast()))
        assertTrue(coverage.unreportedMacros.contains(NutrientKey.FAT) == false)
        assertEquals(1, coverage.entryCount)
        assertFalse(coverage.isEmpty)
    }

    @Test
    fun `meal coverage counts entries rather than distinct nutrients`() {
        val day = Fixtures.day(Fixtures.oatmeal(), Fixtures.butter())
        val coverage = MissingMacroAuditor.audit(day, targets).meals.first { it.meal == MealSlot.BREAKFAST }
        assertEquals(2, coverage.entryCount)
        assertClose(214.4, coverage.totals.calories)
    }

    @Test
    fun `macros below target report their fraction of target`() {
        // 150 g chicken = 69.75 g protein against a 160 g target
        val coverage = MissingMacroAuditor.audit(Fixtures.day(Fixtures.chickenBreast()), targets)
        val protein = coverage.macros.first { it.key == NutrientKey.PROTEIN }
        assertEquals(CoverageStatus.BELOW_TARGET, protein.status)
        assertClose(0.436, protein.fractionOfTarget, 0.01)
    }

    @Test
    fun `a day of only fruit reports protein as absent`() {
        val apple = com.drnitz.aro.domain.model.FoodLogEntry(
            id = "f-apple",
            name = "Apple",
            meal = MealSlot.SNACKS,
            grams = 180.0,
            nutrients = NutrientTotals.of(
                NutrientKey.CALORIES to 95.0,
                NutrientKey.PROTEIN to 0.5,
                NutrientKey.CARBS to 25.0,
                NutrientKey.FAT to 0.3,
                NutrientKey.FIBER to 4.4,
            ),
        )
        val coverage = MissingMacroAuditor.audit(Fixtures.day(apple), targets)
        assertEquals(
            CoverageStatus.ABSENT,
            coverage.macros.first { it.key == NutrientKey.PROTEIN }.status,
        )
    }

    @Test
    fun `a day over target on protein reads as above target`() {
        val big = Fixtures.chickenBreast(grams = 600.0)
        val coverage = MissingMacroAuditor.audit(Fixtures.day(big), targets)
        val protein = coverage.macros.first { it.key == NutrientKey.PROTEIN }
        assertEquals(CoverageStatus.ABOVE_TARGET, protein.status)
    }

    @Test
    fun `fiber is audited against the target from the plan`() {
        val coverage = MissingMacroAuditor.audit(Fixtures.day(Fixtures.oatmeal()), targets)
        assertEquals(NutrientKey.FIBER, coverage.fiber.key)
        assertEquals(31.0, coverage.fiber.target!!, 0.001)
        assertEquals(CoverageStatus.BELOW_TARGET, coverage.fiber.status)
    }

    @Test
    fun `a macro logged below one gram counts as no source`() {
        // Butter reports 0.012 g of carbs: technically reported, but not a carb source.
        val coverage = MissingMacroAuditor.auditMeal(MealSlot.BREAKFAST, listOf(Fixtures.butter()))
        assertTrue(coverage.missingMacros.contains(NutrientKey.CARBS))
        assertFalse(coverage.missingMacros.contains(NutrientKey.FAT))
        assertEquals(1.0, MissingMacroAuditor.MEANINGFUL_SOURCE_THRESHOLD_G, 0.0001)
    }

    @Test
    fun `meals missing macros are listed for the diary alerts`() {
        // Butter is 81% fat with no meaningful carbs, so breakfast is flagged.
        val day = Fixtures.day(Fixtures.butter(), Fixtures.chickenBreast())
        val coverage = MissingMacroAuditor.audit(day, targets)
        assertTrue(coverage.mealsMissingMacros.isNotEmpty())
        val breakfast = coverage.mealsMissingMacros.first { it.meal == MealSlot.BREAKFAST }
        assertTrue(breakfast.missingMacros.contains(NutrientKey.CARBS))
    }

    @Test
    fun `a full day of protein food has no missing macros`() {
        // Every meal here carries a protein source: oatmeal 2.5 g, chicken 46.5 g,
        // chicken again, and butter 0.85 g per 20 g — all above the 1 g threshold.
        val day = Fixtures.day(
            Fixtures.oatmeal(),
            Fixtures.chickenBreast(),
            Fixtures.chickenBreast(),
            Fixtures.butter(),
        )
        val coverage = MissingMacroAuditor.audit(day, targets)
        assertTrue(coverage.mealsMissingMacros.none { NutrientKey.PROTEIN in it.missingMacros })
        assertFalse(coverage.hasNothingLogged)
        assertEquals(4, coverage.totalEntries)
    }

    @Test
    fun `a nutrient only some foods report is flagged estimated not clean`() {
        // Oatmeal reports potassium, chicken does not: the day total is an under-count
        // and the domain says so instead of presenting a clean number.
        val coverage = MissingMacroAuditor.audit(
            Fixtures.day(Fixtures.oatmeal(), Fixtures.chickenBreast()),
            targets,
        )
        assertTrue(coverage.totalTotals[NutrientKey.POTASSIUM].isEstimated)
        assertFalse(coverage.totalTotals[NutrientKey.PROTEIN].isEstimated)
    }

    @Test
    fun `unreported nutrients are surfaced so aro knows its own blind spot`() {
        val blind = com.drnitz.aro.domain.model.FoodLogEntry(
            id = "f-blank",
            name = "Mystery snack",
            meal = MealSlot.SNACKS,
            grams = 50.0,
            nutrients = NutrientTotals.of(
                NutrientKey.CALORIES to 100.0,
                NutrientKey.PROTEIN to 2.0,
                NutrientKey.CARBS to 20.0,
                NutrientKey.FAT to 1.0,
            ),
        )
        val coverage = MissingMacroAuditor.audit(Fixtures.day(blind), targets)
        // Every logged food here lacks fiber, so fiber is the blind spot.
        assertFalse(coverage.totalTotals[NutrientKey.FIBER].isReported)
        assertTrue(coverage.unreportedKeys.contains(NutrientKey.FIBER))
        assertEquals(1, coverage.meals.first { it.meal == MealSlot.SNACKS }.entryCount)
    }

    @Test
    fun `macro targets map only macro keys to gram goals`() {
        assertEquals(160.0, targets.targetFor(NutrientKey.PROTEIN)!!, 0.001)
        assertEquals(220.0, targets.targetFor(NutrientKey.CARBS)!!, 0.001)
        assertEquals(70.0, targets.targetFor(NutrientKey.FAT)!!, 0.001)
        assertEquals(31.0, targets.targetFor(NutrientKey.FIBER)!!, 0.001)
        assertEquals(2200.0, targets.targetFor(NutrientKey.CALORIES)!!, 0.001)
        assertEquals(null, targets.targetFor(NutrientKey.SODIUM))
    }

    @Test
    fun `zero targets do not divide by zero`() {
        val emptyTargets = MacroTargets.EMPTY
        val coverage = MissingMacroAuditor.audit(Fixtures.day(Fixtures.oatmeal()), emptyTargets)
        assertEquals(0.0, coverage.macros.first().fractionOfTarget, 0.0001)
    }

    @Test
    fun `coverage labels are stable for the UI`() {
        assertEquals("Not logged at all", CoverageStatus.ABSENT.label)
        assertEquals("Not tracked by source", CoverageStatus.UNREPORTED.label)
        assertEquals("Nothing logged yet", CoverageStatus.NO_LOG_ENTRIES.label)
    }
}