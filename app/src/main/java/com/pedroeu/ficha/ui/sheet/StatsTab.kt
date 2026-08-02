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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.OverridableStat
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.SectionHeader
import com.pedroeu.ficha.ui.components.StatEditDialog

@Composable
fun StatsTab(character: PlayerCharacter, viewModel: SheetViewModel, editMode: Boolean) {
    val scores = CharacterCalculations.finalAbilityScores(character)
    val pb = CharacterCalculations.proficiencyBonus(character)

    // Which stat, if any, currently has its editor open.
    var editingStat by remember { mutableStateOf<OverridableStat?>(null) }
    var editingAbility by remember { mutableStateOf<Ability?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { VitalsRow(character, viewModel, editMode) { editingStat = it } }
        item { HitPointsCard(character, viewModel, editMode) { editingStat = it } }
        item { ResourcesCard(character, viewModel, editMode) }
        item { DeathSavesCard(character, viewModel) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    tr("Ability Scores"),
                    trailing = trf("Proficiency {0}", CharacterCalculations.formatModifier(pb)),
                )
                Ability.ALL.forEach { ability ->
                    AbilityCard(
                        character = character,
                        ability = ability,
                        score = scores[ability] ?: 10,
                        saveBonus = CharacterCalculations.savingThrowBonus(character, ability),
                        saveProficient = CharacterCalculations.isSavingThrowProficient(character, ability),
                        editMode = editMode,
                        onEditScore = { editingAbility = ability },
                        onToggleSaveProficiency = { viewModel.toggleSaveProficiency(ability) },
                    )
                }
            }
        }
    }

    editingStat?.let { stat ->
        val rulesValue = rulesValueFor(character, stat)
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

    editingAbility?.let { ability ->
        val base = character.baseAbilityScores[ability.name] ?: 10
        val background = character.backgroundAbilityBonuses[ability.name] ?: 0
        val improvements = character.abilityScoreImprovements[ability.name] ?: 0
        StatEditDialog(
            title = ability.fullName,
            rulesValue = base + background + improvements,
            currentBonus = character.abilityScoreBonuses[ability.name],
            currentOverride = character.abilityScoreOverrides[ability.name],
            onDismiss = { editingAbility = null },
            onConfirm = { bonus, override ->
                viewModel.setAbilityBonus(ability, bonus)
                viewModel.setAbilityScore(ability, override)
                editingAbility = null
            },
            supportingText = "Base $base, origin +$background, improvements +$improvements.",
        )
    }
}

