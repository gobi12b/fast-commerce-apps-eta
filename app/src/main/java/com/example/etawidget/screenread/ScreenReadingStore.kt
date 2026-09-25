package com.example.etawidget.screenread

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.etawidget.data.model.ServiceId
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.screenReadingDataStore by preferencesDataStore(name = "screen_readings")

/** An ETA read off a delivery app's screen by [EtaAccessibilityService]. Only the number is kept — no screen text. */
@Serializable
data class ScreenReading(
    val serviceId: ServiceId,
    /** Null when the app said it's unavailable (e.g. high demand) instead of showing an ETA. */
    val etaMinutes: Int?,
    val seenAtEpochMillis: Long,
)

/** Narrow read interface so ScreenReadProvider is testable without a real DataStore/Context. */
interface ScreenReadingSource {
    suspend fun latest(serviceId: ServiceId): ScreenReading?
}

class ScreenReadingStore(private val context: Context) : ScreenReadingSource {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(reading: ScreenReading) {
        context.screenReadingDataStore.edit { prefs ->
            prefs[keyFor(reading.serviceId)] = json.encodeToString(ScreenReading.serializer(), reading)
        }
    }

    override suspend fun latest(serviceId: ServiceId): ScreenReading? {
        val payload = context.screenReadingDataStore.data.first()[keyFor(serviceId)] ?: return null
        return try {
            json.decodeFromString(ScreenReading.serializer(), payload)
        } catch (e: Exception) {
            null
        }
    }

    private fun keyFor(serviceId: ServiceId) = stringPreferencesKey("reading_${serviceId.name}")
}
