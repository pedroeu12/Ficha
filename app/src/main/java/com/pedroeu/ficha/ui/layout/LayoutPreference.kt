package com.pedroeu.ficha.ui.layout

import android.content.Context

/**
 * Remembers whether the player wants the tabbed sheet or the full one, across launches.
 *
 * Beside the appearance and language settings, for the same reason: it belongs to the person
 * and their device, not to any one character, and losing it means falling back to matching the
 * screen — which is the sensible answer anyway.
 */
class LayoutPreference(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun load(): LayoutMode = LayoutMode.fromName(prefs.getString(KEY_LAYOUT, null))

    fun save(mode: LayoutMode) {
        prefs.edit().putString(KEY_LAYOUT, mode.name).apply()
    }

    private companion object {
        const val FILE_NAME = "ficha_appearance"
        const val KEY_LAYOUT = "layout_mode"
    }
}
