package com.lucyprockova.alice.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.logic.SummaryData
import com.lucyprockova.alice.ui.*
import com.lucyprockova.alice.ui.achievementShort
import com.lucyprockova.alice.ui.gString
import kotlin.math.roundToInt
// ---------------------------------------------------------------- 8. calendar (91 days)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CalendarStyle(d: SummaryData, luck: SummaryLuck) {
    val c = Alice.colors
    Column(Modifier.fillMaxWidth().background(c.card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Label("${period(d)} · ${d.name}", c.muted)
        Text(styled(gString(R.string.look_improved_in), SpanStyle(fontSize = 26.sp, fontWeight = FontWeight.Black), waysText(d.ways)),
            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.ink, textAlign = TextAlign.Center)
        Text("${habitsText(d.habits)}   ${swapsText(d.swaps)}   ${avoidsText(d.avoids)}", fontSize = 13.sp, color = c.ink)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.panel).padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            val weeks = d.dailyShare.chunked(7)
            for (row in 0 until 7) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    weeks.forEach { w ->
                        val v = w.getOrNull(row)
                        Box(Modifier.weight(1f).aspectRatio(1.15f).clip(RoundedCornerShape(4.dp)).background(if (v != null && v >= 0.5f) c.kept else c.empty))
                    }
                }
            }
        }
        MonthsUnder(d, c.muted)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("${d.daysCheckedIn}/${d.days}" to stringResource(R.string.look_days_in), "${d.onTrackPercent}%" to stringResource(R.string.look_on_track), "${d.slips}" to slipsWord(d.slips), "+${d.newSteps}" to stringResource(R.string.look_new)).forEach { (n, l) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(c.panel).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(n, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = c.ink); Text(l, fontSize = 10.5.sp, color = c.muted)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(c.panel).padding(10.dp)) {
                Label(stringResource(R.string.look_kept_up_longest), c.muted)
                if (d.achievements.isEmpty()) Text(stringResource(R.string.look_coming_soon), fontSize = 12.sp, color = c.muted)
                d.achievements.forEach { a -> Row { Text(a.stepName, Modifier.weight(1f), fontSize = 12.5.sp, color = c.ink, maxLines = 1); Text(achievementShort(a), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = c.ink) } }
            }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(c.panel).padding(10.dp)) {
                Label(stringResource(R.string.look_calendar_added), c.muted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    d.added.forEach { (_, n) -> Text(n, fontSize = 11.sp, color = c.ink, modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, c.line, RoundedCornerShape(50)).padding(horizontal = 7.dp, vertical = 2.dp)) }
                }
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.panel).padding(10.dp)) {
            Label(stringResource(R.string.look_week_by_week), c.muted)
            Spacer(Modifier.height(4.dp))
            for (level in 4 downTo 0) {
                Row(Modifier.padding(vertical = 1.5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    d.weekShares.forEach { s -> Box(Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(2.dp)).background(if ((s * 5).roundToInt() > level) c.kept else c.empty)) }
                }
            }
        }
        QuoteFooter(luck, c.ink, c.headerSub)
    }
}
