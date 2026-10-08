package com.lucyprockova.alice.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/** real room db in memory, dao queries and the repository transactions */
@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: AliceDatabase
    private lateinit var repo: AliceRepository
    private val today = LocalDate.of(2026, 10, 5)

    @Before fun open() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AliceDatabase::class.java).allowMainThreadQueries().build()
        // fixed clock so today never moves under the test
        val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        repo = AliceRepository(db, SettingsStore(ctx), clock)
    }

    @After fun close() = db.close()

    private fun step(name: String) = Step(name = name, kind = StepKind.HABIT, createdEpochDay = today.minusDays(10).toEpochDay())

    // deleting a step takes its ticks with it
    @Test fun deleteCascadesToEntries() = runBlocking {
        val id = repo.addStep(step("Water first"))
        db.dao().upsertEntries(listOf(Entry(id, today.toEpochDay(), 1), Entry(id, today.minusDays(1).toEpochDay(), 1)))
        repo.delete(db.dao().allSteps().single())
        assertTrue(db.dao().allEntries().isEmpty())
    }

    // a one-off tick sets finished, untick clears it
    @Test fun oneOffTickFinishesAndUntickReopens() = runBlocking {
        val id = repo.addStep(step("Clear the drawer").copy(oneOff = true))
        repo.setCount(db.dao().allSteps().single { it.id == id }, today, 1)
        assertEquals(today.toEpochDay(), db.dao().allSteps().single().finishedEpochDay)
        repo.setCount(db.dao().allSteps().single(), today, 0)
        assertNull(db.dao().allSteps().single().finishedEpochDay)
    }

    // checkup saves answers and sends a slipping step back to building from today
    @Test fun checkupAppliesRefresher() = runBlocking {
        repo.addStep(step("Read").copy(createdEpochDay = today.minusDays(200).toEpochDay(), buildStartEpochDay = today.minusDays(200).toEpochDay()))
        val s = db.dao().allSteps().single()
        repo.saveCheckup(today, today.minusMonths(3), mapOf(s to SlipLevel.A_FEW))
        assertEquals(1, db.dao().allAnswers().size)
        assertEquals(today.toEpochDay(), db.dao().allSteps().single().buildStartEpochDay)
    }

    // import add keeps both sets, replace keeps only the file
    @Test fun importAddsOrReplaces() = runBlocking {
        repo.addStep(step("Mine"))
        val file = repo.exportData().copy(steps = listOf(step("Theirs").copy(id = 1)))
        repo.importData(file, add = true)
        assertEquals(setOf("Mine", "Theirs"), db.dao().allSteps().map { it.name }.toSet())
        repo.importData(file, add = false)
        assertEquals(listOf("Theirs"), db.dao().allSteps().map { it.name })
    }

    // removing a slip reason clears it from old crosses, the crosses stay
    @Test fun deleteReasonKeepsTheSlips() = runBlocking {
        val id = repo.addStep(step("Gym"))
        repo.addReason("Rain")
        db.dao().upsertEntries(listOf(Entry(id, today.toEpochDay(), 0, missed = true, reason = "Rain")))
        repo.deleteReason(db.dao().allReasons().single { it.name == "Rain" })
        val e = db.dao().allEntries().single()
        assertTrue(e.missed)
        assertNull(e.reason)
    }
}
