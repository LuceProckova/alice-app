package com.lucyprockova.alice.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** settings json, ideas file parsing, company brand lookup */
class SettingsIdeasTest {

    // every setting survives a save and load
    @Test fun settingsRoundTrip() {
        val s = AliceSettings(
            name = "Luce", checkInHour = 19, weekStartsSunday = true, newStepEveryWeeks = 2, weeklyDay = 3, ownCheckup = true,
            checkupStartEpochDay = 20000, checkupEveryMonths = 2, checkupDayOfMonth = 31, daysToBuild = 40, nudgeOn = false,
            nudgeHour = 8, theme = 5, summaryStyle = 7, welcomed = true, tutorialSeen = true, firstEpochDay = 19990,
            hiddenIdeas = setOf("Read 10 pages"), celebrated = setOf(3L, 9L), glanceShownFor = 20001, summaryShownFor = 20002,
        )
        assertEquals(s, AliceSettings.fromJson(s.toJson()))
    }

    // missing keys in an old save fall back to defaults
    @Test fun oldSettingsGetDefaults() {
        val s = AliceSettings.fromJson(org.json.JSONObject("""{"name":"Luce"}"""))
        assertEquals("Luce", s.name)
        assertEquals(AliceSettings.DEFAULT_DAYS_TO_BUILD, s.daysToBuild)
    }

    // good lines read, short or unknown group lines skipped, no crash
    @Test fun ideasParseSkipsBadLines() {
        val tab = '\t'
        val ideas = Ideas.parse(listOf("HABIT_ANYTIME${tab}Read 10 pages${tab}Reading daily builds focus.", "NOT_A_GROUP${tab}x${tab}y", "SWAP${tab}only two", ""))
        assertEquals(listOf(Idea(IdeaGroup.HABIT_ANYTIME, "Read 10 pages", "Reading daily builds focus.")), ideas)
    }

    // brands found with or without the accent, unknown company gives none
    @Test fun companyBrands() {
        assertTrue("KitKat" in CompanyBrands.brandsOf("Nestlé"))
        assertEquals(CompanyBrands.brandsOf("Nestlé"), CompanyBrands.brandsOf(" nestle "))
        assertTrue(CompanyBrands.brandsOf("Corner shop").isEmpty())
    }
}
