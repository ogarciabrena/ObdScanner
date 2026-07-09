package com.obd.scanner.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.obd.scanner.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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
