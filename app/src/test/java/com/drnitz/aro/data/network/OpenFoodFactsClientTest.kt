package com.drnitz.aro.data.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertEquals("spreads", food.category)
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
                "sodium_100g": 12.0,
                "sodium_unit": "mg",
                "calcium_100g": 8.5,
                "calcium_unit": "mg",
                "magnesium_100g": 2.5,
                "magnesium_unit": "mg",
                "potassium_100g": 1.0,
                "potassium_unit": "mg"
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
}
