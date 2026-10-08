package com.lucyprockova.alice.logic

import com.lucyprockova.alice.data.Checkup
import com.lucyprockova.alice.data.StepTiming
import com.lucyprockova.alice.data.WeeklyDone
import com.lucyprockova.alice.logic.TestData.TODAY
import com.lucyprockova.alice.logic.TestData.kept
import com.lucyprockova.alice.logic.TestData.settings
import com.lucyprockova.alice.logic.TestData.snapshot
import com.lucyprockova.alice.logic.TestData.step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** checkin due vs still owed */
class CheckInsTest {

    // owed days stack up, today waits for checkin hour
    @Test fun owedDaysStackUpAndTodayWaitsForTheCheckInTime() {
        val steps = listOf(step(1, 30, timing = StepTiming.END_OF_DAY))
        val morning = snapshot(steps, checkIns = (3L..20L).toList(), hour = 9)
        assertEquals(listOf(TODAY.minusDays(2), TODAY.minusDays(1)), CheckIns.owedDays(morning))
        val evening = snapshot(steps, checkIns = (3L..20L).toList(), hour = 21)
        assertEquals(3, CheckIns.owedDays(evening).size)
    }

    // all ticked, nothing owed
    @Test fun dayIsDoneWhenEverythingIsTicked() {
        val s = snapshot(listOf(step(1, 30)), kept(1, 2, 1), checkIns = (3L..20L).toList())
        assertTrue(CheckIns.owedDays(s).isEmpty())
    }

    // periodic and one-off steps skip daily checkin
    @Test fun periodicAndOneOffStepsAreNotAskedDaily() {
        val s = snapshot(listOf(step(1, 30) { it.copy(oneOff = true) }, step(2, 30) { it.copy(repeat = com.lucyprockova.alice.data.RepeatMode.TIMES) }))
        assertTrue(CheckIns.owedDays(s).isEmpty())
    }

    // weekly checkin every n weeks
    @Test fun weeklyCheckInsEveryNWeeks() {
        // first use mon 7 sep 2026, checkin on sundays
        val st = settings({ it.copy(firstEpochDay = LocalDate.of(2026, 9, 7).toEpochDay(), weeklyDay = 7) })
        val s = snapshot(emptyList(), settings = st)
        assertEquals(listOf(13, 20, 27).map { LocalDate.of(2026, 9, it) }, CheckIns.weeklyDueDates(s))
        val every2 = snapshot(emptyList(), settings = st.copy(newStepEveryWeeks = 2))
        assertEquals(listOf(13, 27).map { LocalDate.of(2026, 9, it) }, CheckIns.weeklyDueDates(every2))
    }

    // owed weeklies counted only after last done
    @Test fun owedWeeklyMergesAfterTheLastDone() {
        val st = settings({ it.copy(firstEpochDay = LocalDate.of(2026, 9, 7).toEpochDay(), weeklyDay = 7) })
        val s = snapshot(emptyList(), settings = st, weekly = listOf(WeeklyDone(LocalDate.of(2026, 9, 13).toEpochDay(), 0)))
        assertEquals(2, CheckIns.owedWeekly(s).size)
    }

    // shared checkup on quarter starts
    @Test fun sharedCheckupDays() {
        val s = snapshot(emptyList())
        assertEquals(LocalDate.of(2027, 1, 1), CheckIns.nextQuarter(s))
        assertEquals(LocalDate.of(2026, 10, 1), CheckIns.nextQuarter(s, LocalDate.of(2026, 10, 1)))
        assertEquals(LocalDate.of(2026, 7, 1), CheckIns.previousQuarter(s, LocalDate.of(2026, 10, 1)))
    }

    // own checkup rhythm, every 2 months on 15th
    @Test fun ownCheckupRhythm() {
        val st = settings({ it.copy(ownCheckup = true, checkupStartEpochDay = LocalDate.of(2026, 2, 1).toEpochDay(), checkupEveryMonths = 2, checkupDayOfMonth = 15) })
        val s = snapshot(emptyList(), settings = st)
        assertEquals(LocalDate.of(2026, 10, 15), CheckIns.nextQuarter(s))
        assertEquals(LocalDate.of(2026, 8, 15), CheckIns.previousQuarter(s, LocalDate.of(2026, 10, 15)))
    }

    // own checkup on 31st falls back to last day in short months
    @Test fun ownCheckupDayFitsShortMonths() {
        val st = settings({ it.copy(ownCheckup = true, checkupStartEpochDay = LocalDate.of(2027, 1, 1).toEpochDay(), checkupEveryMonths = 1, checkupDayOfMonth = 31) })
        val s = snapshot(emptyList(), settings = st)
        assertEquals(
            listOf(LocalDate.of(2027, 1, 31), LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31), LocalDate.of(2027, 4, 30)),
            CheckIns.quarterDates(s, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 4, 30)),
        )
    }

    // refresher puts settled step back in daily checkin for its own days only
    @Test fun refresherStepIsAskedDailyUntilItSettlesAgain() {
        val water = step(1, 200) { it.copy(buildStartEpochDay = TODAY.minusDays(10).toEpochDay(), buildDays = 30) }
        val s = snapshot(listOf(water))
        assertTrue(CheckIns.askable(s, TODAY.minusDays(11)).isEmpty()) // settled before refresher
        assertEquals(listOf(water), CheckIns.askable(s, TODAY))
        assertEquals(listOf(water), CheckIns.askable(s, TODAY.plusDays(19))) // day 30 of refresher
        assertTrue(CheckIns.askable(s, TODAY.plusDays(20)).isEmpty()) // 30 not settings 66
    }

    // quarter owed till done, never for new user
    @Test fun quarterIsOwedUntilDone() {
        val s = snapshot(emptyList())
        assertEquals(LocalDate.of(2026, 10, 1), CheckIns.owedQuarter(s))
        val done = snapshot(emptyList(), checkups = listOf(Checkup(LocalDate.of(2026, 10, 1).toEpochDay(), 0, 0)))
        assertNull(CheckIns.owedQuarter(done))
        val newUser = snapshot(emptyList(), settings = settings({ it.copy(firstEpochDay = TODAY.toEpochDay()) }))
        assertNull(CheckIns.owedQuarter(newUser))
    }

    // button early, due, done
    @Test fun buttonStates() {
        val newUser = settings({ it.copy(firstEpochDay = TODAY.toEpochDay()) })
        val eod = listOf(step(1, 0, timing = StepTiming.END_OF_DAY))
        assertEquals(ButtonState.Early(20), CheckIns.button(snapshot(eod, settings = newUser, hour = 9)))
        assertEquals(ButtonState.Due(CheckInKind.DAILY, 1), CheckIns.button(snapshot(eod, settings = newUser, hour = 21)))
        assertEquals(ButtonState.Done, CheckIns.button(snapshot(eod, kept(1, 0), settings = newUser, hour = 21)))
    }

    // flow order daily, weekly, quarter
    @Test fun flowRunsDailyWeeklyThenQuarter() {
        assertEquals(
            listOf(CheckInKind.DAILY, CheckInKind.WEEKLY, CheckInKind.QUARTER),
            Owed(listOf(TODAY), listOf(TODAY), TODAY).flow,
        )
        assertEquals(listOf(CheckInKind.QUARTER), Owed(emptyList(), emptyList(), TODAY).flow)
    }
}
