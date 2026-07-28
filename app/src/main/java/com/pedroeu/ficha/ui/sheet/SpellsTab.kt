package com.pedroeu.ficha.ui.sheet

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.SectionHeader

@Composable
fun SpellsTab(character: PlayerCharacter, viewModel: SheetViewModel) {
    val ability = CharacterCalculations.spellcastingAbility(character)
    val slots = CharacterCalculations.spellSlots(character)

    if (ability == null && character.knownSpells.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "This character has no spellcasting at level 1.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp),
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (ability != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        SectionHeader("Spellcasting")
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            SpellStat("Ability", ability.abbreviation)
                            SpellStat(
                                "Save DC",
                                "${CharacterCalculations.spellSaveDc(character) ?: 0}",
                            )
                            SpellStat(
                                "Attack",
                                CharacterCalculations.formatModifier(
                                    CharacterCalculations.spellAttackBonus(character) ?: 0
                                ),
                            )
                        }
                    }
                }
            }
        }

        if (slots.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader("Spell Slots")
                        slots.toSortedMap().forEach { (level, total) ->
                            val expended = character.spellSlotsExpended[level.toString()] ?: 0
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Level $level",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    (1..total).forEach { index ->
                                        val used = index <= expended
                                        Box(
                                            Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (used) MaterialTheme.colorScheme.secondary
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .clickable {
                                                    viewModel.setSpellSlotsExpended(
                                                        level,
                                                        if (expended == index) index - 1 else index,
                                                    )
                                                },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val cantrips = character.knownSpells.filter { it.level == 0 }
        val leveled = character.knownSpells.filter { it.level > 0 }

        if (cantrips.isNotEmpty()) {
            item { SpellSection("Cantrips", cantrips) }
        }
        if (leveled.isNotEmpty()) {
            item { SpellSection("Prepared & Known Spells", leveled) }
        }
    }
}

@Composable
private fun SpellStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun SpellSection(
    title: String,
    spells: List<com.pedroeu.ficha.domain.KnownSpell>,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(title)
            spells.forEach { spell ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = spell.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (spell.level == 0) "Cantrip" else "Level ${spell.level}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Text(
                        text = spell.school,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = spell.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
