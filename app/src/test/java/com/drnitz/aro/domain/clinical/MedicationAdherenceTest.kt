package com.drnitz.aro.domain.clinical

import com.drnitz.aro.domain.model.MedicationLog
import com.drnitz.aro.domain.model.MedicationLogSource
import com.drnitz.aro.domain.model.MedicationReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class MedicationAdherenceTest {

    private val today = LocalDate.of(2026, 10, 23)
    private val zone = ZoneId.of("UTC")

    private fun reminder(
        id: String = "med-1",
        name: String = "Vitamin D3",
        enabled: Boolean = true,
        createdAt: Long = 0L,
    ) = MedicationReminder(
        id = id,
        name = name,
        dosage = "5000 IU",
        hour = 8,
        minute = 0,
        enabled = enabled,
        createdAt = createdAt,
    )

    private fun log(
        medicationId: String = "med-1",
        date: LocalDate = today,
        id: String = UUID.randomUUID().toString(),
    ) = MedicationLog(
        id = id,
        medicationId = medicationId,
        medicationName = "Vitamin D3",
        date = date,
        takenAt = Instant.now(),
        source = MedicationLogSource.MANUAL,
    )

    @Test
    fun `no enabled reminders produces no adherence data`() {
        val result = calculateMedicationAdherence(
            reminders = listOf(reminder(enabled = false)),
            logs = emptyList(),
            today = today,
            zoneId = zone,
        )

        assertFalse(result.hasData)
        assertEquals(0, result.overallPercent)
        assertEquals(0, result.expectedDoses)
    }

    @Test
    fun `daily logs for the full window are perfect adherence`() {
        val logs = (0 until 30).map { offset -> log(date = today.minusDays(offset.toLong())) }

        val result = calculateMedicationAdherence(
            reminders = listOf(reminder()),
            logs = logs,
            today = today,
            zoneId = zone,
        )

        assertTrue(result.hasData)
        assertEquals(30, result.expectedDoses)
        assertEquals(30, result.takenDoses)
        assertEquals(100, result.overallPercent)
        assertEquals(100, result.entries.single().percent)
    }

    @Test
    fun `half of the window logged is fifty percent`() {
        val logs = (0 until 15).map { offset -> log(date = today.minusDays(offset.toLong())) }

        val result = calculateMedicationAdherence(
            reminders = listOf(reminder()),
            logs = logs,
            today = today,
            zoneId = zone,
        )

        assertEquals(15, result.takenDoses)
        assertEquals(30, result.expectedDoses)
        assertEquals(50, result.overallPercent)
    }

    @Test
    fun `multiple doses on one day count once per day`() {
        val logs = listOf(
            log(date = today, id = "a"),
            log(date = today, id = "b"),
            log(date = today, id = "c"),
        )

        val result = calculateMedicationAdherence(
            reminders = listOf(reminder()),
            logs = logs,
            today = today,
            zoneId = zone,
        )

        assertEquals(1, result.takenDoses)
    }

    @Test
    fun `new reminder is graded from its creation day`() {
        val created = today.minusDays(5).atStartOfDay(zone).toInstant().toEpochMilli()
        val logs = (0..5).map { offset -> log(date = today.minusDays(offset.toLong())) }

        val result = calculateMedicationAdherence(
            reminders = listOf(reminder(createdAt = created)),
            logs = logs,
            today = today,
            zoneId = zone,
        )

        assertEquals(6, result.expectedDoses)
        assertEquals(6, result.takenDoses)
        assertEquals(100, result.overallPercent)
    }

    @Test
    fun `logs outside the window are ignored`() {
        val logs = listOf(
            log(date = today.minusDays(30)),
            log(date = today),
        )

        val result = calculateMedicationAdherence(
            reminders = listOf(reminder()),
            logs = logs,
            today = today,
            zoneId = zone,
        )

        assertEquals(1, result.takenDoses)
        assertEquals(30, result.expectedDoses)
    }

    @Test
    fun `logs for other medications are ignored`() {
        val logs = listOf(log(medicationId = "other"))

        val result = calculateMedicationAdherence(
            reminders = listOf(reminder(id = "med-1")),
            logs = logs,
            today = today,
            zoneId = zone,
        )

        assertEquals(0, result.takenDoses)
        assertEquals(0, result.overallPercent)
    }

    @Test
    fun `multiple reminders aggregate into one overall percentage`() {
        val logs = listOf(
            log(medicationId = "a", date = today),
            log(medicationId = "b", date = today),
        )

        val result = calculateMedicationAdherence(
            reminders = listOf(
                reminder(id = "a", name = "A"),
                reminder(id = "b", name = "B"),
            ),
            logs = logs,
            today = today,
            zoneId = zone,
        )

        assertEquals(60, result.expectedDoses)
        assertEquals(2, result.takenDoses)
        assertEquals(3, result.overallPercent)
        assertEquals(2, result.entries.size)
    }
}
