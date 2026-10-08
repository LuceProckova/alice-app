package com.lucyprockova.alice.ui

import android.app.TaskStackBuilder
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.data.Language
import com.lucyprockova.alice.data.Languages

/** language picker, phone's language, english, the 3 lucy checked, then the rest, search by native or english name */
class LanguageActivity : AliceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen {
            val c = Alice.colors
            var query by rememberSaveable { mutableStateOf("") }
            val picked = Languages.of(AppLanguage.current(this))
            val phoneRow = Language("", stringResource(R.string.lang_phone), stringResource(R.string.lang_follow))
            val rows = (listOf(phoneRow) + Languages.ALL).filter { Languages.matches(it, query) }
            Column(Modifier.fillMaxSize().background(c.card).imePadding()) {
                Header(stringResource(R.string.lang_title), back = stringResource(R.string.core_settings), onBack = ::finish)
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.spot)
                            .clickable { open(TranslationNoteActivity::class.java) }.padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Info, null, Modifier.size(20.dp), tint = c.ink)
                        Text(stringResource(R.string.lang_spotted), Modifier.weight(1f).padding(start = 10.dp), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Icon(chevronForward, null, tint = c.ink)
                    }
                    Spacer(Modifier.size(8.dp))
                    SearchBox(query) { query = it }
                }
                LazyColumn(Modifier.weight(1f).navigationBarsPadding().padding(horizontal = 16.dp)) {
                    items(rows, key = { it.tag.ifEmpty { "phone" } }) { l ->
                        val tag = l.tag.ifEmpty { null }
                        LanguageRow(l, on = picked?.tag == tag) { choose(tag) }
                    }
                }
            }
        }
    }

    /** applies straight away, android 13+ redraws every screen itself, older ones are restarted where the user was */
    private fun choose(tag: String?) {
        AppLanguage.set(this, tag)
        if (Build.VERSION.SDK_INT < 33) {
            TaskStackBuilder.create(this)
                .addNextIntent(Intent(this, MainActivity::class.java))
                .addNextIntent(Intent(this, SettingsActivity::class.java))
                .addNextIntent(Intent(this, LanguageActivity::class.java))
                .startActivities()
            finishAffinity()
        }
    }

    @Composable
    private fun SearchBox(query: String, onChange: (String) -> Unit) {
        val c = Alice.colors
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.card).border(1.dp, c.line, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, null, Modifier.size(19.dp), tint = c.muted)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                query, onChange, Modifier.weight(1f), singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = c.ink), cursorBrush = SolidColor(c.ink),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text(stringResource(R.string.lang_search), fontSize = 13.sp, color = c.muted)
                    inner()
                },
            )
        }
    }

    @Composable
    private fun LanguageRow(l: Language, on: Boolean, onClick: () -> Unit) {
        val c = Alice.colors
        Column {
            Row(
                Modifier.fillMaxWidth().selectable(selected = on, role = Role.RadioButton, onClick = onClick).padding(horizontal = 4.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(l.native, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                // english row has no second name, like the mockup
                Text(if (l.tag == "en") "" else l.english, Modifier.weight(1f).padding(start = 10.dp), fontSize = 12.sp, color = c.muted)
                Icon(Icons.Filled.Check, null, Modifier.size(20.dp), tint = if (on) c.headerText else c.card.copy(alpha = 0f))
            }
            HorizontalDivider(color = c.line)
        }
    }
}
