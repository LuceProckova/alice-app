package com.lucyprockova.alice.logic

import com.lucyprockova.alice.data.AliceSettings
import com.lucyprockova.alice.data.PeriodUnit
import com.lucyprockova.alice.data.RepeatMode
import com.lucyprockova.alice.data.SlipLevel
import com.lucyprockova.alice.data.StepTiming
import com.lucyprockova.alice.logic.TestData.TODAY
import com.lucyprockova.alice.logic.TestData.settings
import com.lucyprockova.alice.logic.TestData.step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** step phases, targets, periods, taps, refresher */
class StepRulesTest {

    // day number starts at 1
    @Test fun dayNumberStartsAtOne() {
        assertEquals(1, StepRules.dayNumber(step(1, 0), TODAY))
        assertEquals(10, StepRules.dayNumber(step(1, 9), TODAY))
    }

    // 66th day still building, 67th settled
    @Test fun stepSettlesAfterItsBuildDays() {
        assertEquals(Phase.BUILDING, StepRules.phase(step(1, 65), settings(), TODAY))
        assertEquals(Phase.SETTLED, StepRules.phase(step(1, 66), settings(), TODAY))
        assertEquals(Phase.SETTLED, StepRules.phase(step(1, 40), settings({ it.copy(daysToBuild = 40) }), TODAY))
    }

    // one-off builds till finished
    @Test fun oneOffStaysUntilFinished() {
        val todo = step(1, 200) { it.copy(oneOff = true) }
        assertEquals(Phase.BUILDING, StepRules.phase(todo, settings(), TODAY))
        val done = todo.copy(finishedEpochDay = TODAY.toEpochDay())
        assertEquals(Phase.FINISHED, StepRules.phase(done, settings(), TODAY))
        assertTrue(StepRules.finishedToday(done, TODAY))
        assertFalse(StepRules.finishedToday(done, TODAY.plusDays(1)))
    }

    // end of day unlocks at checkin hour, anytime always open
    @Test fun endOfDayStepsUnlockAtCheckInHour() {
        val eod = step(1, 3, timing = StepTiming.END_OF_DAY)
        assertFalse(StepRules.isUnlocked(eod, 19, 20))
        assertTrue(StepRules.isUnlocked(eod, 20, 20))
        assertTrue(StepRules.isUnlocked(step(2, 3), 8, 20))
    }

    // daily target follows repeat
    @Test fun dailyTargetFollowsTheRepeat() {
        val monday = LocalDate.of(2026, 9, 28)
        assertEquals(1, StepRules.dailyTarget(step(1, 3), monday))
        assertEquals(2, StepRules.dailyTarget(step(1, 3) { it.copy(timesPerDay = 2) }, monday))
        val gym = step(1, 3) { it.copy(repeat = RepeatMode.DAYS, dayCounts = "2,0,1,0,1,0,0") }
        assertEquals(2, StepRules.dailyTarget(gym, monday))
        assertEquals(0, StepRules.dailyTarget(gym, monday.plusDays(1)))
    }

