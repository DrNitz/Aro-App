package com.drnitz.aro.domain.clinical

import com.drnitz.aro.domain.model.ClinicalProtocol
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals
import kotlin.math.roundToInt

/**
 * Sodium-to-potassium ratio in both units that matter clinically.
 *
 * The **mass ratio** (mg Na / mg K) is what food labels let a user compute by hand, but
 * the thresholds published for cardiovascular risk apply to the **molar ratio**, because
 * sodium and potassium do not weigh the same per mole. Conversion:
 *
 * ```
 * molar = (sodiumMg / 22.99) / (potassiumMg / 39.10) = mass x 1.7007
 * ```
 *
 * Verdicts in [ClinicalAuditEngine] use the molar ratio. The web app used the mass ratio
 * against the molar thresholds, so a genuinely poor 0.75 mass ratio (1.28 molar) was
 * reported to users as "Cardioprotective (Optimal < 1.0)".
 */
data class NaKRatio(val massRatio: Double?, val molarRatio: Double?) {

    val isDetermined: Boolean get() = molarRatio != null

    val status: RatioStatus
        get() {
            val molar = molarRatio ?: return RatioStatus.UNDETERMINED
            return when {
                molar < OPTIMAL_MAX -> RatioStatus.OPTIMAL
                molar <= MODERATE_MAX -> RatioStatus.MODERATE
                else -> RatioStatus.HIGH_RISK
            }
        }

    /** Position on the 0-3 visual spectrum, as a 0..1 fraction for Task 12's meter. */
    val spectrumPosition: Float
        get() {
            val molar = molarRatio ?: return 0.5f
            return (molar / SPECTRUM_MAX).coerceIn(0.0, 1.0).toFloat()
        }

    companion object {
        /** mmol(mg) / atomic mass: Na 22.99, K 39.10. */
        const val MOLAR_CONVERSION_FACTOR = 39.10 / 22.99

        const val OPTIMAL_MAX = 1.0
        const val MODERATE_MAX = 2.0
        const val SPECTRUM_MAX = 3.0
    }
}

enum class RatioStatus(val label: String, val shortLabel: String) {
    OPTIMAL("Cardioprotective (molar < 1.0)", "Cardioprotective"),
    MODERATE("Borderline balance (molar 1.0 - 2.0)", "Moderate"),
    HIGH_RISK("Atherogenic risk (molar > 2.0)", "Elevated"),
    UNDETERMINED("Not enough potassium data", "Unknown"),
}

/** How a single nutrient scored against the active protocol. */
enum class AuditStatus(val label: String) {
    /** Inside a target band or under an upper limit. */
    ON_TARGET("Within boundary"),

    /** Below a minimum target (potassium, fiber, magnesium, calcium). */
    BELOW_TARGET("Below target"),

    /** Above an upper limit (sodium, saturated fat, free sugar, potassium ceiling). */
    EXCEEDS_LIMIT("Limit exceeded"),

    /** No food logged provides this nutrient at all. */
    ABSENT("Not logged"),

    /** The food database carries no value for it — unknown, which is not the same as zero. */
    UNREPORTED("Not tracked by source"),

    /** Too little logged today for a percentage-based verdict to mean anything. */
    NOT_ENOUGH_DATA("Not enough data"),
}

data class NutrientVerdict(
    val key: NutrientKey,
    val status: AuditStatus,
    val actual: Double?,
    val target: Double?,
    /** True when [target] is a ceiling rather than a goal. */
    val isUpperLimit: Boolean,
    val unitLabel: String,
)

enum class FindingSeverity { INFO, ADVISORY, WARNING, CRITICAL }

/** Stable identifiers so the UI can pick an icon or localise without string matching. */
enum class FindingCode {
    ELEVATED_SODIUM_HIGH_NAK,
    BUFFERED_SODIUM,
    CARDIOPROTECTIVE_ELECTROLYTES,
    LOW_POTASSIUM,
    POTASSIUM_CEILING_EXCEEDED,
    FIBER_DEFICIT,
    FIBER_OPTIMAL,
    SATURATED_FAT_CEILING,
    ADDED_SUGAR_CEILING,
    BALANCED_DAY,
    INSUFFICIENT_DATA,
    ESTIMATED_TOTALS,
}

data class ClinicalFinding(
    val code: FindingCode,
    val severity: FindingSeverity,
    val message: String,
)

