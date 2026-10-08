package com.lucyprockova.alice.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** every language has every text, and the picker, folders and system list agree */
class TranslationsTest {
    /** column separator in the ideas files */
    private val TAB = 9.toChar()

    // unit tests run inside the app module folder
    private val res = File("src/main/res")

    /** android folder for a language tag, old names for hebrew and indonesian */
    private fun folder(tag: String) = when (tag) {
        "en" -> "values"; "he" -> "values-iw"; "id" -> "values-in"; "fil" -> "values-b+fil"
        "zh-Hans" -> "values-b+zh+Hans"; "zh-Hant" -> "values-b+zh+Hant"
        else -> "values-$tag"
    }

    /** translatable keys of one strings.xml, plurals as plurals:name */
    private fun keys(file: File): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val out = mutableSetOf<String>()
        listOf("string", "plurals", "string-array").forEach { tag ->
            val nodes = doc.getElementsByTagName(tag)
            for (i in 0 until nodes.length) {
                val e = nodes.item(i) as Element
                if (e.getAttribute("translatable") != "false") out += (if (tag == "string") "" else "$tag:") + e.getAttribute("name")
            }
        }
        return out
    }

    // no language misses a text, english would leak in otherwise
    @Test fun everyLanguageHasEveryKey() {
        val english = keys(File(res, "values/strings.xml"))
        Languages.ALL.filter { it.tag != "en" }.forEach { l ->
            val f = File(res, "${folder(l.tag)}/strings.xml")
            assertTrue("${l.english} has no strings.xml", f.exists())
            val missing = english - keys(f)
            assertTrue("${l.english} misses ${missing.take(5)}", missing.isEmpty())
        }
    }

    // picker list = locales_config = 4 checked + 52 others, no doubles
    @Test fun pickerMatchesLocalesConfig() {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "xml/locales_config.xml"))
        val nodes = doc.getElementsByTagName("locale")
        val config = (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute("android:name") }
        assertEquals(Languages.ALL.map { it.tag }, config)
        assertEquals(4, Languages.CHECKED.size)
        assertEquals(52, Languages.UNCHECKED.size)
        assertEquals(56, Languages.ALL.map { it.tag }.toSet().size)
    }

    // tags from the system find the right language
    @Test fun tagLookup() {
        assertEquals("en", Languages.of("en-GB")?.tag)
        assertEquals("zh-Hans", Languages.of("zh-Hans-CN")?.tag)
        assertEquals("zh-Hant", Languages.of("zh-TW")?.tag)
        assertEquals("he", Languages.of("iw")?.tag)
        assertEquals(null, Languages.of("xx"))
    }

    // search finds by native or english name
    @Test fun searchByEitherName() {
        assertTrue(Languages.ALL.single { Languages.matches(it, "slov") && it.tag == "sk" }.tag == "sk")
        assertTrue(Languages.matches(Languages.of("ru")!!, "русс"))
        assertTrue(Languages.matches(Languages.of("ru")!!, "russ"))
    }

    // every language has its own 300 ideas, line by line the same group as the english list
    @Test fun everyLanguageHasAll300Ideas() {
        val assets = File("src/main/assets")
        val english = File(assets, "ideas.tsv").readLines().map { it.substringBefore(TAB) }
        assertEquals(300, english.size)
        Languages.ALL.filter { it.tag != "en" }.forEach { l ->
            val f = File(assets, "ideas-${l.tag}.tsv")
            assertTrue("${l.english} has no ideas file", f.exists())
            val lines = f.readLines(Charsets.UTF_8).filter { it.isNotBlank() }
            assertEquals("${l.english} idea count", 300, lines.size)
            lines.forEachIndexed { i, line ->
                val parts = line.split(TAB)
                assertEquals("${l.english} line ${i + 1} fields", 3, parts.size)
                assertEquals("${l.english} line ${i + 1} group", english[i], parts[0])
                assertTrue("${l.english} line ${i + 1} empty", parts[1].isNotBlank() && parts[2].isNotBlank())
            }
        }
    }

    // ideas file per language, english when not translated
    @Test fun ideasFileFallsBackToEnglish() {
        val files = setOf("ideas.tsv", "ideas-sk.tsv")
        assertEquals("ideas-sk.tsv", Ideas.fileFor("sk", files))
        assertEquals("ideas.tsv", Ideas.fileFor("de", files))
    }
}
