package com.example.etawidget.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val PERIODIC_WORK_NAME = "eta_periodic_refresh"
    private const val ONE_OFF_WORK_NAME = "eta_manual_refresh"

    /**
     * Re-renders the widget periodically so "Xm ago" labels and stale-reading
     * expiry stay current. 15 minutes is WorkManager's minimum period.
     */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<EtaRefreshWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** Triggered whenever a new ETA is read off a delivery app's screen. */
    fun runOnce(context: Context) {
        val request = OneTimeWorkRequestBuilder<EtaRefreshWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_OFF_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
