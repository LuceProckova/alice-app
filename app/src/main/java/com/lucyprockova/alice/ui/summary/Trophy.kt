package com.lucyprockova.alice.ui.summary

import com.lucyprockova.alice.ui.achievementText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.logic.SummaryData
import com.lucyprockova.alice.ui.*
// ---------------------------------------------------------------- 3. trophy cabinet

@Composable
internal fun Trophy(d: SummaryData, luck: SummaryLuck) {
    val c = Alice.colors
    val wood = if (c.dark) lerp(c.panel, Color(0xFF7A5A38), .6f) else Color(0xFF775A38)
    val shelf = Color(0xFFC9A57E)
    val box = lerp(c.button, Color.White, .45f)
    val boxInk = Color(0xFF2B2118)
    Column(Modifier.fillMaxWidth().background(c.card).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF5A4330)).padding(6.dp).clip(RoundedCornerShape(12.dp)).background(wood).padding(10.dp)) {
            Text("${period(d)} · ${d.name}".uppercase(), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, color = Color(0xFFF3E6D0))
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                PixelTrophy(c.button, Color(0xFF5A4330), Modifier.width(80.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.clip(RoundedCornerShape(8.dp)).background(box).padding(10.dp)) {
                    Text("${d.ways}", fontSize = 26.sp, fontWeight = FontWeight.Black, color = boxInk)
                    Text(stringResource(R.string.look_trophy_ways), fontSize = 11.sp, color = boxInk, lineHeight = 13.sp)
                }
            }
            Shelf(shelf)
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                listOf(d.habits to habitsWord(d.habits), d.swaps to swapsWord(d.swaps), d.avoids to avoidsWord(d.avoids)).forEach { (n, l) ->
                    Column(Modifier.clip(RoundedCornerShape(6.dp)).background(box).padding(horizontal = 14.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$n", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = boxInk)
                        Text(l, fontSize = 10.sp, color = boxInk)
                    }
                }
            }
            Shelf(shelf)
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                (if (d.achievements.isEmpty()) listOf(null) else d.achievements).forEach { a ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        PixelMedal(c.button, Color(0xFFC0835A), 26.dp)
                        Text(a?.stepName ?: stringResource(R.string.look_trophy_next_quarter), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF3E6D0), textAlign = TextAlign.Center)
                        Text(if (a != null) achievementText(a) else stringResource(R.string.look_trophy_first_medal), fontSize = 9.5.sp, color = Color(0xFFE2D2BA), textAlign = TextAlign.Center, lineHeight = 11.sp)
                    }
                }
            }
            Shelf(shelf)
            Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("${d.onTrackPercent}%" to stringResource(R.string.look_on_track), "${d.slips}" to slipsWord(d.slips), "${d.newSteps}" to stringResource(R.string.look_new_steps), "${d.daysCheckedIn}/${d.days}" to stringResource(R.string.look_days_in)).forEach { (n, l) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF5A4330)).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(n, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = c.button)
                        Text(l, fontSize = 9.sp, color = Color(0xFFF3E6D0))
                    }
                }
            }
            Shelf(shelf)
            Label(stringResource(R.string.look_week_by_week), Color(0xFFF3E6D0))
            val books = listOf(Color(0xFFD06A5A), Color(0xFF6E9A5E), Color(0xFFD8A43A), Color(0xFF4E7398), Color(0xFFE49A6A), Color(0xFF8A6A50), Color(0xFF9CCB98), Color(0xFFC88A5A), Color(0xFF9EC5E6), Color(0xFFB04848), c.button, Color(0xFF79AE8A), Color(0xFFFFF3C4))
            WeekBars(d.weekShares, c.button, wood, 52.dp, books)
            Shelf(shelf)
        }
        if (d.added.isNotEmpty()) Text(styled(stringResource(R.string.look_trophy_new), SpanStyle(fontWeight = FontWeight.ExtraBold),
            stringResource(R.string.look_trophy_new_label), d.added.joinToString(stringResource(R.string.core_list_separator)) { it.second }), Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.panel).padding(12.dp), fontSize = 13.sp, color = c.ink)
        QuoteFooter(luck, c.ink, c.headerSub)
    }
}

@Composable
internal fun Shelf(color: Color) {
    Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(3.dp)).background(color))
}
