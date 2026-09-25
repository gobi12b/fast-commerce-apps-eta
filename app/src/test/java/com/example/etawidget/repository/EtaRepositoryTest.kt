package com.example.etawidget.repository

import com.example.etawidget.cache.EtaResultsWriter
import com.example.etawidget.data.model.EtaResult
import com.example.etawidget.data.model.ServiceId
import com.example.etawidget.data.repository.EtaRepository
import com.example.etawidget.provider.FakeDeliveryProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeWriter : EtaResultsWriter {
    var lastSaved: List<EtaResult>? = null
    override suspend fun save(results: List<EtaResult>) {
        lastSaved = results
    }
}

class EtaRepositoryTest {

    @Test
    fun `services without a reading are reported without blocking the others`() = runTest {
        val providers = listOf(
            FakeDeliveryProvider(ServiceId.SWIGGY_INSTAMART, 5),
            FakeDeliveryProvider(ServiceId.ZEPTO, null),
            FakeDeliveryProvider(ServiceId.BLINKIT, 12),
        )
        val writer = FakeWriter()

        val results = EtaRepository(providers, writer).refreshAll()

        assertEquals(3, results.size)
        assertTrue(results.any { it is EtaResult.Success && it.serviceId == ServiceId.SWIGGY_INSTAMART })
        assertTrue(results.any { it is EtaResult.NoRecentReading && it.serviceId == ServiceId.ZEPTO })
        assertTrue(results.any { it is EtaResult.Success && it.serviceId == ServiceId.BLINKIT })
        assertEquals(results, writer.lastSaved)
    }

    @Test
    fun `fastest among successes is the minimum eta`() = runTest {
        val providers = listOf(
            FakeDeliveryProvider(ServiceId.SWIGGY_INSTAMART, 20),
            FakeDeliveryProvider(ServiceId.ZEPTO, 8),
            FakeDeliveryProvider(ServiceId.BLINKIT, 15),
        )

        val results = EtaRepository(providers, FakeWriter()).refreshAll()
        val fastest = results.filterIsInstance<EtaResult.Success>().minByOrNull { it.estimate.etaMinutes }

        assertEquals(ServiceId.ZEPTO, fastest?.serviceId)
    }
}
