package com.lucyprockova.alice.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.data.IdeaGroup
import com.lucyprockova.alice.data.Ideas

/** all 300 ideas for one step kind, searchable, pick one = fills name */
class IdeasActivity : AliceActivity() {
    companion object {
        const val EXTRA_GROUP = "group"
        const val RESULT_NAME = "ideaName"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val start = intent.getStringExtra(EXTRA_GROUP)?.let { runCatching { IdeaGroup.valueOf(it) }.getOrNull() }
        screen { s ->
            val c = Alice.colors
            var group by rememberSaveable { mutableStateOf(start) }
            var query by rememberSaveable { mutableStateOf("") }
            var why by remember { mutableStateOf<String?>(null) }
            val all = Ideas.all(this).filter { it.name !in s.settings.hiddenIdeas }
            val list = all.filter { (group == null || it.group == group) && (query.isBlank() || it.name.contains(query, true) || it.why.contains(query, true)) }
            Column(Modifier.fillMaxSize().background(c.card)) {
                Header(stringResource(R.string.forms_ideas_title), back = stringResource(R.string.forms_back), onBack = ::finish)
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = c.muted) }, placeholder = { Text(stringResource(R.string.forms_search), color = c.muted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.button, unfocusedBorderColor = c.line, focusedTextColor = c.ink, unfocusedTextColor = c.ink),
                    )
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
                        Pill(stringResource(R.string.forms_all), group == null) { group = null }
                        IdeaGroup.entries.forEach { g -> Spacer(Modifier.size(6.dp)); Pill(g.title, group == g) { group = g } }
                    }
                }
                LazyColumn(Modifier.weight(1f).navigationBarsPadding().padding(horizontal = 16.dp)) {
                    items(list, key = { it.group.name + it.name }) { idea ->
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { returnIdea(idea.name) }.padding(vertical = 10.dp)) {
                                    Text(idea.name, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                                    if (group == null) Text(idea.group.title, fontSize = 10.5.sp, color = c.muted)
                                }
                                Icon(Icons.Filled.Visibility, stringResource(R.string.forms_why), Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).clickable { why = if (why == idea.name) null else idea.name }.padding(9.dp), tint = c.muted)
                                Icon(Icons.Filled.Close, stringResource(R.string.forms_never_show), Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).clickable {
                                    repo.settingsStore.update { it.copy(hiddenIdeas = it.hiddenIdeas + idea.name) }
                                }.padding(9.dp), tint = c.muted)
                            }
                            if (why == idea.name) Text(idea.why, fontSize = 12.sp, color = c.muted, modifier = Modifier.padding(bottom = 10.dp))
                            HorizontalDivider(color = c.line)
                        }
                    }
                }
            }
        }
    }
}
