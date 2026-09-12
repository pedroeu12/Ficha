package com.pedroeu.ficha.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.pedroeu.ficha.domain.CharacterSummons
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.design.Corner
import com.pedroeu.ficha.ui.design.Space
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.i18n.trf

/**
 * Calling something up: what to summon, which form, and at what level.
 *
 * The three questions are asked only when the rules actually ask them. A spell that names one
 * creature skips the form; a feature that is not cast from a slot skips the level. That falls
 * out of the data rather than being written per spell — a [com.pedroeu.ficha.rules.SummonPick]
 * with one option has nothing to ask about.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SummonPickerSheet(
    character: PlayerCharacter,
    onDismiss: () -> Unit,
    onSummon: (
        statblockId: String,
        sourceId: String,
        sourceLabel: String,
        spellLevel: Int,
        owningClassId: String?,
        concentration: Boolean,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val available = remember(character) { CharacterSummons.available(character) }
    var chosen by remember { mutableStateOf(available.firstOrNull()) }
    var form by remember(chosen) { mutableStateOf(chosen?.options?.firstOrNull()) }
    var level by remember(chosen) { mutableStateOf(chosen?.castableAt?.firstOrNull() ?: 0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.sheetEdge)
                .padding(bottom = Space.sheetBottom),
            verticalArrangement = Arrangement.spacedBy(Space.betweenRows),
        ) {
            Text(
                text = tr("Summon a creature"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            if (available.isEmpty()) {
                Text(
                    text = tr(
                        "Nothing on this sheet summons anything yet. A spell like Find " +
                            "Familiar or Summon Beast, or a feature like the Battle Smith's " +
                            "Steel Defender, will appear here once you have it."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            // A plain Column: a lazy list inside a scrolling sheet is a nested scrollable in
            // the same direction, which is fragile at best, and nobody has more than a
            // handful of summoning spells for laziness to pay for.
            Column(verticalArrangement = Arrangement.spacedBy(Space.tight)) {
                available.forEach { entry ->
                    DetailRow(
                        title = entry.summons.label,
                        supporting = listOfNotNull(
                            entry.summons.duration.takeIf { it.isNotBlank() },
                            if (entry.summons.concentration) tr("Concentration") else null,
                        ).joinToString(" · "),
                        selected = entry === chosen,
                        onClick = { chosen = entry },
                    )
                }
            }

            val current = chosen ?: return@Column

            if (current.needsAChoice) {
                Text(
                    text = tr("Which one?"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                // Wrapping, not a Row. Find Familiar offers nineteen forms once Pact of the
                // Chain widens it, and a Row draws them in one line: the first three fitted
                // the screen and the other sixteen were off the edge and unreachable. It was
                // already wrong with the eleven ordinary forms; the pact only made it obvious.
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.inline),
                    verticalArrangement = Arrangement.spacedBy(Space.tight),
                ) {
                    current.options.forEach { option ->
                        FilterChip(
                            selected = option === form,
                            onClick = { form = option },
                            label = { Text(shortName(option.name)) },
                            shape = Corner.row,
                        )
                    }
                }
            }

            if (current.castableAt.size > 1) {
                Text(
                    text = tr("At what level?"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.inline),
                    verticalArrangement = Arrangement.spacedBy(Space.tight),
                ) {
                    current.castableAt.forEach { slot ->
                        FilterChip(
                            selected = slot == level,
                            onClick = { level = slot },
                            label = { Text(trf("Level {0}", slot)) },
                            shape = Corner.row,
                        )
                    }
                }
            }

            // What the creature will actually be, worked out now rather than after the fact,
            // because a spirit called with a level 7 slot is a different creature from one
            // called with a level 3 slot and the player is choosing between them.
            form?.let { statblock ->
                val hp = statblock.hitPointsFor(character, level, current.owningClassId)
                val ac = statblock.armorClassFor(character, level, current.owningClassId)
                Text(
                    text = trf("{0}: {1} Hit Points, AC {2}", statblock.name, hp, ac),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            Button(
                onClick = {
                    val statblock = form ?: return@Button
                    onSummon(
                        statblock.id,
                        current.summons.summonId,
                        current.summons.label,
                        level,
                        current.owningClassId,
                        current.summons.concentration,
                    )
                },
                enabled = form != null,
                shape = Corner.row,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(tr("Summon")) }
        }
    }
}

/**
 * What to print on a form's chip.
 *
 * The spirits are named "Bestial Spirit (Air)", where only the variant tells them apart; a
 * familiar is named "Owl", where the whole name does. Taking what is in the brackets when
 * there are brackets covers both without a second list to keep in step.
 */
private fun shortName(name: String): String =
    if ('(' in name) name.substringAfter('(').substringBefore(')') else name
