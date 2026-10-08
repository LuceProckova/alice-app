package com.lucyprockova.alice.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lucyprockova.alice.AliceApp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.data.AliceSettings
import com.lucyprockova.alice.logic.CheckIns
import com.lucyprockova.alice.logic.Snapshot
import com.lucyprockova.alice.ui.AppLanguage
import com.lucyprockova.alice.ui.MainActivity
import com.lucyprockova.alice.ui.Pronouns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** daily nudge alarm, notifies only if something today still unticked */
object Nudge {
    private const val CHANNEL = "nudge"
    private const val ALARM_REQUEST = 1
    private const val OPEN_REQUEST = 0
    private const val NOTIFICATION_ID = 1

    fun schedule(context: Context, settings: AliceSettings) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(context, ALARM_REQUEST, Intent(context, NudgeReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        // cancel first so changing hour never leaves two alarms
        am.cancel(pi)
        if (!settings.nudgeOn) return
        var at = LocalDate.now().atTime(settings.nudgeHour, 0)
        if (!at.isAfter(LocalDateTime.now())) at = at.plusDays(1)
        // inexact but fires in doze, no exact alarm permission needed
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), pi)
    }

    fun show(base: Context, open: Int) {
        // receiver context isn't a screen, so give it the picked language
        val context = AppLanguage.wrap(base)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.core_nudge_channel), NotificationManager.IMPORTANCE_DEFAULT))
        val tap = PendingIntent.getActivity(context, OPEN_REQUEST, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_nudge)
            .setContentTitle(Pronouns.string(context, R.string.core_nudge_title))
            .setContentText(context.resources.getQuantityString(R.plurals.core_nudge_text, open, open))
            .setContentIntent(tap).setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
    }
}

/** fires at nudge time, checks today, sets next alarm */
class NudgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repo = (context.applicationContext as AliceApp).repository
        val done = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val s: Snapshot = repo.snapshots(flowOf(LocalDateTime.now(repo.clock))).first()
                val open = CheckIns.openOn(s, s.today).size
                if (open > 0) Nudge.show(context, open)
            } catch (e: Exception) {
                Log.e("Alice", "nudge check failed", e)
            } finally {
                // one shot alarm, so book tomorrow's every time, even after a failed check
                runCatching { Nudge.schedule(context, repo.settingsStore.current) }
                done.finish()
            }
        }
    }
}

/** reboot wipes alarms, so put nudge back */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Nudge.schedule(context, (context.applicationContext as AliceApp).repository.settingsStore.current)
        }
    }
}
