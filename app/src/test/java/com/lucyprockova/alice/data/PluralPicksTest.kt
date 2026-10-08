package com.lucyprockova.alice.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** stepper unit words follow the number like android picks them, slovak, czech, russian, ukrainian */
class PluralPicksTest {

    /** cldr plural category for whole numbers, same rules android uses */
    private fun category(lang: String, n: Int): String = when (lang) {
        "sk", "cs" -> when (n) { 1 -> "one"; in 2..4 -> "few"; else -> "other" }
        "ru", "uk" -> when {
            n % 10 == 1 && n % 100 != 11 -> "one"
            n % 10 in 2..4 && n % 100 !in 12..14 -> "few"
            else -> "many"
        }
        else -> error("no rule for $lang")
    }

    /** the word android would show next to [n] for plurals [key] in [lang] */
    private fun word(lang: String, key: String, n: Int): String {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/values-$lang/strings.xml"))
        val plurals = doc.getElementsByTagName("plurals")
        val node = (0 until plurals.length).map { plurals.item(it) as Element }.single { it.getAttribute("name") == key }
        val items = node.getElementsByTagName("item")
        val byQuantity = (0 until items.length).map { items.item(it) as Element }.associate { it.getAttribute("quantity") to it.textContent }
        return byQuantity[category(lang, n)] ?: byQuantity.getValue("other")
    }

    // slovak days, weeks, months: 1, 2 to 4, 5 and up
    @Test fun slovak() {
        assertEquals(listOf("deň", "dni", "dni", "dní", "dní"), listOf(1, 2, 4, 5, 66).map { word("sk", "forms_unit_days_n", it) })
        assertEquals(listOf("týždeň", "týždne", "týždňov"), listOf(1, 3, 8).map { word("sk", "forms_unit_weeks", it) })
        assertEquals(listOf("mesiac", "mesiace", "mesiacov"), listOf(1, 4, 12).map { word("sk", "forms_unit_months", it) })
    }

    // czech same three steps
    @Test fun czech() {
        assertEquals(listOf("den", "dny", "dní"), listOf(1, 3, 14).map { word("cs", "forms_unit_days_n", it) })
        assertEquals(listOf("týden", "týdny", "týdnů"), listOf(1, 2, 5).map { word("cs", "forms_unit_weeks", it) })
        assertEquals(listOf("měsíc", "měsíce", "měsíců"), listOf(1, 2, 12).map { word("cs", "forms_unit_months", it) })
    }

    // russian, 21 is one again, 11 to 14 are many
    @Test fun russian() {
        assertEquals(listOf("день", "дня", "дней", "дней", "день", "дня"), listOf(1, 2, 5, 11, 21, 22).map { word("ru", "forms_unit_days_n", it) })
        assertEquals(listOf("месяц", "месяца", "месяцев"), listOf(1, 3, 12).map { word("ru", "forms_unit_months", it) })
    }

    // ukrainian, same shape as russian
    @Test fun ukrainian() {
        assertEquals(listOf("день", "дні", "днів", "днів", "день"), listOf(1, 4, 5, 12, 31).map { word("uk", "forms_unit_days_n", it) })
        assertEquals(listOf("тиждень", "тижні", "тижнів"), listOf(1, 2, 7).map { word("uk", "forms_unit_weeks", it) })
    }
}
