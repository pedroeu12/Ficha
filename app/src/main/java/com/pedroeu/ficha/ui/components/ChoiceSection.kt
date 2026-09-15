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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.domain.CustomOption
import com.pedroeu.ficha.domain.CustomOptions

/**
 * Renders one [Choice] as a titled card. Short-labelled kinds become chips; anything with
 * real explanatory text becomes a list of selectable cards so the player can read before
 * committing.
 *
 * Every question in the app is drawn by this one composable, which is why "let me write my
 * own" is a parameter here rather than a feature of any screen: pass [onWriteOwn] and the
 * question gains a way to answer it with something the books do not contain — during
 * creation, at a level up, from Edit Mode, on the phone and on the tablet, all at once.
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
    /** What the player has already written for this question, so it can be edited again. */
    written: List<CustomOption> = emptyList(),
    /** Given, the question offers to be answered with something written by hand. */
    onWriteOwn: ((CustomOption) -> Unit)? = null,
    /** Given, a written option can be taken back — a rewrite reset, an addition deleted. */
    onEraseOwn: ((CustomOption) -> Unit)? = null,
) {
    // Which option's wording is being written, if any. The id alone: the dialog re-reads the
    // option from the list on every pass, so saving one and opening the next cannot show the
    // previous one's text.
    var writing by remember { mutableStateOf<String?>(null) }
    val mine = written.filter { it.choiceId == choice.id }.associateBy { it.id }
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
                        // Writing over an option the books already have. The character still
                        // takes that option, with its level requirement and everything it
                        // depends on; only what it says changes. A table running a homebrew
                        // invocation in the slot of a printed one used to have no way to say
                        // so, and the sheet quietly described a character nobody was playing.
                        if (onWriteOwn != null) {
                            Text(
                                text = if (option.id in mine) tr("Edit your wording")
                                else tr("Rewrite this"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .clickable { writing = option.id }
                                    .padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
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

            if (onWriteOwn != null) {
                WriteYourOwn(
                    written = mine.values.filterNot { it.isRewrite },
                    onNew = { writing = NEW },
                    onEdit = { writing = it.id },
                )
            }
        }
    }

    writing?.let { id ->
        val book = choice.options.find { it.id == id }
        CustomOptionDialog(
            choiceId = choice.id,
            existing = mine[id],
            // A rewrite starts from the book's own wording, so the player edits rather than
            // retypes; a new option starts blank.
            startingName = book?.name.orEmpty(),
            startingDescription = book?.description.orEmpty(),
            onDismiss = { writing = null },
            onSave = {
                onWriteOwn?.invoke(it)
                writing = null
            },
            onErase = onEraseOwn?.let { erase ->
                { option: CustomOption ->
                    erase(option)
                    writing = null
                }
            },
        )
    }
}

/** The way in: one button to write something new, and a way back to anything already written. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WriteYourOwn(
    written: List<CustomOption>,
    onNew: () -> Unit,
    onEdit: (CustomOption) -> Unit,
) {
    Column(
        Modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = tr("+ Write your own"),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onNew).padding(vertical = 4.dp),
        )
        if (written.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                written.forEach { option ->
                    Text(
                        text = "✎ ${option.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .clickable { onEdit(option) }
                            .padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

/** The id the editor is opened with for something that does not exist yet. */
private const val NEW = ""

private fun ChoiceKind.usesChips(): Boolean = when (this) {
    ChoiceKind.SKILL, ChoiceKind.EXPERTISE, ChoiceKind.TOOL,
    ChoiceKind.DAMAGE_TYPE, ChoiceKind.LANGUAGE, ChoiceKind.ABILITY_SCORE,
    -> true

    ChoiceKind.OPTION, ChoiceKind.SPELL, ChoiceKind.FEAT, ChoiceKind.SUBCLASS -> false
}

