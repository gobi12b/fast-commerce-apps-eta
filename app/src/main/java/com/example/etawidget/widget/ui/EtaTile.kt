package com.example.etawidget.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import com.example.etawidget.data.model.EtaResult
import com.example.etawidget.data.model.ServiceId
import com.example.etawidget.data.model.ageLabel
import com.example.etawidget.ui.accent
import com.example.etawidget.ui.shortName
import com.example.etawidget.widget.OpenAppAction
import com.example.etawidget.widget.ServiceIdKey
import androidx.glance.color.ColorProvider as DayNightColor

@Composable
fun EtaTile(
    serviceId: ServiceId,
    result: EtaResult?,
    isFastest: Boolean,
    modifier: GlanceModifier,
) {
    val estimate = (result as? EtaResult.Success)?.estimate
    val unavailable = result as? EtaResult.Unavailable

    Column(
        modifier = modifier
            .background(if (isFastest) WidgetTheme.fastestTile else WidgetTheme.tile)
            .cornerRadius(16.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clickable(actionRunCallback<OpenAppAction>(actionParametersOf(ServiceIdKey to serviceId.name))),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                GlanceModifier
                    .size(8.dp)
                    .cornerRadius(4.dp)
                    .background(DayNightColor(serviceId.accent, serviceId.accent)),
            ) {}
            Spacer(GlanceModifier.width(6.dp))
            Text(serviceId.shortName, style = WidgetTheme.tileName, maxLines = 1)
        }
        Spacer(GlanceModifier.defaultWeight())
        if (estimate != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${estimate.etaMinutes}", style = WidgetTheme.minutes)
                Text(" min", style = WidgetTheme.minutesUnit, modifier = GlanceModifier.padding(bottom = 4.dp))
            }
        } else if (unavailable != null) {
            Text("Busy", style = WidgetTheme.busy)
        } else {
            Text("—", style = WidgetTheme.placeholder)
        }
        Text(
            when {
                unavailable != null -> "High demand · ${ageLabel(unavailable.seenAtEpochMillis)}"
                estimate == null && result == null -> "Loading…"
                estimate == null -> "Tap to check"
                isFastest -> "Fastest · ${ageLabel(estimate.fetchedAtEpochMillis)}"
                else -> ageLabel(estimate.fetchedAtEpochMillis)
            },
            style = if (isFastest) WidgetTheme.fastestCaption else WidgetTheme.caption,
            maxLines = 1,
        )
    }
}
