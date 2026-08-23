package com.pedroeu.ficha.ui.sheet

import com.pedroeu.ficha.ui.i18n.trf
import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.domain.CharacterAttacks
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CustomAttack
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.EditableText
import com.pedroeu.ficha.ui.components.ExpandableOption
import com.pedroeu.ficha.ui.components.SectionHeader

@Composable
fun CombatTab(character: PlayerCharacter, viewModel: SheetViewModel, editMode: Boolean) {
    // Weapons carried, plus everything else the character can attack with: an Unarmed
    // Strike, a feature's conjured weapon, and any damage cantrip they know.
    val attacks = CharacterAttacks.all(character)
    val charClass = ClassData.byId(character.classId)

    var editingAttack by remember { mutableStateOf<CustomAttack?>(null) }
    var addingAttack by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            CombatCard {
                SectionHeader(tr("Weapons & Damage Cantrips"))
                if (attacks.isEmpty()) {
                    Text(
                        text = tr("Nothing to attack with yet. Add a weapon on the Inventory tab."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                    ) {
                        HeaderCell(tr("Name"), Modifier.weight(2f))
                        HeaderCell(tr("Atk"), Modifier.weight(1f))
                        HeaderCell(tr("Damage"), Modifier.weight(2f))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    attacks.forEach { attack ->
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = attack.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(2f),
                                )
                                Text(
                                    text = CharacterCalculations.formatModifier(attack.attackBonus),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "${attack.damage} ${attack.damageType}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(2f),
                                )
                            }
                            if (attack.notes.isNotBlank()) {
                                Text(
                                    text = attack.notes,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            // A mastery the character can actually use opens to its full
                            // rules text — knowing a weapon has Topple is no use without
                            // knowing what Topple does at the moment you hit.
                            if (attack.masteryProperty.isNotBlank()) {
                                ExpandableOption(
                                    name = trf("Mastery: {0}", attack.masteryProperty),
                                    description = attack.masteryDescription,
                                )
                            }
                            // Properties that need a rule to be usable — Burst Fire and
                            // Reload above all, which firearms brought in.
                            attack.explainedProperties.forEach { (name, rule) ->
                                ExpandableOption(name = name, description = rule)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        item {
            CombatCard {
                SectionHeader(
                    tr("Other Attacks & Actions"),
                    trailing = character.customAttacks.size.takeIf { it > 0 }?.toString(),
                )
                if (character.customAttacks.isEmpty()) {
                    Text(
                        text = tr("Nothing here yet. Add a spell attack, a breath weapon, or " +
                            "anything else you want on the sheet."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    character.customAttacks.forEach { attack ->
                        CustomAttackRow(
                            character = character,
                            attack = attack,
                            onOpen = { editingAttack = attack },
                            onDelete = { viewModel.removeCustomAttack(attack.id) },
                        )
                    }
                }
                TextButton(onClick = { addingAttack = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(tr("  Add attack"))
                }
            }
        }

        item {
            CombatCard {
                SectionHeader(tr("Equipment Training & Proficiencies"))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    EditableLine(
                        label = tr("Armor Training"),
                        key = "combat:armor_training",
                        fallback = character.armorTraining
                            .ifEmpty { charClass?.armorProficiencies.orEmpty() }
                            .takeIf { it.isNotEmpty() }?.joinToString() ?: tr("None"),
                        character = character,
                        viewModel = viewModel,
                        editMode = editMode,
                    )
                    EditableLine(
                        label = tr("Weapons"),
                        key = "combat:weapons",
                        fallback = character.weaponProficiencies
                            .ifEmpty { charClass?.weaponProficiencies.orEmpty() }
                            .takeIf { it.isNotEmpty() }?.joinToString() ?: tr("None"),
                        character = character,
                        viewModel = viewModel,
                        editMode = editMode,
                    )
                    EditableLine(
                        label = tr("Tools"),
                        key = "combat:tools",
                        fallback = character.toolProficiencies.takeIf { it.isNotEmpty() }
                            ?.joinToString() ?: tr("None"),
                        character = character,
                        viewModel = viewModel,
                        editMode = editMode,
                    )
                    EditableLine(
                        label = tr("Languages"),
                        key = "combat:languages",
                        fallback = character.languages.joinToString(),
                        character = character,
                        viewModel = viewModel,
                        editMode = editMode,
                    )
                }
            }
        }

        item {
            CombatCard {
                SectionHeader(tr("Defenses"))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    ProficiencyLine(tr("Armor Class"), "${CharacterCalculations.armorClass(character)}")
                    ProficiencyLine(
                        tr("Initiative"),
                        CharacterCalculations.formatModifier(
                            CharacterCalculations.initiative(character)
                        ),
                    )
                    ProficiencyLine(tr("Speed"), "${CharacterCalculations.speed(character)} ft")
                    val equipped = character.inventory.filter { it.equipped && it.armorDefId != null }
                    ProficiencyLine(
                        tr("Equipped Armor"),
                        equipped.takeIf { it.isNotEmpty() }?.joinToString { it.name } ?: tr("None"),
                    )
                    EditableLine(
                        label = tr("Resistances"),
                        key = "combat:resistances",
                        fallback = tr("None recorded"),
                        character = character,
                        viewModel = viewModel,
                        editMode = editMode,
                    )
                    EditableLine(
                        label = tr("Conditions"),
                        key = "combat:conditions",
                        fallback = tr("None"),
                        character = character,
                        viewModel = viewModel,
                        editMode = editMode,
                    )
                }
            }
        }
    }

    if (addingAttack) {
        AttackEditorSheet(
            character = character,
            existing = null,
            onDismiss = { addingAttack = false },
            onSave = {
                viewModel.addCustomAttack(it)
                addingAttack = false
            },
        )
    }

    editingAttack?.let { attack ->
        AttackEditorSheet(
            character = character,
            existing = attack,
            onDismiss = { editingAttack = null },
            onSave = {
                viewModel.updateCustomAttack(it)
                editingAttack = null
            },
        )
    }
}

@Composable
private fun CustomAttackRow(
    character: PlayerCharacter,
    attack: CustomAttack,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    // Worked out from the ability, proficiency and magic bonus unless the player overrode it.
    val (toHit, damage) = CharacterAttacks.customAttackNumbers(character, attack)
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = attack.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (toHit.isNotBlank()) {
                Text(
                    text = toHit,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove ${attack.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val line = listOf(damage, attack.damageType, attack.range)
            .filter { it.isNotBlank() }
            .joinToString(" • ")
        if (line.isNotBlank()) {
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (attack.notes.isNotBlank()) {
            Text(
                text = attack.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun CombatCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) { content() }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun ProficiencyLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.width(130.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

/** A proficiency line whose text the player can rewrite in Edit Mode. */
@Composable
private fun EditableLine(
    label: String,
    key: String,
    fallback: String,
    character: PlayerCharacter,
    viewModel: SheetViewModel,
    editMode: Boolean,
) {
    val override = character.textOverrides[key]
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.width(130.dp),
        )
        EditableText(
            value = override ?: fallback,
            editMode = editMode,
            onChange = { viewModel.setText(key, it) },
            label = label,
            isOverridden = override != null,
            multiline = true,
            modifier = Modifier.weight(1f),
        )
    }
}
