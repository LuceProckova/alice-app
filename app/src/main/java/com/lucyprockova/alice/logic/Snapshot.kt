package com.lucyprockova.alice.logic

import com.lucyprockova.alice.data.AliceSettings
import com.lucyprockova.alice.data.CheckInDay
import com.lucyprockova.alice.data.Checkup
import com.lucyprockova.alice.data.CheckupAnswer
import com.lucyprockova.alice.data.Entry
import com.lucyprockova.alice.data.Step
import com.lucyprockova.alice.data.WeeklyDone
import java.time.LocalDate
import java.time.LocalDateTime

/** db, settings, clock in one object, rules unit test without android */
data class Snapshot(
    val steps: List<Step>,
    val entries: List<Entry>,
    val checkInDays: List<CheckInDay> = emptyList(),
    val weekly: List<WeeklyDone> = emptyList(),
    val checkups: List<Checkup> = emptyList(),
    val answers: List<CheckupAnswer> = emptyList(),
    val settings: AliceSettings,
    val now: LocalDateTime,
) {
    val today: LocalDate get() = now.toLocalDate()

    private val entryIndex: Map<Long, Map<Long, Entry>> by lazy {
        entries.groupBy { it.stepId }.mapValues { (_, list) -> list.associateBy { it.epochDay } }
    }
    val checkedInDays: Set<Long> by lazy { checkInDays.map { it.epochDay }.toSet() }

    fun entry(stepId: Long, day: LocalDate): Entry? = entryIndex[stepId]?.get(day.toEpochDay())
    fun count(stepId: Long, day: LocalDate): Int = entry(stepId, day)?.count ?: 0
    fun entriesFor(stepId: Long): Collection<Entry> = entryIndex[stepId]?.values ?: emptyList()

    val checkInOpen: Boolean get() = now.hour >= settings.checkInHour
}
