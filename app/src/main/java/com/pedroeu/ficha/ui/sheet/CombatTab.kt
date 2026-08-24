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
import com.pedroeu.ficha.domain.AttackLine
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.layout.Spacer
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.EditableText
import com.pedroeu.ficha.ui.components.ExpandableOption
import com.pedroeu.ficha.ui.components.SectionHeader

@Composable
fun CombatTab(character: PlayerCharacter, viewModel: SheetViewModel, editMode: Boolean) {
    // Weapons carried, plus everything else the character can attack with: an Unarmed
    // Strike, a feature's conjured weapon, and any damage cantrip they know.
    val attacks = CharacterAttacks.all(character)
    // Only used when the character records nothing of its own — a sheet made before level up
    // started writing training down. A class taken later grants the narrower multiclass set,
    // which is already on the character, so this shows the starting class's list alone rather
    // than inventing training out of another class's full one.
    val starting = ClassLevels.startingClass(character)?.classId ?: character.classId
    val classArmor = ClassData.byId(starting)?.armorProficiencies.orEmpty()
    val classWeapons = ClassData.byId(starting)?.weaponProficiencies.orEmpty()

    var editingAttack by remember { mutableStateOf<CustomAttack?>(null) }
    var addingAttack by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            CombatCard {
                SectionHeader(
                    tr("Attacks"),
                    trailing = attacks.size.takeIf { it > 0 }?.toString(),
                )
                if (attacks.isEmpty()) {
                    Text(
                        text = tr("Nothing to attack with yet. Add a weapon on the Inventory " +
                            "tab, or write an attack here."),
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
                    attacks.forEachIndexed { index, attack ->
                        AttackRow(
                            character = character,
                            viewModel = viewModel,
                            attack = attack,
                            editMode = editMode,
                            isFirst = index == 0,
                            isLast = index == attacks.lastIndex,
                            onOpen = {
                                character.customAttacks
                                    .find { it.id == attack.id }
                                    ?.let { editingAttack = it }
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                // Hidden lines are only listed in Edit Mode: they are off the sheet on
                // purpose, and one hidden by mistake still has to be findable.
                val hidden = character.hiddenAttackIds
                if (editMode && hidden.isNotEmpty()) {
                    Text(
                        text = tr("Hidden"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    hidden.sorted().forEach { id ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = character.textOverrides["attack:$id:name"] ?: id,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { viewModel.showAttack(id) }) {
                                Text(tr("Put back"))
                            }
                        }
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
                            .ifEmpty { classArmor }
                            .takeIf { it.isNotEmpty() }?.joinToString() ?: tr("None"),
                        character = character,
                        viewModel = viewModel,
                        editMode = editMode,
                    )
                    EditableLine(
                        label = tr("Weapons"),
                        key = "combat:weapons",
                        fallback = character.weaponProficiencies
                            .ifEmpty { classWeapons }
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
private fun AttackRow(
    character: PlayerCharacter,
    viewModel: SheetViewModel,
    attack: AttackLine,
    editMode: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onOpen: () -> Unit,
) {
    /** Every field is a text override keyed on the line's id, so all of them are editable. */
    @Composable
    fun field(
        which: String,
        value: String,
        modifier: Modifier = Modifier,
        style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
        color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight: FontWeight? = null,
    ) {
        val key = "attack:${attack.id}:$which"
        EditableText(
            value = value,
            editMode = editMode,
            onChange = { viewModel.setText(key, it) },
            label = which,
            style = style,
            color = color,
            fontWeight = fontWeight,
            isOverridden = character.textOverrides.containsKey(key),
            modifier = modifier,
        )
    }

    Column(
        Modifier
            .fillMaxWidth()
            .then(if (attack.isCustom && !editMode) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            field(
                "name", attack.name, Modifier.weight(2f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            field(
                "bonus", attack.shownBonus, Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
            )
            field("damage", attack.damage, Modifier.weight(1.2f))
            field("damageType", attack.damageType, Modifier.weight(0.8f))
        }

        field("notes", attack.notes, Modifier.fillMaxWidth(), MaterialTheme.typography.labelSmall)

        // Reordering and removal are Edit Mode work: they change the sheet's shape rather
        // than what happens at the table, and a stray tap should not move an attack.
        if (editMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.moveAttack(attack.id, up = true) }, enabled = !isFirst) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move ${attack.name} up",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { viewModel.moveAttack(attack.id, up = false) }, enabled = !isLast) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move ${attack.name} down",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (attack.isCustom) {
                    TextButton(onClick = onOpen) { Text(tr("Edit")) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { viewModel.hideAttack(attack.id) }) {
                    // A derived line comes back on the next read, so "remove" would be a lie.
                    Text(if (attack.isCustom) tr("Delete") else tr("Take off the sheet"))
                }
            }
        }

        if (attack.masteryProperty.isNotBlank()) {
            ExpandableOption(
                name = trf("Mastery: {0}", attack.masteryProperty),
                description = attack.masteryDescription,
            )
        }
        attack.explainedProperties.forEach { (name, rule) ->
            ExpandableOption(name = name, description = rule)
        }
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
