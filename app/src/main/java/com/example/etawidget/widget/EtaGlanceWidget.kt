package com.example.etawidget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import com.example.etawidget.R
import com.example.etawidget.cache.CachedEta
import com.example.etawidget.cache.EtaCache
import com.example.etawidget.data.model.ServiceId
import com.example.etawidget.data.model.ageLabel
import com.example.etawidget.data.model.fastest
import com.example.etawidget.settings.SettingsActivity
import com.example.etawidget.widget.ui.EtaTile
import com.example.etawidget.widget.ui.WidgetTheme

class EtaGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cache = EtaCache(context)
        provideContent {
            val cached by cache.observe().collectAsState(initial = CachedEta.empty())
            EtaWidgetContent(cached)
        }
    }
}

@Composable
private fun EtaWidgetContent(cached: CachedEta) {
    val fastestServiceId = cached.results.fastest()?.serviceId
    val tiles = ServiceId.entries.chunked(2)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetTheme.background)
            .cornerRadius(24.dp)
            .padding(10.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(start = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Delivery ETAs", style = WidgetTheme.header)
            Spacer(GlanceModifier.width(6.dp))
            Text(updatedLabel(cached.updatedAtEpochMillis), style = WidgetTheme.caption)
            Spacer(GlanceModifier.defaultWeight())
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = "Check all apps",
                // Opening the app runs a capture sweep across all delivery apps.
                modifier = GlanceModifier.size(20.dp).clickable(actionStartActivity<SettingsActivity>()),
            )
        }
        tiles.forEachIndexed { rowIndex, rowServices ->
            if (rowIndex > 0) Spacer(GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                rowServices.forEachIndexed { i, id ->
                    if (i > 0) Spacer(GlanceModifier.width(8.dp))
                    EtaTile(
                        serviceId = id,
                        result = cached.results.find { it.serviceId == id },
                        isFastest = id == fastestServiceId,
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                    )
                }
            }
        }
    }
}

private fun updatedLabel(updatedAtEpochMillis: Long): String =
    if (updatedAtEpochMillis == 0L) "" else "· ${ageLabel(updatedAtEpochMillis)}"
