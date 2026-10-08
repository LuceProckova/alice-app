package com.lucyprockova.alice.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.data.AliceDatabase
import com.lucyprockova.alice.data.Category
import com.lucyprockova.alice.data.IdeaGroup
import com.lucyprockova.alice.data.PeriodUnit
import com.lucyprockova.alice.data.SlipLevel
import com.lucyprockova.alice.data.Step
import com.lucyprockova.alice.data.StepKind
import com.lucyprockova.alice.data.Verdict
import com.lucyprockova.alice.logic.Achievement
import com.lucyprockova.alice.logic.AchievementKind
import com.lucyprockova.alice.logic.Cell
import com.lucyprockova.alice.logic.CellState
import com.lucyprockova.alice.logic.GlanceStep
import com.lucyprockova.alice.logic.Rhythm
import com.lucyprockova.alice.logic.StepRules
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

fun StepKind.icon(): ImageVector = when (this) {
    StepKind.HABIT -> Icons.Filled.Repeat
    StepKind.SWAP -> Icons.Filled.SwapHoriz
    StepKind.AVOID -> Icons.Filled.Block
}

@Composable
fun StepKind.label(): String = stringResource(when (this) {
    StepKind.HABIT -> R.string.core_kind_habit
    StepKind.SWAP -> R.string.core_kind_swap
    StepKind.AVOID -> R.string.core_kind_avoid
})

fun iconFor(name: String): ImageVector = when (name) {
    "bedtime" -> Icons.Filled.Bedtime
    "cookie" -> Icons.Filled.Cookie
    "schedule" -> Icons.Filled.Schedule
    "help" -> Icons.AutoMirrored.Filled.HelpOutline
    "storefront" -> Icons.Filled.Storefront
    "group" -> Icons.Filled.Group
    "bolt" -> Icons.Filled.Bolt
    "payments" -> Icons.Filled.Payments
    "flight" -> Icons.Filled.Flight
    "task_alt" -> Icons.Filled.TaskAlt
    else -> Icons.Filled.Sell
}

/** day label for screen readers, e.g. monday 5 october, english like the rest of the app */
private val SPOKEN_DAY get() = DateTimeFormatter.ofPattern("EEEE d MMMM", APP_LOCALE)

/** what a square means, read out by talkback after the day */
@Composable
fun spokenState(cell: Cell): String = when (cell.state) {
    CellState.KEPT -> if (cell.count > 1) pluralStringResource(R.plurals.core_cell_kept_times, cell.count, cell.count) else stringResource(R.string.core_cell_kept)
    CellState.MISSED -> stringResource(R.string.core_cell_missed)
    CellState.PARTIAL -> stringResource(R.string.core_cell_partial, cell.count)
    CellState.UNFILLED -> stringResource(R.string.core_cell_unfilled)
    CellState.OPEN -> stringResource(R.string.core_cell_open)
    CellState.LOCKED -> stringResource(R.string.core_cell_locked)
    CellState.FUTURE -> stringResource(R.string.core_cell_future)
    CellState.EMPTY -> stringResource(R.string.core_cell_empty)
    CellState.BAND -> stringResource(R.string.core_cell_band)
    CellState.OFF -> stringResource(R.string.core_cell_off)
    CellState.NONE -> stringResource(R.string.core_cell_none)
}

