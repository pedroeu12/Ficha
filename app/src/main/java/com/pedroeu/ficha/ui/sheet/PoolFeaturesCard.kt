package com.pedroeu.ficha.ui.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.ResolvedChoice
import com.pedroeu.ficha.ui.components.SectionHeader
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.i18n.trf

/**
 * Every "pick some from a pool" feature in one place, addable and removable at will.
 *
 * Invocations, arcane plans, Fighting Styles, weapon masteries, Metamagic, maneuvers — they
 * are all chosen once, buried inside the feature that granted them, and awkward to revisit.
 * The Spells tab solved the same problem long ago with a "+" to add and an "×" to take away,
 * and there is no reason a Warlock should have a harder time swapping an invocation than a
 * spell.
 *
 * Edit Mode only, and deliberately unpoliced: the count the class table allows is shown, and
 * going over it is marked but not prevented. A DM who hands out an extra invocation should
 * not have to argue with the app about it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PoolFeaturesCard(
    character: PlayerCharacter,
    viewModel: SheetViewModel,
    framed: Boolean = true,
) {
    val pools = ChoiceResolver.poolChoices(character)
    if (pools.isEmpty()) return

    var editing by remember { mutableStateOf<ResolvedChoice?>(null) }

    PoolContainer(framed) {
        Column(
            if (framed) Modifier.padding(14.dp) else Modifier,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionHeader(tr("Chosen Features"))
            Text(
                text = tr(
                    "Everything you pick from a list — invocations, plans, styles, masteries. " +
                        "Add or remove any of them freely here."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            pools.forEach { resolved ->
                PoolRow(resolved) { editing = resolved }
            }
        }
    }

    editing?.let { resolved ->
        PoolPickerDialog(
            resolved = resolved,
            onDismiss = { editing = null },
            onToggle = { optionId ->
                val current = resolved.selectedIds
                // Removing always works; adding is never blocked, because the whole point of
                // Edit Mode is that the sheet answers to the table, not the other way round.
                val next = if (optionId in current) current - optionId else current + optionId
                viewModel.setChoiceSelection(resolved.choice.id, resolved.level, next)
            },
        )
    }
}

/** The tablet's paper page hosts these without a card, the way it hosts everything else. */
@Composable
private fun PoolContainer(framed: Boolean, content: @Composable () -> Unit) {
    if (!framed) {
        content()
        return
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PoolRow(resolved: ResolvedChoice, onOpen: () -> Unit) {
    val over = resolved.selectedIds.size > resolved.choice.count

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = resolved.choice.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = trf(
                        "{0} • {1} of {2} chosen",
                        resolved.featureName,
                        resolved.selectedIds.size,
                        resolved.choice.count,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (over) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpen) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = trf("Change {0}", resolved.choice.label),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        if (resolved.selectedNames.isEmpty()) {
            Text(
                text = tr("Nothing chosen yet."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                resolved.selectedNames.forEach { name ->
                    AssistChip(onClick = onOpen, label = { Text(name) })
                }
            }
        }
    }
}

/**
 * The pool itself: everything on offer, with what is held already ticked.
 *
 * The whole list stays visible rather than hiding what is taken, because swapping is the
 * common case and a list that reorders itself under your finger is hard to read.
 */
@Composable
private fun PoolPickerDialog(
    resolved: ResolvedChoice,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(resolved.choice.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = trf(
                        "{0} of {1} chosen. Tap to take one up or set one down.",
                        resolved.selectedIds.size,
                        resolved.choice.count,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.heightIn(max = 380.dp),
                ) {
                    val options = resolved.choice.options
                    items(options.size, key = { options[it].id }) { index ->
                        val option = options[index]
                        val chosen = option.id in resolved.selectedIds
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(option.id) }
                                .padding(vertical = 5.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = option.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (option.supporting.isNotBlank()) {
                                    Text(
                                        text = option.supporting,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (chosen) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = trf("Remove {0}", option.name),
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            } else {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = trf("Add {0}", option.name),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Done")) } },
    )
}
