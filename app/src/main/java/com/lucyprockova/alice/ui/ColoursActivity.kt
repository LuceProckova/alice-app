package com.lucyprockova.alice.ui

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.data.StepKind
import com.lucyprockova.alice.logic.Achievement
import com.lucyprockova.alice.logic.AchievementKind
import com.lucyprockova.alice.logic.Snapshot
import com.lucyprockova.alice.logic.SummaryData
import com.lucyprockova.alice.ui.summary.*
import java.time.LocalDate

fun sampleSummary(ctx: Context, name: String): SummaryData {
    val r = kotlin.random.Random(4)
    val t = { id: Int -> ctx.getString(id) }
    return SummaryData(
        name = name.ifBlank { t(R.string.look_you) }, from = LocalDate.of(2026, 7, 1), to = LocalDate.of(2026, 9, 30),
        ways = 15, habits = 9, swaps = 4, avoids = 2, onTrackPercent = 85, slips = 2, newSteps = 6, daysCheckedIn = 84, days = 92,
        achievements = listOf(Achievement(t(R.string.look_sample_water_first), AchievementKind.MONTHS_STRONG, 6), Achievement(t(R.string.look_sample_own_bag), AchievementKind.NEVER_MISSED, 4), Achievement(t(R.string.look_sample_read_pages), AchievementKind.STREAK, 30)),
        added = listOf(StepKind.HABIT to t(R.string.look_sample_reading), StepKind.HABIT to t(R.string.look_sample_late_snacks), StepKind.HABIT to t(R.string.look_sample_stretching), StepKind.HABIT to t(R.string.look_sample_cooler_home), StepKind.SWAP to t(R.string.look_sample_loofah), StepKind.AVOID to t(R.string.look_sample_palm_oil)),
        dailyShare = List(91) { if (r.nextFloat() < 0.87f) 1f else 0f },
        weekShares = List(13) { 0.45f + it * 0.04f + r.nextFloat() * 0.08f },
    )
}

/** user's latest summary if any, else example */
fun previewSummary(ctx: Context, s: Snapshot): SummaryData {
    val last = s.checkups.maxByOrNull { it.dueEpochDay }
    if (last != null) return com.lucyprockova.alice.logic.Stats.summary(
        com.lucyprockova.alice.logic.StepRules.epoch(last.fromEpochDay), com.lucyprockova.alice.logic.StepRules.epoch(last.dueEpochDay).minusDays(1), s)
    return if (s.steps.size >= 3) {
        val next = com.lucyprockova.alice.logic.CheckIns.nextQuarter(s, s.today.plusDays(1))
        com.lucyprockova.alice.logic.Stats.summary(com.lucyprockova.alice.logic.CheckIns.previousQuarter(s, next), next.minusDays(1), s)
    } else sampleSummary(ctx, s.settings.name)
}

/** colours screen, ten presets light and dark, live summary preview */
class ColoursActivity : AliceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen { s ->
            val c = Alice.colors
            Column(Modifier.fillMaxSize().background(c.card)) {
                Header(stringResource(R.string.look_colours_title), back = stringResource(R.string.core_settings), onBack = ::finish)
                Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp)) {
                    ThemeGrid(s.settings.theme) { t -> repo.settingsStore.update { it.copy(theme = t) } }
                    SectionLabel(stringResource(R.string.look_colours_preview))
                    Text(stringResource(R.string.look_colours_your_summary), fontSize = 12.sp, color = c.muted)
                    Spacer(Modifier.height(8.dp))
                    val width = windowWidthDp() - 32.dp
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ScaledSummary(s.settings.summaryStyle, previewSummary(this@ColoursActivity, s), remember { SummaryLuck(1) }, width, width * 1.9f)
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun ThemeGrid(selected: Int, onPick: (Int) -> Unit) {
    listOf(stringResource(R.string.look_colours_light) to THEMES.indices.filter { !THEMES[it].dark }, stringResource(R.string.look_colours_dark) to THEMES.indices.filter { THEMES[it].dark }).forEach { (title, idx) ->
        SectionLabel(title)
        idx.chunked(2).forEach { pair ->
            Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { i -> ThemeTile(i, i == selected, Modifier.weight(1f)) { onPick(i) } }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ThemeTile(i: Int, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Alice.colors
    val t = THEMES[i]
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(t.header)
            .border(if (on) 3.dp else 1.dp, if (on) c.button else c.line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(t.header, t.button, t.kept).forEach { col ->
            Box(Modifier.size(15.dp).clip(CircleShape).background(col).border(1.dp, Color.Black.copy(alpha = .12f), CircleShape))
            Spacer(Modifier.width(2.dp))
        }
        Text(t.name, Modifier.padding(start = 6.dp), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = t.headerText, lineHeight = 13.sp)
    }
}
