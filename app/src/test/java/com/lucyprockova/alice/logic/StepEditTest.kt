package com.lucyprockova.alice.logic

import com.lucyprockova.alice.data.Category
import com.lucyprockova.alice.data.PeriodUnit
import com.lucyprockova.alice.data.RepeatMode
import com.lucyprockova.alice.data.StepKind
import com.lucyprockova.alice.data.StepTiming
import com.lucyprockova.alice.logic.TestData.TODAY
import com.lucyprockova.alice.logic.TestData.kept
import com.lucyprockova.alice.logic.TestData.snapshot
import com.lucyprockova.alice.logic.TestData.step
import com.lucyprockova.alice.ui.Often
import com.lucyprockova.alice.ui.StepDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** step edit, form fills from step, new schedule from today, past untouched */
class StepEditTest {

    private val monWedFri = "1,0,1,0,1,0,0"

    // past schedule encodes and decodes back
    @Test fun scheduleTextRoundTrips() {
        val gym = step(1, 10) { it.copy(repeat = RepeatMode.DAYS, dayCounts = monWedFri) }
        val past = PastSchedule(5, 9, Schedule.of(gym))
        assertEquals(past, PastSchedule.decode(past.encode()))
        assertEquals(listOf(past), PastSchedule.of(gym.copy(pastSchedules = "rubbish\n" + past.encode())))
    }

    // rename keeps schedule, no history added
    @Test fun renameKeepsTheScheduleAndAddsNoHistory() {
        val water = step(1, 10)
        val edited = StepRules.applyEdit(water, water.copy(name = "Water before coffee"), TODAY)
        assertEquals("Water before coffee", edited.name)
        assertEquals("", edited.pastSchedules)
        assertNull(edited.scheduleFromEpochDay)
    }

    // new schedule from today, past keeps ticks and stats
    @Test fun newScheduleAppliesFromTodayAndThePastKeepsItsTicks() {
        val water = step(1, 10)
        val entries = kept(1, *(1L..10L).toList().toLongArray())
        val before = Stats.habit(water, snapshot(listOf(water), entries))

        val edited = StepRules.applyEdit(water, water.copy(repeat = RepeatMode.DAYS, dayCounts = monWedFri), TODAY)
        assertEquals(TODAY.toEpochDay(), edited.scheduleFromEpochDay)
        assertEquals(1, PastSchedule.of(edited).size)

        val s = snapshot(listOf(edited), entries)
        val tuesday = LocalDate.of(2026, 9, 29)
        assertEquals(CellState.KEPT, Grid.cell(edited, tuesday, s).state)
        assertEquals(CellState.OFF, Grid.cell(edited, TODAY, s).state)
        assertEquals(CellState.OFF, Grid.cell(edited, TODAY.plusDays(3), s).state) // next tuesday too
        assertEquals(1, StepRules.dailyTarget(edited, TODAY.plusDays(2))) // monday on
        assertEquals(before.successPercent, Stats.habit(edited, s).successPercent)
        assertEquals(before.longestStreak, Stats.habit(edited, s).longestStreak)
    }

    // second edit same day replaces first
    @Test fun aSecondEditTheSameDayReplacesTheFirst() {
        val water = step(1, 10)
        val once = StepRules.applyEdit(water, water.copy(timesPerDay = 2), TODAY)
        val twice = StepRules.applyEdit(once, once.copy(timesPerDay = 3), TODAY)
        assertEquals(1, PastSchedule.of(twice).size)
        assertEquals(1, PastSchedule.of(twice).single().schedule.timesPerDay)
        assertEquals(1, StepRules.dailyTarget(twice, TODAY.minusDays(1)))
        assertEquals(3, StepRules.dailyTarget(twice, TODAY))
    }

