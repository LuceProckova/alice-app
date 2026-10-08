package com.lucyprockova.alice.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.data.Languages

/** lucy's note on translations, the not yet approved languages, and two email buttons */
class TranslationNoteActivity : AliceActivity() {
    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen {
            val c = Alice.colors
            Column(Modifier.fillMaxSize().background(c.card)) {
                Header(stringResource(R.string.lang_note_title), back = stringResource(R.string.lang_title), onBack = ::finish)
                Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp)) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.card).border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        listOf(R.string.lang_note_1, R.string.lang_note_2, R.string.lang_note_3, R.string.lang_note_4).forEach {
                            Text(stringResource(it), fontSize = 13.sp, lineHeight = 19.5.sp, color = c.ink)
                        }
                        Text(stringResource(R.string.lang_not_approved), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = c.muted)
                        // chips box scrolls on its own, 52 names would push the buttons off screen
                        FlowRow(
                            Modifier.heightIn(max = 96.dp).verticalScroll(rememberScrollState()).padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Languages.UNCHECKED.forEach { l ->
                                Text(l.native, Modifier.clip(RoundedCornerShape(99.dp)).border(1.dp, c.line, RoundedCornerShape(99.dp)).background(c.panel)
                                    .padding(horizontal = 9.dp, vertical = 4.dp), fontSize = 11.5.sp, color = c.ink)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    MainButton(stringResource(R.string.lang_report)) { mail(mistake = true) }
                    Spacer(Modifier.height(8.dp))
                    MainButton(stringResource(R.string.lang_all_correct), quiet = true) { mail(mistake = false) }
                }
            }
        }
    }

    /** opens an email to lucy, subject and language filled in, she reads the rest */
    private fun mail(mistake: Boolean) {
        val lang = Languages.of(AppLanguage.current(this)) ?: Languages.of(resources.configuration.locales[0].toLanguageTag())
        val name = lang?.let { if (it.native == it.english) it.native else "${it.native} (${it.english})" } ?: resources.configuration.locales[0].toLanguageTag()
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "?"
        val subject = getString(if (mistake) R.string.lang_mail_mistake_subject else R.string.lang_mail_correct_subject, name)
        val body = getString(if (mistake) R.string.lang_mail_mistake_body else R.string.lang_mail_correct_body) + getString(R.string.lang_mail_info, name, version)
        // mailto with subject and body in the uri, the only form every mail app reads
        val uri = Uri.parse("mailto:$MAIL?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
        try {
            startActivity(Intent(Intent.ACTION_SENDTO, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.lang_no_mail, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object { const val MAIL = "hi.it.is.luce@gmail.com" }
}
