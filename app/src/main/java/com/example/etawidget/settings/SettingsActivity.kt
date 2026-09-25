package com.example.etawidget.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.etawidget.screenread.CaptureSweep
import com.example.etawidget.screenread.EtaAccessibilityService
import com.example.etawidget.ui.EtaTheme

/** The app's only screen. Opening it runs a capture sweep across the installed delivery apps. */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            EtaTheme {
                SettingsScreen()
            }
        }

        // Fresh launch only — not on rotation/restore, and not when the sweep itself brings us back.
        val fromSweep = intent.getBooleanExtra(EXTRA_FROM_SWEEP, false)
        if (savedInstanceState == null && !fromSweep && EtaAccessibilityService.isEnabled(this)) {
            CaptureSweep.request()
        }
    }

    companion object {
        const val EXTRA_FROM_SWEEP = "from_sweep"
    }
}
