package com.lucyprockova.alice.logic

import com.lucyprockova.alice.data.AliceSettings
import com.lucyprockova.alice.data.CheckInDay
import com.lucyprockova.alice.data.Checkup
import com.lucyprockova.alice.data.CheckupAnswer
import com.lucyprockova.alice.data.Entry
import com.lucyprockova.alice.data.Step
import com.lucyprockova.alice.data.StepKind
import com.lucyprockova.alice.data.StepTiming
import com.lucyprockova.alice.data.WeeklyDone
import java.time.LocalDate
import java.time.LocalDateTime

/** shared test helpers, fixed date */
object TestData {
    /** sat 3 oct 2026 */
    val TODAY: LocalDate = LocalDate.of(2026, 10, 3)

    fun settings(vararg tweaks: (AliceSettings) -> AliceSettings): AliceSettings =
        tweaks.fold(AliceSettings(firstEpochDay = TODAY.minusDays(100).toEpochDay(), checkupStartEpochDay = TODAY.toEpochDay())) { s, f -> f(s) }

    fun step(id: Long, ageDays: Long, kind: StepKind = StepKind.HABIT, timing: StepTiming = StepTiming.ANYTIME, f: (Step) -> Step = { it }) =
        f(Step(id = id, name = "Step $id", kind = kind, timing = timing, createdEpochDay = TODAY.minusDays(ageDays).toEpochDay()))

    fun kept(stepId: Long, vararg daysAgo: Long, count: Int = 1) = daysAgo.map { Entry(stepId, TODAY.minusDays(it).toEpochDay(), count) }

    fun crossed(stepId: Long, daysAgo: Long, reason: String? = null) = Entry(stepId, TODAY.minusDays(daysAgo).toEpochDay(), 0, missed = true, reason = reason)

    fun snapshot(
        steps: List<Step>,
        entries: List<Entry> = emptyList(),
        checkIns: List<Long> = emptyList(),
        weekly: List<WeeklyDone> = emptyList(),
        checkups: List<Checkup> = emptyList(),
        answers: List<CheckupAnswer> = emptyList(),
        settings: AliceSettings = settings(),
        hour: Int = 10,
        today: LocalDate = TODAY,
    ) = Snapshot(
        steps, entries, checkIns.map { CheckInDay(today.minusDays(it).toEpochDay()) }, weekly, checkups, answers, settings,
        LocalDateTime.of(today, java.time.LocalTime.of(hour, 0)),
    )
}
