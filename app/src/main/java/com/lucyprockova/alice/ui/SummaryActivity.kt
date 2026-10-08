package com.lucyprockova.alice.ui

import android.Manifest
import android.content.ClipData
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.lucyprockova.alice.R
import com.lucyprockova.alice.logic.CheckIns
import com.lucyprockova.alice.logic.Snapshot
import com.lucyprockova.alice.logic.Stats
import com.lucyprockova.alice.logic.StepRules
import com.lucyprockova.alice.logic.SummaryData
import com.lucyprockova.alice.ui.summary.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** 3-month summary in picked style, share and save image buttons, opens after check-up or from insights */
class SummaryActivity : AliceActivity() {
    companion object {
        const val EXTRA_DUE = "due"
        const val EXTRA_AFTER_CHECKUP = "after"
    }

    fun summaryFor(s: Snapshot, due: Long): Pair<SummaryData, Int> {
        val cu = s.checkups.firstOrNull { it.dueEpochDay == due }
        return if (cu != null) {
            Stats.summary(StepRules.epoch(cu.fromEpochDay), StepRules.epoch(cu.dueEpochDay).minusDays(1), s) to
                (s.checkups.sortedBy { it.dueEpochDay }.indexOf(cu) + 1)
        } else {
            val next = CheckIns.nextQuarter(s, s.today.plusDays(1))
            Stats.summary(CheckIns.previousQuarter(s, next), next.minusDays(1), s) to (s.checkups.size + 1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val due = intent.getLongExtra(EXTRA_DUE, -1)
        val after = intent.getBooleanExtra(EXTRA_AFTER_CHECKUP, false)
        screen { s ->
            val c = Alice.colors
            val (data, number) = remember(s.checkups, s.steps) { summaryFor(s, due) }
            val luck = remember { SummaryLuck((due * 7 + 3).toInt()) }
            val layer = rememberGraphicsLayer()
            val scope = rememberCoroutineScope()
            val weeklyToday = s.weekly.any { it.doneEpochDay == s.today.toEpochDay() }

            // card recorded into [layer] as it draws, made into bitmap on share or save
            fun withImage(block: suspend (Bitmap) -> Unit) = scope.launch { block(layer.toImageBitmap().asAndroidBitmap()) }
            // storage permission only on android 8 and 9, newer dont need it for gallery
            val storage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) withImage { saveImage(it) }
                else Toast.makeText(this, getString(R.string.look_storage_needed), Toast.LENGTH_SHORT).show()
            }
            fun save() {
                if (Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    storage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                else withImage { saveImage(it) }
            }

            Column(Modifier.fillMaxSize().background(c.card)) {
                Box(Modifier.weight(1f).statusBarsPadding().verticalScroll(rememberScrollState())) {
                    Box(Modifier.drawWithContent { layer.record { this@drawWithContent.drawContent() }; drawLayer(layer) }) {
                        SummaryCard(s.settings.summaryStyle, data, luck, number)
                    }
                }
                Row(Modifier.navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SquareButton(Icons.Filled.IosShare, stringResource(R.string.look_share)) { withImage { share(it) } }
                    SquareButton(Icons.Filled.Download, stringResource(R.string.look_save_image)) { save() }
                    if (after && weeklyToday) MainButton(stringResource(R.string.look_next_week), Modifier.weight(1f)) { finish() }
                    else MainButton(stringResource(R.string.look_keep_going), Modifier.weight(1f)) { finish() }
                }
            }
        }
    }

    @Composable
    private fun SquareButton(icon: ImageVector, label: String, onClick: () -> Unit) {
        val c = Alice.colors
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).border(1.5.dp, c.button, RoundedCornerShape(14.dp)).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = c.ink) }
    }

    private suspend fun imageFile(bitmap: Bitmap): Uri {
        // cache file so other apps can read it via FileProvider, png made off main thread
        val f = withContext(Dispatchers.IO) {
            File(File(cacheDir, "share").apply { mkdirs() }, "alice-summary.png").apply {
                outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        return FileProvider.getUriForFile(this, "$packageName.files", f)
    }

    /** share sheet with summary png, link as caption */
    private suspend fun share(bitmap: Bitmap) {
        val uri = imageFile(bitmap)
        val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, getString(R.string.look_share_caption, SHARE_LINK))
            // lets receiving app read our FileProvider uri
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // clip data gives sheet its preview and carries read grant
        send.clipData = ClipData.newRawUri(getString(R.string.look_share_clip), uri)
        startActivity(Intent.createChooser(send, getString(R.string.look_share_chooser)))
    }

    /** summary pic into gallery, Pictures/Alice, then toast */
    private suspend fun saveImage(bitmap: Bitmap) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Alice summary ${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            // android 10+ RELATIVE_PATH, MediaStore makes folder itself
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Alice")
            else {
                // before android 10 gallery wants real file path in DATA, so make folder by hand
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Alice").apply { mkdirs() }
                @Suppress("DEPRECATION") put(MediaStore.Images.Media.DATA, File(dir, "Alice summary ${System.currentTimeMillis()}.png").path)
            }
        }
        val saved = withContext(Dispatchers.IO) {
            runCatching {
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("no gallery")
                contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: error("no stream")
            }.isSuccess
        }
        Toast.makeText(this, getString(if (saved) R.string.look_saved else R.string.look_save_failed), Toast.LENGTH_SHORT).show()
    }
}

/** summary drawn at real width then scaled down, for style pickers */
@Composable
fun ScaledSummary(style: Int, data: SummaryData, luck: SummaryLuck, width: Dp, height: Dp) {
    val full = 340.dp
    val scale = width / full
    Box(Modifier.size(width, height).clip(RoundedCornerShape(10.dp)).border(1.dp, Alice.colors.line, RoundedCornerShape(10.dp))) {
        Box(
            Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).requiredWidth(full)
                .graphicsLayer { scaleX = scale; scaleY = scale; transformOrigin = TransformOrigin(0f, 0f) },
        ) { SummaryCard(style, data, luck) }
    }
}

