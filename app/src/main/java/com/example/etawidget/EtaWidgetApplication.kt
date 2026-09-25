package com.example.etawidget

import android.app.Application
import com.example.etawidget.cache.EtaCache
import com.example.etawidget.data.model.ServiceId
import com.example.etawidget.data.provider.ScreenReadProvider
import com.example.etawidget.data.repository.EtaRepository
import com.example.etawidget.screenread.ScreenReadingStore
import com.example.etawidget.work.WorkScheduler

class EtaWidgetApplication : Application() {

    val etaCache: EtaCache by lazy { EtaCache(this) }
    val screenReadingStore: ScreenReadingStore by lazy { ScreenReadingStore(this) }
    val etaRepository: EtaRepository by lazy {
        EtaRepository(ServiceId.entries.map { ScreenReadProvider(it, screenReadingStore) }, etaCache)
    }

    override fun onCreate() {
        super.onCreate()
        WorkScheduler.schedulePeriodic(this)
    }
}
