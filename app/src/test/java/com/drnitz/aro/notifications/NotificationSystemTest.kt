package com.drnitz.aro.notifications

import com.drnitz.aro.domain.model.MedicationReminder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class NotificationSystemTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `calculateNextHydrationTrigger triggers today at start hour when before start`() {
        val now = LocalDateTime.of(LocalDate.of(2026, 10, 21), LocalTime.of(6, 30))
        val next = AroNotificationScheduler.calculateNextHydrationTrigger(
            now = now,
            startHour = 8,
            endHour = 21,
            intervalHours = 2,
        )

        assertEquals(LocalDate.of(2026, 10, 21), next.toLocalDate())
        assertEquals(8, next.hour)
        assertEquals(0, next.minute)
    }

    @Test
    fun `calculateNextHydrationTrigger advances by interval when within active window`() {
        val now = LocalDateTime.of(LocalDate.of(2026, 10, 21), LocalTime.of(10, 15))
        val next = AroNotificationScheduler.calculateNextHydrationTrigger(
            now = now,
            startHour = 8,
            endHour = 21,
            intervalHours = 2,
        )

        assertEquals(LocalDate.of(2026, 10, 21), next.toLocalDate())
        assertEquals(12, next.hour)
        assertEquals(0, next.minute)
    }

    @Test
    fun `calculateNextHydrationTrigger wraps to tomorrow when next interval exceeds end hour`() {
        val now = LocalDateTime.of(LocalDate.of(2026, 10, 21), LocalTime.of(20, 15))
        val next = AroNotificationScheduler.calculateNextHydrationTrigger(
            now = now,
            startHour = 8,
            endHour = 21,
            intervalHours = 2,
        )

        assertEquals(LocalDate.of(2026, 10, 22), next.toLocalDate())
        assertEquals(8, next.hour)
        assertEquals(0, next.minute)
    }

    @Test
    fun `calculateNextHydrationTrigger wraps to tomorrow when current time is past end hour`() {
        val now = LocalDateTime.of(LocalDate.of(2026, 10, 21), LocalTime.of(22, 0))
        val next = AroNotificationScheduler.calculateNextHydrationTrigger(
            now = now,
            startHour = 8,
            endHour = 21,
            intervalHours = 2,
        )

        assertEquals(LocalDate.of(2026, 10, 22), next.toLocalDate())
        assertEquals(8, next.hour)
        assertEquals(0, next.minute)
    }

    @Test
    fun `calculateNextMedicationTrigger schedules for today if med time is in future`() {
        val now = LocalDateTime.of(LocalDate.of(2026, 10, 21), LocalTime.of(7, 30))
        val next = AroNotificationScheduler.calculateNextMedicationTrigger(
            now = now,
            medHour = 9,
            medMinute = 0,
        )

        assertEquals(LocalDate.of(2026, 10, 21), next.toLocalDate())
        assertEquals(9, next.hour)
        assertEquals(0, next.minute)
    }

    @Test
    fun `calculateNextMedicationTrigger schedules for tomorrow if med time already passed today`() {
        val now = LocalDateTime.of(LocalDate.of(2026, 10, 21), LocalTime.of(14, 0))
        val next = AroNotificationScheduler.calculateNextMedicationTrigger(
            now = now,
            medHour = 8,
            medMinute = 30,
        )

        assertEquals(LocalDate.of(2026, 10, 22), next.toLocalDate())
        assertEquals(8, next.hour)
        assertEquals(30, next.minute)
    }

    @Test
    fun `calculateFastingCompletionTrigger calculates exact millisecond timestamp`() {
        val startedAt = 1_700_000_000_000L
        val targetHours = 16
        val expected = startedAt + 16 * 3600 * 1000L

        val result = AroNotificationScheduler.calculateFastingCompletionTrigger(startedAt, targetHours)
        assertEquals(expected, result)
        assertEquals(startedAt + 57_600_000L, result)
    }

    @Test
    fun `MedicationReminder serializes and deserializes accurately with JSON`() {
        val reminder = MedicationReminder(
            id = "test-uuid-1234",
            name = "Vitamin D3",
            dosage = "5000 IU",
            hour = 8,
            minute = 30,
            enabled = true,
        )

        val encoded = json.encodeToString(reminder)
        val decoded = json.decodeFromString<MedicationReminder>(encoded)

        assertEquals(reminder.id, decoded.id)
        assertEquals(reminder.name, decoded.name)
        assertEquals(reminder.dosage, decoded.dosage)
        assertEquals(reminder.hour, decoded.hour)
        assertEquals(reminder.minute, decoded.minute)
        assertTrue(decoded.enabled)
    }

    @Test
    fun `MedicationReminder list serializes and deserializes correctly`() {
        val list = listOf(
            MedicationReminder(id = "1", name = "Creatine", dosage = "5g", hour = 7, minute = 0),
            MedicationReminder(id = "2", name = "Omega 3", dosage = "1000mg", hour = 12, minute = 30),
            MedicationReminder(id = "3", name = "Magnesium Glycinate", dosage = "400mg", hour = 21, minute = 0),
        )

        val encoded = json.encodeToString(list)
        val decoded = json.decodeFromString<List<MedicationReminder>>(encoded)

        assertEquals(3, decoded.size)
        assertEquals("Creatine", decoded[0].name)
        assertEquals("Omega 3", decoded[1].name)
        assertEquals("Magnesium Glycinate", decoded[2].name)
    }

    @Test
    fun `notification channel IDs and action constants are properly configured`() {
        assertEquals("aro_channel_hydration", NotificationHelper.CHANNEL_ID_HYDRATION)
        assertEquals("aro_channel_fasting", NotificationHelper.CHANNEL_ID_FASTING)
        assertEquals("aro_channel_medication", NotificationHelper.CHANNEL_ID_MEDICATION)
        assertEquals("aro_channel_plan", NotificationHelper.CHANNEL_ID_PLAN)

        assertEquals("com.drnitz.aro.ACTION_LOG_WATER", NotificationHelper.ACTION_LOG_WATER)
        assertEquals("com.drnitz.aro.ACTION_END_FAST", NotificationHelper.ACTION_END_FAST)
        assertEquals("com.drnitz.aro.ACTION_MARK_TAKEN", NotificationHelper.ACTION_MARK_TAKEN)
        assertEquals("com.drnitz.aro.ACTION_SNOOZE", NotificationHelper.ACTION_SNOOZE)
    }

    @Test
    fun `alarm scheduler intent actions and types match constants`() {
        assertEquals("com.drnitz.aro.ACTION_FIRE_ALARM", AlarmBroadcastReceiver.ACTION_FIRE_ALARM)
        assertEquals("extra_alarm_type", AroNotificationScheduler.EXTRA_ALARM_TYPE)
        assertEquals("type_hydration", AroNotificationScheduler.TYPE_HYDRATION)
        assertEquals("type_fasting", AroNotificationScheduler.TYPE_FASTING)
        assertEquals("type_medication", AroNotificationScheduler.TYPE_MEDICATION)
        assertEquals("type_plan", PlanAlarmScheduler.TYPE_PLAN)
        assertEquals("extra_plan_id", NotificationHelper.EXTRA_PLAN_ID)
        assertEquals("extra_plan_date_epoch_day", NotificationHelper.EXTRA_PLAN_DATE_EPOCH_DAY)
    }

    @Test
    fun `plan notification request codes are stable and in the plan range`() {
        val first = PlanAlarmScheduler.requestCodeFor("plan-a")
        val second = PlanAlarmScheduler.requestCodeFor("plan-a")
        val other = PlanAlarmScheduler.requestCodeFor("plan-b")

        assertEquals(first, second)
        assertTrue(first >= 10_000)
        assertTrue(other >= 10_000)
    }
}
