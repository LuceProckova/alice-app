package com.lucyprockova.alice.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** all db access, flows re-emit when their tables change */
@Dao
interface AliceDao {

    @Query("SELECT * FROM steps ORDER BY buildStartEpochDay DESC, id DESC")
    fun observeSteps(): Flow<List<Step>>

    @Query("SELECT * FROM steps WHERE id = :id")
    fun observeStep(id: Long): Flow<Step?>

    @Query("SELECT * FROM steps")
    suspend fun allSteps(): List<Step>

    @Insert
    suspend fun insertStep(step: Step): Long

    @Update
    suspend fun updateStep(step: Step)

    @Delete
    suspend fun deleteStep(step: Step)

    @Query("SELECT * FROM entries")
    fun observeEntries(): Flow<List<Entry>>

    @Query("SELECT * FROM entries")
    suspend fun allEntries(): List<Entry>

    @Query("SELECT * FROM entries WHERE stepId = :stepId AND epochDay = :day")
    suspend fun entry(stepId: Long, day: Long): Entry?

    @Upsert
    suspend fun upsertEntry(entry: Entry)

    @Upsert
    suspend fun upsertEntries(entries: List<Entry>)

    @Query("DELETE FROM entries WHERE stepId = :stepId AND epochDay = :day")
    suspend fun deleteEntry(stepId: Long, day: Long)

    @Query("UPDATE entries SET reason = NULL WHERE reason = :reason")
    suspend fun clearReason(reason: String)

    @Query("SELECT * FROM checkin_days")
    fun observeCheckInDays(): Flow<List<CheckInDay>>

    @Query("SELECT * FROM checkin_days")
    suspend fun allCheckInDays(): List<CheckInDay>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckInDays(days: List<CheckInDay>)

    @Query("SELECT * FROM weekly_done")
    fun observeWeekly(): Flow<List<WeeklyDone>>

    @Query("SELECT * FROM weekly_done")
    suspend fun allWeekly(): List<WeeklyDone>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeekly(done: List<WeeklyDone>)

    @Query("SELECT * FROM checkups ORDER BY dueEpochDay DESC")
    fun observeCheckups(): Flow<List<Checkup>>

    @Query("SELECT * FROM checkups")
    suspend fun allCheckups(): List<Checkup>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckups(checkups: List<Checkup>)

    @Query("SELECT * FROM checkup_answers")
    fun observeAnswers(): Flow<List<CheckupAnswer>>

    @Query("SELECT * FROM checkup_answers")
    suspend fun allAnswers(): List<CheckupAnswer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<CheckupAnswer>)

    @Query("SELECT * FROM slip_reasons ORDER BY position, name")
    fun observeReasons(): Flow<List<SlipReason>>

    @Query("SELECT * FROM slip_reasons")
    suspend fun allReasons(): List<SlipReason>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReasons(reasons: List<SlipReason>)

    @Delete
    suspend fun deleteReason(reason: SlipReason)

    @Query("DELETE FROM steps") suspend fun clearSteps()
    @Query("DELETE FROM entries") suspend fun clearEntries()
    @Query("DELETE FROM checkin_days") suspend fun clearCheckIns()
    @Query("DELETE FROM weekly_done") suspend fun clearWeekly()
    @Query("DELETE FROM checkups") suspend fun clearCheckups()
    @Query("DELETE FROM checkup_answers") suspend fun clearAnswers()
    @Query("DELETE FROM slip_reasons") suspend fun clearReasons()

    @Transaction
    suspend fun clearAll() {
        clearAnswers(); clearEntries(); clearSteps(); clearCheckIns(); clearWeekly(); clearCheckups(); clearReasons()
    }
}
