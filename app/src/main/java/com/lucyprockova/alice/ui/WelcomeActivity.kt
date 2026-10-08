package com.lucyprockova.alice.ui

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.ui.summary.*

/** first screen on new install, name then start, or import data from other phone */
class WelcomeActivity : AliceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen { s ->
            val c = Alice.colors
            var name by remember { mutableStateOf(s.settings.name) }
            Column(Modifier.fillMaxSize().background(c.header).statusBarsPadding().navigationBarsPadding().imePadding().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.weight(1f))
                PixelPicture(PIXEL_SCENES[0], Modifier.width(180.dp).rotate(-2f).background(Color.White).padding(8.dp))
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.app_name), fontSize = 44.sp, fontWeight = FontWeight.Black, color = c.headerText)
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.look_welcome_name)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.button, unfocusedBorderColor = c.line, focusedContainerColor = c.card, unfocusedContainerColor = c.card, focusedTextColor = c.ink, unfocusedTextColor = c.ink),
                )
                Spacer(Modifier.height(14.dp))
                MainButton(stringResource(R.string.look_welcome_start), enabled = name.isNotBlank()) {
                    repo.settingsStore.update { it.copy(name = name.trim(), welcomed = true, firstEpochDay = repo.today().toEpochDay()) }
                    // habits underneath, tutorial on top, so closing tutorial lands on habits
                    startActivity(Intent(this@WelcomeActivity, MainActivity::class.java))
                    open(TutorialActivity::class.java)
                    finish()
                }
                Spacer(Modifier.height(8.dp))
                MainButton(stringResource(R.string.look_welcome_import), quiet = true) { open(ImportActivity::class.java) }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
