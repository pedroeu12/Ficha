package com.pedroeu.ficha.ui.components

import com.pedroeu.ficha.ui.design.Corner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.foundation.clickable
import com.pedroeu.ficha.data.model.ChoiceOption
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind

/**
 * Renders one [Choice] as a titled card. Short-labelled kinds become chips; anything with
 * real explanatory text becomes a list of selectable cards so the player can read before
 * committing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceSection(
    choice: Choice,
    selected: List<String>,
    onToggle: (optionId: String) -> Unit,
    modifier: Modifier = Modifier,
    /** Options that are unavailable, e.g. a skill the character already has. */
    disabledOptionIds: Set<String> = emptySet(),
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Corner.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            SectionHeader(
                title = choice.label,
                trailing = "${selected.size} / ${choice.count}",
            )
            if (choice.source.isNotBlank()) {
                Text(
                    text = choice.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (choice.prompt.isNotBlank()) {
                Text(
                    text = choice.prompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                )
            }

            if (choice.kind.usesChips()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    choice.options.forEach { option ->
                        ChoiceChip(
                            label = option.name,
                            supporting = option.supporting.ifBlank { null },
                            selected = selected.contains(option.id),
                            enabled = option.id !in disabledOptionIds,
                            onClick = { onToggle(option.id) },
                        )
                    }
                }
            } else {
                var query by rememberSaveable(choice.id) { mutableStateOf("") }
                var collapsed by rememberSaveable(choice.id) { mutableStateOf(setOf<String>()) }

                val matching = SourceGrouping.matching(choice.options, query) {
                    it.name + " " + it.supporting
                }
                val grouped = SourceGrouping.worthGrouping(choice.options) { it.book }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    if (choice.options.size >= SourceGrouping.SEARCH_THRESHOLD) {
                        PickerSearchField(query = query, onQueryChange = { query = it })
                    }

                    @Composable
                    fun option(option: ChoiceOption) {
                        var showFullText by rememberSaveable(option.id) { mutableStateOf(false) }
                        val available = option.id !in disabledOptionIds
                        SelectableCard(
                            title = option.name,
                            subtitle = option.description,
                            selected = selected.contains(option.id),
                            onClick = { onToggle(option.id) },
                            trailingLabel = option.supporting.ifBlank { null },
                            // The card branch used to ignore this, so every list that isn't
                            // chips — invocations, Metamagic, maneuvers, feats, spells — let
                            // you pick something you already had or didn't qualify for.
                            enabled = available,
                            // Options carry the book's full rules text; clamp it so a list of
                            // nineteen maneuvers is still something you can scroll.
                            subtitleMaxLines = 3,
                        )
                        // What the rules ask of it, printed whether or not it is met: an
                        // option greyed out with no reason given reads as a bug.
                        if (option.prerequisite.isNotBlank()) {
                            Text(
                                text = option.prerequisite,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (available) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp),
                            )
                        }
                        // Tapping the card picks the option, so reading the rest of the rules
                        // needs its own target — otherwise the only way to read a long option
                        // is to select it.
                        if (option.description.length > LONG_TEXT_THRESHOLD) {
                            Text(
                                text = tr("Read the full rules"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { showFullText = true }
                                    .padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                            )
                        }
                        if (showFullText) {
                            RulesTextSheet(
                                title = option.name,
                                body = option.description,
                                subtitle = option.supporting,
                                onDismiss = { showFullText = false },
                            )
                        }
                    }

                    when {
                        matching.isEmpty() -> NoSearchResults(query)

                        // A search is already a filter; grouping its results as well buries
                        // the two matches under headers.
                        grouped && query.isBlank() ->
                            SourceGrouping.byBook(matching) { it.book }.forEach { (book, entries) ->
                                val key = book?.id ?: "other"
                                val open = key !in collapsed
                                SourceSectionHeader(
                                    book = book,
                                    count = entries.size,
                                    expanded = open,
                                    onToggle = {
                                        collapsed = if (open) collapsed + key else collapsed - key
                                    },
                                )
                                if (open) entries.forEach { option(it) }
                            }

                        else -> matching.forEach { option(it) }
                    }
                }
            }
        }
    }
}

private fun ChoiceKind.usesChips(): Boolean = when (this) {
    ChoiceKind.SKILL, ChoiceKind.EXPERTISE, ChoiceKind.TOOL,
    ChoiceKind.DAMAGE_TYPE, ChoiceKind.LANGUAGE, ChoiceKind.ABILITY_SCORE,
    -> true

    ChoiceKind.OPTION, ChoiceKind.SPELL, ChoiceKind.FEAT, ChoiceKind.SUBCLASS -> false
}
