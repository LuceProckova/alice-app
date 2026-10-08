package com.lucyprockova.alice.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** v2 to v3 upgrade keeps every step and tick, adds empty schedule history */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AliceDatabase::class.java)

    // old row survives, new columns get their defaults, room checks the result matches 3.json
    @Test fun migrate2To3KeepsData() {
        helper.createDatabase("migration-test", 2).use { db ->
            db.execSQL(
                "INSERT INTO steps (id, name, kind, timing, createdEpochDay, buildStartEpochDay, buildDays, refresherDays, oneOff, finishedEpochDay, repeat, timesPerDay, dayCounts, times, unit, everyN, category, verdict, tags, note, brands, alternatives) " +
                    "VALUES (7, 'Water first', 'HABIT', 'ANYTIME', 20000, 20000, NULL, 30, 0, NULL, 'DAILY', 1, '1,1,1,1,1,1,1', 1, 'WEEK', 1, NULL, 'AVOID', '', '', '', '')",
            )
            db.execSQL("INSERT INTO entries (stepId, epochDay, count, missed, reason) VALUES (7, 20001, 1, 0, NULL)")
        }
        val db = helper.runMigrationsAndValidate("migration-test", 3, true, AliceDatabase.MIGRATION_2_3)
        db.query("SELECT name, scheduleFromEpochDay, pastSchedules FROM steps").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Water first", c.getString(0))
            assertTrue(c.isNull(1))
            assertEquals("", c.getString(2))
        }
        db.query("SELECT COUNT(*) FROM entries").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
    }
}
