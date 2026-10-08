package com.lucyprockova.alice.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** backup export, import, merge */
class BackupTest {
    private val data = BackupData(
        settings = AliceSettings(name = "Luce", daysToBuild = 50, theme = 7, firstEpochDay = 20000, checkupStartEpochDay = 20000, hiddenIdeas = setOf("Make your bed")),
        steps = listOf(
            Step(id = 1, name = "Water first", kind = StepKind.HABIT, createdEpochDay = 20000),
            Step(id = 2, name = "Nestlé", kind = StepKind.AVOID, timing = StepTiming.END_OF_DAY, createdEpochDay = 20001,
                category = Category.COMPANY, verdict = Verdict.EDGE, tags = "Ethical", brands = "KitKat\nNescafé", buildDays = 30),
            Step(id = 3, name = "Yoga", kind = StepKind.HABIT, createdEpochDay = 20002, repeat = RepeatMode.TIMES, times = 2, unit = PeriodUnit.MONTH, oneOff = false),
        ),
        entries = listOf(Entry(1, 20005, 1), Entry(2, 20006, 0, missed = true, reason = "Craving")),
        checkInDays = listOf(CheckInDay(20005), CheckInDay(20006, skipped = true)),
        weekly = listOf(WeeklyDone(20004, 20004, 3)),
        checkups = listOf(Checkup(20090, 20000, 20090)),
        answers = listOf(CheckupAnswer(20090, 1, SlipLevel.A_FEW)),
        reasons = listOf(SlipReason("Craving", "cookie", 1)),
    )

    // export then import, same data back
    @Test fun exportThenImportGivesTheSameData() {
        assertEquals(data, Backup.fromJson(Backup.toJson(data)))
    }

    // non alice file or bad json rejected
    @Test fun rejectsFilesThatAreNotAliceBackups() {
        assertThrows(IllegalArgumentException::class.java) { Backup.fromJson("{\"app\":\"Other\"}") }
        assertThrows(IllegalArgumentException::class.java) { Backup.fromJson("not json") }
    }

    // merge keeps both sets, history moves to new ids
    @Test fun addingKeepsBothAndMovesHistoryToNewIds() {
        val merged = Backup.merge(data, data)
        assertEquals(6, merged.steps.size)
        assertEquals(setOf(1L, 2L, 3L, 4L, 5L, 6L), merged.steps.map { it.id }.toSet())
        assertEquals(4, merged.entries.size)
        assertEquals(listOf(4L, 5L), merged.entries.drop(2).map { it.stepId })
        assertEquals(2, merged.checkInDays.size) // same days not doubled
        assertEquals("Luce", merged.settings.name)
    }

    // adding a backup moves every step id it holds: entries, weekly pick, check-up answers
    @Test fun addingRemapsEveryStepId() {
        val mine = data.copy(steps = listOf(Step(id = 10, name = "Mine", kind = StepKind.HABIT, createdEpochDay = 19990)),
            entries = emptyList(), weekly = emptyList(), checkups = emptyList(), answers = emptyList())
        val merged = Backup.merge(mine, data)
        // file ids 1, 2, 3 become 11, 12, 13
        val newId = merged.steps.associate { it.name to it.id }
        assertEquals(mapOf("Mine" to 10L, "Water first" to 11L, "Nestlé" to 12L, "Yoga" to 13L), newId)
        assertEquals(listOf(11L, 12L), merged.entries.map { it.stepId })
        assertEquals(13L, merged.weekly.single().newStepId)
        assertEquals(11L, merged.answers.single().stepId)
        // a weekly pick pointing at a step not in the file ends up as none
        val orphan = data.copy(weekly = listOf(WeeklyDone(20011, 20011, 99)))
        assertEquals(null, Backup.merge(mine, orphan).weekly.single().newStepId)
    }
}
