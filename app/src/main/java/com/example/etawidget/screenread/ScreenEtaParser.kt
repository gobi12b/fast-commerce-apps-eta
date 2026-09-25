package com.example.etawidget.screenread

import com.example.etawidget.data.model.ServiceId

/**
 * How to recognise one service's ETA among the text visible in its app.
 *
 * These are best guesses at each app's current wording — tune them against
 * what your device actually shows (debug builds log the visible text to
 * Logcat under the "EtaScreenRead" tag).
 */
data class ScreenEtaRule(
    val serviceId: ServiceId,
    val packageName: String,
    /** If set, the screen must contain this text somewhere (case-insensitive) before any ETA is accepted. */
    val requiredMarker: String? = null,
    /**
     * If true, a minutes value only counts when it, or the text just above it,
     * mentions delivery/arrival. Needed for apps whose home screens list many
     * per-restaurant times (e.g. "25-30 mins") that aren't "the" ETA.
     */
    val requireDeliveryKeyword: Boolean = true,
)

object ScreenEtaRules {
    // Keep packages in sync with ServiceAppInfoRegistry.
    val all: List<ScreenEtaRule> = listOf(
        // Swiggy food and Instamart share one app; only trust screens that say "Instamart".
        ScreenEtaRule(
            ServiceId.SWIGGY_INSTAMART,
            "in.swiggy.android",
            requiredMarker = "instamart",
        ),
        // Standalone Instamart app — everything in it is Instamart; its ETA shows as a bare "17 MINS".
        ScreenEtaRule(ServiceId.SWIGGY_INSTAMART, "in.swiggy.android.instamart", requireDeliveryKeyword = false),
        ScreenEtaRule(ServiceId.ZEPTO, "com.zeptoconsumerapp"),
        // Blinkit's header shows a single store-wide ETA near the top of the screen.
        ScreenEtaRule(
            ServiceId.BLINKIT,
            "com.grofers.customerapp",
            requireDeliveryKeyword = false,
        ),
        ScreenEtaRule(ServiceId.BBNOW, "com.bigbasket.mobileapp"),
    )

    val packageNames: Set<String> = all.map { it.packageName }.toSet()

    fun forPackage(packageName: String): ScreenEtaRule? = all.find { it.packageName == packageName }
}

object ScreenEtaParser {

    // "8 minutes", "11 mins", "10-15 min", "10 – 15 mins", "10 to 15 minutes"
    private val MINUTES = Regex(
        """(\d{1,3})\s*(?:(?:-|–|to)\s*(\d{1,3})\s*)?(?:minutes?|mins?)\b""",
        RegexOption.IGNORE_CASE,
    )
    // "\bin\s*$" covers a header split across nodes, e.g. "bigbasket in" / "33 mins".
    private val DELIVERY_KEYWORD = Regex("""deliver|arriv|reach|\bin\s+\d|\bin\s*$""", RegexOption.IGNORE_CASE)
    private val PLAUSIBLE_MINUTES = 1..180

    // Zepto: "High Demand" + "Schedule Order"; Blinkit: "Currently unavailable due to high demand".
    private val UNAVAILABLE = Regex(
        """currently unavailable|high demand|store is closed|closed for now|unserviceable|not serviceable""",
        RegexOption.IGNORE_CASE,
    )

    /** True if the screen says the service isn't taking instant orders. Only meaningful when [parse] found no ETA. */
    fun isUnavailable(texts: List<String>): Boolean = texts.any { UNAVAILABLE.containsMatchIn(it) }

    /**
     * @param texts visible text on screen, in reading order (top-to-bottom, left-to-right).
     * @return ETA in minutes (the lower bound for a range), or null if none recognised.
     */
    fun parse(texts: List<String>, rule: ScreenEtaRule): Int? {
        if (rule.requiredMarker != null &&
            texts.none { it.contains(rule.requiredMarker, ignoreCase = true) }
        ) {
            return null
        }

        var firstAny: Int? = null
        texts.forEachIndexed { i, text ->
            val minutes = minutesIn(text) ?: return@forEachIndexed
            val keywordNearby = DELIVERY_KEYWORD.containsMatchIn(text) ||
                (i > 0 && DELIVERY_KEYWORD.containsMatchIn(texts[i - 1]))
            if (keywordNearby) return minutes
            if (firstAny == null) firstAny = minutes
        }
        return if (rule.requireDeliveryKeyword) null else firstAny
    }

    private fun minutesIn(text: String): Int? =
        MINUTES.findAll(text)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .firstOrNull { it in PLAUSIBLE_MINUTES }
}
