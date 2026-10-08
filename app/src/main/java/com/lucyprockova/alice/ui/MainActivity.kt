package com.lucyprockova.alice.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** tab switch length in ms, calm m3 motion */
private const val TAB_MS = 220

/** one activity for the 4 main tabs, bottom bar only swaps content, each tab keeps its state */
class MainActivity : AliceActivity() {
    companion object {
        /** tab name to open on, e.g. from adb or a notification */
        const val EXTRA_TAB = "tab"
    }

    // tab asked for by intent, read by compose, so onNewIntent can switch tab too
    private var requested by mutableStateOf<Tab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!repo.settingsStore.current.welcomed) { open(WelcomeActivity::class.java); finish(); return }
        // only on first start, after rotation the saved tab wins
        if (savedInstanceState == null) requested = tabFrom(intent)
        screen { s ->
            var tab by rememberSaveable { mutableStateOf(requested ?: Tab.HABITS) }
            LaunchedEffect(requested) { requested?.let { tab = it; requested = null } }
            // one saved state slot per tab, so scroll, week, filters survive switching
            val tabs = rememberSaveableStateHolder()
            // back from other tab goes to habits, back from habits leaves app
            BackHandler(enabled = tab != Tab.HABITS) { tab = Tab.HABITS }
            Column(Modifier.fillMaxSize()) {
                // short shared axis slide towards the tab picked, so it reads as one space
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (slideInHorizontally(tween(TAB_MS)) { w -> dir * w / 5 } + fadeIn(tween(TAB_MS)))
                            .togetherWith(slideOutHorizontally(tween(TAB_MS)) { w -> -dir * w / 5 } + fadeOut(tween(TAB_MS / 2)))
                    },
                    modifier = Modifier.weight(1f),
                    label = "tab",
                ) { shown ->
                    tabs.SaveableStateProvider(shown.name) {
                        when (shown) {
                            Tab.HABITS -> HabitsTab(s)
                            Tab.CHANGES -> ChangesTab(s)
                            Tab.AVOID -> AvoidListTab(s)
                            Tab.INSIGHTS -> InsightsTab(s)
                        }
                    }
                }
                BottomNav(tab) { tab = it }
            }
        }
    }

    // singleTask, so a new launch lands here instead of a second copy
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        tabFrom(intent)?.let { requested = it }
    }

    private fun tabFrom(i: Intent?): Tab? = i?.getStringExtra(EXTRA_TAB)?.let { n -> Tab.entries.firstOrNull { it.name.equals(n, true) } }
}
