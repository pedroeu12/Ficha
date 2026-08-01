package com.pedroeu.ficha.ui.theme

import android.content.Context

/**
 * Remembers which scheme the player chose, across launches.
 *
 * This is a single string, so it lives in SharedPreferences rather than in the character
 * database — there is nothing to migrate and nothing to lose if it's ever cleared, since an
 * unset value simply means "follow the phone".
 */
class ThemePreference(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun load(): ThemeMode = ThemeMode.fromName(prefs.getString(KEY_MODE, null))

    fun save(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    private companion object {
        const val FILE_NAME = "ficha_appearance"
        const val KEY_MODE = "theme_mode"
    }
}
