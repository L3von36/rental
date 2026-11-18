package com.loypos.desktop.core

import com.loypos.desktop.model.DesktopRentalOrder
import kotlin.math.abs

data class DesktopReturnReminder(
    val order: DesktopRentalOrder,
    val triggerAtMs: Long,
    val msUntilDue: Long,
    val isOverdue: Boolean,
)

object DesktopRentalReturnReminderCalculator {
    const val TWENTY_FOUR_HOURS_MS = 86_400_000L

    fun findRentalsDueWithin24Hours(
        orders: List<DesktopRentalOrder>,
        nowMs: Long,
    ): List<DesktopReturnReminder> {
        return orders
            .asSequence()
            .filter { it.isRentalOut && it.rentalEndMs > 0L }
            .filter { nowMs >= it.rentalEndMs - TWENTY_FOUR_HOURS_MS }
            .map { order ->
                val delta = order.rentalEndMs - nowMs
                DesktopReturnReminder(
                    order = order,
                    triggerAtMs = order.rentalEndMs - TWENTY_FOUR_HOURS_MS,
                    msUntilDue = delta,
                    isOverdue = delta < 0L,
                )
            }
            .sortedBy { it.order.rentalEndMs }
            .toList()
    }

    fun formatRemainingLabel(msUntilDue: Long): String {
        return if (msUntilDue < 0L) {
            val overdueMins = abs(msUntilDue) / 60_000L
            val overdueHours = overdueMins / 60L
            val overdueDays = overdueHours / 24L
            when {
                overdueDays >= 1L -> "Overdue ${overdueDays}d"
                overdueHours >= 1L -> "Overdue ${overdueHours}h"
                else -> "Overdue ${overdueMins.coerceAtLeast(1L)}m"
            }
        } else {
            val remMins = msUntilDue / 60_000L
            val remHours = remMins / 60L
            when {
                remHours >= 24L -> "Due in 24h"
                remHours >= 1L -> "Due in ${remHours}h"
                remMins >= 1L -> "Due in ${remMins}m"
                else -> "Due now"
            }
        }
    }
}
