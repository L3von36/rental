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
                "Equipment Safety Alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "WorkManager alerts when specific event equipment drops below its defined safety threshold."
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
        globalThreshold: Double,
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

        val sorted = lowStockItems.sortedWith(
            compareBy<ProductEntity> { it.stock > 0.0 }
                .thenBy {
                    val th = it.effectiveSafetyThreshold(globalThreshold)
                    if (th > 0.0) it.stock / th else it.stock
                }
                .thenBy { it.name }
        )
        val outOfStockCount = sorted.count { it.stock <= 0.0 }
        val customThresholdCount = sorted.count { it.safetyThreshold != null }
        val count = sorted.size

        val title = when {
            count == 1 && outOfStockCount == 1 -> "Rented out: ${sorted.first().name}"
            count == 1 -> "Safety alert: ${sorted.first().name}"
            outOfStockCount > 0 -> "$count equipment items below safety ($outOfStockCount out)"
            else -> "$count equipment items below safety threshold"
        }

        val summaryLine = if (count == 1) {
            val item = sorted.first()
            val itemMin = item.effectiveSafetyThreshold(globalThreshold)
            val skuNote = if (item.sku.isNotEmpty()) " · SKU ${item.sku}" else ""
            if (item.stock <= 0.0) {
                "0 available (safety min: ${formatQty(itemMin)})$skuNote"
            } else {
                "${formatQty(item.stock)} available (below safety min: ${formatQty(itemMin)})$skuNote"
            }
        } else {
            val preview = sorted.take(3).joinToString(", ") {
                val min = it.effectiveSafetyThreshold(globalThreshold)
                val qtyStr = if (it.stock <= 0.0) "0" else formatQty(it.stock)
                "${it.name} ($qtyStr/${formatQty(min)})"
            }
            if (count > 3) "$preview +${count - 3} more" else preview
        }

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText(
                if (customThresholdCount > 0) {
                    "$customThresholdCount custom item thresholds · Tap to review fleet"
                } else {
                    "Safety threshold: < ${formatQty(globalThreshold)} · Tap to review fleet"
                }
            )
        sorted.take(5).forEach { item ->
            val itemMin = item.effectiveSafetyThreshold(globalThreshold)
            val stockText = if (item.stock <= 0.0) "0 avail" else "${formatQty(item.stock)} avail"
            val skuTag = if (item.sku.isNotEmpty()) " [${item.sku}]" else ""
            inboxStyle.addLine("${item.name}$skuTag — $stockText (min ${formatQty(itemMin)})")
        }
        if (count > 5) {
            inboxStyle.addLine("+${count - 5} more equipment items need replenishment")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_low_stock_alert)
            .setContentTitle(title)
            .setContentText(summaryLine)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Review Fleet", contentIntent)
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

        val globalThreshold = settings.lowStockThreshold.coerceAtLeast(0.0)
        val lowItems = repo.getLowStockProducts(globalThreshold)
        val checkedAt = nowMs()
        repo.saveLowStockCheckResult(checkedAt, lowItems.size)

        if ((settings.lowStockAlertsEnabled || forceNotify) && lowItems.isNotEmpty()) {
            LowStockNotifier.showLowStockNotification(applicationContext, lowItems, globalThreshold)
        }
        return Result.success(
            workDataOf(
                KEY_LOW_COUNT to lowItems.size,
                KEY_CHECKED_AT to checkedAt,
                KEY_ALERT_ITEM_NAMES to lowItems.take(5).joinToString(", ") { it.name },
            )
        )
    }

    companion object {
        const val KEY_FORCE_NOTIFY = "force_notify"
        const val KEY_LOW_COUNT = "low_count"
        const val KEY_CHECKED_AT = "checked_at"
        const val KEY_ALERT_ITEM_NAMES = "alert_item_names"
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
