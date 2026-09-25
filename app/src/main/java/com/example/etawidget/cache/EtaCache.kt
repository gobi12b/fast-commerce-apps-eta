package com.example.etawidget.cache

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.etawidget.data.model.EtaResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "eta_cache")

data class CachedEta(val results: List<EtaResult>, val updatedAtEpochMillis: Long) {
    companion object {
        fun empty() = CachedEta(emptyList(), 0L)
    }
}

/** Narrow write interface so EtaRepository is testable without a real DataStore/Context. */
interface EtaResultsWriter {
    suspend fun save(results: List<EtaResult>)
}

/** Last-known ETA results + timestamp, read by the widget. */
class EtaCache(private val context: Context) : EtaResultsWriter {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun save(results: List<EtaResult>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PAYLOAD] = json.encodeToString(results)
            prefs[KEY_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    fun observe(): Flow<CachedEta> = context.dataStore.data.map { prefs ->
        val payload = prefs[KEY_PAYLOAD]
        val updatedAt = prefs[KEY_UPDATED_AT] ?: 0L
        val results = if (payload != null) {
            try {
                json.decodeFromString<List<EtaResult>>(payload)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        CachedEta(results, updatedAt)
    }

    private companion object {
        val KEY_PAYLOAD = stringPreferencesKey("payload_json")
        val KEY_UPDATED_AT = longPreferencesKey("updated_at")
    }
}
