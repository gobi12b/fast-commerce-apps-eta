package com.example.etawidget.data.model

import java.util.concurrent.TimeUnit

/** An hours-old reading shouldn't win "fastest" over a fresh one. */
private val FASTEST_MAX_AGE_MS = TimeUnit.HOURS.toMillis(1)

fun List<EtaResult>.fastest(now: Long = System.currentTimeMillis()): EtaResult.Success? =
    filterIsInstance<EtaResult.Success>()
        .filter { now - it.estimate.fetchedAtEpochMillis <= FASTEST_MAX_AGE_MS }
        .minByOrNull { it.estimate.etaMinutes }

/** "just now", "12m ago", "3h ago". */
fun ageLabel(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(now - epochMillis)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        else -> "${minutes / 60}h ago"
    }
}
