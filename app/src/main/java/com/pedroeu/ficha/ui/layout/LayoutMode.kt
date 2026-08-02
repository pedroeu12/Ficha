package com.pedroeu.ficha.ui.layout

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.ui.i18n.tr

/**
 * Which shape the character sheet takes.
 *
 * A phone can only ever show one column, so the sheet is a row of tabs. A tablet has room for
 * the printed sheet's actual arrangement — abilities and skills down the side, attacks and
 * features filling the width beside them — and paging through seven tabs on a ten-inch screen
 * hides two thirds of what would fit.
 */
enum class LayoutMode(private val labelKey: String) {
    /** Decide from the width of the window, which also handles a folding phone opening. */
    AUTOMATIC("Match the screen"),
    PHONE("Phone — tabs"),
    TABLET("Tablet — full sheet");

    /** Translated on read, since an enum's constructor runs only once. */
    val label: String get() = tr(labelKey)

    fun next(): LayoutMode = entries[(ordinal + 1) % entries.size]

    /** Whether to lay the sheet out in columns, given how wide the window actually is. */
    fun isWide(availableWidth: Dp): Boolean = when (this) {
        AUTOMATIC -> availableWidth >= WIDE_THRESHOLD
        PHONE -> false
        TABLET -> true
    }

    companion object {
        /**
         * The width at which the three-column sheet stops being cramped.
         *
         * Material's own "expanded" breakpoint, which is roughly a small tablet in landscape
         * or a large one in portrait. Below it the columns would each be narrower than a
         * phone, which is worse than tabs rather than better.
         */
        val WIDE_THRESHOLD: Dp = 840.dp

        fun fromName(name: String?): LayoutMode = entries.find { it.name == name } ?: AUTOMATIC
    }
}

/** The current layout choice and a way to change it, reachable from any screen. */
class LayoutController(
    val mode: LayoutMode,
    val setMode: (LayoutMode) -> Unit,
)

val LocalLayoutController = staticCompositionLocalOf {
    LayoutController(LayoutMode.AUTOMATIC) {}
}

/**
 * Splits [this] into [count] groups, keeping the printed sheet's order.
 *
 * Earlier groups take the extra element, because the front of the sheet is read left to right
 * and the leftmost columns are the ones a player looks at most. A count of zero or one leaves
 * the list whole, and a count larger than the list simply gives every element its own group.
 */
internal fun <T> List<T>.distributeInto(count: Int): List<List<T>> {
    if (count <= 1 || isEmpty()) return listOf(this)

    val groups = count.coerceAtMost(size)
    val base = size / groups
    val extra = size % groups

    var index = 0
    return (0 until groups).map { group ->
        val take = base + if (group < extra) 1 else 0
        subList(index, index + take).also { index += take }
    }
}
