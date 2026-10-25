package com.drnitz.aro.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.drnitz.aro.domain.model.MedicationReminder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object AroNotificationScheduler {

    const val EXTRA_ALARM_TYPE = "extra_alarm_type"
    const val TYPE_HYDRATION = "type_hydration"
    const val TYPE_FASTING = "type_fasting"
    const val TYPE_MEDICATION = "type_medication"
    const val EXTRA_HOURS = "extra_hours"

    const val RC_HYDRATION = 5001
    const val RC_FASTING = 5002
    const val RC_MEDICATION_BASE = 6000

    fun calculateNextHydrationTrigger(
        now: LocalDateTime,
        startHour: Int,
        endHour: Int,
        intervalHours: Int,
    ): LocalDateTime {
        val currentHour = now.hour
        return when {
            currentHour < startHour -> {
                // Today at startHour
                now.withHour(startHour).withMinute(0).withSecond(0).withNano(0)
            }
            currentHour >= endHour -> {
                // Tomorrow at startHour
                now.plusDays(1).withHour(startHour).withMinute(0).withSecond(0).withNano(0)
            }
            else -> {
                // Next interval within today
                val next = now.plusHours(intervalHours.toLong()).withMinute(0).withSecond(0).withNano(0)
                if (next.hour > endHour) {
                    now.plusDays(1).withHour(startHour).withMinute(0).withSecond(0).withNano(0)
                } else {
                    next
                }
            }
        }
    }

    fun calculateNextMedicationTrigger(
        now: LocalDateTime,
        medHour: Int,
        medMinute: Int,
    ): LocalDateTime {
        var scheduledTime = now.withHour(medHour).withMinute(medMinute).withSecond(0).withNano(0)
        if (scheduledTime.isBefore(now)) {
            scheduledTime = scheduledTime.plusDays(1)
        }
        return scheduledTime
    }

    fun calculateFastingCompletionTrigger(
        startedAtMillis: Long,
        targetHours: Int,
    ): Long {
        return startedAtMillis + targetHours * 3600 * 1000L
    }

    suspend fun scheduleHydrationReminders(context: Context) {
        val settings = NotificationPreferences(context).currentSettings()
        if (!settings.hydrationEnabled) {
            cancelHydrationReminders(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val now = LocalDateTime.now()
        val nextTrigger = calculateNextHydrationTrigger(
            now = now,
            startHour = settings.hydrationStartHour,
            endHour = settings.hydrationEndHour,
            intervalHours = settings.hydrationIntervalHours,
        )

        val triggerAtMillis = nextTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = AlarmBroadcastReceiver.ACTION_FIRE_ALARM
            putExtra(EXTRA_ALARM_TYPE, TYPE_HYDRATION)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            RC_HYDRATION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        scheduleAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    fun cancelHydrationReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = AlarmBroadcastReceiver.ACTION_FIRE_ALARM
            putExtra(EXTRA_ALARM_TYPE, TYPE_HYDRATION)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            RC_HYDRATION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
    }

    suspend fun scheduleFastingCompletion(context: Context, targetHours: Int, startedAt: Instant) {
        scheduleFastingCompletion(context, targetHours, startedAt.toEpochMilli())
    }

    suspend fun scheduleFastingCompletion(context: Context, targetHours: Int, startedAtMillis: Long) {
        val settings = NotificationPreferences(context).currentSettings()
        if (!settings.fastingEnabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val triggerAtMillis = calculateFastingCompletionTrigger(startedAtMillis, targetHours)
        val now = System.currentTimeMillis()

        if (triggerAtMillis <= now) {
            // Already complete, fire immediately
            NotificationHelper.showFastingNotification(context, targetHours)
            return
        }

        val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = AlarmBroadcastReceiver.ACTION_FIRE_ALARM
            putExtra(EXTRA_ALARM_TYPE, TYPE_FASTING)
            putExtra(EXTRA_HOURS, targetHours)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            RC_FASTING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        scheduleAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    fun cancelFastingCompletion(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = AlarmBroadcastReceiver.ACTION_FIRE_ALARM
            putExtra(EXTRA_ALARM_TYPE, TYPE_FASTING)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            RC_FASTING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
    }

    suspend fun scheduleMedicationReminders(context: Context) {
        val medications = NotificationPreferences(context)
            .currentSettings()
            .medicationReminders
            .filter { it.enabled }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        medications.forEach { med ->
            scheduleSingleMedication(context, alarmManager, med)
        }
    }

    fun scheduleSingleMedication(context: Context, alarmManager: AlarmManager, med: MedicationReminder) {
        val now = LocalDateTime.now()
        val scheduledTime = calculateNextMedicationTrigger(now, med.hour, med.minute)

        val triggerAtMillis = scheduledTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val requestCode = RC_MEDICATION_BASE + (med.id.hashCode() % 1000).coerceAtLeast(0)

        val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = AlarmBroadcastReceiver.ACTION_FIRE_ALARM
            putExtra(EXTRA_ALARM_TYPE, TYPE_MEDICATION)
            putExtra(NotificationHelper.EXTRA_MED_ID, med.id)
            putExtra(NotificationHelper.EXTRA_MED_NAME, med.name)
            putExtra(NotificationHelper.EXTRA_MED_DOSAGE, med.dosage)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        scheduleAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    fun cancelMedicationReminder(context: Context, medId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = RC_MEDICATION_BASE + (medId.hashCode() % 1000).coerceAtLeast(0)

        val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = AlarmBroadcastReceiver.ACTION_FIRE_ALARM
            putExtra(EXTRA_ALARM_TYPE, TYPE_MEDICATION)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleSnooze(context: Context, medId: String, name: String, dosage: String, minutes: Int = 15) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAtMillis = System.currentTimeMillis() + minutes * 60 * 1000L
        val requestCode = RC_MEDICATION_BASE + (medId.hashCode() % 1000).coerceAtLeast(0) + 500

        val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = AlarmBroadcastReceiver.ACTION_FIRE_ALARM
            putExtra(EXTRA_ALARM_TYPE, TYPE_MEDICATION)
            putExtra(NotificationHelper.EXTRA_MED_ID, medId)
            putExtra(NotificationHelper.EXTRA_MED_NAME, name)
            putExtra(NotificationHelper.EXTRA_MED_DOSAGE, dosage)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        scheduleAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    internal fun scheduleAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }
}
