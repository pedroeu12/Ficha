package com.pedroeu.ficha.ui.sheet

import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.SectionHeader
import com.pedroeu.ficha.ui.components.StatEditDialog

@Composable
fun SkillsTab(character: PlayerCharacter, viewModel: SheetViewModel, editMode: Boolean) {
    var editingSkill by remember { mutableStateOf<Skill?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = if (editMode) {
                    tr("Tap a pip to cycle none, proficient, and expertise. " +
                        "Tap a skill name to add a bonus or override its total.")
                } else {
                    tr("Filled pips mark proficiency; a doubled pip marks expertise.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // The official sheet groups each skill beneath the ability that governs it.
        Ability.ALL.forEach { ability ->
            val skills = Skill.ALL.filter { it.ability == ability }
            if (skills.isEmpty()) return@forEach
            item(key = ability.name) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        SectionHeader(
                            title = ability.fullName,
                            trailing = "Mod ${
                                CharacterCalculations.formatModifier(
                                    CharacterCalculations.abilityModifiers(character)[ability] ?: 0
                                )
                            }",
                        )
                        skills.forEach { skill ->
                            SkillRow(
                                skill = skill,
                                bonus = CharacterCalculations.skillBonus(character, skill),
                                proficient = character.skillProficiencies.contains(skill.name),
                                expert = character.skillExpertise.contains(skill.name),
                                adjusted = character.skillBonuses.containsKey(skill.name) ||
                                    character.skillOverrides.containsKey(skill.name),
                                editMode = editMode,
                                onTogglePip = {
                                    // One tap cycles none → proficient → expertise → none.
                                    when {
                                        character.skillExpertise.contains(skill.name) -> {
                                            viewModel.toggleSkillExpertise(skill)
                                            viewModel.toggleSkillProficiency(skill)
                                        }
                                        character.skillProficiencies.contains(skill.name) ->
                                            viewModel.toggleSkillExpertise(skill)
                                        else -> viewModel.toggleSkillProficiency(skill)
                                    }
                                },
                                onEdit = { editingSkill = skill },
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader(tr("Passive Perception"))
                    Text(
                        text = "${CharacterCalculations.passivePerception(character)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }

        item {
            ToolProficienciesCard(character, viewModel, editMode)
        }
    }

    editingSkill?.let { skill ->
        val stripped = character.copy(skillBonuses = emptyMap(), skillOverrides = emptyMap())
        StatEditDialog(
            title = skill.displayName,
            rulesValue = CharacterCalculations.skillBonus(stripped, skill),
            currentBonus = character.skillBonuses[skill.name],
            currentOverride = character.skillOverrides[skill.name],
            onDismiss = { editingSkill = null },
            onConfirm = { bonus, override ->
                viewModel.setSkillBonus(skill, bonus)
                viewModel.setSkillOverride(skill, override)
                editingSkill = null
            },
            supportingText = tr("Use a bonus for a temporary or story award from your DM."),
        )
    }
}

@Composable
private fun SkillRow(
    skill: Skill,
    bonus: Int,
    proficient: Boolean,
    expert: Boolean,
    adjusted: Boolean,
    editMode: Boolean,
    onTogglePip: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProficiencyPip(
            proficient = proficient,
            expert = expert,
            editMode = editMode,
            onClick = onTogglePip,
        )
        Column(
            Modifier
                .weight(1f)
                .padding(start = 10.dp)
                .then(if (editMode) Modifier.clickable { onEdit() } else Modifier),
        ) {
            Text(
                text = skill.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (proficient) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (editMode) {
                Text(
                    text = if (adjusted) tr("adjusted — tap to change") else tr("tap to add a bonus"),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (adjusted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = CharacterCalculations.formatModifier(bonus),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                adjusted -> MaterialTheme.colorScheme.primary
                proficient -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.width(44.dp),
        )
    }
}

@Composable
private fun ProficiencyPip(
    proficient: Boolean,
    expert: Boolean,
    editMode: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(if (editMode) 22.dp else 16.dp)
            .clip(CircleShape)
            .background(
                if (proficient) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (expert) 3.dp else 1.dp,
                color = if (proficient) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .then(if (editMode) Modifier.clickable(onClick = onClick) else Modifier),
    )
}

@Composable
private fun ToolProficienciesCard(
    character: PlayerCharacter,
    viewModel: SheetViewModel,
    editMode: Boolean,
) {
    var newTool by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionHeader(tr("Tool Proficiencies"))

            if (character.toolProficiencies.isEmpty() && !editMode) {
                Text(
                    text = tr("None"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            character.toolProficiencies.forEach { tool ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "• $tool",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (editMode) {
                        IconButton(onClick = { viewModel.removeToolProficiency(tool) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove $tool",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (editMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTool,
                        onValueChange = { newTool = it },
                        label = { Text(tr("Add a tool or proficiency")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            viewModel.addToolProficiency(newTool)
                            newTool = ""
                        },
                        enabled = newTool.isNotBlank(),
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = tr("Add tool"),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
    }
}
