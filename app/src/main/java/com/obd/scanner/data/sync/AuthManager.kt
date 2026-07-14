package com.obd.scanner.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Context.authDataStore by preferencesDataStore(name = "auth_session")

data class AuthResult(val ok: Boolean, val message: String)

/**
 * Supabase Auth (email/contraseña). Guarda la sesión (access + refresh token)
 * y la renueva sola. La subida de viajes usa este access token para que las
 * políticas RLS aten cada viaje al usuario dueño.
 */
class AuthManager(
    private val context: Context,
    private val sync: SyncSettings
) {
    private val accessKey = stringPreferencesKey("auth_access")
    private val refreshKey = stringPreferencesKey("auth_refresh")
    private val emailKey = stringPreferencesKey("auth_email")
    private val expiresKey = longPreferencesKey("auth_expires_at")

    val emailFlow: Flow<String?> = context.authDataStore.data.map { it[emailKey] }
    val isLoggedInFlow: Flow<Boolean> = context.authDataStore.data.map { !it[refreshKey].isNullOrBlank() }

    suspend fun signUp(email: String, password: String): AuthResult =
        authCall("/auth/v1/signup", email, password)

    suspend fun signIn(email: String, password: String): AuthResult =
        authCall("/auth/v1/token?grant_type=password", email, password)

    suspend fun signOut() {
        context.authDataStore.edit { it.clear() }
    }

    /** Devuelve un access token válido, renovándolo si expiró. Null si no hay sesión. */
    suspend fun validAccessToken(): String? = withContext(Dispatchers.IO) {
        val prefs = context.authDataStore.data.first()
        val refresh = prefs[refreshKey] ?: return@withContext null
        val access = prefs[accessKey]
        val expires = prefs[expiresKey] ?: 0L
        if (access != null && System.currentTimeMillis() < expires - 60_000) {
            return@withContext access
        }
        // renovar
        val creds = sync.current()
        val body = JSONObject().put("refresh_token", refresh)
        val (code, resp) = post("${creds.url}/auth/v1/token?grant_type=refresh_token", creds.key, body.toString())
        if (code in 200..299) {
            saveSession(resp)
            resp.optString("access_token").ifBlank { null }
        } else {
            null
        }
    }

    private suspend fun authCall(path: String, email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val creds = sync.current()
            if (!creds.isConfigured) {
                return@withContext AuthResult(false, "Configura la URL y la key de Supabase primero")
            }
            val body = JSONObject().put("email", email.trim()).put("password", password)
            val (code, resp) = post("${creds.url}$path", creds.key, body.toString())
            when {
                resp.has("access_token") -> {
                    saveSession(resp)
                    AuthResult(true, "Sesión iniciada")
                }
                // signup con confirmación de email activada
                resp.has("confirmation_sent_at") || resp.optJSONObject("user") != null ->
                    AuthResult(false, "Cuenta creada. Revisa tu correo para confirmarla y luego inicia sesión.")
                code in 200..299 -> AuthResult(true, "Listo")
                else -> AuthResult(false, resp.optString("msg").ifBlank {
                    resp.optString("error_description").ifBlank { "Error de autenticación ($code)" }
                })
            }
        }

    private suspend fun saveSession(resp: JSONObject) {
        val access = resp.optString("access_token")
        val refresh = resp.optString("refresh_token")
        val expiresIn = resp.optLong("expires_in", 3600)
        val email = resp.optJSONObject("user")?.optString("email") ?: resp.optString("email")
        if (access.isBlank() || refresh.isBlank()) return
        context.authDataStore.edit {
            it[accessKey] = access
            it[refreshKey] = refresh
            it[expiresKey] = System.currentTimeMillis() + expiresIn * 1000
            if (!email.isNullOrBlank()) it[emailKey] = email
        }
    }

    private fun post(urlStr: String, apiKey: String, json: String): Pair<Int, JSONObject> {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true
            conn.setRequestProperty("apikey", apiKey)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.readText().orEmpty()
            code to (runCatching { JSONObject(text) }.getOrDefault(JSONObject()))
        } catch (e: Exception) {
            0 to JSONObject().put("msg", e.message ?: "Sin conexión")
        } finally {
            conn.disconnect()
        }
    }
}
