package com.pedroeu.ficha.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.ui.design.Space

/** One labelled number or short phrase, e.g. "Range: 120 feet" or "Weight: 3 lb". */
data class DetailFact(val label: String, val value: String)

/**
 * Everything the app can say about one thing, in the order it always says it.
 *
 * The shape is the point. A spell, a weapon, an invocation, a species trait and a class
 * feature are different kinds of thing with the same anatomy — what it is called, what kind
 * of thing it is, the handful of numbers you need mid-turn, and the book's own words — and
 * before this each was presented by whichever screen happened to own it. Equipment opened a
 * bespoke sheet, spells opened a different bespoke sheet, features expanded in place, choice
 * options had a "Read the full rules" link, and resource options folded open inline. Five
 * behaviours for one intention.
 *
 * Every field is optional because not every thing has every part; the order never changes.
 */
data class Detail(
    /** What it is called. */
    val title: String,
    /** What kind of thing it is: "Level 3 Evocation", "Martial Weapon", "Eldritch Invocation". */
    val kind: String = "",
    /**
     * The one line a player needs at the table, above the rules text.
     *
     * A spell's damage, a weapon's mastery property, a feature's cost in uses. Left empty
     * where the rules text is itself short.
     */
    val summary: String = "",
    /** The numbers, as label/value pairs. Blank values are dropped. */
    val facts: List<DetailFact> = emptyList(),
    /** The book's own words, in full. */
    val body: String = "",
    /** Where it comes from, or what it asks of you. Shown last, quietly. */
    val footnote: String = "",
)

/**
 * The one way anything's description opens.
 *
 * A bottom sheet, always: it keeps the list underneath in view, dismisses by swiping down
 * wherever you are on the screen, and never loses your place the way a full screen does.
 *
 * [actions] is for the few things that can be done to the subject from here — renaming an
 * item, preparing a spell — and sits below the text so reading is never interrupted by a row
 * of buttons.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailSheet(
    detail: Detail,
    onDismiss: () -> Unit,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.sheetEdge)
                .padding(bottom = Space.sheetBottom),
            verticalArrangement = Arrangement.spacedBy(Space.betweenRows),
        ) {
            // Name and kind, tight together: they are one label in two lines.
            Column(verticalArrangement = Arrangement.spacedBy(Space.tight)) {
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (detail.kind.isNotBlank()) {
                    Text(
                        text = detail.kind,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            if (detail.summary.isNotBlank()) {
                Text(
                    text = detail.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            val facts = detail.facts.filter { it.value.isNotBlank() }
            if (facts.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(Space.inline),
                ) {
                    facts.forEach { fact ->
                        Column(Modifier.width(120.dp)) {
                            Text(
                                text = fact.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = fact.value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // The books run long — Wish is most of a page — so the text scrolls inside the
            // sheet rather than pushing the sheet past the bottom of the screen.
            if (detail.body.isNotBlank()) {
                Text(
                    text = detail.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }

            if (detail.footnote.isNotBlank()) {
                Text(
                    text = detail.footnote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Space.inline)) {}
            actions()
        }
    }
}

/**
 * The first sentence of [text], for the one-line preview under a name.
 *
 * A clean sentence break keeps its full stop and gets no ellipsis — it is a whole sentence,
 * not a fragment. Only a cut made mid-sentence is marked as one.
 */
fun firstSentenceOf(text: String, limit: Int = 110): String {
    val trimmed = text.trim()
    if (trimmed.length <= limit) return trimmed

    val stop = trimmed.indexOf(". ")
    if (stop in 1..limit) return trimmed.take(stop + 1)

    val wordBreak = trimmed.lastIndexOf(' ', limit).coerceAtLeast(1)
    return trimmed.take(wordBreak).trimEnd().trimEnd('.', ',', ';') + "…"
}