data class ClinicalAssessment(
    val protocol: ClinicalProtocol,
    val naKRatio: NaKRatio,
    val fiberDensityGPer1000Kcal: Double,
    val fiberTargetG: Int,
    val saturatedFatPercent: Double,
    val verdicts: List<NutrientVerdict>,
    val findings: List<ClinicalFinding>,
    /** False when too little was logged for fiber and percentage verdicts to be meaningful. */
    val hasSufficientData: Boolean,
) {
    fun verdictFor(key: NutrientKey): NutrientVerdict =
        verdicts.firstOrNull { it.key == key } ?: NutrientVerdict(
            key = key,
            status = AuditStatus.UNREPORTED,
            actual = null,
            target = null,
            isUpperLimit = key.isUpperLimit,
            unitLabel = key.unit.name.lowercase(),
        )

    val warnings: List<ClinicalFinding>
        get() = findings.filter { it.severity == FindingSeverity.WARNING || it.severity == FindingSeverity.CRITICAL }
}

/**
 * The "Dr. Aro" clinical engine: turns a day's [NutrientTotals] into verdicts and
 * evidence-cited findings against a [ClinicalProtocol].
 *
 * Ported from the web app's `generateDoctorClinicalAssessment` with four corrections:
 *  1. Na:K is judged **molar** (see [NaKRatio]), not mass.
 *  2. An **undetermined** ratio (no potassium data) no longer invents a value of 3.0;
 *     it reports [RatioStatus.UNDETERMINED] and raises a low-potassium finding.
 *  3. **Potassium ceilings are honoured** — on the Renal protocol exceeding the limit is
 *     [AuditStatus.EXCEEDS_LIMIT], never an "on target" badge.
 *  4. The fiber note renders a real `≥` character instead of the leaked LaTeX `$\\ge$`.
 */
object ClinicalAuditEngine {

    /** WHO/AHA guidance on free sugars, expressed in grams for a 2000 kcal reference diet. */
    const val ADDED_SUGAR_CEILING_G = 36.0

    fun assess(
        totals: NutrientTotals,
        protocol: ClinicalProtocol = com.drnitz.aro.domain.model.ClinicalCatalog.default,
        caloriesEaten: Int = totals.calories.toInt(),
    ): ClinicalAssessment {
        val sodium = totals.amountOf(NutrientKey.SODIUM)
        val potassium = totals.amountOf(NutrientKey.POTASSIUM)
        val fiber = totals.amountOf(NutrientKey.FIBER)
        val saturatedFat = totals.amountOf(NutrientKey.SATURATED_FAT)
        val sugar = totals.amountOf(NutrientKey.SUGAR)

        val sufficientData = caloriesEaten >= ClinicalProtocol.MIN_AUDITABLE_CALORIES
        val basisCalories = caloriesEaten.coerceAtLeast(ClinicalProtocol.MIN_FIBER_BASIS_KCAL)

        val ratio = naKRatio(sodium, potassium)
        val fiberDensity = fiberDensity(fiber, basisCalories)
        val fiberTarget = protocol.fiberTargetG(caloriesEaten)
        val satFatPercent = saturatedFatPercent(saturatedFat, basisCalories)

        val verdicts = listOf(
            verdict(NutrientKey.SODIUM, totals, protocol.maxSodiumMg.toDouble(), isUpperLimit = true),
            verdict(
                NutrientKey.POTASSIUM,
                totals,
                protocol.targetPotassiumMg.toDouble(),
                isUpperLimit = protocol.potassiumIsCeiling,
            ),
            verdict(NutrientKey.FIBER, totals, fiberTarget.toDouble(), isUpperLimit = false, evaluable = sufficientData),
            verdict(
                NutrientKey.SATURATED_FAT,
                totals,
                protocol.maxSaturatedFatPercent.toDouble(),
                isUpperLimit = true,
                evaluable = sufficientData,
                actual = satFatPercent,
            ),
            verdict(NutrientKey.MAGNESIUM, totals, protocol.targetMagnesiumMg.toDouble(), isUpperLimit = false),
            verdict(NutrientKey.CALCIUM, totals, protocol.targetCalciumMg.toDouble(), isUpperLimit = false),
            verdict(NutrientKey.SUGAR, totals, ADDED_SUGAR_CEILING_G, isUpperLimit = true),
        )

        val findings = buildFindings(
            protocol = protocol,
            caloriesEaten = caloriesEaten,
            basisCalories = basisCalories,
            sufficientData = sufficientData,
            sodium = sodium,
            potassium = potassium,
            ratio = ratio,
            fiber = fiber,
            fiberDensity = fiberDensity,
            fiberTarget = fiberTarget,
            satFatPercent = satFatPercent,
            sugar = sugar,
        )

        return ClinicalAssessment(
            protocol = protocol,
            naKRatio = ratio,
            fiberDensityGPer1000Kcal = fiberDensity,
            fiberTargetG = fiberTarget,
            saturatedFatPercent = satFatPercent,
            verdicts = verdicts,
            findings = findings,
            hasSufficientData = sufficientData,
        )
    }

