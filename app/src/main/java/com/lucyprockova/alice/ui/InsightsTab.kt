package com.lucyprockova.alice.ui

import com.lucyprockova.alice.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.data.StepKind
import com.lucyprockova.alice.logic.CheckIns
import com.lucyprockova.alice.logic.Snapshot
import com.lucyprockova.alice.logic.Stats
import com.lucyprockova.alice.logic.StepRules
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** insights, what all steps add up to, per week, month, year or all time */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AliceActivity.InsightsTab(s: Snapshot) {
    val c = Alice.colors
    var mode by rememberSaveable { mutableStateOf(Span.MONTH) }
    var back by rememberSaveable { mutableIntStateOf(0) }
    val first = s.steps.minOfOrNull { it.createdEpochDay }?.let(StepRules::epoch) ?: s.today
    val (from, to, title) = range(mode, back, s, first)
    val p = Stats.period(from, to, s)
    val live = s.steps.filter { !it.oneOff || it.finishedEpochDay != null }

    Column(Modifier.fillMaxSize().background(c.card)) {
        stringResource(R.string.core_tab_insights).let { t -> Header(t, actions = { SettingsButton(t) }) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.spot).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${live.size}", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = c.headerText)
                Text(pluralStringResource(R.plurals.tabs_insights_ways, live.size), fontSize = 13.sp, color = c.muted)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigStat("${live.count { it.kind == StepKind.HABIT }}", stringResource(R.string.tabs_insights_habits), Modifier.weight(1f))
                    BigStat("${live.count { it.kind == StepKind.SWAP }}", stringResource(R.string.tabs_insights_swaps), Modifier.weight(1f))
                    BigStat("${live.count { it.kind == StepKind.AVOID }}", stringResource(R.string.tabs_insights_avoids), Modifier.weight(1f))
                }
            }

            SectionLabel(stringResource(R.string.tabs_insights_on_track_section))
            Segmented(Span.entries.map { it.label }, mode.ordinal, { mode = Span.entries[it]; back = 0 }, fontSize = 12.sp)
            if (mode != Span.ALL) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = { back++ }, Modifier.size(32.dp)) { Icon(chevronBack, stringResource(R.string.tabs_earlier), tint = c.muted) }
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = { if (back > 0) back-- }, Modifier.size(32.dp), enabled = back > 0) { Icon(chevronForward, stringResource(R.string.tabs_later), tint = if (back > 0) c.muted else c.line) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigStat(stringResource(R.string.tabs_insights_avg_days, p.averageDaysGoing), stringResource(R.string.tabs_insights_average_kept), Modifier.weight(1f))
                BigStat("${p.daysCheckedIn}/${p.days}", stringResource(R.string.tabs_insights_days_checked_in), Modifier.weight(1f))
                BigStat("${p.onTrackPercent}%", stringResource(R.string.tabs_insights_on_track), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigStat("${p.slips}", pluralStringResource(R.plurals.tabs_insights_slips, p.slips), Modifier.weight(1f))
                BigStat("+${p.newSteps}", pluralStringResource(R.plurals.tabs_insights_new_steps, p.newSteps), Modifier.weight(1f))
                BigStat("${p.oneOffsDone}", pluralStringResource(R.plurals.tabs_insights_one_offs_done, p.oneOffsDone), Modifier.weight(1f))
            }

            if (p.slipReasons.isNotEmpty()) {
                SectionLabel(stringResource(R.string.tabs_insights_slip_reasons))
                val max = p.slipReasons.first().second
                p.slipReasons.take(5).forEach { (r, n) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(iconFor(defaultIconFor(r)), null, Modifier.size(18.dp), tint = c.headerSub)
                        Text(r, fontSize = 12.5.sp, color = c.ink, modifier = Modifier.width(110.dp).padding(start = 6.dp))
                        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(c.empty)) {
                            Box(Modifier.fillMaxWidth(n / max.toFloat()).height(10.dp).clip(RoundedCornerShape(5.dp)).background(c.missed))
                        }
                        Text("$n", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    }
                }
            }

            val qFrom = CheckIns.previousQuarter(s, CheckIns.nextQuarter(s, s.today.plusDays(1)))
            val added = s.steps.filter { it.createdEpochDay >= qFrom.toEpochDay() }
            if (added.isNotEmpty()) {
                SectionLabel(stringResource(R.string.tabs_insights_added_quarter))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    added.forEach { Pill(it.name, icon = it.kind.icon()) }
                }
            }

            SectionLabel(stringResource(R.string.tabs_insights_past_summaries))
            if (s.checkups.isEmpty()) Empty(stringResource(R.string.tabs_insights_first_on, CheckIns.nextQuarter(s).format(DateTimeFormatter.ofPattern("d MMM yyyy"))))
            s.checkups.sortedByDescending { it.dueEpochDay }.forEach { cu ->
                Column {
                    Row(
                        Modifier.fillMaxWidth().clickable { open(SummaryActivity::class.java, SummaryActivity.EXTRA_DUE to cu.dueEpochDay) }.padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(periodLabel(StepRules.epoch(cu.fromEpochDay), StepRules.epoch(cu.dueEpochDay).minusDays(1)), Modifier.weight(1f), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Icon(chevronForward, null, tint = c.muted)
                    }
                    HorizontalDivider(color = c.line)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

private fun AliceActivity.range(mode: Span, back: Int, s: Snapshot, first: LocalDate): Triple<LocalDate, LocalDate, String> = when (mode) {
    Span.WEEK -> {
        val start = StepRules.weekStart(s.today, s.settings.firstDayOfWeek).minusWeeks(back.toLong())
        Triple(start, start.plusDays(6), "${start.format(DateTimeFormatter.ofPattern("d MMM"))} – ${start.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM"))}")
    }
    Span.MONTH -> {
        val m = YearMonth.from(s.today).minusMonths(back.toLong())
        Triple(m.atDay(1), m.atEndOfMonth(), m.format(DateTimeFormatter.ofPattern("MMMM yyyy")))
    }
    Span.YEAR -> {
        val y = s.today.year - back
        Triple(LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31), "$y")
    }
    Span.ALL -> Triple(first, s.today, getString(R.string.tabs_insights_all_time))
}

@Composable
fun periodLabel(from: LocalDate, to: LocalDate): String {
    val m = DateTimeFormatter.ofPattern("MMM")
    return stringResource(R.string.tabs_insights_period, from.format(m), to.format(m), to.year)
}
