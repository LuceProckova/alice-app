package com.lucyprockova.alice.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucyprockova.alice.R
import com.lucyprockova.alice.data.SlipReason

/** slip reasons, tags offered when step crossed, use count, add + delete */
class ReasonsActivity : AliceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen { s ->
            val c = Alice.colors
            val reasons by repo.reasons.collectAsStateWithLifecycle(emptyList())
            var text by remember { mutableStateOf("") }
            var deleting by remember { mutableStateOf<SlipReason?>(null) }
            val used = remember(s.entries) { s.entries.mapNotNull { it.reason }.groupingBy { it }.eachCount() }
            Column(Modifier.fillMaxSize().background(c.card)) {
                Header(stringResource(R.string.forms_reasons_title), back = stringResource(R.string.forms_back_settings), onBack = ::finish)
                Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            text, { text = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text(stringResource(R.string.forms_reasons_hint), color = c.muted) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.button, unfocusedBorderColor = c.line, focusedTextColor = c.ink, unfocusedTextColor = c.ink),
                        )
                        Spacer(Modifier.size(8.dp))
                        Icon(Icons.Filled.Add, stringResource(R.string.forms_add), Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(c.button).clickable {
                            if (text.isNotBlank() && reasons.none { it.name.equals(text.trim(), true) }) { io { addReason(text) }; text = "" }
                        }.padding(12.dp), tint = c.buttonText)
                    }
                    Spacer(Modifier.height(8.dp))
                    reasons.sortedByDescending { used[it.name] ?: 0 }.forEach { r ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(iconFor(r.icon), null, Modifier.size(20.dp), tint = c.headerSub)
                            Text(r.name, Modifier.weight(1f).padding(start = 10.dp), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = c.ink)
                            Text(stringResource(R.string.forms_reasons_used, used[r.name] ?: 0), fontSize = 11.sp, color = c.muted)
                            Icon(Icons.Filled.Delete, stringResource(R.string.forms_delete), Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).clickable { deleting = r }.padding(7.dp), tint = c.muted)
                        }
                        HorizontalDivider(color = c.line)
                    }
                }
            }
            deleting?.let { r ->
                ConfirmDialog(stringResource(R.string.forms_reasons_delete_title, r.name), stringResource(R.string.forms_reasons_delete_text), stringResource(R.string.forms_delete), { io { deleteReason(r) } }) { deleting = null }
            }
        }
    }
}
