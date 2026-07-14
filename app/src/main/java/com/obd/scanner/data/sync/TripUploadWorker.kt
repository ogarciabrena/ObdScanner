package com.obd.scanner.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.obd.scanner.ObdScannerApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Uploads completed trip files (JSONL) from the pending directory to Supabase,
 * then deletes them. Retries with backoff while offline.
 */
class TripUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as ObdScannerApp
        val creds = app.syncSettings.current()
        if (!creds.isConfigured) return@withContext Result.success()

        // Con Auth activado, se sube con el token del usuario (RLS ata cada
        // viaje a su cuenta). Sin sesión, los viajes quedan en cola local.
        val token = app.authManager.validAccessToken() ?: return@withContext Result.retry()

        val files = app.tripRecorder.listPendingTrips()
        if (files.isEmpty()) return@withContext Result.success()

        var anyFailure = false
        for (file in files) {
            try {
                if (uploadTrip(file, creds, token)) {
                    file.delete()
                } else {
                    anyFailure = true
                }
            } catch (_: Exception) {
                anyFailure = true
            }
        }
        if (anyFailure) Result.retry() else Result.success()
    }

    private fun uploadTrip(file: File, creds: SupabaseCredentials, token: String): Boolean {
        val tripId = file.nameWithoutExtension
        var device = ""
        var startTs = 0L
        var endTs = 0L
        var sampleCount = 0
        val samples = mutableListOf<JSONObject>()

        file.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val obj = try { JSONObject(line) } catch (_: Exception) { return@forEachLine }
            when (obj.optString("type")) {
                "start" -> {
                    startTs = obj.optLong("ts")
                    device = obj.optString("device")
                }
                "end" -> {
                    endTs = obj.optLong("ts")
                    sampleCount = obj.optInt("samples")
                }
                "sample" -> samples.add(
                    JSONObject()
                        .put("trip_id", tripId)
                        .put("ts", obj.optLong("ts"))
                        .put("pid", obj.optInt("pid"))
                        .put("name", obj.optString("name"))
                        .put("value", obj.optDouble("value"))
                        .put("unit", obj.optString("unit"))
                )
            }
        }
        // Recovered partial trips may lack start/end records; derive them from
        // the samples. A file with no samples at all is poison — delete it so
        // it can't jam the upload queue with endless retries.
        if (startTs == 0L) startTs = samples.firstOrNull()?.optLong("ts") ?: 0L
        if (startTs == 0L) {
            file.delete()
            return true
        }
        if (endTs == 0L) endTs = samples.lastOrNull()?.optLong("ts") ?: startTs
        if (sampleCount == 0) sampleCount = samples.size

        val trip = JSONObject()
            .put("id", tripId)
            .put("device", device)
            .put("start_ts", startTs)
            .put("end_ts", endTs)
            .put("sample_count", sampleCount)

        if (!post(creds, token, "trips?on_conflict=id", JSONArray().put(trip), "resolution=merge-duplicates")) {
            return false
        }

        samples.chunked(500).forEach { chunk ->
            val body = JSONArray().apply { chunk.forEach { put(it) } }
            if (!post(creds, token, "telemetry?on_conflict=trip_id,ts,pid", body, "resolution=ignore-duplicates")) {
                return false
            }
        }
        return true
    }

    private fun post(creds: SupabaseCredentials, token: String, path: String, body: JSONArray, prefer: String): Boolean {
        val conn = URL("${creds.url}/rest/v1/$path").openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.doOutput = true
            conn.setRequestProperty("apikey", creds.key)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", prefer)
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            conn.responseCode in 200..299
        } catch (_: Exception) {
            false
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val WORK_NAME = "trip_upload"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<TripUploadWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
