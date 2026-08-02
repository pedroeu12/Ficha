package com.pedroeu.ficha.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.pedroeu.ficha.domain.ActiveChoice
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.PerUseChoices
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.ChoiceChip
import com.pedroeu.ficha.ui.components.ExpandableOption
import com.pedroeu.ficha.ui.components.SectionHeader
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.i18n.trf

/**
 * A decision the rules have you make at the moment you use a feature.
 *
 * It reads as "what is happening now" rather than as a settled fact about the character: the
 * chips are always live, picking a different one costs nothing, and a rest clears it. That is
 * the whole difference between this and the choices on the rest of the sheet, and the reason
 * these were wrong when they were asked once during character creation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerUseChoiceRow(
    active: ActiveChoice,
    onChoose: (String) -> Unit,
    onClear: () -> Unit,
    /** Present when the feature draws on a pool that still has uses left. */
    onUse: ((String) -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tr(active.choice.label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            if (active.isSet) {
                TextButton(onClick = onClear) { Text(tr("Clear")) }
            }
        }

        Text(
            text = tr(active.choice.prompt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            active.choice.options.forEach { option ->
                ChoiceChip(
                    label = option.name,
                    selected = active.selected?.id == option.id,
                    supporting = option.supporting.takeIf { it.isNotBlank() },
                    onClick = { onChoose(option.id) },
                )
            }
        }

        // Only the option in force opens to its rules text. Showing all of them at once turns
        // a decision you make mid-combat into a wall of prose.
        active.selected?.let { option ->
            ExpandableOption(
                name = option.name,
                description = option.description,
                subtitle = option.supporting,
            )
            if (onUse != null) {
                TextButton(onClick = { onUse(option.id) }) {
                    Text(trf("Use one — {0}", option.name))
                }
            }
        }
    }
}

/**
 * Every per-use decision the character has, gathered beside their features.
 *
 * The ones drawing on a pool also appear on that pool's tracker, which is the same state seen
 * from the other side — you can set the cannon's mode from either place. The ones that draw on
 * nothing at all, like a Hexblade's Maneuvers, have no tracker to appear on, so without this
 * card they would have nowhere to live.
 */
@Composable
fun PerUseChoicesCard(character: PlayerCharacter, viewModel: SheetViewModel) {
    val choices = PerUseChoices.all(character)
    if (choices.isEmpty()) return

    val remaining = CharacterResources.states(character).associate { it.def.id to it.remaining }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(tr("Chosen As You Use Them"))
            Text(
                text = tr("These aren't settled once. Pick whichever you want each time the " +
                    "feature comes up; resting clears whatever is showing."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            choices.forEach { active ->
                val pool = active.choice.resourceId
                PerUseChoiceRow(
                    active = active,
                    onChoose = { viewModel.setPerUseChoice(active.choice.id, it) },
                    onClear = { viewModel.clearPerUseChoice(active.choice.id) },
                    // Spending is offered only where there is something to spend, and only
                    // while the pool still has a use left in it.
                    onUse = if (pool.isNotBlank() && (remaining[pool] ?: 0) > 0) {
                        { optionId -> viewModel.spendResourceOn(pool, active.choice.id, optionId) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
