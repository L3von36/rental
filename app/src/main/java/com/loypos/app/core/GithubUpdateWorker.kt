package com.loypos.app.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.loypos.app.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker that queries the GitHub Releases API for new APK versions
 * and notifies the user if an update is available, preventing 'app not installed' confusion.
 */
class GithubUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val CHANNEL_ID = "loypos_updates_channel"
        const val NOTIFICATION_ID = 9912
        const val WORK_NAME = "GithubUpdateCheckPeriodicWork"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<GithubUpdateWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun checkNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = androidx.work.OneTimeWorkRequestBuilder<GithubUpdateWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            // GitHub Releases API endpoint for LoyPOS Event Rental
            val apiUrl = "https://api.github.com/repos/L3von36/rental/releases/latest"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "LoyPOS-Android-App")
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                return Result.retry()
            }

            val responseText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            conn.disconnect()

            val json = JSONObject(responseText)
            val tagName = json.optString("tagName", json.optString("tag_name", "")).trim()
            val htmlUrl = json.optString("html_url", "").trim()
            val releaseNotes = json.optString("body", "New version available on GitHub.").trim()

            if (tagName.isNotEmpty()) {
                val remoteClean = tagName.removePrefix("v").removePrefix("V").trim()
                val localClean = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V").trim()

                // Simple version comparison or inequality check
                if (remoteClean.isNotEmpty() && remoteClean != localClean) {
                    showUpdateNotification(context, tagName, releaseNotes, htmlUrl.ifEmpty { "https://github.com/L3von36/rental/releases" })
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showUpdateNotification(context: Context, version: String, notes: String, url: String) {
        val notificationManager = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Notifications for new LoyPOS APK releases on GitHub"
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("LoyPOS Update Available ($version)")
            .setContentText(notes.take(90))
            .setStyle(NotificationCompat.BigTextStyle().bigText("A new version ($version) of LoyPOS is available on GitHub releases. Tap to download and install to avoid 'app not installed' errors."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted for notifications
        }
    }
}
