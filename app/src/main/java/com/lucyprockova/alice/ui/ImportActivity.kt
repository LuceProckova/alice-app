package com.lucyprockova.alice.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.lucyprockova.alice.R
import com.lucyprockova.alice.data.Backup
import com.lucyprockova.alice.data.BackupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/** replace what's on the phone, or add the file to it */
enum class ImportMode { REPLACE, ADD }

/** import, read backup, show what's in it, then replace or add to what's on this phone */
class ImportActivity : AliceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen {
            val c = Alice.colors
            var file by remember { mutableStateOf<Pair<String, BackupData>?>(null) }
            var error by remember { mutableStateOf<Int?>(null) }
            // picked file and choice saveable, so they survive android closing the app in the background
            var picked by rememberSaveable { mutableStateOf<String?>(null) }
            var mode by rememberSaveable { mutableStateOf(ImportMode.REPLACE) }
            var confirm by rememberSaveable { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            // file read and parsed off the main thread
            suspend fun load(uri: Uri) {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val name = contentResolver.query(uri, null, null, null, null)?.use { cur ->
                            val i = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (cur.moveToFirst() && i >= 0) cur.getString(i) else null
                        } ?: "backup.json"
                        val text = runCatching { contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
                            ?: throw IOException("unreadable")
                        name to Backup.fromJson(text)
                    }
                }
                result.onSuccess { file = it; error = null }
                    .onFailure { e ->
                        Log.w("Alice", "import failed", e)
                        // can't open vs opened but not ours, two different fixes for the user
                        error = if (e is IOException) R.string.forms_import_cant_open else R.string.forms_import_not_backup
                        file = null; picked = null
                    }
            }
            // after a restart read the picked file again, if it can't be read the picker just shows again
            LaunchedEffect(Unit) { picked?.let { u -> if (file == null) load(u.toUri()) } }
            val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                if (uri != null) { picked = uri.toString(); scope.launch { load(uri) } }
            }
            Column(Modifier.fillMaxSize().background(c.card)) {
                Header(stringResource(R.string.forms_import_title), back = stringResource(R.string.forms_back), onBack = ::finish)
                Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val f = file
                    if (f == null) {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(2.dp, c.line, RoundedCornerShape(14.dp)).background(c.panel)
                                // loose mime types, some file apps label .json as text or octet-stream
                                .clickable { pick.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }.padding(vertical = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Filled.UploadFile, null, Modifier.size(36.dp), tint = c.headerSub)
                            Text(stringResource(R.string.forms_import_choose), fontWeight = FontWeight.Bold, color = c.ink)
                            Text(stringResource(R.string.forms_import_choose_sub), fontSize = 11.5.sp, color = c.muted)
                        }
                        error?.let { Text(stringResource(it), color = c.missed, fontSize = 13.sp) }
                    } else {
                        Panel {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Description, null, Modifier.size(18.dp), tint = c.headerSub)
                                Text("  ${f.first}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                            }
                            Text(describeBackup(f.second), fontSize = 12.sp, color = c.muted, modifier = Modifier.padding(top = 6.dp))
                        }
                        Segmented(listOf(stringResource(R.string.forms_import_replace_mode), stringResource(R.string.forms_import_add_mode)), mode.ordinal, { mode = ImportMode.entries[it] }, fontSize = 12.sp)
                        MainButton(stringResource(R.string.forms_import_button)) { confirm = true }
                    }
                }
            }
            if (confirm) file?.let { (_, data) ->
                val replace = mode == ImportMode.REPLACE
                ConfirmDialog(
                    stringResource(if (replace) R.string.forms_import_replace_title else R.string.forms_import_add_title),
                    stringResource(if (replace) R.string.forms_import_replace_text else R.string.forms_import_add_text),
                    stringResource(R.string.forms_import_button),
                    {
                        io {
                            // replace = file only, add = merged with this phone's data, either way one transaction
                            importData(data, add = !replace)
                            // import from welcome counts as onboarded, restart at habits, fresh back stack
                            repo.settingsStore.update { it.copy(welcomed = true) }
                            Toast.makeText(this@ImportActivity, getString(R.string.forms_import_done), Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@ImportActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    },
                ) { confirm = false }
            }
        }
    }
}
