package com.lucyprockova.alice.ui.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.lucyprockova.alice.R
import com.lucyprockova.alice.logic.SummaryData
import com.lucyprockova.alice.ui.*
import kotlin.random.Random

// bits every summary style shares, qr code, bars, pixel trophies, confetti
// ---------------------------------------------------------------- shared bits

@Composable
fun QrCode(size: androidx.compose.ui.unit.Dp, color: Color) {
    val matrix = remember { QRCodeWriter().encode(SHARE_LINK, BarcodeFormat.QR_CODE, 0, 0) }
    Canvas(Modifier.size(size)) {
        val n = matrix.width
        val px = this.size.width / n
        for (y in 0 until n) for (x in 0 until n) if (matrix.get(x, y)) drawRect(color, Offset(x * px, y * px), Size(px + 0.4f, px + 0.4f))
    }
}

@Composable
internal fun ScanBlock(color: Color, mono: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        QrCode(46.dp, color)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.look_scan), fontSize = 8.5.sp, lineHeight = 11.sp, letterSpacing = 1.sp, color = color, fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default)
    }
}

@Composable
internal fun QuoteFooter(luck: SummaryLuck, ink: Color, accent: Color) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.FavoriteBorder, null, Modifier.size(28.dp), tint = accent)
        Text(luck.quote, Modifier.weight(1f).padding(horizontal = 8.dp), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = ink, lineHeight = 17.sp)
        ScanBlock(ink)
    }
}

@Composable
internal fun Label(text: String, color: Color, size: TextUnit = 9.5.sp, family: FontFamily = FontFamily.Default) {
    Text(text.uppercase(), fontSize = size, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, color = color, fontFamily = family)
}

@Composable
internal fun WeekBars(shares: List<Float>, color: Color, empty: Color, height: androidx.compose.ui.unit.Dp = 46.dp, colors: List<Color>? = null) {
    Canvas(Modifier.fillMaxWidth().height(height)) {
        if (shares.isEmpty()) return@Canvas
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (shares.size - 1)) / shares.size
        shares.forEachIndexed { i, s ->
            val h = size.height * (0.15f + 0.85f * s.coerceIn(0f, 1f))
            drawRoundRect(colors?.get(i % colors.size) ?: lerp(empty, color, 0.35f + 0.65f * s), Offset(i * (w + gap), size.height - h), Size(w, h), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
        }
    }
}

@Composable
internal fun MonthsUnder(d: SummaryData, color: Color, family: FontFamily = FontFamily.Default) {
    Row(Modifier.fillMaxWidth()) {
        Text(monthText(d.from), fontSize = 10.sp, color = color, fontFamily = family)
        Spacer(Modifier.weight(1f))
        Text(monthText(d.from.plusMonths(1)), fontSize = 10.sp, color = color, fontFamily = family)
        Spacer(Modifier.weight(1f))
        Text(monthText(d.to), fontSize = 10.sp, color = color, fontFamily = family)
    }
}

@Composable
internal fun PixelTrophy(gold: Color, base: Color, modifier: Modifier) {
    val rows = listOf(
        "GGGGGGGGGG", "GGGGGGGGGG", "G.GGGGGG.G", "G.GGGGGG.G", ".GGGGGGGG.", "..GGGGGG..",
        "....GG....", "....GG....", "...GGGG...", "..BBBBBB..",
    )
    Canvas(modifier.aspectRatio(1f)) {
        val px = size.width / 10
        rows.forEachIndexed { y, row -> row.forEachIndexed { x, ch ->
            val col = when (ch) { 'G' -> gold; 'B' -> base; else -> null }
            if (col != null) drawRect(col, Offset(x * px, y * px), Size(px + .5f, px + .5f))
        } }
        drawRect(Color.White.copy(alpha = .7f), Offset(2 * px, 1 * px), Size(px * .7f, px * 1.5f))
    }
}

@Composable
internal fun PixelMedal(gold: Color, ribbon: Color, size: androidx.compose.ui.unit.Dp) {
    val rows = listOf("R....R", ".R..R.", "..RR..", ".GGGG.", "GGGGGG", "GGGGGG", ".GGGG.")
    Canvas(Modifier.size(size)) {
        val px = this.size.width / 6
        rows.forEachIndexed { y, row -> row.forEachIndexed { x, ch ->
            val col = when (ch) { 'G' -> gold; 'R' -> ribbon; else -> null }
            if (col != null) drawRect(col, Offset(x * px, y * px), Size(px + .5f, px + .5f))
        } }
    }
}

@Composable
internal fun Confetti(seed: Int, colors: List<Color>, modifier: Modifier) {
    Canvas(modifier) {
        val r = Random(seed)
        repeat(26) {
            drawRect(colors[r.nextInt(colors.size)], Offset(r.nextFloat() * size.width, r.nextFloat() * size.height), Size(4.dp.toPx(), (4 + r.nextInt(8)).dp.toPx()))
        }
    }
}

internal fun confettiColors(c: AliceColors) = listOf(c.kept, c.missed, Color(0xFF9EC5E6), Color.White, c.headerText)

@Composable
internal fun Card(bg: Brush, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).padding(18.dp), content = content)
}

internal fun brightStops(c: AliceColors): List<Color> =
    if (c.dark) listOf(lerp(c.panel, c.button, 0.35f), lerp(c.panel, c.missed, 0.45f))
    else listOf(lerp(c.button, Color.White, 0.6f), lerp(c.button, c.missed, 0.45f).copy(alpha = .85f))

internal fun softBox(c: AliceColors): Color = if (c.dark) Color.Black.copy(alpha = .25f) else Color.White.copy(alpha = .55f)
