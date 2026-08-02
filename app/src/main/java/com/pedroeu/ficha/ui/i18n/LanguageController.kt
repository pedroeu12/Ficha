package com.pedroeu.ficha.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The current language and a way to change it, reachable from any screen.
 *
 * Split out from [AppLanguage] and [tr] so that the lookup itself stays free of Compose: the
 * table and the fallback rule are worth testing off the device, and a CompositionLocal in the
 * same file would drag the whole toolkit into that test.
 */
class LanguageController(
    val language: AppLanguage,
    val setLanguage: (AppLanguage) -> Unit,
)

val LocalLanguageController = staticCompositionLocalOf {
    LanguageController(AppLanguage.ENGLISH) {}
}
