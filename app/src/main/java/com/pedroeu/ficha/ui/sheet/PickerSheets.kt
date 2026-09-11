package com.pedroeu.ficha.ui.sheet

import com.pedroeu.ficha.ui.design.Corner
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
import com.pedroeu.ficha.ui.i18n.trf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.material3.Checkbox
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterAttacks
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.ui.components.SourceSectionHeader
import com.pedroeu.ficha.ui.components.SourceGrouping
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ClassLevels
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
    var collapsedBooks by rememberSaveable { mutableStateOf(setOf<String>()) }
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
                shape = Corner.row,
                modifier = Modifier.fillMaxWidth(),
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 420.dp),
            ) {
                // A search is already a filter; grouping its results too buries the matches.
                val sections =
                    if (query.isBlank() && SourceGrouping.worthGrouping(results) { it.book }) {
                        SourceGrouping.byBook(results) { it.book }
                    } else {
                        listOf(null to results)
                    }
                sections.forEach { (book, entries) ->
                val sectionKey = book?.id ?: "all"
                val open = sectionKey !in collapsedBooks
                if (book != null) {
                    item(key = "header:$sectionKey") {
                        SourceSectionHeader(
                            book = book,
                            count = entries.size,
                            expanded = open,
                            onToggle = {
                                collapsedBooks =
                                    if (open) collapsedBooks + sectionKey
                                    else collapsedBooks - sectionKey
                            },
                        )
                    }
                }
                if (open) items(entries.size, key = { entries[it].id }) { index ->
                    val feat = entries[index]
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
                                    shape = Corner.row,
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
    var collapsedBooks by rememberSaveable { mutableStateOf(setOf<String>()) }

    // Spells already on the sheet, chosen or granted. A spell the rules hand over is not a
    // spell to add: offering it again puts a second copy in the list.
    val known = character.knownSpells.map { it.id }.toSet() +
        CharacterSpells.granted(character).map { it.spell.id }
    // "My class's list" means every class the character has, or a Cleric/Wizard browsing for
    // a Wizard spell would be told there isn't one.
    val classIds = ClassLevels.of(character).map { it.classId }.toSet()
    // Off-book spells stay out of the catalogue; the custom-name field below is the way in.
    val fromBooks = SourceFiltering.available(SpellData.ALL, character.enabledSources)
    val results = remember(query, levelFilter, classListOnly, known, fromBooks) {
        val q = query.trim().lowercase()
        fromBooks.filter { spell ->
            (!classListOnly || spell.classes.any { it in classIds }) &&
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
                shape = Corner.row,
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
                    val sections =
                        if (query.isBlank() && SourceGrouping.worthGrouping(results) { it.book }) {
                            SourceGrouping.byBook(results) { it.book }
                        } else {
                            listOf(null to results)
                        }
                    sections.forEach { (book, entries) ->
                    val sectionKey = book?.id ?: "all"
                    val open = sectionKey !in collapsedBooks
                    if (book != null) {
                        item(key = "header:$sectionKey") {
                            SourceSectionHeader(
                                book = book,
                                count = entries.size,
                                expanded = open,
                                onToggle = {
                                    collapsedBooks =
                                        if (open) collapsedBooks + sectionKey
                                        else collapsedBooks - sectionKey
                                },
                            )
                        }
                    }
                    if (open) items(entries.size, key = { entries[it].id }) { index ->
                        val spell = entries[index]
                        SpellRow(spell) { onAdd(spell.toKnownSpell()) }
                    }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text(tr("Or add a spell by name")) },
                    singleLine = true,
                    shape = Corner.row,
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
                    shape = Corner.row,
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
                    shape = Corner.row,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add ${spell.name}") }
            }
        },
    )
}

// ---------------------------------------------------------------------- Attacks

/** Create or edit an attack by hand, for anything the weapon table doesn't cover. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AttackEditorSheet(
    character: PlayerCharacter,
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
    var abilityName by remember { mutableStateOf(existing?.abilityName.orEmpty()) }
    var proficient by remember { mutableStateOf(existing?.proficient ?: true) }
    var magicBonus by remember { mutableStateOf(existing?.magicBonus ?: 0) }

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
                shape = Corner.row,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dice,
                    onValueChange = { dice = it },
                    label = { Text(tr("Damage die")) },
                    placeholder = { Text("2d6") },
                    singleLine = true,
                    shape = Corner.row,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = damageType,
                    onValueChange = { damageType = it },
                    label = { Text(tr("Damage type")) },
                    placeholder = { Text(tr("Slashing")) },
                    singleLine = true,
                    shape = Corner.row,
                    modifier = Modifier.weight(1f),
                )
            }

            // The three things that decide the numbers. Everything below is worked out from
            // them, so the player picks a Strength longsword rather than doing the addition.
            Text(
                text = tr("Ability used"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoiceChip(
                    label = tr("None"),
                    selected = abilityName.isBlank(),
                    onClick = { abilityName = "" },
                )
                Ability.ALL.forEach { ability ->
                    ChoiceChip(
                        label = ability.abbreviation,
                        supporting = CharacterCalculations.formatModifier(
                            CharacterCalculations.abilityModifiers(character)[ability] ?: 0,
                        ),
                        selected = abilityName == ability.name,
                        onClick = { abilityName = ability.name },
                    )
                }
            }

            Text(
                text = tr("Magic bonus"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 1, 2, 3).forEach { value ->
                    ChoiceChip(
                        label = if (value == 0) tr("None") else "+$value",
                        selected = magicBonus == value,
                        onClick = { magicBonus = value },
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { proficient = !proficient },
            ) {
                Checkbox(checked = proficient, onCheckedChange = { proficient = it })
                Text(
                    text = trf(
                        "Proficient ({0})",
                        CharacterCalculations.formatModifier(
                            CharacterCalculations.proficiencyBonus(character),
                        ),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = range,
                    onValueChange = { range = it },
                    label = { Text(tr("Range")) },
                    placeholder = { Text(tr("30 ft")) },
                    singleLine = true,
                    shape = Corner.row,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = bonus,
                    onValueChange = { bonus = it },
                    label = { Text(tr("Override the bonus")) },
                    placeholder = { Text(tr("e.g. DEX + PB")) },
                    singleLine = true,
                    shape = Corner.row,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(tr("Special effects / notes")) },
                placeholder = { Text(tr("On a hit, the target must succeed on a DC 15 save or…")) },
                minLines = 3,
                shape = Corner.row,
                modifier = Modifier.fillMaxWidth(),
            )
            val draft = CustomAttack(
                id = existing?.id ?: "attack:${UUID.randomUUID()}",
                name = name.trim(),
                damageDice = dice.trim(),
                bonus = bonus.trim(),
                damageType = damageType.trim(),
                range = range.trim(),
                notes = notes.trim(),
                abilityName = abilityName,
                proficient = proficient,
                magicBonus = magicBonus,
            )
            val (toHit, damage) = CharacterAttacks.customAttackNumbers(character, draft)
            Text(
                text = trf(
                    "This attack: {0} to hit, {1} {2}",
                    toHit,
                    damage.ifBlank { "-" },
                    damageType.trim(),
                ).trimEnd(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
            )

            Button(
                onClick = { onSave(draft) },
                enabled = name.isNotBlank(),
                shape = Corner.row,
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
                shape = Corner.row,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = max,
                onValueChange = { max = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text(tr("Number of uses")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = Corner.row,
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
                shape = Corner.row,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    onSave(name.trim(), max.toIntOrNull() ?: 1, recharge, notes.trim())
                },
                enabled = name.isNotBlank(),
                shape = Corner.row,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(tr("Add tracker")) }
        }
    }
}
