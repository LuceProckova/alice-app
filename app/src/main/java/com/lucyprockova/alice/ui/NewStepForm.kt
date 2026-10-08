package com.lucyprockova.alice.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.data.AliceSettings
import com.lucyprockova.alice.data.Category
import com.lucyprockova.alice.data.CompanyBrands
import com.lucyprockova.alice.data.IdeaGroup
import com.lucyprockova.alice.data.Ideas
import com.lucyprockova.alice.data.PeriodUnit
import com.lucyprockova.alice.data.RepeatMode
import com.lucyprockova.alice.data.SettingsStore
import com.lucyprockova.alice.data.Step
import com.lucyprockova.alice.data.StepKind
import com.lucyprockova.alice.data.StepTiming
import com.lucyprockova.alice.logic.Schedule
import java.time.DayOfWeek
import java.time.LocalDate

enum class Often { SPECIFIC, WHENEVER, ONCE }

/** form state so far, becomes [Step] on save, or built from one to edit, serializable so it survives app kill */
data class StepDraft(
    val kind: StepKind = StepKind.HABIT,
    val name: String = "",
    val timing: StepTiming = StepTiming.ANYTIME,
    val often: Often = Often.SPECIFIC,
    val dayCounts: List<Int> = List(7) { 1 },
    val times: Int = 1,
    /** 0 day, 1 week, 2 month, 3 every x days */
    val unit: Int = 1,
    val everyDays: Int = 3,
    val category: Category = Category.PRODUCT,
) : java.io.Serializable {
    val valid: Boolean get() = name.isNotBlank() && (often != Often.SPECIFIC || timing != StepTiming.ANYTIME || dayCounts.any { it > 0 })

    /** form into [Step], simplest repeat mode that fits */
    fun toStep(today: LocalDate): Step {
        val base = Step(
            name = name.trim(), kind = kind, timing = timing, createdEpochDay = today.toEpochDay(),
            category = if (kind == StepKind.HABIT) null else category,
            brands = if (kind == StepKind.AVOID && category == Category.COMPANY) CompanyBrands.brandsOf(name).joinToString("\n") else "",
        )
        if (timing == StepTiming.END_OF_DAY) return base
        return when (often) {
            Often.ONCE -> base.copy(oneOff = true)
            // all 1s = plain daily, all same = daily n times, mixed = per weekday list
            Often.SPECIFIC ->
                if (dayCounts.all { it == 1 }) base
                else if (dayCounts.all { it == dayCounts[0] }) base.copy(timesPerDay = dayCounts[0])
                else base.copy(repeat = RepeatMode.DAYS, dayCounts = dayCounts.joinToString(","))
            Often.WHENEVER -> when (unit) {
                0 -> base.copy(timesPerDay = times)
                1 -> base.copy(repeat = RepeatMode.TIMES, times = times, unit = PeriodUnit.WEEK, everyN = 1)
                2 -> base.copy(repeat = RepeatMode.TIMES, times = times, unit = PeriodUnit.MONTH, everyN = 1)
                else -> base.copy(repeat = RepeatMode.TIMES, times = times, unit = PeriodUnit.DAYS, everyN = everyDays)
            }
        }
    }

    /** [old] plus form changes, how-often untouched keeps [old]'s exact schedule so every 2 weeks survives rename */
    fun editOf(old: Step, today: LocalDate): Step {
        val start = from(old)
        val untouched = copy(name = start.name, kind = start.kind, category = start.category) == start
        val step = toStep(today)
        return if (untouched) Schedule.of(old).applyTo(step) else step
    }

    companion object {
        /** form prefilled from [step], reverse of [toStep] */
        fun from(step: Step): StepDraft {
            val base = StepDraft(kind = step.kind, name = step.name, timing = step.timing, category = step.category ?: Category.PRODUCT)
            if (step.timing == StepTiming.END_OF_DAY) return base
            if (step.oneOff) return base.copy(often = Often.ONCE)
            return when (step.repeat) {
                RepeatMode.DAILY -> base.copy(dayCounts = List(7) { step.timesPerDay.coerceAtLeast(1) })
                RepeatMode.DAYS -> base.copy(dayCounts = step.dayCountList)
                RepeatMode.TIMES -> {
                    val whenever = base.copy(often = Often.WHENEVER, times = step.times)
                    when {
                        step.unit == PeriodUnit.WEEK && step.everyN == 1 -> whenever.copy(unit = 1)
                        step.unit == PeriodUnit.MONTH -> whenever.copy(unit = 2)
                        // form has no every n weeks, shown as days instead
                        step.unit == PeriodUnit.WEEK -> whenever.copy(unit = 3, everyDays = step.everyN * 7)
                        else -> whenever.copy(unit = 3, everyDays = step.everyN)
                    }
                }
            }
        }
    }
}

