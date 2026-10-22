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

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 0)

        val app = context.applicationContext as? AroApplication
        val repository = app?.repository ?: RoomAroRepository(AroDatabase.get(context))

        when (action) {
            NotificationHelper.ACTION_LOG_WATER -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val today = app?.dateProvider?.today() ?: LocalDate.now()
                        repository.addWater(today, 250)
                        NotificationHelper.cancelNotification(context, notificationId)
                        NotificationHelper.showFeedbackNotification(
                            context,
                            "Hydration Logged",
                            "+250 mL added to today's diary. Keep going!",
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            NotificationHelper.ACTION_END_FAST -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        repository.endFast()
                        AroNotificationScheduler.cancelFastingCompletion(context)
                        NotificationHelper.cancelNotification(context, notificationId)
                        NotificationHelper.showFeedbackNotification(
                            context,
                            "Fast Completed",
                            "Fasting session recorded in your daily diary.",
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            NotificationHelper.ACTION_MARK_TAKEN -> {
                val medName = intent.getStringExtra(NotificationHelper.EXTRA_MED_NAME) ?: "Medication"
                NotificationHelper.cancelNotification(context, notificationId)
                NotificationHelper.showFeedbackNotification(
                    context,
                    "Marked Taken",
                    "$medName taken. Great consistency!",
                )
            }
            NotificationHelper.ACTION_SNOOZE -> {
                val medId = intent.getStringExtra(NotificationHelper.EXTRA_MED_ID) ?: ""
                val medName = intent.getStringExtra(NotificationHelper.EXTRA_MED_NAME) ?: "Medication"
                val medDosage = intent.getStringExtra(NotificationHelper.EXTRA_MED_DOSAGE) ?: ""

                NotificationHelper.cancelNotification(context, notificationId)
                AroNotificationScheduler.scheduleSnooze(context, medId, medName, medDosage, 15)
                NotificationHelper.showFeedbackNotification(
                    context,
                    "Reminder Snoozed",
                    "$medName snoozed for 15 minutes.",
                )
            }
        }
    }
}
