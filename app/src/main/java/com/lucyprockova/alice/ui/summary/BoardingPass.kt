package com.lucyprockova.alice.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.logic.SummaryData
import com.lucyprockova.alice.ui.*
import com.lucyprockova.alice.ui.achievementHeadline
import com.lucyprockova.alice.ui.gString
// ---------------------------------------------------------------- 6. boarding pass

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BoardingPass(d: SummaryData, luck: SummaryLuck) {
    val c = Alice.colors
    val gold = lerp(c.button, Color(0xFFB98B17), .3f)
    @Composable fun RowScope.field(l: String, v: String) = Column(Modifier.weight(1f).padding(vertical = 5.dp)) {
        Text(l.uppercase(), fontSize = 9.5.sp, letterSpacing = 1.sp, color = c.muted)
        Text(v, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
    }
    Column(Modifier.fillMaxWidth().background(c.card).padding(12.dp)) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, c.line, RoundedCornerShape(16.dp))) {
            Row(Modifier.fillMaxWidth().background(c.panel).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Flight, null, Modifier.size(18.dp).rotate(90f), tint = gold)
                Text(" " + stringResource(R.string.look_boarding_airline), fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.5.sp, color = gold, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.look_boarding_flight, quarterNo(d)), fontWeight = FontWeight.Black, fontSize = 15.sp, color = gold)
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.look_boarding_from), fontSize = 10.sp, color = c.muted); Text(stringResource(R.string.look_boarding_old), fontSize = 34.sp, fontWeight = FontWeight.Black, color = c.ink); Text(stringResource(R.string.look_boarding_habits), fontSize = 12.sp, color = c.muted)
                    }
                    Icon(Icons.Filled.Flight, null, Modifier.weight(1f).size(30.dp).rotate(90f), tint = gold)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.look_boarding_to), fontSize = 10.sp, color = c.muted); Text(stringResource(R.string.look_boarding_new), fontSize = 34.sp, fontWeight = FontWeight.Black, color = c.ink); Text(gString(R.string.look_boarding_better_you), fontSize = 12.sp, color = c.muted)
                    }
                }
                Row { field(stringResource(R.string.look_boarding_passenger), d.name.ifBlank { stringResource(R.string.look_you) }); field(stringResource(R.string.look_boarding_dates), period(d)); field(stringResource(R.string.look_boarding_miles), waysText(d.ways)) }
                Row { field(stringResource(R.string.look_boarding_on_time), "${d.onTrackPercent}%"); field(stringResource(R.string.look_boarding_turbulence), slipsText(d.slips)); field(stringResource(R.string.look_boarding_days), "${d.daysCheckedIn}/${d.days}") }
                Text("- ".repeat(60), maxLines = 1, fontSize = 9.sp, color = c.line)
                Row { field(stringResource(R.string.look_habits), "${d.habits}"); field(stringResource(R.string.look_swaps), "${d.swaps}"); field(stringResource(R.string.look_avoids), "${d.avoids}") }
            }
            Column(Modifier.fillMaxWidth().background(c.panel).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (d.achievements.isNotEmpty()) {
                    Text(stringResource(R.string.look_boarding_flyer), fontSize = 9.5.sp, letterSpacing = 1.sp, color = c.muted)
                    d.achievements.forEach { a ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Flight, null, Modifier.size(15.dp), tint = gold)
                            Text(" " + a.stepName, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = c.ink, modifier = Modifier.weight(1f))
                            Text(achievementHeadline(a), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = c.ink)
                        }
                    }
                }
                Text(stringResource(R.string.look_boarding_log), fontSize = 9.5.sp, letterSpacing = 1.sp, color = c.muted, modifier = Modifier.padding(top = 4.dp))
                WeekBars(d.weekShares, c.ink, c.ink, 40.dp, List(13) { c.ink.copy(alpha = .75f) })
                MonthsUnder(d, c.muted)
                if (d.added.isNotEmpty()) {
                    Text(stringResource(R.string.look_boarding_stops, d.added.size), fontSize = 9.5.sp, letterSpacing = 1.sp, color = c.muted, modifier = Modifier.padding(top = 4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        d.added.forEach { (_, n) -> Text(n, fontSize = 11.5.sp, color = c.ink, modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, c.line, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp)) }
                    }
                }
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.look_boarding_note), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = c.muted)
                        Text(luck.quote, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
                    }
                    ScanBlock(c.ink)
                }
            }
        }
    }
}
