package com.lucyprockova.alice.ui

import com.lucyprockova.alice.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import android.os.Bundle
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.data.Step
import com.lucyprockova.alice.logic.CheckIns
import com.lucyprockova.alice.logic.Grid
import com.lucyprockova.alice.logic.Phase
import com.lucyprockova.alice.logic.Snapshot
import com.lucyprockova.alice.logic.Stats
import com.lucyprockova.alice.logic.StepRules
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val longDate get() = DateTimeFormatter.ofPattern("d MMM yyyy")

/** one habit or swap, dates, figures, streaks, history chart, why it slipped */
class HabitActivity : AliceActivity() {
    companion object { const val EXTRA_ID = "stepId" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra(EXTRA_ID, -1)
        screen { s ->
            val step = s.steps.firstOrNull { it.id == id }
            if (step == null) { finish(); return@screen }
            val c = Alice.colors
            var deleting by remember { mutableStateOf(false) }
            Column(Modifier.fillMaxSize().background(c.card)) {
                Header(step.name, back = stringResource(R.string.tabs_back), onBack = ::finish, actions = {
                    IconButton(onClick = { open(NewStepActivity::class.java, NewStepActivity.EXTRA_EDIT_ID to step.id) }) { Icon(Icons.Filled.Edit, stringResource(R.string.tabs_edit), tint = c.headerText) }
                    IconButton(onClick = { deleting = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.tabs_delete), tint = c.headerText) }
                })
                Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StepDates(step, s)
                    StepFigures(step, s)
                    HistoryChart(step, s) { day ->
                        if (com.lucyprockova.alice.logic.Grid.cell(step, day, s).state == com.lucyprockova.alice.logic.CellState.LOCKED)
                            android.widget.Toast.makeText(this@HabitActivity, getString(R.string.tabs_eod_unlock, s.settings.checkInHour), android.widget.Toast.LENGTH_SHORT).show()
                        else io { tap(step, day, s) }
                    }
                    SlipList(step, s) { reason -> io { clearStepReason(step, reason) } }
                }
            }
            if (deleting) ConfirmDialog(stringResource(R.string.tabs_delete_title, step.name), stringResource(R.string.tabs_habit_delete_text), stringResource(R.string.tabs_delete), { io { delete(step) }; finish() }) { deleting = false }
        }
    }
}

/** removes one slip reason from step's history, slips themselves stay */
suspend fun com.lucyprockova.alice.data.AliceRepository.clearStepReason(step: Step, reason: String) {
    val list = exportData().entries.filter { it.stepId == step.id && it.reason == reason }
    saveDaily(list.map { it.copy(reason = null) }, emptyList())
}

@Composable
fun StepDates(step: Step, s: Snapshot) {
    val c = Alice.colors
    val phase = StepRules.phase(step, s.settings, s.today)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DateBox(stringResource(R.string.tabs_habit_created), StepRules.epoch(step.createdEpochDay).format(longDate), Modifier.weight(1f))
        when (phase) {
            Phase.BUILDING -> if (step.oneOff) DateBox(stringResource(R.string.tabs_one_off), stringResource(R.string.tabs_habit_to_do), Modifier.weight(1f))
            else DateBox(stringResource(R.string.tabs_habit_daily_ticks), stringResource(R.string.tabs_habit_day_of, StepRules.dayNumber(step, s.today), StepRules.buildLength(step, s.settings)), Modifier.weight(1f))
            Phase.SETTLED -> DateBox(stringResource(R.string.tabs_check_up), CheckIns.nextQuarter(s).format(longDate), Modifier.weight(1f))
            Phase.FINISHED -> DateBox(stringResource(R.string.tabs_habit_finished), StepRules.epoch(step.finishedEpochDay!!).format(longDate), Modifier.weight(1f))
        }
    }
    if (phase == Phase.BUILDING && !step.oneOff) {
        val p = StepRules.dayNumber(step, s.today) / StepRules.buildLength(step, s.settings).toFloat()
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.empty)) {
            Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.button))
        }
    }
    if (!step.oneOff && !StepRules.rhythm(step).everyDay) Text(rhythmText(step), fontSize = 12.sp, color = c.muted)
}

@Composable
private fun DateBox(label: String, value: String, modifier: Modifier) {
    val c = Alice.colors
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(c.panel).padding(10.dp)) {
        Text(label.uppercase(), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = c.muted)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
    }
}

@Composable
fun StepFigures(step: Step, s: Snapshot) {
    val c = Alice.colors
    val st = Stats.habit(step, s)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BigStat("${st.daysGoing}", pluralStringResource(R.plurals.tabs_habit_days_going, st.daysGoing), Modifier.weight(1f))
        BigStat("${st.timesBroken}", pluralStringResource(R.plurals.tabs_habit_times_broken, st.timesBroken), Modifier.weight(1f))
        BigStat("${st.successPercent}%", stringResource(R.string.tabs_habit_success), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Triple(Icons.Filled.LocalFireDepartment, st.currentStreak, stringResource(R.string.tabs_habit_current_streak)), Triple(Icons.Filled.EmojiEvents, st.longestStreak, stringResource(R.string.tabs_habit_longest_streak))).forEach { (icon, n, label) ->
            Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(c.spot).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(24.dp), tint = c.headerText)
                Column(Modifier.padding(start = 8.dp)) {
                    Text(pluralStringResource(R.plurals.tabs_habit_days, n, n), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
                    Text(label, fontSize = 10.5.sp, color = c.muted)
                }
            }
        }
    }
}

