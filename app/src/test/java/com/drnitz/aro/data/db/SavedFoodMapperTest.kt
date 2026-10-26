package com.drnitz.aro.data.db

import com.drnitz.aro.data.db.mapper.toCuratedFoodItem
import com.drnitz.aro.data.db.mapper.toFavoriteEntity
import com.drnitz.aro.data.db.mapper.toRecentEntity
import com.drnitz.aro.domain.model.CommonServing
import com.drnitz.aro.domain.model.CuratedFoodItem
import com.drnitz.aro.domain.model.NutrientKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Favourite/recent rows are nutrient snapshots scaled to the saved portion. These tests
 * pin the round-trip: a 40 g serving must come back as the original per-100 g profile,
 * otherwise the portion dialog would compound the scale factor every time it reopened.
 */
class SavedFoodMapperTest {

    private val oats = CuratedFoodItem(
        id = "curated-oats",
        name = "Rolled Oats",
        brand = "Whole Food / Fresh",
        category = "Grains",
        calories = 389.0,
        protein = 16.9,
        carbs = 66.3,
        fat = 6.9,
        saturatedFat = 1.2,
        fiber = 10.6,
        sugar = 1.0,
        sodium = 2.0,
        potassium = 429.0,
        calcium = 54.0,
        magnesium = 177.0,
        servingSize = 100.0,
        commonServing = CommonServing("1/2 cup dry", 40.0),
        nutriscore = "a",
        nova = 1,
        image = "https://example.test/oats.jpg",
    )

    @Test
    fun `favorite snapshot stores the common serving and recovers per-100g values`() {
        val entity = oats.toFavoriteEntity(createdAt = 1_000L)

        assertEquals("curated-oats", entity.foodId)
        assertEquals("Rolled Oats", entity.name)
        assertEquals(40.0, entity.grams, 1e-9)
        assertEquals(389.0 * 0.40, entity.calories!!, 1e-6)
        assertEquals(16.9 * 0.40, entity.protein!!, 1e-6)

        val restored = entity.toCuratedFoodItem()

        assertEquals("curated-oats", restored.id)
        assertEquals("Rolled Oats", restored.name)
        assertEquals(100.0, restored.servingSize, 1e-9)
        assertEquals(389.0, restored.calories, 1e-6)
        assertEquals(16.9, restored.protein, 1e-6)
        assertEquals(66.3, restored.carbs, 1e-6)
        assertEquals(6.9, restored.fat, 1e-6)
        assertEquals(10.6, restored.fiber, 1e-6)
        assertEquals(429.0, restored.potassium, 1e-6)
    }

    @Test
    fun `open food facts snapshots recover the barcode for full-label reload`() {
        val nutella = oats.copy(
            id = "off-3017620422003",
            code = "3017620422003",
            name = "Nutella",
        )

        val restored = nutella.toFavoriteEntity().toCuratedFoodItem()

        assertEquals("off-3017620422003", restored.id)
        assertEquals("3017620422003", restored.code)
        assertNull(oats.toFavoriteEntity().toCuratedFoodItem().code)
    }

    @Test
    fun `recent snapshot keeps the portion actually logged`() {
        val entity = oats.toRecentEntity(grams = 25.0, lastUsedAt = 2_000L)

        assertEquals(25.0, entity.grams, 1e-9)
        assertEquals(389.0 * 0.25, entity.calories!!, 1e-6)

        val restored = entity.toCuratedFoodItem()

        assertEquals(389.0, restored.calories, 1e-6)
        assertEquals(16.9, restored.protein, 1e-6)
    }

    @Test
    fun `unknown nutrients do not crash and stay unreported after a snapshot`() {
        val sparse = CuratedFoodItem(
            id = "sparse",
            name = "Mystery Food",
            calories = 100.0,
            protein = 1.0,
            carbs = 2.0,
            fat = 3.0,
            unreportedNutrients = setOf("FIBER", "SODIUM", "POTASSIUM", "CALCIUM", "MAGNESIUM"),
        )

        val restored = sparse.toFavoriteEntity().toCuratedFoodItem()

        assertEquals(100.0, restored.calories, 1e-6)
        assertEquals(0.0, restored.fiber, 1e-9)
        assertEquals(0.0, restored.sodium, 1e-9)
        assertFalse(restored.isReported(NutrientKey.FIBER))
        assertNull(restored.per100gNutrients[NutrientKey.FIBER].value)
        assertNull(restored.image)
    }
}
