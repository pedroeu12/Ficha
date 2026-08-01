package com.pedroeu.ficha.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/** Which of the two schemes the app shows, or whether it defers to the phone. */
enum class ThemeMode(val label: String) {
    /** Follow the phone's own light/dark setting. */
    SYSTEM("Follow system"),
    LIGHT("Daylight"),
    DARK("Candlelight");

    /** The next mode in the cycle, for a single-tap toggle. */
    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]

    /** True when this mode means dark, given what the phone is currently set to. */
    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDarkTheme
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.find { it.name == name } ?: SYSTEM
    }
}

/**
 * The current mode and a way to change it, reachable from anywhere without threading a
 * callback through every screen. The theme is a genuinely app-wide concern, and the only
 * alternative was passing a setter down through the navigation graph to one app bar.
 */
class ThemeController(
    val mode: ThemeMode,
    /** True for whichever scheme is actually showing, after resolving [ThemeMode.SYSTEM]. */
    val isDark: Boolean,
    val setMode: (ThemeMode) -> Unit,
)

val LocalThemeController = staticCompositionLocalOf {
    ThemeController(ThemeMode.SYSTEM, isDark = false) {}
}
