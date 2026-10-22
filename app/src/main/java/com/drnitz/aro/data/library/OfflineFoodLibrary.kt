package com.drnitz.aro.data.library

import android.content.Context
import com.drnitz.aro.domain.model.CuratedFoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream

/**
 * 100% Offline Curated Whole Foods Database.
 * Staple whole foods, lean proteins, fruits, veggies, and fitness essentials.
 */
class OfflineFoodLibrary(
    private val assetStreamProvider: () -> InputStream,
) {
    constructor(context: Context) : this({ context.assets.open("curated_foods.json") })

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Volatile
    private var cachedFoods: List<CuratedFoodItem>? = null

    suspend fun getCuratedFoods(): List<CuratedFoodItem> = cachedFoods ?: withContext(Dispatchers.IO) {
        cachedFoods ?: run {
            val list = assetStreamProvider().use { stream ->
                val content = stream.bufferedReader().readText()
                json.decodeFromString<List<CuratedFoodItem>>(content)
            }
            cachedFoods = list
            list
        }
    }

    fun getCuratedFoodsBlocking(): List<CuratedFoodItem> = cachedFoods ?: synchronized(this) {
        cachedFoods ?: run {
            val list = assetStreamProvider().use { stream ->
                val content = stream.bufferedReader().readText()
                json.decodeFromString<List<CuratedFoodItem>>(content)
            }
            cachedFoods = list
            list
        }
    }

    suspend fun search(query: String = "", category: String? = null): List<CuratedFoodItem> {
        val all = getCuratedFoods()
        val cleanQ = query.trim().lowercase()
        val cleanCat = category?.trim()?.lowercase()?.takeIf { it != "all" }

        return all.filter { food ->
            val matchesQuery = cleanQ.isEmpty() || food.matchesQuery(cleanQ)
            val matchesCategory = cleanCat == null || food.category?.lowercase() == cleanCat
            matchesQuery && matchesCategory
        }
    }

    suspend fun findById(id: String): CuratedFoodItem? {
        return getCuratedFoods().find { it.id == id }
    }

    suspend fun findByBarcode(code: String): CuratedFoodItem? {
        val clean = code.trim()
        if (clean.isEmpty()) return null
        return getCuratedFoods().find { it.code.equals(clean, ignoreCase = true) }
    }

    suspend fun getCategories(): List<String> {
        return getCuratedFoods()
            .mapNotNull { it.category }
            .distinct()
            .sorted()
    }
}
