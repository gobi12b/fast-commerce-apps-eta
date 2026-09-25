package com.example.etawidget.screenread

import android.os.SystemClock
import com.example.etawidget.data.model.ServiceId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hand-off between the UI and [EtaAccessibilityService] for a capture sweep:
 * opening each installed delivery app in turn and reading its ETA. The sweep
 * itself runs in the service, since an accessibility service is allowed to
 * start activities while this app is in the background.
 */
object CaptureSweep {

    sealed interface State {
        data object Idle : State
        data class Running(val current: ServiceId, val index: Int, val total: Int) : State
        data class Finished(
            val captured: Set<ServiceId>,
            val missed: Set<ServiceId>,
            /** True if the user left mid-sweep (went Home/Recents), so the rest was skipped. */
            val stoppedEarly: Boolean,
        ) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * Pending request, as its SystemClock.uptimeMillis(). Held rather than
     * fire-and-forget so a request made just before the service (re)connects
     * isn't lost; the service ignores requests older than [REQUEST_MAX_AGE_MS]
     * so a check never starts long after the user asked.
     */
    private val _requests = Channel<Long>(Channel.CONFLATED)
    internal val requests: ReceiveChannel<Long> = _requests

    const val REQUEST_MAX_AGE_MS = 10_000L

    /** Asks the accessibility service to start a sweep. */
    fun request() {
        _requests.trySend(SystemClock.uptimeMillis())
    }

    internal fun update(state: State) {
        _state.value = state
    }
}
