package com.drnitz.aro.notifications

import java.time.LocalDate

/**
 * The side-effect boundary between view-model plan actions and Android alarms.
 *
 * Keeping this interface Android-free lets the view-model stay unit-testable with a
 * no-op implementation while the real implementation lives in [PlanReminderManager].
 */
interface PlanReminderScheduler {

    /**
     * Recomputes the single next alarm for a series.
     *
     * When [completedDate] is supplied it identifies the occurrence that changed.
     * [dismissNotification] also clears any visible reminder for the series; save and
     * completion paths use it, while uncompleting an older occurrence does not need to
     * hide a legitimate reminder for a different date.
     */
    suspend fun reschedulePlan(
        planId: String,
        completedDate: LocalDate? = null,
        dismissNotification: Boolean = false,
    )

    /** Cancels both the pending alarm and any visible notification for a deleted plan. */
    suspend fun cancelPlan(planId: String)

    /** Rebuilds alarms for every series after boot, package replacement, or app start. */
    suspend fun rescheduleAll()

    object NoOp : PlanReminderScheduler {
        override suspend fun reschedulePlan(
            planId: String,
            completedDate: LocalDate?,
            dismissNotification: Boolean,
        ) = Unit

        override suspend fun cancelPlan(planId: String) = Unit

        override suspend fun rescheduleAll() = Unit
    }
}
