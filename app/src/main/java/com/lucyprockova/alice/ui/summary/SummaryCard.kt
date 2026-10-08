package com.lucyprockova.alice.ui.summary

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.lucyprockova.alice.R
import com.lucyprockova.alice.logic.SummaryData
import com.lucyprockova.alice.ui.*
import com.lucyprockova.alice.ui.gString
import java.time.format.DateTimeFormatter
import kotlin.random.Random

// 3-month summary, the styles list, the random bits, and the switch that picks a style
/** nine 3-month summary looks, settings order, index = style number */
val SUMMARY_STYLES = listOf(
    R.string.look_style_poster, R.string.look_style_wrapped, R.string.look_style_trophy, R.string.look_style_receipt, R.string.look_style_report,
    R.string.look_style_boarding, R.string.look_style_polaroid, R.string.look_style_calendar, R.string.look_style_trading,
)

/** where every summary qr points, friend scans it, starts own journey */
const val SHARE_LINK = "https://github.com/LuceProckova/alice-app"

// stored title case already, order matters for the seed
internal val QUOTES = listOf(
    R.string.look_quote_1, R.string.look_quote_2, R.string.look_quote_3, R.string.look_quote_4,
    R.string.look_quote_5, R.string.look_quote_6, R.string.look_quote_7, R.string.look_quote_8,
    R.string.look_quote_9, R.string.look_quote_10, R.string.look_quote_11, R.string.look_quote_12,
    R.string.look_quote_13, R.string.look_quote_14, R.string.look_quote_15, R.string.look_quote_16,
)
internal val STAMPS = listOf(
    R.string.look_stamp_1, R.string.look_stamp_2, R.string.look_stamp_3, R.string.look_stamp_4,
    R.string.look_stamp_5, R.string.look_stamp_6, R.string.look_stamp_7, R.string.look_stamp_8,
)

/** random quote, stamp, pic, seeded so refresh can reroll */
class SummaryLuck(seed: Int) {
    private val r = Random(seed)
    private val quoteRes = QUOTES[r.nextInt(QUOTES.size)]
    private val stampRes = STAMPS[r.nextInt(STAMPS.size)]
    val scene = PIXEL_SCENES[r.nextInt(PIXEL_SCENES.size)]

    // quotes can change with pronouns, so read through gString
    val quote: String @Composable get() = gString(quoteRes)
    val stamp: String @Composable get() = stringResource(stampRes)
}

@Composable internal fun habitsText(n: Int) = pluralStringResource(R.plurals.look_habits_n, n, n)
@Composable internal fun swapsText(n: Int) = pluralStringResource(R.plurals.look_swaps_n, n, n)
@Composable internal fun avoidsText(n: Int) = pluralStringResource(R.plurals.look_avoids_n, n, n)
@Composable internal fun slipsText(n: Int) = pluralStringResource(R.plurals.look_slips_n, n, n)
@Composable internal fun waysText(n: Int) = pluralStringResource(R.plurals.look_ways_n, n, n)

/** word only, number drawn apart */
@Composable internal fun habitsWord(n: Int) = pluralStringResource(R.plurals.look_habits_word, n)
@Composable internal fun swapsWord(n: Int) = pluralStringResource(R.plurals.look_swaps_word, n)
@Composable internal fun avoidsWord(n: Int) = pluralStringResource(R.plurals.look_avoids_word, n)
@Composable internal fun slipsWord(n: Int) = pluralStringResource(R.plurals.look_slips_word, n)
@Composable internal fun waysWord(n: Int) = pluralStringResource(R.plurals.look_ways_word, n)

private val MONTH get() = DateTimeFormatter.ofPattern("MMM", APP_LOCALE)

internal fun monthText(d: java.time.LocalDate): String = d.format(MONTH)

@Composable
internal fun period(d: SummaryData): String = stringResource(R.string.look_period, monthText(d.from), monthText(d.to))

internal fun quarterNo(d: SummaryData) = (d.to.monthValue - 1) / 3 + 1

/** fills %n$s from [args], %1$s gets [style] */
internal fun styled(template: String, style: SpanStyle, vararg args: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    Regex("%(\\d)\\\$s").findAll(template).forEach { m ->
        append(template.substring(last, m.range.first))
        val n = m.groupValues[1].toInt()
        if (n == 1) withStyle(style) { append(args[0]) } else append(args[n - 1])
        last = m.range.last + 1
    }
    append(template.substring(last))
}

/** draws summary in [style], every style follows theme colours */
@Composable
fun SummaryCard(style: Int, d: SummaryData, luck: SummaryLuck, number: Int = 1) {
    when (style) {
        0 -> Poster(d, luck)
        1 -> Wrapped(d, luck)
        2 -> Trophy(d, luck)
        3 -> Receipt(d, luck)
        4 -> ReportCard(d, luck)
        5 -> BoardingPass(d, luck)
        6 -> Polaroid(d, luck)
        7 -> CalendarStyle(d, luck)
        else -> TradingCard(d, luck, number)
    }
}
