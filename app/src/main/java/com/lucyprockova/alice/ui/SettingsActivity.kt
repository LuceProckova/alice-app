package com.lucyprockova.alice.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lucyprockova.alice.R
import com.lucyprockova.alice.data.AliceSettings
import com.lucyprockova.alice.data.Languages
import com.lucyprockova.alice.logic.Snapshot
import com.lucyprockova.alice.logic.StepRules
import com.lucyprockova.alice.notify.Nudge
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

/** settings, name, look, check-in rhythms, nudge, data */
class SettingsActivity : AliceActivity() {
    companion object {
        const val EXTRA_BACK = "back"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen { SettingsScreen(it) }
    }

    /** save, then reschedule nudge in case its hour or on/off changed */
    private fun set(change: (AliceSettings) -> AliceSettings) {
        repo.settingsStore.update(change)
        Nudge.schedule(this, repo.settingsStore.current)
    }

    @Composable
    private fun SettingsScreen(s: Snapshot) {
        val c = Alice.colors
        val st = s.settings
        var name by remember { mutableStateOf(st.name) }
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            set { it.copy(nudgeOn = granted) }
        }
        Column(Modifier.fillMaxSize().background(c.card)) {
            Header(stringResource(R.string.forms_settings_title), back = intent.getStringExtra(EXTRA_BACK) ?: stringResource(R.string.forms_back_habits), onBack = ::finish)
            Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.forms_name))
                OutlinedTextField(
                    name, { name = it; set { o -> o.copy(name = it.trim()) } }, Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.button, unfocusedBorderColor = c.line, focusedTextColor = c.ink, unfocusedTextColor = c.ink),
                )
                LinkRow(Icons.Filled.Translate, stringResource(R.string.lang_title), Languages.of(AppLanguage.current(this@SettingsActivity))?.native ?: stringResource(R.string.lang_phone)) {
                    open(LanguageActivity::class.java)
                }
                PronounRow()
                LinkRow(Icons.Filled.Palette, stringResource(R.string.forms_set_colours)) { open(ColoursActivity::class.java) }
                LinkRow(Icons.Filled.AutoAwesome, stringResource(R.string.forms_set_style)) { open(StylesActivity::class.java) }

                SectionLabel(stringResource(R.string.forms_set_checkins))
                Field(stringResource(R.string.forms_set_checkin_hour), gString(R.string.forms_set_checkin_hour_tip)) {
                    Stepper(st.checkInHour, ":00", 12, 23) { v -> set { it.copy(checkInHour = v) } }
                }
                Field(stringResource(R.string.forms_set_week_start), stringResource(R.string.forms_set_week_start_tip)) {}
                Segmented(listOf(stringResource(R.string.forms_monday), stringResource(R.string.forms_sunday)), if (st.weekStartsSunday) 1 else 0, { i -> set { it.copy(weekStartsSunday = i == 1) } })
                Field(stringResource(R.string.forms_set_new_step_every), stringResource(R.string.forms_set_new_step_every_tip)) {
                    Stepper(st.newStepEveryWeeks, pluralStringResource(R.plurals.forms_unit_weeks, st.newStepEveryWeeks), 1, 8) { v -> set { it.copy(newStepEveryWeeks = v) } }
                }
                Field(stringResource(R.string.forms_set_weekly_day), stringResource(R.string.forms_set_weekly_day_tip)) {}
                DayPicker(st.weeklyDay) { d -> set { it.copy(weeklyDay = d) } }

                Field(stringResource(R.string.forms_set_checkup), gString(R.string.forms_set_checkup_tip)) {}
                Segmented(listOf(stringResource(R.string.forms_set_same), stringResource(R.string.forms_set_own)), if (st.ownCheckup) 1 else 0, { i -> set { it.copy(ownCheckup = i == 1) } }, fontSize = 12.sp)
                if (st.ownCheckup) {
                    val start = StepRules.epoch(st.checkupStartEpochDay)
                    Field(stringResource(R.string.forms_set_starting), stringResource(R.string.forms_set_starting_tip)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(chevronBack, stringResource(R.string.forms_earlier), Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).clickable {
                                set { it.copy(checkupStartEpochDay = start.minusMonths(1).toEpochDay()) }
                            }.padding(4.dp), tint = c.ink)
                            Text(start.format(DateTimeFormatter.ofPattern("MMM yyyy", APP_LOCALE)), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = c.ink)
                            Icon(chevronForward, stringResource(R.string.forms_later), Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).clickable {
                                set { it.copy(checkupStartEpochDay = start.plusMonths(1).toEpochDay()) }
                            }.padding(4.dp), tint = c.ink)
                        }
                    }
                    Field(stringResource(R.string.forms_every), stringResource(R.string.forms_set_every_tip)) {
                        Stepper(st.checkupEveryMonths, pluralStringResource(R.plurals.forms_unit_months, st.checkupEveryMonths), 1, 12) { v -> set { it.copy(checkupEveryMonths = v) } }
                    }
                    Field(stringResource(R.string.forms_set_day_of_month), stringResource(R.string.forms_set_day_of_month_tip)) {
                        // max 28 so every month has that day
                        Stepper(st.checkupDayOfMonth, "", 1, 28) { v -> set { it.copy(checkupDayOfMonth = v) } }
                    }
                } else Text(stringResource(R.string.forms_set_everyone), fontSize = 12.sp, color = c.muted)

                Field(stringResource(R.string.forms_set_days_build), stringResource(R.string.forms_set_days_build_tip)) {
                    Stepper(st.daysToBuild, pluralStringResource(R.plurals.forms_unit_days_n, st.daysToBuild), 14, 120) { v -> set { it.copy(daysToBuild = v) } }
                }
                Field(stringResource(R.string.forms_set_nudge), stringResource(R.string.forms_set_nudge_tip)) {
                    Switch(
                        st.nudgeOn,
                        {
                            on ->
                            // android 13+ needs runtime permission to post notifications
                            if (on && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this@SettingsActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else set { it.copy(nudgeOn = on) }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = c.button, checkedThumbColor = c.buttonText),
                    )
                }
                if (st.nudgeOn) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Stepper(st.nudgeHour, ":00", 6, 23) { v -> set { it.copy(nudgeHour = v) } }
                }

                SectionLabel(stringResource(R.string.forms_set_more))
                LinkRow(Icons.Filled.Sell, stringResource(R.string.forms_set_reasons)) { open(ReasonsActivity::class.java) }
                LinkRow(Icons.Filled.School, stringResource(R.string.forms_set_tutorial)) { open(TutorialActivity::class.java) }
                LinkRow(Icons.Filled.Download, stringResource(R.string.forms_set_export)) { open(ExportActivity::class.java) }
                LinkRow(Icons.Filled.Upload, stringResource(R.string.forms_set_import)) { open(ImportActivity::class.java) }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    @Composable
    private fun Field(title: String, info: String, control: @Composable () -> Unit) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Alice.colors.ink)
            InfoTip(title, info, Alice.colors.muted)
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { control() }
        }
    }

    /** pronouns row, how alice talks to you where words have a gender, same panel look as the rows above */
    @Composable
    private fun PronounRow() {
        val c = Alice.colors
        val picked = Pronouns.current(this)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.panel).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, null, Modifier.size(20.dp), tint = c.headerSub)
                Text(stringResource(R.string.pron_title), Modifier.padding(start = 10.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
            }
            val options = listOf(Pronoun.SHE to R.string.pron_she, Pronoun.HE to R.string.pron_he, Pronoun.THEY to R.string.pron_they)
            Segmented(options.map { stringResource(it.second) }, options.indexOfFirst { it.first == picked }, { i -> choosePronoun(options[i].first) }, fontSize = 12.sp)
        }
    }

    /** saves the pick, android 14+ redraws screens itself, older phones restart here so every text picks it up */
    private fun choosePronoun(p: Pronoun) {
        if (p == Pronouns.current(this)) return
        Pronouns.set(this, p)
        if (Build.VERSION.SDK_INT < 34) recreate()
    }

    @Composable
    private fun LinkRow(icon: ImageVector, text: String, value: String? = null, onClick: () -> Unit) {
        val c = Alice.colors
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.panel).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = c.headerSub)
            Text(text, Modifier.weight(1f).padding(start = 10.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
            if (value != null) Text(value, Modifier.padding(end = 4.dp), fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = c.muted)
            Icon(chevronForward, null, tint = c.muted)
        }
    }
}

@Composable
fun DayPicker(day: Int, onPick: (Int) -> Unit) {
    val c = Alice.colors
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        (1..7).map { DayOfWeek.of(it).letter() }.forEachIndexed { i, d ->
            val on = day == i + 1
            Text(
                d, Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (on) c.button else c.empty).clickable { onPick(i + 1) }.padding(vertical = 9.dp),
                textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = if (on) c.buttonText else c.muted,
            )
        }
    }
}

