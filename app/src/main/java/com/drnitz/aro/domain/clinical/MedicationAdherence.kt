package com.drnitz.aro.domain.clinical

import com.drnitz.aro.domain.model.MedicationAdherence
import com.drnitz.aro.domain.model.MedicationAdherenceEntry
import com.drnitz.aro.domain.model.MedicationLog
import com.drnitz.aro.domain.model.MedicationReminder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 30-day medication adherence.
 *
 * Only enabled reminders count towards the expected dose count. A reminder added inside
 * the window is judged from its creation day, so a brand-new prescription does not open
 * with a misleading 0% score. Reminders migrated from older builds have no timestamp;
 * for those the full window is used.
 *
 * A day with at least one recorded dose counts as taken; the current data model allows
 * one scheduled dose per reminder per day.
 */
fun calculateMedicationAdherence(
    reminders: List<MedicationReminder>,
    logs: List<MedicationLog>,
    today: LocalDate,
    windowDays: Int = 30,
    zoneId: ZoneId = ZoneId.systemDefault(),
): MedicationAdherence {
    val safeWindow = windowDays.coerceAtLeast(1)
    val windowStart = today.minusDays((safeWindow - 1).toLong())
    val enabled = reminders.filter { it.enabled }
    if (enabled.isEmpty()) {
        return MedicationAdherence(
            entries = emptyList(),
            takenDoses = 0,
            expectedDoses = 0,
            windowDays = safeWindow,
        )
    }

    val logsByMedication = logs
        .filter { !it.date.isBefore(windowStart) && !it.date.isAfter(today) }
        .groupBy { it.medicationId }

    val entries = enabled.map { reminder ->
        val createdDate = reminder.createdAt
            .takeIf { it > 0L }
            ?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }

        val start = when {
            createdDate == null -> windowStart
            createdDate.isAfter(today) -> today
            createdDate.isBefore(windowStart) -> windowStart
            else -> createdDate
        }
        val expectedDays = (ChronoUnit.DAYS.between(start, today).toInt() + 1).coerceAtLeast(1)
        val takenDays = logsByMedication[reminder.id]
            .orEmpty()
            .map { it.date }
            .filter { !it.isBefore(start) && !it.isAfter(today) }
            .distinct()
            .size
            .coerceAtMost(expectedDays)

        MedicationAdherenceEntry(
            medicationId = reminder.id,
            name = reminder.name,
            dosage = reminder.dosage,
            takenDays = takenDays,
            expectedDays = expectedDays,
        )
    }

    return MedicationAdherence(
        entries = entries,
        takenDoses = entries.sumOf { it.takenDays },
        expectedDoses = entries.sumOf { it.expectedDays },
        windowDays = safeWindow,
    )
}