/** new step form for + button and weekly check-in, [editing] hides ideas */
@Composable
fun NewStepForm(draft: StepDraft, settingsStore: SettingsStore, settings: AliceSettings, editing: Boolean = false, onChange: (StepDraft) -> Unit) {
    val c = Alice.colors
    val ctx = LocalContext.current
    val ideasLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringExtra(IdeasActivity.RESULT_NAME)?.let { onChange(draft.copy(name = it)) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel(stringResource(R.string.forms_what_kind))
        Segmented(StepKind.entries.map { it.label() }, draft.kind.ordinal, { onChange(draft.copy(kind = StepKind.entries[it], timing = if (it == 0) draft.timing else StepTiming.END_OF_DAY)) })

        SectionLabel(stringResource(if (editing) R.string.forms_name else R.string.forms_your_new_step))
        OutlinedTextField(
            draft.name, { onChange(draft.copy(name = it)) }, Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(when (draft.kind) { StepKind.HABIT -> R.string.forms_hint_habit; StepKind.SWAP -> R.string.forms_hint_swap; StepKind.AVOID -> R.string.forms_hint_avoid }), color = c.muted) },
            singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.button, unfocusedBorderColor = c.line, focusedTextColor = c.ink, unfocusedTextColor = c.ink, cursorColor = c.ink),
        )

        if (draft.kind == StepKind.HABIT) {
            SectionLabel(stringResource(R.string.forms_habit_kind)) {
                InfoTip(
                    stringResource(R.string.forms_timing_tip_title),
                    gString(R.string.forms_timing_tip),
                    tint = c.muted,
                )
            }
            Segmented(listOf(stringResource(R.string.forms_anytime), stringResource(R.string.forms_end_of_day)), draft.timing.ordinal, { onChange(draft.copy(timing = StepTiming.entries[it])) })
            if (draft.timing == StepTiming.ANYTIME) OftenPicker(draft, onChange)
        } else {
            SectionLabel(stringResource(R.string.forms_what_is_it))
            Segmented(Category.entries.map { it.categoryLabel() }, draft.category.ordinal, { onChange(draft.copy(category = Category.entries[it])) })
        }

        if (!editing) IdeasBox(draft, settingsStore, settings, onPick = { onChange(draft.copy(name = it)) }) {
            ideasLauncher.launch(
                Intent(ctx, IdeasActivity::class.java).putExtra(IdeasActivity.EXTRA_GROUP, Ideas.groupFor(draft.kind, draft.timing, draft.category).name),
            )
        }
    }
}