    // step made today, edit keeps no history
    @Test fun editingAStepMadeTodayKeepsNoHistory() {
        val fresh = step(1, 0)
        val edited = StepRules.applyEdit(fresh, fresh.copy(timesPerDay = 2), TODAY)
        assertEquals("", edited.pastSchedules)
        assertEquals(2, StepRules.dailyTarget(edited, TODAY))
    }

    // periods count from edit day
    @Test fun periodsCountFromTheEdit() {
        val yoga = step(1, 30)
        val edited = StepRules.applyEdit(yoga, yoga.copy(repeat = RepeatMode.TIMES, times = 2, unit = PeriodUnit.DAYS, everyN = 3), TODAY)
        assertEquals(Period(TODAY, TODAY.plusDays(2)), StepRules.periodOf(edited, TODAY, DayOfWeek.MONDAY))
    }

    // one-off turned habit starts building today
    @Test fun oneOffTurnedIntoAHabitStartsBuildingToday() {
        val tidy = step(1, 20) { it.copy(oneOff = true, finishedEpochDay = TODAY.minusDays(2).toEpochDay()) }
        val habit = StepRules.applyEdit(tidy, tidy.copy(oneOff = false), TODAY)
        assertNull(habit.finishedEpochDay)
        assertEquals(TODAY.toEpochDay(), habit.buildStartEpochDay)
        assertEquals(Phase.BUILDING, StepRules.phase(habit, TestData.settings(), TODAY))
        assertTrue(StepRules.on(habit, TODAY.minusDays(5)).oneOff)
    }

    // edit keeps fields form doesn't set
    @Test fun editKeepsWhatTheFormDoesNotSet() {
        val nestle = step(1, 40, StepKind.AVOID, StepTiming.END_OF_DAY) { it.copy(category = Category.COMPANY, note = "Water", tags = "Ethical", brands = "KitKat") }
        val edited = StepRules.applyEdit(nestle, nestle.copy(name = "Nestlé", note = "", tags = "", createdEpochDay = TODAY.toEpochDay()), TODAY)
        assertEquals("Water", edited.note)
        assertEquals("Ethical", edited.tags)
        assertEquals(nestle.createdEpochDay, edited.createdEpochDay)
    }

    // form round trips every schedule kind
    @Test fun formFillsInFromEveryKindOfSchedule() {
        listOf(
            step(1, 5),
            step(2, 5) { it.copy(timesPerDay = 2) },
            step(3, 5) { it.copy(repeat = RepeatMode.DAYS, dayCounts = "2,0,1,0,1,0,0") },
            step(4, 5) { it.copy(repeat = RepeatMode.TIMES, times = 2, unit = PeriodUnit.WEEK) },
            step(5, 5) { it.copy(repeat = RepeatMode.TIMES, times = 1, unit = PeriodUnit.MONTH) },
            step(6, 5) { it.copy(repeat = RepeatMode.TIMES, times = 3, unit = PeriodUnit.DAYS, everyN = 10) },
            step(7, 5) { it.copy(oneOff = true) },
            step(8, 5, StepKind.SWAP, StepTiming.END_OF_DAY) { it.copy(category = Category.PRODUCT) },
        ).forEach { original ->
            val back = StepDraft.from(original).toStep(TODAY)
            assertEquals(original.name, Schedule.of(original), Schedule.of(back))
            assertEquals(original.kind, back.kind)
        }
        assertEquals(Often.ONCE, StepDraft.from(step(1, 5) { it.copy(oneOff = true) }).often)
    }

    // rhythm form can't show survives rename
    @Test fun untouchedRhythmTheFormCannotShowSurvivesARename() {
        val clean = step(1, 30) { it.copy(repeat = RepeatMode.TIMES, unit = PeriodUnit.WEEK, everyN = 2) }
        val renamed = StepDraft.from(clean).copy(name = "Deep clean the flat").editOf(clean, TODAY)
        assertEquals(Schedule.of(clean), Schedule.of(renamed))
        assertEquals("", StepRules.applyEdit(clean, renamed, TODAY).pastSchedules)
    }
}
