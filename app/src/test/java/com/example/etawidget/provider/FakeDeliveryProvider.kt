package com.example.etawidget.provider

import com.example.etawidget.data.model.DeliveryEstimate
import com.example.etawidget.data.model.EtaResult
import com.example.etawidget.data.model.ServiceId
import com.example.etawidget.data.provider.DeliveryProvider

/** A test double used across repository tests. Null [etaMinutes] means no recent reading. */
class FakeDeliveryProvider(
    override val serviceId: ServiceId,
    private val etaMinutes: Int?,
) : DeliveryProvider {
    override suspend fun latest(): EtaResult =
        etaMinutes?.let { EtaResult.Success(DeliveryEstimate(serviceId, it, fetchedAtEpochMillis = 0L)) }
            ?: EtaResult.NoRecentReading(serviceId)
}
