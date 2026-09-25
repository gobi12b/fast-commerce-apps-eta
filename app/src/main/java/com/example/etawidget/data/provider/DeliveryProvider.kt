package com.example.etawidget.data.provider

import com.example.etawidget.data.model.EtaResult
import com.example.etawidget.data.model.ServiceId

interface DeliveryProvider {
    val serviceId: ServiceId

    /** Latest known state for this service. */
    suspend fun latest(): EtaResult
}
