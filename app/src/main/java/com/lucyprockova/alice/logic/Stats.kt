package com.lucyprockova.alice.logic

import com.lucyprockova.alice.data.SlipLevel
import com.lucyprockova.alice.data.Step
import com.lucyprockova.alice.data.StepKind
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** figures on one habit screen */
data class HabitStats(
    val daysGoing: Int,
    val timesBroken: Int,
    val successPercent: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val slipReasons: List<Pair<String, Int>>,
)

/** figures for one period on insights */
data class PeriodStats(
    val keptPercent: Int,
    val daysCheckedIn: Int,
    val days: Int,
    val onTrackPercent: Int,
    val slips: Int,
    val newSteps: Int,
    val oneOffsDone: Int,
    val averageDaysGoing: Int,
    val slipReasons: List<Pair<String, Int>>,
)

/** week at a glance popup */
data class WeekGlance(
    val keptPercent: Int,
    val daysCheckedIn: Int,
    val topReason: Pair<String, Int>?,
    val easiest: GlanceStep?,
    val hardest: GlanceStep?,
)

/** one step's week on the glance popup, [kept] of [days] */
data class GlanceStep(val name: String, val kept: Int, val days: Int)

/** what an achievement is about, ui turns it into words */
enum class AchievementKind { NEVER_MISSED, MONTHS_STRONG, STREAK }

/** achievement line on 3-month summary, [count] months or days, [days] only for sorting */
data class Achievement(val stepName: String, val kind: AchievementKind, val count: Int, val days: Int = 0)

/** all a 3-month summary shows, any style */
data class SummaryData(
    val name: String,
    val from: LocalDate,
    val to: LocalDate,
    val ways: Int,
    val habits: Int,
    val swaps: Int,
    val avoids: Int,
    val onTrackPercent: Int,
    val slips: Int,
    val newSteps: Int,
    val daysCheckedIn: Int,
    val days: Int,
    val achievements: List<Achievement>,
    val added: List<Pair<StepKind, String>>,
    val dailyShare: List<Float>,
    val weekShares: List<Float>,
)

/** maths behind one habit, insights, week at a glance, 3-month summary */
object Stats {

    fun days(from: LocalDate, to: LocalDate): List<LocalDate> =
        generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()

    /** days step counts in stats, from creation to yesterday, today only once kept */
    private fun countedDays(step: Step, from: LocalDate, to: LocalDate, s: Snapshot): List<LocalDate> {
        val start = maxOf(from, StepRules.epoch(step.createdEpochDay))
        return days(start, minOf(to, s.today)).filter { d ->
            d != s.today || Grid.isKept(step, d, s)
        }.filter { d -> StepRules.on(step, d).let { StepRules.isPeriodic(it) || it.oneOff } || StepRules.dailyTarget(step, d) > 0 }
    }

    /** building, tick decides, settled, 3-month answer decides, null when nothing to judge */
    fun keptOn(stepNow: Step, day: LocalDate, s: Snapshot): Boolean? {
        val step = StepRules.on(stepNow, day)
        if (step.oneOff) return null
        val buildEnd = StepRules.buildEnd(step, s.settings)
        val building = day.isBefore(buildEnd) && !day.isBefore(StepRules.epoch(step.buildStartEpochDay))
        // days before a refresher restart were ticked too, so grid still decides
        if (building || day.isBefore(StepRules.epoch(step.buildStartEpochDay))) {
            if (StepRules.isPeriodic(step)) return if (s.count(step.id, day) > 0) true else null
            val cell = Grid.cell(step, day, s)
            return when (cell.state) {
                CellState.KEPT -> true
                CellState.MISSED -> false
                else -> null
            }
        }
        val checkup = s.checkups.firstOrNull { day.toEpochDay() >= it.fromEpochDay && day.toEpochDay() < it.dueEpochDay } ?: return true
        val level = s.answers.firstOrNull { it.checkupDue == checkup.dueEpochDay && it.stepId == step.id }?.level ?: SlipLevel.NEVER
        val length = checkup.dueEpochDay - checkup.fromEpochDay
        // no real slip dates, so slip share drawn as one red block at end of period
        val redFrom = checkup.dueEpochDay - (length * level.share).roundToInt()
        return day.toEpochDay() < redFrom
    }

    fun habit(step: Step, s: Snapshot): HabitStats {
        val created = StepRules.epoch(step.createdEpochDay)
        val all = days(created, s.today).map { it to keptOn(step, it, s) }
        val known = all.filter { it.second != null }
        val kept = known.count { it.second == true }
        var current = 0
        for ((d, k) in all.reversed()) {
            if (d == s.today && k != true) continue
            if (k == true) current++ else break
        }
        var best = 0; var run = 0
        for ((_, k) in all) { if (k == true) { run++; best = maxOf(best, run) } else if (k == false) run = 0 }
        return HabitStats(
            daysGoing = ChronoUnit.DAYS.between(created, s.today).toInt() + 1,
            timesBroken = known.count { it.second == false },
            successPercent = if (known.isEmpty()) 0 else (kept * 100f / known.size).roundToInt(),
            currentStreak = current,
            longestStreak = best,
            slipReasons = reasons(s.entriesFor(step.id).mapNotNull { it.reason }),
        )
    }

