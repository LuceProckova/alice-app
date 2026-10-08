package com.lucyprockova.alice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lucyprockova.alice.logic.Cell
import com.lucyprockova.alice.logic.CellState
import java.time.LocalDate

// android studio previews of the building blocks, light and a dark theme, not used by the app itself

/** all day square states in a row */
@Composable
private fun SquaresRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(CellState.KEPT, CellState.MISSED, CellState.PARTIAL, CellState.OPEN, CellState.UNFILLED, CellState.LOCKED, CellState.OFF, CellState.FUTURE)
            .forEach { DaySquare(Cell(LocalDate.of(2026, 10, 5), it, count = 1)) }
    }
}

/** buttons, segments, pills, stepper and squares on one card */
@Composable
private fun Blocks() {
    Column(Modifier.background(Alice.colors.card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MainButton("Add habit") {}
        MainButton("No new step this week", quiet = true) {}
        Segmented(listOf("Habit", "Swap", "Avoid"), 0, {})
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Pill("Ethical", on = true) {}; Pill("Health") {} }
        Stepper(2, "times every") {}
        SquaresRow()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { BigStat("41", "days going", Modifier.weight(1f)); BigStat("90%", "success", Modifier.weight(1f)) }
    }
}

@Preview(name = "blocks, light", widthDp = 360)
@Composable
private fun BlocksLight() = AliceTheme(0) { Blocks() }

@Preview(name = "blocks, dark", widthDp = 360)
@Composable
private fun BlocksDark() = AliceTheme(6) { Blocks() }

@Preview(name = "header", widthDp = 360)
@Composable
private fun HeaderPreview() = AliceTheme(0) { Header("Alice", actions = { SettingsButton("Habits") }) }

@Preview(name = "bottom bar", widthDp = 360)
@Composable
private fun BottomNavPreview() = AliceTheme(0) { BottomNav(Tab.HABITS) {} }
