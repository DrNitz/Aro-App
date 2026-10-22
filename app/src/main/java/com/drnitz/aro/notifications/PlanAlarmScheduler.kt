package com.drnitz.aro.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.drnitz.aro.data.repository.AroRepository
import com.drnitz.aro.domain.model.PlanItem
import com.drnitz.aro.domain.model.nextReminderOccurrence
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules one pending reminder per plan series.
 *
 * A repeating series is not materialised into an alarm per day. Instead, each firing
 * posts the current occurrence's notification and chains the next pending occurrence.
 * Completion and edit paths replace or cancel that same pending intent, so a plan can
 * never have more than one live alarm.
 */
object PlanAlarmScheduler {

    const val TYPE_PLAN = "type_plan"

    private const val RC_PLAN_BASE = 10_000
    private const val RC_PLAN_SPAN = 100_000
    private const val REMINDER_HORIZON_YEARS = 5L

    private val mutex = Mutex()

    /** Stable per-series request code; a plan's next alarm replaces its previous one. */
    fun requestCodeFor(planId: String): Int =
        RC_PLAN_BASE + ((planId.hashCode() and Int.MAX_VALUE) % RC_PLAN_SPAN)

    suspend fun scheduleAll(
        context: Context,
        repository: AroRepository,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        mutex.withLock {
            repository.getAllPlanItems().forEach { item ->
                scheduleOne(context, repository, item, now)
            }
        }
    }

    /**
     * Recomputes the next alarm for one plan and replaces the previous one. This does
     * not dismiss a visible notification: the alarm receiver calls this immediately
     * after posting one, and the user should still see it.
     */
    suspend fun schedulePlan(
        context: Context,
        repository: AroRepository,
        planId: String,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        mutex.withLock {
            val item = repository.getPlanItem(planId)
            if (item == null) {
                cancelAlarm(context, planId)
            } else {
                scheduleOne(context, repository, item, now)
            }
        }
    }

    suspend fun cancelPlan(context: Context, planId: String) {
        mutex.withLock {
            cancelAlarm(context, planId)
        }
    }

    private suspend fun scheduleOne(
        context: Context,
        repository: AroRepository,
        item: PlanItem,
        now: LocalDateTime,
    ) {
        if (!item.reminderEnabled) {
            cancelAlarm(context, item.id)
            return
        }

        val completedDates = repository.getPlanCompletionDates(
            planId = item.id,
            from = now.toLocalDate(),
            toInclusive = now.toLocalDate().plusYears(REMINDER_HORIZON_YEARS),
        )
        val reminder = item.nextReminderOccurrence(now, completedDates)
        if (reminder == null) {
            cancelAlarm(context, item.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAtMillis = reminder.triggerAt
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(item.id),
            alarmIntent(context, item.id, reminder.date),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        AroNotificationScheduler.scheduleAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    private fun cancelAlarm(context: Context, planId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(planId),
            alarmIntent(context, planId, null),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        pendingIntent?.let(alarmManager::cancel)
    }

    private fun alarmIntent(context: Context, planId: String, date: LocalDate?): Intent =
        Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = AlarmBroadcastReceiver.ACTION_FIRE_ALARM
            putExtra(AroNotificationScheduler.EXTRA_ALARM_TYPE, TYPE_PLAN)
            putExtra(NotificationHelper.EXTRA_PLAN_ID, planId)
            if (date != null) {
                putExtra(NotificationHelper.EXTRA_PLAN_DATE_EPOCH_DAY, date.toEpochDay())
            }
        }
}
