package com.lucyprockova.alice.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lucyprockova.alice.AliceApp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** the 4 tabs in one activity, bar switches, back returns to habits */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule val compose = createEmptyComposeRule()

    // skip welcome so main opens straight away
    @Before fun welcomed() {
        ApplicationProvider.getApplicationContext<AliceApp>().repository.settingsStore.update { it.copy(welcomed = true) }
    }

    // insights tab shows its header next to the bar label, back goes to habits header
    @Test fun tabsSwitchAndBackGoesToHabits() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithText("Insights").performClick()
            compose.onAllNodesWithText("Insights").assertCountEquals(2)
            // same as the system back button
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.onAllNodesWithText("Insights").assertCountEquals(1)
            compose.onNodeWithText("Alice").assertExists()
        }
    }
}