/** one day square, looks [size] big but taps land on a 48dp area, talkback reads day and state */
@Composable
fun DaySquare(cell: Cell, size: Dp = 20.dp, onClick: (() -> Unit)? = null) {
    val c = Alice.colors
    val shape = RoundedCornerShape(5.dp)
    val tappable = onClick != null && cell.tappable
    val spoken = stringResource(R.string.core_cell_spoken, cell.day.format(SPOKEN_DAY), spokenState(cell))
    // outer box carries tap + one label replacing the digits inside, not clipped, so compose stretches the tap area to 48dp around it
    var outer = Modifier.size(size).clearAndSetSemantics {
        contentDescription = spoken
        if (tappable) {
            role = Role.Button
            // clear wipes clickable's own action, so give talkback the tap back
            onClick { onClick!!(); true }
        }
    }
    if (tappable) outer = outer.clickable(onClick = onClick!!)
    var m = Modifier.size(size).clip(shape)
    m = when (cell.state) {
        CellState.KEPT -> m.background(c.kept)
        CellState.MISSED -> m.background(c.missed)
        CellState.PARTIAL -> m.background(c.card).border(2.dp, c.kept, shape)
        CellState.UNFILLED -> m.background(c.empty).border(1.5.dp, c.line, shape)
        CellState.OPEN -> m.background(c.card).border(2.dp, c.button, shape)
        CellState.LOCKED, CellState.FUTURE, CellState.EMPTY -> m.background(c.empty)
        // band strip drawn behind by grid row, square stays see-through
        CellState.BAND -> m
        CellState.OFF, CellState.NONE -> m
    }
    Box(outer) { Box(m, contentAlignment = Alignment.Center) {
        when (cell.state) {
            CellState.LOCKED -> Icon(Icons.Filled.Lock, null, Modifier.size(size * 0.55f), tint = c.headerSub)
            CellState.OFF -> Box(Modifier.size(5.dp).clip(CircleShape).background(c.line))
            CellState.PARTIAL -> Text("${cell.count}", fontSize = (size.value * 0.45f).sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
            CellState.KEPT -> if (cell.count > 1) Text("${cell.count}", fontSize = (size.value * 0.45f).sp, fontWeight = FontWeight.ExtraBold, color = c.keptText)
            CellState.BAND -> if (cell.count > 0) Text(if (cell.count > 1) "${cell.count}" else "✓", fontSize = (size.value * 0.5f).sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            else -> Unit
        }
    } }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, fontSize: TextUnit = 13.sp) {
    val c = Alice.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.empty).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, text ->
            val on = i == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (on) c.button else c.empty)
                    // tab role + selected, so talkback says which option is picked
                    .selectable(selected = on, role = Role.Tab) { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text, fontSize = fontSize, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) c.buttonText else c.muted, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun InfoTip(title: String, text: String, tint: Color = Alice.colors.headerSub) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.size(26.dp).clip(CircleShape).clickable { open = true }, contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Info, stringResource(R.string.core_about, title), Modifier.size(16.dp), tint = tint)
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.core_ok)) } },
            title = { Text(title) },
            text = { Text(text) },
        )
    }
}

@Composable
fun Header(
    title: String,
    back: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    below: @Composable ColumnScope.() -> Unit = {},
) {
    val c = Alice.colors
    // header colour runs up under status bar, content padded below it
    Column(Modifier.fillMaxWidth().background(c.header).statusBarsPadding().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 10.dp)) {
        if (back != null) {
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable { onBack?.invoke() }.padding(end = 8.dp, top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(chevronBack, null, Modifier.size(18.dp), tint = c.headerSub)
                Text(back, fontSize = 12.5.sp, color = c.headerSub, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = c.headerText, modifier = Modifier.weight(1f), maxLines = 2, lineHeight = 24.sp)
            actions()
        }
        below()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = Alice.colors.muted)
        trailing()
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = Alice.colors.panel, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(color).border(1.dp, Alice.colors.line, RoundedCornerShape(14.dp)).padding(12.dp), content = content)
}

@Composable
fun Pill(text: String, on: Boolean = false, icon: ImageVector? = null, onClick: (() -> Unit)? = null) {
    val c = Alice.colors
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(if (on) c.button else c.empty)
            // clickable pills are on/off filters or tags, so talkback hears a checkbox and its state
            .let { if (onClick != null) it.toggleable(value = on, role = Role.Checkbox) { onClick() } else it }
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(14.dp), tint = if (on) c.buttonText else c.muted); Spacer(Modifier.width(4.dp)) }
        Text(text, fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = if (on) c.buttonText else c.ink)
    }
}

@Composable
fun MainButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, quiet: Boolean = false, onClick: () -> Unit) {
    val c = Alice.colors
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (quiet) Color.Transparent else if (enabled) c.button else c.empty)
            .border(if (quiet) 1.dp else 0.dp, if (quiet) c.line else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (quiet) c.ink else if (enabled) c.buttonText else c.muted)
    }
}

@Composable
fun Stepper(value: Int, unit: String = "", min: Int = 1, max: Int = 999, onChange: (Int) -> Unit) {
    val c = Alice.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StepButton(Icons.Filled.Remove, stringResource(R.string.core_less)) { onChange((value - 1).coerceIn(min, max)) }
        Text("$value", Modifier.width(34.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = c.ink)
        StepButton(Icons.Filled.Add, stringResource(R.string.core_more)) { onChange((value + 1).coerceIn(min, max)) }
        if (unit.isNotEmpty()) Text(unit, fontSize = 11.5.sp, color = c.muted)
    }
}

