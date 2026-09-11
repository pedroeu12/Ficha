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
import com.pedroeu.ficha.domain.ActionCost
import com.pedroeu.ficha.domain.ActiveChoice
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.PerUseChoices
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.ResourceState
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.pedroeu.ficha.ui.components.SelectableCard
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
    /**
     * False drops the card around the content, for the tablet sheet — which is one page of
     * paper, and a card floating on it would be the only thing casting a shadow.
     */
    framed: Boolean = true,
) {
    val resources = CharacterResources.states(character)
    var showAdd by remember { mutableStateOf(false) }
    var editingMax by remember { mutableStateOf<ResourceState?>(null) }
    var collapsed by remember { mutableStateOf(emptySet<ActionCost>()) }

    // You get one Action, one Bonus Action and one Reaction a round, so that is the split a
    // player reads this list by. A sheet whose pools all cost the same thing gets no headers:
    // one heading over the whole list says nothing the list didn't already say.
    val sections = ActionCost.ORDER
        .map { cost -> cost to resources.filter { ActionCost.of(it.def) == cost } }
        .filter { (_, rows) -> rows.isNotEmpty() }
    val grouped = sections.size > 1

    MaybeCard(framed) {
        Column(
            if (framed) Modifier.padding(14.dp) else Modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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

            sections.forEach { (cost, rows) ->
                val open = !grouped || cost !in collapsed
                if (grouped) {
                    ActionSectionHeader(
                        cost = cost,
                        count = rows.size,
                        expanded = open,
                        onToggle = {
                            collapsed = if (open) collapsed + cost else collapsed - cost
                        },
                    )
                }
                if (open) {
                    rows.forEach { state ->
                        ResourceRow(
                            state = state,
                            editMode = editMode,
                            // The decisions the rules attach to this ability's use, asked
                            // here rather than once at character creation.
                            perUseChoices = PerUseChoices.forResource(character, state.def.id),
                            onSetSpent = { viewModel.setResourceSpent(state.def.id, it) },
                            onChoose = { choiceId, optionId ->
                                viewModel.setPerUseChoice(choiceId, optionId)
                            },
                            onClearChoice = { viewModel.clearPerUseChoice(it) },
                            onUse = { choiceId, optionId ->
                                viewModel.spendResourceOn(state.def.id, choiceId, optionId)
                            },
                            onEditMax = { editingMax = state },
                            onDelete = { viewModel.removeCustomResource(state.def.id) },
                        )
                    }
                }
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
    perUseChoices: List<ActiveChoice>,
    onSetSpent: (Int) -> Unit,
    onChoose: (String, String) -> Unit,
    onClearChoice: (String) -> Unit,
    onUse: (String, String) -> Unit,
    onEditMax: () -> Unit,
    onDelete: () -> Unit,
) {
    val def = state.def
    var optionsShown by remember(def.id) { mutableStateOf(false) }
    // The rules attach a decision to spending this pool, so spending it has to ask. Set when
    // a pip or the Spend button is tapped, and answered by picking from the list.
    var choosingUse by remember(def.id) { mutableStateOf(false) }

    /**
     * Spends a use. Where the feature asks which effect you are spending it on, the question
     * comes first — tapping a pip used to spend the use silently and leave the choice
     * unanswered beside it, which is the one order the rules never mean.
     */
    fun spend(newSpent: Int) {
        val isSpending = newSpent > state.spent
        if (isSpending && perUseChoices.any { it.choice.options.isNotEmpty() }) {
            choosingUse = true
            return
        }
        onSetSpent(newSpent)
    }

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
                            .clickable { spend(if (state.spent == index) index - 1 else index) },
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = { spend(state.spent + 1) },
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

        // What this use is being spent on. Chosen here because the rules choose here — an
        // Artillerist picks the cannon's mode when the cannon fires, not at level 3.
        perUseChoices.forEach { active ->
            PerUseChoiceRow(
                active = active,
                onChoose = { onChoose(active.choice.id, it) },
                onClear = { onClearChoice(active.choice.id) },
                onUse = if (state.remaining > 0) {
                    { optionId -> onUse(active.choice.id, optionId) }
                } else {
                    null
                },
            )
        }

        if (choosingUse) {
            UseChoiceDialog(
                poolName = def.name,
                choices = perUseChoices,
                onDismiss = { choosingUse = false },
                onPick = { choiceId, optionId ->
                    choosingUse = false
                    onUse(choiceId, optionId)
                },
            )
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

/**
 * The heading over one action's worth of trackers.
 *
 * Tappable because the split is only useful when you can fold away the two thirds of the list
 * that aren't the question you're asking: on your turn you want the Actions and the Bonus
 * Actions, and on someone else's turn you want the one Reaction.
 */
@Composable
private fun ActionSectionHeader(
    cost: ActionCost,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = (if (expanded) "\u25BE  " else "\u25B8  ") + tr(cost.label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * What are you spending this on?
 *
 * Asked when a use is spent from a pool the rules attach a decision to. A list of the actual
 * effects, each with its rules text, rather than a button that spends the use and leaves you
 * to remember what you meant by it.
 */
@Composable
private fun UseChoiceDialog(
    poolName: String,
    choices: List<ActiveChoice>,
    onDismiss: () -> Unit,
    onPick: (choiceId: String, optionId: String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(trf("Use {0}", poolName)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                choices.forEach { active ->
                    if (choices.size > 1) {
                        Text(
                            text = tr(active.choice.label),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Text(
                        text = tr(active.choice.prompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    active.choice.options.forEach { option ->
                        SelectableCard(
                            title = option.name,
                            subtitle = option.description,
                            selected = active.selected?.id == option.id,
                            onClick = { onPick(active.choice.id, option.id) },
                            trailingLabel = option.supporting.ifBlank { null },
                            subtitleMaxLines = 4,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

/**
 * A card, or nothing at all.
 *
 * The phone wants each of these surfaces bounded — it shows one at a time and the boundary is
 * what says where it ends. The tablet sheet is one page of paper, where the same boundary
 * would be the only thing on screen casting a shadow.
 */
@Composable
private fun MaybeCard(framed: Boolean, content: @Composable () -> Unit) {
    if (!framed) {
        content()
        return
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) { content() }
}
