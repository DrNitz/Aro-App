package com.drnitz.aro.data.network

import com.drnitz.aro.domain.model.CommonServing
import com.drnitz.aro.domain.model.CuratedFoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Open Food Facts Live API Client.
 * Queries barcode and text search with graceful fallbacks.
 * Complies with Open Food Facts User-Agent identification guidelines.
 */
class OpenFoodFactsClient(
    private val userAgent: String = USER_AGENT,
    private val timeoutMs: Int = 4000,
) {
    companion object {
        const val USER_AGENT = "AroHealthApp/1.0 (https://github.com/DrNitz/Aro; contact@arohealth.org)"
        private val BARCODE_URLS = listOf(
            "https://world.openfoodfacts.org/api/v2/product/%s.json",
            "https://world.openfoodfacts.org/api/v0/product/%s.json",
        )
        private val SEARCH_URLS = listOf(
            "https://world.openfoodfacts.org/cgi/search.pl?search_terms=%s&search_simple=1&action=process&json=1&page_size=%d",
        )
    }

    private val barcodeCache = java.util.concurrent.ConcurrentHashMap<String, CuratedFoodItem>()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun getProductByBarcode(barcode: String): CuratedFoodItem? = withContext(Dispatchers.IO) {
        val clean = barcode.trim()
        if (clean.isBlank()) return@withContext null

        val cached = barcodeCache[clean]
        if (cached != null) return@withContext cached

        for (template in BARCODE_URLS) {
            try {
                val urlStr = String.format(template, URLEncoder.encode(clean, "UTF-8"))
                val responseText = executeGet(urlStr) ?: continue
                val root = json.parseToJsonElement(responseText).jsonObject
                val status = root["status"]?.jsonPrimitive?.intOrNull ?: 0
                val product = root["product"]?.jsonObject
                if (status == 1 && product != null) {
                    val normalized = normalizeOffProduct(product, clean)
                    if (normalized != null) {
                        barcodeCache[clean] = normalized
                        return@withContext normalized
                    }
                }
            } catch (_: Exception) {
                // Try next endpoint
            }
        }
        null
    }

    suspend fun searchProducts(query: String, pageSize: Int = 24): List<CuratedFoodItem> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(clean, "UTF-8")

        for (template in SEARCH_URLS) {
            try {
                val urlStr = String.format(template, encoded, pageSize)
                val responseText = executeGet(urlStr) ?: continue
                val root = json.parseToJsonElement(responseText).jsonObject
                val products = root["products"]?.jsonArray ?: root["hits"]?.jsonArray
                if (products != null && products.isNotEmpty()) {
                    val results = products.mapNotNull { element ->
                        val obj = element as? JsonObject ?: return@mapNotNull null
                        normalizeOffProduct(obj, null)
                    }.filter { item ->
                        item.calories > 0 || item.protein > 0 || item.carbs > 0 || item.fat > 0
                    }
                    if (results.isNotEmpty()) return@withContext results
                }
            } catch (_: Exception) {
                // Try next endpoint
            }
        }
        emptyList()
    }

    private fun executeGet(urlStr: String): String? {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.setRequestProperty("User-Agent", userAgent)
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Normalizes an Open Food Facts product JSON object into a CuratedFoodItem.
     */
    fun normalizeOffProduct(product: JsonObject, fallbackCode: String?): CuratedFoodItem? {
        val nutriments = product["nutriments"]?.jsonObject ?: JsonObject(emptyMap())

        // Extract calories: energy-kcal_100g, energy-kcal, energy-kcal_value, or energy_100g / 4.184
        val calories = extractDouble(nutriments, "energy-kcal_100g")
            ?: extractDouble(nutriments, "energy-kcal")
            ?: extractDouble(nutriments, "energy-kcal_value")
            ?: nutriments["energy_100g"]?.jsonPrimitive?.doubleOrNull?.let { it / 4.184 }
            ?: nutriments["energy"]?.jsonPrimitive?.doubleOrNull?.let { it / 4.184 }
            ?: 0.0

        val protein = extractDouble(nutriments, "proteins_100g") ?: extractDouble(nutriments, "proteins") ?: 0.0
        val carbs = extractDouble(nutriments, "carbohydrates_100g") ?: extractDouble(nutriments, "carbohydrates") ?: 0.0
        val fat = extractDouble(nutriments, "fat_100g") ?: extractDouble(nutriments, "fat") ?: 0.0
        val saturatedFat = extractDouble(nutriments, "saturated-fat_100g") ?: extractDouble(nutriments, "saturated-fat") ?: 0.0
        val fiber = extractDouble(nutriments, "fiber_100g") ?: extractDouble(nutriments, "fiber") ?: 0.0
        val sugar = extractDouble(nutriments, "sugars_100g") ?: extractDouble(nutriments, "sugars") ?: 0.0

        // Sodium (convert g to mg if needed)
        val sodiumRaw = extractDouble(nutriments, "sodium_100g") ?: extractDouble(nutriments, "sodium")
        val saltRaw = extractDouble(nutriments, "salt_100g") ?: extractDouble(nutriments, "salt")
        val sodiumUnit = nutriments["sodium_unit"]?.jsonPrimitive?.content?.lowercase()
        val sodium = when {
            sodiumRaw != null -> when (sodiumUnit) {
                "mg" -> sodiumRaw
                "g" -> sodiumRaw * 1000.0
                else -> if (sodiumRaw > 20.0) sodiumRaw else sodiumRaw * 1000.0
            }
            saltRaw != null -> saltRaw * 400.0 // salt is ~40% sodium (salt g to sodium mg: salt_g * 0.4 * 1000 = * 400)
            else -> 0.0
        }

        // Micronutrients (mg)
        val potassiumRaw = extractDouble(nutriments, "potassium_100g") ?: extractDouble(nutriments, "potassium")
        val potassiumUnit = nutriments["potassium_unit"]?.jsonPrimitive?.content?.lowercase()
        val potassium = when {
            potassiumRaw == null -> 0.0
            potassiumUnit == "mg" -> potassiumRaw
            potassiumUnit == "g" -> potassiumRaw * 1000.0
            potassiumRaw > 20.0 -> potassiumRaw
            else -> potassiumRaw * 1000.0
        }

        val calciumRaw = extractDouble(nutriments, "calcium_100g") ?: extractDouble(nutriments, "calcium")
        val calciumUnit = nutriments["calcium_unit"]?.jsonPrimitive?.content?.lowercase()
        val calcium = when {
            calciumRaw == null -> 0.0
            calciumUnit == "mg" -> calciumRaw
            calciumUnit == "g" -> calciumRaw * 1000.0
            calciumRaw > 20.0 -> calciumRaw
            else -> calciumRaw * 1000.0
        }

        val magnesiumRaw = extractDouble(nutriments, "magnesium_100g") ?: extractDouble(nutriments, "magnesium")
        val magnesiumUnit = nutriments["magnesium_unit"]?.jsonPrimitive?.content?.lowercase()
        val magnesium = when {
            magnesiumRaw == null -> 0.0
            magnesiumUnit == "mg" -> magnesiumRaw
            magnesiumUnit == "g" -> magnesiumRaw * 1000.0
            magnesiumRaw > 20.0 -> magnesiumRaw
            else -> magnesiumRaw * 1000.0
        }

        val code = product["code"]?.jsonPrimitive?.content ?: fallbackCode ?: ""
        val id = "off-${code.ifEmpty { java.util.UUID.randomUUID().toString() }}"

        val name = product["product_name"]?.jsonPrimitive?.content
            ?: product["product_name_en"]?.jsonPrimitive?.content
            ?: product["generic_name"]?.jsonPrimitive?.content
            ?: "Unknown Product"

        val brand = product["brands"]?.jsonPrimitive?.content
            ?: (product["brands_tags"] as? JsonArray)?.firstOrNull()?.jsonPrimitive?.content
            ?: "Open Food Facts"

        val category = (product["categories_tags"] as? JsonArray)?.firstOrNull()?.jsonPrimitive?.content
            ?.removePrefix("en:")
            ?.replace('-', ' ')
            ?: "Packaged Food"

        val servingQty = product["serving_quantity"]?.jsonPrimitive?.doubleOrNull ?: 100.0
        val servingSizeStr = product["serving_size"]?.jsonPrimitive?.content

        val commonServing = CommonServing(
            name = if (!servingSizeStr.isNullOrBlank()) "1 Serving ($servingSizeStr)" else "1 Serving (${servingQty.toInt()}g)",
            grams = if (servingQty > 0) servingQty else 100.0,
        )

        val nutriscore = product["nutriscore_grade"]?.jsonPrimitive?.content?.lowercase()
            ?: product["nutrition_grades"]?.jsonPrimitive?.content?.lowercase()

        val nova = product["nova_group"]?.jsonPrimitive?.intOrNull
            ?: product["nova_groups"]?.jsonPrimitive?.intOrNull

        val image = product["image_front_small_url"]?.jsonPrimitive?.content
            ?: product["image_front_url"]?.jsonPrimitive?.content
            ?: product["image_url"]?.jsonPrimitive?.content

        return CuratedFoodItem(
            id = id,
            code = code,
            name = name,
            brand = brand,
            category = category,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            saturatedFat = saturatedFat,
            fiber = fiber,
            sugar = sugar,
            sodium = sodium,
            potassium = potassium,
            calcium = calcium,
            magnesium = magnesium,
            servingSize = 100.0,
            servingUnit = "g",
            commonServing = commonServing,
            nutriscore = nutriscore?.takeIf { it in listOf("a", "b", "c", "d", "e") },
            nova = nova?.takeIf { it in 1..4 },
            image = image,
        )
    }

    private fun extractDouble(obj: JsonObject, key: String): Double? {
        val el = obj[key] ?: return null
        return try {
            el.jsonPrimitive.doubleOrNull
        } catch (_: Exception) {
            null
        }
    }
}
