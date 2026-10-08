package com.lucyprockova.alice.logic

import com.lucyprockova.alice.data.AliceSettings
import com.lucyprockova.alice.data.PeriodUnit
import com.lucyprockova.alice.data.RepeatMode
import com.lucyprockova.alice.data.SlipLevel
import com.lucyprockova.alice.data.Step
import com.lucyprockova.alice.data.StepTiming
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** where step lives now */
enum class Phase {
    BUILDING,
    SETTLED,
    FINISHED,
}

/** period of repeating step, both ends incl */
data class Period(val start: LocalDate, val end: LocalDate) {
    operator fun contains(day: LocalDate) = !day.isBefore(start) && !day.isAfter(end)
    val days: List<LocalDate> get() = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
}

/** how often a step repeats, words come from ui so they can be translated */
sealed class Rhythm {
    data object OneOff : Rhythm()
    data object EndOfDay : Rhythm()
    /** 1 = plain every day */
    data class Daily(val timesPerDay: Int) : Rhythm()
    /** weekday index monday 0 to count, only days with count over 0 */
    data class Days(val days: List<Pair<Int, Int>>) : Rhythm()
    data class Times(val times: Int, val unit: PeriodUnit, val everyN: Int) : Rhythm()

    val everyDay: Boolean get() = this == Daily(1)
}

/** rules for one step, phase, how often due, periods */
object StepRules {

    private const val LINE = "\n"

    fun epoch(day: Long): LocalDate = LocalDate.ofEpochDay(day)

