package com.drnitz.aro.data.db

import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `meal slots round-trip by name`() {
        for (slot in MealSlot.entries) {
            val stored = converters.mealSlotToString(slot)
            assertEquals(slot.name, stored)
            assertEquals(slot, converters.stringToMealSlot(stored))
        }
    }

    @Test
    fun `workout types round-trip by name`() {
        for (type in WorkoutType.entries) {
            assertEquals(type, converters.stringToWorkoutType(converters.workoutTypeToString(type)))
        }
    }

    @Test
    fun `an unknown meal falls back instead of throwing`() {
        assertEquals(MealSlot.SNACKS, converters.stringToMealSlot("BRUNCH"))
    }

    @Test
    fun `an unknown workout type falls back instead of throwing`() {
        assertEquals(WorkoutType.CARDIO, converters.stringToWorkoutType("CALISTHENICS"))
    }

    @Test
    fun `instants round-trip through epoch millis`() {
        val instant = Instant.parse("2026-10-18T07:30:00Z")
        assertEquals(instant, converters.epochMillisToInstant(converters.instantToEpochMillis(instant)))
    }

    @Test
    fun `epoch millis survive the trip exactly`() {
        val millis = 1_768_000_000_123L
        assertEquals(millis, converters.instantToEpochMillis(converters.epochMillisToInstant(millis)))
    }

    @Test
    fun `storing by name not ordinal means reordering an enum is safe`() {
        // If these were stored as ordinals, reordering MealSlot would silently reinterpret
        // every existing row. Names are stable across refactors.
        assertEquals("BREAKFAST", converters.mealSlotToString(MealSlot.BREAKFAST))
        assertEquals(0, MealSlot.entries.indexOf(MealSlot.BREAKFAST))
    }
}