    // weekly period follows week start
    @Test fun weeklyPeriodsFollowTheWeekStart() {
        val yoga = step(1, 20) { it.copy(repeat = RepeatMode.TIMES, times = 2, unit = PeriodUnit.WEEK) }
        val p = StepRules.periodOf(yoga, TODAY, DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 9, 28), p.start)
        assertEquals(LocalDate.of(2026, 10, 4), p.end)
    }

    // 2 week periods count from creation week
    @Test fun twoWeekPeriodsCountFromTheCreationWeek() {
        val clean = step(1, 0) { it.copy(createdEpochDay = LocalDate.of(2026, 9, 21).toEpochDay(), repeat = RepeatMode.TIMES, unit = PeriodUnit.WEEK, everyN = 2) }
        assertEquals(Period(LocalDate.of(2026, 9, 21), LocalDate.of(2026, 10, 4)), StepRules.periodOf(clean, TODAY, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 10, 5), StepRules.periodOf(clean, TODAY.plusDays(2), DayOfWeek.MONDAY).start)
    }

    // month and every x days periods
    @Test fun monthAndXDayPeriods() {
        val monthly = step(1, 50) { it.copy(repeat = RepeatMode.TIMES, unit = PeriodUnit.MONTH) }
        assertEquals(Period(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)), StepRules.periodOf(monthly, TODAY, DayOfWeek.MONDAY))
        val every3 = step(1, 4) { it.copy(repeat = RepeatMode.TIMES, unit = PeriodUnit.DAYS, everyN = 3) }
        assertEquals(Period(TODAY.minusDays(1), TODAY.plusDays(1)), StepRules.periodOf(every3, TODAY, DayOfWeek.MONDAY))
    }

    // taps cycle up to target then back to 0
    @Test fun tapsCycleThroughTheTarget() {
        val vitamins = step(1, 3) { it.copy(timesPerDay = 2) }
        assertEquals(1, StepRules.nextCount(vitamins, TODAY, 0, 0))
        assertEquals(2, StepRules.nextCount(vitamins, TODAY, 1, 1))
        assertEquals(0, StepRules.nextCount(vitamins, TODAY, 2, 2))
        val yoga = step(2, 3) { it.copy(repeat = RepeatMode.TIMES, times = 2) }
        assertEquals(2, StepRules.nextCount(yoga, TODAY, 1, 1))
        assertEquals(0, StepRules.nextCount(yoga, TODAY, 1, 2)) // week full, tap clears this day
        val gymOff = step(3, 3) { it.copy(repeat = RepeatMode.DAYS, dayCounts = "1,0,0,0,0,0,0") }
        assertEquals(0, StepRules.nextCount(gymOff, TODAY, 0, 0))
    }

    // checkup answer shrinks, keeps or grows refresher
    @Test fun checkupRulesForTheRefresher() {
        val settled = step(1, 200) { it.copy(refresherDays = 30) }
        assertEquals(25, StepRules.applyCheckup(settled, SlipLevel.NEVER, TODAY, 66).refresherDays)
        assertEquals(5, StepRules.applyCheckup(settled.copy(refresherDays = 7), SlipLevel.NEVER, TODAY, 66).refresherDays)
        assertEquals(settled, StepRules.applyCheckup(settled, SlipLevel.ONCE, TODAY, 66))

        val few = StepRules.applyCheckup(settled, SlipLevel.A_FEW, TODAY, 66)
        assertEquals(TODAY.toEpochDay(), few.buildStartEpochDay)
        assertEquals(30, few.buildDays)
        assertEquals(Phase.BUILDING, StepRules.phase(few, settings(), TODAY))

        val half = StepRules.applyCheckup(settled, SlipLevel.HALF, TODAY, 66)
        assertEquals(35, half.refresherDays)
        assertEquals(35, half.buildDays)
        assertEquals(66, StepRules.applyCheckup(settled.copy(refresherDays = 64), SlipLevel.ALWAYS, TODAY, 66).refresherDays)
    }

    // own days to build 40, refresher never past 40, default still 66
    @Test fun refresherCappedByOwnBuildDays() {
        val settled = step(1, 200) { it.copy(refresherDays = 38) }
        assertEquals(40, StepRules.applyCheckup(settled, SlipLevel.ALWAYS, TODAY, 40).refresherDays)
        assertEquals(40, StepRules.applyCheckup(settled.copy(refresherDays = 40), SlipLevel.HALF, TODAY, 40).buildDays)
        val few = StepRules.applyCheckup(settled.copy(refresherDays = 55), SlipLevel.A_FEW, TODAY, 40)
        assertEquals(40, few.buildDays)
        assertEquals(40, few.refresherDays)
        assertEquals(20, StepRules.applyCheckup(settled.copy(refresherDays = 30), SlipLevel.NEVER, TODAY, 20).refresherDays)
        assertEquals(66, AliceSettings().daysToBuild)
        assertEquals(66, StepRules.applyCheckup(settled.copy(refresherDays = 64), SlipLevel.ALWAYS, TODAY, AliceSettings().daysToBuild).refresherDays)
    }

    // repeat as data, words come from ui
    @Test fun describesRepeats() {
        assertEquals(Rhythm.Days(listOf(0 to 1, 2 to 1, 4 to 1)), StepRules.rhythm(step(1, 1) { it.copy(repeat = RepeatMode.DAYS, dayCounts = "1,0,1,0,1,0,0") }))
        assertEquals(Rhythm.Times(2, PeriodUnit.WEEK, 1), StepRules.rhythm(step(1, 1) { it.copy(repeat = RepeatMode.TIMES, times = 2) }))
        assertEquals(Rhythm.Times(1, PeriodUnit.WEEK, 2), StepRules.rhythm(step(1, 1) { it.copy(repeat = RepeatMode.TIMES, everyN = 2) }))
        assertEquals(Rhythm.Daily(2), StepRules.rhythm(step(1, 1) { it.copy(timesPerDay = 2) }))
        assertEquals(Rhythm.OneOff, StepRules.rhythm(step(1, 1) { it.copy(oneOff = true) }))
        assertEquals(Rhythm.EndOfDay, StepRules.rhythm(step(1, 1) { it.copy(timing = StepTiming.END_OF_DAY) }))
        assertTrue(StepRules.rhythm(step(1, 1)).everyDay)
    }
}
