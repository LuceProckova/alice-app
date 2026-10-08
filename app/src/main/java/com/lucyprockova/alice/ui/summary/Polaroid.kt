package com.lucyprockova.alice.ui.summary

import com.lucyprockova.alice.ui.achievementShort
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.logic.SummaryData
import com.lucyprockova.alice.ui.*
// ---------------------------------------------------------------- 7. polaroid

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Polaroid(d: SummaryData, luck: SummaryLuck) {
    val c = Alice.colors
    val hand = FontFamily.Cursive
    val notes = listOf(lerp(c.button, Color.White, .6f), Color(0xFFDDEBDA), if (c.dark) Color(0xFFFFF1C9) else lerp(c.spot, Color.White, .3f), Color(0xFFF6DDDA))
    @Composable fun Note(color: Color, angle: Float, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) =
        Column(modifier.rotate(angle).background(color).padding(12.dp), content = content)
    Column(Modifier.fillMaxWidth().background(if (c.dark) c.card else Color(0xFFFFF9EA)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(Modifier.width(230.dp).rotate(-3f).background(Color.White).padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PixelPicture(luck.scene, Modifier.fillMaxWidth())
                Text("${d.name} · ${period(d)} ♡", fontFamily = hand, fontSize = 17.sp, color = Color(0xFF3B2F28), modifier = Modifier.padding(top = 6.dp))
            }
            Box(Modifier.offset(y = (-8).dp).align(Alignment.TopCenter).width(80.dp).height(18.dp).background(c.button.copy(alpha = .6f)))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Note(notes[0], -1f, Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Spa, null, Modifier.size(14.dp), tint = c.headerSub); Text(" " + stringResource(R.string.look_polaroid_life), fontFamily = hand, fontSize = 14.sp, color = c.headerSub) }
                listOf(stringResource(R.string.look_habits) to d.habits, stringResource(R.string.look_swaps) to d.swaps, stringResource(R.string.look_avoids) to d.avoids).forEach { (l, n) ->
                    Row { Text(l, Modifier.weight(1f), fontFamily = hand, fontSize = 15.sp, color = Color(0xFF3B2F28)); Text("$n", fontWeight = FontWeight.Bold, color = Color(0xFF3B2F28)) }
                }
                Row { Text(stringResource(R.string.look_ways_improved), Modifier.weight(1f), fontFamily = hand, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B2F28)); Text("${d.ways}", fontWeight = FontWeight.Bold, color = Color(0xFF3B2F28)) }
            }
            Note(notes[1], 1.5f, Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.AutoMirrored.Filled.TrendingUp, null, Modifier.size(14.dp), tint = c.headerSub); Text(" " + stringResource(R.string.look_polaroid_quarter), fontFamily = hand, fontSize = 14.sp, color = c.headerSub) }
                Row(verticalAlignment = Alignment.Bottom) { Text("${d.onTrackPercent}%", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF3B2F28)); Text(" " + stringResource(R.string.look_on_track), fontWeight = FontWeight.Bold, color = Color(0xFF3B2F28)) }
                Text(slipsText(d.slips), fontFamily = hand, fontSize = 15.sp, color = Color(0xFF3B2F28))
                Text(stringResource(R.string.look_polaroid_days_in, d.daysCheckedIn, d.days), fontFamily = hand, fontSize = 15.sp, color = Color(0xFF3B2F28))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Note(notes[2], 0.5f, Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.EmojiEvents, null, Modifier.size(14.dp), tint = c.headerSub); Text(" " + stringResource(R.string.look_polaroid_longest), fontFamily = hand, fontSize = 14.sp, color = c.headerSub) }
                if (d.achievements.isEmpty()) Text(stringResource(R.string.look_coming_soon), fontFamily = hand, color = Color(0xFF3B2F28))
                d.achievements.forEach { a -> Row { Text(a.stepName, Modifier.weight(1f), fontFamily = hand, fontSize = 15.sp, color = Color(0xFF3B2F28), maxLines = 1); Text(achievementShort(a), fontWeight = FontWeight.Bold, color = Color(0xFF3B2F28)) } }
            }
            Note(notes[3], -1.5f, Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.AddCircleOutline, null, Modifier.size(14.dp), tint = c.headerSub); Text(" " + stringResource(R.string.look_polaroid_new, d.added.size), fontFamily = hand, fontSize = 14.sp, color = c.headerSub) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    d.added.forEach { (_, n) -> Text(n, fontSize = 11.sp, color = Color(0xFF3B2F28), modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 7.dp, vertical = 2.dp)) }
                }
            }
        }
        Column(Modifier.fillMaxWidth().rotate(0.6f).background(Color.White).padding(12.dp)) {
            Text(stringResource(R.string.look_week_by_week), fontFamily = hand, fontSize = 16.sp, color = Color(0xFF3B2F28))
            WeekBars(d.weekShares, c.kept, c.empty, 50.dp, listOf(c.button, c.kept, Color(0xFFA8C8E2), c.missed))
            MonthsUnder(d, Color(0xFF3B2F28), hand)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).rotate(-2f).background(Color(0xFFEAD8B4)).border(1.dp, Color(0xFFD5BF94)).padding(12.dp)) {
                Text(luck.quote, fontFamily = hand, fontSize = 17.sp, color = Color(0xFF3B2F28))
            }
            Spacer(Modifier.width(10.dp))
            ScanBlock(c.ink)
        }
    }
}