@Composable
private fun OftenPicker(draft: StepDraft, onChange: (StepDraft) -> Unit) {
    val c = Alice.colors
    SectionLabel(stringResource(R.string.forms_how_often))
    Segmented(listOf(stringResource(R.string.forms_often_specific), stringResource(R.string.forms_often_whenever), stringResource(R.string.forms_often_once)), draft.often.ordinal, { onChange(draft.copy(often = Often.entries[it])) }, fontSize = 12.sp)
    Spacer(Modifier.height(6.dp))
    when (draft.often) {
        Often.SPECIFIC -> {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                (1..7).map { DayOfWeek.of(it).letter() }.forEachIndexed { i, d ->
                    val n = draft.dayCounts[i]
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (n > 0) c.button else c.empty),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(d, Modifier.fillMaxWidth().clickable {
                            onChange(draft.copy(dayCounts = draft.dayCounts.toMutableList().also { it[i] = if (n > 0) 0 else 1 }))
                        }.padding(vertical = 8.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = if (n > 0) c.buttonText else c.muted)
                        Text(if (n > 0) stringResource(R.string.forms_day_count, n) else " ", Modifier.fillMaxWidth().clickable(enabled = n > 0) {
                            onChange(draft.copy(dayCounts = draft.dayCounts.toMutableList().also { it[i] = if (n >= 5) 1 else n + 1 }))
                        }.padding(bottom = 6.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = if (n > 0) c.buttonText else c.muted)
                    }
                }
            }
        }
        Often.WHENEVER -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stepper(draft.times, pluralStringResource(R.plurals.forms_times_every, draft.times), 1, 20) { onChange(draft.copy(times = it)) }
            }
            Spacer(Modifier.height(6.dp))
            Segmented(listOf(stringResource(R.string.forms_unit_day), stringResource(R.string.forms_unit_week), stringResource(R.string.forms_unit_month), stringResource(R.string.forms_unit_x_days)), draft.unit, { onChange(draft.copy(unit = it)) }, fontSize = 12.sp)
            if (draft.unit == 3) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Text(stringResource(R.string.forms_every), fontSize = 13.sp, color = c.ink, modifier = Modifier.padding(end = 8.dp))
                    Stepper(draft.everyDays, pluralStringResource(R.plurals.forms_unit_days_n, draft.everyDays), 2, 60) { onChange(draft.copy(everyDays = it)) }
                }
            }
        }
        Often.ONCE -> {}
    }
}

@Composable
private fun IdeasBox(draft: StepDraft, settingsStore: SettingsStore, settings: AliceSettings, onPick: (String) -> Unit, onSeeAll: () -> Unit) {
    val c = Alice.colors
    val ctx = LocalContext.current
    val group: IdeaGroup = Ideas.groupFor(draft.kind, draft.timing, draft.category)
    var seed by remember { mutableIntStateOf(0) }
    var whyOpen by remember { mutableStateOf<String?>(null) }
    val pool = remember(group, settings.hiddenIdeas) { Ideas.all(ctx).filter { it.group == group && it.name !in settings.hiddenIdeas } }
    val shown = remember(pool, seed) { pool.shuffled(kotlin.random.Random(seed * 31 + group.ordinal)).take(5) }

    SectionLabel(stringResource(R.string.forms_ideas)) {
        Spacer(Modifier.weight(1f))
        Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable { seed++ }.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Shuffle, null, Modifier.size(15.dp), tint = c.muted)
            Text(" " + stringResource(R.string.forms_ideas_others), fontSize = 11.5.sp, color = c.muted, fontWeight = FontWeight.SemiBold)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        shown.forEach { idea ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.panel).border(1.dp, c.line, RoundedCornerShape(12.dp))) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(idea.name, Modifier.weight(1f).clickable { onPick(idea.name) }.padding(horizontal = 12.dp, vertical = 10.dp), fontSize = 13.sp, color = c.ink, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Filled.Visibility, stringResource(R.string.forms_why), Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).clickable { whyOpen = if (whyOpen == idea.name) null else idea.name }.padding(8.dp), tint = c.muted)
                    Icon(Icons.Filled.Close, stringResource(R.string.forms_never_show), Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).clickable {
                        settingsStore.update { it.copy(hiddenIdeas = it.hiddenIdeas + idea.name) }
                    }.padding(8.dp), tint = c.muted)
                    Spacer(Modifier.width(4.dp))
                }
                if (whyOpen == idea.name) Text(idea.why, fontSize = 12.sp, color = c.muted, modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp))
            }
        }
        Text(pluralStringResource(R.plurals.forms_see_all_ideas, pool.size, pool.size), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = c.headerText,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onSeeAll).padding(vertical = 8.dp, horizontal = 2.dp))
    }
}

fun Activity.returnIdea(name: String) {
    setResult(Activity.RESULT_OK, Intent().putExtra(IdeasActivity.RESULT_NAME, name))
    finish()
}
