package com.drnitz.aro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class VitalsTest {

    @Test
    fun `BloodPressure correctly categorizes AHA ACC stages`() {
        // Normal: SBP < 120 and DBP < 80
        val normal = BloodPressure(115, 75)
        assertEquals(BloodPressureStage.NORMAL, normal.stage)

        // Elevated: SBP 120-129 and DBP < 80
        val elevated = BloodPressure(125, 75)
        assertEquals(BloodPressureStage.ELEVATED, elevated.stage)

        // Stage 1: SBP 130-139 or DBP 80-89
        val stage1Sys = BloodPressure(135, 78)
        assertEquals(BloodPressureStage.STAGE_1_HYPERTENSION, stage1Sys.stage)

        val stage1Dia = BloodPressure(118, 85)
        assertEquals(BloodPressureStage.STAGE_1_HYPERTENSION, stage1Dia.stage)

        // Stage 2: SBP >= 140 or DBP >= 90
        val stage2Sys = BloodPressure(145, 82)
        assertEquals(BloodPressureStage.STAGE_2_HYPERTENSION, stage2Sys.stage)

        val stage2Dia = BloodPressure(128, 92)
        assertEquals(BloodPressureStage.STAGE_2_HYPERTENSION, stage2Dia.stage)

        // Hypertensive Crisis: SBP > 180 and/or DBP > 120
        val crisisSys = BloodPressure(185, 95)
        assertEquals(BloodPressureStage.HYPERTENSIVE_CRISIS, crisisSys.stage)

        val crisisDia = BloodPressure(135, 125)
        assertEquals(BloodPressureStage.HYPERTENSIVE_CRISIS, crisisDia.stage)
    }

    @Test
    fun `BloodPressure calculates Mean Arterial Pressure and Pulse Pressure accurately`() {
        val bp = BloodPressure(120, 80, pulse = 70)

        // MAP = (2 * 80 + 120) / 3 = 280 / 3 = 93.333...
        assertEquals(93.333, bp.meanArterialPressure, 0.001)

        // Pulse Pressure = 120 - 80 = 40
        assertEquals(40, bp.pulsePressure)
        assertEquals(70, bp.pulse)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `BloodPressure rejects implausible systolic values`() {
        BloodPressure(350, 80)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `BloodPressure rejects implausible diastolic values`() {
        BloodPressure(120, 25)
    }

    @Test
    fun `BloodGlucose categorizes fasting readings accurately according to ADA criteria`() {
        val low = BloodGlucose(65.0, GlucoseMeasurementContext.FASTING)
        assertEquals(GlucoseCategory.LOW, low.category)

        val normal = BloodGlucose(88.0, GlucoseMeasurementContext.FASTING)
        assertEquals(GlucoseCategory.NORMAL, normal.category)

        val prediabetes = BloodGlucose(110.0, GlucoseMeasurementContext.FASTING)
        assertEquals(GlucoseCategory.PREDIABETES, prediabetes.category)

        val diabetes = BloodGlucose(135.0, GlucoseMeasurementContext.FASTING)
        assertEquals(GlucoseCategory.DIABETES, diabetes.category)
    }

    @Test
    fun `BloodGlucose categorizes postprandial readings accurately`() {
        val normal = BloodGlucose(125.0, GlucoseMeasurementContext.POSTPRANDIAL)
        assertEquals(GlucoseCategory.NORMAL, normal.category)

        val prediabetes = BloodGlucose(165.0, GlucoseMeasurementContext.POSTPRANDIAL)
        assertEquals(GlucoseCategory.PREDIABETES, prediabetes.category)

        val diabetes = BloodGlucose(215.0, GlucoseMeasurementContext.POSTPRANDIAL)
        assertEquals(GlucoseCategory.DIABETES, diabetes.category)
    }

    @Test
    fun `BloodGlucose converts mg dL to mmol L correctly`() {
        val bg = BloodGlucose(180.182, GlucoseMeasurementContext.RANDOM)
        assertEquals(10.0, bg.valueMmolL, 0.01)
    }

    @Test
    fun `VitalsEntry converts body temperature to Fahrenheit correctly`() {
        val entry = VitalsEntry(
            date = LocalDate.of(2026, 10, 21),
            bodyTemperatureC = 37.0,
            spo2Percent = 98,
            ketonesMmolL = 1.2,
        )

        assertNotNull(entry.bodyTemperatureF)
        assertEquals(98.6, entry.bodyTemperatureF!!, 0.01)
        assertEquals(98, entry.spo2Percent)
        assertEquals(1.2, entry.ketonesMmolL!!, 0.01)
    }

    @Test
    fun `DoctorConsultation initializes and validates required fields`() {
        val consult = DoctorConsultation(
            date = LocalDate.of(2026, 10, 21),
            doctorName = "Dr. Sarah Jenkins",
            specialty = "Cardiology",
            reasonForVisit = "Annual cardiometabolic audit",
            diagnosisNotes = "Normal sinus rhythm, stage 1 HTN controlled",
            prescribedMedications = "Telmisartan 40mg",
            followUpPlan = "12 weeks repeat BP log",
        )

        assertEquals("Dr. Sarah Jenkins", consult.doctorName)
        assertEquals("Cardiology", consult.specialty)
        assertEquals("Annual cardiometabolic audit", consult.reasonForVisit)
        assertEquals("Telmisartan 40mg", consult.prescribedMedications)
        assertNotNull(consult.createdAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `DoctorConsultation rejects blank doctor name`() {
        DoctorConsultation(
            date = LocalDate.now(),
            doctorName = "   ",
            specialty = "Cardiology",
            reasonForVisit = "Routine",
            diagnosisNotes = "None",
        )
    }
}
