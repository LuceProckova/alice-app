package com.lucyprockova.alice.ui

import android.app.GrammaticalInflectionManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

/** how alice talks to the user in languages where words change with gender, they = neutral wording */
enum class Pronoun(val suffix: String?) { SHE("feminine"), HE("masculine"), THEY(null) }

/** pronoun picked in settings, android 14+ uses the system's grammatical gender, older ones pick by hand */
object Pronouns {
    private const val PREFS = "alice_pronouns"
    private const val KEY = "pronoun"

    /** picked pronoun, they/them when never set */
    fun current(context: Context): Pronoun {
        if (Build.VERSION.SDK_INT >= 34) {
            return when (context.getSystemService(GrammaticalInflectionManager::class.java).applicationGrammaticalGender) {
                Configuration.GRAMMATICAL_GENDER_FEMININE -> Pronoun.SHE
                Configuration.GRAMMATICAL_GENDER_MASCULINE -> Pronoun.HE
                else -> Pronoun.THEY
            }
        }
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return Pronoun.entries.firstOrNull { it.name == saved } ?: Pronoun.THEY
    }

    /** saves the pick, android 14+ redraws every screen itself with the -feminine or -masculine texts */
    fun set(context: Context, p: Pronoun) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY, p.name) }
        if (Build.VERSION.SDK_INT >= 34) {
            context.getSystemService(GrammaticalInflectionManager::class.java).setRequestedApplicationGrammaticalGender(
                when (p) {
                    Pronoun.SHE -> Configuration.GRAMMATICAL_GENDER_FEMININE
                    Pronoun.HE -> Configuration.GRAMMATICAL_GENDER_MASCULINE
                    // not specified, so the neutral wording in the plain folder is used
                    Pronoun.THEY -> Configuration.GRAMMATICAL_GENDER_NOT_SPECIFIED
                },
            )
        }
    }

    /** text for [id] in the picked form; below 14 the key__feminine / key__masculine copy is read by hand */
    fun string(context: Context, @StringRes id: Int, vararg args: Any): String {
        val suffix = current(context).suffix
        if (Build.VERSION.SDK_INT < 34 && suffix != null) {
            val res = context.resources
            @Suppress("DiscouragedApi")
            val variant = res.getIdentifier(res.getResourceEntryName(id) + "__" + suffix, "string", context.packageName)
            if (variant != 0) return text(context, variant, args)
        }
        return text(context, id, args)
    }

    /** below android 14 each screen's texts swap to their key__feminine / key__masculine copy, 14+ uses its own folders */
    fun wrap(base: Context): Context = if (Build.VERSION.SDK_INT >= 34) base else object : ContextWrapper(base) {
        private val res by lazy { GenderedResources(base) }
        override fun getResources(): Resources = res
    }

    // no args = raw text like stringResource, so a %1$s filled in later stays untouched
    private fun text(context: Context, id: Int, args: Array<out Any>) = if (args.isEmpty()) context.getString(id) else context.getString(id, *args)
}

/** stringResource for texts that change with the user's pronouns */
@Composable
fun gString(@StringRes id: Int, vararg args: Any): String = Pronouns.string(LocalContext.current, id, *args)

/** same resources, but a text with a __feminine or __masculine copy gives that copy for the picked pronoun */
@Suppress("DEPRECATION")
private class GenderedResources(private val context: Context) :
    Resources(context.resources.assets, context.resources.displayMetrics, context.resources.configuration) {
    // text id and pronoun to copy id, 0 = no copy, so each name is looked up once
    private val copies = HashMap<Long, Int>()

    // getString and stringResource both end up here
    override fun getText(id: Int): CharSequence = super.getText(copyOf(id))

    private fun copyOf(id: Int): Int {
        val suffix = Pronouns.current(context).suffix ?: return id
        val key = (id.toLong() shl 1) or if (suffix == "feminine") 0L else 1L
        val copy = copies.getOrPut(key) {
            runCatching {
                if (getResourceTypeName(id) != "string") 0
                else getIdentifier(getResourceEntryName(id) + "__" + suffix, "string", context.packageName)
            }.getOrDefault(0)
        }
        return if (copy != 0) copy else id
    }
}
