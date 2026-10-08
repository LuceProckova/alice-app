package com.lucyprockova.alice.data

import android.content.Context

/** idea list, matches choices on new step screen, titles in ui */
enum class IdeaGroup {
    HABIT_ANYTIME,
    HABIT_END,
    SWAP,
    AVOID_PRODUCT,
    AVOID_COMPANY,
    AVOID_FOOD,
}

/** ready-made step idea plus why */
data class Idea(val group: IdeaGroup, val name: String, val why: String)

/** 300 hand-written ideas, read once from assets/ideas.tsv */
object Ideas {
    // one list per file, language can change while the app runs
    private val cache = mutableMapOf<String, List<Idea>>()

    /** ideas in the app language if translated (ideas-sk.tsv...), else the english ideas.tsv */
    fun all(context: Context): List<Idea> = synchronized(this) {
        // canonical tag from the picker list, so zh-Hant-TW, iw and in find their files
        val tag = Languages.of(context.resources.configuration.locales[0].toLanguageTag())?.tag ?: "en"
        val file = fileFor(tag, context.assets.list("").orEmpty().toSet())
        cache.getOrPut(file) { parse(context.assets.open(file).bufferedReader().readLines()) }
    }

    /** which ideas file to read for a language tag, english when there is no translation */
    fun fileFor(tag: String, files: Set<String>): String = "ideas-$tag.tsv".takeIf { it in files } ?: "ideas.tsv"

    /** tsv lines to ideas, bad or unknown group lines skipped, never a crash */
    fun parse(lines: List<String>): List<Idea> = lines.mapNotNull { line ->
        val p = line.split('\t')
        val group = IdeaGroup.entries.firstOrNull { it.name == p[0] }
        if (p.size < 3 || group == null) null else Idea(group, p[1], p[2])
    }

    fun groupFor(kind: StepKind, timing: StepTiming, category: Category?): IdeaGroup = when (kind) {
        StepKind.HABIT -> if (timing == StepTiming.END_OF_DAY) IdeaGroup.HABIT_END else IdeaGroup.HABIT_ANYTIME
        StepKind.SWAP -> IdeaGroup.SWAP
        StepKind.AVOID -> when (category) {
            Category.COMPANY -> IdeaGroup.AVOID_COMPANY
            Category.FOOD -> IdeaGroup.AVOID_FOOD
            else -> IdeaGroup.AVOID_PRODUCT
        }
    }
}

/** big companies' brands, prefilled on a new company avoid */
object CompanyBrands {
    private val data: Map<String, List<String>> = mapOf(
        "nestle" to listOf("KitKat", "Nescafé", "Nespresso", "San Pellegrino", "Perrier", "Purina", "Smarties", "Maggi", "Nesquik", "Garden Gourmet", "Aero", "Buitoni", "Vittel", "Acqua Panna", "Cheerios (outside US)", "Shreddies", "Felix", "Gerber", "Häagen-Dazs (outside US)", "Milo", "Coffee-mate", "Lion", "Polo", "Quality Street", "Rolo", "After Eight", "Shredded Wheat", "Thomy", "Wagner", "Herta"),
        "unilever" to listOf("Dove", "Axe", "Rexona", "Hellmann's", "Knorr", "Magnum", "Ben & Jerry's", "Lipton (ice tea)", "Persil (UK)", "Domestos", "Cif", "Vaseline", "Sunsilk", "TRESemmé", "Marmite", "Pot Noodle", "Cornetto", "Carte d'Or"),
        "coca-cola" to listOf("Coca-Cola", "Fanta", "Sprite", "Costa Coffee", "Innocent", "Powerade", "Smartwater", "Schweppes (some countries)", "Minute Maid", "Fuze Tea", "Monster (part owner)", "Glacéau Vitaminwater"),
        "pepsico" to listOf("Pepsi", "7UP (outside US)", "Lay's", "Walkers", "Doritos", "Cheetos", "Quaker", "Tropicana (part)", "Gatorade", "Mountain Dew", "Lipton (bottled, part)", "Alvalle", "SodaStream"),
        "mondelez" to listOf("Cadbury", "Milka", "Oreo", "Toblerone", "Côte d'Or", "Belvita", "Ritz", "Philadelphia (outside US)", "Trident", "Halls", "Daim", "Freia", "Marabou", "TUC"),
        "amazon" to listOf("Amazon Basics", "Whole Foods Market", "Audible", "Twitch", "IMDb", "Kindle", "Ring", "Alexa", "Prime Video", "Zappos", "AbeBooks", "Goodreads"),
        "shein" to listOf("Shein", "Romwe", "Missguided", "Cider (partner)", "Emery Rose", "Dazy"),
        "temu" to listOf("Temu", "Pinduoduo"),
        "primark" to listOf("Primark", "Penneys"),
        "zara" to listOf("Zara", "Pull&Bear", "Bershka", "Massimo Dutti", "Stradivarius", "Oysho", "Zara Home", "Lefties"),
        "inditex" to listOf("Zara", "Pull&Bear", "Bershka", "Massimo Dutti", "Stradivarius", "Oysho", "Zara Home", "Lefties"),
        "ryanair" to listOf("Ryanair", "Buzz", "Lauda Europe", "Malta Air"),
        "mcdonald's" to listOf("McDonald's", "McCafé"),
        "mars" to listOf("Mars", "Snickers", "Twix", "M&M's", "Bounty", "Milky Way", "Skittles", "Pedigree", "Whiskas", "Royal Canin", "Uncle Ben's (Ben's Original)", "Dolmio", "Extra", "Wrigley's"),
        "procter & gamble" to listOf("Ariel", "Pampers", "Gillette", "Oral-B", "Head & Shoulders", "Pantene", "Fairy", "Lenor", "Always", "Febreze", "Braun", "Olay"),
    )

    // so nestlé and nestle both match
    private fun key(name: String) = name.lowercase().replace("é", "e").trim()

    fun brandsOf(company: String): List<String> = data[key(company)] ?: emptyList()
}