/** The value the rules alone produce, ignoring any bonus or override already stored. */
private fun rulesValueFor(character: PlayerCharacter, stat: OverridableStat): Int {
    val stripped = character.copy(statOverrides = emptyMap(), statBonuses = emptyMap())
    return when (stat) {
        OverridableStat.MAX_HIT_POINTS -> CharacterCalculations.maxHitPoints(stripped)
        OverridableStat.ARMOR_CLASS -> CharacterCalculations.armorClass(stripped)
        OverridableStat.INITIATIVE -> CharacterCalculations.initiative(stripped)
        OverridableStat.SPEED -> CharacterCalculations.speed(stripped)
        OverridableStat.PROFICIENCY_BONUS -> CharacterCalculations.proficiencyBonus(stripped)
        OverridableStat.PASSIVE_PERCEPTION -> CharacterCalculations.passivePerception(stripped)
        OverridableStat.SPELL_SAVE_DC -> CharacterCalculations.spellSaveDc(stripped) ?: 0
        OverridableStat.SPELL_ATTACK_BONUS -> CharacterCalculations.spellAttackBonus(stripped) ?: 0
        OverridableStat.MAX_PREPARED_SPELLS -> CharacterCalculations.maxPreparedSpells(stripped)
        OverridableStat.CANTRIPS_KNOWN -> CharacterCalculations.maxCantripsKnown(stripped)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VitalsRow(
    character: PlayerCharacter,
    viewModel: SheetViewModel,
    editMode: Boolean,
    onEdit: (OverridableStat) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        StatTile(
            label = tr("Armor Class"),
            value = "${CharacterCalculations.armorClass(character)}",
            adjusted = character.isAdjusted(OverridableStat.ARMOR_CLASS),
            editMode = editMode,
            onClick = { onEdit(OverridableStat.ARMOR_CLASS) },
        )
        StatTile(
            label = tr("Initiative"),
            value = CharacterCalculations.formatModifier(CharacterCalculations.initiative(character)),
            adjusted = character.isAdjusted(OverridableStat.INITIATIVE),
            editMode = editMode,
            onClick = { onEdit(OverridableStat.INITIATIVE) },
        )
        StatTile(
            label = tr("Speed"),
            value = "${CharacterCalculations.speed(character)} ft",
            adjusted = character.isAdjusted(OverridableStat.SPEED),
            editMode = editMode,
            onClick = { onEdit(OverridableStat.SPEED) },
        )
        StatTile(label = tr("Size"), value = CharacterCalculations.size(character))
        StatTile(
            label = tr("Passive Perception"),
            value = "${CharacterCalculations.passivePerception(character)}",
            adjusted = character.isAdjusted(OverridableStat.PASSIVE_PERCEPTION),
            editMode = editMode,
            onClick = { onEdit(OverridableStat.PASSIVE_PERCEPTION) },
        )
        StatTile(
            label = tr("Proficiency"),
            value = CharacterCalculations.formatModifier(
                CharacterCalculations.proficiencyBonus(character)
            ),
            adjusted = character.isAdjusted(OverridableStat.PROFICIENCY_BONUS),
            editMode = editMode,
            onClick = { onEdit(OverridableStat.PROFICIENCY_BONUS) },
        )
        InspirationTile(character.heroicInspiration, viewModel::toggleHeroicInspiration)
    }
}

private fun PlayerCharacter.isAdjusted(stat: OverridableStat): Boolean =
    statOverrides.containsKey(stat.name) || statBonuses.containsKey(stat.name)

@Composable
private fun StatTile(
    label: String,
    value: String,
    adjusted: Boolean = false,
    editMode: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val clickable = editMode && onClick != null
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .width(108.dp)
            .then(if (clickable) Modifier.clickable { onClick!!() } else Modifier),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (adjusted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary,
                )
                if (clickable) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit $label",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(14.dp),
                    )
                }
            }
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (adjusted) {
                Text(
                    text = tr("adjusted"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun InspirationTile(active: Boolean, onToggle: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .width(108.dp)
            .clickable(onClick = onToggle),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (active) tr("YES") else tr("NO"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (active) MaterialTheme.colorScheme.onSecondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = tr("HEROIC INSPIRATION"),
                style = MaterialTheme.typography.labelSmall,
                color = if (active) MaterialTheme.colorScheme.onSecondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HitPointsCard(
    character: PlayerCharacter,
    viewModel: SheetViewModel,
    editMode: Boolean,
    onEdit: (OverridableStat) -> Unit,
) {
    val max = CharacterCalculations.maxHitPoints(character)
    val hitDie = CharacterCalculations.hitDie(character)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                tr("Hit Points"),
                // A multiclass character has a mix, e.g. "5d10 + 3d6", so show the breakdown.
                trailing = "Hit Dice: ${character.level - character.hitDiceSpent}/" +
                    "${character.level} ${ClassLevels.hitDiceLabel(character)}",
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.adjustHitPoints(-1) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = tr("Take 1 damage"),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }

                Column(
                    Modifier
                        .weight(1f)
                        .then(
                            if (editMode) {
                                Modifier.clickable { onEdit(OverridableStat.MAX_HIT_POINTS) }
                            } else {
                                Modifier
                            }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "${character.currentHitPoints} / $max",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (character.temporaryHitPoints > 0) {
                        Text(
                            text = trf("+{0} temporary", character.temporaryHitPoints),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    if (editMode) {
                        Text(
                            text = tr("Tap to change max HP"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.adjustHitPoints(1) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = tr("Heal 1"),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(-5, -1, 1, 5).forEach { delta ->
                    QuickAdjustButton(
                        label = if (delta > 0) "+$delta" else "$delta",
                        onClick = { viewModel.adjustHitPoints(delta) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = tr("Temp HP"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                QuickAdjustButton(
                    label = "-1",
                    onClick = { viewModel.setTemporaryHitPoints(character.temporaryHitPoints - 1) },
                )
                Text(
                    text = "${character.temporaryHitPoints}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                QuickAdjustButton(
                    label = "+1",
                    onClick = { viewModel.setTemporaryHitPoints(character.temporaryHitPoints + 1) },
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = tr("Hit Dice Spent"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                QuickAdjustButton(
                    label = "-1",
                    onClick = { viewModel.setHitDiceSpent(character.hitDiceSpent - 1) },
                )
                Text(
                    text = "${character.hitDiceSpent}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                QuickAdjustButton(
                    label = "+1",
                    onClick = { viewModel.setHitDiceSpent(character.hitDiceSpent + 1) },
                )
            }
        }
    }
}

@Composable
private fun QuickAdjustButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeathSavesCard(character: PlayerCharacter, viewModel: SheetViewModel) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(tr("Death Saves"))
            DeathSaveRow(
                label = tr("Successes"),
                count = character.deathSaves.successes,
                color = MaterialTheme.colorScheme.secondary,
                onSet = { viewModel.setDeathSaves(it, character.deathSaves.failures) },
            )
            DeathSaveRow(
                label = tr("Failures"),
                count = character.deathSaves.failures,
                color = MaterialTheme.colorScheme.error,
                onSet = { viewModel.setDeathSaves(character.deathSaves.successes, it) },
            )
        }
    }
}

@Composable
private fun DeathSaveRow(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    onSet: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            (1..3).forEach { index ->
                val filled = index <= count
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (filled) color else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            1.dp,
                            if (filled) color else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        )
                        // Tapping a filled pip clears back to just below it.
                        .clickable { onSet(if (count == index) index - 1 else index) },
                )
            }
        }
    }
}

@Composable
private fun AbilityCard(
    character: PlayerCharacter,
    ability: Ability,
    score: Int,
    saveBonus: Int,
    saveProficient: Boolean,
    editMode: Boolean,
    onEditScore: () -> Unit,
    onToggleSaveProficiency: () -> Unit,
) {
    val mod = CharacterCalculations.modifier(score)
    val adjusted = character.abilityScoreOverrides.containsKey(ability.name) ||
        character.abilityScoreBonuses.containsKey(ability.name)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = if (editMode) Modifier.clickable { onEditScore() } else Modifier,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(74.dp),
            ) {
                Text(
                    text = CharacterCalculations.formatModifier(mod),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (adjusted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ability.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (editMode) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit ${ability.fullName}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(14.dp),
                        )
                    }
                }
                Text(
                    text = "Saving Throw ${CharacterCalculations.formatModifier(saveBonus)}" +
                        if (saveProficient) tr(" (proficient)") else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (saveProficient) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (editMode) {
                // A filled pip means proficient; tapping toggles it independently of the class.
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (saveProficient) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            1.dp,
                            if (saveProficient) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        )
                        .clickable(onClick = onToggleSaveProficiency),
                )
            }
        }
    }
}
