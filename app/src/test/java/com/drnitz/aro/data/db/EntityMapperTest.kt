package com.drnitz.aro.data.db

import com.drnitz.aro.data.db.mapper.buildDiaryDay
import com.drnitz.aro.data.db.mapper.toDomain
import com.drnitz.aro.data.db.mapper.toEntity
import com.drnitz.aro.domain.model.DiaryDay
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.WorkoutLog
import com.drnitz.aro.domain.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DateProviderTest {

    private val instantAt = { iso: String -> Instant.parse(iso) }

    @Test
    fun `a fixed provider always answers the same day`() {
        val provider = FixedDateProvider(LocalDate.of(2026, 10, 18))
        assertEquals(LocalDate.of(2026, 10, 18), provider.today())
        assertEquals(LocalDate.of(2026, 10, 18), provider.today())
        assertEquals(LocalDate.of(2026, 10, 17), provider.yesterday())
    }

    @Test
    fun `yesterday is exactly one day before today`() {
        val provider = FixedDateProvider(LocalDate.of(2026, 1, 1))
        assertEquals(LocalDate.of(2025, 12, 31), provider.yesterday())
    }

    @Test
    fun `the system provider resolves the local calendar day, not UTC`() {
        // 2026-10-18T20:00Z is already the 19th in Tokyo (UTC+9) but still the 18th in
        // London (UTC+1) — the exact case a shared "today" bug would get wrong.
        val instant = instantAt("2026-10-18T20:00:00Z")
        val tokyo = SystemDateProvider(clock = { instant }, zoneId = ZoneOffset.ofHours(9))
        val london = SystemDateProvider(clock = { instant }, zoneId = ZoneOffset.ofHours(1))
        assertEquals(LocalDate.of(2026, 10, 19), tokyo.today())
        assertEquals(LocalDate.of(2026, 10, 18), london.today())
    }

    @Test
    fun `dayOf files an instant against the provider's zone`() {
        val instant = instantAt("2026-10-18T23:30:00Z")
        val provider = SystemDateProvider(clock = { instant }, zoneId = ZoneOffset.ofHours(9))
        assertEquals(LocalDate.of(2026, 10, 19), provider.dayOf(instant))
    }

    @Test
    fun `dayOf and today agree for the same instant`() {
        val instant = instantAt("2026-10-18T12:00:00Z")
        val provider = SystemDateProvider(clock = { instant }, zoneId = ZoneOffset.UTC)
        assertEquals(provider.today(), provider.dayOf(instant))
    }
}

class DayRangeTest {

    @Test
    fun `a single day range holds exactly one day`() {
        val day = LocalDate.of(2026, 10, 18)
        val range = DayRange.singleDay(day)
        assertEquals(day.toEpochDay(), range.startEpochDay)
        assertEquals(day.toEpochDay(), range.endEpochDay)
        assertEquals(1L, range.dayCount)
        assertTrue(range.contains(day.toEpochDay()))
        assertFalse(range.contains(day.plusDays(1).toEpochDay()))
    }

    @Test
    fun `last days covers an inclusive window`() {
        val today = LocalDate.of(2026, 10, 18)
        val range = DayRange.lastDays(endingOn = today, days = 7)
        assertEquals(7L, range.dayCount)
        assertEquals(today.minusDays(6).toEpochDay(), range.startEpochDay)
        assertEquals(today.toEpochDay(), range.endEpochDay)
    }

