package com.lucyprockova.alice.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

/** all settings, small, so one json in SharedPreferences not db */
data class AliceSettings(
    val name: String = "",
    val checkInHour: Int = 20,
    val weekStartsSunday: Boolean = false,
    val newStepEveryWeeks: Int = 1,
    val weeklyDay: Int = 7,
    /** false = shared check-up days everyone uses */
    val ownCheckup: Boolean = false,
    val checkupStartEpochDay: Long = LocalDate.now().toEpochDay(),
    val checkupEveryMonths: Int = 3,
    val checkupDayOfMonth: Int = 15,
    val daysToBuild: Int = DEFAULT_DAYS_TO_BUILD,
    val nudgeOn: Boolean = true,
    val nudgeHour: Int = 21,
    val theme: Int = 0,
    val summaryStyle: Int = 0,
    val welcomed: Boolean = false,
    val tutorialSeen: Boolean = false,
    /** every n weeks counts from here */
    val firstEpochDay: Long = LocalDate.now().toEpochDay(),
    val hiddenIdeas: Set<String> = emptySet(),
    val celebrated: Set<Long> = emptySet(),
    val glanceShownFor: Long = 0,
    val summaryShownFor: Long = 0,
) {
    val weeklyDayOfWeek: DayOfWeek get() = DayOfWeek.of(weeklyDay)
    val firstDayOfWeek: DayOfWeek get() = if (weekStartsSunday) DayOfWeek.SUNDAY else DayOfWeek.MONDAY

    fun toJson(): JSONObject = JSONObject()
        .put("name", name).put("checkInHour", checkInHour).put("weekStartsSunday", weekStartsSunday)
        .put("newStepEveryWeeks", newStepEveryWeeks).put("weeklyDay", weeklyDay).put("ownCheckup", ownCheckup)
        .put("checkupStartEpochDay", checkupStartEpochDay).put("checkupEveryMonths", checkupEveryMonths)
        .put("checkupDayOfMonth", checkupDayOfMonth).put("daysToBuild", daysToBuild).put("nudgeOn", nudgeOn)
        .put("nudgeHour", nudgeHour).put("theme", theme).put("summaryStyle", summaryStyle).put("welcomed", welcomed)
        .put("tutorialSeen", tutorialSeen).put("firstEpochDay", firstEpochDay)
        .put("hiddenIdeas", org.json.JSONArray(hiddenIdeas.toList()))
        .put("celebrated", org.json.JSONArray(celebrated.toList()))
        .put("glanceShownFor", glanceShownFor).put("summaryShownFor", summaryShownFor)

    companion object {
        /** median days to form habit, lally et al 2010 */
        const val DEFAULT_DAYS_TO_BUILD = 66

        fun fromJson(o: JSONObject): AliceSettings {
            // defaults so older backups without new keys still load
            val d = AliceSettings()
            fun strings(key: String) = o.optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet()
            fun longs(key: String) = o.optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getLong(it) }.toSet() } ?: emptySet()
            return AliceSettings(
                name = o.optString("name", d.name),
                checkInHour = o.optInt("checkInHour", d.checkInHour),
                weekStartsSunday = o.optBoolean("weekStartsSunday", d.weekStartsSunday),
                newStepEveryWeeks = o.optInt("newStepEveryWeeks", d.newStepEveryWeeks),
                weeklyDay = o.optInt("weeklyDay", d.weeklyDay),
                ownCheckup = o.optBoolean("ownCheckup", d.ownCheckup),
                checkupStartEpochDay = o.optLong("checkupStartEpochDay", d.checkupStartEpochDay),
                checkupEveryMonths = o.optInt("checkupEveryMonths", d.checkupEveryMonths),
                checkupDayOfMonth = o.optInt("checkupDayOfMonth", d.checkupDayOfMonth),
                daysToBuild = o.optInt("daysToBuild", d.daysToBuild),
                nudgeOn = o.optBoolean("nudgeOn", d.nudgeOn),
                nudgeHour = o.optInt("nudgeHour", d.nudgeHour),
                theme = o.optInt("theme", d.theme),
                summaryStyle = o.optInt("summaryStyle", d.summaryStyle),
                welcomed = o.optBoolean("welcomed", d.welcomed),
                tutorialSeen = o.optBoolean("tutorialSeen", d.tutorialSeen),
                firstEpochDay = o.optLong("firstEpochDay", d.firstEpochDay),
                hiddenIdeas = strings("hiddenIdeas"),
                celebrated = longs("celebrated"),
                glanceShownFor = o.optLong("glanceShownFor", 0),
                summaryShownFor = o.optLong("summaryShownFor", 0),
            )
        }
    }
}

/** saves [AliceSettings], every change out as [StateFlow] */
class SettingsStore(context: Context) {
    private companion object { const val KEY = "settings" }

    private val prefs = context.applicationContext.getSharedPreferences("alice", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())
    val settings: StateFlow<AliceSettings> = state.asStateFlow()
    val current: AliceSettings get() = state.value

    // defaults saved straight away so start dates stay fixed
    private fun load(): AliceSettings {
        val saved = prefs.getString(KEY, null) ?: return AliceSettings().also { save(it) }
        return runCatching { AliceSettings.fromJson(JSONObject(saved)) }.getOrElse { e ->
            // broken save kept aside, not overwritten, defaults only in memory till next real change
            Log.w("Alice", "settings unreadable, using defaults", e)
            prefs.edit { putString("$KEY.broken", saved) }
            AliceSettings()
        }
    }

    private fun save(s: AliceSettings) = prefs.edit { putString(KEY, s.toJson().toString()) }

    @Synchronized
    fun update(change: (AliceSettings) -> AliceSettings) {
        val next = change(state.value)
        save(next)
        state.value = next
    }
}
