package com.lucyprockova.alice.ui.summary

import com.lucyprockova.alice.ui.achievementShort
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.data.StepKind
import com.lucyprockova.alice.logic.SummaryData
import com.lucyprockova.alice.ui.*
// ---------------------------------------------------------------- 9. trading card

@Composable
internal fun TradingCard(d: SummaryData, luck: SummaryLuck, number: Int) {
    val c = Alice.colors
    val red = lerp(c.missed, Color(0xFFA03A3A), .5f)
    val ordinal = stringResource(when { number % 100 in 11..13 -> R.string.look_ordinal_th; number % 10 == 1 -> R.string.look_ordinal_st; number % 10 == 2 -> R.string.look_ordinal_nd; number % 10 == 3 -> R.string.look_ordinal_rd; else -> R.string.look_ordinal_th }, number)
    val stars = when { d.onTrackPercent >= 90 -> 4; d.onTrackPercent >= 75 -> 3; d.onTrackPercent >= 50 -> 2; else -> 1 }
    val rarity = stringResource(listOf(R.string.look_trading_common, R.string.look_trading_uncommon, R.string.look_trading_rare, R.string.look_trading_legendary)[stars - 1])
    Column(Modifier.fillMaxWidth().background(c.card).padding(12.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.sweepGradient(listOf(c.button, c.missed, c.kept, c.button))).padding(7.dp)
                .clip(RoundedCornerShape(14.dp)).background(if (c.dark) c.panel else Color(0xFFFFFAEC)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(d.name.ifBlank { stringResource(R.string.look_you) }, Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
                Text("${d.ways}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = red)
                Text(" " + pluralStringResource(R.plurals.look_trading_ways, d.ways), fontSize = 12.sp, fontWeight = FontWeight.Black, color = red)
            }
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))) {
                PixelPicture(luck.scene, Modifier.fillMaxWidth())
                Text(stringResource(R.string.look_trading_number, number, ordinal), Modifier.padding(8.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .8f)).padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2B2B2B))
                Text(stringResource(R.string.look_trading_quarter, quarterNo(d), period(d)), Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .8f)).padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2B2B2B))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.look_trading_hp) + " ", fontWeight = FontWeight.Black, fontSize = 13.sp, color = red)
                Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(c.empty)) {
                    Box(Modifier.fillMaxWidth(if (d.days == 0) 0f else d.daysCheckedIn / d.days.toFloat()).height(10.dp).clip(RoundedCornerShape(5.dp)).background(c.kept))
                }
                Text(" " + stringResource(R.string.look_trading_days, d.daysCheckedIn, d.days), fontWeight = FontWeight.Black, fontSize = 13.sp, color = red)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(StepKind.HABIT to habitsText(d.habits), StepKind.SWAP to swapsText(d.swaps), StepKind.AVOID to avoidsText(d.avoids)).forEach { (k, t) ->
                    Row(Modifier.weight(1f).clip(RoundedCornerShape(50)).background(c.panel).padding(vertical = 5.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(k.icon(), null, Modifier.size(13.dp), tint = c.ink); Text(" $t", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = c.ink)
                    }
                }
            }
            d.achievements.forEach { a ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, null, Modifier.size(18.dp), tint = c.button)
                    Text(" " + a.stepName, Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = c.ink)
                    Text(achievementShort(a), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = red)
                }
            }
            Row {
                listOf("${d.onTrackPercent}%" to stringResource(R.string.look_on_track), "${d.slips}" to slipsWord(d.slips), "+${d.newSteps}" to stringResource(R.string.look_new), "${d.ways}" to waysWord(d.ways)).forEach { (n, l) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Text(n, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = c.ink); Text(l, fontSize = 10.5.sp, color = c.muted) }
                }
            }
            Label(stringResource(R.string.look_trading_power), c.muted)
            Canvas(Modifier.fillMaxWidth().height(50.dp)) {
                val pts = d.weekShares.ifEmpty { listOf(0f) }
                val step = if (pts.size > 1) size.width / (pts.size - 1) else 0f
                val path = Path(); val fill = Path()
                pts.forEachIndexed { i, p ->
                    val o = Offset(i * step, size.height * (1 - 0.15f - 0.75f * p))
                    if (i == 0) { path.moveTo(o.x, o.y); fill.moveTo(o.x, size.height); fill.lineTo(o.x, o.y) } else { path.lineTo(o.x, o.y); fill.lineTo(o.x, o.y) }
                }
                fill.lineTo((pts.size - 1) * step, size.height); fill.close()
                drawPath(fill, c.button.copy(alpha = .35f))
                drawPath(path, lerp(c.button, c.missed, .5f), style = Stroke(2.5.dp.toPx()))
                pts.forEachIndexed { i, p -> drawCircle(lerp(c.button, c.missed, .5f), 3.dp.toPx(), Offset(i * step, size.height * (1 - 0.15f - 0.75f * p))) }
            }
            Text(luck.quote, Modifier.fillMaxWidth().padding(vertical = 4.dp), textAlign = TextAlign.Center, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = c.ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("★".repeat(stars) + "☆".repeat(4 - stars), color = c.headerSub, fontSize = 13.sp)
                    Text(stringResource(R.string.look_trading_edition, rarity, quarterNo(d)), color = c.headerSub, fontSize = 12.sp)
                }
                ScanBlock(c.ink)
            }
        }
    }
}
