package com.lucyprockova.alice.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.lucyprockova.alice.AliceApp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.data.Backup
import com.lucyprockova.alice.data.BackupData
import com.lucyprockova.alice.data.StepKind
import com.lucyprockova.alice.logic.StepRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter

/** month of the first tick, e.g. oct 2026, null when nothing ticked */
private fun firstTickMonth(b: BackupData): String? =
    b.entries.minOfOrNull { it.epochDay }?.let { StepRules.epoch(it).format(DateTimeFormatter.ofPattern("MMM yyyy", APP_LOCALE)) }

/** counts per kind, e.g. 3 habits, 1 swap, 2 avoids */
@Composable
fun backupKinds(b: BackupData): String {
    val habits = b.steps.count { it.kind == StepKind.HABIT }
    val swaps = b.steps.count { it.kind == StepKind.SWAP }
    val avoids = b.steps.count { it.kind == StepKind.AVOID }
    return stringResource(
        R.string.forms_backup_kinds,
        pluralStringResource(R.plurals.forms_backup_habits, habits, habits),
        pluralStringResource(R.plurals.forms_backup_swaps, swaps, swaps),
        pluralStringResource(R.plurals.forms_backup_avoids, avoids, avoids),
    )
}

@Composable
fun describeBackup(b: BackupData): String {
    val kinds = backupKinds(b)
    return firstTickMonth(b)?.let { stringResource(R.string.forms_backup_since, kinds, it) } ?: kinds
}

/** export, everything in one .json, saved via system file picker or sent to another app */
class ExportActivity : AliceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen { s ->
            val c = Alice.colors
            var data by remember { mutableStateOf<BackupData?>(null) }
            var text by remember { mutableStateOf("") }
            val scope = rememberCoroutineScope()
            // backup and its text built once, off the main thread, not on every redraw
            LaunchedEffect(Unit) {
                val d = repo.exportData()
                text = withContext(Dispatchers.Default) { Backup.toJson(d) }
                data = d
            }
            val fileName = "alice-${s.settings.name.lowercase().ifBlank { "backup" }.replace(' ', '-')}-${s.today.format(DateTimeFormatter.ofPattern("d.M.yy"))}.json"
            val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                if (uri != null) (application as AliceApp).scope.launch {
                    // json built again at save time, android may have closed this screen while the picker was open
                    val ok = withContext(Dispatchers.IO) {
                        runCatching {
                            val json = Backup.toJson(repo.exportData())
                            contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } ?: error("no output stream")
                        }.onFailure { Log.e("Alice", "export failed", it) }.isSuccess
                    }
                    // saved only when the write really worked
                    Toast.makeText(applicationContext, if (ok) applicationContext.getString(R.string.forms_export_saved, fileName) else applicationContext.getString(R.string.forms_export_save_failed), Toast.LENGTH_SHORT).show()
                }
            }
            Column(Modifier.fillMaxSize().background(c.card)) {
                Header(stringResource(R.string.forms_export_title), back = stringResource(R.string.forms_back_settings), onBack = ::finish)
                Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Panel {
                        Text(stringResource(R.string.forms_export_what), fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = c.ink)
                        data?.let { d ->
                            val since = firstTickMonth(d)
                            listOf(
                                backupKinds(d),
                                since?.let { stringResource(R.string.forms_export_ticks_since, it) } ?: stringResource(R.string.forms_export_ticks),
                                stringResource(R.string.forms_export_summaries),
                                stringResource(R.string.forms_export_settings),
                            ).forEach { line ->
                                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = c.kept)
                                    Text("  $line", fontSize = 12.5.sp, color = c.ink)
                                }
                            }
                        }
                    }
                    Panel {
                        Text(stringResource(R.string.forms_export_file), fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = c.ink)
                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Description, null, Modifier.size(18.dp), tint = c.headerSub)
                            Text("  $fileName", Modifier.weight(1f), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                            Text(stringResource(R.string.forms_export_kb, (text.length / 1024).coerceAtLeast(1)), fontSize = 11.sp, color = c.muted)
                        }
                    }
                    MainButton(stringResource(R.string.forms_export_save), enabled = data != null) { save.launch(fileName) }
                    MainButton(stringResource(R.string.forms_export_send), quiet = true, enabled = data != null) {
                        scope.launch {
                            // other apps can't read our files, so cache/share + FileProvider uri, authority must match manifest, file written off main thread
                            val f = withContext(Dispatchers.IO) { File(File(cacheDir, "share").apply { mkdirs() }, fileName).apply { writeText(text) } }
                            val uri = FileProvider.getUriForFile(this@ExportActivity, "$packageName.files", f)
                            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), getString(R.string.forms_export_send_title)))
                        }
                    }
                }
            }
        }
    }
}