@Composable
private fun StepButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = Alice.colors
    Box(
        Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, c.line, RoundedCornerShape(8.dp))
            .background(c.card).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, Modifier.size(18.dp), tint = c.ink) }
}

@Composable
fun ConfirmDialog(title: String, text: String, action: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        // dismiss first, onConfirm may finish the activity
        confirmButton = { TextButton(onClick = { onDismiss(); onConfirm() }) { Text(action, color = Alice.colors.danger, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_cancel)) } },
    )
}

@Composable
fun SettingsButton(back: String) {
    val ctx = LocalContext.current
    IconButton(onClick = { ctx.open(SettingsActivity::class.java, SettingsActivity.EXTRA_BACK to back) }) { Icon(Icons.Filled.Settings, stringResource(R.string.core_settings), tint = Alice.colors.headerText) }
}

/** time span picked on insights and one habit charts */
enum class Span(@StringRes val labelRes: Int) {
    WEEK(R.string.core_span_week), MONTH(R.string.core_span_month), YEAR(R.string.core_span_year), ALL(R.string.core_span_all);

    val label: String @Composable get() = stringResource(labelRes)
}

/** the 4 main tabs, all inside [MainActivity] */
enum class Tab(@StringRes val labelRes: Int, val icon: ImageVector) {
    HABITS(R.string.core_tab_habits, Icons.Filled.CheckCircle),
    CHANGES(R.string.core_tab_changes, Icons.Filled.Repeat),
    AVOID(R.string.core_tab_avoid, Icons.Filled.Block),
    INSIGHTS(R.string.core_tab_insights, Icons.Filled.BarChart);

    val label: String @Composable get() = stringResource(labelRes)
}

/** m3 bottom bar for the 4 tabs, tap only swaps tab, no new activity */
@Composable
fun BottomNav(current: Tab, onSelect: (Tab) -> Unit) {
    val c = Alice.colors
    NavigationBar(containerColor = c.card, tonalElevation = 0.dp, modifier = Modifier.border(1.dp, c.line)) {
        Tab.entries.forEach { tab ->
            val on = tab == current
            NavigationBarItem(
                selected = on,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, null, Modifier.size(21.dp)) },
                label = { Text(tab.label, fontSize = 11.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = c.navOn, selectedTextColor = c.navOn, indicatorColor = c.spot,
                    unselectedIconColor = c.muted, unselectedTextColor = c.muted,
                ),
            )
        }
    }
}

// shared text helpers, words for values that logic and data keep as plain data

/** how often a step repeats in words, e.g. every mon, wed and fri */
@Composable
fun rhythmText(step: Step): String = rhythmText(StepRules.rhythm(step))

@Composable
fun rhythmText(r: Rhythm): String = when (r) {
    Rhythm.OneOff -> stringResource(R.string.core_rhythm_one_off)
    Rhythm.EndOfDay -> stringResource(R.string.core_rhythm_end_of_day)
    is Rhythm.Daily -> if (r.timesPerDay <= 1) stringResource(R.string.core_rhythm_every_day)
        else pluralStringResource(R.plurals.core_rhythm_times_a_day, r.timesPerDay, r.timesPerDay)
    is Rhythm.Days -> {
        val days = r.days.map { (i, c) ->
            val day = DayOfWeek.of(i + 1).getDisplayName(TextStyle.SHORT, APP_LOCALE)
            if (c > 1) stringResource(R.string.core_rhythm_day_count, day, c) else day
        }
        stringResource(R.string.core_rhythm_every_days, listText(days))
    }
    is Rhythm.Times -> {
        val n = r.everyN
        val every = when (r.unit) {
            PeriodUnit.WEEK -> if (n == 1) stringResource(R.string.core_rhythm_a_week) else pluralStringResource(R.plurals.core_rhythm_every_weeks, n, n)
            PeriodUnit.MONTH -> if (n == 1) stringResource(R.string.core_rhythm_a_month) else pluralStringResource(R.plurals.core_rhythm_every_months, n, n)
            PeriodUnit.DAYS -> pluralStringResource(R.plurals.core_rhythm_every_n_days, n, n)
        }
        stringResource(R.string.core_rhythm_times_any_day, pluralStringResource(R.plurals.core_rhythm_times, r.times, r.times), every)
    }
}

/** a, b and c, last pair joined with and */
@Composable
fun listText(items: List<String>): String {
    if (items.size < 2) return items.firstOrNull() ?: ""
    val sep = stringResource(R.string.core_list_separator)
    return stringResource(R.string.core_list_last, items.dropLast(1).joinToString(sep), items.last())
}

