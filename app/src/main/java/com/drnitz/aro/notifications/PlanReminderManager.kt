package com.drnitz.aro.notifications

import android.content.Context
import com.drnitz.aro.data.repository.AroRepository
import java.time.LocalDate

/** Android-backed implementation of the plan reminder side effects. */
class PlanReminderManager(
    context: Context,
    private val repository: AroRepository,
) : PlanReminderScheduler {

    private val appContext = context.applicationContext

    override suspend fun reschedulePlan(
        planId: String,
        completedDate: LocalDate?,
        dismissNotification: Boolean,
    ) {
        if (dismissNotification) {
            // One visible notification per series is enough, and an edit/completion
            // makes the old occurrence's notification stale.
            NotificationHelper.cancelPlanNotification(appContext, planId)
        }
        PlanAlarmScheduler.schedulePlan(appContext, repository, planId)
    }

    override suspend fun cancelPlan(planId: String) {
        PlanAlarmScheduler.cancelPlan(appContext, planId)
        NotificationHelper.cancelPlanNotification(appContext, planId)
    }

    override suspend fun rescheduleAll() {
        PlanAlarmScheduler.scheduleAll(appContext, repository)
    }
}
