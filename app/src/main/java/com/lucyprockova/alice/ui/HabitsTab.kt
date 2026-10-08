package com.lucyprockova.alice.ui

import com.lucyprockova.alice.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.data.Step
import com.lucyprockova.alice.data.StepKind
import com.lucyprockova.alice.data.StepTiming
import com.lucyprockova.alice.logic.ButtonState
import com.lucyprockova.alice.logic.CellState
import com.lucyprockova.alice.logic.CheckInKind
import com.lucyprockova.alice.logic.CheckIns
import com.lucyprockova.alice.logic.Grid
import com.lucyprockova.alice.logic.Phase
import com.lucyprockova.alice.logic.Snapshot
import com.lucyprockova.alice.logic.Stats
import com.lucyprockova.alice.logic.StepRules
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** start screen, week's habits grid + checkin button */
@Composable
fun AliceActivity.HabitsTab(s: Snapshot) {
    val c = Alice.colors
    var weekOffset by rememberSaveable { mutableLongStateOf(0L) }
    val weekStart = StepRules.weekStart(s.today, s.settings.firstDayOfWeek).minusWeeks(weekOffset)
    val days = StepRules.weekDays(weekStart)

    // building, finished today, or settled during a past week shown, never before created
    // remembered on steps, settings, day and week, so ticking a square doesn't redo the sorting
    val visible = remember(s.steps, s.settings, s.today, weekOffset) {
        s.steps.filter {
            val ph = StepRules.phase(it, s.settings, s.today)
            ph == Phase.BUILDING || StepRules.finishedToday(it, s.today) ||
                (ph == Phase.SETTLED && weekOffset > 0 && StepRules.buildEnd(it, s.settings).isAfter(weekStart))
        }.filter { !StepRules.epoch(it.createdEpochDay).isAfter(days.last()) }
    }
    val settled = remember(visible) { s.steps.filter { StepRules.phase(it, s.settings, s.today) == Phase.SETTLED && it !in visible } }
    val newest = remember(visible) { visible.filter { !it.oneOff && StepRules.dayNumber(it, s.today) <= 7 }.maxWithOrNull(compareBy({ it.buildStartEpochDay }, { it.id })) }
    val anytime = remember(visible) { visible.filter { it.timing == StepTiming.ANYTIME } }
    val endOfDay = remember(visible) { visible.filter { it.timing == StepTiming.END_OF_DAY } }

    Column(Modifier.fillMaxSize().background(c.card)) {
        Header(
            title = stringResource(R.string.app_name),
            actions = {
                CheckInButton(CheckIns.button(s))
                SettingsButton(stringResource(R.string.core_tab_habits))
            },
            below = { WeekBar(days, weekOffset == 0L, { weekOffset++ }, { if (weekOffset > 0) weekOffset-- }) },
        )
        Box(Modifier.weight(1f)) {
            if (s.steps.isNotEmpty()) GridList(s, days, anytime, endOfDay, settled, newest)
            FloatingActionButton(
                onClick = { open(NewStepActivity::class.java) },
                containerColor = c.button, contentColor = c.buttonText,
                modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
            ) { Icon(Icons.Filled.Add, stringResource(R.string.tabs_new_step)) }
        }
    }
    Popups(s)
}

@Composable
private fun AliceActivity.CheckInButton(state: ButtonState) {
    val c = Alice.colors
    val (bg, fg) = when (state) {
        is ButtonState.Early -> c.empty to c.muted
        is ButtonState.Due -> c.button to c.buttonText
        ButtonState.Done -> c.kept to c.keptText
    }
    val (icon, text) = when (state) {
        is ButtonState.Early -> Icons.Filled.Schedule to stringResource(R.string.tabs_checkin_from, state.hour)
        is ButtonState.Due -> when (state.kind) {
            CheckInKind.DAILY -> Icons.AutoMirrored.Filled.EventNote to stringResource(R.string.tabs_checkin_daily)
            CheckInKind.WEEKLY -> Icons.Filled.EventAvailable to stringResource(R.string.tabs_checkin_weekly)
            CheckInKind.QUARTER -> Icons.Filled.EventRepeat to stringResource(R.string.tabs_checkin_quarter)
        }
        ButtonState.Done -> Icons.Filled.Check to stringResource(R.string.tabs_checked_in)
    }
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(bg)
            .clickable(enabled = state is ButtonState.Due) { open(CheckInActivity::class.java) }
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = fg)
        Spacer(Modifier.width(5.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
        if (state is ButtonState.Due && state.count > 0) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(18.dp).clip(CircleShape).background(c.headerText), contentAlignment = Alignment.Center) {
                Text("${state.count}", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = c.header)
            }
        }
    }
}

