package com.pedroeu.ficha.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.ui.components.ChoiceChip
import com.pedroeu.ficha.ui.components.SectionHeader

/**
 * Everything about one spell: what it costs to cast, how far it reaches, how long it lasts,
 * what it needs, and what it actually does.
 *
 * The stored [KnownSpell] only carries a name, level, school, and description, so anything
 * else comes from the catalog. A spell the player wrote by hand won't be in there, and the
 * sheet shows what it has rather than pretending to know the rest.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpellDetailSheet(spell: KnownSpell, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val catalogEntry = SpellData.byId(spell.id)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = spell.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = listOf(
                    if (spell.level == 0) "Cantrip" else "Level ${spell.level}",
                    spell.school,
                ).filter { it.isNotBlank() }.joinToString(" • "),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            if (catalogEntry != null) {
                // Concentration and Ritual are the two flags that change how a spell is
                // played, so they get chips rather than being buried in a row of labels.
                if (catalogEntry.concentration || catalogEntry.ritual) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (catalogEntry.concentration) {
                            ChoiceChip("Concentration", selected = true, onClick = {})
                        }
                        if (catalogEntry.ritual) {
                            ChoiceChip("Ritual", selected = true, onClick = {})
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DetailRow("Casting Time", catalogEntry.castingTime)
                    DetailRow("Range", catalogEntry.range)
                    DetailRow("Components", catalogEntry.components)
                    DetailRow("Duration", catalogEntry.duration)
                    DetailRow(
                        "Concentration",
                        if (catalogEntry.concentration) "Yes" else "No",
                    )
                    if (catalogEntry.damage.isNotBlank()) {
                        DetailRow(
                            "Damage",
                            "${catalogEntry.damage} ${catalogEntry.damageType}",
                        )
                        DetailRow(
                            "Resolves with",
                            if (catalogEntry.needsAttackRoll) {
                                "A spell attack roll"
                            } else {
                                catalogEntry.saveAbility
                                    ?.let { "${it.fullName} saving throw" }
                                    .orEmpty()
                            },
                        )
                    }
                }
            }

            if (spell.source.isNotBlank()) {
                DetailRow("From", spell.source)
            }

            SectionHeader("Effect")
            Text(
                text = catalogEntry?.description?.takeIf { it.isNotBlank() }
                    ?: spell.description.ifBlank { "No description recorded for this spell." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (catalogEntry == null) {
                Text(
                    text = "This spell isn't in the rulebook data, so only what you entered is " +
                        "shown. You can edit its text in Edit Mode.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.6f),
        )
    }
}