    /**
     * Sodium-to-potassium ratio in both units.
     *
     * Returns an undetermined ratio when potassium is absent — a "ratio" computed from
     * incomplete data is not a ratio. [fallbackMassRatio] exists only so the UI can show
     * a neutral needle position rather than an empty gauge.
     */
    fun naKRatio(sodiumMg: Double, potassiumMg: Double): NaKRatio {
        if (potassiumMg <= 0.0 || sodiumMg < 0.0) return NaKRatio(null, null)
        val mass = sodiumMg / potassiumMg
        return NaKRatio(
            massRatio = round2(mass),
            molarRatio = round2(mass * NaKRatio.MOLAR_CONVERSION_FACTOR),
        )
    }

    /** Grams of fiber per 1000 kcal. */
    fun fiberDensity(fiberG: Double, calories: Int): Double {
        if (calories <= 0) return 0.0
        return round1((fiberG / calories) * 1000)
    }

    /** Saturated fat as a percentage of energy (fat yields 9 kcal/g). */
    fun saturatedFatPercent(saturatedFatG: Double, calories: Int): Double {
        if (calories <= 0) return 0.0
        return round1((saturatedFatG * com.drnitz.aro.domain.model.EnergyFactors.KCAL_PER_G_FAT / calories) * 100)
    }

    private fun buildFindings(
        protocol: ClinicalProtocol,
        caloriesEaten: Int,
        basisCalories: Int,
        sufficientData: Boolean,
        sodium: Double,
        potassium: Double,
        ratio: NaKRatio,
        fiber: Double,
        fiberDensity: Double,
        fiberTarget: Int,
        satFatPercent: Double,
        sugar: Double,
    ): List<ClinicalFinding> {
        val findings = mutableListOf<ClinicalFinding>()

        if (!sufficientData) {
            findings += ClinicalFinding(
                code = FindingCode.INSUFFICIENT_DATA,
                severity = FindingSeverity.INFO,
                message = "Only $caloriesEaten kcal logged. Log at least " +
                    "${ClinicalProtocol.MIN_AUDITABLE_CALORIES} kcal for percentage-based " +
                    "electrolyte and fiber verdicts to be meaningful.",
            )
        }

        // 1. Electrolyte balance
        val molar = ratio.molarRatio
        if (molar == null) {
            findings += ClinicalFinding(
                code = FindingCode.LOW_POTASSIUM,
                severity = FindingSeverity.ADVISORY,
                message = "No potassium data was logged, so the Na:K ratio cannot be " +
                    "calculated. Potassium-rich whole foods (spinach, avocado, wild " +
                    "salmon, sweet potato) are the fastest way to establish it.",
            )
        } else {
            val molarText = molar.toString()
            when {
                sodium > protocol.maxSodiumMg && molar > HIGH_RATIO_BUFFER_THRESHOLD ->
                    findings += ClinicalFinding(
                        FindingCode.ELEVATED_SODIUM_HIGH_NAK,
                        FindingSeverity.CRITICAL,
                        "Elevated sodium and vascular tone: $sodium mg sodium exceeds the " +
                            "${protocol.maxSodiumMg} mg ${protocol.shortName} ceiling, with a " +
                            "Na:K molar ratio of $molarText. Sodium without potassium " +
                            "counterbalance promotes endothelial stiffening and fluid retention.",
                    )

                sodium > protocol.maxSodiumMg ->
                    findings += ClinicalFinding(
                        FindingCode.BUFFERED_SODIUM,
                        FindingSeverity.ADVISORY,
                        "Buffered sodium intake: sodium is elevated at $sodium mg, but the " +
                            "molar Na:K ratio of $molarText still buffers arterial tone. " +
                            "Reducing sodium remains worthwhile.",
                    )

                molar <= NaKRatio.OPTIMAL_MAX && potassium >= protocol.targetPotassiumMg * POTASSIUM_BUFFER_FRACTION ->
                    findings += ClinicalFinding(
                        FindingCode.CARDIOPROTECTIVE_ELECTROLYTES,
                        FindingSeverity.INFO,
                        "Cardioprotective electrolyte profile: molar Na:K of $molarText " +
                            "(optimal < 1.0). Higher dietary potassium relative to sodium " +
                            "correlates with reduced stroke incidence and relaxed arterial tone.",
                    )

                potassium < protocol.targetPotassiumMg * POTASSIUM_DEFICIT_FRACTION && sodium > ELEVATED_SODIUM_FLOOR ->
                    findings += ClinicalFinding(
                        FindingCode.LOW_POTASSIUM,
                        FindingSeverity.WARNING,
                        "Low potassium intake: $potassium mg against the " +
                            "${protocol.targetPotassiumMg} mg ${protocol.shortName} target, " +
                            "with sodium at $sodium mg. Prioritise potassium-rich whole foods " +
                            "to support cardiovascular homeostasis.",
                    )
            }
        }

        // 2. Potassium ceiling (Renal protocol)
        if (protocol.potassiumIsCeiling && potassium > protocol.targetPotassiumMg) {
            findings += ClinicalFinding(
                code = FindingCode.POTASSIUM_CEILING_EXCEEDED,
                severity = FindingSeverity.CRITICAL,
                message = "Potassium ceiling exceeded: $potassium mg against the " +
                    "${protocol.targetPotassiumMg} mg upper limit for " +
                    "${protocol.name}. For compromised renal filtration, intake above this " +
                    "threshold risks hyperkalemia; follow your clinician's plan.",
            )
        }

        // 3. Fiber density
        if (sufficientData) {
            when {
                fiber < fiberTarget * FIBER_DEFICIT_FRACTION ->
                    findings += ClinicalFinding(
                        FindingCode.FIBER_DEFICIT,
                        FindingSeverity.WARNING,
                        "Fiber deficit ($fiber g of $fiberTarget g): current density is " +
                            "$fiberDensity g per 1000 kcal, below the ≥ " +
                            "${protocol.minFiberPer1000KcalG} g guideline. Soluble fiber binds " +
                            "dietary bile acids to support LDL-C clearance and feeds gut " +
                            "microbiome short-chain fatty acid synthesis.",
                    )

                fiber >= fiberTarget ->
                    findings += ClinicalFinding(
                        FindingCode.FIBER_OPTIMAL,
                        FindingSeverity.INFO,
                        "Optimal glycemic and microbiome fiber: $fiber g logged " +
                            "($fiberDensity g per 1000 kcal), supporting stable " +
                            "postprandial glucose regulation and colonic motility.",
                    )
            }
        }

        // 4. Saturated fat
        if (sufficientData && satFatPercent > protocol.maxSaturatedFatPercent) {
            findings += ClinicalFinding(
                code = FindingCode.SATURATED_FAT_CEILING,
                severity = FindingSeverity.WARNING,
                message = "Saturated fat ceiling: $satFatPercent% of energy against the " +
                    "${protocol.maxSaturatedFatPercent}% ${protocol.shortName} limit. AHA " +
                    "and ACC recommend limiting saturated fat to prevent hepatic LDL-receptor " +
                    "downregulation and ApoB particle increases.",
            )
        }

        // 5. Free sugars
        if (sugar > ADDED_SUGAR_CEILING_G) {
            findings += ClinicalFinding(
                code = FindingCode.ADDED_SUGAR_CEILING,
                severity = FindingSeverity.ADVISORY,
                message = "Sugars at $sugar g against the $ADDED_SUGAR_CEILING_G g daily " +
                    "guideline (World Health Organization, 2015).",
            )
        }

        if (findings.none { it.severity == FindingSeverity.WARNING || it.severity == FindingSeverity.CRITICAL }) {
            findings += ClinicalFinding(
                code = FindingCode.BALANCED_DAY,
                severity = FindingSeverity.INFO,
                message = "Balanced clinical architecture: electrolyte ratios and " +
                    "macronutrient partitions align well with the ${protocol.name} benchmarks.",
            )
        }

        return findings
    }

