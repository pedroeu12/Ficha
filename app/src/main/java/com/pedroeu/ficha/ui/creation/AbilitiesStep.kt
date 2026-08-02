package com.pedroeu.ficha.ui.creation

import com.pedroeu.ficha.ui.i18n.trf
import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import com.pedroeu.ficha.domain.AbilityScoreGeneration
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.ScoreMethod
import com.pedroeu.ficha.ui.components.ChoiceChip
import com.pedroeu.ficha.ui.components.SectionHeader

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AbilitiesStep(state: CreationState, viewModel: CreationViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(tr("Generation Method"))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ScoreMethod.entries.forEach { method ->
                        ChoiceChip(
                            label = method.displayName,
                            selected = state.scoreMethod == method,
                            onClick = { viewModel.setScoreMethod(method) },
                        )
                    }
                }
                Text(
                    text = state.scoreMethod.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when (state.scoreMethod) {
            ScoreMethod.STANDARD_ARRAY, ScoreMethod.ROLL -> {
                item {
                    PoolSection(state, viewModel)
                }
                items(count = Ability.ALL.size, key = { Ability.ALL[it].name }) { index ->
                    AssignRow(Ability.ALL[index], state, viewModel)
                }
            }
            ScoreMethod.POINT_BUY -> {
                item {
                    val remaining = AbilityScoreGeneration.pointsRemaining(state.directScores.values)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = tr("Points remaining"),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                text = "$remaining",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
                items(count = Ability.ALL.size, key = { Ability.ALL[it].name }) { index ->
                    StepperRow(Ability.ALL[index], state, viewModel, showCost = true)
                }
            }
            ScoreMethod.MANUAL -> {
                items(count = Ability.ALL.size, key = { Ability.ALL[it].name }) { index ->
                    StepperRow(Ability.ALL[index], state, viewModel, showCost = false)
                }
            }
        }

        item { FinalPreview(state) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PoolSection(state: CreationState, viewModel: CreationViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(
                title = tr("Available Scores"),
                modifier = Modifier.weight(1f),
            )
            if (state.scoreMethod == ScoreMethod.ROLL) {
                FilledTonalButton(onClick = viewModel::rerollScores) {
                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(tr("Reroll"), modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        val pool = state.availablePool()
        if (pool.isEmpty()) {
            Text(
                text = tr("All scores assigned."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pool.forEach { value ->
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "$value",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssignRow(ability: Ability, state: CreationState, viewModel: CreationViewModel) {
    val assigned = state.assignedScores[ability]
    val pool = state.availablePool().distinct().sortedDescending()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = ability.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val bonus = state.backgroundBonuses[ability]
                    if (bonus != null) {
                        Text(
                            text = trf("Origin bonus +{0}", bonus),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                if (assigned != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$assigned",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        IconButton(onClick = { viewModel.assignScore(ability, null) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear ${ability.fullName}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (assigned == null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    pool.forEach { value ->
                        ChoiceChip(
                            label = "$value",
                            selected = false,
                            onClick = { viewModel.assignScore(ability, value) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperRow(
    ability: Ability,
    state: CreationState,
    viewModel: CreationViewModel,
    showCost: Boolean,
) {
    val score = state.directScores[ability] ?: 8
    val bonus = state.backgroundBonuses[ability] ?: 0
    val finalScore = score + bonus

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = ability.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = buildString {
                        if (showCost) append("Cost ${AbilityScoreGeneration.pointBuyCost(score)}")
                        if (bonus > 0) {
                            if (isNotEmpty()) append(" • ")
                            append("Origin +$bonus")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            IconButton(onClick = { viewModel.setDirectScore(ability, score - 1) }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease ${ability.fullName}")
            }
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(44.dp),
            )
            IconButton(onClick = { viewModel.setDirectScore(ability, score + 1) }) {
                Icon(Icons.Default.Add, contentDescription = "Increase ${ability.fullName}")
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(52.dp),
            ) {
                Text(
                    text = CharacterCalculations.formatModifier(
                        CharacterCalculations.modifier(finalScore)
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = tr("mod"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FinalPreview(state: CreationState) {
    val finals = state.previewFinalScores()
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            SectionHeader(tr("Final Scores (with origin bonuses)"))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Ability.ALL.forEach { ability ->
                    val score = finals[ability] ?: 10
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = ability.abbreviation,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = CharacterCalculations.formatModifier(
                                CharacterCalculations.modifier(score)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}
