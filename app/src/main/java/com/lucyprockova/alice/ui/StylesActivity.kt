package com.lucyprockova.alice.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.ui.summary.*

/** summary styles, nine looks side by side, tap to use */
class StylesActivity : AliceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen { s ->
            val c = Alice.colors
            var seed by rememberSaveable { mutableIntStateOf(1) }
            val data = remember(s.steps.size) { previewSummary(this@StylesActivity, s) }
            val w = (windowWidthDp() - 48.dp) / 3
            Column(Modifier.fillMaxSize().background(c.card)) {
                Header(stringResource(R.string.look_styles_title), back = stringResource(R.string.core_settings), onBack = ::finish)
                Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SUMMARY_STYLES.indices.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { i ->
                                val on = s.settings.summaryStyle == i
                                Column(
                                    Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) c.button else Color.Transparent)
                                        .clickable { repo.settingsStore.update { it.copy(summaryStyle = i) } }.padding(3.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    ScaledSummary(i, data, SummaryLuck(seed * 13 + i), w - 6.dp, (w - 6.dp) * 2f)
                                    Text(stringResource(SUMMARY_STYLES[i]), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (on) c.buttonText else c.ink, modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(stringResource(R.string.look_styles_all_pictures), icon = Icons.Filled.PhotoLibrary) { open(PicturesActivity::class.java) }
                        Pill(stringResource(R.string.look_styles_refresh), icon = Icons.Filled.Refresh) { seed++ }
                    }
                }
            }
        }
    }
}
