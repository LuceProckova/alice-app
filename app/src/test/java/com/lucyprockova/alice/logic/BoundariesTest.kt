package com.lucyprockova.alice.logic

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** next moment the screens need a fresh now: check-in hour or midnight, also on dst days */
class BoundariesTest {
    private val vienna = ZoneId.of("Europe/Vienna")
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int = 0) = ZonedDateTime.of(LocalDateTime.of(y, mo, d, h, mi), vienna)

    // before the check-in hour the next boundary is the check-in hour today
    @Test fun beforeCheckIn() = assertEquals(at(2026, 10, 7, 20), Boundaries.next(at(2026, 10, 7, 14, 30), 20))

    // after the check-in hour it is midnight
    @Test fun afterCheckIn() = assertEquals(at(2026, 10, 8, 0), Boundaries.next(at(2026, 10, 7, 21, 15), 20))

    // exactly at the check-in hour it is not that same moment again
    @Test fun atCheckIn() = assertEquals(at(2026, 10, 8, 0), Boundaries.next(at(2026, 10, 7, 20), 20))

    // just after midnight the check-in hour comes before the next midnight
    @Test fun afterMidnight() = assertEquals(at(2026, 10, 8, 20), Boundaries.next(at(2026, 10, 8, 0, 1), 20))

    // check-in at midnight is the same boundary as the new day
    @Test fun checkInAtMidnight() = assertEquals(at(2026, 10, 8, 0), Boundaries.next(at(2026, 10, 7, 22), 0))

    // clocks go back 25 oct 2026: the 25 hour day still ends at the real midnight
    @Test fun dstAutumnDay() {
        val next = Boundaries.next(at(2026, 10, 25, 21), 20)
        assertEquals(at(2026, 10, 26, 0), next)
    }

    // clocks go forward 29 mar 2026 at 2:00, a check-in at 2 moves to 3:00 that day
    @Test fun dstSpringGap() {
        val next = Boundaries.next(at(2026, 3, 29, 1), 2)
        assertEquals(ZonedDateTime.of(LocalDateTime.of(2026, 3, 29, 3, 0), vienna), next)
    }

    // an extra hour like the nudge counts when it comes first
    @Test fun extraHourFirst() = assertEquals(at(2026, 10, 7, 18), Boundaries.next(at(2026, 10, 7, 9), 20, listOf(18)))
}
