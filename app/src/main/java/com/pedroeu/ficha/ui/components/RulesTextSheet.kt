package com.pedroeu.ficha.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The full rules text for one thing, in a sheet of its own.
 *
 * The catalogues carry the books' own wording now, which is several paragraphs for anything
 * interesting. Printing that inline turns a list of six features into a page of prose you have
 * to scroll past to reach the seventh, so the lists show the name and open this on a tap —
 * the same shape the equipment tab already used for items.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            if (facts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    facts.filter { it.second.isNotBlank() }.forEach { (label, value) ->
                        Text(
                            text = "$label: $value",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // The books run long — Wish is the better part of a page — so the text scrolls
            // inside the sheet rather than pushing the sheet past the screen.
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}

/** How long a description has to be before reading it inline stops being reasonable. */
const val LONG_TEXT_THRESHOLD = 180

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
