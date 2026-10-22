package com.drnitz.aro.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.drnitz.aro.data.db.entity.CustomFoodEntity
import com.drnitz.aro.data.db.entity.FavoriteFoodEntity
import com.drnitz.aro.data.db.entity.MealTemplateEntity
import com.drnitz.aro.data.db.entity.MealTemplateItemEntity
import com.drnitz.aro.data.db.entity.MealTemplateWithItems
import com.drnitz.aro.data.db.entity.RecentFoodEntity
import kotlinx.coroutines.flow.Flow

/**
 * Custom foods, favourites, recents and meal templates — the Task 6/8 food library.
 *
 * The recents cap is the interesting one. The web app read the whole recents array out
 * of `localStorage`, truncated it in JavaScript and wrote the whole thing back on every
 * log. Here the cap is enforced by a single indexed statement inside the same
 * transaction as the insert, so it cannot drift and costs one extra pass over at most
 * `MAX_RECENTS` rows.
 */
@Dao
interface FoodLibraryDao {

    // ---- Custom foods -----------------------------------------------------

    @Query("SELECT * FROM custom_foods ORDER BY name COLLATE NOCASE ASC")
    fun observeCustomFoods(): Flow<List<CustomFoodEntity>>

    @Query("SELECT * FROM custom_foods WHERE id = :id LIMIT 1")
    suspend fun findCustomFood(id: String): CustomFoodEntity?

    /**
     * Prefix search, case-insensitive.
     *
     * Note the absence of `COLLATE NOCASE` on the WHERE clause: SQLite's `LIKE` is
     * already case-insensitive for ASCII, and adding an explicit NOCASE collating
     * sequence *silently flips LIKE to case-sensitive* per the SQLite spec. An
     * instrumented test caught exactly that — "homa" stopped matching "Homemade".
     */
    @Query("SELECT * FROM custom_foods WHERE name LIKE :prefix || '%' ORDER BY name COLLATE NOCASE ASC")
    suspend fun searchCustomFoods(prefix: String): List<CustomFoodEntity>

    @Upsert
    suspend fun upsertCustomFood(food: CustomFoodEntity)

    @Query("DELETE FROM custom_foods WHERE id = :id")
    suspend fun deleteCustomFood(id: String): Int

    // ---- Favourites -------------------------------------------------------

    @Query("SELECT * FROM favorite_foods ORDER BY created_at DESC")
    fun observeFavorites(): Flow<List<FavoriteFoodEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_foods WHERE food_id = :foodId)")
    suspend fun isFavorite(foodId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_foods WHERE food_id = :foodId)")
    fun observeIsFavorite(foodId: String): Flow<Boolean>

    @Upsert
    suspend fun upsertFavorite(food: FavoriteFoodEntity)

    @Query("DELETE FROM favorite_foods WHERE food_id = :foodId")
    suspend fun deleteFavorite(foodId: String): Int

    // ---- Recents ----------------------------------------------------------

    @Query("SELECT * FROM recent_foods ORDER BY last_used_at DESC LIMIT :limit")
    fun observeRecents(limit: Int = MAX_RECENTS): Flow<List<RecentFoodEntity>>

    /**
     * Records a use and enforces the cap in the same transaction, so the table can never
     * grow past [MAX_RECENTS] however many foods get logged.
     *
     * The delete-by-food-id step is not optional: `recent_foods` has a unique index on
     * `food_id`, and Room's `@Upsert` only resolves *primary key* conflicts — without it,
     * re-logging a known food would be silently dropped and the recents list would
     * freeze on the first few foods ever logged.
     */
    @Transaction
    suspend fun recordRecentUsage(food: RecentFoodEntity, maxRecents: Int = MAX_RECENTS) {
        deleteRecentByFoodId(food.foodId)
        upsertRecent(food)
        trimRecents(maxRecents)
    }

    /** Starring the same food twice updates the existing star rather than being ignored. */
    @Transaction
    suspend fun starFavorite(food: FavoriteFoodEntity) {
        deleteFavorite(food.foodId)
        upsertFavorite(food)
    }

    /**
     * Atomically toggles the favorite status of a food in a single transaction,
     * eliminating race conditions between read and write.
     */
    @Transaction
    suspend fun toggleFavorite(food: FavoriteFoodEntity): Boolean {
        return if (isFavorite(food.foodId)) {
            deleteFavorite(food.foodId)
            false
        } else {
            starFavorite(food)
            true
        }
    }

    @Upsert
    suspend fun upsertRecent(food: RecentFoodEntity)

    @Query("DELETE FROM recent_foods WHERE food_id = :foodId")
    suspend fun deleteRecentByFoodId(foodId: String): Int

    @Query(
        """
        DELETE FROM recent_foods
        WHERE id NOT IN (
            SELECT id FROM recent_foods ORDER BY last_used_at DESC LIMIT :maxRecents
        )
        """,
    )
    suspend fun trimRecents(maxRecents: Int): Int

    @Query("DELETE FROM recent_foods")
    suspend fun clearRecents(): Int

    // ---- Meal templates ---------------------------------------------------

    @Transaction
    @Query("SELECT * FROM meal_templates ORDER BY created_at DESC")
    fun observeTemplates(): Flow<List<MealTemplateWithItems>>

    @Transaction
    @Query("SELECT * FROM meal_templates WHERE id = :id LIMIT 1")
    suspend fun findTemplate(id: String): MealTemplateWithItems?

    @Query("SELECT * FROM meal_templates WHERE meal_slot = :mealSlot ORDER BY created_at DESC")
    fun observeTemplatesForMeal(mealSlot: com.drnitz.aro.domain.model.MealSlot): Flow<List<MealTemplateEntity>>

    /** Saves a template and its items atomically, stamping the template id onto each item. */
    @Transaction
    suspend fun saveTemplate(template: MealTemplateEntity, items: List<MealTemplateItemEntity>) {
        upsertTemplate(template)
        deleteTemplateItems(template.id)
        if (items.isNotEmpty()) insertTemplateItems(items.map { it.copy(templateId = template.id) })
    }

    @Upsert
    suspend fun upsertTemplate(template: MealTemplateEntity)

    @Upsert
    suspend fun insertTemplateItems(items: List<MealTemplateItemEntity>)

    @Query("DELETE FROM meal_template_items WHERE template_id = :templateId")
    suspend fun deleteTemplateItems(templateId: String): Int

    /** Items are removed by the CASCADE on `meal_template_items.template_id`. */
    @Query("DELETE FROM meal_templates WHERE id = :id")
    suspend fun deleteTemplate(id: String): Int

    companion object {
        /** Matches the web app's `MAX_RECENTS`. */
        const val MAX_RECENTS = 24
    }
}