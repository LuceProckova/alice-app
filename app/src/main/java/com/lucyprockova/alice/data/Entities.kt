package com.lucyprockova.alice.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** kind of change, own icon and tab each */
enum class StepKind { HABIT, SWAP, AVOID }

/** END_OF_DAY = all day rule, unlocks at check-in time */
enum class StepTiming { ANYTIME, END_OF_DAY }

/** how often step repeats */
enum class RepeatMode {
    DAILY,
    DAYS,
    TIMES,
}

/** period for [RepeatMode.TIMES] */
enum class PeriodUnit { WEEK, MONTH, DAYS }

/** what sort of thing an avoid or swap is */
enum class Category { PRODUCT, COMPANY, FOOD }

/** how hard avoid is avoided, badge on avoid list */
enum class Verdict { AVOID, EDGE, FINE }

/** habit, swap or avoid, builds [buildDays] then settles, dates are epoch days */
@Entity(tableName = "steps")
data class Step(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: StepKind,
    val timing: StepTiming = StepTiming.ANYTIME,
    val createdEpochDay: Long,
    val buildStartEpochDay: Long = createdEpochDay,
    val buildDays: Int? = null,
    val refresherDays: Int = DEFAULT_REFRESHER,
    val oneOff: Boolean = false,
    val finishedEpochDay: Long? = null,
    val repeat: RepeatMode = RepeatMode.DAILY,
    val timesPerDay: Int = 1,
    /** per weekday, monday first, 0 = off */
    val dayCounts: String = "1,1,1,1,1,1,1",
    val times: Int = 1,
    val unit: PeriodUnit = PeriodUnit.WEEK,
    val everyN: Int = 1,
    val category: Category? = null,
    val verdict: Verdict = Verdict.AVOID,
    val tags: String = "",
    val note: String = "",
    val brands: String = "",
    val alternatives: String = "",
    /** start of current schedule after edit, null = never changed */
    val scheduleFromEpochDay: Long? = null,
    /** older schedules, so old days keep old schedule */
    val pastSchedules: String = "",
) {
    val dayCountList: List<Int> get() = dayCounts.split(",").map { it.trim().toIntOrNull() ?: 0 }.let { l -> List(7) { l.getOrElse(it) { 0 } } }
    val tagList: List<String> get() = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val brandList: List<String> get() = brands.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val alternativeList: List<String> get() = alternatives.lines().map { it.trim() }.filter { it.isNotEmpty() }

    companion object {
        /** first refresher after slip, shrinks or grows each check-up */
        const val DEFAULT_REFRESHER = 30
    }
}

/** one step on one day, taps or check-in cross */
@Entity(
    tableName = "entries",
    primaryKeys = ["stepId", "epochDay"],
    foreignKeys = [ForeignKey(entity = Step::class, parentColumns = ["id"], childColumns = ["stepId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("stepId")],
)
data class Entry(
    val stepId: Long,
    val epochDay: Long,
    val count: Int = 0,
    val missed: Boolean = false,
    val reason: String? = null,
)

/** day daily check-in was done or skipped */
@Entity(tableName = "checkin_days")
data class CheckInDay(
    @PrimaryKey val epochDay: Long,
    val skipped: Boolean = false,
)

/** weekly check-in done, keyed by due date it covered */
@Entity(tableName = "weekly_done")
data class WeeklyDone(
    @PrimaryKey val dueEpochDay: Long,
    val doneEpochDay: Long,
    val newStepId: Long? = null,
)

/** finished 3-month check-up, covers 3 months before [dueEpochDay] */
@Entity(tableName = "checkups")
data class Checkup(
    @PrimaryKey val dueEpochDay: Long,
    val fromEpochDay: Long,
    val doneEpochDay: Long,
)

/** one step's slips at one check-up */
@Entity(
    tableName = "checkup_answers",
    primaryKeys = ["checkupDue", "stepId"],
    foreignKeys = [ForeignKey(entity = Step::class, parentColumns = ["id"], childColumns = ["stepId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("stepId")],
)
data class CheckupAnswer(
    val checkupDue: Long,
    val stepId: Long,
    val level: SlipLevel,
)

/** check-up answers, [share] = rough part of days slipped, words in ui */
enum class SlipLevel(val share: Float) {
    NEVER(0f),
    ONCE(0.02f),
    A_FEW(0.1f),
    HALF(0.5f),
    MOST(0.8f),
    ALWAYS(1f),
}

/** reason tag for a check-in cross */
@Entity(tableName = "slip_reasons")
data class SlipReason(
    @PrimaryKey val name: String,
    val icon: String = "sell",
    val position: Int = 0,
)
