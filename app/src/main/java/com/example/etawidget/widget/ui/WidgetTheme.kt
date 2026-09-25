package com.example.etawidget.widget.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle

object WidgetTheme {
    val background = ColorProvider(Color(0xFFF4F4F6), Color(0xFF121214))
    val tile = ColorProvider(Color(0xFFFFFFFF), Color(0xFF232327))
    val fastestTile = ColorProvider(Color(0xFFDDF4E4), Color(0xFF173A24))
    val onSurface = ColorProvider(Color(0xFF16161A), Color(0xFFF2F2F4))
    val muted = ColorProvider(Color(0xFF6B6B73), Color(0xFFA0A0A8))
    val busyColor = ColorProvider(Color(0xFFB3261E), Color(0xFFF2B8B5))
    val fastestLabel = ColorProvider(Color(0xFF1B7A3D), Color(0xFF7FD69B))

    val header = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = onSurface)
    val caption = TextStyle(fontSize = 11.sp, color = muted)
    val tileName = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = onSurface)
    val minutes = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = onSurface)
    val minutesUnit = TextStyle(fontSize = 12.sp, color = muted)
    val busy = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = busyColor)
    val placeholder = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = muted)
    val fastestCaption = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = fastestLabel)
}
