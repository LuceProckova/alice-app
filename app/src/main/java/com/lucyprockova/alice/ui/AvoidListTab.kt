package com.lucyprockova.alice.ui

import com.lucyprockova.alice.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucyprockova.alice.data.Category
import com.lucyprockova.alice.data.StepKind
import com.lucyprockova.alice.data.Verdict
import com.lucyprockova.alice.logic.Snapshot

val AVOID_TAGS = listOf("Ethical", "Health", "Clutter", "Ecological", "Animal welfare", "Workers' rights", "Plastic", "Palm oil", "Deforestation")

/** type filter keys, saved in state, shown with categoryLabel */
private val TYPE_FILTERS = mapOf("Company" to Category.COMPANY, "Food" to Category.FOOD, "Product" to Category.PRODUCT)

@Composable
fun Verdict.color(): Color = when (this) { Verdict.AVOID -> Alice.colors.missed; Verdict.EDGE -> Alice.colors.spot; Verdict.FINE -> Alice.colors.kept }

/** avoid list, everything user stays away from, search + filter by type and why */
@Composable
fun AliceActivity.AvoidListTab(s: Snapshot) {
    val c = Alice.colors
    var query by rememberSaveable { mutableStateOf("") }
    var filters by rememberSaveable { mutableStateOf(setOf<String>()) }
    val typeFilters = filters.filter { it in TYPE_FILTERS }
    val tagFilters = filters - typeFilters.toSet()
    val avoids = s.steps.filter { it.kind == StepKind.AVOID }
        .filter { query.isBlank() || it.name.contains(query, true) || it.brands.contains(query, true) || it.note.contains(query, true) }
        .filter { typeFilters.isEmpty() || (it.category ?: Category.PRODUCT) in typeFilters.map { k -> TYPE_FILTERS.getValue(k) } }
        .filter { tagFilters.isEmpty() || it.tagList.any { t -> t in tagFilters } }
    Column(Modifier.fillMaxSize().background(c.card)) {
        val title = stringResource(R.string.tabs_avoid_title)
        Header(title, actions = { SettingsButton(title) }) {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth().padding(end = 8.dp, top = 4.dp), singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = c.muted) }, placeholder = { Text(stringResource(R.string.tabs_avoid_search), color = c.muted) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.button, unfocusedBorderColor = c.line, focusedContainerColor = c.card, unfocusedContainerColor = c.card, focusedTextColor = c.ink, unfocusedTextColor = c.ink),
            )
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (TYPE_FILTERS.keys + AVOID_TAGS).forEach { f ->
                val shown = TYPE_FILTERS[f]?.categoryLabel() ?: tagLabel(f)
                Pill(shown, f in filters) { filters = if (f in filters) filters - f else filters + f }
            }
        }
        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                // empty list stays blank, only search or filter with no hits says so
                if (avoids.isEmpty() && s.steps.any { it.kind == StepKind.AVOID }) item { Empty(stringResource(R.string.tabs_avoid_no_match)) }
                listOf(Category.COMPANY to R.string.tabs_avoid_companies, Category.FOOD to R.string.tabs_avoid_food, Category.PRODUCT to R.string.tabs_avoid_products).forEach { (cat, titleRes) ->
                    val list = avoids.filter { (it.category ?: Category.PRODUCT) == cat }.sortedWith(compareBy({ it.verdict.ordinal }, { it.name.lowercase() }))
                    if (list.isNotEmpty()) {
                        item { SectionLabel(stringResource(titleRes)) }
                        items(list, key = { it.id }) { step ->
                            Column {
                                Row(
                                    Modifier.fillMaxWidth().clickable { open(AvoidItemActivity::class.java, AvoidItemActivity.EXTRA_ID to step.id) }.padding(vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(step.name, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                                    Badge(step.verdict.verdictLabel(), step.verdict.color())
                                }
                                HorizontalDivider(color = c.line)
                            }
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = { open(NewStepActivity::class.java) }, containerColor = c.button, contentColor = c.buttonText,
                modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
            ) { Icon(Icons.Filled.Add, stringResource(R.string.tabs_new_step)) }
        }
    }
}
