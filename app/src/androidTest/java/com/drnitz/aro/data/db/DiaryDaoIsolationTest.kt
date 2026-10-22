package com.drnitz.aro.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The heart of Task 5: proving that **yesterday and today never mix**.
 *
 * Every isolation bug in a diary app comes from one of three mistakes — a query that
 * forgot its day filter, a date computed at the wrong moment, or a row written under
 * the wrong day. Each is exercised here against real SQLite.
 */
@RunWith(AndroidJUnit4::class)
class DiaryDaoIsolationTest : DatabaseTestBase() {

    private val dao get() = db.diaryDao()

    @Test
    fun todayAndYesterdayAreReadIndependently() = runBlocking {
        dao.upsertFoods(
            listOf(
                food("t1", today, MealSlot.BREAKFAST, calories = 400.0),
                food("t2", today, MealSlot.LUNCH, calories = 600.0),
                food("y1", yesterday, MealSlot.BREAKFAST, calories = 250.0),
            ),
        )

        assertEquals(2, dao.countFoodsForDay(today.toEpochDay()))
        assertEquals(1, dao.countFoodsForDay(yesterday.toEpochDay()))

        val todayIds = dao.observeFoodsForDay(today.toEpochDay()).first().map { it.id }
        val yesterdayIds = dao.observeFoodsForDay(yesterday.toEpochDay()).first().map { it.id }

        assertEquals(listOf("t1", "t2"), todayIds)
        assertEquals(listOf("y1"), yesterdayIds)
        assertTrue(todayIds.intersect(yesterdayIds.toSet()).isEmpty())
    }

    @Test
    fun dayTotalsOnlyIncludeThatDay() = runBlocking {
        dao.upsertFoods(
            listOf(
                food("t1", today, calories = 400.0, protein = 30.0),
                food("y1", yesterday, calories = 250.0, protein = 20.0),
            ),
        )

        val todayTotals = dao.observeDayTotals(today.toEpochDay()).first()
        val yesterdayTotals = dao.observeDayTotals(yesterday.toEpochDay()).first()

        assertEquals(400.0, todayTotals.calories!!, 0.0001)
        assertEquals(30.0, todayTotals.protein!!, 0.0001)
        assertEquals(250.0, yesterdayTotals.calories!!, 0.0001)
        assertEquals(20.0, yesterdayTotals.protein!!, 0.0001)
    }

    @Test
    fun anEmptyDayReportsNullsRatherThanZeroes() = runBlocking {
        val totals = dao.observeDayTotals(today.toEpochDay()).first()
        assertTrue("nothing logged must not look like a logged zero", totals.isEmptyDay)
        assertNull(totals.calories)
        assertFalse(totals.toNutrientTotals()[NutrientKey.CALORIES].isReported)
    }

    @Test
    fun aNeighbouringDayStaysEmptyWhenOnlyTodayIsLogged() = runBlocking {
        dao.upsertFood(food("t1", today))
        assertTrue(dao.observeDayTotals(yesterday.toEpochDay()).first().isEmptyDay)
        assertTrue(dao.observeDayTotals(dayBeforeYesterday.toEpochDay()).first().isEmptyDay)
        assertFalse(dao.observeDayTotals(today.toEpochDay()).first().isEmptyDay)
    }

    @Test
    fun aRangeQueryGroupsEachDaySeparately() = runBlocking {
        dao.upsertFoods(
            listOf(
                food("t1", today, calories = 400.0),
                food("y1", yesterday, calories = 500.0),
                food("y2", yesterday, calories = 300.0),
                food("d1", dayBeforeYesterday, calories = 200.0),
            ),
        )

        val rows = dao.observeTotalsBetween(
            yesterday.toEpochDay(),
            today.toEpochDay(),
        ).first()

        assertEquals(2, rows.size)
        assertEquals(yesterday, rows[0].date)
        assertEquals(800.0, rows[0].calories!!, 0.0001)
        assertEquals(today, rows[1].date)
        assertEquals(400.0, rows[1].calories!!, 0.0001)
    }

    @Test
    fun replacingADayLeavesNeighbouringDaysUntouched() = runBlocking {
        dao.upsertFoods(
            listOf(
                food("y1", yesterday, calories = 250.0),
                food("t1", today),
                food("t2", today),
            ),
        )

        dao.replaceDayFoods(today.toEpochDay(), listOf(food("t3", today, calories = 999.0)))

        assertEquals(1, dao.countFoodsForDay(today.toEpochDay()))
        assertEquals(999.0, dao.observeDayTotals(today.toEpochDay()).first().calories!!, 0.0001)
        assertEquals(1, dao.countFoodsForDay(yesterday.toEpochDay()))
        assertEquals(250.0, dao.observeDayTotals(yesterday.toEpochDay()).first().calories!!, 0.0001)
    }

