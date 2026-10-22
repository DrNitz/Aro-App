package com.drnitz.aro.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.drnitz.aro.MainActivity
import com.drnitz.aro.R
import com.drnitz.aro.domain.model.PlanItem
import com.drnitz.aro.domain.model.PlanItemType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object NotificationHelper {

    const val CHANNEL_ID_HYDRATION = "aro_channel_hydration"
    const val CHANNEL_ID_FASTING = "aro_channel_fasting"
    const val CHANNEL_ID_MEDICATION = "aro_channel_medication"
    const val CHANNEL_ID_PLAN = "aro_channel_plan"

    const val NOTIFICATION_ID_HYDRATION = 1001
    const val NOTIFICATION_ID_FASTING = 1002
    const val NOTIFICATION_ID_MEDICATION_BASE = 2000
    const val NOTIFICATION_ID_FEEDBACK = 3001
    const val NOTIFICATION_ID_PLAN = 4001

    const val EXTRA_PLAN_ID = "extra_plan_id"
    const val EXTRA_PLAN_DATE_EPOCH_DAY = "extra_plan_date_epoch_day"

    private val PLAN_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    const val ACTION_LOG_WATER = "com.drnitz.aro.ACTION_LOG_WATER"
    const val ACTION_END_FAST = "com.drnitz.aro.ACTION_END_FAST"
    const val ACTION_MARK_TAKEN = "com.drnitz.aro.ACTION_MARK_TAKEN"
    const val ACTION_SNOOZE = "com.drnitz.aro.ACTION_SNOOZE"

    const val EXTRA_MED_ID = "extra_med_id"
    const val EXTRA_MED_NAME = "extra_med_name"
    const val EXTRA_MED_DOSAGE = "extra_med_dosage"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Hydration Reminders Channel
            val hydrationChannel = NotificationChannel(
                CHANNEL_ID_HYDRATION,
                "Hydration Reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Periodic reminders to log water and stay hydrated throughout the day."
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 2. Intermittent Fasting Channel
            val fastingChannel = NotificationChannel(
                CHANNEL_ID_FASTING,
                "Intermittent Fasting",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alerts when your fasting goal or metabolic stage is reached."
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 3. Medication & Supplement Channel
            val medicationChannel = NotificationChannel(
                CHANNEL_ID_MEDICATION,
                "Medications & Supplements",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Timely alerts to take prescribed medications and supplements."
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 4. Per-item planned food, exercise, and pill reminders
            val plannedChannel = NotificationChannel(
                CHANNEL_ID_PLAN,
                "Planned reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Reminders for the meals, workouts, pills, fasts and water you planned in Aro."
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannels(
                listOf(hydrationChannel, fastingChannel, medicationChannel, plannedChannel),
            )
        }
    }

    fun showHydrationNotification(context: Context, id: Int = NOTIFICATION_ID_HYDRATION) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Action: Log 250 mL
        val logWaterIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_LOG_WATER
            putExtra(EXTRA_NOTIFICATION_ID, id)
        }
        val logWaterPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            logWaterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_HYDRATION)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle("Hydration Check-in")
            .setContentText("Stay hydrated! Tap below to quickly log 250 mL of water.")
            .setColor(0xFF06B6D4.toInt()) // Electric Cyan
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_check,
                "+250 mL Logged",
                logWaterPendingIntent,
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Handled when notification permission is denied
        }
    }

    fun showFastingNotification(context: Context, hours: Int, id: Int = NOTIFICATION_ID_FASTING) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Action: End Fast
        val endFastIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_END_FAST
            putExtra(EXTRA_NOTIFICATION_ID, id)
        }
        val endFastPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            endFastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FASTING)
            .setSmallIcon(R.drawable.ic_fasting)
            .setContentTitle("Fasting Goal Achieved!")
            .setContentText("You completed your $hours-hour fast. Tap below to end and record your fast.")
            .setColor(0xFF6366F1.toInt()) // Indigo
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_check,
                "End Fast",
                endFastPendingIntent,
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Handled when notification permission is denied
        }
    }

    fun showMedicationNotification(
        context: Context,
        medId: String,
        name: String,
        dosage: String = "",
        notificationId: Int = NOTIFICATION_ID_MEDICATION_BASE + medId.hashCode().coerceAtLeast(0) % 1000,
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Action: Mark Taken
        val markTakenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_TAKEN
            putExtra(EXTRA_MED_ID, medId)
            putExtra(EXTRA_MED_NAME, name)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Action: Snooze 15m
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_MED_ID, medId)
            putExtra(EXTRA_MED_NAME, name)
            putExtra(EXTRA_MED_DOSAGE, dosage)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val bodyText = if (dosage.isNotBlank()) "Scheduled dose: $dosage" else "Time to take your scheduled dose."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MEDICATION)
            .setSmallIcon(R.drawable.ic_medication)
            .setContentTitle("Medication: $name")
            .setContentText(bodyText)
            .setColor(0xFF10B981.toInt()) // Emerald
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_check,
                "Mark Taken",
                markTakenPendingIntent,
            )
            .addAction(
                R.drawable.ic_fasting,
                "Snooze 15m",
                snoozePendingIntent,
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Handled when notification permission is denied
        }
    }

    fun showPlanNotification(
        context: Context,
        item: PlanItem,
        date: LocalDate,
    ) {
        val openPlanIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_PLAN_ID, item.id)
            putExtra(EXTRA_PLAN_DATE_EPOCH_DAY, date.toEpochDay())
        }
        val openPlanPendingIntent = PendingIntent.getActivity(
            context,
            PlanAlarmScheduler.requestCodeFor(item.id),
            openPlanIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = when (item.type) {
            PlanItemType.FOOD -> "Planned meal: ${item.title}"
            PlanItemType.EXERCISE -> "Planned workout: ${item.title}"
            PlanItemType.PILL -> "Planned pill: ${item.title}"
            PlanItemType.FASTING -> "Planned fast: ${item.title}"
            PlanItemType.WATER -> "Planned water: ${item.title}"
        }
        val detail = when (item.type) {
            PlanItemType.FOOD -> {
                val meal = item.food?.meal?.label.orEmpty()
                val grams = item.food?.grams?.toInt()?.let { "$it g" }.orEmpty()
                listOf(meal, grams).filter { it.isNotBlank() }.joinToString(" · ")
            }
            PlanItemType.EXERCISE ->
                "${item.exercise?.durationMinutes ?: 0} min planned"
            PlanItemType.PILL ->
                item.pill?.dosage.orEmpty().ifBlank { "Scheduled dose" }
            PlanItemType.FASTING ->
                "${item.fasting?.targetHours ?: 16}h fast · tap to start when ready"
            PlanItemType.WATER ->
                "${item.water?.amountMl ?: 250} mL planned"
        }
        val body = listOf(detail, "Tap to open the plan for this day.")
            .filter { it.isNotBlank() }
            .joinToString(" · ")

        val icon = when (item.type) {
            PlanItemType.FOOD -> R.drawable.ic_check
            PlanItemType.EXERCISE -> R.drawable.ic_check
            PlanItemType.PILL -> R.drawable.ic_medication
            PlanItemType.FASTING -> R.drawable.ic_fasting
            PlanItemType.WATER -> R.drawable.ic_water_drop
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_PLAN)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSubText("${date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))} · " +
                item.time.format(PLAN_TIME_FORMAT))
            .setColor(0xFF6366F1.toInt())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPlanPendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            // Tag by series so a completion/edit can dismiss this plan's latest reminder
            // without needing to remember the occurrence date.
            NotificationManagerCompat.from(context).notify(item.id, NOTIFICATION_ID_PLAN, notification)
        } catch (_: SecurityException) {
            // Handled when notification permission is denied
        }
    }

    fun cancelPlanNotification(context: Context, planId: String) {
        NotificationManagerCompat.from(context).cancel(planId, NOTIFICATION_ID_PLAN)
    }

    fun showFeedbackNotification(context: Context, title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_HYDRATION)
            .setSmallIcon(R.drawable.ic_check)
            .setContentTitle(title)
            .setContentText(message)
            .setColor(0xFF10B981.toInt())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setTimeoutAfter(5000) // Auto-dismiss after 5 seconds
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_FEEDBACK, notification)
        } catch (_: SecurityException) {
            // Denied
        }
    }

    fun cancelNotification(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }
}
