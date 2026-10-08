package com.lucyprockova.alice.ui

import com.lucyprockova.alice.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.data.Step
import com.lucyprockova.alice.data.StepKind
import com.lucyprockova.alice.logic.Phase
import com.lucyprockova.alice.logic.Snapshot
import com.lucyprockova.alice.logic.StepRules
import java.time.format.DateTimeFormatter

/** changes tab, ongoing swaps, one-offs to do, finished one-offs */
@Composable
fun AliceActivity.ChangesTab(s: Snapshot) {
    val c = Alice.colors
    var finishedOpen by rememberSaveable { mutableStateOf(false) }
    val swaps = s.steps.filter { it.kind == StepKind.SWAP && !it.oneOff }
    val todo = s.steps.filter { it.oneOff && it.finishedEpochDay == null }
    val done = s.steps.filter { it.oneOff && it.finishedEpochDay != null }.sortedByDescending { it.finishedEpochDay }
    Column(Modifier.fillMaxSize().background(c.card)) {
        stringResource(R.string.core_tab_changes).let { t -> Header(t, actions = { SettingsButton(t) }) }
        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                item {
                    SectionLabel(stringResource(R.string.tabs_changes_ongoing)) {
                        InfoTip(stringResource(R.string.tabs_changes_ongoing_tip_title), pluralStringResource(R.plurals.tabs_changes_ongoing_tip, s.settings.daysToBuild, s.settings.daysToBuild), c.muted)
                    }
                }
                if (swaps.isEmpty()) item { Empty(stringResource(R.string.tabs_changes_no_swaps)) }
                items(swaps, key = { it.id }) { step ->
                    val building = StepRules.phase(step, s.settings, s.today) == Phase.BUILDING
                    val badge = if (building) stringResource(R.string.tabs_changes_day_badge, StepRules.dayNumber(step, s.today), StepRules.buildLength(step, s.settings))
                        else stringResource(R.string.tabs_changes_sticking)
                    ChangeRow(step, badge, badgeOn = !building)
                }
                item {
                    SectionLabel(stringResource(R.string.tabs_changes_one_off_todo)) {
                        InfoTip(stringResource(R.string.tabs_one_off), stringResource(R.string.tabs_changes_one_off_tip), c.muted)
                    }
                }
                if (todo.isEmpty()) item { Empty(stringResource(R.string.tabs_changes_nothing_waiting)) }
                items(todo, key = { it.id }) { step ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(26.dp).clip(CircleShape).border(2.dp, c.kept, CircleShape).clickable { io { setCount(step, s.today, 1) } },
                        )
                        Text(step.name, Modifier.weight(1f).clickable { open(HabitActivity::class.java, HabitActivity.EXTRA_ID to step.id) }.padding(horizontal = 10.dp, vertical = 8.dp), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().clickable { finishedOpen = !finishedOpen }.padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.tabs_changes_finished, done.size), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = c.muted)
                        Icon(if (finishedOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, Modifier.size(18.dp), tint = c.muted)
                    }
                }
                if (finishedOpen) items(done, key = { "f" + it.id }) { step ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("✓", color = c.kept, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text(step.name, Modifier.weight(1f).padding(start = 10.dp), fontSize = 13.sp, color = c.ink)
                        Text(StepRules.epoch(step.finishedEpochDay!!).format(DateTimeFormatter.ofPattern("d MMM")), fontSize = 11.5.sp, color = c.muted)
                    }
                }
            }
            FloatingActionButton(
                onClick = { open(NewStepActivity::class.java) }, containerColor = c.button, contentColor = c.buttonText,
                modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
            ) { Icon(Icons.Filled.Add, stringResource(R.string.tabs_new_step)) }
        }
    }
}

@Composable
private fun AliceActivity.ChangeRow(step: Step, badge: String, badgeOn: Boolean) {
    val c = Alice.colors
    Column {
        Row(
            Modifier.fillMaxWidth().clickable { open(HabitActivity::class.java, HabitActivity.EXTRA_ID to step.id) }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(step.name, Modifier.weight(1f), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
            Spacer(Modifier.width(8.dp))
            Badge(badge, if (badgeOn) c.kept else c.spot)
        }
        HorizontalDivider(color = c.line)
    }
}

@Composable
fun Badge(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp, color = Alice.colors.ink.copy(alpha = 0.85f),
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color).padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
fun Empty(text: String) {
    Text(text, fontSize = 12.5.sp, color = Alice.colors.muted, modifier = Modifier.padding(vertical = 6.dp))
}
