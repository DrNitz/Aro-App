package com.drnitz.aro.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * A saved daily medication / supplement reminder.
 *
 * This lives in the domain layer (not the notification layer) because the schedule is
 * also the expected-dose source for adherence calculations.
 */
@Serializable
data class MedicationReminder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dosage: String = "",
    val hour: Int, // 0..23
    val minute: Int, // 0..59
    val enabled: Boolean = true,
    /** Epoch millis when the reminder was created; 0 for reminders migrated from older builds. */
    val createdAt: Long = 0L,
)

/** How a medication dose was recorded. */
enum class MedicationLogSource {
    NOTIFICATION,
    MANUAL,
}

/**
 * One recorded medication dose.
 *
 * [date] is the diary day the dose belongs to (not necessarily the day the row was
 * written) and [takenAt] is the exact instant, which keeps adherence calculations
 * deterministic when the user taps a notification after midnight.
 */
data class MedicationLog(
    val id: String,
    val medicationId: String,
    val medicationName: String,
    val dosage: String = "",
    val date: LocalDate,
    val scheduledHour: Int? = null,
    val scheduledMinute: Int? = null,
    val takenAt: Instant = Instant.now(),
    val source: MedicationLogSource = MedicationLogSource.MANUAL,
)

/** 30-day adherence for one reminder. */
data class MedicationAdherenceEntry(
    val medicationId: String,
    val name: String,
    val dosage: String,
    val takenDays: Int,
    val expectedDays: Int,
) {
    /** 0..100, rounded to the nearest whole percent. */
    val percent: Int
        get() = if (expectedDays <= 0) 0 else {
            (takenDays.toDouble() / expectedDays * 100.0).toInt().coerceIn(0, 100)
        }
}

/** Aggregate adherence across every enabled reminder. */
data class MedicationAdherence(
    val entries: List<MedicationAdherenceEntry>,
    val takenDoses: Int,
    val expectedDoses: Int,
    val windowDays: Int,
) {
    val overallPercent: Int
        get() = if (expectedDoses <= 0) 0 else {
            (takenDoses.toDouble() / expectedDoses * 100.0).toInt().coerceIn(0, 100)
        }

    val hasData: Boolean get() = entries.isNotEmpty()
}
