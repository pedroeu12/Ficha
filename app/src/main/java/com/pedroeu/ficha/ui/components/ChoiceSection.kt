package com.pedroeu.ficha.ui.components

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
        shape = RoundedCornerShape(14.dp),
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    choice.options.forEach { option ->
                        SelectableCard(
                            title = option.name,
                            subtitle = option.description,
                            selected = selected.contains(option.id),
                            onClick = { onToggle(option.id) },
                            trailingLabel = option.supporting.ifBlank { null },
                            // Options now carry full rules text; clamp it so a list of
                            // nineteen maneuvers is still something you can scroll.
                            subtitleMaxLines = 3,
                        )
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
