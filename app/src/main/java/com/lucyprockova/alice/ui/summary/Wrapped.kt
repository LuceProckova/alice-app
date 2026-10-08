package com.lucyprockova.alice.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.logic.SummaryData
import com.lucyprockova.alice.ui.*
import com.lucyprockova.alice.ui.achievementText
import com.lucyprockova.alice.ui.gString
// ---------------------------------------------------------------- 2. wrapped

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Wrapped(d: SummaryData, luck: SummaryLuck) {
    val c = Alice.colors
    Box {
        Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(brightStops(c))).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("${d.name} · ${period(d)}", c.headerText)
            // big number on its own line, so only the words before it here
            Text(gString(R.string.look_improved_in).substringBefore("%1\$s").trimEnd(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.ink)
            Text(waysText(d.ways), fontSize = 54.sp, fontWeight = FontWeight.Black, color = c.ink, lineHeight = 56.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(habitsText(d.habits), swapsText(d.swaps), avoidsText(d.avoids)).forEach {
                    Text(it, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.clip(RoundedCornerShape(50)).background(softBox(c)).padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(c.headerText.copy(alpha = .3f)))
            Row {
                listOf("${d.onTrackPercent}%" to stringResource(R.string.look_on_track), "${d.slips}" to slipsWord(d.slips), "${d.newSteps}" to stringResource(R.string.look_new_steps), "${d.daysCheckedIn}/${d.days}" to stringResource(R.string.look_days_in)).forEach { (n, l) ->
                    Column(Modifier.weight(1f)) {
                        Text(n, fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.ink)
                        Text(l, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.ink)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(c.headerText.copy(alpha = .3f)))
            if (d.achievements.isNotEmpty()) {
                Label(stringResource(R.string.look_wrapped_stars), c.headerText)
                d.achievements.forEach { a ->
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Black, fontSize = 16.sp)) { append(a.stepName) }
                        append(" · ${achievementText(a)}")
                    }, fontSize = 14.sp, color = c.ink)
                }
            }
            Label(stringResource(R.string.look_week_by_week), c.headerText)
            WeekBars(d.weekShares, Color.White, Color.White.copy(alpha = .5f), 50.dp, d.weekShares.indices.map { if (it == d.weekShares.lastIndex) Color.White else Color.White.copy(alpha = .6f) })
            MonthsUnder(d, c.headerText)
            if (d.added.isNotEmpty()) {
                Label(stringResource(R.string.look_new_this_quarter), c.headerText)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    d.added.forEach { (_, n) -> Text(n, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.clip(RoundedCornerShape(50)).background(softBox(c)).padding(horizontal = 10.dp, vertical = 4.dp)) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(luck.quote, Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Black, color = c.ink)
                ScanBlock(c.ink)
            }
        }
        Confetti(luck.stamp.hashCode(), confettiColors(c), Modifier.matchParentSize())
    }
}
