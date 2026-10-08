package com.lucyprockova.alice.logic

import com.lucyprockova.alice.data.PeriodUnit
import com.lucyprockova.alice.data.RepeatMode
import com.lucyprockova.alice.data.Step
import com.lucyprockova.alice.data.StepTiming

/** when step is due, edits keep old one as [PastSchedule] */
data class Schedule(
    val timing: StepTiming,
    val oneOff: Boolean,
    val repeat: RepeatMode,
    val timesPerDay: Int,
    val dayCounts: String,
    val times: Int,
    val unit: PeriodUnit,
    val everyN: Int,
) {
    fun applyTo(step: Step): Step = step.copy(
        timing = timing, oneOff = oneOff, repeat = repeat, timesPerDay = timesPerDay,
        dayCounts = dayCounts, times = times, unit = unit, everyN = everyN,
    )

    /** one line, fields split by |, day counts use commas so no clash */
    fun encode(): String = listOf(timing.name, oneOff, repeat.name, timesPerDay, dayCounts, times, unit.name, everyN).joinToString("|")

    companion object {
        fun of(step: Step) = Schedule(step.timing, step.oneOff, step.repeat, step.timesPerDay, step.dayCounts, step.times, step.unit, step.everyN)

        fun decode(text: String): Schedule? = runCatching {
            val f = text.split("|")
            Schedule(
                StepTiming.valueOf(f[0]), f[1].toBooleanStrict(), RepeatMode.valueOf(f[2]), f[3].toInt(),
                f[4], f[5].toInt(), PeriodUnit.valueOf(f[6]), f[7].toInt(),
            )
        }.getOrNull()
    }
}

/** schedule step used before edit, epoch day [from] up to but not incl [until] */
data class PastSchedule(val from: Long, val until: Long, val schedule: Schedule) {
    /** stored as "from;until;schedule", one per line in [Step.pastSchedules] */
    fun encode(): String = "$from;$until;${schedule.encode()}"

    companion object {
        fun decode(line: String): PastSchedule? {
            // limit 3 so schedule part stays whole
            val f = line.split(";", limit = 3)
            if (f.size < 3) return null
            val from = f[0].toLongOrNull() ?: return null
            val until = f[1].toLongOrNull() ?: return null
            return Schedule.decode(f[2])?.let { PastSchedule(from, until, it) }
        }

        fun of(step: Step): List<PastSchedule> =
            step.pastSchedules.lines().filter { it.isNotBlank() }.mapNotNull(::decode).sortedBy { it.from }
    }
}