    @Test
    fun deletingADayLeavesNoRowsBehind() = runBlocking {
        dao.upsertFoods(listOf(food("t1", today), food("t2", today)))
        assertEquals(2, dao.deleteAllFoodsForDay(today.toEpochDay()))
        assertEquals(0, dao.countFoodsForDay(today.toEpochDay()))
        assertTrue(dao.observeDayTotals(today.toEpochDay()).first().isEmptyDay)
    }

    @Test
    fun movingFoodBetweenDaysMovesRatherThanCopies() = runBlocking {
        dao.upsertFood(food("f1", today))

        dao.moveFood("f1", yesterday.toEpochDay(), MealSlot.DINNER)

        assertEquals(0, dao.countFoodsForDay(today.toEpochDay()))
        assertEquals(1, dao.countFoodsForDay(yesterday.toEpochDay()))
        val moved = dao.observeFoodsForDay(yesterday.toEpochDay()).first().single()
        assertEquals("f1", moved.id)
        assertEquals(MealSlot.DINNER, moved.mealSlot)
    }

    @Test
    fun unreportedNutrientsSurviveTheRoundTripThroughSqlite() = runBlocking {
        dao.upsertFood(food("f1", today, potassium = null))

        val stored = dao.findFood("f1")!!
        val totals = stored.toNutrientTotals()

        assertFalse("a missing potassium value must not become 0", totals[NutrientKey.POTASSIUM].isReported)
        assertFalse(totals[NutrientKey.FIBER].isReported)
        assertTrue(totals[NutrientKey.SODIUM].isReported)
    }

    @Test
    fun aReportedZeroStaysZero() = runBlocking {
        dao.upsertFood(food("f1", today))
        val stored = dao.findFood("f1")!!
        assertTrue(stored.toNutrientTotals()[NutrientKey.CARBS].isReported)
        assertEquals(0.0, stored.toNutrientTotals()[NutrientKey.CARBS].value!!, 0.0001)
    }

    @Test
    fun upsertUpdatesInPlaceRatherThanDuplicating() = runBlocking {
        dao.upsertFood(food("f1", today, calories = 100.0))
        dao.upsertFood(food("f1", today, calories = 250.0))

        assertEquals(1, dao.countFoodsForDay(today.toEpochDay()))
        assertEquals(250.0, dao.observeDayTotals(today.toEpochDay()).first().calories!!, 0.0001)
    }

    @Test
    fun mealsAreFilteredWithinTheDay() = runBlocking {
        dao.upsertFoods(
            listOf(
                food("b1", today, MealSlot.BREAKFAST),
                food("l1", today, MealSlot.LUNCH),
                food("b2", yesterday, MealSlot.BREAKFAST),
            ),
        )

        val breakfast = dao.observeFoodsForMeal(today.toEpochDay(), MealSlot.BREAKFAST).first()
        assertEquals(listOf("b1"), breakfast.map { it.id })
    }

    @Test
    fun recentDaysListsOnlyDaysWithContentNewestFirst() = runBlocking {
        dao.upsertFoods(listOf(food("d1", dayBeforeYesterday), food("t1", today)))
        dao.upsertDay(dayRow(today))

        val recent = dao.observeRecentDays(10).first()
        assertEquals(listOf(today.toEpochDay(), dayBeforeYesterday.toEpochDay()), recent)
        assertFalse("an untouched day must not appear", yesterday.toEpochDay() in recent)
    }

    @Test
    fun dayStateRoundTrips() = runBlocking {
        dao.upsertDay(dayRow(today, waterMl = 750, notes = "felt strong"))
        val stored = dao.observeDay(today.toEpochDay()).first()
        assertNotNull(stored)
        assertEquals(750, stored!!.waterMl)
        assertEquals("felt strong", stored.notes)

        // Neighbouring days must remain untouched.
        assertNull(dao.observeDay(yesterday.toEpochDay()).first())
    }

    @Test
    fun dayStateUpsertDoesNotCreateDuplicateRows() = runBlocking {
        dao.upsertDay(dayRow(today, waterMl = 250))
        dao.upsertDay(dayRow(today, waterMl = 500, notes = "updated"))

        val stored = dao.observeDay(today.toEpochDay()).first()!!
        assertEquals(500, stored.waterMl)
        assertEquals("updated", stored.notes)
    }
}