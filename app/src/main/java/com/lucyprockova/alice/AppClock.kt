package com.lucyprockova.alice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.lucyprockova.alice.data.AliceSettings
import com.lucyprockova.alice.logic.Boundaries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZonedDateTime

/**
 * one "now" for every screen, updated only when it matters: at the next check-in hour or midnight,
 * when the app comes back to the front, and when the phone's time, date or time zone changes
 */
class AppClock(
    private val context: Context,
    private val clock: Clock,
    private val settings: StateFlow<AliceSettings>,
    private val scope: CoroutineScope,
) : DefaultLifecycleObserver {
    private val state = MutableStateFlow(LocalDateTime.now(clock))
    val now: StateFlow<LocalDateTime> = state.asStateFlow()

    private var waiting: Job? = null
    private var listening = false

    // phone time, date or zone changed by hand or by the network
    private val changes = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) = refresh()
    }

    init {
        // a new check-in hour moves the next boundary
        scope.launch { settings.distinctUntilChangedBy { it.checkInHour }.drop(1).collect { refresh() } }
    }

    /** fresh now plus a new wait for the next boundary, used on resume and after any time change */
    fun refresh() {
        state.value = LocalDateTime.now(clock)
        schedule()
    }

    /** one wait for the next boundary, the old one is cancelled so there is only ever one */
    private fun schedule() {
        waiting?.cancel()
        val now = ZonedDateTime.now(clock)
        val next = Boundaries.next(now, settings.value.checkInHour)
        waiting = scope.launch {
            delay(Duration.between(now, next).toMillis().coerceAtLeast(0))
            refresh()
        }
    }

    // app in front: listen for time changes and wait for the next boundary
    override fun onStart(owner: LifecycleOwner) {
        if (!listening) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED); addAction(Intent.ACTION_DATE_CHANGED)
            }
            // system broadcasts still reach a not exported receiver
            ContextCompat.registerReceiver(context, changes, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            listening = true
        }
        refresh()
    }

    // app in the background: nothing on screen to update, so stop waiting and listening
    override fun onStop(owner: LifecycleOwner) {
        waiting?.cancel()
        if (listening) { context.unregisterReceiver(changes); listening = false }
    }
}
