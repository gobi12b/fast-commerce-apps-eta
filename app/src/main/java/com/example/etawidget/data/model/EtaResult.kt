package com.example.etawidget.data.model

import kotlinx.serialization.Serializable

/** Latest known ETA for one service. Serializable since it's written to the on-disk cache and read by the widget. */
@Serializable
sealed class EtaResult {
    abstract val serviceId: ServiceId

    @Serializable
    data class Success(val estimate: DeliveryEstimate) : EtaResult() {
        override val serviceId: ServiceId get() = estimate.serviceId
    }

    /** The app said it isn't taking instant orders right now (e.g. "Currently unavailable due to high demand"). */
    @Serializable
    data class Unavailable(override val serviceId: ServiceId, val seenAtEpochMillis: Long) : EtaResult()

    /** No ETA captured from this service's app recently enough to show. */
    @Serializable
    data class NoRecentReading(override val serviceId: ServiceId) : EtaResult()
}
