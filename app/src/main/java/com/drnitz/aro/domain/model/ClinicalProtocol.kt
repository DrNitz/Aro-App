package com.drnitz.aro.domain.model

import kotlin.math.roundToInt

/**
 * An evidence-based electrolyte and macronutrient guideline Aro can audit a day against.
 *
 * Ported from the web app's `clinicalProtocols.js` with two corrections:
 *  - every threshold carries its **direction** ([potassiumIsCeiling]) so an upper limit
 *    can never be rendered as a "target reached" success badge — the web app ignored
 *    `isPotassiumCeiling`, showing green on the Renal protocol at 200% of the limit;
 *  - citations are kept as plain text (the web app leaked a LaTeX `$\ge$` fragment into
 *    user-facing strings).
 */
data class ClinicalProtocol(
    val id: String,
    val name: String,
    val shortName: String,
    val badge: String,
    val description: String,
    val citation: String,
    /** Maximum daily sodium, mg. Always an upper limit. */
    val maxSodiumMg: Int,
    /** Daily potassium goal in mg — a target, or a ceiling when [potassiumIsCeiling]. */
    val targetPotassiumMg: Int,
    /** When true, exceeding [targetPotassiumMg] is a finding, not a success. */
    val potassiumIsCeiling: Boolean = false,
    /** Minimum dietary fiber density, g per 1000 kcal. */
    val minFiberPer1000KcalG: Int,
    /** Maximum saturated fat as a percentage of total energy. */
    val maxSaturatedFatPercent: Int,
    val targetMagnesiumMg: Int,
    val targetCalciumMg: Int,
) {
    /** Fiber target in grams for a given energy intake, floored at 1200 kcal like the web app. */
    fun fiberTargetG(caloriesEaten: Int): Int {
        val basis = caloriesEaten.coerceAtLeast(MIN_FIBER_BASIS_KCAL)
        return ((basis / 1000.0) * minFiberPer1000KcalG).roundToInt()
    }

    /** Energy below which fiber/percent audits are withheld as statistically meaningless. */
    val minAuditableCalories: Int get() = MIN_AUDITABLE_CALORIES

    companion object {
        /**
         * Fiber density is evaluated against at least this much energy. Below ~800 kcal
         * logged, a meal's fiber and saturated-fat percentages swing wildly and produce
         * alarming, meaningless clinical notes — the web app used 1200 kcal here and 800
         * kcal as the note gate, which disagreed with itself.
         */
        const val MIN_FIBER_BASIS_KCAL = 1200

        /** Below this intake the audit returns findings marked `insufficientData`. */
        const val MIN_AUDITABLE_CALORIES = 800
    }
}

/**
 * The five guideline protocols shipped with Aro.
 *
 * Selectable per user and persisted in Task 5; defaulting to [DEFAULT_PROTOCOL_ID]
 * keeps a fresh install clinically sane.
 */
object ClinicalCatalog {

    const val DEFAULT_PROTOCOL_ID = "aha_standard"

    val all: List<ClinicalProtocol> = listOf(
        ClinicalProtocol(
            id = "aha_standard",
            name = "AHA / WHO General Standard",
            shortName = "AHA Standard",
            badge = "Cardioprotective Baseline",
            description = "General prevention for healthy adults per American Heart Association and WHO consensus guidance.",
            citation = "AHA/ACC 2017 & WHO 2012 Guideline on Sodium Intake for Adults",
            maxSodiumMg = 2300,
            targetPotassiumMg = 3500,
            minFiberPer1000KcalG = 14,
            maxSaturatedFatPercent = 10,
            targetMagnesiumMg = 400,
            targetCalciumMg = 1000,
        ),
        ClinicalProtocol(
            id = "dash_cardio",
            name = "DASH / Therapeutic Protocol",
            shortName = "DASH Protocol",
            badge = "Hypertension Management",
            description = "Therapeutic sodium restriction with enhanced potassium for vascular compliance.",
            citation = "Appel LJ, et al. DASH Collaborative Research Group. N Engl J Med. 1997",
            maxSodiumMg = 1500,
            targetPotassiumMg = 4700,
            minFiberPer1000KcalG = 16,
            maxSaturatedFatPercent = 6,
            targetMagnesiumMg = 420,
            targetCalciumMg = 1200,
        ),
        ClinicalProtocol(
            id = "athletic_exertion",
            name = "Athletic & Heavy Exertion",
            shortName = "Athletic Electrolytes",
            badge = "High Sweat Rate",
            description = "Endurance athletes and heavy resistance training; compensates sweat electrolyte losses (~900-1400 mg Na+/L sweat).",
            citation = "ACSM Joint Position Statement: Nutrition and Athletic Performance",
            maxSodiumMg = 4200,
            targetPotassiumMg = 4000,
            minFiberPer1000KcalG = 14,
            maxSaturatedFatPercent = 10,
            targetMagnesiumMg = 500,
            targetCalciumMg = 1000,
        ),
        ClinicalProtocol(
            id = "renal_restricted",
            name = "Renal / Electrolyte Restricted",
            shortName = "Renal Guide",
            badge = "CKD Care Guideline",
            description = "Controlled potassium and sodium to prevent hyperkalemia and fluid overload in compromised renal filtration.",
            citation = "KDOQI Clinical Practice Guideline for Nutrition in CKD (2020 Update)",
            maxSodiumMg = 1500,
            targetPotassiumMg = 2200,
            potassiumIsCeiling = true,
            minFiberPer1000KcalG = 12,
            maxSaturatedFatPercent = 7,
            targetMagnesiumMg = 320,
            targetCalciumMg = 1000,
        ),
        ClinicalProtocol(
            id = "low_carb_keto",
            name = "Low-Carb / Ketogenic Protocol",
            shortName = "Keto Natriuresis",
            badge = "Natriuresis Support",
            description = "Compensates the natriuresis of fasting and reduced insulin, which accelerate renal sodium and mineral excretion.",
            citation = "Volek JS, Phinney SD. The Art and Science of Low Carbohydrate Living",
            maxSodiumMg = 4500,
            targetPotassiumMg = 3600,
            minFiberPer1000KcalG = 12,
            maxSaturatedFatPercent = 25,
            targetMagnesiumMg = 450,
            targetCalciumMg = 1000,
        ),
    )

    val default: ClinicalProtocol get() = byId(DEFAULT_PROTOCOL_ID)

    /** Never returns null — an unknown id falls back to the AHA baseline. */
    fun byId(id: String?): ClinicalProtocol =
        all.firstOrNull { it.id == id } ?: default

    fun indexOf(id: String?): Int = all.indexOfFirst { it.id == id }.coerceAtLeast(0)
}