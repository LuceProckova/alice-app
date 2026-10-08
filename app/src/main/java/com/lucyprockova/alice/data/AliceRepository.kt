package com.lucyprockova.alice.data

import androidx.room.withTransaction
import com.lucyprockova.alice.logic.Snapshot
import com.lucyprockova.alice.logic.StepRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/** one way in for data, screens never touch Room */
class AliceRepository(
    private val db: AliceDatabase,
    val settingsStore: SettingsStore,
    val clock: Clock = Clock.systemDefaultZone(),
    /** default slip reasons in the app language, put back when an import has none */
    private val defaultReasons: () -> List<SlipReason> = { emptyList() },
) {
    private val dao = db.dao()

    /** today from the one shared clock */
    fun today(): LocalDate = LocalDate.now(clock)

    val steps: Flow<List<Step>> = dao.observeSteps()
    val reasons: Flow<List<SlipReason>> = dao.observeReasons()
    val checkups: Flow<List<Checkup>> = dao.observeCheckups()
    fun step(id: Long): Flow<Step?> = dao.observeStep(id)

    fun snapshots(clock: Flow<LocalDateTime>): Flow<Snapshot> {
        // combine takes max 5 flows, so tables go in two triples first
        val db1 = combine(dao.observeSteps(), dao.observeEntries(), dao.observeCheckInDays()) { a, b, c -> Triple(a, b, c) }
        val db2 = combine(dao.observeWeekly(), dao.observeCheckups(), dao.observeAnswers()) { a, b, c -> Triple(a, b, c) }
        return combine(db1, db2, settingsStore.settings, clock) { (steps, entries, days), (weekly, checkups, answers), settings, now ->
            Snapshot(steps, entries, days, weekly, checkups, answers, settings, now)
        }
    }

    suspend fun addStep(step: Step): Long = dao.insertStep(step.copy(name = step.name.trim()))
    suspend fun update(step: Step) = dao.updateStep(step)
    suspend fun delete(step: Step) = dao.deleteStep(step)

    /** save edit, old schedule kept for old days, see [StepRules.applyEdit] */
    suspend fun edit(old: Step, edited: Step) = dao.updateStep(StepRules.applyEdit(old, edited, today()))

    suspend fun setCount(step: Step, day: LocalDate, count: Int) {
        val old = dao.entry(step.id, day.toEpochDay())
        if (count <= 0 && old?.reason == null) dao.deleteEntry(step.id, day.toEpochDay())
        else dao.upsertEntry(Entry(step.id, day.toEpochDay(), count.coerceAtLeast(0), missed = false, reason = null))
        if (step.oneOff) dao.updateStep(step.copy(finishedEpochDay = if (count > 0) day.toEpochDay() else null))
    }

    suspend fun tap(step: Step, day: LocalDate, snapshot: Snapshot) {
        val then = StepRules.on(step, day) // old days keep old schedule
        val current = snapshot.count(step.id, day)
        val total = if (StepRules.isPeriodic(then))
            StepRules.periodOf(then, day, snapshot.settings.firstDayOfWeek).days.sumOf { snapshot.count(step.id, it) } else current
        setCount(step, day, StepRules.nextCount(then, day, current, total))
    }

    suspend fun saveDaily(answers: List<Entry>, days: List<CheckInDay>) = db.withTransaction {
        dao.upsertEntries(answers)
        dao.insertCheckInDays(days)
    }

    suspend fun saveWeekly(due: List<LocalDate>, newStep: Step?): Long? = db.withTransaction {
        val id = newStep?.let { dao.insertStep(it) }
        dao.insertWeekly(due.map { WeeklyDone(it.toEpochDay(), today().toEpochDay(), id) })
        id
    }

    suspend fun saveCheckup(due: LocalDate, from: LocalDate, answers: Map<Step, SlipLevel>) = db.withTransaction {
        val today = today()
        dao.insertCheckups(listOf(Checkup(due.toEpochDay(), from.toEpochDay(), today.toEpochDay())))
        dao.insertAnswers(answers.map { (step, level) -> CheckupAnswer(due.toEpochDay(), step.id, level) })
        // slips can send step back to building for a refresher
        // refresher never longer than the user's own days to build a habit
        val maxDays = settingsStore.current.daysToBuild
        answers.forEach { (step, level) -> dao.updateStep(StepRules.applyCheckup(step, level, today, maxDays)) }
    }

    suspend fun addReason(name: String) {
        val all = dao.allReasons()
        dao.insertReasons(listOf(SlipReason(name.trim(), "sell", (all.maxOfOrNull { it.position } ?: 0) + 1)))
    }

    suspend fun deleteReason(reason: SlipReason) = db.withTransaction {
        dao.deleteReason(reason)
        // reason is plain text not foreign key, so clear by hand
        dao.clearReason(reason.name)
    }

    suspend fun exportData(): BackupData = BackupData(
        settingsStore.current, dao.allSteps(), dao.allEntries(), dao.allCheckInDays(), dao.allWeekly(),
        dao.allCheckups(), dao.allAnswers(), dao.allReasons(),
    )

    /** replace all with [data], or merge in when [add] */
    suspend fun importData(data: BackupData, add: Boolean) {
        val final = if (add) Backup.merge(exportData(), data) else data
        db.withTransaction {
            // all or nothing, bad file never leaves half a db
            dao.clearAll()
            final.steps.forEach { dao.insertStep(it) }
            dao.upsertEntries(final.entries)
            dao.insertCheckInDays(final.checkInDays)
            dao.insertWeekly(final.weekly)
            dao.insertCheckups(final.checkups)
            dao.insertAnswers(final.answers)
            dao.insertReasons(final.reasons.ifEmpty { defaultReasons() })
        }
        if (!add) settingsStore.update { final.settings.copy(welcomed = true, tutorialSeen = true) }
    }
}
