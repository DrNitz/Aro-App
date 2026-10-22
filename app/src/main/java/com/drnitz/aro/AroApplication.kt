package com.drnitz.aro

import android.app.Application
import com.drnitz.aro.data.db.AroDatabase
import com.drnitz.aro.data.library.OfflineExerciseLibrary
import com.drnitz.aro.data.library.OfflineFoodLibrary
import com.drnitz.aro.data.network.OpenFoodFactsClient
import com.drnitz.aro.data.repository.AroRepository
import com.drnitz.aro.data.repository.RoomAroRepository
import com.drnitz.aro.notifications.AroNotificationScheduler
import com.drnitz.aro.notifications.NotificationHelper
import com.drnitz.aro.notifications.PlanReminderManager
import com.drnitz.aro.notifications.PlanReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AroApplication : Application() {
    val database: AroDatabase by lazy { AroDatabase.get(this) }
    val repository: AroRepository by lazy { RoomAroRepository(database) }
    val exerciseLibrary: OfflineExerciseLibrary by lazy { OfflineExerciseLibrary(this) }
    val foodLibrary: OfflineFoodLibrary by lazy { OfflineFoodLibrary(this) }
    val openFoodFactsClient: OpenFoodFactsClient by lazy { OpenFoodFactsClient() }
    val planReminderScheduler: PlanReminderScheduler by lazy {
        PlanReminderManager(this, repository)
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        AroNotificationScheduler.scheduleHydrationReminders(this)
        AroNotificationScheduler.scheduleMedicationReminders(this)
        // Rebuild plan alarms whenever the process starts. This is cheap (one query per
        // series), and it repairs alarms after an app update or an interrupted edit.
        applicationScope.launch { planReminderScheduler.rescheduleAll() }
    }
}
