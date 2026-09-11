package com.pedroeu.ficha.ui.components

import androidx.compose.runtime.Composable
import com.pedroeu.ficha.ui.design.LONG_TEXT_CHARS

/**
 * The full rules text for one thing.
 *
 * Kept as a name because half the app calls it, but it is now one line: every description in
 * the app opens the same [DetailSheet], with the same anatomy and the same gesture. What used
 * to be four sheets and two inline expanders is one sheet.
 */
@Composable
fun RulesTextSheet(
    title: String,
    body: String,
    onDismiss: () -> Unit,
    /** A line under the title: the level it arrives at, the book, the spell's school. */
    subtitle: String = "",
    /** Extra label/value rows above the text, e.g. a spell's casting time and range. */
    facts: List<Pair<String, String>> = emptyList(),
) {
    DetailSheet(
        detail = Detail(
            title = title,
            kind = subtitle,
            facts = facts.map { (label, value) -> DetailFact(label, value) },
            body = body,
        ),
        onDismiss = onDismiss,
    )
}

/**
 * How long a description has to be before reading it inline stops being reasonable.
 *
 * The rule lives in the design tokens now; this is the name the existing callers use.
 */
const val LONG_TEXT_THRESHOLD = LONG_TEXT_CHARS
