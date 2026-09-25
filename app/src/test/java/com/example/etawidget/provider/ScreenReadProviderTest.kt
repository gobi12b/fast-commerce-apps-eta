package com.example.etawidget.provider

import com.example.etawidget.data.model.EtaResult
import com.example.etawidget.data.model.ServiceId
import com.example.etawidget.data.provider.ScreenReadProvider
import com.example.etawidget.screenread.ScreenReading
import com.example.etawidget.screenread.ScreenReadingSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeReadings(private val reading: ScreenReading?) : ScreenReadingSource {
    override suspend fun latest(serviceId: ServiceId) = reading?.takeIf { it.serviceId == serviceId }
}

class ScreenReadProviderTest {

    private val now = 10_000_000L
    private val id = ServiceId.BLINKIT

    private fun provider(reading: ScreenReading?) =
        ScreenReadProvider(id, FakeReadings(reading), now = { now })

    @Test
    fun `reports reading time as fetch time`() = runTest {
        val estimate = (provider(ScreenReading(id, 9, now - 60_000)).latest() as EtaResult.Success).estimate
        assertEquals(9, estimate.etaMinutes)
        assertEquals(now - 60_000, estimate.fetchedAtEpochMillis)
    }

    @Test
    fun `ignores readings older than max age`() = runTest {
        assertTrue(provider(ScreenReading(id, 9, now - ScreenReadProvider.MAX_AGE_MS - 1)).latest() is EtaResult.NoRecentReading)
    }

    @Test
    fun `no reading yields NoRecentReading`() = runTest {
        assertTrue(provider(null).latest() is EtaResult.NoRecentReading)
    }

    @Test
    fun `reading without minutes is Unavailable`() = runTest {
        assertEquals(EtaResult.Unavailable(id, now - 1_000), provider(ScreenReading(id, null, now - 1_000)).latest())
    }
}
