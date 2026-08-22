package com.pedroeu.ficha.ui.creation

import com.pedroeu.ficha.ui.i18n.trf
import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.data.model.Species
import com.pedroeu.ficha.ui.components.ChoiceChip
import com.pedroeu.ficha.ui.components.SectionHeader
import com.pedroeu.ficha.ui.components.SelectableCard

@Composable
fun SpeciesStep(state: CreationState, viewModel: CreationViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = tr("Your species shapes your size, speed, senses, and the traits you were born with."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(state.availableSpecies, key = { it.id }) { species ->
            SelectableCard(
                title = species.name,
                subtitle = species.summary,
                selected = state.speciesId == species.id,
                onClick = { viewModel.selectSpecies(species.id) },
                trailingLabel = buildString {
                    append("${species.size} • ${species.speed} ft")
                    if (species.darkvisionRange > 0) append(" • Darkvision ${species.darkvisionRange} ft")
                },
                expandedContent = { SpeciesDetails(species, state, viewModel) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeciesDetails(
    species: Species,
    state: CreationState,
    viewModel: CreationViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SectionHeader(tr("Traits"))
        species.traits.forEach { trait ->
            Column {
                Text(
                    text = trait.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = trait.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (species.lineageOptions.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SectionHeader(
                title = species.lineageChoiceLabel ?: tr("Lineage"),
                trailing = if (state.lineageId == null) tr("Choose 1") else null,
            )
            species.lineageOptions.forEach { option ->
                SelectableCard(
                    title = option.name,
                    subtitle = option.description,
                    selected = state.lineageId == option.id,
                    onClick = { viewModel.selectLineage(option.id) },
                )
            }
        }

        if (species.bonusSkillChoiceCount > 0) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SectionHeader(
                title = tr("Skill Proficiency"),
                trailing = "${state.speciesSkillChoices.size} / ${species.bonusSkillChoiceCount}",
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Keen Senses names three skills; Skillful allows any.
                val options = species.bonusSkillOptions.ifEmpty { Skill.ALL }
                options.forEach { skill ->
                    val selected = state.speciesSkillChoices.contains(skill)
                    ChoiceChip(
                        label = skill.displayName,
                        supporting = skill.ability.abbreviation,
                        selected = selected,
                        onClick = { viewModel.toggleSpeciesSkill(skill) },
                    )
                }
            }
        }

        if (species.grantedSkills.isNotEmpty()) {
            Row(Modifier.padding(top = 4.dp)) {
                Text(
                    text = trf("Granted skill: {0}", species.grantedSkills.joinToString { it.displayName }),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
