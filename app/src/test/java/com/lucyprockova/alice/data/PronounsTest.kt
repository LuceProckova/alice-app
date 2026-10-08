package com.lucyprockova.alice.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** gendered texts: neutral, feminine and masculine all there, and the by-hand copies match the android 14 folders */
class PronounsTest {
    private val res = File("src/main/res")

    /** all plain strings of one file, name to text */
    private fun strings(folder: String): Map<String, String> {
        val f = File(res, "$folder/strings.xml")
        if (!f.exists()) return emptyMap()
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f).getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) as Element }.associate { it.getAttribute("name") to it.textContent }
    }

    /** all plurals of one file, name to its items */
    private fun plurals(folder: String): Map<String, Map<String, String>> {
        val f = File(res, "$folder/strings.xml")
        if (!f.exists()) return emptyMap()
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f).getElementsByTagName("plurals")
        return (0 until nodes.length).map { nodes.item(it) as Element }.associate { p ->
            val items = p.getElementsByTagName("item")
            p.getAttribute("name") to (0 until items.length).map { items.item(it) as Element }.associate { it.getAttribute("quantity") to it.textContent }
        }
    }

    // slovak, czech, russian, arabic, hebrew, amharic, hausa, urdu: each gendered text has a neutral, a feminine and a masculine version, and they differ
    @Test fun checkedLanguagesHaveAllThreeForms() {
        listOf("sk", "cs", "ru", "ar", "iw", "am", "ha", "ur").forEach { lang ->
            val neutral = strings("values-$lang")
            val fem = strings("values-$lang-feminine")
            val mas = strings("values-$lang-masculine")
            assertTrue("$lang has no gendered texts", fem.isNotEmpty())
            assertEquals("$lang feminine and masculine keys", fem.keys, mas.keys)
            fem.keys.forEach { k ->
                assertTrue("$lang $k has no neutral text", neutral.containsKey(k))
                assertNotEquals("$lang $k feminine same as masculine", fem[k], mas[k])
            }
        }
    }

    // the 5 rtl and african languages: neutral is its own wording, never just the masculine one, plurals too
    @Test fun neutralIsNotMasculine() {
        listOf("ar", "iw", "am", "ha", "ur").forEach { lang ->
            val neutral = strings("values-$lang")
            strings("values-$lang-masculine").forEach { (k, v) -> assertNotEquals("$lang $k neutral is masculine", v, neutral[k]) }
            val neutralP = plurals("values-$lang")
            val fem = plurals("values-$lang-feminine")
            val mas = plurals("values-$lang-masculine")
            assertEquals("$lang feminine and masculine plurals", fem.keys, mas.keys)
            mas.forEach { (k, items) ->
                assertEquals("$lang $k quantities", neutralP[k]?.keys, items.keys)
                assertEquals("$lang $k quantities", fem[k]?.keys, items.keys)
                assertNotEquals("$lang $k feminine plural same as masculine", fem[k], items)
                assertNotEquals("$lang $k neutral plural is masculine", neutralP[k], items)
            }
        }
    }

    // below android 14 the app reads key__feminine and key__masculine, they must equal the android 14 folders
    @Test fun byHandCopiesMatchTheFolders() {
        val aliases = strings("values").keys.filter { it.contains("__") }
        res.listFiles { f -> f.name.endsWith("-feminine") }!!.forEach { dir ->
            val lang = dir.name.removePrefix("values-").removeSuffix("-feminine")
            val plain = strings("values-$lang")
            listOf("feminine", "masculine").forEach { g ->
                strings("values-$lang-$g").forEach { (k, v) ->
                    assertEquals("$lang $k $g copy", v, plain["${k}__$g"])
                    assertTrue("default values miss alias ${k}__$g", "${k}__$g" in aliases)
                }
            }
        }
    }
}
