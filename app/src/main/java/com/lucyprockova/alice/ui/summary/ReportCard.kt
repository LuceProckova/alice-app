package com.lucyprockova.alice.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.logic.SummaryData
import com.lucyprockova.alice.ui.*
import com.lucyprockova.alice.ui.achievementText
import com.lucyprockova.alice.ui.gString
// ---------------------------------------------------------------- 5. report card

@Composable
internal fun ReportCard(d: SummaryData, luck: SummaryLuck) {
    val c = Alice.colors
    val serif = FontFamily.Serif
    val red = lerp(c.missed, Color(0xFF9A3B30), .4f)
    val grade = { p: Int -> when { p >= 95 -> "A+"; p >= 85 -> "A"; p >= 70 -> "B"; p >= 50 -> "C"; else -> "D" } }
    Column(Modifier.fillMaxWidth().background(c.card).padding(12.dp)) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (c.dark) c.panel else Color(0xFFFCF8EA)).border(1.dp, c.line, RoundedCornerShape(10.dp)).padding(18.dp)) {
            Text(stringResource(R.string.look_report_title), fontFamily = serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.ink)
            Text(stringResource(R.string.look_report_pupil, period(d), d.name), fontFamily = serif, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = c.ink)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
                Text(stringResource(R.string.look_report_subject), Modifier.weight(1f), fontFamily = serif, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp, color = c.ink)
                Text(stringResource(R.string.look_report_built), Modifier.width(60.dp), fontFamily = serif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = c.ink)
                Text(stringResource(R.string.look_report_grade), Modifier.width(50.dp), fontFamily = serif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = c.ink)
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(c.ink))
            listOf(Triple(stringResource(R.string.look_habits), d.habits, grade(d.onTrackPercent)), Triple(stringResource(R.string.look_swaps), d.swaps, grade((d.onTrackPercent + 10).coerceAtMost(100))),
                Triple(stringResource(R.string.look_avoids), d.avoids, grade(d.onTrackPercent)), Triple(stringResource(R.string.look_ways_improved), d.ways, grade(d.onTrackPercent))).forEachIndexed { i, (s, n, g) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s, Modifier.weight(1f), fontFamily = serif, fontSize = 15.sp, fontWeight = if (i == 3) FontWeight.Bold else FontWeight.Normal, color = c.ink)
                    Text("$n", Modifier.width(60.dp), fontFamily = serif, fontSize = 15.sp, color = c.ink)
                    Text(g, Modifier.width(50.dp), fontFamily = FontFamily.Cursive, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = red)
                }
                Text("- ".repeat(50), maxLines = 1, fontSize = 8.sp, color = c.line)
            }
            val bold = SpanStyle(fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(styled(stringResource(R.string.look_report_attendance), bold, "${d.daysCheckedIn}/${d.days}"), Modifier.weight(1f), fontFamily = serif, fontSize = 12.5.sp, color = c.ink)
                Text(styled(stringResource(R.string.look_report_on_track), bold, "${d.onTrackPercent}%"), Modifier.weight(1f), fontFamily = serif, fontSize = 12.5.sp, color = c.ink)
                Text(styled(stringResource(R.string.look_report_slips), bold, "${d.slips}"), fontFamily = serif, fontSize = 12.5.sp, color = c.ink)
            }
            if (d.achievements.isNotEmpty()) {
                Text(stringResource(R.string.look_report_achievements), fontFamily = serif, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, letterSpacing = 1.sp, color = c.ink, modifier = Modifier.padding(top = 6.dp))
                d.achievements.forEach { a ->
                    Text(styled(stringResource(R.string.look_report_achievement), bold, a.stepName, achievementText(a)), fontFamily = serif, fontSize = 14.sp, color = c.ink)
                }
            }
            if (d.added.isNotEmpty()) {
                Text(stringResource(R.string.look_report_new, d.added.size), fontFamily = serif, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, letterSpacing = 1.sp, color = c.ink, modifier = Modifier.padding(top = 10.dp))
                Text(d.added.joinToString(stringResource(R.string.core_list_separator)) { it.second }, fontFamily = serif, fontSize = 14.sp, color = c.ink)
            }
            val star = d.achievements.firstOrNull()?.stepName
            Text(styled(stringResource(R.string.look_report_comment), bold,
                stringResource(R.string.look_report_comment_label),
                gString(if (d.onTrackPercent >= 70) R.string.look_report_calm else R.string.look_report_ups_downs),
                if (star != null) stringResource(R.string.look_report_star, star) else stringResource(R.string.look_report_keep_up),
            ), fontFamily = serif, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = c.ink, modifier = Modifier.padding(top = 10.dp))
            Text(stringResource(R.string.look_report_week), fontFamily = serif, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, letterSpacing = 1.sp, color = c.ink, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            WeekBars(d.weekShares, red.copy(alpha = .55f), red.copy(alpha = .55f), 40.dp, List(13) { red.copy(alpha = .5f) })
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                ScanBlock(c.ink)
                Spacer(Modifier.weight(1f))
                Text(luck.stamp, Modifier.rotate(-12f).border(3.dp, red, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.5.sp, color = red)
            }
        }
    }
}