@Composable
fun HistoryChart(step: Step, s: Snapshot, onTap: (LocalDate) -> Unit) {
    val c = Alice.colors
    var mode by rememberSaveable { mutableStateOf(Span.WEEK) }
    var back by rememberSaveable { mutableIntStateOf(0) }
    Segmented(Span.entries.map { it.label }, mode.ordinal, { mode = Span.entries[it]; back = 0 }, fontSize = 12.sp)
    val created = StepRules.epoch(step.createdEpochDay)
    if (mode != Span.ALL) {
        val title = when (mode) {
            Span.WEEK -> StepRules.weekStart(s.today, s.settings.firstDayOfWeek).minusWeeks(back.toLong()).let { "${it.format(DateTimeFormatter.ofPattern("d MMM"))} – ${it.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM"))}" }
            Span.MONTH -> YearMonth.from(s.today).minusMonths(back.toLong()).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            else -> "${s.today.year - back}"
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = { back++ }, Modifier.size(32.dp)) { Icon(chevronBack, stringResource(R.string.tabs_earlier), tint = c.muted) }
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { if (back > 0) back-- }, Modifier.size(32.dp), enabled = back > 0) { Icon(chevronForward, stringResource(R.string.tabs_later), tint = if (back > 0) c.muted else c.line) }
        }
    }
    when (mode) {
        Span.WEEK -> {
            val days = StepRules.weekDays(StepRules.weekStart(s.today, s.settings.firstDayOfWeek).minusWeeks(back.toLong()))
            Row {
                days.forEach { d ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(d.dayOfWeek.letter(), fontSize = 11.sp, color = c.muted, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        DaySquare(Grid.cell(step, d, s), size = 34.dp) { onTap(d) }
                        Spacer(Modifier.height(2.dp))
                        Text("${d.dayOfMonth}", fontSize = 10.sp, color = c.muted)
                    }
                }
            }
        }
        Span.MONTH -> {
            val ym = YearMonth.from(s.today).minusMonths(back.toLong())
            val first = StepRules.weekStart(ym.atDay(1), s.settings.firstDayOfWeek)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row { StepRules.weekDays(first).forEach { Text(it.dayOfWeek.letter(), Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp, color = c.muted, fontWeight = FontWeight.Bold) } }
                var w = first
                while (!w.isAfter(ym.atEndOfMonth())) {
                    val week = StepRules.weekDays(w)
                    Row {
                        week.forEach { d ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (YearMonth.from(d) == ym) DaySquare(Grid.cell(step, d, s), size = 30.dp) { onTap(d) }
                            }
                        }
                    }
                    w = w.plusWeeks(1)
                }
            }
        }
        Span.YEAR -> {
            val year = s.today.year - back
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (0 until 3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..4).forEach { col ->
                            val m = YearMonth.of(year, row * 4 + col)
                            val share = if (m.atEndOfMonth().isBefore(created) || m.atDay(1).isAfter(s.today)) null
                            else Stats.keptShare(step, m.atDay(1), m.atEndOfMonth(), s)
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (share == null) c.empty else c.kept.copy(alpha = 0.25f + share * 0.75f)).padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(m.month.short(), fontSize = 10.5.sp, color = c.muted)
                                Text(share?.let { "${(it * 100).roundToInt()}%" } ?: "·", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
                            }
                        }
                    }
                }
            }
        }
        Span.ALL -> {
            val months = generateSequence(YearMonth.from(created)) { it.plusMonths(1) }.takeWhile { !it.isAfter(YearMonth.from(s.today)) }.toList()
            val points = months.map { m -> Stats.keptShare(step, m.atDay(1), m.atEndOfMonth(), s) ?: 0f }
            LineChart(points, months.first().format(DateTimeFormatter.ofPattern("MMM yy")), months.last().format(DateTimeFormatter.ofPattern("MMM yy")))
        }
    }
}

@Composable
fun LineChart(points: List<Float>, startLabel: String, endLabel: String) {
    val c = Alice.colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.panel).padding(12.dp)) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            listOf(0f, 0.5f, 1f).forEach { y -> drawLine(c.line, Offset(0f, size.height * (1 - y)), Offset(size.width, size.height * (1 - y)), 1.dp.toPx()) }
            if (points.size == 1) drawCircle(c.button, 5.dp.toPx(), Offset(size.width / 2, size.height * (1 - points[0])))
            else {
                val path = Path()
                points.forEachIndexed { i, p ->
                    val o = Offset(size.width * i / (points.size - 1), size.height * (1 - p))
                    if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                drawPath(path, c.headerText, style = Stroke(3.dp.toPx()))
                points.forEachIndexed { i, p -> drawCircle(c.button, 3.5.dp.toPx(), Offset(size.width * i / (points.size - 1), size.height * (1 - p))) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(startLabel, fontSize = 10.sp, color = c.muted, modifier = Modifier.weight(1f))
            Text(endLabel, fontSize = 10.sp, color = c.muted)
        }
    }
}

@Composable
fun SlipList(step: Step, s: Snapshot, onDelete: (String) -> Unit) {
    val c = Alice.colors
    val reasons = Stats.habit(step, s).slipReasons
    if (reasons.isEmpty()) return
    SectionLabel(stringResource(R.string.tabs_habit_why_slipped))
    reasons.forEach { (r, n) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(iconFor(defaultIconFor(r)), null, Modifier.size(18.dp), tint = c.headerSub)
            Text("  $r · $n", fontSize = 13.sp, color = c.ink, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Delete, stringResource(R.string.tabs_habit_remove_reason), Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).clickable { onDelete(r) }.padding(7.dp), tint = c.muted)
        }
    }
}

