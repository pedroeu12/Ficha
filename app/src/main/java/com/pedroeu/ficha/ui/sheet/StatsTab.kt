package com.pedroeu.ficha.ui.sheet

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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.SectionHeader

@Composable
fun StatsTab(character: PlayerCharacter, viewModel: SheetViewModel) {
    val scores = CharacterCalculations.finalAbilityScores(character)
    val pb = CharacterCalculations.proficiencyBonus(character.level)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { VitalsRow(character, viewModel) }
        item { HitPointsCard(character, viewModel) }
        item { DeathSavesCard(character, viewModel) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("Ability Scores", trailing = "Proficiency +$pb")
                Ability.ALL.forEach { ability ->
                    AbilityCard(
                        ability = ability,
                        score = scores[ability] ?: 10,
                        saveBonus = CharacterCalculations.savingThrowBonus(character, ability),
                        saveProficient = CharacterCalculations.isSavingThrowProficient(character, ability),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VitalsRow(character: PlayerCharacter, viewModel: SheetViewModel) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        StatTile("Armor Class", "${CharacterCalculations.armorClass(character)}")
        StatTile(
            "Initiative",
            CharacterCalculations.formatModifier(CharacterCalculations.initiative(character)),
        )
        StatTile("Speed", "${CharacterCalculations.speed(character)} ft")
        StatTile("Size", CharacterCalculations.size(character))
        StatTile("Passive Perception", "${CharacterCalculations.passivePerception(character)}")
        StatTile(
            "Proficiency",
            CharacterCalculations.formatModifier(
                CharacterCalculations.proficiencyBonus(character.level)
            ),
        )
        InspirationTile(character.heroicInspiration, viewModel::toggleHeroicInspiration)
    }
}

@Composable
private fun StatTile(label: String, value: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.width(108.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
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
                text = if (active) "YES" else "NO",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (active) MaterialTheme.colorScheme.onSecondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "HEROIC INSPIRATION",
                style = MaterialTheme.typography.labelSmall,
                color = if (active) MaterialTheme.colorScheme.onSecondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HitPointsCard(character: PlayerCharacter, viewModel: SheetViewModel) {
    val max = CharacterCalculations.maxHitPoints(character)
    val hitDie = CharacterCalculations.hitDie(character)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Hit Points", trailing = "Hit Dice: ${character.level - character.hitDiceSpent}/${character.level} d$hitDie")

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.adjustHitPoints(-1) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Take 1 damage", tint = MaterialTheme.colorScheme.error)
                }

                Column(
                    Modifier.weight(1f),
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
                            text = "+${character.temporaryHitPoints} temporary",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
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
                    Icon(Icons.Default.Add, contentDescription = "Heal 1", tint = MaterialTheme.colorScheme.secondary)
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
                    text = "Temp HP",
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
                    text = "Hit Dice Spent",
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
            SectionHeader("Death Saves")
            DeathSaveRow(
                label = "Successes",
                count = character.deathSaves.successes,
                color = MaterialTheme.colorScheme.secondary,
                onSet = { viewModel.setDeathSaves(it, character.deathSaves.failures) },
            )
            DeathSaveRow(
                label = "Failures",
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
    ability: Ability,
    score: Int,
    saveBonus: Int,
    saveProficient: Boolean,
) {
    val mod = CharacterCalculations.modifier(score)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = ability.fullName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Saving Throw ${CharacterCalculations.formatModifier(saveBonus)}" +
                        if (saveProficient) " (proficient)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (saveProficient) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
