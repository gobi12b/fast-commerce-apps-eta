package com.example.etawidget.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.etawidget.data.model.EtaResult
import com.example.etawidget.data.model.ServiceId
import com.example.etawidget.data.model.ageLabel
import com.example.etawidget.data.model.fastest
import com.example.etawidget.deeplink.AppLaunchResolver
import com.example.etawidget.screenread.CaptureSweep
import com.example.etawidget.screenread.EtaAccessibilityService
import com.example.etawidget.ui.accent
import com.example.etawidget.ui.shortName

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val cached by viewModel.cached.collectAsState()
    val sweep by viewModel.sweepState.collectAsState()
    val context = LocalContext.current
    var serviceEnabled by remember { mutableStateOf(EtaAccessibilityService.isEnabled(context)) }
    // Re-check when returning from system Settings.
    LifecycleResumeEffect(Unit) {
        serviceEnabled = EtaAccessibilityService.isEnabled(context)
        onPauseOrDispose { }
    }
    val fastest = cached.results.fastest()

    Scaffold(
        topBar = { LargeTopAppBar(title = { Text("Delivery ETAs") }) },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!serviceEnabled) {
                fullWidth { EnableServiceCard() }
            }

            fullWidth { HeroCard(fastest, sweep) }

            items(ServiceId.entries) { id ->
                EtaTile(
                    serviceId = id,
                    result = cached.results.find { it.serviceId == id },
                    sweep = sweep,
                    isFastest = id == fastest?.serviceId,
                    onClick = {
                        context.startActivity(
                            AppLaunchResolver(context).resolveLaunchIntent(id)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                )
            }

            if (serviceEnabled) {
                fullWidth {
                    Column {
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = viewModel::checkAllApps,
                            enabled = sweep !is CaptureSweep.State.Running,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Text("Check all apps")
                        }
                        TextButton(
                            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Accessibility settings")
                        }
                    }
                }
            }
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun HeroCard(fastest: EtaResult.Success?, sweep: CaptureSweep.State) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            if (sweep is CaptureSweep.State.Running) {
                Text("Checking ${sweep.current.shortName}…", style = MaterialTheme.typography.titleMedium)
                Text(
                    "App ${sweep.index} of ${sweep.total}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { (sweep.index - 1f) / sweep.total },
                    modifier = Modifier.fillMaxWidth(),
                )
                return@Column
            }

            Text(
                "Fastest right now",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            if (fastest == null) {
                Text("No readings yet", style = MaterialTheme.typography.headlineMedium)
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${fastest.estimate.etaMinutes}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        " min",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AccentDot(fastest.serviceId)
                    Spacer(Modifier.width(8.dp))
                    Text(fastest.serviceId.displayName, style = MaterialTheme.typography.titleMedium)
                }
            }
            if (sweep is CaptureSweep.State.Finished) {
                val note = when {
                    sweep.stoppedEarly -> "Check stopped — you left the app."
                    sweep.missed.isNotEmpty() -> "Couldn't read ${sweep.missed.joinToString { it.shortName }}."
                    else -> null
                }
                if (note != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun EtaTile(
    serviceId: ServiceId,
    result: EtaResult?,
    sweep: CaptureSweep.State,
    isFastest: Boolean,
    onClick: () -> Unit,
) {
    val checkingNow = sweep is CaptureSweep.State.Running && sweep.current == serviceId
    val missed = sweep is CaptureSweep.State.Finished && serviceId in sweep.missed
    val estimate = (result as? EtaResult.Success)?.estimate
    val unavailable = result as? EtaResult.Unavailable

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (isFastest) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth().height(152.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccentDot(serviceId)
                Spacer(Modifier.width(8.dp))
                Text(
                    serviceId.shortName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                if (checkingNow) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
            Spacer(Modifier.weight(1f))
            if (estimate != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${estimate.etaMinutes}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        " min",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            } else if (unavailable != null) {
                Text(
                    "Busy",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text(
                    "—",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                when {
                    checkingNow -> "Checking…"
                    isFastest -> "Fastest · ${ageLabel(estimate!!.fetchedAtEpochMillis)}"
                    estimate != null -> ageLabel(estimate.fetchedAtEpochMillis)
                    unavailable != null -> "High demand · ${ageLabel(unavailable.seenAtEpochMillis)}"
                    missed -> "Couldn't read"
                    else -> "Tap to open"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isFastest && !checkingNow) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun AccentDot(serviceId: ServiceId) {
    Box(
        Modifier
            .size(10.dp)
            .background(serviceId.accent, CircleShape),
    )
}

@Composable
private fun EnableServiceCard() {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Turn on screen reading", style = MaterialTheme.typography.titleMedium)
            Text(
                "Each time you open this app, it opens Instamart, Zepto, Blinkit and BBNow " +
                    "in turn, reads the delivery time each one shows, and comes back here. " +
                    "Only the minutes are saved. Android will warn that this service can see " +
                    "your screen; outside a check it only receives events from those apps.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "In the next screen, open \"ETA Widget screen reader\" and turn it on. " +
                    "If it's greyed out, open this app's App info → ⋮ → \"Allow restricted settings\" first.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                Text("Turn on")
            }
        }
    }
}
