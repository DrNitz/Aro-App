package com.drnitz.aro.domain.calculation

import com.drnitz.aro.domain.Fixtures
import com.drnitz.aro.domain.assertClose
import com.drnitz.aro.domain.model.SetEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthTest {

    // ---- One rep max ------------------------------------------------------

    @Test
    fun `a single rep is measured, not estimated`() {
        val estimate = OneRepMaxCalculator.estimate(100.0, 1)!!
        assertEquals(100.0, estimate.oneRepMaxKg, 0.001)
        assertEquals(OneRepMaxMethod.DIRECT_SINGLE, estimate.method)
        assertTrue(estimate.isReliable)
        assertNull(estimate.caveat)
    }

    @Test
    fun `100kg for 5 reps estimates 112_5 via Brzycki`() {
        val estimate = OneRepMaxCalculator.estimate(100.0, 5)!!
        assertClose(112.5, estimate.oneRepMaxKg)
        assertEquals(OneRepMaxMethod.BRZYCKI, estimate.method)
        assertTrue(estimate.isReliable)
    }

    @Test
    fun `60kg for 10 reps estimates 80kg`() {
        assertClose(80.0, OneRepMaxCalculator.estimate(60.0, 10)!!.oneRepMaxKg)
    }

    @Test
    fun `100kg for 12 reps estimates 144_1kg`() {
        assertClose(144.1, OneRepMaxCalculator.estimate(100.0, 12)!!.oneRepMaxKg)
    }

    @Test
    fun `13 reps switches to Epley and is flagged unreliable`() {
        val estimate = OneRepMaxCalculator.estimate(100.0, 13)!!
        assertEquals(OneRepMaxMethod.EPLEY, estimate.method)
        assertClose(143.3, estimate.oneRepMaxKg)
        assertFalse(estimate.isReliable)
        assertNotNull(estimate.caveat)
    }

    @Test
    fun `20 reps is the last rep count that produces an estimate`() {
        assertNotNull(OneRepMaxCalculator.estimate(100.0, 20))
    }

    @Test
    fun `no estimate is invented past 20 reps`() {
        assertNull(OneRepMaxCalculator.estimate(100.0, 21))
        assertNull(OneRepMaxCalculator.estimate(100.0, 30))
        assertNull(OneRepMaxCalculator.estimate(100.0, 100))
    }

    @Test
    fun `non positive input yields null rather than zero`() {
        assertNull(OneRepMaxCalculator.estimate(0.0, 5))
        assertNull(OneRepMaxCalculator.estimate(-50.0, 5))
        assertNull(OneRepMaxCalculator.estimate(100.0, 0))
        assertNull(OneRepMaxCalculator.estimate(100.0, -3))
    }

    /**
     * Regression guard for the web app's 1RM cliff: it returned weight/0.1938 (~5.16x)
     * at 30 reps and weight*1.5 at 31 reps — a 3.4x jump between adjacent inputs.
     *
     * Aro switches formula at 12 reps, where Brzycki (144.1 for 100 kg) and Epley (143.3
     * for 13 reps) meet, so a sub-1% dip is expected and harmless; what must never happen
     * is a large discontinuity or an absurd extrapolation.
     */
    @Test
    fun `estimates never jump or blow up across the rep range`() {
        var previous = 0.0
        for (reps in 1..OneRepMaxCalculator.MAX_EXTRAPOLATION_REPS) {
            val estimate = OneRepMaxCalculator.estimate(100.0, reps)!!
            if (previous > 0.0) {
                val dropPercent = ((previous - estimate.oneRepMaxKg) / previous) * 100
                assertTrue("rep $reps dropped $dropPercent% below the previous estimate", dropPercent <= 1.0)
                assertTrue(
                    "rep $reps jumped to ${estimate.oneRepMaxKg}",
                    estimate.oneRepMaxKg <= previous * 1.10,
                )
            }
            assertTrue("rep $reps blew up to ${estimate.oneRepMaxKg}", estimate.oneRepMaxKg <= 170.0)
            previous = estimate.oneRepMaxKg
        }
    }

    @Test
    fun `best of a session picks the heaviest estimate not the heaviest set`() {
        val sets = listOf(SetEntry(100.0, 5), SetEntry(60.0, 10))
        assertClose(112.5, OneRepMaxCalculator.bestOf(sets)!!.oneRepMaxKg)
    }

    @Test
    fun `best of an empty or invalid session is null`() {
        assertNull(OneRepMaxCalculator.bestOf(emptyList()))
        assertNull(OneRepMaxCalculator.bestOf(listOf(SetEntry(0.0, 5), SetEntry(60.0, 0))))
    }

    // ---- Tonnage ----------------------------------------------------------

    @Test
    fun `tonnage sums load times reps`() {
        val sets = listOf(SetEntry(100.0, 5), SetEntry(100.0, 5), SetEntry(100.0, 5))
        assertEquals(1500.0, OneRepMaxCalculator.tonnageKg(sets), 0.001)
    }

    @Test
    fun `tonnage ignores invalid sets`() {
        val sets = listOf(SetEntry(100.0, 5), SetEntry(60.0, 0), SetEntry(0.0, 10))
        assertEquals(500.0, OneRepMaxCalculator.tonnageKg(sets), 0.001)
    }

    @Test
    fun `workout log reports its own tonnage and top set`() {
        val log = Fixtures.strength(100.0 to 5, 100.0 to 5, 90.0 to 8)
        assertEquals(1720.0, log.totalVolumeKg, 0.001) // 500 + 500 + 720
        assertEquals(100.0, log.topSetKg, 0.001)
        assertEquals(3, log.workingSets.size)
    }

    // ---- Inverse ----------------------------------------------------------

    @Test
    fun `estimated reps inverts the epley relation`() {
        assertEquals(6, OneRepMaxCalculator.estimatedReps(oneRepMaxKg = 120.0, weightKg = 100.0))
        assertEquals(1, OneRepMaxCalculator.estimatedReps(oneRepMaxKg = 100.0, weightKg = 100.0))
    }

    @Test
    fun `estimated reps is null when the load exceeds the one rep max`() {
        assertNull(OneRepMaxCalculator.estimatedReps(100.0, 110.0))
        assertNull(OneRepMaxCalculator.estimatedReps(0.0, 100.0))
    }

    // ---- Percentage table -------------------------------------------------

    @Test
    fun `rep max table covers 1 to 12 reps and decreases`() {
        val table = OneRepMaxCalculator.repMaxPercentages()
        assertEquals(12, table.size)
        assertEquals(1.0, table.getValue(1), 0.0001)
        assertEquals(0.95, table.getValue(2), 0.0001)
        assertEquals(table.entries.sortedBy { it.key }.map { it.value }, table.values.toList())
    }

    // ---- Strength calories ------------------------------------------------

    @Test
    fun `strength burn is volume based at 0_065 kcal per kg`() {
        // 3 x 100 kg x 5 reps = 1500 kg tonnage -> 97.5 kcal
        assertEquals(98, Fixtures.strength(100.0 to 5, 100.0 to 5, 100.0 to 5).estimatedCaloriesBurned())
        // 5 x 100 kg x 10 reps = 5000 kg tonnage -> 325 kcal
        assertEquals(
            325,
            Fixtures.strength(100.0 to 10, 100.0 to 10, 100.0 to 10, 100.0 to 10, 100.0 to 10)
                .estimatedCaloriesBurned(),
        )
    }

    @Test
    fun `cardio logs report no volume based burn`() {
        assertEquals(0, Fixtures.cardio().estimatedCaloriesBurned())
    }

    @Test
    fun `tonnage coefficient matches the documented constant`() {
        assertEquals(0.065, KCAL_PER_KG_TONNAGE, 0.0001)
    }

    // ---- Set duplicator tests ---------------------------------------------

    @Test
    fun `duplicate clone preserves load and reps and unsets completion`() {
        val original = SetEntry(weightKg = 80.0, reps = 8, isCompleted = true)
        val clone = original.duplicate()
        assertEquals(80.0, clone.weightKg, 0.001)
        assertEquals(8, clone.reps)
        assertFalse("Duplicated sets must start as not completed", clone.isCompleted)
    }

    @Test
    fun `duplicate clone supports weight and rep deltas for progressive overload`() {
        val original = SetEntry(weightKg = 80.0, reps = 8)
        val progressive = original.duplicate(weightDeltaKg = 2.5, repsDelta = -1)
        assertEquals(82.5, progressive.weightKg, 0.001)
        assertEquals(7, progressive.reps)
    }

    @Test
    fun `duplicateSetAt inserts clone immediately after target set`() {
        val sets = listOf(
            SetEntry(weightKg = 60.0, reps = 12),
            SetEntry(weightKg = 80.0, reps = 8),
            SetEntry(weightKg = 100.0, reps = 4),
        )
        val updated = sets.duplicateSetAt(index = 1, weightDeltaKg = 5.0)
        assertEquals(4, updated.size)
        assertEquals(60.0, updated[0].weightKg, 0.001)
        assertEquals(80.0, updated[1].weightKg, 0.001)
        assertEquals(85.0, updated[2].weightKg, 0.001) // Cloned set right after index 1
        assertEquals(8, updated[2].reps)
        assertEquals(100.0, updated[3].weightKg, 0.001)
    }

    @Test
    fun `duplicateLastSet appends copy to end of list`() {
        val sets = listOf(
            SetEntry(weightKg = 70.0, reps = 10),
            SetEntry(weightKg = 75.0, reps = 8),
        )
        val updated = sets.duplicateLastSet(weightDeltaKg = 2.5)
        assertEquals(3, updated.size)
        assertEquals(77.5, updated[2].weightKg, 0.001)
        assertEquals(8, updated[2].reps)
    }

    @Test
    fun `duplicateLastSet on empty list supplies default working set`() {
        val empty = emptyList<SetEntry>()
        val result = empty.duplicateLastSet()
        assertEquals(1, result.size)
        assertEquals(60.0, result[0].weightKg, 0.001)
        assertEquals(10, result[0].reps)
    }
}