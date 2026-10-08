package com.lucyprockova.alice.ui

import android.os.Bundle
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.lucyprockova.alice.R
import com.lucyprockova.alice.data.AliceSettings
import com.lucyprockova.alice.data.StepKind
import com.lucyprockova.alice.logic.Cell
import com.lucyprockova.alice.logic.CellState
import com.lucyprockova.alice.ui.summary.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/** seven swipe cards, explain alice, set first settings on the way */
class TutorialActivity : AliceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen { s ->
            val c = Alice.colors
            val pager = rememberPagerState { 7 }
            val scope = rememberCoroutineScope()
            val done = { repo.settingsStore.update { it.copy(tutorialSeen = true) }; finish() }
            Column(Modifier.fillMaxSize().background(c.card).statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(R.string.look_tut_skip), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = c.headerSub, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { done() }.padding(6.dp))
                }
                HorizontalPager(pager, Modifier.weight(1f)) { page ->
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.look_tut_page, page + 1, 7), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = c.headerSub, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
                        when (page) {
                            0 -> Card1()
                            1 -> Card2()
                            2 -> Card3(s.settings)
                            3 -> Card4(s.settings)
                            4 -> Card5(s.settings, pager.currentPage == 4)
                            5 -> Card6(s.settings)
                            else -> Card7 { open(SettingsActivity::class.java) }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(7) { i ->
                        Box(Modifier.padding(3.dp).height(7.dp).width(if (i == pager.currentPage) 18.dp else 7.dp).clip(CircleShape)
                            .background(if (i == pager.currentPage) c.button else c.line).clickable { scope.launch { pager.animateScrollToPage(i) } })
                    }
                }
                Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pager.currentPage > 0) MainButton(stringResource(R.string.look_tut_back), Modifier.weight(1f), quiet = true) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }
                    else Spacer(Modifier.weight(1f))
                    MainButton(stringResource(if (pager.currentPage == 6) R.string.look_tut_start else R.string.look_tut_next), Modifier.weight(1f)) {
                        if (pager.currentPage == 6) done() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    }
                }
            }
        }
    }

    @Composable private fun Title(t: String) = Text(t, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Alice.colors.ink, textAlign = TextAlign.Center, lineHeight = 26.sp)
    @Composable private fun Body(t: String) = Text(t, fontSize = 14.sp, color = Alice.colors.muted, textAlign = TextAlign.Center, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp, bottom = 14.dp))
    @Composable private fun Small(t: String) = Text(t, fontSize = 12.sp, color = Alice.colors.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))

    @Composable
    private fun Card1() {
        val c = Alice.colors
        Title(stringResource(R.string.look_tut1_title))
        Body(stringResource(R.string.look_tut1_body))
        listOf(
            Triple(StepKind.HABIT, stringResource(R.string.look_habits), stringResource(R.string.look_tut1_habits)),
            Triple(StepKind.SWAP, stringResource(R.string.look_swaps), stringResource(R.string.look_tut1_swaps)),
            Triple(StepKind.AVOID, stringResource(R.string.look_avoids), stringResource(R.string.look_tut1_avoids)),
        ).forEach { (k, t, d) ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(12.dp)).background(c.panel).border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c.spot), contentAlignment = Alignment.Center) { Icon(k.icon(), null, tint = c.headerText) }
                Column(Modifier.padding(start = 10.dp)) {
                    Text(t, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = c.ink)
                    Text(d, fontSize = 12.sp, color = c.muted, lineHeight = 16.sp)
                }
            }
        }
    }

    @Composable
    private fun Card2() {
        val c = Alice.colors
        Title(stringResource(R.string.look_tut2_title))
        Body(gString(R.string.look_tut2_body))
        val t = rememberInfiniteTransition(label = "tap")
        val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "p")
        val today = 3
        Column(Modifier.clip(RoundedCornerShape(14.dp)).background(c.panel).padding(12.dp)) {
            Row { Spacer(Modifier.width(110.dp)); DayOfWeek.entries.map { it.letter() }.forEach { Text(it, Modifier.width(24.dp), textAlign = TextAlign.Center, fontSize = 10.sp, color = c.muted, fontWeight = FontWeight.Bold) } }
            listOf(stringResource(R.string.look_sample_water_first), stringResource(R.string.look_sample_stretch), stringResource(R.string.look_sample_read_pages)).forEachIndexed { row, name ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, Modifier.width(110.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.ink)
                    (0 until 7).forEach { d ->
                        val state = when {
                            d < today -> if ((row + d) % 4 == 3) CellState.MISSED else CellState.KEPT
                            d == today -> if (row == 0 && p > 0.45f) CellState.KEPT else CellState.OPEN
                            else -> CellState.FUTURE
                        }
                        Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                            DaySquare(Cell(LocalDate.now(), state), 18.dp)
                            if (row == 0 && d == today) {
                                val lift = if (p < 0.45f) (1 - p / 0.45f) * 10 else 0f
                                Icon(Icons.Filled.TouchApp, null, Modifier.size(26.dp).offset(x = 8.dp, y = (12 + lift).dp), tint = c.headerText)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Card3(st: AliceSettings) {
        val c = Alice.colors
        val months = if (st.ownCheckup) st.checkupEveryMonths else 3
        Title(stringResource(R.string.look_tut3_title, pluralStringResource(R.plurals.look_tut3_days, st.daysToBuild, st.daysToBuild), pluralStringResource(R.plurals.look_tut3_months, months, months)))
        Body(stringResource(R.string.look_tut3_body))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.panel).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.look_tut3_days_label), Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = c.ink)
                Stepper(st.daysToBuild, "", 14, 120) { v -> repo.settingsStore.update { it.copy(daysToBuild = v) } }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.look_tut3_checkup), Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = c.ink)
                // 3 = shared quarterly dates, anything else = own date
                Stepper(if (st.ownCheckup) st.checkupEveryMonths else 3, "", 1, 12) { v ->
                    repo.settingsStore.update { it.copy(ownCheckup = v != 3, checkupEveryMonths = v, checkupDayOfMonth = 1) }
                }
            }
        }
        Small(gString(R.string.look_tut3_small))
    }

    @Composable
    private fun Card4(st: AliceSettings) {
        Title(stringResource(R.string.look_tut4_title))
        Body(stringResource(R.string.look_tut4_body))
        Text(stringResource(R.string.look_tut4_day), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Alice.colors.ink, modifier = Modifier.padding(bottom = 6.dp))
        DayPicker(st.weeklyDay) { d -> repo.settingsStore.update { it.copy(weeklyDay = d) } }
        Small(gString(R.string.look_tut4_small))
    }

    @Composable
    private fun Card5(st: AliceSettings, visible: Boolean) {
        val c = Alice.colors
        Title(stringResource(R.string.look_tut5_title))
        Body(gString(R.string.look_tut5_body))
        val scroll = rememberScrollState()
        var touched by remember { mutableStateOf(false) }
        LaunchedEffect(visible, touched) {
            while (visible && !touched) {
                delay(16)
                val next = if (scroll.value >= scroll.maxValue) 0 else scroll.value + 1
                scroll.scrollTo(next)
            }
        }
        val data = remember { sampleSummary(this@TutorialActivity, st.name) }
        Row(Modifier.fillMaxWidth().horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SUMMARY_STYLES.forEachIndexed { i, name ->
                val on = st.summaryStyle == i
                Column(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) c.button else Color.Transparent)
                        .clickable { touched = true; repo.settingsStore.update { it.copy(summaryStyle = i) } }.padding(3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ScaledSummary(i, data, remember { SummaryLuck(i + 5) }, 120.dp, 240.dp)
                    Text(stringResource(name), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = if (on) c.buttonText else c.ink, modifier = Modifier.padding(vertical = 3.dp))
                }
            }
        }
        Small(gString(R.string.look_tut_change_mind))
    }

    @Composable
    private fun Card6(st: AliceSettings) {
        Title(stringResource(R.string.look_tut6_title))
        Body(stringResource(R.string.look_tut6_body))
        ThemeGrid(st.theme) { t -> repo.settingsStore.update { it.copy(theme = t) } }
        Small(gString(R.string.look_tut_change_mind))
    }

    @Composable
    private fun Card7(openSettings: () -> Unit) {
        val c = Alice.colors
        Title(stringResource(R.string.look_tut7_title))
        Body(stringResource(R.string.look_tut7_body))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.panel).border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Settings, null, Modifier.size(26.dp), tint = c.headerText)
            Text("  " + stringResource(R.string.look_tut7_gear), fontSize = 13.sp, color = c.ink)
        }
        Spacer(Modifier.height(14.dp))
        MainButton(stringResource(R.string.look_tut7_open), quiet = true, onClick = openSettings)
    }
}