    private fun verdict(
        key: NutrientKey,
        totals: NutrientTotals,
        target: Double,
        isUpperLimit: Boolean,
        evaluable: Boolean = true,
        actual: Double = totals.amountOf(key),
    ): NutrientVerdict {
        val amount = totals[key]
        val status = when {
            !amount.isReported -> AuditStatus.UNREPORTED
            !evaluable -> AuditStatus.NOT_ENOUGH_DATA
            isUpperLimit && actual > target -> AuditStatus.EXCEEDS_LIMIT
            !isUpperLimit && actual < target -> AuditStatus.BELOW_TARGET
            else -> AuditStatus.ON_TARGET
        }
        return NutrientVerdict(
            key = key,
            status = status,
            actual = actual.takeIf { amount.isReported },
            target = target,
            isUpperLimit = isUpperLimit,
            unitLabel = key.unit.name.lowercase(),
        )
    }

    /** Potassium at or above this fraction of target counts as protective buffering. */
    const val POTASSIUM_BUFFER_FRACTION = 0.7

    /** Below this fraction of target, potassium is flagged as clearly deficient. */
    const val POTASSIUM_DEFICIT_FRACTION = 0.4

    /** Na:K molar ratio above which elevated sodium is no longer considered buffered. */
    const val HIGH_RATIO_BUFFER_THRESHOLD = 1.8

    /** Sodium intake, mg, above which a low-potassium day is worth warning about. */
    const val ELEVATED_SODIUM_FLOOR = 1000.0

    /** Fiber below this fraction of target is reported as a deficit. */
    const val FIBER_DEFICIT_FRACTION = 0.5

    private fun round1(value: Double): Double = (value * 10).roundToInt() / 10.0

    private fun round2(value: Double): Double = (value * 100).roundToInt() / 100.0
}