package com.example.etawidget.data.provider

import com.example.etawidget.data.model.DeliveryEstimate
import com.example.etawidget.data.model.EtaResult
import com.example.etawidget.data.model.ServiceId
import com.example.etawidget.screenread.ScreenReadingSource
import java.util.concurrent.TimeUnit

/**
 * Serves the last ETA [com.example.etawidget.screenread.EtaAccessibilityService]
 * read off this service's app. [DeliveryEstimate.fetchedAtEpochMillis] is when
 * it was seen, so the widget can show how old it is.
 */
class ScreenReadProvider(
    override val serviceId: ServiceId,
    private val source: ScreenReadingSource,
    private val now: () -> Long = System::currentTimeMillis,
) : DeliveryProvider {

    override suspend fun latest(): EtaResult {
        val reading = source.latest(serviceId)
        if (reading == null || now() - reading.seenAtEpochMillis > MAX_AGE_MS) {
            return EtaResult.NoRecentReading(serviceId)
        }
        val minutes = reading.etaMinutes
            ?: return EtaResult.Unavailable(serviceId, reading.seenAtEpochMillis)
        return EtaResult.Success(DeliveryEstimate(serviceId, minutes, reading.seenAtEpochMillis))
    }

    companion object {
        val MAX_AGE_MS = TimeUnit.HOURS.toMillis(6)
    }
}
