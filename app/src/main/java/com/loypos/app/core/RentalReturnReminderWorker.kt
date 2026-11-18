package com.loypos.app.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.loypos.app.MainActivity
import com.loypos.app.R
import com.loypos.app.data.LoyPosRepository
import com.loypos.app.data.local.SaleEntity
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class RentalReturnReminderCandidate(
    val sale: SaleEntity,
    val triggerAtMs: Long,
    val msUntilDue: Long,
    val isOverdue: Boolean,
)

/**
 * Pure calculation & formatting helpers for 24-hour rental return notifications.
 * Kept free of Android framework state so it can run in fast local JVM tests as well.
 */
object RentalReturnReminderCalculator {
    const val TWENTY_FOUR_HOURS_MS = 86_400_000L
    const val SAME_DAY_CHECKOUT_GRACE_MS = 60_000L
    private const val MAX_STORED_TOKENS = 300

    fun reminderToken(saleId: String, rentalEndMs: Long): String = "$saleId:$rentalEndMs"

    fun computeReminderTriggerAtMs(
        rentalStartMs: Long,
        rentalEndMs: Long,
        createdAt: Long,
    ): Long {
        if (rentalEndMs <= 0L) return Long.MAX_VALUE
        val exact24hBefore = rentalEndMs - TWENTY_FOUR_HOURS_MS
        // For multi-day or future-dated rentals, trigger strictly 24 hours before rentalEndMs.
        // For an immediate 1-day rental checked out right now, give a 60s post-checkout grace
        // so the confirmation screen is not immediately interrupted by a notification popup.
        return if (exact24hBefore <= createdAt + SAME_DAY_CHECKOUT_GRACE_MS &&
            rentalStartMs <= createdAt + SAME_DAY_CHECKOUT_GRACE_MS &&
            rentalEndMs > createdAt + SAME_DAY_CHECKOUT_GRACE_MS * 2
        ) {
            createdAt + SAME_DAY_CHECKOUT_GRACE_MS
        } else {
            exact24hBefore
        }
    }

    fun computeDelayMs(sale: SaleEntity, nowMs: Long): Long {
        val triggerAt = computeReminderTriggerAtMs(
            rentalStartMs = sale.rentalStartMs,
            rentalEndMs = sale.rentalEndMs,
            createdAt = sale.createdAt,
        )
        return (triggerAt - nowMs).coerceAtLeast(0L)
    }

    fun isDueFor24HourReminder(
        sale: SaleEntity,
        nowMs: Long,
        notifiedTokens: Set<String>,
        forceIncludeNotified: Boolean = false,
    ): Boolean {
        if (!sale.isRentalOut || sale.rentalEndMs <= 0L) return false
        val token = reminderToken(sale.id, sale.rentalEndMs)
        if (!forceIncludeNotified && token in notifiedTokens) return false
        val in24HourWindow = nowMs >= (sale.rentalEndMs - TWENTY_FOUR_HOURS_MS)
        if (!in24HourWindow) return false
        if (!forceIncludeNotified) {
            val triggerAt = computeReminderTriggerAtMs(sale.rentalStartMs, sale.rentalEndMs, sale.createdAt)
            if (nowMs < triggerAt) return false
        }
        return true
    }

    fun findDueRentals(
        sales: List<SaleEntity>,
        nowMs: Long,
        notifiedTokens: Set<String>,
        forceIncludeNotified: Boolean = false,
    ): List<RentalReturnReminderCandidate> {
        return sales
            .filter { isDueFor24HourReminder(it, nowMs, notifiedTokens, forceIncludeNotified) }
            .map { sale ->
                val triggerAt = computeReminderTriggerAtMs(sale.rentalStartMs, sale.rentalEndMs, sale.createdAt)
                val delta = sale.rentalEndMs - nowMs
                RentalReturnReminderCandidate(
                    sale = sale,
                    triggerAtMs = triggerAt,
                    msUntilDue = delta,
                    isOverdue = delta < 0L,
                )
            }
            .sortedBy { it.sale.rentalEndMs }
    }

