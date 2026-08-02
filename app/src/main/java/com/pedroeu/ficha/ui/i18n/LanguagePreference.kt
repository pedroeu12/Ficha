package com.pedroeu.ficha.ui.i18n

import android.content.Context

/**
 * Remembers the chosen language across launches.
 *
 * Kept beside the appearance setting rather than in the character database: it belongs to the
 * person using the app, not to any one character, and losing it means falling back to English
 * rather than losing anything.
 */
class LanguagePreference(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun load(): AppLanguage = AppLanguage.fromTag(prefs.getString(KEY_LANGUAGE, null))

    fun save(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.tag).apply()
    }

    private companion object {
        const val FILE_NAME = "ficha_appearance"
        const val KEY_LANGUAGE = "app_language"
    }
}
