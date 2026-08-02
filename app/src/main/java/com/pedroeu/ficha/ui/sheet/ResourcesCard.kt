package com.pedroeu.ficha.ui.sheet

import com.pedroeu.ficha.ui.i18n.trf
import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.ResourceState
import com.pedroeu.ficha.ui.components.ExpandableOption
import com.pedroeu.ficha.ui.components.SectionHeader
import com.pedroeu.ficha.ui.components.StatEditDialog

/**
 * Trackers for everything with a limited number of uses, from Focus Points and Rage down to a
 * single once-per-day free spell. Small pools show tappable pips; larger ones show a counter.
 */
@Composable
fun ResourcesCard(
    character: PlayerCharacter,
    viewModel: SheetViewModel,
    editMode: Boolean,
) {
    val resources = CharacterResources.states(character)
    var showAdd by remember { mutableStateOf(false) }
    var editingMax by remember { mutableStateOf<ResourceState?>(null) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                tr("Limited Uses"),
                trailing = if (resources.isEmpty()) null else "${resources.size}",
            )

            if (resources.isEmpty()) {
                Text(
                    text = tr("Nothing with a limited number of uses yet. Level up, or add your own tracker."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            resources.forEach { state ->
                ResourceRow(
                    state = state,
                    editMode = editMode,
                    onSetSpent = { viewModel.setResourceSpent(state.def.id, it) },
                    onEditMax = { editingMax = state },
                    onDelete = { viewModel.removeCustomResource(state.def.id) },
                )
            }

            TextButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(tr("  Track something else"))
            }
        }
    }

    if (showAdd) {
        CustomResourceSheet(
            onDismiss = { showAdd = false },
            onSave = { name, max, recharge, notes ->
                viewModel.addCustomResource(name, max, recharge, notes)
                showAdd = false
            },
        )
    }

    editingMax?.let { state ->
        StatEditDialog(
            title = trf("{0} — maximum uses", state.def.name),
            rulesValue = state.def.max,
            currentBonus = null,
            currentOverride = character.resourceMaxOverrides[state.def.id],
            onDismiss = { editingMax = null },
            onConfirm = { _, override ->
                viewModel.setResourceMax(state.def.id, override)
                editingMax = null
            },
            allowNegative = false,
            supportingText = tr("Set a maximum of 0 to hide this tracker entirely."),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResourceRow(
    state: ResourceState,
    editMode: Boolean,
    onSetSpent: (Int) -> Unit,
    onEditMax: () -> Unit,
    onDelete: () -> Unit,
) {
    val def = state.def
    var optionsShown by remember(def.id) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = def.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${def.source} • ${def.recharge.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Text(
                text = "${state.remaining} / ${def.max}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (state.isDepleted) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.secondary,
            )
            if (editMode) {
                IconButton(onClick = onEditMax) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Change ${def.name} maximum",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (def.isCustom) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove ${def.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // A handful of uses reads best as pips; a big pool reads best as plus and minus.
        if (!def.isPointPool && def.max <= 10) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (1..def.max).forEach { index ->
                    val used = index <= state.spent
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (used) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (used) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape,
                            )
                            // Tapping a spent pip gives that use back.
                            .clickable { onSetSpent(if (state.spent == index) index - 1 else index) },
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = { onSetSpent(state.spent + 1) },
                    enabled = state.remaining > 0,
                ) { Text(tr("Spend 1")) }
                TextButton(
                    onClick = { onSetSpent(state.spent - 1) },
                    enabled = state.spent > 0,
                ) { Text(tr("Give back")) }
                if (state.spent > 0) {
                    TextButton(onClick = { onSetSpent(0) }) { Text(tr("Reset")) }
                }
            }
        }

        if (def.notes.isNotBlank()) {
            Text(
                text = def.notes,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Everything this pool pays for, each opening to its own rules text. Without this the
        // sheet would only tell you how many points you have, never what to spend them on.
        val hasDetail = def.options.isNotEmpty() || def.description.isNotBlank()
        if (hasDetail) {
            TextButton(
                onClick = { optionsShown = !optionsShown },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Icon(
                    imageVector = if (optionsShown) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = when {
                        optionsShown -> tr("  Hide details")
                        def.options.isNotEmpty() ->
                            "  What you can spend it on (${def.options.size})"
                        else -> tr("  What this does")
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            if (optionsShown) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 2.dp),
                ) {
                    if (def.description.isNotBlank()) {
                        Text(
                            text = def.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    def.options.forEach { option ->
                        ExpandableOption(
                            name = option.name,
                            description = option.description,
                            subtitle = option.subtitle,
                            trailingLabel = if (option.isChosen) "chosen" else "",
                        )
                    }
                }
            }
        }
    }
}
