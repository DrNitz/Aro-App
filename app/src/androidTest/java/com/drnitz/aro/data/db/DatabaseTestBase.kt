package com.drnitz.aro.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drnitz.aro.data.db.entity.DayEntity
import com.drnitz.aro.data.db.entity.FoodLogEntity
import com.drnitz.aro.domain.model.MealSlot
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Shared in-memory database for the DAO tests.
 *
 * These run on a **real device against real SQLite**, not a JVM mock: index usage,
 * CASCADE behaviour, aggregate queries and transaction semantics are exactly the things
 * that a mocked database would fake, and exactly the things Task 5 has to get right.
 */
@RunWith(AndroidJUnit4::class)
abstract class DatabaseTestBase {

    protected lateinit var db: AroDatabase

    protected val today: LocalDate = LocalDate.of(2026, 10, 18)
    protected val yesterday: LocalDate = today.minusDays(1)
    protected val dayBeforeYesterday: LocalDate = today.minusDays(2)

    @Before
    fun createDatabase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AroDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(Exception::class)
    fun closeDatabase() {
        db.close()
    }

    protected fun food(
        id: String,
        date: LocalDate,
        meal: MealSlot = MealSlot.LUNCH,
        calories: Double? = 500.0,
        protein: Double? = 30.0,
        name: String = "Food $id",
        createdAt: Long = 0,
        potassium: Double? = null,
    ) = FoodLogEntity(
        id = id,
        epochDay = date.toEpochDay(),
        mealSlot = meal,
        name = name,
        brand = null,
        foodId = null,
        grams = 100.0,
        isCustom = false,
        createdAt = createdAt,
        calories = calories,
        protein = protein,
        carbs = 0.0,
        fat = 1.0,
        saturatedFat = null,
        fiber = null,
        sugar = null,
        sodium = 100.0,
        potassium = potassium,
        calcium = null,
        magnesium = null,
    )

    protected fun dayRow(date: LocalDate, waterMl: Int = 0, notes: String = "") =
        DayEntity(
            epochDay = date.toEpochDay(),
            waterMl = waterMl,
            notes = notes,
            createdAt = 0,
            updatedAt = 0,
        )
}