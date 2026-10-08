package com.lucyprockova.alice

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.lucyprockova.alice.data.AliceDatabase
import com.lucyprockova.alice.data.AliceRepository
import com.lucyprockova.alice.data.SettingsStore
import com.lucyprockova.alice.logic.PhoneClock
import com.lucyprockova.alice.notify.Nudge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/** app class, one shared repo, keeps nudge alarm set */
class AliceApp : Application() {
    /** app wide scope, saves finish even after the screen closes */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val repository: AliceRepository by lazy {
        AliceRepository(AliceDatabase.get(this), SettingsStore(this), PhoneClock) { AliceDatabase.defaultReasons(this) }
    }

    /** the one shared now every screen reads, see [AppClock] */
    val appClock: AppClock by lazy { AppClock(this, PhoneClock, repository.settingsStore.settings, scope) }

    override fun onCreate() {
        super.onCreate()
        // clock follows the whole app coming to the front or going away, not each screen
        ProcessLifecycleOwner.get().lifecycle.addObserver(appClock)
        // shared exports and pictures are copies of personal data, drop leftovers from last time
        scope.launch(Dispatchers.IO) { File(cacheDir, "share").deleteRecursively() }
        // alarm can be lost on update or force stop, so reset every start
        Nudge.schedule(this, repository.settingsStore.current)
    }
}
