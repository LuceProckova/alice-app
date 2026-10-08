package com.lucyprockova.alice.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color as AColor
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lucyprockova.alice.AliceApp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.data.AliceRepository
import com.lucyprockova.alice.logic.Snapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

/** colours darker than this get light text and status bar icons */
const val DARK_LUMINANCE = 0.45f

/** live [Snapshot] per screen, now comes from the one shared [com.lucyprockova.alice.AppClock] */
class SnapshotViewModel(val repo: AliceRepository, now: StateFlow<LocalDateTime>) : ViewModel() {
    val snapshot: StateFlow<Snapshot?> = repo.snapshots(now)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** base of every screen, shared repo, live snapshot, theme, edge to edge */
abstract class AliceActivity : ComponentActivity() {
    val repo: AliceRepository get() = (application as AliceApp).repository
    protected val vm: SnapshotViewModel by viewModels {
        viewModelFactory { initializer { SnapshotViewModel((application as AliceApp).repository, (application as AliceApp).appClock.now) } }
    }

    // before android 13 the picked language, before 14 the picked pronoun, are put on each screen by hand
    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(Pronouns.wrap(AppLanguage.wrap(newBase)))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    override fun onResume() {
        super.onResume()
        // back on screen, fresh now straight away
        (application as AliceApp).appClock.refresh()
    }

    protected fun screen(content: @Composable (Snapshot) -> Unit) {
        setContent {
            val settings by repo.settingsStore.settings.collectAsStateWithLifecycle()
            val snap by vm.snapshot.collectAsStateWithLifecycle()
            AliceTheme(settings.theme) {
                val dark = Alice.colors.dark
                val headerDark = Alice.colors.header.luminance() < DARK_LUMINANCE
                // status bar icons follow header colour, nav bar follows theme
                LaunchedEffect(dark, headerDark) {
                    val style = if (headerDark) SystemBarStyle.dark(AColor.TRANSPARENT) else SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT)
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = if (dark) SystemBarStyle.dark(AColor.TRANSPARENT) else SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT))
                }
                snap?.let { content(it) }
            }
        }
    }

    /** runs a save on the app scope so it finishes after finish(), a failure becomes a message, never a crash */
    fun io(block: suspend AliceRepository.() -> Unit) {
        val app = application as AliceApp
        app.scope.launch {
            try {
                app.repository.block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("Alice", "save failed", e)
                Toast.makeText(app, app.getString(R.string.core_save_failed), Toast.LENGTH_LONG).show()
            }
        }
    }
}

/** opens another screen, extras by type */
fun Context.open(screen: Class<out Activity>, vararg extras: Pair<String, Any?>) {
    val i = Intent(this, screen)
    extras.forEach { (k, v) ->
        when (v) {
            is Long -> i.putExtra(k, v)
            is Int -> i.putExtra(k, v)
            is String -> i.putExtra(k, v)
            is Boolean -> i.putExtra(k, v)
        }
    }
    startActivity(i)
}

/** saves a state map as a plain HashMap, so rememberSaveable keeps answers through process death */
fun <K, V> stateMapSaver(): Saver<SnapshotStateMap<K, V>, HashMap<K, V>> = Saver(
    save = { HashMap(it) },
    restore = { saved -> mutableStateMapOf<K, V>().apply { putAll(saved) } },
)

/** language for day and month names, the app language picked in settings */
val APP_LOCALE: Locale get() = Locale.getDefault()

/** m, t, w... from the system calendar names, not from the code's enum names */
fun DayOfWeek.letter(): String = getDisplayName(TextStyle.NARROW, APP_LOCALE).uppercase(APP_LOCALE)

/** jan, feb... from the system calendar names */
fun Month.short(): String = getDisplayName(TextStyle.SHORT, APP_LOCALE)

/** window width in dp, the newer way, screenWidthDp is deprecated */
@Composable
fun windowWidthDp(): Dp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
