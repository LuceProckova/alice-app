package com.lucyprockova.alice.ui

import com.lucyprockova.alice.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import android.os.Bundle
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.data.Category
import com.lucyprockova.alice.data.Step
import com.lucyprockova.alice.data.Verdict
import com.lucyprockova.alice.logic.CheckIns
import com.lucyprockova.alice.logic.Snapshot
import com.lucyprockova.alice.logic.Stats
import com.lucyprockova.alice.logic.StepRules
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** one avoid, why, brands behind it, what to have instead, figures, slips, every box editable */
class AvoidItemActivity : AliceActivity() {
    companion object { const val EXTRA_ID = "stepId" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra(EXTRA_ID, -1)
        screen { s ->
            val step = s.steps.firstOrNull { it.id == id }
            if (step == null) { finish(); return@screen }
            AvoidItem(step, s)
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun AvoidItem(step: Step, s: Snapshot) {
        val c = Alice.colors
        var deleting by remember { mutableStateOf(false) }
        var editing by rememberSaveable { mutableStateOf<String?>(null) }
        var allBrands by rememberSaveable { mutableStateOf(false) }
        val save = { st: Step -> io { update(st) } }
        val fmt = DateTimeFormatter.ofPattern("d MMM yyyy")

        Column(Modifier.fillMaxSize().background(c.card)) {
            Header(step.name, back = stringResource(R.string.tabs_avoid_title), onBack = ::finish, actions = {
                Box(Modifier.clickable { save(step.copy(verdict = Verdict.entries[(step.verdict.ordinal + 1) % 3])) }) { Badge(step.verdict.verdictLabel(), step.verdict.color()) }
                IconButton(onClick = { open(NewStepActivity::class.java, NewStepActivity.EXTRA_EDIT_ID to step.id) }) { Icon(Icons.Filled.Edit, stringResource(R.string.tabs_edit), tint = c.headerText) }
                IconButton(onClick = { deleting = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.tabs_delete), tint = c.headerText) }
            })
            Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).clickable { save(step.copy(category = Category.entries[((step.category?.ordinal ?: 0) + 1) % 3])) }) {
                        InfoBox(step.category.categoryLabel().uppercase(), "", Modifier.fillMaxWidth())
                    }
                    InfoBox(stringResource(R.string.tabs_avoid_added), StepRules.epoch(step.createdEpochDay).format(fmt), Modifier.weight(1f))
                    InfoBox(stringResource(R.string.tabs_check_up), CheckIns.nextQuarter(s).format(DateTimeFormatter.ofPattern("d MMM")), Modifier.weight(1f))
                }

                EditableBox(stringResource(R.string.tabs_avoid_why), editing == "why", { editing = if (editing == "why") null else "why" }) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val shownTags = if (editing == "why") AVOID_TAGS else step.tagList
                        shownTags.forEach { t ->
                            val on = t in step.tagList
                            Pill(tagLabel(t), on, onClick = if (editing == "why") ({
                                save(step.copy(tags = (if (on) step.tagList - t else step.tagList + t).joinToString(",")))
                            }) else null)
                        }
                    }
                    if (editing == "why") TextArea(step.note, stringResource(R.string.tabs_avoid_own_words)) { save(step.copy(note = it)) }
                    else if (step.note.isNotBlank()) Text(step.note, fontSize = 13.sp, color = c.ink, modifier = Modifier.padding(top = 8.dp))
                }

                if (step.category == Category.COMPANY) {
                    EditableBox(stringResource(R.string.tabs_avoid_brands, step.brandList.size), editing == "brands", { editing = if (editing == "brands") null else "brands" }) {
                        if (editing == "brands") TextArea(step.brands, stringResource(R.string.tabs_avoid_brand_hint)) { save(step.copy(brands = it)) }
                        else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val list = if (allBrands) step.brandList else step.brandList.take(10)
                                list.forEach { Pill(it) }
                                if (!allBrands && step.brandList.size > 10) Pill(stringResource(R.string.tabs_avoid_more_brands, step.brandList.size - 10), onClick = { allBrands = true })
                            }
                        }
                    }
                }

                EditableBox(stringResource(R.string.tabs_avoid_instead), editing == "alt", { editing = if (editing == "alt") null else "alt" }) {
                    if (editing == "alt") TextArea(step.alternatives, stringResource(R.string.tabs_avoid_instead_hint)) { save(step.copy(alternatives = it)) }
                    else if (step.alternativeList.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        step.alternativeList.forEach { Pill(it) }
                    }
                }

                val st = Stats.habit(step, s)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigStat("${st.timesBroken}", pluralStringResource(R.plurals.tabs_avoid_slips, st.timesBroken), Modifier.weight(1f))
                    BigStat("${st.successPercent}%", stringResource(R.string.tabs_avoid_avoided), Modifier.weight(1f))
                    val months = ChronoUnit.MONTHS.between(StepRules.epoch(step.createdEpochDay), s.today).toInt()
                    BigStat("$months", pluralStringResource(R.plurals.tabs_avoid_months_on_list, months), Modifier.weight(1f))
                }
                SlipList(step, s) { reason -> io { clearStepReason(step, reason) } }
            }
        }
        if (deleting) ConfirmDialog(stringResource(R.string.tabs_delete_title, step.name), stringResource(R.string.tabs_avoid_delete_text), stringResource(R.string.tabs_delete), { io { delete(step) }; finish() }) { deleting = false }
    }

    @Composable
    private fun InfoBox(label: String, value: String, modifier: Modifier) {
        val c = Alice.colors
        Column(modifier.clip(RoundedCornerShape(12.dp)).background(c.panel).padding(10.dp)) {
            Text(label.uppercase(), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = c.muted)
            if (value.isNotEmpty()) Text(value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
        }
    }

    @Composable
    private fun EditableBox(title: String, editing: Boolean, onEdit: () -> Unit, content: @Composable () -> Unit) {
        val c = Alice.colors
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = c.ink, modifier = Modifier.weight(1f))
                Icon(if (editing) Icons.Filled.Check else Icons.Filled.Edit, stringResource(if (editing) R.string.tabs_done else R.string.tabs_edit),
                    Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onEdit).padding(6.dp), tint = c.headerSub)
            }
            Spacer(Modifier.width(4.dp))
            content()
        }
    }

    @Composable
    private fun TextArea(value: String, hint: String, onChange: (String) -> Unit) {
        val c = Alice.colors
        // own local copy so cursor doesn't jump while db write round trips
        var text by remember { mutableStateOf(value) }
        OutlinedTextField(
            text, { text = it; onChange(it) }, Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 3,
            placeholder = { Text(hint, color = c.muted) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.button, unfocusedBorderColor = c.line, focusedTextColor = c.ink, unfocusedTextColor = c.ink),
        )
    }
}
