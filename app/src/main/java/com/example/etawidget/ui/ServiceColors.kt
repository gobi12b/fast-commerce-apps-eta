package com.example.etawidget.ui

import androidx.compose.ui.graphics.Color
import com.example.etawidget.data.model.ServiceId

/** Accent dot per service, shared by the app and the widget. */
val ServiceId.accent: Color
    get() = when (this) {
        ServiceId.SWIGGY_INSTAMART -> Color(0xFFFC8019)
        ServiceId.ZEPTO -> Color(0xFF7B2FF7)
        ServiceId.BLINKIT -> Color(0xFFF8CB46)
        ServiceId.BBNOW -> Color(0xFF84C225)
    }

/** Short label that fits a small tile. */
val ServiceId.shortName: String
    get() = when (this) {
        ServiceId.SWIGGY_INSTAMART -> "Instamart"
        else -> displayName
    }
