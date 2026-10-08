package com.lucyprockova.alice.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lucyprockova.alice.R
import com.lucyprockova.alice.data.StepKind

/** add step from + button, or edit one when given [EXTRA_EDIT_ID] */
class NewStepActivity : AliceActivity() {
    companion object { const val EXTRA_EDIT_ID = "editStepId" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val editId = intent.getLongExtra(EXTRA_EDIT_ID, -1)
        screen { s ->
            val c = Alice.colors
            val old = s.steps.firstOrNull { it.id == editId }
            if (editId != -1L && old == null) { finish(); return@screen }
            var draft by rememberSaveable { mutableStateOf(old?.let { StepDraft.from(it) } ?: StepDraft()) }
            Column(Modifier.fillMaxSize().background(c.card).imePadding()) {
                Header(if (old == null) stringResource(R.string.forms_new_title) else stringResource(R.string.forms_edit_title, old.kind.titleWord()), back = stringResource(if (old == null) R.string.forms_back_habits else R.string.forms_back), onBack = ::finish)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    NewStepForm(draft, repo.settingsStore, s.settings, editing = old != null) { draft = it }
                    Spacer(Modifier.height(16.dp))
                }
                if (old == null) MainButton(stringResource(R.string.forms_add_kind, draft.kind.titleWord()), Modifier.navigationBarsPadding().padding(16.dp), enabled = draft.valid) {
                    io { addStep(draft.toStep(s.today)); finish() }
                } else MainButton(stringResource(R.string.forms_save_changes), Modifier.navigationBarsPadding().padding(16.dp), enabled = draft.valid) {
                    io { edit(old, draft.editOf(old, s.today)); finish() }
                }
            }
        }
    }
}

/** kind word inside add and edit titles, lower case in english, capital noun after a colon elsewhere */
@Composable
private fun StepKind.titleWord(): String = stringResource(
    when (this) { StepKind.HABIT -> R.string.forms_title_habit; StepKind.SWAP -> R.string.forms_title_swap; StepKind.AVOID -> R.string.forms_title_avoid },
)
