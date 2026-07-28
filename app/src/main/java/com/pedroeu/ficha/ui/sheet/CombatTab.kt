package com.pedroeu.ficha.ui.sheet

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.SectionHeader

@Composable
fun CombatTab(character: PlayerCharacter) {
    val attacks = CharacterCalculations.attacks(character)
    val charClass = ClassData.byId(character.classId)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
                Column(Modifier.padding(14.dp)) {
                    SectionHeader("Weapons & Damage Cantrips")
                    if (attacks.isEmpty()) {
                        Text(
                            text = "No weapons carried. Add gear on the Inventory tab.",
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
                            HeaderCell("Name", Modifier.weight(2f))
                            HeaderCell("Atk", Modifier.weight(1f))
                            HeaderCell("Damage", Modifier.weight(2f))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        attacks.forEach { attack ->
                            Column(Modifier.padding(vertical = 8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = attack.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(2f),
                                    )
                                    Text(
                                        text = CharacterCalculations.formatModifier(attack.attackBonus),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = "${attack.damage} ${attack.damageType}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(2f),
                                    )
                                }
                                if (attack.notes.isNotBlank()) {
                                    Text(
                                        text = attack.notes,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("Equipment Training & Proficiencies")
                    ProficiencyLine(
                        "Armor Training",
                        charClass?.armorProficiencies?.takeIf { it.isNotEmpty() }
                            ?.joinToString() ?: "None",
                    )
                    ProficiencyLine(
                        "Weapons",
                        charClass?.weaponProficiencies?.joinToString().orEmpty(),
                    )
                    ProficiencyLine(
                        "Tools",
                        character.toolProficiencies.takeIf { it.isNotEmpty() }
                            ?.joinToString() ?: "None",
                    )
                    ProficiencyLine("Languages", character.languages.joinToString())
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
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("Defenses")
                    ProficiencyLine(
                        "Armor Class",
                        "${CharacterCalculations.armorClass(character)}",
                    )
                    ProficiencyLine(
                        "Initiative",
                        CharacterCalculations.formatModifier(
                            CharacterCalculations.initiative(character)
                        ),
                    )
                    ProficiencyLine("Speed", "${CharacterCalculations.speed(character)} ft")
                    val equipped = character.inventory.filter { it.equipped && it.armorDefId != null }
                    ProficiencyLine(
                        "Equipped Armor",
                        equipped.takeIf { it.isNotEmpty() }?.joinToString { it.name } ?: "None",
                    )
                }
            }
        }
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
