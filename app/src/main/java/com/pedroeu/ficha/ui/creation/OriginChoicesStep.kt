package com.pedroeu.ficha.ui.creation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.domain.OwnedOptions
import com.pedroeu.ficha.ui.components.ChoiceSection

/**
 * Collects every remaining "of your choice" grant from the species, class, background, and
 * origin feat, so nothing gets silently auto-assigned. A Sage's Magic Initiate spells land here.
 */
@Composable
fun OriginChoicesStep(state: CreationState, viewModel: CreationViewModel) {
    val choices = state.originChoices

    if (choices.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Your species, class, and origin didn't leave any further choices open. " +
                    "Continue to your ability scores.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp),
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "These come from your species, class, and origin. Each one is yours to pick.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        items(choices.size, key = { choices[it].id }) { index ->
            val choice = choices[index]
            // Anything the character already has — a skill, a tool, a language, a spell —
            // is greyed out here, whichever earlier step granted it.
            val disabled = OwnedOptions.disabledFor(
                choice = choice,
                owned = state.owned,
                currentSelection = state.originSelections[choice.id].orEmpty().toSet(),
            )

            ChoiceSection(
                choice = choice,
                selected = state.originSelections[choice.id].orEmpty(),
                onToggle = { optionId ->
                    viewModel.toggleOriginChoice(choice.id, optionId, choice.count)
                },
                disabledOptionIds = disabled,
            )
        }
    }
}
