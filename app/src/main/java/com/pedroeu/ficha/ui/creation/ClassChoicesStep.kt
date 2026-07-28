package com.pedroeu.ficha.ui.creation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.ui.components.ChoiceChip
import com.pedroeu.ficha.ui.components.SectionHeader
import com.pedroeu.ficha.ui.components.SelectableCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassChoicesStep(state: CreationState, viewModel: CreationViewModel) {
    val charClass = state.charClass ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(
                text = "${charClass.name} decisions. These lock in what your character is trained in and what they can do at level 1.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        charClass.choices.forEach { choice ->
            when (choice) {
                is ClassChoice.SkillProficiencyChoice -> item(key = choice.id) {
                    ChoiceBlock {
                        SectionHeader(
                            title = choice.label,
                            trailing = "${state.classSkillChoices.size} / ${choice.count}",
                        )
                        Text(
                            text = "Skills already covered by your species or origin are shown as unavailable.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            choice.options.forEach { skill ->
                                val alreadyGranted = state.grantedSkills.contains(skill)
                                ChoiceChip(
                                    label = skill.displayName,
                                    supporting = skill.ability.abbreviation,
                                    selected = state.classSkillChoices.contains(skill),
                                    enabled = !alreadyGranted,
                                    onClick = { viewModel.toggleClassSkill(skill) },
                                )
                            }
                        }
                    }
                }

                is ClassChoice.FeatureOption -> item(key = choice.id) {
                    ChoiceBlock {
                        SectionHeader(
                            title = choice.label,
                            trailing = if (state.classSelections[choice.id].isNullOrEmpty()) "Choose 1" else null,
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            choice.options.forEach { option ->
                                SelectableCard(
                                    title = option.name,
                                    subtitle = option.description,
                                    selected = state.classSelections[choice.id]
                                        ?.contains(option.id) == true,
                                    onClick = { viewModel.selectFeatureOption(choice.id, option.id) },
                                )
                            }
                        }
                    }
                }

                is ClassChoice.CantripChoice -> item(key = choice.id) {
                    val picked = state.classSelections[choice.id].orEmpty()
                    ChoiceBlock {
                        SectionHeader(
                            title = choice.label,
                            trailing = "${picked.size} / ${choice.count}",
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            choice.options.forEach { spell ->
                                SelectableCard(
                                    title = spell.name,
                                    subtitle = spell.description,
                                    selected = picked.contains(spell.id),
                                    onClick = {
                                        viewModel.toggleSpell(choice.id, spell.id, choice.count)
                                    },
                                    trailingLabel = if (spell.level == 0) "Cantrip • ${spell.school}"
                                    else "Level ${spell.level} • ${spell.school}",
                                )
                            }
                        }
                    }
                }
            }
        }

        if (charClass.id == "rogue") {
            item(key = "expertise") {
                ChoiceBlock {
                    SectionHeader(
                        title = "Expertise",
                        trailing = "${state.expertiseChoices.size} / 2",
                    )
                    Text(
                        text = "Choose two of your skill proficiencies. Your proficiency bonus is doubled for checks with them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                    val eligible = state.allSkillProficiencies.sortedBy { it.displayName }
                    if (eligible.isEmpty()) {
                        Text(
                            text = "Pick your skill proficiencies above first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            eligible.forEach { skill: Skill ->
                                ChoiceChip(
                                    label = skill.displayName,
                                    supporting = skill.ability.abbreviation,
                                    selected = state.expertiseChoices.contains(skill),
                                    onClick = { viewModel.toggleExpertise(skill) },
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
private fun ChoiceBlock(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(14.dp)) { content() }
    }
}
