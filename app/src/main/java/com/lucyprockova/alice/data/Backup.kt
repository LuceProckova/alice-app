package com.lucyprockova.alice.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** everything in one alice backup file */
data class BackupData(
    val settings: AliceSettings,
    val steps: List<Step>,
    val entries: List<Entry>,
    val checkInDays: List<CheckInDay>,
    val weekly: List<WeeklyDone>,
    val checkups: List<Checkup>,
    val answers: List<CheckupAnswer>,
    val reasons: List<SlipReason>,
)

/** [BackupData] to .json export file and back */
object Backup {
    const val VERSION = 1

    fun toJson(b: BackupData): String = JSONObject()
        .put("app", "Alice").put("version", VERSION)
        .put("settings", b.settings.toJson())
        .put("steps", JSONArray(b.steps.map { stepJson(it) }))
        .put("entries", JSONArray(b.entries.map {
            JSONObject().put("stepId", it.stepId).put("day", it.epochDay).put("count", it.count).put("missed", it.missed).put("reason", it.reason ?: JSONObject.NULL)
        }))
        .put("checkIns", JSONArray(b.checkInDays.map { JSONObject().put("day", it.epochDay).put("skipped", it.skipped) }))
        .put("weekly", JSONArray(b.weekly.map { JSONObject().put("due", it.dueEpochDay).put("done", it.doneEpochDay).put("newStepId", it.newStepId ?: JSONObject.NULL) }))
        .put("checkups", JSONArray(b.checkups.map { JSONObject().put("due", it.dueEpochDay).put("from", it.fromEpochDay).put("done", it.doneEpochDay) }))
        .put("answers", JSONArray(b.answers.map { JSONObject().put("due", it.checkupDue).put("stepId", it.stepId).put("level", it.level.name) }))
        .put("reasons", JSONArray(b.reasons.map { JSONObject().put("name", it.name).put("icon", it.icon).put("position", it.position) }))
        .toString(2)

    private fun stepJson(s: Step) = JSONObject()
        .put("id", s.id).put("name", s.name).put("kind", s.kind.name).put("timing", s.timing.name)
        .put("created", s.createdEpochDay).put("buildStart", s.buildStartEpochDay).put("buildDays", s.buildDays ?: JSONObject.NULL)
        .put("refresher", s.refresherDays).put("oneOff", s.oneOff).put("finished", s.finishedEpochDay ?: JSONObject.NULL)
        .put("repeat", s.repeat.name).put("timesPerDay", s.timesPerDay).put("dayCounts", s.dayCounts)
        .put("times", s.times).put("unit", s.unit.name).put("everyN", s.everyN)
        .put("category", s.category?.name ?: JSONObject.NULL).put("verdict", s.verdict.name)
        .put("tags", s.tags).put("note", s.note).put("brands", s.brands).put("alternatives", s.alternatives)
        .put("scheduleFrom", s.scheduleFromEpochDay ?: JSONObject.NULL).put("pastSchedules", s.pastSchedules)

    /** read backup, throws [IllegalArgumentException] if not alice or broken */
    fun fromJson(text: String): BackupData =
        try {
            parse(text)
        } catch (e: JSONException) {
            // missing or wrong typed field, same error as any other bad file
            throw IllegalArgumentException("Broken Alice backup", e)
        }

