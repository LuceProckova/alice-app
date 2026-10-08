package com.lucyprockova.alice

import android.app.Activity
import android.os.Bundle
import com.lucyprockova.alice.data.AliceDatabase
import com.lucyprockova.alice.data.BackupData
import com.lucyprockova.alice.data.Category
import com.lucyprockova.alice.data.CheckInDay
import com.lucyprockova.alice.data.Checkup
import com.lucyprockova.alice.data.CheckupAnswer
import com.lucyprockova.alice.data.CompanyBrands
import com.lucyprockova.alice.data.Entry
import com.lucyprockova.alice.data.PeriodUnit
import com.lucyprockova.alice.data.RepeatMode
import com.lucyprockova.alice.data.SlipLevel
import com.lucyprockova.alice.data.Step
import com.lucyprockova.alice.data.StepKind
import com.lucyprockova.alice.data.StepTiming
import com.lucyprockova.alice.data.Verdict
import com.lucyprockova.alice.data.WeeklyDone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.random.Random

/** debug only, swaps all data for 7 months of sample history, then closes */
class DemoDataActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as AliceApp).repository
        val today = LocalDate.now()
        fun ago(d: Long) = today.minusDays(d).toEpochDay()
        // fixed ids from 1, so answers and celebrated below can point at them
        var id = 0L
        fun step(name: String, kind: StepKind, age: Long, timing: StepTiming = StepTiming.ANYTIME, f: (Step) -> Step = { it }) =
            f(Step(id = ++id, name = name, kind = kind, timing = timing, createdEpochDay = ago(age)))

        val steps = listOf(
            step("Water first", StepKind.HABIT, 40),
            step("Stretch 10 min", StepKind.HABIT, 30),
            step("Read 10 pages", StepKind.HABIT, 18),
            step("Yoga", StepKind.HABIT, 20) { it.copy(repeat = RepeatMode.TIMES, times = 2, unit = PeriodUnit.WEEK) },
            step("Gym", StepKind.HABIT, 22) { it.copy(repeat = RepeatMode.DAYS, dayCounts = "1,0,1,0,1,0,0") },
            step("Vitamins", StepKind.HABIT, 15) { it.copy(timesPerDay = 2) },
            step("Deep clean", StepKind.HABIT, 25) { it.copy(repeat = RepeatMode.TIMES, times = 1, unit = PeriodUnit.WEEK, everyN = 2) },
            step("Green energy tariff", StepKind.SWAP, 4) { it.copy(oneOff = true, category = Category.COMPANY) },
            step("Clear out the bathroom cabinet", StepKind.HABIT, 9) { it.copy(oneOff = true) },
            step("Cleared the kitchen drawer", StepKind.HABIT, 30) { it.copy(oneOff = true, finishedEpochDay = ago(13)) },
            step("LED bulbs everywhere", StepKind.SWAP, 90) { it.copy(oneOff = true, finishedEpochDay = ago(83)) },
            step("No late snacks", StepKind.HABIT, 25, StepTiming.END_OF_DAY),
            step("Phone away by 22:00", StepKind.HABIT, 10, StepTiming.END_OF_DAY),
            step("Heating 1°C lower", StepKind.HABIT, 3, StepTiming.END_OF_DAY),
            step("Loofah, not sponge", StepKind.SWAP, 12, StepTiming.END_OF_DAY) { it.copy(category = Category.PRODUCT, alternatives = "Natural loofah\nCotton cloth") },
            step("Own bag shopping", StepKind.SWAP, 150, StepTiming.END_OF_DAY) { it.copy(category = Category.PRODUCT) },
            step("Rinse and sort the recycling", StepKind.HABIT, 100),
            step("Shampoo bar", StepKind.SWAP, 120, StepTiming.END_OF_DAY) { it.copy(category = Category.PRODUCT) },
            step("Nestlé", StepKind.AVOID, 213, StepTiming.END_OF_DAY) {
                it.copy(category = Category.COMPANY, tags = "Ethical,Palm oil,Workers' rights",
                    note = "Boycotted since 1977 over baby formula marketing. I also don't want to fund palm oil.",
                    brands = CompanyBrands.brandsOf("Nestlé").joinToString("\n"),
                    alternatives = "Tony's Chocolonely\nLidl Fairglobe\nLocal roastery coffee\nTap water")
            },
            step("Shein", StepKind.AVOID, 140, StepTiming.END_OF_DAY) { it.copy(category = Category.COMPANY, tags = "Ethical,Workers' rights,Clutter", brands = CompanyBrands.brandsOf("Shein").joinToString("\n")) },
            step("Amazon", StepKind.AVOID, 80, StepTiming.END_OF_DAY) { it.copy(category = Category.COMPANY, verdict = Verdict.EDGE, tags = "Workers' rights", brands = CompanyBrands.brandsOf("Amazon").joinToString("\n")) },
            step("Tony's Chocolonely", StepKind.AVOID, 60, StepTiming.END_OF_DAY) { it.copy(category = Category.COMPANY, verdict = Verdict.FINE, tags = "Ethical") },
            step("Energy drinks", StepKind.AVOID, 20, StepTiming.END_OF_DAY) { it.copy(category = Category.FOOD, tags = "Health") },
            step("Palm oil", StepKind.AVOID, 110, StepTiming.END_OF_DAY) { it.copy(category = Category.FOOD, tags = "Deforestation,Ecological") },
            step("Make your bed", StepKind.HABIT, 67),
            step("Impulse gadgets", StepKind.AVOID, 70, StepTiming.END_OF_DAY) { it.copy(category = Category.PRODUCT, tags = "Clutter") },
        )

        val reasons = AliceDatabase.defaultReasons(this).map { it.name }
        // fixed seed, same demo every run
        val rnd = Random(11)
        val entries = mutableListOf<Entry>()
        steps.filter { !it.oneOff }.forEach { s ->
            var d = LocalDate.ofEpochDay(s.createdEpochDay)
            while (d.isBefore(today)) {
                val days = d.dayOfWeek.value - 1
                val want = when {
                    s.repeat == RepeatMode.DAYS -> s.dayCountList[days]
                    s.repeat == RepeatMode.TIMES -> if (rnd.nextFloat() < 0.3f) 1 else 0
                    else -> s.timesPerDay
                }
                // last two evenings left open for daily check-in to ask
                val leaveOpen = s.timing == StepTiming.END_OF_DAY && !d.isBefore(today.minusDays(2))
                if (want > 0 && !leaveOpen) {
                    if (s.repeat == RepeatMode.TIMES || rnd.nextFloat() < 0.84f) entries += Entry(s.id, d.toEpochDay(), want)
                    else entries += Entry(s.id, d.toEpochDay(), 0, missed = true, reason = if (rnd.nextFloat() < 0.75f) reasons[rnd.nextInt(5)] else null)
                }
                d = d.plusDays(1)
            }
        }
        steps.filter { it.oneOff && it.finishedEpochDay != null }.forEach { entries += Entry(it.id, it.finishedEpochDay!!, 1) }

        val first = today.minusDays(214)
        val checkIns = (3L..214L).map { CheckInDay(ago(it)) }
        // weekly check-ins done every sunday except latest
        val lastSunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val weekly = generateSequence(first.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))) { it.plusWeeks(1) }
            .takeWhile { it.isBefore(lastSunday) }.map { WeeklyDone(it.toEpochDay(), it.toEpochDay()) }.toList()
        // july check-up done, couple of slips
        val july = LocalDate.of(today.year, 7, 1)
        val checkups = if (july.isAfter(first)) listOf(Checkup(july.toEpochDay(), LocalDate.of(today.year, 4, 1).toEpochDay(), july.toEpochDay())) else emptyList()
        val answers = checkups.flatMap { cu -> listOf(CheckupAnswer(cu.dueEpochDay, 19, SlipLevel.ONCE), CheckupAnswer(cu.dueEpochDay, 16, SlipLevel.NEVER)) }

        CoroutineScope(Dispatchers.IO).launch {
            repo.importData(
                BackupData(
                    settings = repo.settingsStore.current.copy(name = "Luce", welcomed = true, tutorialSeen = true, firstEpochDay = first.toEpochDay(), celebrated = setOf(16L, 17L, 18L, 19L, 20L, 21L, 24L, 25L)),
                    steps = steps, entries = entries, checkInDays = checkIns, weekly = weekly,
                    checkups = checkups, answers = answers, reasons = AliceDatabase.defaultReasons(this@DemoDataActivity),
                ),
                add = false,
            )
            finish()
        }
    }
}
