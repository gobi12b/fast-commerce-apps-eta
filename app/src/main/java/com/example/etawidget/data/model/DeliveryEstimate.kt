package com.example.etawidget.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DeliveryEstimate(
    val serviceId: ServiceId,
    val etaMinutes: Int,
    /** When the ETA was seen on screen — readings are only as fresh as the last capture. */
    val fetchedAtEpochMillis: Long,
)
