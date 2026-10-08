package com.lucyprockova.alice.logic

import com.lucyprockova.alice.data.RepeatMode
import com.lucyprockova.alice.data.StepTiming
import com.lucyprockova.alice.logic.TestData.TODAY
import com.lucyprockova.alice.logic.TestData.crossed
import com.lucyprockova.alice.logic.TestData.kept
import com.lucyprockova.alice.logic.TestData.snapshot
import com.lucyprockova.alice.logic.TestData.step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** grid cell state per day */
class GridTest {
    private fun state(s: Snapshot, daysAgo: Long, id: Long = 1) = Grid.cell(s.steps.first { it.id == id }, TODAY.minusDays(daysAgo), s).state

    // daily step, every cell state across days
    @Test fun dailyStepDays() {
        val s = snapshot(listOf(step(1, 5)), kept(1, 1) + crossed(1, 2), checkIns = listOf(3))
        assertEquals(CellState.NONE, state(s, 6))
        assertEquals(CellState.MISSED, state(s, 3))
        assertEquals(CellState.MISSED, state(s, 2))
        assertEquals(CellState.KEPT, state(s, 1))
        assertEquals(CellState.OPEN, state(s, 0))
        assertEquals(CellState.FUTURE, state(s, -1))
    }

    // past day no checkin unfilled, too old missed
    @Test fun pastDayWithoutCheckInIsUnfilled() {
        val s = snapshot(listOf(step(1, 5)))
        assertEquals(CellState.UNFILLED, state(s, 2))
        assertEquals(CellState.MISSED, state(snapshot(listOf(step(1, 30))), 20)) // older than owed window
    }

    // end of day locked before checkin hour
    @Test fun endOfDayIsLockedBeforeCheckInTime() {
        val eod = listOf(step(1, 5, timing = StepTiming.END_OF_DAY))
        assertEquals(CellState.LOCKED, state(snapshot(eod, hour = 10), 0))
        assertEquals(CellState.OPEN, state(snapshot(eod, hour = 21), 0))
    }

    // multi tap, partial then kept
    @Test fun multiTapDayShowsPartialThenKept() {
        val vit = listOf(step(1, 5) { it.copy(timesPerDay = 2) })
        assertEquals(CellState.PARTIAL, state(snapshot(vit, kept(1, 0, count = 1)), 0))
        assertEquals(CellState.KEPT, state(snapshot(vit, kept(1, 0, count = 2)), 0))
    }

    // off day shows dot
    @Test fun offDaysShowADot() {
        // today saturday, step mondays only
        val s = snapshot(listOf(step(1, 20) { it.copy(repeat = RepeatMode.DAYS, dayCounts = "1,0,0,0,0,0,0") }))
        assertEquals(CellState.OFF, state(s, 0))
    }

    // full period turns into band
    @Test fun completedPeriodBecomesABand() {
        val yoga = listOf(step(1, 20) { it.copy(repeat = RepeatMode.TIMES, times = 2) })
        val half = snapshot(yoga, kept(1, 5))
        assertEquals(CellState.KEPT, state(half, 5))
        assertEquals(CellState.OPEN, state(half, 0))
        val full = snapshot(yoga, kept(1, 5, 3))
        assertTrue((0L..5L).all { state(full, it) == CellState.BAND })
    }

    // one-off green only on day done
    @Test fun oneOffIsGreenOnlyOnTheDayItWasDone() {
        val done = step(1, 5) { it.copy(oneOff = true, finishedEpochDay = TODAY.toEpochDay()) }
        assertEquals(CellState.KEPT, state(snapshot(listOf(done)), 0))
        assertEquals(CellState.EMPTY, state(snapshot(listOf(done)), 1))
        val todo = step(1, 5) { it.copy(oneOff = true) }
        assertEquals(CellState.OPEN, state(snapshot(listOf(todo)), 0))
    }
}
