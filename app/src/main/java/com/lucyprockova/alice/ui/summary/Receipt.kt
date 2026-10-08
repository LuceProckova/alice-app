package com.lucyprockova.alice.ui.summary

import com.lucyprockova.alice.ui.achievementText
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.logic.SummaryData
import com.lucyprockova.alice.ui.*
import kotlin.random.Random
// ---------------------------------------------------------------- 4. receipt

@Composable
internal fun Receipt(d: SummaryData, luck: SummaryLuck) {
    val c = Alice.colors
    val paper = if (c.dark) c.panel else Color(0xFFFFFAEC)
    val mono = FontFamily.Monospace
    @Composable fun line(l: String, r: String, bold: Boolean = false) = Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(l, Modifier.weight(1f), fontFamily = mono, fontSize = 12.5.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = c.ink)
        Text(r, fontFamily = mono, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = c.ink)
    }
    @Composable fun dash() = Text("- ".repeat(40), maxLines = 1, fontFamily = mono, fontSize = 10.sp, color = c.muted, modifier = Modifier.padding(vertical = 4.dp))
    Column(Modifier.fillMaxWidth().background(c.card).padding(12.dp)) {
        Column(Modifier.fillMaxWidth().background(paper).border(1.dp, c.line).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("A L I C E", fontFamily = mono, fontWeight = FontWeight.Black, fontSize = 22.sp, color = c.ink)
            Text(stringResource(R.string.look_receipt_title, period(d)), fontFamily = mono, fontSize = 11.sp, color = c.ink)
            Text(stringResource(R.string.look_receipt_customer, d.name), fontFamily = mono, fontSize = 11.sp, color = c.ink, modifier = Modifier.padding(top = 4.dp))
            dash()
            line(stringResource(R.string.look_receipt_habits), "${d.habits}"); line(stringResource(R.string.look_receipt_swaps), "${d.swaps}"); line(stringResource(R.string.look_receipt_avoids), "${d.avoids}")
            dash()
            line(stringResource(R.string.look_receipt_ways), "${d.ways}", bold = true)
            dash()
            line(stringResource(R.string.look_receipt_on_track), "${d.onTrackPercent}%"); line(stringResource(R.string.look_receipt_slips), "${d.slips}"); line(stringResource(R.string.look_receipt_new_steps), "${d.newSteps}")
            dash()
            d.achievements.forEach { line("★ ${it.stepName}", achievementText(it)) }
            if (d.achievements.isNotEmpty()) dash()
            if (d.added.isNotEmpty()) Text(stringResource(R.string.look_receipt_new, d.added.joinToString(stringResource(R.string.core_list_separator)) { it.second }), fontFamily = mono, fontSize = 11.5.sp, color = c.ink, modifier = Modifier.fillMaxWidth())
            Text("${luck.quote} ✦", fontFamily = mono, fontSize = 12.sp, color = c.ink, modifier = Modifier.padding(vertical = 10.dp))
            Canvas(Modifier.width(200.dp).height(44.dp)) {
                val r = Random(d.ways * 31 + d.days)
                var x = 0f
                while (x < size.width) { val w = (1 + r.nextInt(3)).dp.toPx(); drawRect(c.ink, Offset(x, 0f), Size(w, size.height)); x += w + (1 + r.nextInt(3)).dp.toPx() }
            }
            Spacer(Modifier.height(8.dp))
            ScanBlock(c.ink)
        }
    }
}