    fun weekStart(date: LocalDate, first: DayOfWeek = DayOfWeek.MONDAY): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(first))

    fun weekDays(start: LocalDate): List<LocalDate> = (0L..6L).map { start.plusDays(it) }

    fun dayNumber(step: Step, today: LocalDate): Int =
        ChronoUnit.DAYS.between(epoch(step.buildStartEpochDay), today).toInt() + 1

    /** step's own refresher length, else setting, [AliceSettings.DEFAULT_DAYS_TO_BUILD] by default */
    fun buildLength(step: Step, settings: AliceSettings): Int = step.buildDays ?: settings.daysToBuild

    /** first day step counts as settled */
    fun buildEnd(step: Step, settings: AliceSettings): LocalDate =
        epoch(step.buildStartEpochDay).plusDays(buildLength(step, settings).toLong())

    fun phase(step: Step, settings: AliceSettings, today: LocalDate): Phase = when {
        step.oneOff && step.finishedEpochDay != null -> Phase.FINISHED
        step.oneOff -> Phase.BUILDING
        today.isBefore(buildEnd(step, settings)) -> Phase.BUILDING
        else -> Phase.SETTLED
    }

    fun finishedToday(step: Step, today: LocalDate) = step.oneOff && step.finishedEpochDay == today.toEpochDay()

    fun isUnlocked(step: Step, hourNow: Int, checkInHour: Int): Boolean =
        step.timing == StepTiming.ANYTIME || hourNow >= checkInHour

    fun scheduleStart(step: Step): Long = step.scheduleFromEpochDay ?: step.createdEpochDay

    /** step as scheduled on [day], old days keep old schedule so edits never turn ticks into slips */
    fun on(step: Step, day: LocalDate): Step {
        val e = day.toEpochDay()
        if (step.pastSchedules.isEmpty() || e >= scheduleStart(step)) return step
        // until exclusive, so edit day itself already uses new schedule
        val past = PastSchedule.of(step).firstOrNull { e >= it.from && e < it.until } ?: return step
        return past.schedule.applyTo(step).copy(scheduleFromEpochDay = past.from)
    }

    /** old schedule kept as [PastSchedule] to yesterday, new one from [today], ticks never touched */
    fun applyEdit(old: Step, edited: Step, today: LocalDate): Step {
        val sameThing = old.name == edited.name && old.kind == edited.kind && old.category == edited.category
        val kept = edited.copy(
            id = old.id, createdEpochDay = old.createdEpochDay, buildStartEpochDay = old.buildStartEpochDay,
            buildDays = old.buildDays, refresherDays = old.refresherDays, finishedEpochDay = old.finishedEpochDay,
            verdict = old.verdict, tags = old.tags, note = old.note, alternatives = old.alternatives,
            brands = if (sameThing || edited.brands.isEmpty()) old.brands else edited.brands,
            scheduleFromEpochDay = old.scheduleFromEpochDay, pastSchedules = old.pastSchedules,
        )
        val before = Schedule.of(old)
        if (Schedule.of(edited) == before) return kept

        val t = today.toEpochDay()
        val start = scheduleStart(old)
        // second edit same day just replaces first, no empty 0-day history line
        val history = if (start < t) (PastSchedule.of(old) + PastSchedule(start, t, before)).joinToString(LINE) { it.encode() } else old.pastSchedules
        val changed = kept.copy(scheduleFromEpochDay = if (start < t) t else old.scheduleFromEpochDay, pastSchedules = history)
        return when {
            // one-off turned repeating, fresh build from today
            old.oneOff && !edited.oneOff -> changed.copy(finishedEpochDay = null, buildStartEpochDay = t, buildDays = null)
            !old.oneOff && edited.oneOff -> changed.copy(finishedEpochDay = null)
            else -> changed
        }
    }

    fun dailyTarget(step: Step, day: LocalDate): Int = dailyTargetNow(on(step, day), day)

    private fun dailyTargetNow(step: Step, day: LocalDate): Int = when {
        step.timing == StepTiming.END_OF_DAY -> 1
        step.repeat == RepeatMode.DAILY -> step.timesPerDay.coerceAtLeast(1)
        step.repeat == RepeatMode.DAYS -> step.dayCountList[day.dayOfWeek.value - 1]
        else -> 0
    }

    fun isPeriodic(step: Step) = step.timing == StepTiming.ANYTIME && step.repeat == RepeatMode.TIMES

    fun periodOf(step: Step, day: LocalDate, firstDay: DayOfWeek): Period {
        // anchor on schedule start, not creation, so edit restarts the count
        val created = epoch(scheduleStart(step))
        val n = step.everyN.coerceAtLeast(1).toLong()
        return when (step.unit) {
            PeriodUnit.WEEK -> {
                val anchor = weekStart(created, firstDay)
                // floorDiv so days before anchor land in earlier period, not period 0
                val weeks = Math.floorDiv(ChronoUnit.WEEKS.between(anchor, weekStart(day, firstDay)), n) * n
                val start = anchor.plusWeeks(weeks)
                Period(start, start.plusWeeks(n).minusDays(1))
            }
            PeriodUnit.MONTH -> {
                val anchor = created.withDayOfMonth(1)
                val months = Math.floorDiv(ChronoUnit.MONTHS.between(anchor, day.withDayOfMonth(1)), n) * n
                val start = anchor.plusMonths(months)
                Period(start, start.plusMonths(n).minusDays(1))
            }
            PeriodUnit.DAYS -> {
                val blocks = Math.floorDiv(ChronoUnit.DAYS.between(created, day), n) * n
                val start = created.plusDays(blocks)
                Period(start, start.plusDays(n - 1))
            }
        }
    }

    fun nextCount(step: Step, day: LocalDate, current: Int, periodTotal: Int): Int {
        if (step.oneOff) return if (current > 0) 0 else 1
        if (isPeriodic(step)) return if (periodTotal < step.times) current + 1 else 0
        val target = dailyTarget(step, day)
        if (target == 0) return current
        return if (current >= target) 0 else current + 1
    }

    /** how often step repeats, as data, ui turns it into words */
    fun rhythm(step: Step): Rhythm {
        if (step.oneOff) return Rhythm.OneOff
        if (step.timing == StepTiming.END_OF_DAY) return Rhythm.EndOfDay
        return when (step.repeat) {
            RepeatMode.DAILY -> Rhythm.Daily(step.timesPerDay.coerceAtLeast(1))
            RepeatMode.DAYS -> Rhythm.Days(step.dayCountList.mapIndexedNotNull { i, c -> if (c > 0) i to c else null })
            RepeatMode.TIMES -> Rhythm.Times(step.times, step.unit, step.everyN)
        }
    }

    /** NEVER shortens refresher, A_FEW rebuilds for refresher length, worse rebuilds longer, max (build days) */
    fun applyCheckup(step: Step, level: SlipLevel, today: LocalDate, maxDays: Int): Step = when (level) {
        SlipLevel.NEVER -> step.copy(refresherDays = (step.refresherDays - 5).coerceIn(5, maxDays))
        SlipLevel.ONCE -> step
        SlipLevel.A_FEW -> {
            val r = step.refresherDays.coerceAtMost(maxDays)
            step.copy(refresherDays = r, buildStartEpochDay = today.toEpochDay(), buildDays = r)
        }
        else -> {
            val r = (step.refresherDays + 5).coerceAtMost(maxDays)
            step.copy(refresherDays = r, buildStartEpochDay = today.toEpochDay(), buildDays = r)
        }
    }
}
