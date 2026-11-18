package com.loypos.app.core

import android.content.Context
import com.loypos.app.BuildConfig
import com.loypos.app.data.LoyPosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Plesk MySQL/MariaDB HTTPS REST sync client targeting novelsapps.com.et.
 * Uses EncryptedTokenManager (Android Keystore / EncryptedSharedPreferences) or BuildConfig.PLESK_SYNC_TOKEN.
 * Local Room SQLite remains the offline-first runtime store so POS checkout never blocks.
 */
object CloudSyncClient {
    val ENDPOINT: String get() = BuildConfig.PLESK_API_BASE_URL.trimEnd('/')

    fun resolveSyncToken(context: Context): String {
        val secureToken = SecurityManager.getSecureToken(context)
        if (secureToken.isNotBlank()) return secureToken

        return BuildConfig.PLESK_SYNC_TOKEN
            .trim()
            .takeIf { it.isNotEmpty() && it != "YOUR_PLESK_SYNC_TOKEN" }
            .orEmpty()
    }

    sealed class SyncOutcome {
        data class Success(val message: String) : SyncOutcome()
        data class Offline(val message: String) : SyncOutcome()
    }

    suspend fun syncWithCloud(context: Context, repo: LoyPosRepository): SyncOutcome = withContext(Dispatchers.IO) {
        val raw = repo.loadRawSettings()
        val lastSyncMs = raw["last_sync_ms"]?.toLongOrNull() ?: 0L
        val token = resolveSyncToken(context)

        if (token.isEmpty()) {
            return@withContext SyncOutcome.Offline(
                "Enter Plesk Sync Token in Settings (secured by Android Keystore) to enable cloud sync · saving offline"
            )
        }

        try {
            // 1. Verify Plesk API & MySQL connectivity
            val health = httpGetJson("$ENDPOINT/health", token)
            if (health.code !in 200..299) {
                val serverErr = health.json?.optString("error")?.takeIf { it.isNotEmpty() }
                val hint = when (health.code) {
                    401, 403 -> "Plesk API rejected sync token (HTTP ${health.code})"
                    404 -> "Plesk API not deployed at $ENDPOINT yet (HTTP 404)"
                    503 -> serverErr ?: "Plesk MySQL database not created or unavailable (HTTP 503)"
                    else -> serverErr ?: "Plesk server returned HTTP ${health.code}"
                }
                return@withContext SyncOutcome.Offline("$hint · local Room store active")
            }

            // 2. Push incremental/full local Room changes to Plesk MySQL
            val (payload, localCount) = repo.exportPleskSyncPayload(lastSyncMs)
            val pushRes = httpPostJson("$ENDPOINT/sync/push", token, payload)
            if (pushRes.code !in 200..299) {
                val err = pushRes.json?.optString("error")?.takeIf { it.isNotEmpty() }
                    ?: "Push failed (HTTP ${pushRes.code})"
                return@withContext SyncOutcome.Offline("Plesk sync error: $err · saved locally")
            }
            val pushedServerCount = pushRes.json?.optInt("upserted", localCount) ?: localCount

            // 3. Pull remote changes updated since lastSyncMs from Plesk MySQL
            val pullRes = httpGetJson("$ENDPOINT/sync/pull?since=$lastSyncMs", token)
            var pulledCount = 0
            if (pullRes.code in 200..299 && pullRes.json != null) {
                pulledCount = repo.applyPleskPullPayload(pullRes.json)
            }

            val serverTimeMs = pullRes.json?.optLong("server_time_ms")
                ?: pushRes.json?.optLong("server_time_ms")
                ?: nowMs()
            repo.saveSetting("last_sync_ms", serverTimeMs.toString())

            SyncOutcome.Success(
                "Synced with Plesk MySQL · ↑$pushedServerCount pushed · ↓$pulledCount pulled"
            )
        } catch (e: Exception) {
            SyncOutcome.Offline(
                "Plesk unreachable (${e.javaClass.simpleName}) · all sales & stock saved locally"
            )
        }
    }

    private data class HttpJsonResult(val code: Int, val raw: String, val json: JSONObject?)

    private fun httpGetJson(urlString: String, token: String): HttpJsonResult {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 10000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-Sync-Token", token)
        }
        return readResponse(conn)
    }

    private fun httpPostJson(urlString: String, token: String, body: JSONObject): HttpJsonResult {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 7000
            readTimeout = 15000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-Sync-Token", token)
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(body.toString())
            writer.flush()
        }
        return readResponse(conn)
    }

    private fun readResponse(conn: HttpURLConnection): HttpJsonResult {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val raw = runCatching {
            stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        }.getOrDefault("")
        conn.disconnect()
        val parsed = runCatching { if (raw.isNotBlank()) JSONObject(raw) else null }.getOrNull()
        return HttpJsonResult(code, raw, parsed)
    }
}
