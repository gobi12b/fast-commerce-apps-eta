package com.example.etawidget.screenread

import com.example.etawidget.data.model.ServiceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenEtaParserTest {

    private val blinkit = ScreenEtaRule(ServiceId.BLINKIT, "blinkit.pkg", requireDeliveryKeyword = false)
    private val strict = ScreenEtaRule(ServiceId.ZEPTO, "strict.pkg")
    private val instamart = ScreenEtaRule(ServiceId.SWIGGY_INSTAMART, "swiggy.pkg", requiredMarker = "instamart")

    @Test
    fun `reads single-line delivery text`() {
        assertEquals(8, ScreenEtaParser.parse(listOf("Home", "Delivery in 8 minutes", "Search"), blinkit))
    }

    @Test
    fun `accepts keyword on the line above the minutes`() {
        assertEquals(11, ScreenEtaParser.parse(listOf("Delivery in", "11 mins", "Koramangala"), strict))
    }

    @Test
    fun `header split as brand-in then minutes counts as keyword-backed`() {
        val bbnow = ScreenEtaRule(ServiceId.BBNOW, "bb.pkg")
        val texts = listOf("bigbasket in", "33 mins", "Selected Location - Chennai", "Search 20000+ products")
        assertEquals(33, ScreenEtaParser.parse(texts, bbnow))
    }

    @Test
    fun `uppercase MINS is recognised`() {
        assertEquals(17, ScreenEtaParser.parse(listOf("₹60", "17% OFF", "Hatsun Curd", "17 MINS"), blinkit))
    }

    @Test
    fun `takes lower bound of a range`() {
        assertEquals(10, ScreenEtaParser.parse(listOf("Arriving in 10-15 mins"), strict))
        assertEquals(20, ScreenEtaParser.parse(listOf("Delivery in 20 – 25 min"), strict))
    }

    @Test
    fun `keyword-backed match wins over an earlier bare time`() {
        val texts = listOf("Pizza Place", "25-30 mins", "Your order", "Delivery in 35 mins")
        assertEquals(35, ScreenEtaParser.parse(texts, strict))
    }

    @Test
    fun `bare restaurant times are ignored when a keyword is required`() {
        assertNull(ScreenEtaParser.parse(listOf("Pizza Place", "25-30 mins", "Burger Barn", "30-35 mins"), strict))
    }

    @Test
    fun `bare time is accepted when keyword not required`() {
        assertEquals(9, ScreenEtaParser.parse(listOf("Blinkit", "9 minutes", "HOME - Indiranagar"), blinkit))
    }

    @Test
    fun `required marker gates the whole screen`() {
        assertNull(ScreenEtaParser.parse(listOf("Food delivery", "Delivery in 30 mins"), instamart))
        assertEquals(12, ScreenEtaParser.parse(listOf("Instamart", "Delivery in 12 mins"), instamart))
    }

    @Test
    fun `ignores implausible numbers and non-minute text`() {
        assertNull(ScreenEtaParser.parse(listOf("₹50 off above ₹199", "Delivery in 0 mins"), blinkit))
        assertNull(ScreenEtaParser.parse(listOf("Delivery in 500 minutes"), blinkit))
        assertNull(ScreenEtaParser.parse(listOf("Delivery charges ₹25"), blinkit))
    }

    @Test
    fun `detects high-demand unavailability`() {
        assertTrue(ScreenEtaParser.isUnavailable(listOf("High Demand", "Schedule Order", "₹0")))
        assertTrue(ScreenEtaParser.isUnavailable(listOf("Currently unavailable due to high demand", "Notify Me")))
        assertFalse(ScreenEtaParser.isUnavailable(listOf("Delivery in 8 minutes", "Search")))
    }
}
