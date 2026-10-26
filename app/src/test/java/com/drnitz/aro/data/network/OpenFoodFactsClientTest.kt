package com.drnitz.aro.data.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenFoodFactsClientTest {

    private val client = OpenFoodFactsClient()
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun normalizeOffProduct_parsesStandardProductCorrectly() {
        val sampleJson = """
        {
            "code": "3017620422003",
            "product_name": "Nutella Hazelnut Spread",
            "brands": "Ferrero",
            "categories_tags": ["en:spreads", "en:hazelnut-spreads"],
            "serving_quantity": 30.0,
            "serving_size": "2 tbsp (30g)",
            "nutriscore_grade": "e",
            "nova_group": 4,
            "image_front_small_url": "https://images.openfoodfacts.org/images/nutella.jpg",
            "nutriments": {
                "energy-kcal_100g": 539,
                "proteins_100g": 6.3,
                "carbohydrates_100g": 57.5,
                "fat_100g": 30.9,
                "saturated-fat_100g": 10.6,
                "fiber_100g": 3.4,
                "sugars_100g": 56.3,
                "sodium_100g": 0.042,
                "calcium_100g": 0.130,
                "magnesium_100g": 0.045
            }
        }
        """.trimIndent()

        val productObj = json.parseToJsonElement(sampleJson).jsonObject
        val food = client.normalizeOffProduct(productObj, fallbackCode = "3017620422003")

        assertNotNull(food)
        food!!
        assertEquals("off-3017620422003", food.id)
        assertEquals("3017620422003", food.code)
        assertEquals("Nutella Hazelnut Spread", food.name)
        assertEquals("Ferrero", food.brand)
        assertEquals("Hazelnut Spreads", food.category)
        assertEquals(539.0, food.calories, 0.01)
        assertEquals(6.3, food.protein, 0.01)
        assertEquals(57.5, food.carbs, 0.01)
        assertEquals(30.9, food.fat, 0.01)
        assertEquals(10.6, food.saturatedFat, 0.01)
        assertEquals(3.4, food.fiber, 0.01)
        assertEquals(56.3, food.sugar, 0.01)
        assertEquals(42.0, food.sodium, 0.1) // 0.042g * 1000 = 42mg
        assertEquals(130.0, food.calcium, 0.1) // 0.130 * 1000 = 130mg
        assertEquals(45.0, food.magnesium, 0.1)
        assertEquals("e", food.nutriscore)
        assertEquals(4, food.nova)
        assertEquals(30.0, food.commonServing?.grams ?: 0.0, 0.01)
        assertEquals("2 tbsp (30 g)", food.servingSizeText)
    }

    @Test
    fun normalizeOffProduct_convertsKiloJoulesAndSaltWhenKcalMissing() {
        val sampleJson = """
        {
            "code": "737628064502",
            "product_name": "Rice Crackers",
            "brands": "Thai Pantry",
            "nutriments": {
                "energy_100g": 1673.6,
                "proteins": 7.0,
                "carbohydrates": 82.0,
                "fat": 2.5,
                "salt_100g": 1.5
            }
        }
        """.trimIndent()

        val productObj = json.parseToJsonElement(sampleJson).jsonObject
        val food = client.normalizeOffProduct(productObj, fallbackCode = null)

        assertNotNull(food)
        food!!
        assertEquals(400.0, food.calories, 1.0) // 1673.6 kJ / 4.184 = 400 kcal
        assertEquals(7.0, food.protein, 0.01)
        assertEquals(82.0, food.carbs, 0.01)
        assertEquals(2.5, food.fat, 0.01)
        assertEquals(600.0, food.sodium, 1.0) // 1.5g salt * 400 = 600mg sodium
    }

    @Test
    fun normalizeOffProduct_respectsExplicitMilligramUnits() {
        val sampleJson = """
        {
            "code": "123456789012",
            "product_name": "Mineral Water",
            "nutriments": {
                "energy-kcal_100g": 0.0,
                "proteins_100g": 0.0,
                "carbohydrates_100g": 0.0,
                "fat_100g": 0.0,
                "sodium_100g": 0.012,
                "calcium_100g": 0.0085,
                "magnesium_100g": 0.0025,
                "potassium_100g": 0.001
            }
        }
        """.trimIndent()

        val productObj = json.parseToJsonElement(sampleJson).jsonObject
        val food = client.normalizeOffProduct(productObj, fallbackCode = null)

        assertNotNull(food)
        food!!
        assertEquals(12.0, food.sodium, 0.01)
        assertEquals(8.5, food.calcium, 0.01)
        assertEquals(2.5, food.magnesium, 0.01)
        assertEquals(1.0, food.potassium, 0.01)
    }

    @Test
    fun normalizeOffProduct_convertsRawValueWhenPer100gFieldIsAbsent() {
        val sampleJson = """
        {
            "code": "123456789013",
            "product_name": "Mineral Water",
            "nutriments": {
                "sodium_value": 12.0,
                "sodium_unit": "mg",
                "calcium_value": 8.5,
                "calcium_unit": "mg",
                "magnesium_value": 2.5,
                "magnesium_unit": "mg",
                "potassium_value": 1.0,
                "potassium_unit": "mg"
            }
        }
        """.trimIndent()

        val food = client.normalizeOffProduct(
            json.parseToJsonElement(sampleJson).jsonObject,
            fallbackCode = null,
        )

        assertNotNull(food)
        food!!
        assertEquals(12.0, food.sodium, 0.01)
        assertEquals(8.5, food.calcium, 0.01)
        assertEquals(2.5, food.magnesium, 0.01)
        assertEquals(1.0, food.potassium, 0.01)
    }

    @Test
    fun normalizeOffProduct_marksMissingNutrientsAsUnreported() {
        val sampleJson = """
        {
            "code": "000000000001",
            "product_name": "Protein Bar",
            "nutriments": {
                "proteins_100g": 20.0,
                "carbohydrates_100g": 40.0,
                "fat_100g": 10.0
            }
        }
        """.trimIndent()

        val food = client.normalizeOffProduct(
            json.parseToJsonElement(sampleJson).jsonObject,
            fallbackCode = null,
        )

        assertNotNull(food)
        food!!
        // Energy is derived from Atwater factors and flagged as estimated.
        assertEquals(330.0, food.calories, 0.01)
        assertTrue("CALORIES" in food.estimatedNutrients)
        assertFalse("CALORIES" in food.unreportedNutrients)

        // Not reported is not the same as zero.
        assertFalse(food.isReported(com.drnitz.aro.domain.model.NutrientKey.POTASSIUM))
        assertTrue("POTASSIUM" in food.unreportedNutrients)
        assertTrue("SODIUM" in food.unreportedNutrients)
        assertTrue("FIBER" in food.unreportedNutrients)
        assertTrue("SATURATED_FAT" in food.unreportedNutrients)

        val per100 = food.per100gNutrients
        assertNull(per100[com.drnitz.aro.domain.model.NutrientKey.POTASSIUM].value)
        assertEquals(0.0, per100[com.drnitz.aro.domain.model.NutrientKey.POTASSIUM].orZero, 0.0)
        assertEquals(20.0, per100.proteinG, 0.01)

        val halfBar = food.toFoodLogEntry(
            meal = com.drnitz.aro.domain.model.MealSlot.SNACKS,
            grams = 50.0,
        ).nutrients
        assertEquals(10.0, halfBar.proteinG, 0.01)
        assertFalse(halfBar[com.drnitz.aro.domain.model.NutrientKey.SODIUM].isReported)
    }

    @Test
    fun normalizeOffProduct_parsesMicronutrientsInDisplayUnits() {
        val sampleJson = """
        {
            "code": "000000000002",
            "product_name": "Fortified Cereal",
            "nutriments": {
                "energy-kcal_100g": 380.0,
                "proteins_100g": 8.0,
                "carbohydrates_100g": 70.0,
                "fat_100g": 5.0,
                "fiber_100g": 6.0,
                "sugars_100g": 20.0,
                "sodium_100g": 0.4,
                "potassium_100g": 0.3,
                "calcium_100g": 0.2,
                "magnesium_100g": 0.05,
                "iron_100g": 0.0000123,
                "vitamin-d_100g": 0.000005,
                "selenium_100g": 0.00002,
                "cholesterol_100g": 0.05
            }
        }
        """.trimIndent()

        val food = client.normalizeOffProduct(
            json.parseToJsonElement(sampleJson).jsonObject,
            fallbackCode = null,
        )

        assertNotNull(food)
        food!!
        assertEquals(0.0123, food.micronutrients["iron"] ?: 0.0, 1e-6)
        assertEquals(5.0, food.micronutrients["vitamin-d"] ?: 0.0, 1e-6)
        assertEquals(20.0, food.micronutrients["selenium"] ?: 0.0, 1e-6)
        assertEquals(50.0, food.micronutrients["cholesterol"] ?: 0.0, 1e-6)
        assertFalse("iron" in food.unreportedMicronutrients)
        assertTrue("zinc" in food.unreportedMicronutrients)
        assertEquals(400.0, food.sodium, 0.01)
        assertEquals(300.0, food.potassium, 0.01)
        assertEquals(200.0, food.calcium, 0.01)
        assertEquals(50.0, food.magnesium, 0.01)
    }

    @Test
    fun normalizeOffProduct_acceptsSearchHitBrandArrayAndServingText() {
        val sampleJson = """
        {
            "code": "5000108236832",
            "product_name": "Oats so simple",
            "brands": ["Quaker", " Quaker Oats"],
            "serving_size": "2 tbsp (30g)",
            "categories_tags": ["en:breakfasts", "en:rolled-oats"],
            "allergens_tags": ["en:gluten"],
            "traces_tags": ["en:milk", "en:soybeans"],
            "additives_tags": ["en:e322"],
            "ingredients_text": "Wholegrain oats, sugar",
            "nutriments": {
                "energy-kcal_100g": 379.0,
                "proteins_100g": 8.9,
                "carbohydrates_100g": 68.0,
                "fat_100g": 6.4,
                "saturated-fat_100g": 1.2,
                "fiber_100g": 8.0,
                "sugars_100g": 19.0,
                "sodium_100g": 0.01
            }
        }
        """.trimIndent()

        val food = client.normalizeOffProduct(
            json.parseToJsonElement(sampleJson).jsonObject,
            fallbackCode = null,
        )

        assertNotNull(food)
        food!!
        assertEquals("Quaker, Quaker Oats", food.brand)
        assertEquals("Rolled Oats", food.category)
        assertEquals(30.0, food.commonServing?.grams ?: 0.0, 0.01)
        assertEquals(listOf("Gluten"), food.allergens)
        assertEquals(listOf("Milk", "Soybeans"), food.traces)
        assertEquals(listOf("E322"), food.additives)
        assertEquals("Wholegrain oats, sugar", food.ingredientsText)
        assertEquals("Open Food Facts", food.dataSource)
    }

    @Test
    fun normalizeOffProduct_doesNotInventAProductName() {
        val sampleJson = """
        {
            "code": "000000000003",
            "nutriments": { "proteins_100g": 1.0 }
        }
        """.trimIndent()

        val food = client.normalizeOffProduct(
            json.parseToJsonElement(sampleJson).jsonObject,
            fallbackCode = null,
        )

        assertEquals(OpenFoodFactsClient.UNNAMED_PRODUCT, food?.name)
    }
}
