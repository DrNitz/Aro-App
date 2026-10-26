package com.drnitz.aro.data.network

import com.drnitz.aro.domain.model.CommonServing
import com.drnitz.aro.domain.model.CuratedFoodItem
import com.drnitz.aro.domain.model.MicronutrientCatalog
import com.drnitz.aro.domain.model.NutrientKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * Open Food Facts live API client.
 *
 * Search uses the modern Search-a-licious endpoint first (the legacy `cgi/search.pl`
 * endpoint now answers with HTTP 503), then falls back to the v2 and legacy APIs. Every
 * request is field-filtered so a search returns a few kilobytes instead of a full product
 * dump, and successful searches/barcodes are cached to respect OFF's rate limits.
 *
 * Missing nutrients are never invented: keys absent from `nutriments` are listed in
 * [CuratedFoodItem.unreportedNutrients] and flow through the domain as
 * `NutrientAmount(null)`, so the audit can say "not reported" instead of "zero".
 */
class OpenFoodFactsClient(
    private val userAgent: String = USER_AGENT,
    private val timeoutMs: Int = 10_000,
) {
    companion object {
        const val USER_AGENT = "AroHealthApp/1.0 (Android; https://github.com/DrNitz/Aro-App)"

        const val UNNAMED_PRODUCT = "Unnamed product"

        private const val KCAL_PER_KJ = 4.184

        /** Field projection keeps search responses small and fast. */
        private val PRODUCT_FIELDS = listOf(
            "code",
            "product_name",
            "product_name_en",
            "abbreviated_product_name",
            "generic_name",
            "brands",
            "quantity",
            "serving_size",
            "serving_quantity",
            "product_quantity",
            "product_quantity_unit",
            "nutriments",
            "nutriscore_grade",
            "nutrition_grades",
            "nova_group",
            "nova_groups",
            "image_front_small_url",
            "image_front_url",
            "image_url",
            "categories_tags",
            "allergens_tags",
            "traces_tags",
            "additives_tags",
            "labels_tags",
            "countries_tags",
            "packaging_tags",
            "ingredients_text",
            "ingredients_text_en",
            "ecoscore_grade",
            "completeness",
            "last_modified_t",
        ).joinToString(",")

        private val BARCODE_URLS = listOf(
            "https://world.openfoodfacts.org/api/v2/product/%s.json?fields=$PRODUCT_FIELDS",
            "https://world.openfoodfacts.org/api/v0/product/%s.json?fields=$PRODUCT_FIELDS",
        )

        private data class SearchSource(val urlTemplate: String, val arrayKey: String)

        private val SEARCH_SOURCES = listOf(
            SearchSource(
                "https://search.openfoodfacts.org/search?q=%s&page_size=%d&fields=$PRODUCT_FIELDS",
                "hits",
            ),
            SearchSource(
                "https://world.openfoodfacts.org/api/v2/search?search_terms=%s&search_simple=1&action=process&page_size=%d&fields=$PRODUCT_FIELDS",
                "products",
            ),
            SearchSource(
                "https://world.openfoodfacts.org/cgi/search.pl?search_terms=%s&search_simple=1&action=process&json=1&page_size=%d",
                "products",
            ),
        )

        /** Tags emitted by OFF carry a language prefix; strip it for display. */
        private val TAG_PREFIX = Regex("^[a-z]{2}:")

        /** Characters that would be interpreted as Search-a-licious query syntax. */
        private val QUERY_SYNTAX = Regex("[\"\\\\:()\\[\\]{}^~*?!|&+/]")
        private val WHITESPACE = Regex("\\s+")
    }

    private class OffHttpException(val status: Int, message: String) : IOException(message)

    private val barcodeCache = ConcurrentHashMap<String, CuratedFoodItem>()
    private val searchCache = ConcurrentHashMap<String, List<CuratedFoodItem>>()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun getProductByBarcode(barcode: String): CuratedFoodItem? = withContext(Dispatchers.IO) {
        val clean = barcode.trim()
        if (clean.isBlank()) return@withContext null

        barcodeCache[clean]?.let { return@withContext it }

        val errors = mutableListOf<String>()
        var gotDefinitiveAnswer = false

        for (template in BARCODE_URLS) {
            try {
                val url = String.format(
                    Locale.US,
                    template,
                    URLEncoder.encode(clean, "UTF-8"),
                )
                val responseText = executeGet(url)
                if (responseText == null) {
                    // HTTP 404: the code definitively does not exist.
                    gotDefinitiveAnswer = true
                    continue
                }
                if (responseText.trimStart().startsWith("<")) {
                    errors += "Open Food Facts returned an unexpected HTML page."
                    continue
                }
                val root = json.parseToJsonElement(responseText).jsonObject
                val status = root["status"]?.jsonPrimitive?.intOrNull ?: 0
                val product = root["product"]?.jsonObject
                if (status == 1 && product != null) {
                    val normalized = normalizeOffProduct(product, clean)
                    if (normalized != null) {
                        barcodeCache[clean] = normalized
                        return@withContext normalized
                    }
                } else {
                    // v2 answers HTTP 200 with status 0 for unknown barcodes.
                    gotDefinitiveAnswer = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: OffHttpException) {
                errors += e.message ?: "HTTP ${e.status}"
            } catch (e: Exception) {
                errors += e.message ?: "Unknown error"
            }
        }

        if (errors.isNotEmpty() && !gotDefinitiveAnswer) {
            throw IOException(errors.first())
        }
        null
    }

    suspend fun searchProducts(query: String, pageSize: Int = 24): List<CuratedFoodItem> =
        withContext(Dispatchers.IO) {
            val clean = sanitizeQuery(query)
            if (clean.length < 2) return@withContext emptyList()

            val cacheKey = "$clean|$pageSize"
            searchCache[cacheKey]?.let { return@withContext it }

            val errors = mutableListOf<String>()
            var decodedAtLeastOnce = false

            for (source in SEARCH_SOURCES) {
                try {
                    val url = String.format(
                        Locale.US,
                        source.urlTemplate,
                        URLEncoder.encode(clean, "UTF-8"),
                        pageSize,
                    )
                    val responseText = executeGet(url) ?: continue
                    if (responseText.trimStart().startsWith("<")) {
                        errors += "Open Food Facts search returned an unexpected HTML page."
                        continue
                    }
                    val root = json.parseToJsonElement(responseText).jsonObject
                    val products = root[source.arrayKey]?.jsonArray ?: continue
                    decodedAtLeastOnce = true

                    val results = products.asSequence()
                        .mapNotNull { it as? JsonObject }
                        .mapNotNull { normalizeOffProduct(it, fallbackCode = null) }
                        .filter { it.hasUsableNutrition && it.name != UNNAMED_PRODUCT }
                        .distinctBy { it.id }
                        .take(pageSize)
                        .toList()

                    if (results.isNotEmpty()) {
                        rememberSearch(cacheKey, results)
                    }
                    return@withContext results
                } catch (e: CancellationException) {
                    throw e
                } catch (e: OffHttpException) {
                    errors += e.message ?: "HTTP ${e.status}"
                } catch (e: Exception) {
                    errors += e.message ?: "Unknown error"
                }
            }

            if (decodedAtLeastOnce) return@withContext emptyList()
            throw IOException(
                errors.firstOrNull()
                    ?: "Open Food Facts search is unavailable right now. Check your connection and try again."
            )
        }

    private fun rememberSearch(key: String, results: List<CuratedFoodItem>) {
        if (searchCache.size >= 40) searchCache.clear()
        searchCache[key] = results
    }

    fun clearSearchCache() = searchCache.clear()

    private fun sanitizeQuery(query: String): String =
        query.trim()
            .replace(QUERY_SYNTAX, " ")
            .replace(WHITESPACE, " ")
            .trim()

    private fun executeGet(urlStr: String): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            when (val code = conn.responseCode) {
                in 200..299 -> conn.inputStream.bufferedReader().use { it.readText() }
                404 -> null
                429 -> throw OffHttpException(
                    429,
                    "Open Food Facts is rate-limiting requests. Please wait a minute and try again.",
                )
                in 500..599 -> throw OffHttpException(
                    code,
                    "Open Food Facts is temporarily unavailable (HTTP $code).",
                )
                else -> throw OffHttpException(code, "Open Food Facts request failed (HTTP $code).")
            }
        } catch (e: OffHttpException) {
            throw e
        } catch (e: IOException) {
            throw IOException("Could not reach Open Food Facts. Check your internet connection.", e)
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Normalizes an Open Food Facts product JSON object into a CuratedFoodItem.
     *
     * Handles both barcode responses (`brands` is a comma-separated string) and
     * Search-a-licious hits (`brands` is an array of strings).
     */
    fun normalizeOffProduct(product: JsonObject, fallbackCode: String?): CuratedFoodItem? {
        val nutriments = product["nutriments"]?.jsonObject ?: JsonObject(emptyMap())

        val proteinG = extractMassGrams(nutriments, "proteins")
        val carbsG = extractMassGrams(nutriments, "carbohydrates")
        val fatG = extractMassGrams(nutriments, "fat")
        val saturatedFatG = extractMassGrams(nutriments, "saturated-fat")
        val fiberG = extractMassGrams(nutriments, "fiber")
        val sugarG = extractMassGrams(nutriments, "sugars")

        // Sodium is canonically reported in grams by OFF. Salt is 40% sodium by mass.
        val sodiumFromSodium = extractMassGrams(nutriments, "sodium")?.times(1_000.0)
        val saltG = extractMassGrams(nutriments, "salt")
        val sodiumMg = sodiumFromSodium ?: saltG?.times(400.0)

        val potassiumMg = extractMassGrams(nutriments, "potassium")?.times(1_000.0)
        val calciumMg = extractMassGrams(nutriments, "calcium")?.times(1_000.0)
        val magnesiumMg = extractMassGrams(nutriments, "magnesium")?.times(1_000.0)

        val labelCalories = extractCalories(nutriments)
        val caloriesDerived = labelCalories == null &&
            proteinG != null && carbsG != null && fatG != null
        val calories = labelCalories ?: if (caloriesDerived) {
            proteinG * 4.0 + carbsG * 4.0 + fatG * 9.0
        } else {
            null
        }

        val code = product.string("code") ?: fallbackCode ?: ""
        val id = "off-${code.ifEmpty { java.util.UUID.randomUUID().toString() }}"

        val name = product.string("product_name")
            ?: product.string("product_name_en")
            ?: product.string("abbreviated_product_name")
            ?: product.string("generic_name")
            ?: UNNAMED_PRODUCT

        val brand = parseBrands(product["brands"])
            ?: (product["brands_tags"] as? JsonArray)?.firstOrNull()?.jsonPrimitive?.contentOrNull
            ?: "Open Food Facts"

        val categoryTags = rawTags(product["categories_tags"])
        val categoryTag = categoryTags.lastOrNull { it.startsWith("en:") } ?: categoryTags.lastOrNull()
        val category = categoryTag?.let(::prettyTag)?.takeIf { it.isNotBlank() } ?: "Packaged Food"

        val servingText = product.string("serving_size")?.let(::normalizeServingText)
        val servingGrams = parseServingGrams(product.double("serving_quantity"), servingText)
        val commonServing = servingGrams?.let { grams ->
            CommonServing(
                name = servingText?.takeIf { it.length <= 32 } ?: "1 serving (${grams.roundToInt()} g)",
                grams = grams,
            )
        }

        val nutriscore = (product.string("nutriscore_grade") ?: product.string("nutrition_grades"))
            ?.lowercase()
            ?.takeIf { it in setOf("a", "b", "c", "d", "e") }
        val nova = product.int("nova_group") ?: product.int("nova_groups")
            ?: product.string("nova_group")?.toIntOrNull()
        val ecoscore = product.string("ecoscore_grade")
            ?.lowercase()
            ?.takeIf { it in setOf("a", "b", "c", "d", "e") }

        val image = product.string("image_front_small_url")
            ?: product.string("image_front_url")
            ?: product.string("image_url")

        val quantity = product.string("quantity")
            ?: product.double("product_quantity")?.let { qty ->
                "${qty.roundToInt()} ${product.string("product_quantity_unit") ?: "g"}"
            }

        val unreported = buildSet {
            if (calories == null) add(NutrientKey.CALORIES.name)
            if (proteinG == null) add(NutrientKey.PROTEIN.name)
            if (carbsG == null) add(NutrientKey.CARBS.name)
            if (fatG == null) add(NutrientKey.FAT.name)
            if (saturatedFatG == null) add(NutrientKey.SATURATED_FAT.name)
            if (fiberG == null) add(NutrientKey.FIBER.name)
            if (sugarG == null) add(NutrientKey.SUGAR.name)
            if (sodiumMg == null) add(NutrientKey.SODIUM.name)
            if (potassiumMg == null) add(NutrientKey.POTASSIUM.name)
            if (calciumMg == null) add(NutrientKey.CALCIUM.name)
            if (magnesiumMg == null) add(NutrientKey.MAGNESIUM.name)
        }

        val estimated = buildSet {
            if (caloriesDerived) add(NutrientKey.CALORIES.name)
            if (sodiumFromSodium == null && sodiumMg != null) add(NutrientKey.SODIUM.name)
        }

        val micronutrients = buildMap {
            MicronutrientCatalog.ALL.forEach { definition ->
                val grams = definition.offKeys.firstNotNullOfOrNull { key ->
                    extractMassGrams(nutriments, key)
                } ?: return@forEach
                val value = grams * definition.scaleFromGrams
                if (value.isFinite() && value >= 0.0) {
                    put(definition.key, roundForStorage(value))
                }
            }
        }

        val unreportedMicros = MicronutrientCatalog.ALL
            .asSequence()
            .map { it.key }
            .filterNot { it in micronutrients }
            .toSet()

        val ingredients = product.string("ingredients_text")
            ?: product.string("ingredients_text_en")

        return CuratedFoodItem(
            id = id,
            code = code.ifEmpty { null },
            name = name,
            brand = brand,
            category = category,
            calories = calories ?: 0.0,
            protein = proteinG ?: 0.0,
            carbs = carbsG ?: 0.0,
            fat = fatG ?: 0.0,
            saturatedFat = saturatedFatG ?: 0.0,
            fiber = fiberG ?: 0.0,
            sugar = sugarG ?: 0.0,
            sodium = sodiumMg ?: 0.0,
            potassium = potassiumMg ?: 0.0,
            calcium = calciumMg ?: 0.0,
            magnesium = magnesiumMg ?: 0.0,
            servingSize = 100.0,
            servingUnit = "g",
            commonServing = commonServing,
            nutriscore = nutriscore,
            nova = nova?.takeIf { it in 1..4 },
            image = image,
            unreportedNutrients = unreported,
            estimatedNutrients = estimated,
            micronutrients = micronutrients,
            unreportedMicronutrients = unreportedMicros,
            quantity = quantity,
            servingSizeText = servingText,
            ingredientsText = ingredients,
            allergens = parseTags(product["allergens_tags"]),
            traces = parseTags(product["traces_tags"]),
            additives = parseTags(product["additives_tags"]),
            labels = parseTags(product["labels_tags"]),
            countries = parseTags(product["countries_tags"]),
            packaging = parseTags(product["packaging_tags"]),
            ecoscore = ecoscore,
            completeness = product.double("completeness")?.coerceIn(0.0, 1.0),
            lastModified = product.string("last_modified_t")?.toLongOrNull()
                ?: product.double("last_modified_t")?.toLong(),
            dataSource = "Open Food Facts",
        )
    }

    /** Energy per 100 g in kcal, or null when the label has no usable energy value. */
    private fun extractCalories(nutriments: JsonObject): Double? {
        nutriments.double("energy-kcal_100g")?.let { return it }
        nutriments.double("energy-kcal")?.let { return it }
        nutriments.double("energy-kcal_value")?.let { return it }

        val kilojoules = nutriments.double("energy-kj_100g")
            ?: nutriments.double("energy-kj")
            ?: nutriments.double("energy_100g")
            ?: nutriments.double("energy")
            ?: nutriments.double("energy_value")
        return kilojoules?.div(KCAL_PER_KJ)
    }

    /**
     * Reads a mass nutrient in grams, the canonical unit for `*_100g` fields.
     *
     * `*_unit` describes the raw `*_value` field, not the normalized `*_100g` field, which
     * is why unit conversion only happens on the value fallback.
     */
    private fun extractMassGrams(nutriments: JsonObject, key: String): Double? {
        nutriments.double("${key}_100g")?.let { return it }
        nutriments.double(key)?.let { return it }

        val raw = nutriments.double("${key}_value") ?: return null
        return when (nutriments.string("${key}_unit")?.lowercase(Locale.US)) {
            "mg" -> raw / 1_000.0
            "µg", "ug", "mcg" -> raw / 1_000_000.0
            "g", null -> raw
            else -> null
        }
    }

    /** OFF often writes "90g" or "2 tbsp (30g)"; a space reads better in the picker. */
    private fun normalizeServingText(raw: String): String =
        raw.replace(Regex("(?<=\\d)(g|ml|kg|l)\\b", RegexOption.IGNORE_CASE), " $1")

    private fun parseServingGrams(quantity: Double?, text: String?): Double? {
        if (quantity != null && quantity > 0.0 && quantity <= 5_000.0) return quantity
        if (text.isNullOrBlank()) return null

        val match = Regex(
            "(\\d+(?:[.,]\\d+)?)\\s*(kg|g|grams?|mg|ml|cl|l|oz|ounces?|lb|pounds?)\\b",
            RegexOption.IGNORE_CASE,
        ).find(text) ?: return null

        val value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val grams = when (match.groupValues[2].lowercase(Locale.US)) {
            "kg" -> value * 1_000.0
            "g", "gram", "grams" -> value
            "mg" -> value / 1_000.0
            "ml", "cl" -> if (match.groupValues[2].equals("cl", true)) value * 10.0 else value
            "l" -> value * 1_000.0
            "oz", "ounce", "ounces" -> value * 28.3495
            "lb", "pound", "pounds" -> value * 453.592
            else -> return null
        }
        return grams.takeIf { it > 0.0 && it <= 5_000.0 }
    }

    private fun parseBrands(element: JsonElement?): String? {
        val brands = when (element) {
            is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            is JsonPrimitive -> element.contentOrNull?.split(',')
            else -> null
        } ?: return null

        return brands
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(2)
            .joinToString(", ")
            .ifBlank { null }
    }

    private fun rawTags(element: JsonElement?): List<String> =
        (element as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    private fun parseTags(element: JsonElement?, limit: Int = 10): List<String> =
        rawTags(element)
            .map(::prettyTag)
            .filter { it.isNotBlank() }
            .distinct()
            .take(limit)

    private fun prettyTag(raw: String): String {
        val tag = raw.replace(TAG_PREFIX, "")
            .replace('-', ' ')
            .replace('_', ' ')
            .trim()
        if (tag.isEmpty()) return ""
        return tag.split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
                }
            }
    }

    private fun roundForStorage(value: Double): Double =
        String.format(Locale.US, "%.4f", value).toDouble()

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    private fun JsonObject.double(key: String): Double? =
        (this[key] as? JsonPrimitive)?.doubleOrNull

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? JsonPrimitive)?.intOrNull
}
