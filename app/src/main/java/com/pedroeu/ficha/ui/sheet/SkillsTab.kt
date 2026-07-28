package com.pedroeu.ficha.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.SectionHeader

@Composable
fun SkillsTab(character: PlayerCharacter) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Filled pips mark proficiency; a doubled pip marks expertise.",
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
                                    CharacterCalculations.modifier(
                                        CharacterCalculations.finalAbilityScores(character)[ability] ?: 10
                                    )
                                )
                            }",
                        )
                        skills.forEach { skill ->
                            SkillRow(
                                skill = skill,
                                bonus = CharacterCalculations.skillBonus(character, skill),
                                proficient = character.skillProficiencies.contains(skill.name),
                                expert = character.skillExpertise.contains(skill.name),
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
                    SectionHeader("Passive Perception")
                    Text(
                        text = "${CharacterCalculations.passivePerception(character)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }

        if (character.toolProficiencies.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionHeader("Tool Proficiencies")
                        character.toolProficiencies.forEach { tool ->
                            Text(
                                text = "• $tool",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillRow(
    skill: Skill,
    bonus: Int,
    proficient: Boolean,
    expert: Boolean,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProficiencyPip(proficient = proficient, expert = expert)
        Text(
            text = skill.displayName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (proficient) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        )
        Text(
            text = CharacterCalculations.formatModifier(bonus),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (proficient) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp),
        )
    }
}

@Composable
private fun ProficiencyPip(proficient: Boolean, expert: Boolean) {
    Box(
        Modifier
            .size(16.dp)
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
            ),
    )
}
