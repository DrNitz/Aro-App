package com.drnitz.aro.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One recorded medication dose.
 *
 * The unique `(medication_id, epoch_day)` index means re-marking the same reminder as
 * taken on the same day replaces the earlier row instead of double-counting adherence.
 * The log is intentionally independent of the reminder itself, which lives in DataStore:
 * deleting a reminder must not erase the historical evidence that doses were taken.
 */
@Entity(
    tableName = "medication_logs",
    indices = [
        Index(value = ["medication_id"]),
        Index(value = ["epoch_day"]),
        Index(value = ["taken_at"]),
        Index(value = ["medication_id", "epoch_day"], unique = true),
    ],
)
data class MedicationLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "medication_id")
    val medicationId: String,

    @ColumnInfo(name = "medication_name")
    val medicationName: String,

    @ColumnInfo(name = "dosage")
    val dosage: String = "",

    @ColumnInfo(name = "epoch_day")
    val epochDay: Long,

    @ColumnInfo(name = "scheduled_hour")
    val scheduledHour: Int? = null,

    @ColumnInfo(name = "scheduled_minute")
    val scheduledMinute: Int? = null,

    @ColumnInfo(name = "taken_at")
    val takenAt: Long,

    @ColumnInfo(name = "source")
    val source: String,
)
