package com.lucyprockova.alice.logic

import com.lucyprockova.alice.data.Step
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 3 checkin kinds, in run order when several due */
enum class CheckInKind { DAILY, WEEKLY, QUARTER }

/** slim checkin button on habits, grey before time, yellow due, green done */
sealed interface ButtonState {
    data class Early(val hour: Int) : ButtonState
    data class Due(val kind: CheckInKind, val count: Int) : ButtonState
    data object Done : ButtonState
}

/** checkins owed now, oldest first */
data class Owed(
    val dailyDays: List<LocalDate>,
    val weeklyDue: List<LocalDate>,
    val quarterDue: LocalDate?,
) {
    val flow: List<CheckInKind> get() = buildList {
        if (dailyDays.isNotEmpty()) add(CheckInKind.DAILY)
        if (weeklyDue.isNotEmpty()) add(CheckInKind.WEEKLY)
        if (quarterDue != null) add(CheckInKind.QUARTER)
    }
}

/** checkin due vs still owed */
object CheckIns {

    /** only building daily or fixed-day steps, no one-offs or periodic */
    fun askable(s: Snapshot, day: LocalDate): List<Step> = s.steps.filter { step ->
        val then = StepRules.on(step, day)
        !then.oneOff && !StepRules.isPeriodic(then) &&
            !day.isBefore(StepRules.epoch(step.createdEpochDay)) &&
            !day.isBefore(StepRules.epoch(step.buildStartEpochDay)) &&
            day.isBefore(StepRules.buildEnd(step, s.settings)) &&
            StepRules.dailyTarget(step, day) > 0
    }

    fun openOn(s: Snapshot, day: LocalDate): List<Step> = askable(s, day).filter { step ->
        val e = s.entry(step.id, day)
        (e?.count ?: 0) < StepRules.dailyTarget(step, day) && e?.missed != true
    }

    /** owed window, last [Grid.OWED_WINDOW] days incl today, today only after checkin hour */
    fun owedDays(s: Snapshot): List<LocalDate> {
        val last = if (s.checkInOpen) s.today else s.today.minusDays(1)
        val first = s.today.minusDays(Grid.OWED_WINDOW - 1)
        return generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }
            .filter { it.toEpochDay() !in s.checkedInDays && openOn(s, it).isNotEmpty() }
            .toList()
    }

    fun weeklyDueDates(s: Snapshot, upTo: LocalDate = s.today): List<LocalDate> {
        val st = s.settings
        val first = StepRules.epoch(st.firstEpochDay)
        val anchor = StepRules.weekStart(first, st.firstDayOfWeek)
        val n = st.newStepEveryWeeks.coerceAtLeast(1)
        return generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(upTo) }
            .filter { it.dayOfWeek == st.weeklyDayOfWeek }
            // every n weeks counted from week of first use, week starts so mid-week start still week 0
            .filter { ChronoUnit.WEEKS.between(anchor, StepRules.weekStart(it, st.firstDayOfWeek)) % n == 0L }
            .toList()
    }

    /** several owed still only 1 new step, capped at 8 */
    fun owedWeekly(s: Snapshot): List<LocalDate> {
        val lastDone = s.weekly.maxOfOrNull { it.dueEpochDay } ?: Long.MIN_VALUE
        return weeklyDueDates(s).filter { it.toEpochDay() > lastDone }.takeLast(8)
    }

    /** 3-month checkup days in [from]..[to], 1 jan, apr, jul, oct, or user's own rhythm */
    fun quarterDates(s: Snapshot, from: LocalDate, to: LocalDate): List<LocalDate> {
        val st = s.settings
        val out = mutableListOf<LocalDate>()
        if (!st.ownCheckup) {
            var d = LocalDate.of(from.year, 1, 1)
            while (!d.isAfter(to)) {
                if (!d.isBefore(from)) out += d
                d = d.plusMonths(3)
            }
        } else {
            val start = StepRules.epoch(st.checkupStartEpochDay)
            var i = 0L
            while (true) {
                // add from start, not last date, and clamp day, so short months don't drift it
                val m = start.plusMonths(i * st.checkupEveryMonths.coerceAtLeast(1))
                val d = m.withDayOfMonth(st.checkupDayOfMonth.coerceIn(1, m.lengthOfMonth()))
                if (d.isAfter(to)) break
                if (!d.isBefore(from)) out += d
                i++
            }
        }
        return out
    }

    fun nextQuarter(s: Snapshot, today: LocalDate = s.today): LocalDate =
        quarterDates(s, today, today.plusYears(2)).first()

    /** checkup before [due], where its period starts */
    fun previousQuarter(s: Snapshot, due: LocalDate): LocalDate =
        quarterDates(s, due.minusYears(2), due.minusDays(1)).lastOrNull() ?: due.minusMonths(3)

    fun owedQuarter(s: Snapshot): LocalDate? {
        val first = StepRules.epoch(s.settings.firstEpochDay)
        // plusDays(1) so first-use day itself never owes a checkup
        val due = quarterDates(s, first.plusDays(1), s.today).lastOrNull() ?: return null
        return if (s.checkups.any { it.dueEpochDay == due.toEpochDay() }) null else due
    }

    fun owed(s: Snapshot) = Owed(owedDays(s), owedWeekly(s), owedQuarter(s))

    fun button(s: Snapshot): ButtonState {
        val owed = owed(s)
        return when {
            owed.dailyDays.isNotEmpty() -> ButtonState.Due(CheckInKind.DAILY, owed.dailyDays.size)
            owed.weeklyDue.isNotEmpty() -> ButtonState.Due(CheckInKind.WEEKLY, owed.weeklyDue.size)
            owed.quarterDue != null -> ButtonState.Due(CheckInKind.QUARTER, 1)
            !s.checkInOpen && openOn(s, s.today).isNotEmpty() -> ButtonState.Early(s.settings.checkInHour)
            else -> ButtonState.Done
        }
    }
}
