package com.example.etawidget.data.repository

import com.example.etawidget.cache.EtaResultsWriter
import com.example.etawidget.data.model.EtaResult
import com.example.etawidget.data.provider.DeliveryProvider

class EtaRepository(
    private val providers: List<DeliveryProvider>,
    private val cache: EtaResultsWriter,
) {
    /** Collects each service's latest ETA and writes them to the widget's cache. */
    suspend fun refreshAll(): List<EtaResult> {
        val results = providers.map { it.latest() }
        cache.save(results)
        return results
    }
}
