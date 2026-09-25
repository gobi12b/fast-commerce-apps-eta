package com.example.etawidget.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.etawidget.EtaWidgetApplication
import com.example.etawidget.cache.CachedEta
import com.example.etawidget.screenread.CaptureSweep
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val cached: StateFlow<CachedEta> = getApplication<EtaWidgetApplication>().etaCache.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CachedEta.empty())

    val sweepState: StateFlow<CaptureSweep.State> = CaptureSweep.state

    fun checkAllApps() = CaptureSweep.request()
}