    private fun parse(text: String): BackupData {
        // opt* defaults so older files missing new fields still load
        val o = runCatching { JSONObject(text) }.getOrElse { throw IllegalArgumentException("Not a JSON file") }
        require(o.optString("app") == "Alice") { "Not an Alice backup" }
        fun arr(key: String): List<JSONObject> = o.optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } } ?: emptyList()
        fun JSONObject.longOrNull(k: String) = if (isNull(k) || !has(k)) null else getLong(k)
        fun JSONObject.strOrNull(k: String) = if (isNull(k) || !has(k)) null else getString(k)
        return BackupData(
            settings = AliceSettings.fromJson(o.optJSONObject("settings") ?: JSONObject()),
            steps = arr("steps").map { j ->
                Step(
                    id = j.getLong("id"), name = j.getString("name"),
                    kind = StepKind.valueOf(j.getString("kind")), timing = StepTiming.valueOf(j.optString("timing", "ANYTIME")),
                    createdEpochDay = j.getLong("created"), buildStartEpochDay = j.optLong("buildStart", j.getLong("created")),
                    buildDays = if (j.isNull("buildDays")) null else j.optInt("buildDays"),
                    refresherDays = j.optInt("refresher", Step.DEFAULT_REFRESHER), oneOff = j.optBoolean("oneOff"),
                    finishedEpochDay = j.longOrNull("finished"),
                    repeat = RepeatMode.valueOf(j.optString("repeat", "DAILY")), timesPerDay = j.optInt("timesPerDay", 1),
                    dayCounts = j.optString("dayCounts", "1,1,1,1,1,1,1"), times = j.optInt("times", 1),
                    unit = PeriodUnit.valueOf(j.optString("unit", "WEEK")), everyN = j.optInt("everyN", 1),
                    category = j.strOrNull("category")?.let { Category.valueOf(it) },
                    verdict = Verdict.valueOf(j.optString("verdict", "AVOID")),
                    tags = j.optString("tags"), note = j.optString("note"), brands = j.optString("brands"), alternatives = j.optString("alternatives"),
                    scheduleFromEpochDay = j.longOrNull("scheduleFrom"), pastSchedules = j.optString("pastSchedules"),
                )
            },
            entries = arr("entries").map { Entry(it.getLong("stepId"), it.getLong("day"), it.optInt("count"), it.optBoolean("missed"), it.strOrNull("reason")) },
            checkInDays = arr("checkIns").map { CheckInDay(it.getLong("day"), it.optBoolean("skipped")) },
            weekly = arr("weekly").map { WeeklyDone(it.getLong("due"), it.getLong("done"), it.longOrNull("newStepId")) },
            checkups = arr("checkups").map { Checkup(it.getLong("due"), it.getLong("from"), it.getLong("done")) },
            answers = arr("answers").map { CheckupAnswer(it.getLong("due"), it.getLong("stepId"), SlipLevel.valueOf(it.getString("level"))) },
            reasons = arr("reasons").map { SlipReason(it.getString("name"), it.optString("icon", "sell"), it.optInt("position")) },
        )
    }

    /** add [incoming] to [current], settings untouched */
    fun merge(current: BackupData, incoming: BackupData): BackupData {
        // new ids above current max, so imported steps never clash
        val nextId = (current.steps.maxOfOrNull { it.id } ?: 0L) + 1
        val idMap = incoming.steps.mapIndexed { i, s -> s.id to nextId + i }.toMap()
        return current.copy(
            steps = current.steps + incoming.steps.map { it.copy(id = idMap.getValue(it.id)) },
            // entries follow step to new id, orphans dropped
            entries = current.entries + incoming.entries.mapNotNull { e -> idMap[e.stepId]?.let { e.copy(stepId = it) } },
            // duplicate day, current wins
            checkInDays = (current.checkInDays + incoming.checkInDays).distinctBy { it.epochDay },
            // the step picked in an imported weekly check-in moves to its new id too, unknown ids become none
            weekly = (current.weekly + incoming.weekly.map { w -> w.copy(newStepId = w.newStepId?.let { idMap[it] }) }).distinctBy { it.dueEpochDay },
            checkups = (current.checkups + incoming.checkups).distinctBy { it.dueEpochDay },
            answers = current.answers + incoming.answers.mapNotNull { a -> idMap[a.stepId]?.let { a.copy(stepId = it) } }
                .filter { a -> current.answers.none { it.checkupDue == a.checkupDue && it.stepId == a.stepId } },
            reasons = (current.reasons + incoming.reasons).distinctBy { it.name },
        )
    }
}
