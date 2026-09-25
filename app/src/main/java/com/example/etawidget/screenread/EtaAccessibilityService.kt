package com.example.etawidget.screenread

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.etawidget.BuildConfig
import com.example.etawidget.EtaWidgetApplication
import com.example.etawidget.data.model.ServiceId
import com.example.etawidget.deeplink.AppLaunchResolver
import com.example.etawidget.settings.SettingsActivity
import com.example.etawidget.work.WorkScheduler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Reads the delivery ETA off the screen while a delivery app is open, and
 * runs capture sweeps ([CaptureSweep]) that open each installed delivery app
 * in turn to read it. Only the parsed minutes value is stored.
 *
 * Outside a sweep, only events from the delivery apps are delivered (see
 * [watchAllPackages]). During a sweep it also checks whether the launcher is
 * in front, so it can stop if the user leaves.
 */
class EtaAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var scanPending = false
    private val scanRunnable = Runnable {
        scanPending = false
        scanActiveWindow()
    }
    private val lastSaved = mutableMapOf<ServiceId, ScreenReading>()

    private var sweepJob: Job? = null

    /** Set while a sweep waits on one app; its signal completes when that app yields a reading. */
    private var awaiting: SweepWait? = null

    private class SweepWait(
        val serviceId: ServiceId,
        val launchedAtUptime: Long,
        /** Scans of this app's loaded screen that found no ETA. */
        var unreadableScans: Int = 0,
        val signal: CompletableDeferred<Unit> = CompletableDeferred(),
    )

    private val store get() = (application as EtaWidgetApplication).screenReadingStore

    override fun onServiceConnected() {
        watchAllPackages(false)
        scope.launch {
            for (requestedAt in CaptureSweep.requests) {
                if (SystemClock.uptimeMillis() - requestedAt <= CaptureSweep.REQUEST_MAX_AGE_MS) startSweep()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName?.toString() !in ScreenEtaRules.packageNames) return
        // Throttle rather than debounce: animated banners fire content changes
        // continuously, which would keep postponing a debounced scan forever.
        if (!scanPending) {
            scanPending = true
            handler.postDelayed(scanRunnable, SCAN_INTERVAL_MS)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(scanRunnable)
        if (sweepJob?.isActive == true) CaptureSweep.update(CaptureSweep.State.Idle)
        scope.cancel()
        super.onDestroy()
    }

    private fun startSweep() {
        if (sweepJob?.isActive == true) return
        sweepJob = scope.launch { runSweep() }
    }

    private suspend fun runSweep() {
        val resolver = AppLaunchResolver(this)
        val targets = ServiceId.entries.mapNotNull { id ->
            resolver.installedLaunchIntent(id)?.let { id to it }
        }
        val captured = mutableSetOf<ServiceId>()
        var stoppedEarly = false

        homePackage = resolveHomePackage()
        debugLog("sweep start: targets=${targets.map { it.first }} home=$homePackage")
        watchAllPackages(true)
        try {
            for ((index, target) in targets.withIndex()) {
                val (serviceId, launchIntent) = target
                CaptureSweep.update(CaptureSweep.State.Running(serviceId, index + 1, targets.size))
                val wait = SweepWait(serviceId, SystemClock.uptimeMillis())
                awaiting = wait
                startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

                val outcome = awaitOutcome(wait)
                when (outcome) {
                    true -> captured += serviceId
                    false -> stoppedEarly = true
                    null -> Unit // gave up: no recognisable ETA on that app's screen
                }
                debugLog("sweep $serviceId: ${outcome ?: "gave up"}")
                awaiting = null
                if (stoppedEarly) break
                delay(BETWEEN_APPS_DELAY_MS)
            }
        } finally {
            awaiting = null
            watchAllPackages(false)
        }

        debugLog("sweep done: captured=$captured stoppedEarly=$stoppedEarly")
        if (!stoppedEarly) returnToApp()
        CaptureSweep.update(
            CaptureSweep.State.Finished(
                captured = captured,
                missed = targets.map { it.first }.toSet() - captured,
                stoppedEarly = stoppedEarly,
            ),
        )
    }

    /**
     * Waits for [wait]'s app to yield a reading. Returns true on a reading,
     * false if the user left (launcher in front for [LEFT_CONFIRM_MS] — a
     * single launcher event isn't enough, as app transitions can report one),
     * or null if it gave up: the app's screen loaded but showed no readable
     * ETA over [GIVE_UP_AFTER_UNREADABLE_SCANS] scans, or [PER_APP_TIMEOUT_MS] passed.
     */
    private suspend fun awaitOutcome(wait: SweepWait): Boolean? {
        val deadline = wait.launchedAtUptime + PER_APP_TIMEOUT_MS
        var homeSince: Long? = null
        while (SystemClock.uptimeMillis() < deadline) {
            if (wait.signal.isCompleted) return true
            if (wait.unreadableScans >= GIVE_UP_AFTER_UNREADABLE_SCANS) return null
            val now = SystemClock.uptimeMillis()
            val onHome = now - wait.launchedAtUptime > LEAVE_DETECTION_GRACE_MS &&
                rootInActiveWindow?.packageName?.toString() == homePackage
            homeSince = if (onHome) homeSince ?: now else null
            if (homeSince != null && now - homeSince >= LEFT_CONFIRM_MS) return false
            delay(POLL_INTERVAL_MS)
        }
        return if (wait.signal.isCompleted) true else null
    }

    private fun returnToApp() {
        startActivity(
            Intent(this, SettingsActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
                .putExtra(SettingsActivity.EXTRA_FROM_SWEEP, true),
        )
    }

    private var homePackage: String? = null

    private fun resolveHomePackage(): String? =
        packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
        )?.activityInfo?.packageName

    /** Outside a sweep, limit events to the delivery apps; during one, also see the launcher. */
    private fun watchAllPackages(all: Boolean) {
        serviceInfo = serviceInfo.apply {
            packageNames = if (all) null else ScreenEtaRules.packageNames.toTypedArray()
        }
    }

    private fun scanActiveWindow() {
        val root = rootInActiveWindow
        val rule = root?.packageName?.toString()?.let(ScreenEtaRules::forPackage)
        if (root == null || rule == null) {
            debugLog("scan skipped: active window=${root?.packageName}")
            return
        }
        val texts = collectVisibleTexts(root)
        val minutes = ScreenEtaParser.parse(texts, rule)
        val unavailable = minutes == null && ScreenEtaParser.isUnavailable(texts)

        debugLog("${rule.serviceId}: parsed=$minutes unavailable=$unavailable from ${texts.take(MAX_LOGGED_TEXTS)}")
        val wait = awaiting?.takeIf { it.serviceId == rule.serviceId }
        if (minutes != null || unavailable) {
            onReading(ScreenReading(rule.serviceId, minutes, System.currentTimeMillis()))
            wait?.signal?.complete(Unit)
        } else if (wait != null && texts.size >= LOADED_SCREEN_MIN_TEXTS) {
            wait.unreadableScans++
        }
    }

    private fun onReading(reading: ScreenReading) {
        val previous = lastSaved[reading.serviceId]
        if (previous != null &&
            previous.etaMinutes == reading.etaMinutes &&
            reading.seenAtEpochMillis - previous.seenAtEpochMillis < RESAVE_INTERVAL_MS
        ) {
            return
        }
        lastSaved[reading.serviceId] = reading
        scope.launch {
            store.save(reading)
            WorkScheduler.runOnce(applicationContext)
        }
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    /** Visible text/content descriptions in reading order (top-to-bottom, then left-to-right). */
    private fun collectVisibleTexts(root: AccessibilityNodeInfo): List<String> {
        val found = mutableListOf<Pair<Rect, String>>()
        val stack = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var visited = 0
        while (stack.isNotEmpty() && visited < MAX_NODES) {
            val node = stack.removeLast()
            visited++
            if (!node.isVisibleToUser) continue
            val text = (node.text ?: node.contentDescription)?.toString()?.trim()
            if (!text.isNullOrEmpty()) {
                found += Rect().also(node::getBoundsInScreen) to text
            }
            for (i in node.childCount - 1 downTo 0) {
                node.getChild(i)?.let(stack::add)
            }
        }
        return found
            .sortedWith(compareBy({ it.first.top }, { it.first.left }))
            .map { it.second }
    }

    companion object {
        private const val TAG = "EtaScreenRead"
        private const val SCAN_INTERVAL_MS = 700L
        private const val RESAVE_INTERVAL_MS = 60_000L
        private const val PER_APP_TIMEOUT_MS = 6_000L
        /** ~2s of a loaded screen (scans run every [SCAN_INTERVAL_MS]) with no ETA → move on. */
        private const val GIVE_UP_AFTER_UNREADABLE_SCANS = 3
        /** Splash screens show only a logo; don't count those as "loaded, no ETA". */
        private const val LOADED_SCREEN_MIN_TEXTS = 5
        private const val POLL_INTERVAL_MS = 250L
        private const val LEFT_CONFIRM_MS = 2_000L
        private const val BETWEEN_APPS_DELAY_MS = 300L
        /** Don't check for the launcher right after launching an app — the transition can briefly show it. */
        private const val LEAVE_DETECTION_GRACE_MS = 1_500L
        private const val MAX_NODES = 2_000
        private const val MAX_LOGGED_TEXTS = 40

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val self = ComponentName(context, EtaAccessibilityService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == self }
        }
    }
}
