package com.pedroeu.ficha.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.OverridableStat
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.NumberStepper
import com.pedroeu.ficha.ui.components.SectionHeader
import com.pedroeu.ficha.ui.components.StatEditDialog

@Composable
fun SpellsTab(character: PlayerCharacter, viewModel: SheetViewModel, editMode: Boolean) {
    val ability = CharacterCalculations.spellcastingAbility(character)
    val slots = CharacterCalculations.spellSlots(character)
    var editingStat by remember { mutableStateOf<OverridableStat?>(null) }

    if (ability == null && character.knownSpells.isEmpty() && !editMode) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "This character has no spellcasting.",
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
        if (ability != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        SectionHeader("Spellcasting")
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            SpellStat("Ability", ability.abbreviation)
                            SpellStat(
                                label = "Save DC",
                                value = "${CharacterCalculations.spellSaveDc(character) ?: 0}",
                                editMode = editMode,
                                onClick = { editingStat = OverridableStat.SPELL_SAVE_DC },
                            )
                            SpellStat(
                                label = "Attack",
                                value = CharacterCalculations.formatModifier(
                                    CharacterCalculations.spellAttackBonus(character) ?: 0
                                ),
                                editMode = editMode,
                                onClick = { editingStat = OverridableStat.SPELL_ATTACK_BONUS },
                            )
                        }
                    }
                }
            }

            item {
                PreparedCountCard(character, editMode) { editingStat = it }
            }
        }

        if (slots.isNotEmpty() || editMode) {
            item {
                SpellSlotsCard(character, viewModel, editMode, slots)
            }
        }

        val cantrips = character.knownSpells.filter { it.level == 0 }
        val leveled = character.knownSpells.filter { it.level > 0 }

        if (cantrips.isNotEmpty()) {
            item { SpellSection("Cantrips", cantrips, viewModel, editMode) }
        }
        if (leveled.isNotEmpty()) {
            item { SpellSection("Prepared & Known Spells", leveled, viewModel, editMode) }
        }

        if (character.knownSpells.isEmpty()) {
            item {
                Text(
                    text = "No spells recorded yet. Level up to learn some, or add them here in Edit Mode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    editingStat?.let { stat ->
        val stripped = character.copy(statOverrides = emptyMap(), statBonuses = emptyMap())
        val rulesValue = when (stat) {
            OverridableStat.SPELL_SAVE_DC -> CharacterCalculations.spellSaveDc(stripped) ?: 0
            OverridableStat.SPELL_ATTACK_BONUS -> CharacterCalculations.spellAttackBonus(stripped) ?: 0
            OverridableStat.MAX_PREPARED_SPELLS -> CharacterCalculations.maxPreparedSpells(stripped)
            OverridableStat.CANTRIPS_KNOWN -> CharacterCalculations.maxCantripsKnown(stripped)
            else -> 0
        }
        StatEditDialog(
            title = stat.label,
            rulesValue = rulesValue,
            currentBonus = character.statBonuses[stat.name],
            currentOverride = character.statOverrides[stat.name],
            onDismiss = { editingStat = null },
            onConfirm = { bonus, override ->
                viewModel.setStatBonus(stat, bonus)
                viewModel.setStatOverride(stat, override)
                editingStat = null
            },
        )
    }
}

@Composable
private fun PreparedCountCard(
    character: PlayerCharacter,
    editMode: Boolean,
    onEdit: (OverridableStat) -> Unit,
) {
    val prepared = character.knownSpells.count { it.level > 0 && it.prepared }
    val maxPrepared = CharacterCalculations.maxPreparedSpells(character)
    val cantrips = character.knownSpells.count { it.level == 0 }
    val maxCantrips = CharacterCalculations.maxCantripsKnown(character)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Capacity")
            CountRow(
                label = "Prepared spells",
                value = "$prepared / $maxPrepared",
                overBudget = prepared > maxPrepared,
                editMode = editMode,
                onClick = { onEdit(OverridableStat.MAX_PREPARED_SPELLS) },
            )
            CountRow(
                label = "Cantrips known",
                value = "$cantrips / $maxCantrips",
                overBudget = cantrips > maxCantrips,
                editMode = editMode,
                onClick = { onEdit(OverridableStat.CANTRIPS_KNOWN) },
            )
        }
    }
}

@Composable
private fun CountRow(
    label: String,
    value: String,
    overBudget: Boolean,
    editMode: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (editMode) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (overBudget) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun SpellSlotsCard(
    character: PlayerCharacter,
    viewModel: SheetViewModel,
    editMode: Boolean,
    slots: Map<Int, Int>,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(
                "Spell Slots",
                trailing = if (character.spellSlotOverrides.isNotEmpty()) "adjusted" else null,
            )

            if (editMode) {
                // In Edit Mode every level 1-9 is listed so slots can be granted outright.
                (1..9).forEach { level ->
                    NumberStepper(
                        label = "Level $level",
                        value = slots[level] ?: 0,
                        onChange = { viewModel.setSpellSlotTotal(level, it) },
                        min = 0,
                        max = 9,
                    )
                }
                if (character.spellSlotOverrides.isNotEmpty()) {
                    TextButton(onClick = viewModel::clearSpellSlotOverrides) {
                        Text("Reset to the class table")
                    }
                }
            } else {
                slots.toSortedMap().forEach { (level, total) ->
                    val expended = character.spellSlotsExpended[level.toString()] ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Level $level",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (1..total).forEach { index ->
                                val used = index <= expended
                                Box(
                                    Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (used) MaterialTheme.colorScheme.secondary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            viewModel.setSpellSlotsExpended(
                                                level,
                                                if (expended == index) index - 1 else index,
                                            )
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpellStat(
    label: String,
    value: String,
    editMode: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (editMode && onClick != null) {
            Modifier.clickable { onClick() }
        } else {
            Modifier
        },
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun SpellSection(
    title: String,
    spells: List<KnownSpell>,
    viewModel: SheetViewModel,
    editMode: Boolean,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(title, trailing = "${spells.size}")
            spells.forEach { spell ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = spell.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (spell.level > 0) {
                            // Preparation is a per-day decision, so it stays tappable always.
                            Text(
                                text = if (spell.prepared) "Prepared" else "Not prepared",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (spell.prepared) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { viewModel.toggleSpellPrepared(spell.id) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        Text(
                            text = if (spell.level == 0) "Cantrip" else "Level ${spell.level}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        if (editMode) {
                            IconButton(onClick = { viewModel.removeSpell(spell.id) }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove ${spell.name}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (spell.school.isNotBlank() || spell.source.isNotBlank()) {
                        Text(
                            text = listOf(spell.school, spell.source)
                                .filter { it.isNotBlank() }
                                .joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Text(
                        text = spell.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
