package com.example.etawidget.deeplink

import com.example.etawidget.data.model.ServiceId

data class ServiceAppInfo(
    /** Candidate apps in order of preference; the first installed one is launched. */
    val packageNames: List<String>,
    val playStoreUrl: String,
)

object ServiceAppInfoRegistry {
    // Keep in sync with <queries> in AndroidManifest.xml and ScreenEtaRules.
    private val infoByService = mapOf(
        ServiceId.SWIGGY_INSTAMART to ServiceAppInfo(
            // Standalone Instamart app opens straight to Instamart; the main Swiggy app opens on food.
            packageNames = listOf("in.swiggy.android.instamart", "in.swiggy.android"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=in.swiggy.android.instamart",
        ),
        ServiceId.ZEPTO to ServiceAppInfo(
            packageNames = listOf("com.zeptoconsumerapp"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.zeptoconsumerapp",
        ),
        ServiceId.BLINKIT to ServiceAppInfo(
            packageNames = listOf("com.grofers.customerapp"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.grofers.customerapp",
        ),
        ServiceId.BBNOW to ServiceAppInfo(
            packageNames = listOf("com.bigbasket.mobileapp"),
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.bigbasket.mobileapp",
        ),
    )

    fun forService(serviceId: ServiceId): ServiceAppInfo =
        infoByService.getValue(serviceId)
}
