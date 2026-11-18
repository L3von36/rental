package com.loypos.app.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
import com.loypos.app.data.local.ProductEntity
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object LowStockNotifier {
    const val CHANNEL_ID = "loypos_low_stock_alerts"
    const val EXTRA_OPEN_LOW_STOCK = "com.loypos.app.OPEN_LOW_STOCK"
    private const val NOTIFICATION_ID = 4102

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Low Stock Alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Background alerts when Room inventory falls below your defined threshold."
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showLowStockNotification(
        context: Context,
        lowStockItems: List<ProductEntity>,
        threshold: Double,
    ): Boolean {
        if (lowStockItems.isEmpty() || !hasNotificationPermission(context)) return false
        ensureChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_LOW_STOCK, true)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val sorted = lowStockItems.sortedWith(compareBy<ProductEntity> { it.stock }.thenBy { it.name })
        val outOfStockCount = sorted.count { it.stock <= 0.0 }
        val count = sorted.size

        val title = when {
            count == 1 && outOfStockCount == 1 -> "Out of stock: ${sorted.first().name}"
            count == 1 -> "Low stock alert: ${sorted.first().name}"
            outOfStockCount > 0 -> "$count items low ($outOfStockCount out of stock)"
            else -> "$count products below ${formatQty(threshold)} in stock"
        }

        val summaryLine = if (count == 1) {
            val item = sorted.first()
            val skuNote = if (item.sku.isNotEmpty()) " · SKU ${item.sku}" else ""
            if (item.stock <= 0.0) {
                "Out of stock (threshold: < ${formatQty(threshold)})$skuNote"
            } else {
                "Only ${formatQty(item.stock)} left (threshold: < ${formatQty(threshold)})$skuNote"
            }
        } else {
            val preview = sorted.take(3).joinToString(", ") {
                "${it.name} (${if (it.stock <= 0.0) "0" else formatQty(it.stock)})"
            }
            if (count > 3) "$preview +${count - 3} more" else preview
        }

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText("Threshold: < ${formatQty(threshold)} · Tap to restock")
        sorted.take(5).forEach { item ->
            val stockText = if (item.stock <= 0.0) "Out of stock" else "${formatQty(item.stock)} left"
            val skuTag = if (item.sku.isNotEmpty()) " [${item.sku}]" else ""
            inboxStyle.addLine("${item.name}$skuTag — $stockText")
        }
        if (count > 5) {
            inboxStyle.addLine("+${count - 5} more items need restocking")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_low_stock_alert)
            .setContentTitle(title)
            .setContentText(summaryLine)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Review & Restock", contentIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

class LowStockCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = LoyPosRepository(applicationContext)
        repo.ensureInitialized()
        val settings = repo.settingsFlow.first()
        val forceNotify = inputData.getBoolean(KEY_FORCE_NOTIFY, false)

        val threshold = settings.lowStockThreshold.coerceAtLeast(0.0)
        val lowItems = repo.getLowStockProducts(threshold)
        repo.saveLowStockCheckResult(nowMs(), lowItems.size)

        if ((settings.lowStockAlertsEnabled || forceNotify) && lowItems.isNotEmpty()) {
            LowStockNotifier.showLowStockNotification(applicationContext, lowItems, threshold)
        }
        return Result.success(
            workDataOf(
                KEY_LOW_COUNT to lowItems.size,
                KEY_CHECKED_AT to nowMs(),
            )
        )
    }

    companion object {
        const val KEY_FORCE_NOTIFY = "force_notify"
        const val KEY_LOW_COUNT = "low_count"
        const val KEY_CHECKED_AT = "checked_at"
    }
}

object LowStockWorkScheduler {
    const val PERIODIC_WORK_NAME = "loypos_low_stock_periodic_check"
    private const val IMMEDIATE_WORK_NAME = "loypos_low_stock_immediate_check"

    fun schedulePeriodicCheck(
        context: Context,
        intervalMinutes: Long = 15L,
    ) {
        LowStockNotifier.ensureChannel(context)
        val clampedMinutes = intervalMinutes.coerceAtLeast(15L)
        val request = PeriodicWorkRequestBuilder<LowStockCheckWorker>(
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
        LowStockNotifier.ensureChannel(context)
        val request = OneTimeWorkRequestBuilder<LowStockCheckWorker>()
            .setInputData(workDataOf(LowStockCheckWorker.KEY_FORCE_NOTIFY to forceNotify))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelPeriodicCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }
}
