package com.drnitz.aro.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class BloodPressureStage(val label: String, val clinicalAdvice: String) {
    NORMAL(
        label = "Normal",
        clinicalAdvice = "Optimal cardiovascular status (< 120/80 mmHg). Maintain balanced diet and activity.",
    ),
    ELEVATED(
        label = "Elevated",
        clinicalAdvice = "Systolic 120-129 and Diastolic < 80 mmHg. Lifestyle modifications and sodium moderation advised.",
    ),
    STAGE_1_HYPERTENSION(
        label = "Stage 1 Hypertension",
        clinicalAdvice = "Systolic 130-139 or Diastolic 80-89 mmHg. Consider DASH diet, exercise, and clinical consultation.",
    ),
    STAGE_2_HYPERTENSION(
        label = "Stage 2 Hypertension",
        clinicalAdvice = "Systolic >= 140 or Diastolic >= 90 mmHg. Clinical medical evaluation and therapy recommended.",
    ),
    HYPERTENSIVE_CRISIS(
        label = "Hypertensive Crisis",
        clinicalAdvice = "Systolic > 180 and/or Diastolic > 120 mmHg. Seek immediate medical attention if symptomatic.",
    ),
}

data class BloodPressure(
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int? = null,
) {
    init {
        require(systolic in 50..300) { "Systolic pressure $systolic is outside plausible range (50-300)" }
        require(diastolic in 30..200) { "Diastolic pressure $diastolic is outside plausible range (30-200)" }
        if (pulse != null) {
            require(pulse in 30..250) { "Pulse rate $pulse is outside plausible range (30-250)" }
        }
    }

    /**
     * Mean Arterial Pressure (MAP) represents perfusion pressure across organ vascular beds.
     * Standard formula: MAP = (2 * DBP + SBP) / 3
     */
    val meanArterialPressure: Double
        get() = (2.0 * diastolic + systolic) / 3.0

    /**
     * Pulse Pressure = SBP - DBP (stiffness indicator, normal 40-50 mmHg).
     */
    val pulsePressure: Int
        get() = systolic - diastolic

    /**
     * AHA / ACC 2017 Clinical Practice Guidelines classification.
     */
    val stage: BloodPressureStage
        get() = when {
            systolic > 180 || diastolic > 120 -> BloodPressureStage.HYPERTENSIVE_CRISIS
            systolic >= 140 || diastolic >= 90 -> BloodPressureStage.STAGE_2_HYPERTENSION
            systolic >= 130 || diastolic >= 80 -> BloodPressureStage.STAGE_1_HYPERTENSION
            systolic in 120..129 && diastolic < 80 -> BloodPressureStage.ELEVATED
            else -> BloodPressureStage.NORMAL
        }
}

enum class GlucoseMeasurementContext(val label: String) {
    FASTING("Fasting (8h+ no food)"),
    POSTPRANDIAL("Postprandial (2h post-meal)"),
    RANDOM("Random / Casual"),
}

enum class GlucoseCategory(val label: String) {
    NORMAL("Normal"),
    PREDIABETES("Prediabetes / Impaired"),
    DIABETES("Diabetes Range"),
    LOW("Hypoglycemic (< 70 mg/dL)"),
}

data class BloodGlucose(
    val valueMgDl: Double,
    val context: GlucoseMeasurementContext = GlucoseMeasurementContext.FASTING,
) {
    init {
        require(valueMgDl in 20.0..800.0) { "Blood glucose value $valueMgDl is outside plausible range (20-800 mg/dL)" }
    }

    /**
     * Converts mg/dL to international standard mmol/L (factor: 18.0182).
     */
    val valueMmolL: Double
        get() = valueMgDl / 18.0182

    /**
     * Clinical categorization aligned with American Diabetes Association (ADA) criteria.
     */
    val category: GlucoseCategory
        get() = when {
            valueMgDl < 70.0 -> GlucoseCategory.LOW
            context == GlucoseMeasurementContext.FASTING -> when {
                valueMgDl < 100.0 -> GlucoseCategory.NORMAL
                valueMgDl <= 125.0 -> GlucoseCategory.PREDIABETES
                else -> GlucoseCategory.DIABETES
            }
            context == GlucoseMeasurementContext.POSTPRANDIAL -> when {
                valueMgDl < 140.0 -> GlucoseCategory.NORMAL
                valueMgDl <= 199.0 -> GlucoseCategory.PREDIABETES
                else -> GlucoseCategory.DIABETES
            }
            else -> when { // RANDOM
                valueMgDl < 140.0 -> GlucoseCategory.NORMAL
                valueMgDl <= 199.0 -> GlucoseCategory.PREDIABETES
                else -> GlucoseCategory.DIABETES
            }
        }
}

data class VitalsEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val timestamp: Instant = Instant.now(),
    val bloodPressure: BloodPressure? = null,
    val bloodGlucose: BloodGlucose? = null,
    val ketonesMmolL: Double? = null,
    val bodyTemperatureC: Double? = null,
    val spo2Percent: Int? = null,
    val notes: String = "",
) {
    init {
        if (ketonesMmolL != null) {
            require(ketonesMmolL in 0.0..15.0) { "Ketones value must be between 0 and 15 mmol/L" }
        }
        if (bodyTemperatureC != null) {
            require(bodyTemperatureC in 30.0..45.0) { "Body temperature must be between 30 and 45 °C" }
        }
        if (spo2Percent != null) {
            require(spo2Percent in 50..100) { "SpO2 percentage must be between 50 and 100%" }
        }
    }

    val bodyTemperatureF: Double?
        get() = bodyTemperatureC?.let { (it * 9.0 / 5.0) + 32.0 }
}

data class DoctorConsultation(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val doctorName: String,
    val specialty: String,
    val reasonForVisit: String,
    val diagnosisNotes: String,
    val prescribedMedications: String = "",
    val recommendedTests: String = "",
    val followUpPlan: String = "",
    val createdAt: Instant = Instant.now(),
) {
    init {
        require(doctorName.isNotBlank()) { "Doctor name cannot be blank" }
        require(specialty.isNotBlank()) { "Specialty cannot be blank" }
        require(reasonForVisit.isNotBlank()) { "Reason for visit cannot be blank" }
    }
}
