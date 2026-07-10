package com.obd.scanner.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.obd.scanner.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val Context.syncDataStore by preferencesDataStore(name = "sync_settings")

data class SupabaseCredentials(val url: String, val key: String) {
    val isConfigured: Boolean get() = url.startsWith("https://") && key.isNotBlank()
}

/**
 * Supabase credentials: user-editable via Settings, falling back to
 * compile-time defaults injected from local.properties (empty on clean clones —
 * the app then works fully offline until the user configures their own project).
 */
class SyncSettings(private val context: Context) {

    private val urlKey = stringPreferencesKey("supabase_url")
    private val keyKey = stringPreferencesKey("supabase_key")

    private val defaultUrl by lazy { context.getString(R.string.supabase_url_default) }
    private val defaultKey by lazy { context.getString(R.string.supabase_key_default) }

    val credentials: Flow<SupabaseCredentials> = context.syncDataStore.data.map { prefs ->
        SupabaseCredentials(
            url = prefs[urlKey]?.takeIf { it.isNotBlank() } ?: defaultUrl,
            key = prefs[keyKey]?.takeIf { it.isNotBlank() } ?: defaultKey
        )
    }

    suspend fun current(): SupabaseCredentials = credentials.first()

    suspend fun save(url: String, key: String) {
        context.syncDataStore.edit { prefs ->
            prefs[urlKey] = url.trim().trimEnd('/')
            prefs[keyKey] = key.trim()
        }
    }
}

/**
 * Hits the trips table with the given credentials and reports a concrete,
 * actionable diagnosis instead of just "saved".
 */
suspend fun testSupabaseConnection(url: String, key: String): Pair<Boolean, String> =
    withContext(Dispatchers.IO) {
        val cleanUrl = url.trim().trimEnd('/')
        if (!cleanUrl.startsWith("https://")) {
            return@withContext false to "La URL debe empezar con https://"
        }
        try {
            val conn = URL("$cleanUrl/rest/v1/trips?limit=1")
                .openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("apikey", key.trim())
                conn.setRequestProperty("Authorization", "Bearer ${key.trim()}")
                when (val code = conn.responseCode) {
                    in 200..299 -> true to "Conexión válida: proyecto y tablas listos"
                    401, 403 -> false to "El proyecto responde pero la clave es inválida — revisa la publishable key"
                    404 -> false to "URL incorrecta — verifica la Project URL"
                    else -> {
                        val body = conn.errorStream?.bufferedReader()?.readText().orEmpty()
                        if (body.contains("PGRST205"))
                            false to "Conecta, pero faltan las tablas — ejecuta supabase_schema.sql en el SQL Editor"
                        else
                            false to "Error HTTP $code del servidor"
                    }
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            false to "No se pudo conectar: ${e.message ?: "sin internet o URL inalcanzable"}"
        }
    }