/** full achievement line, e.g. 4 months, never missed */
@Composable
fun achievementText(a: Achievement): String = when (a.kind) {
    AchievementKind.NEVER_MISSED -> pluralStringResource(R.plurals.core_achievement_never_missed, a.count, a.count)
    AchievementKind.MONTHS_STRONG -> pluralStringResource(R.plurals.core_achievement_months_strong, a.count, a.count)
    AchievementKind.STREAK -> pluralStringResource(R.plurals.core_achievement_streak, a.count, a.count)
}

/** achievement without the never missed tail, e.g. 4 months, for boarding pass */
@Composable
fun achievementHeadline(a: Achievement): String =
    if (a.kind == AchievementKind.NEVER_MISSED) pluralStringResource(R.plurals.core_achievement_months, a.count, a.count) else achievementText(a)

/** tiny achievement, e.g. 4 mo or 30 d, for narrow styles */
@Composable
fun achievementShort(a: Achievement): String =
    if (a.kind == AchievementKind.STREAK) stringResource(R.string.core_achievement_short_days, a.count)
    else stringResource(R.string.core_achievement_short_months, a.count)

/** week glance step, e.g. read 10 pages, 5 of 7 */
@Composable
fun glanceStepText(g: GlanceStep): String = stringResource(R.string.core_glance_step, g.name, g.kept, g.days)

/** check-up answer word, e.g. a few */
val SlipLevel.label: String @Composable get() = stringResource(when (this) {
    SlipLevel.NEVER -> R.string.core_slip_never
    SlipLevel.ONCE -> R.string.core_slip_once
    SlipLevel.A_FEW -> R.string.core_slip_a_few
    SlipLevel.HALF -> R.string.core_slip_half
    SlipLevel.MOST -> R.string.core_slip_most
    SlipLevel.ALWAYS -> R.string.core_slip_always
})

/** idea list name, e.g. habits, anytime */
val IdeaGroup.title: String @Composable get() = stringResource(when (this) {
    IdeaGroup.HABIT_ANYTIME -> R.string.core_ideas_habit_anytime
    IdeaGroup.HABIT_END -> R.string.core_ideas_habit_end
    IdeaGroup.SWAP -> R.string.core_ideas_swap
    IdeaGroup.AVOID_PRODUCT -> R.string.core_ideas_avoid_product
    IdeaGroup.AVOID_COMPANY -> R.string.core_ideas_avoid_company
    IdeaGroup.AVOID_FOOD -> R.string.core_ideas_avoid_food
})

/** avoid tags stay english in Step.tags, this is only what the screen shows */
private val TAG_LABELS = mapOf(
    "Ethical" to R.string.core_tag_ethical, "Health" to R.string.core_tag_health, "Clutter" to R.string.core_tag_clutter,
    "Ecological" to R.string.core_tag_ecological, "Animal welfare" to R.string.core_tag_animal_welfare,
    "Workers' rights" to R.string.core_tag_workers_rights, "Plastic" to R.string.core_tag_plastic,
    "Palm oil" to R.string.core_tag_palm_oil, "Deforestation" to R.string.core_tag_deforestation,
)

/** shown name of a stored tag, own tags come back as typed */
@Composable
fun tagLabel(key: String): String = TAG_LABELS[key]?.let { stringResource(it) } ?: key

/** company, food or product, null counts as product */
@Composable
fun Category?.categoryLabel(): String = stringResource(when (this) {
    Category.COMPANY -> R.string.core_category_company
    Category.FOOD -> R.string.core_category_food
    else -> R.string.core_category_product
})

/** badge word on avoid list, capitals like the badge */
@Composable
fun Verdict.verdictLabel(): String = stringResource(when (this) {
    Verdict.AVOID -> R.string.core_verdict_avoid
    Verdict.EDGE -> R.string.core_verdict_edge
    Verdict.FINE -> R.string.core_verdict_fine
})

/** icon key for a slip reason, default ones in app language or english, else tag icon */
@Composable
fun defaultIconFor(reason: String): String {
    val ctx = LocalContext.current
    val icons = remember(ctx) { AliceDatabase.defaultReasonIcons(ctx) }
    return icons[reason] ?: "sell"
}

/** back arrow, points left, flips for right to left languages like arabic and hebrew */
val chevronBack: ImageVector
    @Composable get() = if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Filled.ChevronRight else Icons.Filled.ChevronLeft

/** forward arrow, the other way round */
val chevronForward: ImageVector
    @Composable get() = if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight
