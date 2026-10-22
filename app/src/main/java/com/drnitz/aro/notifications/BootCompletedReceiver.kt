package com.drnitz.aro.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.drnitz.aro.AroApplication
import com.drnitz.aro.data.db.AroDatabase
import com.drnitz.aro.data.repository.RoomAroRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        // 1. Ensure channels exist
        NotificationHelper.createNotificationChannels(context)

        // 2. Reschedule hydration reminders
        AroNotificationScheduler.scheduleHydrationReminders(context)

        // 3. Reschedule medication / supplement reminders
        AroNotificationScheduler.scheduleMedicationReminders(context)

        // 4. Check for active fast and reschedule completion alarm
        val pendingResult = goAsync()
        val app = context.applicationContext as? AroApplication
        val repository = app?.repository ?: RoomAroRepository(AroDatabase.get(context))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activeFast = repository.observeActiveFast().first()
                if (activeFast != null) {
                    val startedMillis = activeFast.startedAt.toEpochMilli()
                    AroNotificationScheduler.scheduleFastingCompletion(
                        context,
                        activeFast.targetHours,
                        startedMillis,
                    )
                }

                // Alarms do not survive a reboot or a package replacement.
                PlanAlarmScheduler.scheduleAll(context, repository)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
