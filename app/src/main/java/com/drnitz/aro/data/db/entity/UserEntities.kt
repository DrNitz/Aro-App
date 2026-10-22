package com.drnitz.aro.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The user's profile and clinical settings.
 *
 * A single row pinned to [SINGLETON_ID] — a profile is one-per-install, not a
 * collection, so this needs no surrogate key or DAO plumbing beyond upsert.
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = SINGLETON_ID,

    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "sex") val sex: String,
    @ColumnInfo(name = "age_years") val ageYears: Int,
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
    @ColumnInfo(name = "target_weight_kg") val targetWeightKg: Double,
    @ColumnInfo(name = "height_cm") val heightCm: Double,
    @ColumnInfo(name = "activity_level") val activityLevel: String,
    @ColumnInfo(name = "goal") val goal: String,
    @ColumnInfo(name = "macro_preset") val macroPreset: String,
    @ColumnInfo(name = "active_protocol_id") val activeProtocolId: String,
    @ColumnInfo(name = "target_water_ml") val targetWaterMl: Int,
    @ColumnInfo(name = "onboarding_completed") val onboardingCompleted: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

/**
 * One weigh-in.
 *
 * `epochDay` is indexed and **unique**: two weigh-ins on the same calendar day are a
 * duplicate rather than history, so the second upsert replaces the first. That makes the
 * Task 12 trend chart one-point-per-day by construction instead of by convention.
 */
@Entity(
    tableName = "weight_entries",
    indices = [Index(value = ["epoch_day"], unique = true)],
)
data class WeightEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/**
 * An intermittent fasting window.
 *
 * A session is active exactly when [endedAt] is null, which is what makes the Task 9
 * timer crash-safe: after a process death the active session is still discoverable and
 * elapsed time is derived from [startedAt] instead of counted in memory.
 *
 * Indexed on [epochDay] so the dashboard can show today's fast next to today's meals,
 * and on [startedAt] so the active-session lookup stays ordered.
 */
@Entity(
    tableName = "fasting_sessions",
    indices = [
        Index(value = ["epoch_day"]),
        Index(value = ["started_at"]),
    ],
)
data class FastingSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "target_hours") val targetHours: Int,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
) {
    val isActive: Boolean get() = endedAt == null
}