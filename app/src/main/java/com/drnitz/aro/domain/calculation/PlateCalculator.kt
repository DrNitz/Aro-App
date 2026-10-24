package com.drnitz.aro.domain.calculation

import kotlin.math.roundToInt

/**
 * A pair of plates for one side of the barbell.
 */
data class PlatePair(
    val weightKg: Double,
    val countPerSide: Int,
) {
    val totalWeightKg: Double get() = weightKg * countPerSide * 2
}

/**
 * Full breakdown of how to load a barbell to reach a target resistance load.
 */
data class PlateCalculation(
    val targetWeightKg: Double,
    val barWeightKg: Double,
    val weightPerSideKg: Double,
    val platesPerSide: List<PlatePair>,
    val achievedTotalWeightKg: Double,
    val remainderKg: Double,
) {
    val isExact: Boolean get() = remainderKg <= 0.001

    /** Human-readable summary for one side of the barbell, e.g. "1 × 25kg, 1 × 15kg" */
    val sideSummary: String
        get() = if (platesPerSide.isEmpty()) {
            "Empty bar"
        } else {
            platesPerSide.joinToString(", ") { pair ->
                val displayWeight = if (pair.weightKg % 1.0 == 0.0) {
                    pair.weightKg.roundToInt().toString()
                } else {
                    pair.weightKg.toString()
                }
                "${pair.countPerSide} × ${displayWeight}kg"
            }
        }
}

/**
 * Standard Olympic plate calculator (greedy allocation).
 *
 * Supports standard Olympic plate denominations (25kg, 20kg, 15kg, 10kg, 5kg, 2.5kg, 1.25kg)
 * and custom bar weights (20kg Olympic, 15kg Women's, 10kg EZ-bar, 0kg Smith/Machine).
 */
object PlateCalculator {

    val STANDARD_OLYMPIC_PLATES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    val EXTENDED_FRACTIONAL_PLATES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25, 0.5, 0.25)

    const val OLYMPIC_BAR_KG = 20.0
    const val WOMENS_BAR_KG = 15.0
    const val EZ_CURL_BAR_KG = 10.0
    const val MACHINE_ZERO_BAR_KG = 0.0

    /**
     * Calculates the required plates per side to reach [targetWeightKg].
     *
     * @param targetWeightKg Total load to be lifted.
     * @param barWeightKg Weight of the barbell collar & shaft (defaults to 20kg).
     * @param availablePlates Denominations of plates available in the gym, in descending order.
     */
    fun calculate(
        targetWeightKg: Double,
        barWeightKg: Double = OLYMPIC_BAR_KG,
        availablePlates: List<Double> = STANDARD_OLYMPIC_PLATES,
    ): PlateCalculation {
        val safeTarget = targetWeightKg.coerceAtLeast(0.0)
        val safeBar = barWeightKg.coerceAtLeast(0.0)

        if (safeTarget <= safeBar) {
            return PlateCalculation(
                targetWeightKg = safeTarget,
                barWeightKg = safeBar,
                weightPerSideKg = 0.0,
                platesPerSide = emptyList(),
                achievedTotalWeightKg = safeBar,
                remainderKg = 0.0,
            )
        }

        var neededPerSide = (safeTarget - safeBar) / 2.0
        val sortedPlates = availablePlates.filter { it > 0.0 }.sortedDescending()
        val pairs = mutableListOf<PlatePair>()

        for (plate in sortedPlates) {
            if (neededPerSide >= plate) {
                val count = (neededPerSide / plate).toInt()
                if (count > 0) {
                    pairs.add(PlatePair(weightKg = plate, countPerSide = count))
                    neededPerSide -= (count * plate)
                    // Round to avoid micro precision drift
                    neededPerSide = (neededPerSide * 1000.0).roundToInt() / 1000.0
                }
            }
        }

        val totalPlatesWeight = pairs.sumOf { it.totalWeightKg }
        val achievedTotal = safeBar + totalPlatesWeight
        val remainder = (safeTarget - achievedTotal).coerceAtLeast(0.0)

        return PlateCalculation(
            targetWeightKg = safeTarget,
            barWeightKg = safeBar,
            weightPerSideKg = (safeTarget - safeBar) / 2.0,
            platesPerSide = pairs,
            achievedTotalWeightKg = (achievedTotal * 1000.0).roundToInt() / 1000.0,
            remainderKg = (remainder * 1000.0).roundToInt() / 1000.0,
        )
    }
}
