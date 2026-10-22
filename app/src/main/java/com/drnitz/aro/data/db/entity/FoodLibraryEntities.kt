package com.drnitz.aro.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.drnitz.aro.data.db.NutrientColumns
import com.drnitz.aro.domain.model.MealSlot

/**
 * A user-created food (a homemade recipe).
 *
 * Like [FoodLogEntity], nutrients are stored **already scaled to [defaultGrams]** so a
 * log entry is a straight copy rather than a per-100 g factor that every reader has to
 * remember to apply.
 */
@Entity(
    tableName = "custom_foods",
    indices = [Index(value = ["name"])],
)
data class CustomFoodEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "brand") val brand: String?,
    @ColumnInfo(name = "default_grams") val defaultGrams: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long,

    @ColumnInfo(name = "calories") override val calories: Double?,
    @ColumnInfo(name = "protein") override val protein: Double?,
    @ColumnInfo(name = "carbs") override val carbs: Double?,
    @ColumnInfo(name = "fat") override val fat: Double?,
    @ColumnInfo(name = "saturated_fat") override val saturatedFat: Double?,
    @ColumnInfo(name = "fiber") override val fiber: Double?,
    @ColumnInfo(name = "sugar") override val sugar: Double?,
    @ColumnInfo(name = "sodium") override val sodium: Double?,
    @ColumnInfo(name = "potassium") override val potassium: Double?,
    @ColumnInfo(name = "calcium") override val calcium: Double?,
    @ColumnInfo(name = "magnesium") override val magnesium: Double?,
) : NutrientColumns

/**
 * A starred food, kept as a full snapshot rather than a foreign key.
 *
 * Open Food Facts entries can be edited or removed upstream; a snapshot means a starred
 * food still resolves offline with the macros it had when the user starred it.
 */
@Entity(
    tableName = "favorite_foods",
    indices = [Index(value = ["food_id"], unique = true)],
)
data class FavoriteFoodEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "food_id") val foodId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "brand") val brand: String?,
    @ColumnInfo(name = "grams") val grams: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long,

    @ColumnInfo(name = "calories") override val calories: Double?,
    @ColumnInfo(name = "protein") override val protein: Double?,
    @ColumnInfo(name = "carbs") override val carbs: Double?,
    @ColumnInfo(name = "fat") override val fat: Double?,
    @ColumnInfo(name = "saturated_fat") override val saturatedFat: Double?,
    @ColumnInfo(name = "fiber") override val fiber: Double?,
    @ColumnInfo(name = "sugar") override val sugar: Double?,
    @ColumnInfo(name = "sodium") override val sodium: Double?,
    @ColumnInfo(name = "potassium") override val potassium: Double?,
    @ColumnInfo(name = "calcium") override val calcium: Double?,
    @ColumnInfo(name = "magnesium") override val magnesium: Double?,
) : NutrientColumns

/**
 * A recently logged food, ordered by [lastUsedAt].
 *
 * The web app kept recents inside one `localStorage` blob and re-wrote the entire
 * history on every keystroke. Here the cap is enforced by a single indexed statement in
 * the DAO rather than read-modify-write in Kotlin.
 */
@Entity(
    tableName = "recent_foods",
    indices = [Index(value = ["food_id"], unique = true), Index(value = ["last_used_at"])],
)
data class RecentFoodEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "food_id") val foodId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "brand") val brand: String?,
    @ColumnInfo(name = "grams") val grams: Double,
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long,

    @ColumnInfo(name = "calories") override val calories: Double?,
    @ColumnInfo(name = "protein") override val protein: Double?,
    @ColumnInfo(name = "carbs") override val carbs: Double?,
    @ColumnInfo(name = "fat") override val fat: Double?,
    @ColumnInfo(name = "saturated_fat") override val saturatedFat: Double?,
    @ColumnInfo(name = "fiber") override val fiber: Double?,
    @ColumnInfo(name = "sugar") override val sugar: Double?,
    @ColumnInfo(name = "sodium") override val sodium: Double?,
    @ColumnInfo(name = "potassium") override val potassium: Double?,
    @ColumnInfo(name = "calcium") override val calcium: Double?,
    @ColumnInfo(name = "magnesium") override val magnesium: Double?,
) : NutrientColumns

/**
 * A reusable meal, so "the usual breakfast" is one tap to log.
 */
@Entity(tableName = "meal_templates")
data class MealTemplateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "meal_slot") val mealSlot: MealSlot,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/**
 * One food inside a template, with its nutrients frozen at save time so re-logging the
 * template cannot drift if the underlying food is later edited.
 */
@Entity(
    tableName = "meal_template_items",
    indices = [Index(value = ["template_id"])],
    foreignKeys = [
        ForeignKey(
            entity = MealTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MealTemplateItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "template_id") val templateId: String,
    @ColumnInfo(name = "food_id") val foodId: String?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "brand") val brand: String?,
    @ColumnInfo(name = "grams") val grams: Double,

    @ColumnInfo(name = "calories") override val calories: Double?,
    @ColumnInfo(name = "protein") override val protein: Double?,
    @ColumnInfo(name = "carbs") override val carbs: Double?,
    @ColumnInfo(name = "fat") override val fat: Double?,
    @ColumnInfo(name = "saturated_fat") override val saturatedFat: Double?,
    @ColumnInfo(name = "fiber") override val fiber: Double?,
    @ColumnInfo(name = "sugar") override val sugar: Double?,
    @ColumnInfo(name = "sodium") override val sodium: Double?,
    @ColumnInfo(name = "potassium") override val potassium: Double?,
    @ColumnInfo(name = "calcium") override val calcium: Double?,
    @ColumnInfo(name = "magnesium") override val magnesium: Double?,
) : NutrientColumns

/** A template with its items, assembled by Room in one query. */
data class MealTemplateWithItems(
    @Embedded val template: MealTemplateEntity,
    @Relation(parentColumn = "id", entityColumn = "template_id")
    val items: List<MealTemplateItemEntity>,
)