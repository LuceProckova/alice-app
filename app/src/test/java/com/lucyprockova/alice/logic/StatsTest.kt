package com.lucyprockova.alice.logic

import com.lucyprockova.alice.data.CheckupAnswer
import com.lucyprockova.alice.data.Checkup
import com.lucyprockova.alice.data.SlipLevel
import com.lucyprockova.alice.data.StepKind
import com.lucyprockova.alice.logic.TestData.TODAY
import com.lucyprockova.alice.logic.TestData.crossed
import com.lucyprockova.alice.logic.TestData.kept
import com.lucyprockova.alice.logic.TestData.snapshot
import com.lucyprockova.alice.logic.TestData.step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** habit figures, streaks, period and summary stats */
class StatsTest {

    // habit figures and streaks
    @Test fun habitFiguresAndStreaks() {
        // made 9 days ago, kept 9,8,7, crossed 6, kept 5..1, today open
        val s = snapshot(listOf(step(1, 9)), kept(1, 9, 8, 7, 5, 4, 3, 2, 1) + crossed(1, 6, "Too tired"), checkIns = (1L..9L).toList())
        val h = Stats.habit(s.steps[0], s)
        assertEquals(10, h.daysGoing)
        assertEquals(1, h.timesBroken)
        assertEquals(89, h.successPercent) // 8 of 9 finished days
        assertEquals(5, h.currentStreak)
        assertEquals(5, h.longestStreak)
        assertEquals(listOf("Too tired" to 1), h.slipReasons)
    }

    // checkup slips land as one red block at end
    @Test fun checkupSlipsShowAsOneRedBlockAtTheEnd() {
        val due = LocalDate.of(2026, 10, 1)
        val from = LocalDate.of(2026, 7, 1)
        val settled = step(1, 300)
        val s = snapshot(
            listOf(settled),
            checkups = listOf(Checkup(due.toEpochDay(), from.toEpochDay(), due.toEpochDay())),
            answers = listOf(CheckupAnswer(due.toEpochDay(), 1, SlipLevel.HALF)),
        )
        assertTrue(Stats.keptOn(settled, from, s) == true)
        assertFalse(Stats.keptOn(settled, due.minusDays(1), s) == true)
        val share = Stats.keptShare(settled, from, due.minusDays(1), s)!!
        assertEquals(0.5f, share, 0.02f)
    }

    // period counts new steps and done one-offs
    @Test fun periodCountsNewStepsAndOneOffs() {
        val s = snapshot(listOf(
            step(1, 2),
            step(2, 40),
            step(3, 3, kind = StepKind.SWAP) { it.copy(oneOff = true, finishedEpochDay = TODAY.minusDays(1).toEpochDay()) },
        ))
        val p = Stats.period(TODAY.minusDays(6), TODAY, s)
        assertEquals(2, p.newSteps)
        assertEquals(1, p.oneOffsDone)
    }

    // summary counts every kind
    @Test fun summaryCountsEveryKind() {
        val s = snapshot(listOf(step(1, 120), step(2, 100, kind = StepKind.SWAP), step(3, 90, kind = StepKind.AVOID), step(4, 5) { it.copy(oneOff = true) }))
        val sum = Stats.summary(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30), s)
        assertEquals(3, sum.ways) // unfinished one-off not counted yet
        assertEquals(1, sum.habits)
        assertEquals(1, sum.swaps)
        assertEquals(1, sum.avoids)
        assertEquals(92, sum.dailyShare.size)
        assertEquals(14, sum.weekShares.size)
    }

    // week glance picks easiest and hardest step, today open not counted
    @Test fun weekGlanceEasiestAndHardest() {
        // mon to fri checked in, step 1 kept all five, step 2 only wed and thu
        val s = snapshot(listOf(step(1, 10), step(2, 10)), kept(1, 1, 2, 3, 4, 5) + kept(2, 2, 3), checkIns = (1L..5L).toList())
        val g = Stats.glance(TODAY.minusDays(5), s)
        assertEquals(GlanceStep("Step 1", 5, 5), g.easiest)
        assertEquals(GlanceStep("Step 2", 2, 5), g.hardest)
        assertEquals(5, g.daysCheckedIn)
    }
}
