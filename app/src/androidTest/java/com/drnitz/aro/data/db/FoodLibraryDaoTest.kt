package com.drnitz.aro.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drnitz.aro.data.db.entity.CustomFoodEntity
import com.drnitz.aro.data.db.entity.FavoriteFoodEntity
import com.drnitz.aro.data.db.entity.MealTemplateEntity
import com.drnitz.aro.data.db.entity.MealTemplateItemEntity
import com.drnitz.aro.data.db.entity.RecentFoodEntity
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodLibraryDaoTest : DatabaseTestBase() {

    private val dao get() = db.foodLibraryDao()

    private fun recent(id: String, at: Long) = RecentFoodEntity(
        id = id,
        foodId = "off-$id",
        name = "Food $id",
        brand = null,
        grams = 100.0,
        lastUsedAt = at,
        calories = 200.0,
        protein = 10.0,
        carbs = 20.0,
        fat = 5.0,
        saturatedFat = null,
        fiber = null,
        sugar = null,
        sodium = 50.0,
        potassium = null,
        calcium = null,
        magnesium = null,
    )

    @Test
    fun recentsAreCappedInsideTheTransaction() = runBlocking {
        // Log 30 foods against a cap of 24: the oldest six must fall out.
        for (i in 1..30) {
            dao.recordRecentUsage(recent("f$i", at = i.toLong()), maxRecents = 24)
        }

        val stored = dao.observeRecents().first()
        assertEquals(24, stored.size)
        assertEquals("f30", stored.first().id)
        assertEquals("f7", stored.last().id)
        assertFalse("f6 should have been trimmed", stored.any { it.id == "f6" })
    }

    @Test
    fun reLoggingAFoodMovesItToTheTopInsteadOfDuplicating() = runBlocking {
        dao.recordRecentUsage(recent("a", at = 1))
        dao.recordRecentUsage(recent("b", at = 2))
        dao.recordRecentUsage(recent("a", at = 3))

        val stored = dao.observeRecents().first()
        assertEquals(2, stored.size)
        assertEquals("a", stored.first().id)
    }

    @Test
    fun recentsKeepUnreportedNutrientsUnreported() = runBlocking {
        dao.recordRecentUsage(recent("a", at = 1))
        val stored = dao.observeRecents().first().single()
        assertFalse(stored.toNutrientTotals()[NutrientKey.POTASSIUM].isReported)
    }

    @Test
    fun favouritesAreStarredAndUnstarredByFoodId() = runBlocking {
        dao.upsertFavorite(
            FavoriteFoodEntity(
                id = "fv1",
                foodId = "off-1234",
                name = "Greek Yogurt",
                brand = null,
                grams = 150.0,
                createdAt = 1,
                calories = 120.0,
                protein = 15.0,
                carbs = 8.0,
                fat = 2.0,
                saturatedFat = null,
                fiber = null,
                sugar = null,
                sodium = 60.0,
                potassium = null,
                calcium = null,
                magnesium = null,
            ),
        )

        assertTrue(dao.isFavorite("off-1234"))
        assertTrue(dao.observeIsFavorite("off-1234").first())
        assertFalse(dao.isFavorite("off-9999"))

        dao.deleteFavorite("off-1234")
        assertFalse(dao.isFavorite("off-1234"))
    }

    @Test
    fun theSameFoodCannotBeFavouritedTwice() = runBlocking {
        val favorite = FavoriteFoodEntity(
            id = "fv1", foodId = "off-1", name = "Oats", brand = null, grams = 50.0,
            createdAt = 1, calories = 190.0, protein = 6.0, carbs = 33.0, fat = 3.0,
            saturatedFat = null, fiber = 4.0, sugar = null, sodium = 2.0,
            potassium = 150.0, calcium = null, magnesium = null,
        )
        dao.upsertFavorite(favorite)
        dao.upsertFavorite(favorite.copy(id = "fv2"))

        assertEquals(1, dao.observeFavorites().first().size)
    }

    @Test
    fun customFoodsSearchByPrefixCaseInsensitively() = runBlocking {
        dao.upsertCustomFood(
            CustomFoodEntity(
                id = "c1", name = "Homemade Granola", brand = null, defaultGrams = 50.0,
                createdAt = 1, calories = 250.0, protein = 8.0, carbs = 30.0, fat = 10.0,
                saturatedFat = null, fiber = 4.0, sugar = null, sodium = 120.0,
                potassium = null, calcium = null, magnesium = null,
            ),
        )
        dao.upsertCustomFood(
            CustomFoodEntity(
                id = "c2", name = "Chicken Soup", brand = null, defaultGrams = 300.0,
                createdAt = 1, calories = 150.0, protein = 12.0, carbs = 8.0, fat = 4.0,
                saturatedFat = null, fiber = null, sugar = null, sodium = 900.0,
                potassium = null, calcium = null, magnesium = null,
            ),
        )

        assertEquals(1, dao.searchCustomFoods("home").size)
        assertEquals(1, dao.searchCustomFoods("HOME").size)
        assertEquals(1, dao.searchCustomFoods("CHICK").size)
        assertEquals(0, dao.searchCustomFoods("zzz").size)
    }

    /**
     * Regression guard: SQLite's `LIKE` is case-insensitive by default, but adding an
     * explicit `COLLATE NOCASE` to the expression **flips it to case-sensitive** per the
     * SQLite spec. That mistake made "home" stop matching "Homemade" and was only
     * visible against a real SQLite engine.
     */
    @Test
    fun prefixSearchIgnoresCaseInBothDirections() = runBlocking {
        dao.upsertCustomFood(
            CustomFoodEntity(
                id = "c1", name = "Cottage Cheese", brand = null, defaultGrams = 200.0,
                createdAt = 1, calories = 180.0, protein = 25.0, carbs = 6.0, fat = 5.0,
                saturatedFat = null, fiber = null, sugar = null, sodium = 800.0,
                potassium = null, calcium = null, magnesium = null,
            ),
        )
        assertEquals(1, dao.searchCustomFoods("cott").size)
        assertEquals(1, dao.searchCustomFoods("COTT").size)
        assertEquals(1, dao.searchCustomFoods("CoTT").size)
    }

    @Test
    fun starringTheSameFoodTwiceUpdatesRatherThanSilentlyIgnoring() = runBlocking {
        val star = FavoriteFoodEntity(
            id = "fv1", foodId = "off-42", name = "Eggs", brand = null, grams = 100.0,
            createdAt = 1, calories = 143.0, protein = 12.6, carbs = 0.7, fat = 9.5,
            saturatedFat = null, fiber = null, sugar = null, sodium = 142.0,
            potassium = 138.0, calcium = 56.0, magnesium = 12.0,
        )
        dao.starFavorite(star)
        // A different row id for the same food: @Upsert alone would drop this silently.
        dao.starFavorite(star.copy(id = "fv2", name = "Eggs (large)"))

        val stars = dao.observeFavorites().first()
        assertEquals(1, stars.size)
        assertEquals("Eggs (large)", stars.single().name)
    }

    @Test
    fun templatesSaveWithTheirItems() = runBlocking {
        dao.saveTemplate(
            template = MealTemplateEntity("t1", "Usual Breakfast", MealSlot.BREAKFAST, createdAt = 1),
            items = listOf(
                templateItem("oats", 100.0),
                templateItem("banana", 120.0),
            ),
        )

        val stored = dao.findTemplate("t1")
        assertNotNull(stored)
        assertEquals("Usual Breakfast", stored!!.template.name)
        assertEquals(2, stored.items.size)
        assertTrue(stored.items.all { it.templateId == "t1" })
    }

    @Test
    fun reSavingATemplateReplacesItsItems() = runBlocking {
        dao.saveTemplate(
            MealTemplateEntity("t1", "Usual Breakfast", MealSlot.BREAKFAST, createdAt = 1),
            listOf(templateItem("oats", 100.0), templateItem("banana", 120.0)),
        )
        dao.saveTemplate(
            MealTemplateEntity("t1", "Usual Breakfast", MealSlot.BREAKFAST, createdAt = 2),
            listOf(templateItem("oats", 80.0)),
        )

        assertEquals(1, dao.findTemplate("t1")!!.items.size)
    }

    @Test
    fun deletingATemplateCascadesToItsItems() = runBlocking {
        dao.saveTemplate(
            MealTemplateEntity("t1", "Usual Breakfast", MealSlot.BREAKFAST, createdAt = 1),
            listOf(templateItem("oats", 100.0)),
        )
        dao.deleteTemplate("t1")

        assertTrue(dao.observeTemplates().first().isEmpty())
        assertEquals(null, dao.findTemplate("t1"))
    }

    @Test
    fun templatesAreListablePerMeal() = runBlocking {
        dao.saveTemplate(MealTemplateEntity("t1", "Breakfast", MealSlot.BREAKFAST, 1), emptyList())
        dao.saveTemplate(MealTemplateEntity("t2", "Lunch", MealSlot.LUNCH, 2), emptyList())

        val breakfast = dao.observeTemplatesForMeal(MealSlot.BREAKFAST).first()
        assertEquals(1, breakfast.size)
        assertEquals("t1", breakfast.single().id)
    }

    private fun templateItem(foodId: String, grams: Double) = MealTemplateItemEntity(
        templateId = "",
        foodId = foodId,
        name = "Item $foodId",
        brand = null,
        grams = grams,
        calories = 120.0,
        protein = 3.0,
        carbs = 25.0,
        fat = 1.0,
        saturatedFat = null,
        fiber = 3.0,
        sugar = null,
        sodium = 5.0,
        potassium = 200.0,
        calcium = null,
        magnesium = null,
    )
}