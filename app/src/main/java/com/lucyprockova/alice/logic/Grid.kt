package com.lucyprockova.alice.logic

import com.lucyprockova.alice.data.Step
import java.time.LocalDate

/** how one habits grid square is drawn */
enum class CellState {
    KEPT,
    PARTIAL,
    MISSED,
    UNFILLED,
    OPEN,
    LOCKED,
    FUTURE,
    NONE,
    OFF,
    BAND,
    EMPTY,
}

/** one square, look, taps, tappable */
data class Cell(val day: LocalDate, val state: CellState, val count: Int = 0, val tappable: Boolean = false)

/** every square of habits grid and one habit history */
object Grid {

    /** older days not asked anymore, count as missed */
    const val OWED_WINDOW = 7L

    /** square of [stepNow] on [day], judged by schedule it had that day */
    fun cell(stepNow: Step, day: LocalDate, s: Snapshot): Cell {
        val step = StepRules.on(stepNow, day)
        val today = s.today
        val created = StepRules.epoch(step.createdEpochDay)
        val entry = s.entry(step.id, day)
        val count = entry?.count ?: 0
        if (day.isBefore(created)) return Cell(day, CellState.NONE)

        if (step.oneOff) {
            val finished = step.finishedEpochDay?.let(StepRules::epoch)
            return when {
                finished == day -> Cell(day, CellState.KEPT, 1, tappable = day == today)
                day == today && finished == null ->
                    Cell(day, if (StepRules.isUnlocked(step, s.now.hour, s.settings.checkInHour)) CellState.OPEN else CellState.LOCKED, tappable = true)
                day.isAfter(today) -> Cell(day, CellState.FUTURE)
                else -> Cell(day, CellState.EMPTY)
            }
        }

        if (StepRules.isPeriodic(step)) {
            val period = StepRules.periodOf(step, day, s.settings.firstDayOfWeek)
            val total = period.days.sumOf { s.count(step.id, it) }
            val full = total >= step.times
            val tappable = !day.isAfter(today)
            return when {
                full -> Cell(day, CellState.BAND, count, tappable)
                day.isAfter(today) -> Cell(day, CellState.FUTURE)
                count > 0 -> Cell(day, CellState.KEPT, count, tappable)
                // failed period shows one red, on its last day only
                period.end.isBefore(today) && day == period.end -> Cell(day, CellState.MISSED, tappable = true)
                day == today -> Cell(day, CellState.OPEN, tappable = true)
                else -> Cell(day, CellState.EMPTY, tappable = true)
            }
        }

        val target = StepRules.dailyTarget(step, day)
        if (target == 0) return Cell(day, CellState.OFF, count)
        if (count >= target) return Cell(day, CellState.KEPT, 1, tappable = !day.isAfter(today))
        if (day.isAfter(today)) return Cell(day, CellState.FUTURE)
        if (day == today) {
            // tappable so screen can say when it unlocks, tap itself changes nothing
            if (!StepRules.isUnlocked(step, s.now.hour, s.settings.checkInHour)) return Cell(day, CellState.LOCKED, count, tappable = true)
            if (entry?.missed == true) return Cell(day, CellState.MISSED, count, tappable = true)
            return Cell(day, if (count > 0) CellState.PARTIAL else CellState.OPEN, count, tappable = true)
        }
        // past day, final once crossed, checked in, or fallen out of owed window
        val counted = entry?.missed == true || day.toEpochDay() in s.checkedInDays || day.isBefore(today.minusDays(OWED_WINDOW - 1))
        return when {
            count > 0 && !counted -> Cell(day, CellState.PARTIAL, count, tappable = true)
            counted -> Cell(day, CellState.MISSED, count, tappable = true)
            else -> Cell(day, CellState.UNFILLED, count, tappable = true)
        }
    }

    /** true when step done on [day], for stats, periodic counts each tapped day */
    fun isKept(stepNow: Step, day: LocalDate, s: Snapshot): Boolean {
        val step = StepRules.on(stepNow, day)
        return when {
            step.oneOff -> step.finishedEpochDay == day.toEpochDay()
            StepRules.isPeriodic(step) -> s.count(step.id, day) > 0
            else -> {
                val t = StepRules.dailyTarget(step, day)
                t > 0 && s.count(step.id, day) >= t
            }
        }
    }

    fun isMissed(step: Step, day: LocalDate, s: Snapshot): Boolean =
        cell(step, day, s).state == CellState.MISSED
}
