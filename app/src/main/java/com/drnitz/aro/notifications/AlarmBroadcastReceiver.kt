package com.drnitz.aro.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.drnitz.aro.AroApplication
import com.drnitz.aro.data.db.AroDatabase
import com.drnitz.aro.data.repository.RoomAroRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class AlarmBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE_ALARM) return

        when (intent.getStringExtra(AroNotificationScheduler.EXTRA_ALARM_TYPE)) {
            AroNotificationScheduler.TYPE_HYDRATION -> {
                NotificationHelper.showHydrationNotification(context)
                // Continuously chain next interval
                AroNotificationScheduler.scheduleHydrationReminders(context)
            }
            AroNotificationScheduler.TYPE_FASTING -> {
                val hours = intent.getIntExtra(AroNotificationScheduler.EXTRA_HOURS, 16)
                NotificationHelper.showFastingNotification(context, hours)
            }
            AroNotificationScheduler.TYPE_MEDICATION -> {
                val medId = intent.getStringExtra(NotificationHelper.EXTRA_MED_ID) ?: ""
                val medName = intent.getStringExtra(NotificationHelper.EXTRA_MED_NAME) ?: "Medication"
                val medDosage = intent.getStringExtra(NotificationHelper.EXTRA_MED_DOSAGE) ?: ""
                NotificationHelper.showMedicationNotification(context, medId, medName, medDosage)

                // Reschedule for next day if not a snooze
                val prefs = NotificationPreferences(context)
                val med = prefs.getMedicationReminders().find { it.id == medId && it.enabled }
                if (med != null) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                    if (alarmManager != null) {
                        AroNotificationScheduler.scheduleSingleMedication(context, alarmManager, med)
                    }
                }
            }
            PlanAlarmScheduler.TYPE_PLAN -> handlePlanAlarm(context, intent)
        }
    }

    private fun handlePlanAlarm(context: Context, intent: Intent) {
        val planId = intent.getStringExtra(NotificationHelper.EXTRA_PLAN_ID) ?: return
        val epochDay = intent.getLongExtra(
            NotificationHelper.EXTRA_PLAN_DATE_EPOCH_DAY,
            Long.MIN_VALUE,
        )
        val occurrenceDate = if (epochDay == Long.MIN_VALUE) {
            null
        } else {
            LocalDate.ofEpochDay(epochDay)
        }

        val pendingResult = goAsync()
        val app = context.applicationContext as? AroApplication
        val repository = app?.repository ?: RoomAroRepository(AroDatabase.get(context))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val item = repository.getPlanItem(planId)
                val now = LocalDateTime.now()
                if (item != null && occurrenceDate != null) {
                    val shouldShow = item.reminderEnabled &&
                        item.occursOn(occurrenceDate) &&
                        !occurrenceDate.isBefore(now.toLocalDate()) &&
                        !repository.isPlanOccurrenceCompleted(planId, occurrenceDate)
                    if (shouldShow) {
                        NotificationHelper.showPlanNotification(context, item, occurrenceDate)
                    } else if (!item.reminderEnabled) {
                        NotificationHelper.cancelPlanNotification(context, planId)
                    }
                } else {
                    NotificationHelper.cancelPlanNotification(context, planId)
                }

                // Chain the next pending occurrence (or cancel if the series has ended).
                PlanAlarmScheduler.schedulePlan(context, repository, planId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE_ALARM = "com.drnitz.aro.ACTION_FIRE_ALARM"
    }
}