@Composable
private fun AliceActivity.GridList(s: Snapshot, days: List<LocalDate>, anytime: List<Step>, endOfDay: List<Step>, settled: List<Step>, newest: Step?) {
    val c = Alice.colors
    var settledOpen by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
                Spacer(Modifier.weight(1f))
                days.forEach { day ->
                    Text(
                        day.dayOfWeek.letter(), Modifier.width(DAY_CELL), textAlign = TextAlign.Center, fontSize = 11.sp,
                        fontWeight = if (day == s.today) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (day == s.today) c.ink else c.muted,
                    )
                }
            }
        }
        items(anytime, key = { it.id }) { GridLine(s, it, days, it == newest) }
        if (anytime.isNotEmpty() && endOfDay.isNotEmpty()) item { HorizontalDivider(thickness = 2.dp, color = c.divider) }
        items(endOfDay, key = { it.id }) { GridLine(s, it, days, it == newest) }
        if (settled.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth().clickable { settledOpen = !settledOpen }.padding(top = 14.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (settledOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, Modifier.size(18.dp), tint = c.muted)
                    Text(stringResource(R.string.tabs_quarter_section), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = c.muted, modifier = Modifier.padding(start = 4.dp))
                    Text(" · ${settled.size}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = c.muted)
                }
            }
            if (settledOpen) {
                items(settled, key = { "s" + it.id }) { step ->
                    Row(
                        Modifier.fillMaxWidth().clickable { openStep(step) }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GridName(step.name, c.settledText, FontWeight.SemiBold, Modifier.weight(1f).padding(start = 6.dp, end = NAME_GAP))
                        days.forEach { _ ->
                            Box(Modifier.width(DAY_CELL), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)).background(c.settled))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun AliceActivity.tapCell(step: Step, cell: com.lucyprockova.alice.logic.Cell, s: Snapshot) {
    if (cell.state == CellState.LOCKED) {
        android.widget.Toast.makeText(this, getString(R.string.tabs_eod_unlock, s.settings.checkInHour), android.widget.Toast.LENGTH_SHORT).show()
    } else io { tap(step, cell.day, s) }
}

private fun AliceActivity.openStep(step: Step) {
    if (step.kind == StepKind.AVOID) open(AvoidItemActivity::class.java, AvoidItemActivity.EXTRA_ID to step.id)
    else open(HabitActivity::class.java, HabitActivity.EXTRA_ID to step.id)
}

@Composable
private fun AliceActivity.GridName(name: String, color: Color, weight: FontWeight, modifier: Modifier) {
    Text(name, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = weight, color = color, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

@Composable
private fun AliceActivity.GridLine(s: Snapshot, step: Step, days: List<LocalDate>, isNew: Boolean) {
    val c = Alice.colors
    val cells = days.map { Grid.cell(step, it, s) }
    var info by remember { mutableStateOf(false) }
    val rowBg = if (isNew) Modifier.clip(RoundedCornerShape(8.dp)).background(c.spot) else Modifier
    Column {
        Row(Modifier.fillMaxWidth().then(rowBg).padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clickable { openStep(step) }.padding(start = 6.dp, end = NAME_GAP),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when {
                    isNew -> Icon(Icons.Filled.FiberNew, stringResource(R.string.tabs_new_step), Modifier.size(16.dp), tint = c.headerText)
                    step.kind != StepKind.HABIT -> Icon(step.kind.icon(), step.kind.label(), Modifier.size(14.dp), tint = c.muted)
                }
                GridName(step.name, c.ink, FontWeight.Bold, Modifier.weight(1f, fill = false))
                if (step.oneOff || (!StepRules.rhythm(step).everyDay && step.timing == StepTiming.ANYTIME)) {
                    Icon(if (step.oneOff) Icons.Filled.TaskAlt else Icons.Filled.Repeat, rhythmText(step),
                        Modifier.size(15.dp).clip(CircleShape).clickable { info = true }, tint = c.muted)
                }
            }
            cells.forEachIndexed { i, cell ->
                Box(Modifier.width(DAY_CELL).height(22.dp), contentAlignment = Alignment.Center) {
                    if (cell.state == CellState.BAND) {
                        // band neighbour on a side = strip runs flat into it, else rounded end
                        val leftOpen = cells.getOrNull(i - 1)?.state == CellState.BAND
                        val rightOpen = cells.getOrNull(i + 1)?.state == CellState.BAND
                        BandPiece(leftOpen, rightOpen)
                    }
                    DaySquare(cell) { tapCell(step, cell, s) }
                }
            }
        }
        if (!isNew) HorizontalDivider(color = c.line)
    }
    if (info) AlertDialog(
        onDismissRequest = { info = false },
        confirmButton = { TextButton(onClick = { info = false }) { Text(stringResource(R.string.core_ok)) } },
        title = { Text(step.name) }, text = { Text(rhythmText(step)) },
    )
}

/** green strip behind completed period of repeating step, rounded at both ends */
@Composable
private fun AliceActivity.BandPiece(leftOpen: Boolean, rightOpen: Boolean) {
    val color = Alice.colors.kept
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        val r = size.height / 2
        val l = if (leftOpen) 0f else 3.dp.toPx()
        val rr = if (rightOpen) size.width else size.width - 3.dp.toPx()
        drawRoundRect(color, Offset(l, 0f), Size(rr - l, size.height), androidx.compose.ui.geometry.CornerRadius(r, r))
        // square off the rounded corner on open sides, no notch between cells
        if (leftOpen) drawRect(color, Offset(0f, 0f), Size(r * 2, size.height))
        if (rightOpen) drawRect(color, Offset(size.width - r * 2, 0f), Size(r * 2, size.height))
    }
}

@Composable
private fun AliceActivity.Popups(s: Snapshot) {
    val c = Alice.colors
    val celebrate = s.steps.firstOrNull {
        !it.oneOff && it.buildDays == null && it.id !in s.settings.celebrated && StepRules.phase(it, s.settings, s.today) == Phase.SETTLED &&
            !StepRules.buildEnd(it, s.settings).isBefore(s.today.minusDays(14))
    }
    val lastWeekly = s.weekly.maxByOrNull { it.dueEpochDay }
    when {
        celebrate != null -> {
            val next = CheckIns.nextQuarter(s)
            PopupCard(
                icon = { Icon(Icons.Filled.Celebration, null, Modifier.size(28.dp), tint = c.headerText) },
                title = StepRules.buildLength(celebrate, s.settings).let { n -> pluralStringResource(R.plurals.tabs_celebrate_title, n, n, celebrate.name) },
                onOk = { repo.settingsStore.update { it.copy(celebrated = it.celebrated + celebrate.id) } },
            ) {
                Text(stringResource(R.string.tabs_celebrate_text), fontSize = 13.sp, color = c.muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                PixelPicture(PIXEL_SCENES[(celebrate.id % PIXEL_SCENES.size).toInt()], Modifier.width(170.dp))
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.tabs_celebrate_first, next.format(DateTimeFormatter.ofPattern("d MMM"))), fontSize = 12.5.sp, color = c.muted)
            }
        }
        lastWeekly != null && lastWeekly.doneEpochDay > s.settings.glanceShownFor && lastWeekly.doneEpochDay == s.today.toEpochDay() -> {
            val ws = StepRules.weekStart(StepRules.epoch(lastWeekly.dueEpochDay), s.settings.firstDayOfWeek)
            val g = Stats.glance(ws, s)
            PopupCard(
                icon = { Icon(Icons.Filled.Insights, null, Modifier.size(28.dp), tint = c.headerText) },
                title = stringResource(R.string.tabs_glance_title),
                onOk = { repo.settingsStore.update { it.copy(glanceShownFor = lastWeekly.doneEpochDay) } },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigStat("${g.keptPercent}%", stringResource(R.string.tabs_glance_ticks_kept), Modifier.weight(1f))
                    BigStat("${g.daysCheckedIn}/7", stringResource(R.string.tabs_glance_days_checked_in), Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                g.topReason?.let { GlanceLine(stringResource(R.string.tabs_glance_top_tag), pluralStringResource(R.plurals.tabs_glance_tag_times, it.second, it.first, it.second)) }
                g.easiest?.let { GlanceLine(stringResource(R.string.tabs_glance_easiest), glanceStepText(it)) }
                g.hardest?.let { GlanceLine(stringResource(R.string.tabs_glance_hardest), glanceStepText(it)) }
            }
        }
    }
}

private val DAY_CELL = 30.dp

private val NAME_GAP = 10.dp

@Composable
private fun WeekBar(days: List<LocalDate>, isCurrent: Boolean, onBack: () -> Unit, onForward: () -> Unit) {
    val c = Alice.colors
    val fmt = DateTimeFormatter.ofPattern("d MMM")
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) { Icon(chevronBack, stringResource(R.string.tabs_previous_week), tint = c.headerSub) }
        Text("${days.first().format(fmt)} – ${days.last().format(fmt)}", fontSize = 12.sp, color = c.headerSub, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onForward, enabled = !isCurrent, modifier = Modifier.size(28.dp)) {
            Icon(chevronForward, stringResource(R.string.tabs_next_week), tint = if (isCurrent) c.line else c.headerSub)
        }
    }
}

@Composable
fun GlanceLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, fontSize = 12.5.sp, color = Alice.colors.muted, modifier = Modifier.width(140.dp))
        Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Alice.colors.ink)
    }
}

@Composable
fun BigStat(value: String, label: String, modifier: Modifier = Modifier) {
    val c = Alice.colors
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(c.panel).padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = c.headerText)
        Text(label, fontSize = 10.5.sp, color = c.muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun PopupCard(icon: @Composable () -> Unit, title: String, onOk: () -> Unit, okText: String = stringResource(R.string.core_ok), content: @Composable () -> Unit) {
    val c = Alice.colors
    AlertDialog(
        onDismissRequest = {},
        containerColor = c.card,
        icon = icon,
        title = { Text(title, textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold, color = c.ink) },
        text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { content() } },
        confirmButton = { MainButton(okText, onClick = onOk) },
    )
}

@Composable
fun PixelPicture(scene: PixelScene, modifier: Modifier = Modifier) {
    val w = scene.rows.maxOf { it.length }
    val h = scene.rows.size
    Canvas(modifier.aspectRatio(w / h.toFloat())) {
        val px = size.width / w
        scene.rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                scene.palette[ch]?.let { drawRect(Color(it), Offset(x * px, y * px), Size(px + 0.5f, px + 0.5f)) }
            }
        }
    }
}
