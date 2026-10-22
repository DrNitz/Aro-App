package com.drnitz.aro.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Patient Vitals measurement record.
 *
 * Indexed on [epochDay] for daily diary integration and chronological trend queries.
 */
@Entity(
    tableName = "vitals_entries",
    indices = [Index(value = ["epoch_day"]), Index(value = ["timestamp"])],
)
data class VitalsEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "epoch_day")
    val epochDay: Long,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "systolic")
    val systolic: Int? = null,

    @ColumnInfo(name = "diastolic")
    val diastolic: Int? = null,

    @ColumnInfo(name = "pulse")
    val pulse: Int? = null,

    @ColumnInfo(name = "glucose_mg_dl")
    val glucoseMgDl: Double? = null,

    @ColumnInfo(name = "glucose_context")
    val glucoseContext: String? = null,

    @ColumnInfo(name = "ketones_mmol_l")
    val ketonesMmolL: Double? = null,

    @ColumnInfo(name = "body_temperature_c")
    val bodyTemperatureC: Double? = null,

    @ColumnInfo(name = "spo2_percent")
    val spo2Percent: Int? = null,

    @ColumnInfo(name = "notes")
    val notes: String = "",
)

/**
 * Doctor's Clinical Consultation & Visit Record.
 *
 * Indexed on [epochDay] and [createdAt] for chronological timeline views and clinical PDF report generation.
 */
@Entity(
    tableName = "doctor_consultations",
    indices = [Index(value = ["epoch_day"]), Index(value = ["created_at"])],
)
data class ConsultationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "epoch_day")
    val epochDay: Long,

    @ColumnInfo(name = "doctor_name")
    val doctorName: String,

    @ColumnInfo(name = "specialty")
    val specialty: String,

    @ColumnInfo(name = "reason_for_visit")
    val reasonForVisit: String,

    @ColumnInfo(name = "diagnosis_notes")
    val diagnosisNotes: String,

    @ColumnInfo(name = "prescribed_medications")
    val prescribedMedications: String = "",

    @ColumnInfo(name = "recommended_tests")
    val recommendedTests: String = "",

    @ColumnInfo(name = "follow_up_plan")
    val followUpPlan: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