    fun formatRemainingLabel(msUntilDue: Long): String {
        return if (msUntilDue < 0L) {
            val overdueMins = abs(msUntilDue) / 60_000L
            val overdueHours = overdueMins / 60L
            val overdueDays = overdueHours / 24L
            when {
                overdueDays >= 1L -> "Overdue by ${overdueDays}d"
                overdueHours >= 1L -> "Overdue by ${overdueHours}h"
                else -> "Overdue by ${overdueMins.coerceAtLeast(1L)}m"
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

    fun decodeNotifiedTokens(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    fun encodeNotifiedTokens(tokens: Set<String>): String =
        tokens.toList().takeLast(MAX_STORED_TOKENS).joinToString(",")

    fun pruneTokensForActiveSales(tokens: Set<String>, allSales: List<SaleEntity>): Set<String> {
        val activeTokens = allSales
            .asSequence()
            .filter { it.isRentalOut && it.rentalEndMs > 0L }
            .map { reminderToken(it.id, it.rentalEndMs) }
            .toSet()
        return tokens.filter { it in activeTokens }.toSet()
    }
}

object RentalReturnNotifier {
    const val CHANNEL_ID = "loypos_rental_return_reminders"
    const val EXTRA_OPEN_RETURNS = "com.loypos.app.OPEN_RETURNS"
    const val EXTRA_OPEN_RENTAL_ID = "com.loypos.app.OPEN_RENTAL_ID"
    private const val NOTIFICATION_ID = 4208

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rental Return Reminders (24h)",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Local alerts triggered 24 hours before a scheduled event rental return date."
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun showReturnReminders(
        context: Context,
        candidates: List<RentalReturnReminderCandidate>,
    ): Boolean {
        if (candidates.isEmpty() || !LowStockNotifier.hasNotificationPermission(context)) return false
        ensureChannel(context)

        val single = candidates.singleOrNull()
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (single != null) {
                putExtra(EXTRA_OPEN_RENTAL_ID, single.sale.id)
            } else {
                putExtra(EXTRA_OPEN_RETURNS, true)
            }
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val overdueCount = candidates.count { it.isOverdue }
        val count = candidates.size

        val title = when {
            single != null -> {
                val orderTag = "#${single.sale.number.toString().padStart(4, '0')}"
                val statusWord = if (single.isOverdue) "Overdue return" else "Return reminder"
                val timing = RentalReturnReminderCalculator.formatRemainingLabel(single.msUntilDue)
                "$statusWord: $orderTag ($timing)"
            }
            overdueCount > 0 -> "$count rentals need return ($overdueCount overdue)"
            else -> "$count rental returns due within 24h"
        }

        val summaryLine = if (single != null) {
            val sale = single.sale
            val client = sale.customerName?.takeIf { it.isNotBlank() } ?: "Walk-in client"
            val eventPart = sale.eventName.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
            "$client$eventPart · Return ${formatDateTime(sale.rentalEndMs)}"
        } else {
            val preview = candidates.take(3).joinToString(", ") { c ->
                val tag = "#${c.sale.number.toString().padStart(4, '0')}"
                val who = c.sale.customerName?.takeIf { it.isNotBlank() } ?: c.sale.eventName.ifBlank { "Order" }
                "$tag $who (${RentalReturnReminderCalculator.formatRemainingLabel(c.msUntilDue)})"
            }
            if (count > 3) "$preview +${count - 3} more" else preview
        }

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText("Tap to inspect scheduled equipment returns")
        candidates.take(5).forEach { candidate ->
            val sale = candidate.sale
            val orderTag = "#${sale.number.toString().padStart(4, '0')}"
            val who = sale.customerName?.takeIf { it.isNotBlank() } ?: "Walk-in"
            val ev = sale.eventName.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
            val timing = RentalReturnReminderCalculator.formatRemainingLabel(candidate.msUntilDue)
            inboxStyle.addLine("$orderTag · $who$ev — $timing (${formatDateTime(sale.rentalEndMs)})")
        }
        if (count > 5) {
            inboxStyle.addLine("+${count - 5} more scheduled returns due")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_low_stock_alert)
            .setContentTitle(title)
            .setContentText(summaryLine)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, if (single != null) "View Rental" else "View Rentals", contentIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

class RentalReturnReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = LoyPosRepository(applicationContext)
        repo.ensureInitialized()
        val settings = repo.settingsFlow.first()
        val forceNotify = inputData.getBoolean(KEY_FORCE_NOTIFY, false)
        val targetedSaleId = inputData.getString(KEY_SALE_ID)?.takeIf { it.isNotBlank() }

        val now = nowMs()
        val allSales = repo.saleDao.getAllSales()
        val existingTokens = RentalReturnReminderCalculator.decodeNotifiedTokens(
            repo.dao.getSetting(SETTING_NOTIFIED_TOKENS)
        )
        val activeTokens = RentalReturnReminderCalculator.pruneTokensForActiveSales(existingTokens, allSales)

        val scopeSales = if (targetedSaleId != null && !forceNotify) {
            // Check targeted rental AND any other active rentals that happened to cross 24h
            val targeted = allSales.filter { it.id == targetedSaleId }
            val others = allSales.filter { it.id != targetedSaleId }
            targeted + others
        } else {
            allSales
        }

        val dueCandidates = RentalReturnReminderCalculator.findDueRentals(
            sales = scopeSales,
            nowMs = now,
            notifiedTokens = activeTokens,
            forceIncludeNotified = forceNotify,
        )

        repo.saveReturnReminderCheckResult(now, dueCandidates.size)

        var updatedTokens = activeTokens
        if ((settings.returnReminderAlertsEnabled || forceNotify) && dueCandidates.isNotEmpty()) {
            val shown = RentalReturnNotifier.showReturnReminders(applicationContext, dueCandidates)
            if (shown) {
                val newlyNotified = dueCandidates.map {
                    RentalReturnReminderCalculator.reminderToken(it.sale.id, it.sale.rentalEndMs)
                }
                updatedTokens = activeTokens + newlyNotified
            }
        }
        if (updatedTokens != existingTokens) {
            repo.saveReturnReminderNotifiedTokens(
                RentalReturnReminderCalculator.encodeNotifiedTokens(updatedTokens)
            )
        }

        return Result.success(
            workDataOf(
                KEY_DUE_COUNT to dueCandidates.size,
                KEY_CHECKED_AT to now,
                KEY_NOTIFIED_ORDER_NUMBERS to dueCandidates.take(5).joinToString(", ") {
                    "#${it.sale.number.toString().padStart(4, '0')}"
                },
            )
        )
    }

    companion object {
        const val KEY_SALE_ID = "sale_id"
        const val KEY_FORCE_NOTIFY = "force_notify"
        const val KEY_DUE_COUNT = "due_count"
        const val KEY_CHECKED_AT = "checked_at"
        const val KEY_NOTIFIED_ORDER_NUMBERS = "notified_order_numbers"
        const val SETTING_NOTIFIED_TOKENS = "return_reminder_notified_tokens"
    }
}

object RentalReturnWorkScheduler {
    const val PERIODIC_WORK_NAME = "loypos_rental_return_periodic_check"
    private const val IMMEDIATE_WORK_NAME = "loypos_rental_return_immediate_check"
    private fun perRentalWorkName(saleId: String) = "loypos_return_24h_$saleId"

    fun scheduleForRental(
        context: Context,
        sale: SaleEntity,
        currentTimeMs: Long = nowMs(),
    ) {
        if (!sale.isRentalOut || sale.rentalEndMs <= 0L) {
            cancelForRental(context, sale.id)
            return
        }
        RentalReturnNotifier.ensureChannel(context)
        val delayMs = RentalReturnReminderCalculator.computeDelayMs(sale, currentTimeMs)
        val request = OneTimeWorkRequestBuilder<RentalReturnReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(RentalReturnReminderWorker.KEY_SALE_ID to sale.id))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            perRentalWorkName(sale.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelForRental(context: Context, saleId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(perRentalWorkName(saleId))
    }

    fun syncAllActiveRentals(
        context: Context,
        sales: List<SaleEntity>,
        currentTimeMs: Long = nowMs(),
    ) {
        sales.forEach { sale ->
            if (sale.isRentalOut && sale.rentalEndMs > 0L) {
                scheduleForRental(context, sale, currentTimeMs)
            } else {
                cancelForRental(context, sale.id)
            }
        }
    }

    fun schedulePeriodicSweep(
        context: Context,
        intervalMinutes: Long = 60L,
    ) {
        RentalReturnNotifier.ensureChannel(context)
        val clampedMinutes = intervalMinutes.coerceAtLeast(15L)
        val request = PeriodicWorkRequestBuilder<RentalReturnReminderWorker>(
            clampedMinutes,
            TimeUnit.MINUTES,
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun runImmediateCheck(
        context: Context,
        forceNotify: Boolean = false,
    ) {
        RentalReturnNotifier.ensureChannel(context)
        val request = OneTimeWorkRequestBuilder<RentalReturnReminderWorker>()
            .setInputData(workDataOf(RentalReturnReminderWorker.KEY_FORCE_NOTIFY to forceNotify))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelAll(
        context: Context,
        sales: List<SaleEntity> = emptyList(),
    ) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(PERIODIC_WORK_NAME)
        wm.cancelUniqueWork(IMMEDIATE_WORK_NAME)
        sales.forEach { wm.cancelUniqueWork(perRentalWorkName(it.id)) }
    }
}
