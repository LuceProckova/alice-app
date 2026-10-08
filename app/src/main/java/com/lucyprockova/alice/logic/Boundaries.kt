package com.lucyprockova.alice.logic

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** when the screens need a fresh "now": the check-in hour (evening steps unlock) and midnight (new day) */
object Boundaries {

    /** first moment after [now] when something on screen changes, earliest of check-in hour, midnight and [extraHours] */
    fun next(now: ZonedDateTime, checkInHour: Int, extraHours: List<Int> = emptyList()): ZonedDateTime {
        val zone = now.zone
        val today = now.toLocalDate()
        // start of tomorrow in this zone, so a 23 or 25 hour dst day still lands on the real midnight
        val midnight = today.plusDays(1).atStartOfDay(zone)
        val hours = (extraHours + checkInHour).map { h ->
            // atZone moves a time inside a dst gap forward, so the unlock still happens that day
            val t = today.atTime(h, 0).atZone(zone)
            if (t.isAfter(now)) t else today.plusDays(1).atTime(h, 0).atZone(zone)
        }
        return (hours + midnight).minOf { it }
    }
}

/** clock that always reads the phone's current time zone, a changed zone shows straight away */
object PhoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()
    override fun withZone(zone: ZoneId): Clock = system(zone)
    override fun instant(): Instant = Instant.now()
}
