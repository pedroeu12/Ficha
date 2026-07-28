package com.pedroeu.ficha.ui.creation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.ui.components.ChoiceChip
import com.pedroeu.ficha.ui.components.SectionHeader

private val ALIGNMENTS = listOf(
    "Lawful Good", "Neutral Good", "Chaotic Good",
    "Lawful Neutral", "True Neutral", "Chaotic Neutral",
    "Lawful Evil", "Neutral Evil", "Chaotic Evil",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsStep(state: CreationState, viewModel: CreationViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Character Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
            )
        }

        item { CharacterSummary(state) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("Alignment")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ALIGNMENTS.forEach { alignment ->
                        ChoiceChip(
                            label = alignment,
                            selected = state.alignment == alignment,
                            onClick = {
                                viewModel.setAlignment(
                                    if (state.alignment == alignment) "" else alignment
                                )
                            },
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.appearance,
                onValueChange = viewModel::setAppearance,
                label = { Text("Appearance (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
            )
        }

        item {
            OutlinedTextField(
                value = state.backstory,
                onValueChange = viewModel::setBackstory,
                label = { Text("Backstory & Personality (optional)") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
            )
        }
    }
}

@Composable
private fun CharacterSummary(state: CreationState) {
    val species = state.species
    val charClass = state.charClass
    val background = state.background
    val finals = state.previewFinalScores()

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = state.name.ifBlank { "Unnamed Adventurer" },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "Level 1 ${species?.name.orEmpty()} ${charClass?.name.orEmpty()}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            background?.let {
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Row(
                Modifier.fillMaxWidth(),
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

            SummaryLine(
                "Skills",
                state.allSkillProficiencies.sortedBy { it.displayName }
                    .joinToString { it.displayName },
            )
            if (state.expertiseChoices.isNotEmpty()) {
                SummaryLine(
                    "Expertise",
                    state.expertiseChoices.joinToString { it.displayName },
                )
            }
            background?.featId?.let { featId ->
                FeatData.byId(featId)?.let { SummaryLine("Origin Feat", it.name) }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
    }
}
