package com.lucyprockova.alice.ui.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.logic.SummaryData
import com.lucyprockova.alice.ui.*
import com.lucyprockova.alice.ui.achievementText
import com.lucyprockova.alice.ui.gString
// ---------------------------------------------------------------- 1. poster

@Composable
internal fun Poster(d: SummaryData, luck: SummaryLuck) {
    val c = Alice.colors
    val pale = lerp(c.button, Color.White, 0.55f)
    Column(Modifier.fillMaxWidth().background(c.card).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box {
            Card(Brush.linearGradient(brightStops(c))) {
                Label("${period(d)} · ${d.name}", c.headerText)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(styled(gString(R.string.look_poster_improved), SpanStyle(fontSize = 30.sp, fontWeight = FontWeight.Black), waysText(d.ways)), Modifier.weight(1f).padding(vertical = 10.dp), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.ink, lineHeight = 30.sp)
                    PixelTrophy(lerp(pale, Color.White, .5f), c.headerText, Modifier.width(70.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(d.habits to habitsWord(d.habits), d.swaps to swapsWord(d.swaps), d.avoids to avoidsWord(d.avoids)).forEach { (n, l) ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(softBox(c)).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$n", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
                            Text(l, fontSize = 11.sp, color = c.ink)
                        }
                    }
                }
            }
            Confetti(luck.quote.hashCode(), confettiColors(c), Modifier.matchParentSize())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(c.panel).border(1.dp, c.line, RoundedCornerShape(14.dp)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                    Canvas(Modifier.size(90.dp)) {
                        drawArc(c.empty, 0f, 360f, false, style = Stroke(10.dp.toPx()))
                        drawArc(c.kept, -90f, 360f * d.onTrackPercent / 100f, false, style = Stroke(10.dp.toPx()))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${d.onTrackPercent}%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
                        Text(stringResource(R.string.look_poster_on_track), fontSize = 8.sp, color = c.muted)
                    }
                }
                Spacer(Modifier.height(6.dp))
                IconStat(Icons.Filled.SentimentSatisfied, "${d.slips}", slipsWord(d.slips))
                IconStat(Icons.Filled.AddCircleOutline, "${d.newSteps}", stringResource(R.string.look_new_steps))
                IconStat(Icons.Filled.EventAvailable, "${d.daysCheckedIn}/${d.days}", stringResource(R.string.look_poster_days))
            }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(c.panel).border(1.dp, c.line, RoundedCornerShape(14.dp)).padding(12.dp)) {
                Label(stringResource(R.string.look_poster_achievements), c.muted)
                if (d.achievements.isEmpty()) Text(stringResource(R.string.look_poster_first_ones), fontSize = 12.sp, color = c.muted, modifier = Modifier.padding(top = 6.dp))
                d.achievements.forEach { a ->
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        PixelMedal(c.button, c.headerSub, 18.dp)
                        Column(Modifier.padding(start = 6.dp)) {
                            Text(a.stepName, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
                            Text(achievementText(a), fontSize = 11.sp, color = c.muted)
                        }
                    }
                }
            }
        }
        AddedBox(d, stringResource(R.string.look_poster_added))
        QuoteFooter(luck, c.ink, c.headerSub)
    }
}

@Composable
internal fun IconStat(icon: androidx.compose.ui.graphics.vector.ImageVector, n: String, label: String) {
    val c = Alice.colors
    Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(15.dp), tint = c.button)
        Text(" $n ", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
        Text(label, fontSize = 12.sp, color = c.ink)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddedBox(d: SummaryData, title: String) {
    val c = Alice.colors
    if (d.added.isEmpty()) return
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.panel).border(1.dp, c.line, RoundedCornerShape(14.dp)).padding(12.dp)) {
        Label(title, c.muted)
        FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            d.added.forEach { (k, n) ->
                Row(Modifier.clip(RoundedCornerShape(50)).border(1.dp, c.line, RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(k.icon(), null, Modifier.size(13.dp), tint = c.button)
                    Text(" $n", fontSize = 11.5.sp, color = c.ink)
                }
            }
        }
    }
}
