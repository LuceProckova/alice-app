package com.lucyprockova.alice.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucyprockova.alice.R
import com.lucyprockova.alice.data.CheckInDay
import com.lucyprockova.alice.data.Entry
import com.lucyprockova.alice.data.SlipLevel
import com.lucyprockova.alice.data.Step
import com.lucyprockova.alice.data.StepKind
import com.lucyprockova.alice.logic.CheckInKind
import com.lucyprockova.alice.logic.CheckIns
import com.lucyprockova.alice.logic.Owed
import com.lucyprockova.alice.logic.Phase
import com.lucyprockova.alice.logic.Snapshot
import com.lucyprockova.alice.logic.StepRules
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class DailyAnswer(val kept: Boolean, val reason: String? = null) : java.io.Serializable

/** checkin flow, owed daily, weekly, 3 month checkup as numbered steps */
class CheckInActivity : AliceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen { s ->
            // owed frozen on open, so answering doesn't shuffle pages
            val owed = remember { CheckIns.owed(s) }
            val flow = owed.flow
            var page by rememberSaveable { mutableIntStateOf(0) }
            if (flow.isEmpty()) { finish(); return@screen }
            val kind = flow[page.coerceAtMost(flow.lastIndex)]
            val stepLabel = if (flow.size > 1) stringResource(R.string.forms_checkin_step_of, page + 1, flow.size) else null
            val nextLabel = flow.getOrNull(page + 1)?.let {
                when (it) { CheckInKind.WEEKLY -> stringResource(R.string.forms_checkin_next_weekly); CheckInKind.QUARTER -> stringResource(R.string.forms_checkin_next_quarter); else -> "" }
            }
            val next: () -> Unit = { if (page < flow.lastIndex) page++ else finish() }
            when (kind) {
                CheckInKind.DAILY -> DailyPage(s, owed, stepLabel, nextLabel, next)
                CheckInKind.WEEKLY -> WeeklyPage(s, owed, stepLabel, nextLabel, next)
                CheckInKind.QUARTER -> QuarterPage(s, owed.quarterDue!!, stepLabel)
            }
        }
    }

    private val dayFmt get() = DateTimeFormatter.ofPattern("EEE d MMM", APP_LOCALE)

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun DailyPage(s: Snapshot, owed: Owed, stepLabel: String?, nextLabel: String?, next: () -> Unit) {
        val c = Alice.colors
        val reasons by repo.reasons.collectAsStateWithLifecycle(emptyList())
        // saveable, so answers survive android closing the app in the background
        val answers = rememberSaveable(saver = stateMapSaver()) { mutableStateMapOf<Pair<Long, Long>, DailyAnswer>() }
        val skipped = rememberSaveable(saver = stateMapSaver()) { mutableStateMapOf<Long, Boolean>() }
        var newTagFor by rememberSaveable { mutableStateOf<Pair<Long, Long>?>(null) }
        val days = owed.dailyDays.sortedDescending()
        // frozen on open, rows don't vanish mid answer
        val stepsByDay = remember { days.associateWith { CheckIns.openOn(s, it) } }
        val newest = s.steps.filter { !it.oneOff && StepRules.dayNumber(it, s.today) <= 7 }.maxWithOrNull(compareBy({ it.buildStartEpochDay }, { it.id }))

        fun save(then: () -> Unit) {
            val entries = mutableListOf<Entry>()
            days.forEach { d ->
                if (skipped[d.toEpochDay()] == true) return@forEach
                stepsByDay[d].orEmpty().forEach { step ->
                    answers[step.id to d.toEpochDay()]?.let { a ->
                        // tick = full daily target, cross keeps count already logged
                        entries += if (a.kept) Entry(step.id, d.toEpochDay(), StepRules.dailyTarget(step, d))
                        else Entry(step.id, d.toEpochDay(), s.count(step.id, d), missed = true, reason = a.reason)
                    }
                }
            }
            // every owed day gets a CheckInDay row, even skipped, so it stops being owed
            io { saveDaily(entries, days.map { CheckInDay(it.toEpochDay(), skipped[it.toEpochDay()] == true) }); then() }
        }

        Column(Modifier.fillMaxSize().background(c.card)) {
            Header(stringResource(R.string.forms_daily_title), back = stringResource(R.string.forms_back_habits), onBack = ::finish) {
                if (stepLabel != null) Text(stepLabel, fontSize = 11.sp, color = c.headerSub, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                val earlier = days.count { it != s.today }
                if (earlier > 0) {
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(pluralStringResource(R.plurals.forms_daily_earlier, earlier, earlier), fontSize = 12.5.sp, color = c.muted, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.forms_daily_skip_all), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = c.headerText,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { days.forEach { skipped[it.toEpochDay()] = true }; save(next) }.padding(6.dp))
                    }
                }
                days.forEach { d ->
                    val isSkipped = skipped[d.toEpochDay()] == true
                    SectionLabel(if (d == s.today) stringResource(R.string.forms_daily_today, d.format(dayFmt)) else d.format(dayFmt)) {
                        Spacer(Modifier.weight(1f))
                        Text(stringResource(if (isSkipped) R.string.forms_daily_undo_skip else R.string.forms_daily_skip), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = c.headerText,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { skipped[d.toEpochDay()] = !isSkipped }.padding(4.dp))
                    }
                    if (!isSkipped) stepsByDay[d].orEmpty().forEach { step ->
                        val key = step.id to d.toEpochDay()
                        val a = answers[key]
                        Panel(Modifier.padding(bottom = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (step == newest) Icon(Icons.Filled.FiberNew, null, Modifier.size(16.dp).padding(end = 2.dp), tint = c.headerText)
                                else if (step.kind != StepKind.HABIT) Icon(step.kind.icon(), null, Modifier.size(14.dp), tint = c.muted)
                                Text(" " + step.name, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = c.ink)
                                AnswerButton("✓", a?.kept == true, c.kept) { answers[key] = DailyAnswer(true) }
                                Spacer(Modifier.width(6.dp))
                                AnswerButton("✕", a?.kept == false, c.missed) { answers[key] = DailyAnswer(false) }
                            }
                            if (a?.kept == false) {
                                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    reasons.forEach { r ->
                                        Pill(r.name, a.reason == r.name, iconFor(r.icon)) { answers[key] = a.copy(reason = if (a.reason == r.name) null else r.name) }
                                    }
                                    Pill(stringResource(R.string.forms_daily_new_tag), icon = Icons.Filled.Add) { newTagFor = key }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            MainButton(nextLabel ?: stringResource(R.string.forms_save), Modifier.navigationBarsPadding().padding(16.dp)) { save(next) }
        }

        newTagFor?.let { key ->
            var text by rememberSaveable { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { newTagFor = null },
                title = { Text(stringResource(R.string.forms_daily_new_tag)) },
                text = { OutlinedTextField(text, { text = it }, singleLine = true, placeholder = { Text(stringResource(R.string.forms_daily_new_tag_hint)) }) },
                confirmButton = {
                    TextButton(onClick = {
                        if (text.isNotBlank()) { io { addReason(text) }; answers[key] = DailyAnswer(false, text.trim()) }
                        newTagFor = null
                    }) { Text(stringResource(R.string.forms_add)) }
                },
                dismissButton = { TextButton(onClick = { newTagFor = null }) { Text(stringResource(R.string.core_cancel)) } },
            )
        }
    }

    @Composable
    private fun AnswerButton(text: String, on: Boolean, color: Color, onClick: () -> Unit) {
        val c = Alice.colors
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (on) color else c.card)
                .border(1.5.dp, if (on) color else c.line, RoundedCornerShape(10.dp)).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (on) c.keptText else c.muted) }
    }

    @Composable
    private fun WeeklyPage(s: Snapshot, owed: Owed, stepLabel: String?, nextLabel: String?, next: () -> Unit) {
        val c = Alice.colors
        var draft by rememberSaveable { mutableStateOf(StepDraft()) }
        Column(Modifier.fillMaxSize().background(c.card).imePadding()) {
            Header(stringResource(R.string.forms_weekly_title), back = stringResource(R.string.forms_back_habits), onBack = ::finish) {
                if (stepLabel != null) Text(stepLabel, fontSize = 11.sp, color = c.headerSub, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                if (owed.weeklyDue.size > 1) Text(pluralStringResource(R.plurals.forms_weekly_owed, owed.weeklyDue.size, owed.weeklyDue.size), fontSize = 12.sp, color = c.muted, modifier = Modifier.padding(top = 10.dp))
                NewStepForm(draft, repo.settingsStore, s.settings) { draft = it }
                Spacer(Modifier.height(16.dp))
            }
            Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MainButton(nextLabel ?: stringResource(R.string.forms_finish), enabled = draft.valid) {
                    io { saveWeekly(owed.weeklyDue, draft.toStep(s.today)); next() }
                }
                // null step still marks weeks done
                MainButton(stringResource(R.string.forms_weekly_no_step), quiet = true) {
                    io { saveWeekly(owed.weeklyDue, null); next() }
                }
            }
        }
    }

    @Composable
    private fun QuarterPage(s: Snapshot, due: LocalDate, stepLabel: String?) {
        val c = Alice.colors
        val from = CheckIns.previousQuarter(s, due)
        val settled = remember { s.steps.filter { StepRules.phase(it, s.settings, s.today) == Phase.SETTLED } }
        val levels = rememberSaveable(saver = stateMapSaver()) { mutableStateMapOf<Long, SlipLevel>() }
        val open = rememberSaveable(saver = stateMapSaver()) { mutableStateMapOf<Long, Boolean>() }
        Column(Modifier.fillMaxSize().background(c.card)) {
            Header(stringResource(R.string.forms_quarter_title), back = stringResource(R.string.forms_back_habits), onBack = ::finish) {
                if (stepLabel != null) Text(stepLabel, fontSize = 11.sp, color = c.headerSub, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                if (settled.isEmpty()) Text(stringResource(R.string.forms_quarter_nothing), fontSize = 13.sp, color = c.muted, modifier = Modifier.padding(vertical = 20.dp))
                listOf(StepKind.HABIT to R.string.forms_quarter_habits, StepKind.SWAP to R.string.forms_quarter_swaps, StepKind.AVOID to R.string.forms_quarter_avoid).forEach { (kind, title) ->
                    val list = settled.filter { it.kind == kind }
                    if (list.isNotEmpty()) SectionLabel(stringResource(title))
                    list.forEach { step ->
                        val level = levels[step.id] ?: SlipLevel.NEVER
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(step.name, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = c.ink)
                                val missed = level != SlipLevel.NEVER || open[step.id] == true
                                Box(
                                    Modifier.clip(RoundedCornerShape(50)).background(if (missed) c.missed else c.empty)
                                        .clickable {
                                            if (open[step.id] == true) { open[step.id] = false; levels.remove(step.id) } else open[step.id] = true
                                        }.padding(horizontal = 14.dp, vertical = 6.dp),
                                ) { Text(if (level == SlipLevel.NEVER) stringResource(R.string.forms_quarter_missed) else stringResource(R.string.forms_quarter_missed_level, level.label), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (missed) c.keptText else c.muted) }
                            }
                            if (open[step.id] == true) {
                                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    SlipLevel.entries.drop(1).forEach { l ->
                                        Box(
                                            Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (level == l) c.missed else c.empty)
                                                .clickable { levels[step.id] = l }.padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center,
                                        ) { Text(l.label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = if (level == l) c.keptText else c.ink, textAlign = TextAlign.Center) }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            MainButton(stringResource(R.string.forms_finish), Modifier.navigationBarsPadding().padding(16.dp)) {
                // untouched steps count as never missed
                val answers: Map<Step, SlipLevel> = settled.associateWith { levels[it.id] ?: SlipLevel.NEVER }
                // io already runs on main, so the ui calls follow the save directly
                io {
                    saveCheckup(due, from, answers)
                    open(SummaryActivity::class.java, SummaryActivity.EXTRA_DUE to due.toEpochDay(), SummaryActivity.EXTRA_AFTER_CHECKUP to true)
                    finish()
                }
            }
        }
    }
}
