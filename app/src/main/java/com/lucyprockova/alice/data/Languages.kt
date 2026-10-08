package com.lucyprockova.alice.data

/** one language alice speaks, [tag] for android, name in itself and in english */
data class Language(val tag: String, val native: String, val english: String)

/** every language in the picker, english then the 3 lucy checked, then the rest by english name */
object Languages {
    /** checked by lucy herself, the rest wait for native speakers */
    val CHECKED = listOf(
        Language("en", "English", "English"),
        Language("sk", "Slovenčina", "Slovak"),
        Language("cs", "Čeština", "Czech"),
        Language("ru", "Русский", "Russian"),
    )

    /** not approved yet, sorted by english name like the mockup */
    val UNCHECKED = listOf(
        Language("am", "አማርኛ", "Amharic"),
        Language("ar", "العربية", "Arabic"),
        Language("bn", "বাংলা", "Bengali"),
        Language("bg", "Български", "Bulgarian"),
        Language("zh-Hans", "中文 (简体)", "Chinese, Simplified"),
        Language("zh-Hant", "中文 (繁體)", "Chinese, Traditional"),
        Language("hr", "Hrvatski", "Croatian"),
        Language("da", "Dansk", "Danish"),
        Language("nl", "Nederlands", "Dutch"),
        Language("et", "Eesti", "Estonian"),
        Language("fil", "Filipino", "Filipino"),
        Language("fi", "Suomi", "Finnish"),
        Language("fr", "Français", "French"),
        Language("de", "Deutsch", "German"),
        Language("el", "Ελληνικά", "Greek"),
        Language("gu", "ગુજરાતી", "Gujarati"),
        Language("ha", "Hausa", "Hausa"),
        Language("he", "עברית", "Hebrew"),
        Language("hi", "हिन्दी", "Hindi"),
        Language("hu", "Magyar", "Hungarian"),
        Language("id", "Bahasa Indonesia", "Indonesian"),
        Language("it", "Italiano", "Italian"),
        Language("ja", "日本語", "Japanese"),
        Language("kn", "ಕನ್ನಡ", "Kannada"),
        Language("kk", "Қазақша", "Kazakh"),
        Language("ko", "한국어", "Korean"),
        Language("lv", "Latviešu", "Latvian"),
        Language("lt", "Lietuvių", "Lithuanian"),
        Language("ms", "Bahasa Melayu", "Malay"),
        Language("ml", "മലയാളം", "Malayalam"),
        Language("mr", "मराठी", "Marathi"),
        Language("nb", "Norsk", "Norwegian"),
        Language("fa", "فارسی", "Persian"),
        Language("pl", "Polski", "Polish"),
        Language("pt", "Português", "Portuguese"),
        Language("pa", "ਪੰਜਾਬੀ", "Punjabi"),
        Language("ro", "Română", "Romanian"),
        Language("sr", "Српски", "Serbian"),
        Language("sl", "Slovenščina", "Slovenian"),
        Language("es", "Español", "Spanish"),
        Language("sw", "Kiswahili", "Swahili"),
        Language("sv", "Svenska", "Swedish"),
        Language("ta", "தமிழ்", "Tamil"),
        Language("te", "తెలుగు", "Telugu"),
        Language("th", "ไทย", "Thai"),
        Language("tr", "Türkçe", "Turkish"),
        Language("uk", "Українська", "Ukrainian"),
        Language("ur", "اردو", "Urdu"),
        Language("uz", "Oʻzbekcha", "Uzbek"),
        Language("vi", "Tiếng Việt", "Vietnamese"),
        Language("yo", "Yorùbá", "Yoruba"),
        Language("zu", "isiZulu", "Zulu"),
    )

    val ALL = CHECKED + UNCHECKED

    /** language for an android tag, drops region parts till one fits, so en-GB finds english and zh-Hans-CN finds simplified */
    fun of(tag: String?): Language? {
        var t = tag ?: return null
        while (t.isNotEmpty()) {
            ALL.firstOrNull { it.tag.equals(t, true) }?.let { return it }
            // old android names for hebrew, indonesian, norwegian, and chinese by region
            when (t.lowercase()) {
                "iw" -> return of("he"); "in" -> return of("id"); "no" -> return of("nb")
                "zh-tw", "zh-hk", "zh-mo" -> return of("zh-Hant"); "zh", "zh-cn", "zh-sg" -> return of("zh-Hans")
            }
            t = if ('-' in t) t.substringBeforeLast('-') else ""
        }
        return null
    }

    /** search box match, native or english name, any case */
    fun matches(l: Language, query: String): Boolean =
        query.isBlank() || l.native.contains(query.trim(), true) || l.english.contains(query.trim(), true)
}
