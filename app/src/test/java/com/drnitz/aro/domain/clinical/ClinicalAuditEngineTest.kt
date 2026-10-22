package com.drnitz.aro.domain.clinical

import com.drnitz.aro.domain.Fixtures
import com.drnitz.aro.domain.assertClose
import com.drnitz.aro.domain.model.ClinicalCatalog
import com.drnitz.aro.domain.model.ClinicalProtocol
import com.drnitz.aro.domain.model.NutrientKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalAuditEngineTest {

    private fun totals(
        sodium: Double? = null,
        potassium: Double? = null,
        fiber: Double? = null,
        satFat: Double? = null,
        sugar: Double? = null,
        magnesium: Double? = null,
        calcium: Double? = null,
        calories: Double? = 2000.0,
    ) = com.drnitz.aro.domain.model.NutrientTotals.ofAmounts(
        buildMap {
            calories?.let { put(NutrientKey.CALORIES, com.drnitz.aro.domain.model.NutrientAmount(it)) }
            sodium?.let { put(NutrientKey.SODIUM, com.drnitz.aro.domain.model.NutrientAmount(it)) }
            potassium?.let { put(NutrientKey.POTASSIUM, com.drnitz.aro.domain.model.NutrientAmount(it)) }
            fiber?.let { put(NutrientKey.FIBER, com.drnitz.aro.domain.model.NutrientAmount(it)) }
            satFat?.let { put(NutrientKey.SATURATED_FAT, com.drnitz.aro.domain.model.NutrientAmount(it)) }
            sugar?.let { put(NutrientKey.SUGAR, com.drnitz.aro.domain.model.NutrientAmount(it)) }
            magnesium?.let { put(NutrientKey.MAGNESIUM, com.drnitz.aro.domain.model.NutrientAmount(it)) }
            calcium?.let { put(NutrientKey.CALCIUM, com.drnitz.aro.domain.model.NutrientAmount(it)) }
        }
    )

    // ---- Na:K ratio -------------------------------------------------------

    @Test
    fun `ratio is reported in both mass and molar units`() {
        val ratio = ClinicalAuditEngine.naKRatio(sodiumMg = 2400.0, potassiumMg = 3200.0)
        assertEquals(0.75, ratio.massRatio!!, 0.001)
        assertEquals(1.28, ratio.molarRatio!!, 0.01)
    }

    @Test
    fun `molar conversion factor matches the atomic masses`() {
        assertClose(39.10 / 22.99, NaKRatio.MOLAR_CONVERSION_FACTOR, 0.0001)
    }

    @Test
    fun `a mass ratio of 0_75 is only moderate once judged molar`() {
        // The web app called this "Cardioprotective" because it compared the mass ratio
        // against the molar thresholds. 0.75 mass = 1.28 molar, which is borderline.
        assertEquals(RatioStatus.MODERATE, ClinicalAuditEngine.naKRatio(2400.0, 3200.0).status)
    }

    @Test
    fun `genuinely optimal profiles are optimal molar`() {
        assertEquals(RatioStatus.OPTIMAL, ClinicalAuditEngine.naKRatio(800.0, 3200.0).status)
    }

    @Test
    fun `high sodium against low potassium is high risk`() {
        val ratio = ClinicalAuditEngine.naKRatio(3000.0, 500.0)
        assertEquals(10.2, ratio.molarRatio!!, 0.05)
        assertEquals(RatioStatus.HIGH_RISK, ratio.status)
    }

    @Test
    fun `no potassium means no ratio instead of the web app's invented 3_0`() {
        val ratio = ClinicalAuditEngine.naKRatio(2400.0, 0.0)
        assertNull(ratio.molarRatio)
        assertNull(ratio.massRatio)
        assertEquals(RatioStatus.UNDETERMINED, ratio.status)
        assertFalse(ratio.isDetermined)
    }

    @Test
    fun `ratio with no sodium at all is optimal rather than undefined`() {
        val ratio = ClinicalAuditEngine.naKRatio(0.0, 3200.0)
        assertEquals(0.0, ratio.massRatio!!, 0.001)
        assertEquals(RatioStatus.OPTIMAL, ratio.status)
    }

    @Test
    fun `spectrum position stays within 0 to 1 for the UI needle`() {
        // 800/3200 mass = 0.25 -> 0.43 molar -> 0.14 along a 0-3 spectrum
        assertClose(0.14, ClinicalAuditEngine.naKRatio(800.0, 3200.0).spectrumPosition.toDouble(), 0.02)
        assertClose(1.0, ClinicalAuditEngine.naKRatio(9000.0, 300.0).spectrumPosition.toDouble(), 0.01)
        assertEquals(0.5f, ClinicalAuditEngine.naKRatio(0.0, 0.0).spectrumPosition, 0.001f)
    }

    // ---- Fiber and saturated fat -----------------------------------------

    @Test
    fun `fiber density is grams per 1000 kcal`() {
        assertEquals(15.0, ClinicalAuditEngine.fiberDensity(fiberG = 30.0, calories = 2000), 0.001)
    }

    @Test
    fun `fiber target scales with energy on the protocol density`() {
        assertEquals(28, Fixtures.AHA.fiberTargetG(2000))
        assertEquals(32, Fixtures.AHA.fiberTargetG(2300))
        assertEquals(17, Fixtures.AHA.fiberTargetG(1200)) // AHA: 14 g per 1000 kcal
        assertEquals(19, ClinicalCatalog.byId("dash_cardio").fiberTargetG(1200)) // DASH: 16 g
    }

    @Test
    fun `fiber target floors at 1200 kcal of basis`() {
        assertEquals(17, Fixtures.AHA.fiberTargetG(400)) // 1.2 * 14
    }

    @Test
    fun `saturated fat percent uses the 9 kcal per gram factor`() {
        // 11.25% at 2000 kcal, but the audit evaluates against a 1200 kcal floor
        assertEquals(11.3, ClinicalAuditEngine.saturatedFatPercent(25.0, 2000), 0.001)
        assertEquals(18.8, ClinicalAuditEngine.saturatedFatPercent(25.0, 1200), 0.001)
    }

    @Test
    fun `percentage helpers do not divide by zero`() {
        assertEquals(0.0, ClinicalAuditEngine.fiberDensity(30.0, 0), 0.001)
        assertEquals(0.0, ClinicalAuditEngine.saturatedFatPercent(25.0, 0), 0.001)
    }

    // ---- Verdicts ---------------------------------------------------------

    @Test
    fun `sodium above the AHA ceiling is flagged`() {
        val assessment = ClinicalAuditEngine.assess(totals(sodium = 2400.0), Fixtures.AHA, 2000)
        assertEquals(AuditStatus.EXCEEDS_LIMIT, assessment.verdictFor(NutrientKey.SODIUM).status)
        assertTrue(assessment.verdictFor(NutrientKey.SODIUM).isUpperLimit)
    }

    @Test
    fun `sodium within the DASH ceiling is on target`() {
        val assessment = ClinicalAuditEngine.assess(totals(sodium = 1400.0), ClinicalCatalog.byId("dash_cardio"), 2000)
        assertEquals(AuditStatus.ON_TARGET, assessment.verdictFor(NutrientKey.SODIUM).status)
    }

    @Test
    fun `potassium below target is flagged`() {
        val assessment = ClinicalAuditEngine.assess(totals(potassium = 1000.0), Fixtures.AHA, 2000)
        assertEquals(AuditStatus.BELOW_TARGET, assessment.verdictFor(NutrientKey.POTASSIUM).status)
    }

    @Test
    fun `potassium above target passes on a target protocol`() {
        val assessment = ClinicalAuditEngine.assess(totals(potassium = 4000.0), Fixtures.AHA, 2000)
        assertEquals(AuditStatus.ON_TARGET, assessment.verdictFor(NutrientKey.POTASSIUM).status)
    }

    /**
     * The web app rendered the Renal protocol's potassium ceiling with a green
     * "100% of Target" badge because it never read `isPotassiumCeiling`.
     */
    @Test
    fun `potassium above the renal ceiling is a limit breach not a success`() {
        val assessment = ClinicalAuditEngine.assess(totals(potassium = 4000.0), Fixtures.RENAL, 2000)
        val verdict = assessment.verdictFor(NutrientKey.POTASSIUM)
        assertEquals(AuditStatus.EXCEEDS_LIMIT, verdict.status)
        assertTrue(verdict.isUpperLimit)
        assertTrue(
            assessment.findings.any { it.code == FindingCode.POTASSIUM_CEILING_EXCEEDED },
        )
        assertEquals(FindingSeverity.CRITICAL, assessment.warnings.first().severity)
    }

    @Test
    fun `the same intake passes on AHA and fails on renal`() {
        // 3000 mg: below the renal 2200 mg ceiling... no, above it. AHA wants >= 3500.
        val intake = totals(potassium = 5000.0)
        assertEquals(
            AuditStatus.EXCEEDS_LIMIT,
            ClinicalAuditEngine.assess(intake, Fixtures.RENAL, 2000).verdictFor(NutrientKey.POTASSIUM).status,
        )
        assertEquals(
            AuditStatus.ON_TARGET,
            ClinicalAuditEngine.assess(intake, Fixtures.AHA, 2000).verdictFor(NutrientKey.POTASSIUM).status,
        )
    }

    @Test
    fun `a nutrient no food reported is unreported not zero`() {
        val assessment = ClinicalAuditEngine.assess(totals(sodium = 1500.0), Fixtures.AHA, 2000)
        assertEquals(AuditStatus.UNREPORTED, assessment.verdictFor(NutrientKey.POTASSIUM).status)
        assertNull(assessment.verdictFor(NutrientKey.POTASSIUM).actual)
    }

    @Test
    fun `fiber and saturated fat are withheld below the auditable intake`() {
        val assessment = ClinicalAuditEngine.assess(
            totals(fiber = 2.0, satFat = 40.0, calories = 400.0),
            Fixtures.AHA,
            400,
        )
        assertFalse(assessment.hasSufficientData)
        assertEquals(AuditStatus.NOT_ENOUGH_DATA, assessment.verdictFor(NutrientKey.FIBER).status)
        assertTrue(assessment.findings.none { it.code == FindingCode.FIBER_DEFICIT })
        assertTrue(assessment.findings.any { it.code == FindingCode.INSUFFICIENT_DATA })
    }

    @Test
    fun `fiber deficit raises a warning below half the target`() {
        // 1800 kcal basis -> 25 g target, so 12 g is less than half of it
        val assessment = ClinicalAuditEngine.assess(totals(fiber = 12.0), Fixtures.AHA, 1800)
        assertEquals(AuditStatus.BELOW_TARGET, assessment.verdictFor(NutrientKey.FIBER).status)
        val finding = assessment.findings.first { it.code == FindingCode.FIBER_DEFICIT }
        assertEquals(FindingSeverity.WARNING, finding.severity)
        assertTrue(finding.message.contains("≥ 14 g"))
        assertTrue(finding.message.contains("6.7 g per 1000 kcal")) // 12 g over 1800 kcal
    }

    @Test
    fun `meeting the fiber target is reported as optimal`() {
        val assessment = ClinicalAuditEngine.assess(totals(fiber = 30.0), Fixtures.AHA, 2000)
        assertEquals(15.0, assessment.fiberDensityGPer1000Kcal, 0.001)
        assertEquals(28, assessment.fiberTargetG)
        assertTrue(assessment.findings.any { it.code == FindingCode.FIBER_OPTIMAL })
    }

    @Test
    fun `fiber within the target band is not reported as a deficit`() {
        // 1200 kcal basis -> 17 g target; 15 g is 88% of it, so no deficit fires
        val assessment = ClinicalAuditEngine.assess(totals(fiber = 15.0), Fixtures.AHA, 1200)
        assertTrue(assessment.findings.none { it.code == FindingCode.FIBER_DEFICIT })
    }

    @Test
    fun `saturated fat above the ceiling is flagged with its percentage`() {
        val assessment = ClinicalAuditEngine.assess(totals(satFat = 25.0), Fixtures.AHA, 2000)
        assertEquals(11.3, assessment.saturatedFatPercent, 0.001)
        assertEquals(AuditStatus.EXCEEDS_LIMIT, assessment.verdictFor(NutrientKey.SATURATED_FAT).status)
        assertTrue(assessment.findings.any { it.code == FindingCode.SATURATED_FAT_CEILING })
    }

    @Test
    fun `keto protocol tolerates more saturated fat than the AHA protocol`() {
        val intake = totals(satFat = 25.0)
        assertEquals(
            AuditStatus.EXCEEDS_LIMIT,
            ClinicalAuditEngine.assess(intake, Fixtures.AHA, 2000)
                .verdictFor(NutrientKey.SATURATED_FAT).status,
        )
        assertEquals(
            AuditStatus.ON_TARGET,
            ClinicalAuditEngine.assess(intake, ClinicalCatalog.byId("low_carb_keto"), 2000)
                .verdictFor(NutrientKey.SATURATED_FAT).status,
        )
    }

    @Test
    fun `free sugar above 36 g is advisory`() {
        val assessment = ClinicalAuditEngine.assess(totals(sugar = 55.0), Fixtures.AHA, 2000)
        assertEquals(AuditStatus.EXCEEDS_LIMIT, assessment.verdictFor(NutrientKey.SUGAR).status)
        assertTrue(assessment.findings.any { it.code == FindingCode.ADDED_SUGAR_CEILING })
    }

    @Test
    fun `magnesium and calcium use the protocol targets`() {
        val assessment = ClinicalAuditEngine.assess(
            totals(magnesium = 300.0, calcium = 1100.0),
            Fixtures.AHA,
            2000,
        )
        assertEquals(AuditStatus.BELOW_TARGET, assessment.verdictFor(NutrientKey.MAGNESIUM).status)
        assertEquals(AuditStatus.ON_TARGET, assessment.verdictFor(NutrientKey.CALCIUM).status)
    }

    // ---- Findings ---------------------------------------------------------

    @Test
    fun `elevated sodium with a high ratio is critical`() {
        val assessment = ClinicalAuditEngine.assess(totals(sodium = 3000.0, potassium = 600.0), Fixtures.AHA, 2000)
        val finding = assessment.findings.first { it.code == FindingCode.ELEVATED_SODIUM_HIGH_NAK }
        assertEquals(FindingSeverity.CRITICAL, finding.severity)
    }

    @Test
    fun `elevated sodium buffered by potassium is advisory not critical`() {
        // 2400 mg sodium (over the 2300 AHA ceiling) with 3000 mg potassium keeps the
        // molar ratio at 1.36, below the 1.8 buffer threshold.
        val assessment = ClinicalAuditEngine.assess(totals(sodium = 2400.0, potassium = 3000.0), Fixtures.AHA, 2000)
        assertEquals(AuditStatus.EXCEEDS_LIMIT, assessment.verdictFor(NutrientKey.SODIUM).status)
        assertTrue(assessment.findings.any { it.code == FindingCode.BUFFERED_SODIUM })
        assertTrue(assessment.findings.none { it.code == FindingCode.ELEVATED_SODIUM_HIGH_NAK })
    }

    @Test
    fun `missing potassium data asks the user to log it`() {
        val assessment = ClinicalAuditEngine.assess(totals(sodium = 1500.0), Fixtures.AHA, 2000)
        val finding = assessment.findings.first { it.code == FindingCode.LOW_POTASSIUM }
        assertEquals(FindingSeverity.ADVISORY, finding.severity)
        assertTrue(finding.message.contains("cannot be calculated"))
    }

    @Test
    fun `a clean day reports balanced architecture`() {
        val assessment = ClinicalAuditEngine.assess(
            totals(
                sodium = 1500.0,
                potassium = 3000.0,
                fiber = 30.0,
                satFat = 10.0,
                sugar = 20.0,
                magnesium = 400.0,
                calcium = 1000.0,
            ),
            Fixtures.AHA,
            2000,
        )
        assertTrue(assessment.warnings.isEmpty())
        assertTrue(assessment.findings.any { it.code == FindingCode.BALANCED_DAY })
    }

    /** Regression guard: the web app shipped a raw LaTeX `$\\ge$` fragment to users. */
    @Test
    fun `no finding leaks latex or raw markup`() {
        val assessment = ClinicalAuditEngine.assess(
            totals(sodium = 3000.0, potassium = 500.0, fiber = 5.0, satFat = 40.0, sugar = 60.0),
            Fixtures.AHA,
            2000,
        )
        for (finding in assessment.findings) {
            assertFalse("Leaked markup: ${finding.message}", finding.message.contains("$"))
            assertFalse("Leaked markup: ${finding.message}", finding.message.contains("\\"))
            assertTrue(finding.message.isNotBlank())
        }
        for (finding in assessment.findings.filter { it.code == FindingCode.FIBER_DEFICIT }) {
            assertTrue(finding.message.contains("≥"))
        }
    }

    @Test
    fun `assessment exposes every audited nutrient`() {
        val assessment = ClinicalAuditEngine.assess(totals(), Fixtures.AHA, 2000)
        val keys = assessment.verdicts.map { it.key }.toSet()
        assertTrue(
            keys.containsAll(
                setOf(
                    NutrientKey.SODIUM,
                    NutrientKey.POTASSIUM,
                    NutrientKey.FIBER,
                    NutrientKey.SATURATED_FAT,
                    NutrientKey.MAGNESIUM,
                    NutrientKey.CALCIUM,
                    NutrientKey.SUGAR,
                )
            ),
        )
    }

    @Test
    fun `an unknown nutrient key degrades to a neutral verdict`() {
        val assessment = ClinicalAuditEngine.assess(totals(), Fixtures.AHA, 2000)
        assertEquals(AuditStatus.UNREPORTED, assessment.verdictFor(NutrientKey.CARBS).status)
    }

    // ---- Catalog ----------------------------------------------------------

    @Test
    fun `catalog ships the five clinical protocols`() {
        assertEquals(5, ClinicalCatalog.all.size)
        assertEquals("aha_standard", ClinicalCatalog.default.id)
    }

    @Test
    fun `unknown protocol ids fall back to the AHA baseline`() {
        assertEquals("aha_standard", ClinicalCatalog.byId("does-not-exist").id)
        assertEquals("aha_standard", ClinicalCatalog.byId(null).id)
        assertEquals(0, ClinicalCatalog.indexOf("nope"))
        assertEquals(3, ClinicalCatalog.indexOf("renal_restricted"))
    }

    @Test
    fun `only the renal protocol declares a potassium ceiling`() {
        val ceilings = ClinicalCatalog.all.filter { it.potassiumIsCeiling }
        assertEquals(listOf("renal_restricted"), ceilings.map { it.id })
    }

    @Test
    fun `every protocol cites a guideline and sets every threshold`() {
        for (protocol in ClinicalCatalog.all) {
            assertTrue(protocol.citation.isNotBlank())
            assertTrue(protocol.maxSodiumMg > 0)
            assertTrue(protocol.targetPotassiumMg > 0)
            assertTrue(protocol.minFiberPer1000KcalG > 0)
            assertTrue(protocol.maxSaturatedFatPercent in 1..40)
            assertTrue(protocol.targetMagnesiumMg > 0)
            assertTrue(protocol.targetCalciumMg > 0)
            assertNotNull(ClinicalCatalog.byId(protocol.id))
        }
    }
}