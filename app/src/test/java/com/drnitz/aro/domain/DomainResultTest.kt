package com.drnitz.aro.domain

import com.drnitz.aro.domain.model.ActivityLevel
import com.drnitz.aro.domain.model.Goal
import com.drnitz.aro.domain.model.MacroPreset
import com.drnitz.aro.domain.model.Sex
import com.drnitz.aro.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainResultTest {

    @Test
    fun `success carries the value`() {
        val result = DomainResult.Success(42)
        assertTrue(result.isSuccess)
        assertEquals(42, result.valueOrNull())
    }

    @Test
    fun `failure carries a reason and a human readable message`() {
        val result = DomainResult.Failure(ValidationFailure.MISSING_VALUE, "Enter your weight")
        assertFalse(result.isSuccess)
        assertNull(result.valueOrNull())
        assertEquals(ValidationFailure.MISSING_VALUE, result.reason)
        assertEquals("Enter your weight", result.message)
    }

    @Test
    fun `of converts a thrown range error into a failure`() {
        val result = DomainResult.of { require(false) { "out of range" } }
        assertEquals(ValidationFailure.OUT_OF_RANGE, failureOf(result).reason)
        assertEquals("out of range", failureOf(result).message)
    }

    @Test
    fun `validation failure reasons are all distinguishable`() {
        assertEquals(3, ValidationFailure.entries.size)
        assertEquals(
            setOf(ValidationFailure.MISSING_VALUE, ValidationFailure.OUT_OF_RANGE, ValidationFailure.UNSUPPORTED),
            ValidationFailure.entries.toSet(),
        )
    }

    @Test
    fun `profile validation accepts the shipped defaults`() {
        assertTrue(UserProfile.DEFAULT.validationErrors().isEmpty())
        assertTrue(Fixtures.MALE.validationErrors().isEmpty())
        assertTrue(Fixtures.FEMALE.validationErrors().isEmpty())
    }

    @Test
    fun `profile validation reports every problem it finds`() {
        val broken = UserProfile(
            ageYears = 5,
            weightKg = 500.0,
            heightCm = 10.0,
            targetWeightKg = 400.0,
        )
        val errors = broken.validationErrors()
        assertTrue(errors.size >= 3)
        assertTrue(errors.any { it.startsWith("Age") })
        assertTrue(errors.any { it.startsWith("Weight") })
        assertTrue(errors.any { it.startsWith("Height") })
    }

    @Test
    fun `profile exposure helpers are derived not stored`() {
        assertEquals(1.78, Fixtures.MALE.heightMeters, 0.0001)
        assertEquals(5.0, Fixtures.MALE.weightDeltaKg, 0.0001)   // 75 -> 70, still to lose
        assertEquals(2.0, Fixtures.FEMALE.weightDeltaKg, 0.0001) // 60 -> 58
    }

    @Test
    fun `activity multipliers match the published factors`() {
        assertEquals(1.2, ActivityLevel.SEDENTARY.tdeeMultiplier, 0.0001)
        assertEquals(1.375, ActivityLevel.LIGHT.tdeeMultiplier, 0.0001)
        assertEquals(1.55, ActivityLevel.MODERATE.tdeeMultiplier, 0.0001)
        assertEquals(1.725, ActivityLevel.VERY_ACTIVE.tdeeMultiplier, 0.0001)
        assertEquals(1.9, ActivityLevel.EXTRA_ACTIVE.tdeeMultiplier, 0.0001)
    }

    @Test
    fun `goal offsets mirror the web app`() {
        assertEquals(-500, Goal.AGGRESSIVE_CUT.calorieAdjustment)
        assertEquals(-250, Goal.MILD_CUT.calorieAdjustment)
        assertEquals(0, Goal.MAINTAIN.calorieAdjustment)
        assertEquals(250, Goal.LEAN_BULK.calorieAdjustment)
        assertEquals(500, Goal.HYPERTROPHY_BULK.calorieAdjustment)
    }

    @Test
    fun `sex offsets match mifflin st jeor`() {
        assertEquals(5.0, Sex.MALE.mifflinStJeorOffset, 0.0001)
        assertEquals(-161.0, Sex.FEMALE.mifflinStJeorOffset, 0.0001)
    }

    @Test
    fun `assertClose helper detects drift`() {
        assertClose(1.0, 1.01, 0.05)
        try {
            assertClose(1.0, 1.2, 0.05)
            throw AssertionError("assertClose should have failed")
        } catch (expected: AssertionError) {
            assertTrue(expected.message!!.contains("1.2"))
        }
    }
}