    @Test
    fun `between is inclusive at both ends`() {
        val range = DayRange.between(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 18))
        assertEquals(18L, range.dayCount)
        assertTrue(range.contains(LocalDate.of(2026, 10, 1).toEpochDay()))
        assertTrue(range.contains(LocalDate.of(2026, 10, 18).toEpochDay()))
        assertFalse(range.contains(LocalDate.of(2026, 9, 30).toEpochDay()))
    }

    @Test
    fun `toLocalDateRange recovers the original dates`() {
        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 3, 31)
        assertEquals(start to end, DayRange.between(start, end).toLocalDateRange())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an inverted range is rejected`() {
        DayRange(startEpochDay = 200, endEpochDay = 100)
    }

    @Test
    fun `epoch days are monotonic with dates`() {
        // The property the whole isolation guarantee rests on.
        for (offset in 0..400) {
            val a = LocalDate.of(2026, 1, 1).plusDays(offset.toLong())
            val b = LocalDate.of(2026, 1, 1).plusDays((offset + 1).toLong())
            assertTrue("$a should precede $b", a.toEpochDay() < b.toEpochDay())
        }
    }
}

class EntityMapperTest {

    private val day = LocalDate.of(2026, 10, 18)

    @Test
    fun `a food entry round-trips with its unreported nutrients intact`() {
        val entry = FoodLogEntry(
            id = "f1",
            name = "Chicken Breast",
            meal = MealSlot.LUNCH,
            grams = 150.0,
            nutrients = NutrientTotals.of(
                NutrientKey.CALORIES to 372.0,
                NutrientKey.PROTEIN to 69.75,
                NutrientKey.CARBS to 0.0,
                NutrientKey.SODIUM to 111.0,
                // no fiber / sugar / potassium reported
            ),
            brand = null,
            foodId = "off-1234",
        )
        val entity = entry.toEntity(day.toEpochDay(), createdAt = 1_700_000_000_000)
        val back = entity.toDomain()

        assertEquals(entry.id, back.id)
        assertEquals(entry.name, back.name)
        assertEquals(entry.meal, back.meal)
        assertEquals(entry.grams, back.grams, 0.0001)
        assertEquals(entry.foodId, back.foodId)
        assertEquals(372.0, back.nutrients.calories, 0.0001)
        assertEquals(69.75, back.nutrients.proteinG, 0.0001)
        assertFalse(back.nutrients[NutrientKey.POTASSIUM].isReported)
        assertTrue(back.nutrients[NutrientKey.CARBS].isReported)
    }

    @Test
    fun `the entity stores the day it was filed under`() {
        val entry = FoodLogEntry("f", "Oats", MealSlot.BREAKFAST, 100.0, NutrientTotals.EMPTY)
        val entity = entry.toEntity(day.toEpochDay(), createdAt = 0)
        assertEquals(day.toEpochDay(), entity.epochDay)
    }

    @Test
    fun `a workout round-trips its sets in entry order`() {
        val log = WorkoutLog(
            id = "w1",
            type = WorkoutType.STRENGTH,
            exerciseName = "Bench Press",
            loggedAt = Instant.parse("2026-10-18T18:00:00Z"),
            met = 6.0,
            durationMinutes = 45,
            bodyWeightKg = 80.0,
            sets = listOf(
                com.drnitz.aro.domain.model.SetEntry(100.0, 5),
                com.drnitz.aro.domain.model.SetEntry(100.0, 5),
                com.drnitz.aro.domain.model.SetEntry(90.0, 8),
            ),
            category = "Gym & Strength",
            muscle = "Chest",
        )
        val entity = log.toEntity(day.toEpochDay())
        // Written back with reversed set indices, as a shuffled read would return them.
        val withSets = com.drnitz.aro.data.db.entity.WorkoutWithSets(
            workout = entity,
            sets = log.sets.mapIndexed { i, s -> s.toEntity(entity.id, 3 - i) },
        )
        val back = withSets.toDomain()

        assertEquals(log.id, back.id)
        assertEquals(WorkoutType.STRENGTH, back.type)
        assertEquals(log.loggedAt, back.loggedAt)
        assertEquals(log.bodyWeightKg!!, back.bodyWeightKg!!, 0.0001)
        // Ordered by setIndex, not by the order SQLite happened to return them.
        assertEquals(listOf(90.0, 100.0, 100.0), back.sets.map { it.weightKg })
        assertEquals(listOf(8, 5, 5), back.sets.map { it.reps })
    }

    @Test
    fun `cardio distance speed and steps round-trip`() {
        val log = WorkoutLog(
            id = "c1",
            type = WorkoutType.CARDIO,
            exerciseName = "Jogging",
            loggedAt = Instant.parse("2026-10-18T07:00:00Z"),
            met = 8.3,
            durationMinutes = 32,
            distanceKm = 5.24,
            averageSpeedKph = 9.825,
            steps = 6123,
            bodyWeightKg = 80.0,
            category = "Running",
        )
        val back = com.drnitz.aro.data.db.entity.WorkoutWithSets(
            workout = log.toEntity(day.toEpochDay()),
            sets = emptyList(),
        ).toDomain()

        assertEquals(5.24, back.distanceKm!!, 0.0001)
        assertEquals(9.825, back.averageSpeedKph!!, 0.0001)
        assertEquals(6123, back.steps)
    }

    @Test
    fun `older cardio workouts still map without new optional fields`() {
        val entity = com.drnitz.aro.data.db.entity.WorkoutEntity(
            id = "legacy-cardio",
            epochDay = 0,
            type = WorkoutType.CARDIO.name,
            exerciseName = "Walking",
            category = "Walking",
            met = 4.3,
            durationMinutes = 30,
            bodyWeightKg = 80.0,
            muscle = null,
            equipment = null,
            loggedAt = 0,
            createdAt = 0,
        )
        val back = com.drnitz.aro.data.db.entity.WorkoutWithSets(entity, emptyList()).toDomain()
        assertNull(back.distanceKm)
        assertNull(back.averageSpeedKph)
        assertNull(back.steps)
    }

    @Test
    fun `an unknown enum name in the database degrades instead of crashing`() {
        val entity = com.drnitz.aro.data.db.entity.WorkoutEntity(
            id = "w",
            epochDay = 0,
            type = "YOGA_FROM_THE_FUTURE",
            exerciseName = "Something",
            category = "",
            met = 3.0,
            durationMinutes = 30,
            bodyWeightKg = null,
            muscle = null,
            equipment = null,
            loggedAt = 0,
            createdAt = 0,
        )
        val back = com.drnitz.aro.data.db.entity.WorkoutWithSets(entity, emptyList()).toDomain()
        assertEquals(WorkoutType.CARDIO, back.type)
    }
}

class BuildDiaryDayTest {

    private val day = LocalDate.of(2026, 10, 18)

    private fun food(id: String, meal: MealSlot) = FoodLogEntry(
        id = id,
        name = id,
        meal = meal,
        grams = 100.0,
        nutrients = NutrientTotals.of(NutrientKey.CALORIES to 100.0),
    ).toEntity(day.toEpochDay(), createdAt = 0)

    @Test
    fun `meals always come back in breakfast to snacks order`() {
        val diary = buildDiaryDay(
            date = day,
            foods = listOf(food("d", MealSlot.DINNER), food("b", MealSlot.BREAKFAST)),
            workouts = emptyList(),
            day = null,
        )
        assertEquals(
            listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER, MealSlot.SNACKS),
            diary.meals.keys.toList(),
        )
        assertEquals(listOf("b"), diary.meals[MealSlot.BREAKFAST]!!.map { it.id })
        assertEquals(listOf("d"), diary.meals[MealSlot.DINNER]!!.map { it.id })
    }

    @Test
    fun `an untouched day is empty rather than zero-filled`() {
        val diary = buildDiaryDay(day, emptyList(), emptyList(), null)
        assertTrue(diary.isEmpty)
        assertEquals(0, diary.waterMl)
        assertEquals("", diary.notes)
        assertEquals(0, diary.caloriesEaten)
    }

    @Test
    fun `day totals sum every meal`() {
        val diary = buildDiaryDay(
            date = day,
            foods = listOf(
                food("b", MealSlot.BREAKFAST),
                food("l", MealSlot.LUNCH),
                food("s", MealSlot.SNACKS),
            ),
            workouts = emptyList(),
            day = com.drnitz.aro.data.db.entity.DayEntity(day.toEpochDay(), 750, "felt good", 0, 0),
        )
        assertEquals(300, diary.caloriesEaten)
        assertEquals(750, diary.waterMl)
        assertEquals("felt good", diary.notes)
        assertNotNull(diary.nutrientTotals[NutrientKey.CALORIES].value)
    }
}