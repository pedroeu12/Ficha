package com.pedroeu.ficha.ui.i18n

/** The languages the interface is available in. */
enum class AppLanguage(val tag: String, val label: String) {
    ENGLISH("en", "English"),
    PORTUGUESE("pt-BR", "Português (Brasil)");

    /** The next language in the cycle, for a single-tap toggle. */
    fun next(): AppLanguage = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.find { it.tag == tag } ?: ENGLISH
    }
}

/**
 * The language in force, held outside the composition.
 *
 * [tr] is called from ordinary functions as well as composables — a picker's subtitle is built
 * in a `remember` block, a rest summary in a plain helper — and threading a CompositionLocal
 * through all of them would mean marking half the app `@Composable` for the sake of a lookup.
 * The setter is only ever called from [LanguageController], which changes the state the whole
 * app is keyed on, so a change still redraws everything rather than leaving stale text behind.
 */
object Language {
    @Volatile
    var current: AppLanguage = AppLanguage.ENGLISH
        internal set
}

/**
 * The English text, translated into whatever language is in force.
 *
 * Keyed by the English string itself rather than by a symbolic name. That means an untranslated
 * string falls back to reading correctly in English instead of showing a bare key, and it means
 * adding a string to the interface can never crash — the worst case is that one phrase stays in
 * English until someone translates it.
 */
fun tr(text: String): String = when (Language.current) {
    AppLanguage.ENGLISH -> text
    AppLanguage.PORTUGUESE -> PortugueseStrings.of(text)
}

/**
 * The same, for a phrase with values in it: `trf("Level {0} — {1}", level, name)`.
 *
 * Numbered placeholders rather than the values spliced in directly, because word order isn't
 * the same in every language — "Delete {0}?" becomes "Excluir {0}?" here, but a language that
 * put the name first would still have somewhere to put it. A translation that leaves out a
 * placeholder simply drops that value rather than failing.
 */
fun trf(text: String, vararg args: Any?): String {
    var out = tr(text)
    args.forEachIndexed { index, value -> out = out.replace("{$index}", value.toString()) }
    return out
}
