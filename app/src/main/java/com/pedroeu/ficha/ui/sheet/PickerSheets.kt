package com.pedroeu.ficha.ui.sheet

import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.model.SourceFiltering
import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.model.Recharge
import com.pedroeu.ficha.data.model.SpellDef
import com.pedroeu.ficha.domain.CustomAttack
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.ChoiceChip
import com.pedroeu.ficha.ui.components.ChoiceSection
import com.pedroeu.ficha.ui.components.SectionHeader
import com.pedroeu.ficha.ui.components.SelectableCard
import java.util.UUID

// ---------------------------------------------------------------------- Feats

/**
 * Browse the whole feat list and add one. A feat that carries its own decisions — Magic
 * Initiate's spells, Skilled's proficiencies — asks for them before it can be added, so the
 * "always present the choice" rule holds after character creation too.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatPickerSheet(
    character: PlayerCharacter,
    onDismiss: () -> Unit,
    onAdd: (featId: String, selections: Map<String, List<String>>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var selectedFeatId by remember { mutableStateOf<String?>(null) }
    var selections by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }

    val already = character.featIds.toSet()
    // The character's books apply here too, so a sheet can't collect off-book feats by hand.
    val fromBooks = SourceFiltering.available(FeatData.ALL, character.enabledSources)
    val results = remember(query, fromBooks) {
        val q = query.trim().lowercase()
        fromBooks.filter { feat ->
            feat.id != "ability_score_improvement" &&
                (q.isEmpty() || feat.name.lowercase().contains(q) ||
                    feat.description.lowercase().contains(q))
        }
    }

    val pendingChoices = selectedFeatId?.let { id ->
        FeatData.byId(id)?.let { OriginChoices.forFeat(it.id, it.name) }
    }.orEmpty()
    val allAnswered = pendingChoices.all { selections[it.id]?.size == it.count }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader(tr("Add a Feat"))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(tr("Search feats")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 420.dp),
            ) {
                items(results.size, key = { results[it].id }) { index ->
                    val feat = results[index]
                    val owned = feat.id in already
                    SelectableCard(
                        title = if (owned) "${feat.name} (already taken)" else feat.name,
                        subtitle = feat.description,
                        selected = selectedFeatId == feat.id,
                        onClick = {
                            selectedFeatId = if (selectedFeatId == feat.id) null else feat.id
                            selections = emptyMap()
                        },
                        expandedContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                pendingChoices.forEach { choice ->
                                    ChoiceSection(
                                        choice = choice,
                                        selected = selections[choice.id].orEmpty(),
                                        onToggle = { optionId ->
                                            val current = selections[choice.id].orEmpty()
                                            val next = when {
                                                current.contains(optionId) -> current - optionId
                                                current.size < choice.count -> current + optionId
                                                choice.count == 1 -> listOf(optionId)
                                                else -> current.drop(1) + optionId
                                            }
                                            selections = selections + (choice.id to next)
                                        },
                                    )
                                }
                                Button(
                                    onClick = { onAdd(feat.id, selections) },
                                    enabled = !owned && allAnswered,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        when {
                                            owned -> tr("Already taken")
                                            !allAnswered -> tr("Make every choice above first")
                                            else -> "Add ${feat.name}"
                                        }
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------- Spells

/** Browse the spell catalog and add a spell, with a fallback for anything not listed. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpellPickerSheet(
    character: PlayerCharacter,
    onDismiss: () -> Unit,
    onAdd: (KnownSpell) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var levelFilter by remember { mutableStateOf<Int?>(null) }
    var classListOnly by remember { mutableStateOf(true) }
    var customName by remember { mutableStateOf("") }

    val known = character.knownSpells.map { it.id }.toSet()
    // Off-book spells stay out of the catalogue; the custom-name field below is the way in.
    val fromBooks = SourceFiltering.available(SpellData.ALL, character.enabledSources)
    val results = remember(query, levelFilter, classListOnly, known, fromBooks) {
        val q = query.trim().lowercase()
        fromBooks.filter { spell ->
            (!classListOnly || character.classId in spell.classes) &&
                (levelFilter == null || spell.level == levelFilter) &&
                (q.isEmpty() || spell.name.lowercase().contains(q) ||
                    spell.school.lowercase().contains(q)) &&
                spell.id !in known
        }.sortedWith(compareBy({ it.level }, { it.name }))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader(tr("Add a Spell"))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(tr("Search spells")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoiceChip(
                    label = tr("My class list"),
                    selected = classListOnly,
                    onClick = { classListOnly = !classListOnly },
                )
                ChoiceChip(
                    label = tr("All levels"),
                    selected = levelFilter == null,
                    onClick = { levelFilter = null },
                )
                (0..SpellData.MAX_CATALOGUED_LEVEL).forEach { level ->
                    ChoiceChip(
                        label = if (level == 0) tr("Cantrip") else "L$level",
                        selected = levelFilter == level,
                        onClick = { levelFilter = if (levelFilter == level) null else level },
                    )
                }
            }

            if (results.isEmpty()) {
                Text(
                    text = tr("Nothing matches. Add it by name below."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 360.dp),
                ) {
                    items(results.size, key = { results[it].id }) { index ->
                        val spell = results[index]
                        SpellRow(spell) { onAdd(spell.toKnownSpell()) }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text(tr("Or add a spell by name")) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        onAdd(
                            KnownSpell(
                                id = "manual:${customName.lowercase().replace(' ', '_')}",
                                name = customName.trim(),
                                level = levelFilter ?: 1,
                                school = "",
                                description = tr("Added by hand."),
                                source = "Custom",
                            )
                        )
                        customName = ""
                    },
                    enabled = customName.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text(tr("Add")) }
            }
        }
    }
}

private fun SpellDef.toKnownSpell() = KnownSpell(
    id = id,
    name = name,
    level = level,
    school = school,
    description = description,
    // Stored on the character, so it stays in English whatever the interface language.
    source = "Added",
)

@Composable
private fun SpellRow(spell: SpellDef, onAdd: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    SelectableCard(
        title = spell.name,
        subtitle = spell.description,
        selected = expanded,
        onClick = { expanded = !expanded },
        trailingLabel = spell.subtitle,
        expandedContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${spell.castingTime} • ${spell.range} • ${spell.components} • ${spell.duration}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onAdd,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add ${spell.name}") }
            }
        },
    )
}

// ---------------------------------------------------------------------- Attacks

/** Create or edit an attack by hand, for anything the weapon table doesn't cover. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttackEditorSheet(
    existing: CustomAttack?,
    onDismiss: () -> Unit,
    onSave: (CustomAttack) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var dice by remember { mutableStateOf(existing?.damageDice.orEmpty()) }
    var bonus by remember { mutableStateOf(existing?.bonus.orEmpty()) }
    var damageType by remember { mutableStateOf(existing?.damageType.orEmpty()) }
    var range by remember { mutableStateOf(existing?.range.orEmpty()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(if (existing == null) tr("Add an Attack") else tr("Edit Attack"))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(tr("Attack name")) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dice,
                    onValueChange = { dice = it },
                    label = { Text(tr("Damage die")) },
                    placeholder = { Text("2d6") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = bonus,
                    onValueChange = { bonus = it },
                    label = { Text(tr("Attack / damage bonus")) },
                    placeholder = { Text("+7") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = damageType,
                    onValueChange = { damageType = it },
                    label = { Text(tr("Damage type")) },
                    placeholder = { Text(tr("Fire")) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = range,
                    onValueChange = { range = it },
                    label = { Text(tr("Range")) },
                    placeholder = { Text(tr("30 ft")) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(tr("Special effects / notes")) },
                placeholder = { Text(tr("On a hit, the target must succeed on a DC 15 save or…")) },
                minLines = 3,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    onSave(
                        CustomAttack(
                            id = existing?.id ?: "attack:${UUID.randomUUID()}",
                            name = name.trim(),
                            damageDice = dice.trim(),
                            bonus = bonus.trim(),
                            damageType = damageType.trim(),
                            range = range.trim(),
                            notes = notes.trim(),
                        )
                    )
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (existing == null) tr("Add attack") else tr("Save changes")) }
        }
    }
}

// ---------------------------------------------------------------------- Resources

/** Add a limited-use pool by hand, for anything the rules engine doesn't already track. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomResourceSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, max: Int, recharge: Recharge, notes: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var max by remember { mutableStateOf("1") }
    var recharge by remember { mutableStateOf(Recharge.LONG_REST) }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(tr("Track Something Else"))
            Text(
                text = tr("For any ability with a set number of uses that the app doesn't already list."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(tr("Name")) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = max,
                onValueChange = { max = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text(tr("Number of uses")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = tr("Comes back on"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Recharge.entries.forEach { option ->
                    SelectableCard(
                        title = option.label,
                        subtitle = when (option) {
                            Recharge.SHORT_REST -> tr("Refills on a Short Rest and a Long Rest.")
                            Recharge.LONG_REST -> tr("Refills only on a Long Rest.")
                            Recharge.SPECIAL -> tr("Rests leave it alone; you reset it yourself.")
                        },
                        selected = recharge == option,
                        onClick = { recharge = option },
                    )
                }
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(tr("Notes (optional)")) },
                minLines = 2,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    onSave(name.trim(), max.toIntOrNull() ?: 1, recharge, notes.trim())
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(tr("Add tracker")) }
        }
    }
}
