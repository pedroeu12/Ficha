package com.pedroeu.ficha.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
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
@OptIn(ExperimentalMaterial3Api::class)
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

            LazyColumn(
                Modifier.heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(Space.tight),
            ) {
                items(available.size) { index ->
                    val entry = available[index]
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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.inline),
                ) {
                    current.options.forEach { option ->
                        FilterChip(
                            selected = option === form,
                            onClick = { form = option },
                            label = {
                                Text(
                                    option.name
                                        .substringAfter('(')
                                        .substringBefore(')')
                                        .ifBlank { option.name }
                                )
                            },
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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.inline),
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
