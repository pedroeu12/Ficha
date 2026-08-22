package com.pedroeu.ficha.ui.sheet

import com.pedroeu.ficha.ui.i18n.trf
import com.pedroeu.ficha.ui.i18n.tr
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.ui.components.firstSentenceOf
import com.pedroeu.ficha.ui.components.RulesTextSheet
import com.pedroeu.ficha.ui.components.LONG_TEXT_THRESHOLD
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
import com.pedroeu.ficha.domain.CharacterDcs
import com.pedroeu.ficha.domain.CharacterSpells
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
    var addingSpell by remember { mutableStateOf(false) }
    var openSpell by remember { mutableStateOf<KnownSpell?>(null) }


    // Spells the rules grant outright are derived rather than stored, so they appear the
    // moment a character gains the class, subclass, species, or feat that supplies them.
    val spells = CharacterSpells.all(character)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Button(
                onClick = { addingSpell = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(tr("  Add a spell"), style = MaterialTheme.typography.labelLarge)
            }
        }

        if (ability == null && spells.isEmpty()) {
            item {
                Text(
                    text = tr("This character has no spellcasting from their class. You can still " +
                        "add spells from feats, items, or anywhere else."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }

        if (ability != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        SectionHeader(tr("Spellcasting"))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            SpellStat(tr("Ability"), ability.abbreviation)
                            SpellStat(
                                label = tr("Save DC"),
                                value = "${CharacterCalculations.spellSaveDc(character) ?: 0}",
                                editMode = editMode,
                                onClick = { editingStat = OverridableStat.SPELL_SAVE_DC },
                            )
                            SpellStat(
                                label = tr("Attack"),
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

        // A character can impose several different save DCs at once — a Monk's Wisdom DC and
        // a Magic Initiate cantrip's Intelligence DC are both live and neither replaces the
        // other — so each source is listed with its own number.
        val saveDcs = CharacterDcs.all(character)
        if (saveDcs.size > 1 || (ability == null && saveDcs.isNotEmpty())) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SectionHeader(tr("Save DCs by source"), trailing = "${saveDcs.size}")
                        saveDcs.forEach { dc ->
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = dc.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = trf("DC {0}", dc.dc),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                                Text(
                                    text = "${dc.ability.fullName} • Attack " +
                                        CharacterCalculations.formatModifier(dc.attackBonus),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                Text(
                                    text = dc.note,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (slots.isNotEmpty() || editMode) {
            item {
                SpellSlotsCard(character, viewModel, editMode, slots)
            }
        }

        val cantrips = spells.filter { it.level == 0 }
        val leveled = spells.filter { it.level > 0 }

        if (cantrips.isNotEmpty()) {
            item {
                SpellSection(tr("Cantrips"), cantrips, character, viewModel, editMode) {
                    openSpell = it
                }
            }
        }
        if (leveled.isNotEmpty()) {
            item {
                SpellSection(
                    tr("Prepared & Known Spells"), leveled, character, viewModel, editMode,
                ) { openSpell = it }
            }
        }

        if (spells.isEmpty()) {
            item {
                Text(
                    text = tr("No spells recorded yet. Level up to learn some, or add them here in Edit Mode."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    openSpell?.let { spell ->
        SpellDetailSheet(spell = spell, onDismiss = { openSpell = null })
    }

    if (addingSpell) {
        SpellPickerSheet(
            character = character,
            onDismiss = { addingSpell = false },
            onAdd = { spell ->
                viewModel.addSpell(spell)
                addingSpell = false
            },
        )
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
    // Spells the rules always have prepared for you don't count against the limit, so the
    // capacity row counts only what the player actually chose to prepare.
    val granted = CharacterSpells.granted(character).map { it.spell.id }.toSet()
    val onSheet = CharacterSpells.all(character)
    val prepared = onSheet.count { it.level > 0 && it.prepared && it.id !in granted }
    val maxPrepared = CharacterCalculations.maxPreparedSpells(character)
    val cantrips = onSheet.count { it.level == 0 && it.id !in granted }
    val maxCantrips = CharacterCalculations.maxCantripsKnown(character)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(tr("Capacity"))
            CountRow(
                label = tr("Prepared spells"),
                value = "$prepared / $maxPrepared",
                overBudget = prepared > maxPrepared,
                editMode = editMode,
                onClick = { onEdit(OverridableStat.MAX_PREPARED_SPELLS) },
            )
            CountRow(
                label = tr("Cantrips known"),
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
                tr("Spell Slots"),
                trailing = if (character.spellSlotOverrides.isNotEmpty()) "adjusted" else null,
            )

            if (editMode) {
                // In Edit Mode every level 1-9 is listed so slots can be granted outright.
                (1..9).forEach { level ->
                    NumberStepper(
                        label = trf("Level {0}", level),
                        value = slots[level] ?: 0,
                        onChange = { viewModel.setSpellSlotTotal(level, it) },
                        min = 0,
                        max = 9,
                    )
                }
                if (character.spellSlotOverrides.isNotEmpty()) {
                    TextButton(onClick = viewModel::clearSpellSlotOverrides) {
                        Text(tr("Reset to the class table"))
                    }
                }
            } else {
                slots.toSortedMap().forEach { (level, total) ->
                    val expended = character.spellSlotsExpended[level.toString()] ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = trf("Level {0}", level),
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
    character: PlayerCharacter,
    viewModel: SheetViewModel,
    editMode: Boolean,
    onOpenSpell: (KnownSpell) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(title, trailing = "${spells.size}")
            spells.forEach { spell ->
                val isGranted = CharacterSpells.isGranted(character, spell.id)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = spell.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenSpell(spell) },
                        )
                        if (spell.level > 0) {
                            // A granted spell is always prepared, so there's nothing to toggle.
                            Text(
                                text = when {
                                    isGranted -> tr("Always prepared")
                                    spell.prepared -> tr("Prepared")
                                    else -> tr("Not prepared")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (spell.prepared) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .then(
                                        if (isGranted) Modifier
                                        else Modifier.clickable {
                                            viewModel.toggleSpellPrepared(spell.id)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        Text(
                            text = if (spell.level == 0) tr("Cantrip") else "Level ${spell.level}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        // Removing a granted spell would be undone on the next redraw, so
                        // the delete button only appears for spells the player added.
                        if (editMode && !isGranted) {
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
                    // The catalog carries the book's own wording, which runs to paragraphs.
                    // A spell list is a list, so long text collapses to its first line and
                    // opens in full on a tap.
                    if (spell.description.length > LONG_TEXT_THRESHOLD) {
                        var showFullText by remember(spell.id) { mutableStateOf(false) }
                        Column(Modifier.clickable { showFullText = true }) {
                            Text(
                                text = firstSentenceOf(spell.description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = tr("Read the full rules"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (showFullText) {
                            val catalogue = SpellData.byId(spell.id)
                            RulesTextSheet(
                                title = spell.name,
                                body = spell.description,
                                subtitle = listOf(
                                    if (spell.level == 0) tr("Cantrip") else "Level ${spell.level}",
                                    spell.school,
                                ).filter { it.isNotBlank() }.joinToString(" • "),
                                facts = listOfNotNull(
                                    catalogue?.let { tr("Casting Time") to it.castingTime },
                                    catalogue?.let { tr("Range") to it.range },
                                    catalogue?.let { tr("Components") to it.components },
                                    catalogue?.let { tr("Duration") to it.duration },
                                ),
                                onDismiss = { showFullText = false },
                            )
                        }
                    } else {
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
}
