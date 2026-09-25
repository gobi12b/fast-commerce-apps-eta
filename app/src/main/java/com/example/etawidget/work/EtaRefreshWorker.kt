package com.example.etawidget.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.etawidget.EtaWidgetApplication
import com.example.etawidget.widget.EtaGlanceWidget

class EtaRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as EtaWidgetApplication
        return try {
            app.etaRepository.refreshAll()
            EtaGlanceWidget().updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
