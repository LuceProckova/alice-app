package com.lucyprockova.alice.ui

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/** app language picked in settings, null = follow the phone */
object AppLanguage {
    private const val PREFS = "alice_language"
    private const val KEY = "tag"

    /** picked tag, null when following the phone */
    fun current(context: Context): String? =
        if (Build.VERSION.SDK_INT >= 33) {
            // android 13+ keeps it itself, and shows it in system settings too
            context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags().ifEmpty { null }
        } else context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)

    /** saves the choice, android 13+ recreates every screen itself, older ones need [restart] */
    fun set(context: Context, tag: String?) {
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { if (tag == null) remove(KEY) else putString(KEY, tag) }
            // back to the phone's own language for date names too
            if (tag == null) Locale.setDefault(Resources.getSystem().configuration.locales[0])
        }
    }

    /** before android 13 each screen's context gets the picked language by hand */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val tag = current(base) ?: return base
        val locale = Locale.forLanguageTag(tag)
        // default locale too, so date names follow
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale); setLayoutDirection(locale) }
        return base.createConfigurationContext(config)
    }
}
