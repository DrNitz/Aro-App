package com.drnitz.aro

import android.content.Intent
import com.drnitz.aro.notifications.NotificationHelper
import java.time.LocalDate

/**
 * A plan-notification tap, reduced to the date and plan the user wanted to open.
 *
 * [requestId] is deterministic for one plan/date pair so activity recreation does not
 * replay an already-consumed deep link.
 */
data class PlanDeepLink(
    val date: LocalDate,
    val planId: String?,
) {
    val requestId: Long
        get() = ((planId?.hashCode()?.toLong() ?: 0L) shl 32) xor date.toEpochDay()

    companion object {
        fun fromIntent(intent: Intent?): PlanDeepLink? {
            if (intent == null) return null
            val epochDay = intent.getLongExtra(
                NotificationHelper.EXTRA_PLAN_DATE_EPOCH_DAY,
                Long.MIN_VALUE,
            )
            if (epochDay == Long.MIN_VALUE) return null
            return PlanDeepLink(
                date = LocalDate.ofEpochDay(epochDay),
                planId = intent.getStringExtra(NotificationHelper.EXTRA_PLAN_ID),
            )
        }
    }
}