    private fun reasons(list: List<String>) = list.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }

    fun keptShare(step: Step, from: LocalDate, to: LocalDate, s: Snapshot): Float? {
        val known = days(maxOf(from, StepRules.epoch(step.createdEpochDay)), minOf(to, s.today)).mapNotNull { keptOn(step, it, s) }
        if (known.isEmpty()) return null
        return known.count { it } / known.size.toFloat()
    }

    fun period(from: LocalDate, to: LocalDate, s: Snapshot): PeriodStats {
        val active = s.steps.filter { !it.oneOff && !StepRules.epoch(it.createdEpochDay).isAfter(to) }
        val shares = active.mapNotNull { keptShare(it, from, to, s) }
        val inRange = { e: Long -> e >= from.toEpochDay() && e <= to.toEpochDay() }
        val passed = days(from, minOf(to, s.today))
        val slips = s.entries.filter { it.missed && inRange(it.epochDay) }
        val slipDays = active.sumOf { st -> passed.count { d -> keptOn(st, d, s) == false } }
        return PeriodStats(
            keptPercent = if (shares.isEmpty()) 0 else (shares.average() * 100).roundToInt(),
            daysCheckedIn = passed.count { it.toEpochDay() in s.checkedInDays || CheckIns.openOn(s, it).isEmpty() },
            days = passed.size,
            onTrackPercent = if (shares.isEmpty()) 0 else (shares.count { it >= 0.8f } * 100f / shares.size).roundToInt(),
            slips = slipDays,
            newSteps = s.steps.count { inRange(it.createdEpochDay) },
            oneOffsDone = s.steps.count { it.oneOff && it.finishedEpochDay?.let(inRange) == true },
            averageDaysGoing = if (active.isEmpty()) 0 else active.map {
                ChronoUnit.DAYS.between(StepRules.epoch(it.createdEpochDay), minOf(to, s.today)).toInt() + 1
            }.average().roundToInt(),
            slipReasons = reasons(slips.mapNotNull { it.reason }),
        )
    }

    fun glance(weekStart: LocalDate, s: Snapshot): WeekGlance {
        val end = weekStart.plusDays(6)
        val p = period(weekStart, end, s)
        val scored = s.steps.filter { !it.oneOff }.mapNotNull { st ->
            val known = days(weekStart, minOf(end, s.today)).mapNotNull { keptOn(st, it, s) }
            if (known.isEmpty()) null else Triple(st.name, known.count { it }, known.size)
        }
        val best = scored.maxByOrNull { it.second.toFloat() / it.third }
        val worst = scored.minByOrNull { it.second.toFloat() / it.third }
        return WeekGlance(
            keptPercent = p.keptPercent,
            daysCheckedIn = p.daysCheckedIn,
            topReason = p.slipReasons.firstOrNull(),
            easiest = best?.let { GlanceStep(it.first, it.second, it.third) },
            hardest = worst?.takeIf { it != best }?.let { GlanceStep(it.first, it.second, it.third) },
        )
    }

    fun summary(from: LocalDate, to: LocalDate, s: Snapshot): SummaryData {
        val p = period(from, to, s)
        val live = s.steps.filter { !StepRules.epoch(it.createdEpochDay).isAfter(to) && (!it.oneOff || it.finishedEpochDay != null) }
        val achievements = mutableListOf<Achievement>()
        s.steps.filter { !it.oneOff }.forEach { st ->
            val months = ChronoUnit.MONTHS.between(StepRules.epoch(st.createdEpochDay), to)
            val share = keptShare(st, StepRules.epoch(st.createdEpochDay), to, s)
            val stats = habit(st, s)
            when {
                months >= 2 && share != null && share >= 0.999f -> achievements += Achievement(st.name, AchievementKind.NEVER_MISSED, months.toInt(), months.toInt() * 31)
                months >= 3 -> achievements += Achievement(st.name, AchievementKind.MONTHS_STRONG, months.toInt(), months.toInt() * 30)
                stats.longestStreak >= 14 -> achievements += Achievement(st.name, AchievementKind.STREAK, stats.longestStreak, stats.longestStreak)
            }
        }
        val range = days(from, to)
        // -1 marks days with nothing to judge, skipped in week average
        val daily = range.map { d ->
            val known = s.steps.filter { !it.oneOff }.mapNotNull { if (d.isAfter(s.today)) null else keptOn(it, d, s) }
            if (known.isEmpty()) -1f else known.count { it } / known.size.toFloat()
        }
        return SummaryData(
            name = s.settings.name,
            from = from, to = to,
            ways = live.size,
            habits = live.count { it.kind == StepKind.HABIT },
            swaps = live.count { it.kind == StepKind.SWAP },
            avoids = live.count { it.kind == StepKind.AVOID },
            onTrackPercent = p.onTrackPercent,
            slips = p.slips,
            newSteps = p.newSteps,
            daysCheckedIn = p.daysCheckedIn,
            days = p.days,
            achievements = achievements.sortedByDescending { it.days }.take(3),
            added = s.steps.filter { it.createdEpochDay in from.toEpochDay()..to.toEpochDay() }.map { it.kind to it.name },
            dailyShare = daily,
            weekShares = daily.chunked(7).map { w -> w.filter { it >= 0f }.let { k -> if (k.isEmpty()) 0f else k.average().toFloat() } },
        )
    }
}
