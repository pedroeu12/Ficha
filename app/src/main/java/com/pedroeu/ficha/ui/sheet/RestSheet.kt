package com.pedroeu.ficha.ui.sheet

import com.pedroeu.ficha.ui.design.Corner
import com.pedroeu.ficha.ui.i18n.trf
import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Recharge
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterHitDice
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.RestEngine
import com.pedroeu.ficha.domain.RestOutcome
import com.pedroeu.ficha.ui.components.ChoiceSection
import com.pedroeu.ficha.ui.components.SectionDisclosure
import com.pedroeu.ficha.ui.components.Disclosed
import com.pedroeu.ficha.ui.design.Space
import com.pedroeu.ficha.ui.components.SectionHeader

enum class RestKind(private val titleKey: String) {
    SHORT("Short Rest"),
    LONG("Long Rest");

    /** Translated on read, since an enum's constructor runs only once. */
    val title: String get() = tr(titleKey)
}

/**
 * Short Rest lets the player spend Hit Dice one at a time, rolling or taking the average, and
 * shows what each die actually restores. Long Rest simply reports what it will put back.
 * Both offer any pick the rules let you revisit on a rest, such as a Monk reshaping a tattoo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestSheet(
    kind: RestKind,
    character: PlayerCharacter,
    viewModel: SheetViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rolls by remember { mutableStateOf<List<RestEngine.DieSpent>>(emptyList()) }
    var outcome by remember { mutableStateOf<RestOutcome?>(null) }

    val conMod = CharacterCalculations.abilityModifiers(character)[Ability.CON] ?: 0
    // Each class has its own dice, so what is left is counted per pool: a Fighter 5 / Wizard 3
    // who has spent every d10 still has three d6s, and one button for "a Hit Die" cannot say
    // which of the two it means.
    val pools = CharacterHitDice.pools(character)
    val spentHere = rolls.groupingBy { it.classId }.eachCount()
    val available = RestEngine.availableHitDice(character) - rolls.size
    val projectedHealing = rolls.sumOf { (it.roll + conMod).coerceAtLeast(1) }

    val restChangeable = ChoiceResolver.restChangeable(character)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        // The sheet is given a bounded height so the body can scroll inside it and the
        // action button can be pinned below. Without that the body grew past the screen and
        // took the button with it, which is how a Long Rest became impossible to confirm.
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = kind.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            val done = outcome
            if (done != null) {
                RestSummary(done)
                Button(
                    onClick = onDismiss,
                    shape = Corner.row,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(tr("Done")) }
                return@Column
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {

            if (kind == RestKind.SHORT) {
                RestCard {
                    SectionHeader(
                        tr("Hit Dice"),
                        trailing = "$available of ${CharacterHitDice.totalDice(character)} left",
                    )
                    Text(
                        text = "Each die you spend restores its roll plus your Constitution " +
                            "modifier (${CharacterCalculations.formatModifier(conMod)}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
                    )

                    if (rolls.isNotEmpty()) {
                        Text(
                            text = rolls.joinToString(" + ") { "${it.roll}${
                                if (conMod >= 0) "+$conMod" else "$conMod"
                            }" } + "  =  $projectedHealing hit points",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    // A row of buttons per pool. One class is the common case and reads as
                    // it always did; two classes get to choose which die they are spending.
                    pools.forEach { pool ->
                        val left = pool.remaining - (spentHere[pool.classId] ?: 0)
                        if (pools.size > 1) {
                            Text(
                                text = "${pool.className} — ${pool.label}, $left left",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    rolls = rolls + RestEngine.DieSpent(
                                        pool.classId, viewModel.rollHitDie(pool.classId),
                                    )
                                },
                                enabled = left > 0,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.Casino, contentDescription = null)
                                Text(trf("  Roll d{0}", pool.die))
                            }
                            OutlinedButton(
                                onClick = {
                                    rolls = rolls + RestEngine.DieSpent(
                                        pool.classId, pool.die / 2 + 1,
                                    )
                                },
                                enabled = left > 0,
                                modifier = Modifier.weight(1f),
                            ) { Text(tr("Average")) }
                        }
                    }
                    if (rolls.isNotEmpty()) {
                        TextButton(onClick = { rolls = emptyList() }) { Text(tr("Clear dice")) }
                    }
                }
            }

            RestCard {
                SectionHeader(tr("What comes back"))
                val restored = CharacterResources.states(character).filter { state ->
                    state.spent > 0 && state.def.recharge.refilledBy(
                        if (kind == RestKind.SHORT) Recharge.SHORT_REST else Recharge.LONG_REST
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    if (kind == RestKind.LONG) {
                        Bullet(tr("Hit points restored to full"))
                        Bullet(
                            "${RestEngine.hitDiceRecoveredOnLongRest(character)} Hit Dice recovered"
                        )
                        Bullet(tr("All spell slots"))
                        Bullet(tr("Death saves cleared"))
                    } else if (CharacterCalculations.casterType(character) ==
                        com.pedroeu.ficha.data.model.CasterType.PACT
                    ) {
                        Bullet(tr("Pact Magic spell slots"))
                    }
                    restored.forEach { Bullet(it.def.name) }
                    if (restored.isEmpty() && kind == RestKind.SHORT) {
                        Text(
                            text = tr("Nothing else is spent right now."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Clerics, Artificers, and the other prepared casters rebuild their list each
            // day, so a Long Rest is exactly when that decision gets made.
            if (kind == RestKind.LONG && CharacterSpells.preparesDaily(character)) {
                RestSection(
                    title = tr("Prepare spells"),
                    trailing = trf(
                        "{0} / {1}",
                        character.knownSpells.count { it.prepared && it.level > 0 },
                        CharacterCalculations.maxPreparedSpells(character),
                    ),
                ) { PreparationSection(character, viewModel) }
            }

            // The Artificer is alone in rethinking a cantrip overnight rather than on
            // levelling, so this appears only when a class on the sheet actually says so.
            if (kind == RestKind.LONG) {
                CharacterSpells.classesSwappingCantripsOnLongRest(character).forEach { classId ->
                    // Nobody swaps a cantrip most nights, so this one starts folded.
                    RestSection(title = tr("Swap a cantrip"), startExpanded = false) {
                        CantripSwapSection(character, viewModel, classId)
                    }
                }
            }

            if (restChangeable.isNotEmpty()) {
                RestSection(
                    title = tr("You may change these while you rest"),
                    trailing = restChangeable.size.toString(),
                    startExpanded = false,
                ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    restChangeable.forEach { resolved ->
                        ChoiceSection(
                            choice = resolved.choice,
                            selected = resolved.selectedIds,
                            onToggle = { optionId ->
                                val current = resolved.selectedIds
                                val next = when {
                                    current.contains(optionId) -> current - optionId
                                    current.size < resolved.choice.count -> current + optionId
                                    resolved.choice.count == 1 -> listOf(optionId)
                                    else -> current.drop(1) + optionId
                                }
                                viewModel.setChoiceSelection(
                                    resolved.choice.id,
                                    resolved.level,
                                    next,
                                )
                            },
                        )
                    }
                }
                }
            }

            }

            Button(
                onClick = {
                    outcome = if (kind == RestKind.SHORT) {
                        viewModel.shortRest(rolls)
                    } else {
                        viewModel.longRestDetailed()
                    }
                },
                shape = Corner.row,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (kind == RestKind.SHORT && rolls.isEmpty()) {
                        tr("Rest without spending Hit Dice")
                    } else {
                        "Finish ${kind.title}"
                    }
                )
            }
        }
    }
}

/**
 * One folding section of the rest sheet.
 *
 * A Long Rest can ask for a lot at once — a prepared caster's whole list, a cantrip swap, and
 * every choice the rules let you revisit — and stacking all of it open means scrolling past
 * the parts you are not changing today. Sections start open only when they are the point.
 */
@Composable
private fun RestSection(
    title: String,
    trailing: String = "",
    startExpanded: Boolean = true,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(startExpanded) }
    Column(Modifier.fillMaxWidth()) {
        SectionDisclosure(
            title = title,
            expanded = expanded,
            onToggle = { expanded = !expanded },
            trailing = trailing,
        )
        Disclosed(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.inline)) { content() }
        }
    }
}

@Composable
private fun RestSummary(outcome: RestOutcome) {
    RestCard {
        SectionHeader(tr("Rest complete"))
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            if (outcome.hitPointsRegained > 0) {
                Bullet("Regained ${outcome.hitPointsRegained} hit points")
            }
            if (outcome.hitDiceSpent > 0) {
                Bullet("Spent ${outcome.hitDiceSpent} Hit Dice")
            }
            if (outcome.spellSlotsRestored) Bullet(tr("Spell slots restored"))
            outcome.resourcesRestored.forEach { Bullet("$it restored") }
            if (outcome.hitPointsRegained == 0 &&
                outcome.resourcesRestored.isEmpty() &&
                !outcome.spellSlotsRestored
            ) {
                Text(
                    text = tr("Nothing needed restoring."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RestCard(content: @Composable () -> Unit) {
    Card(
        shape = Corner.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) { content() }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "•  ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Choosing which spells to carry for the day.
 *
 * A prepared caster picks from its whole class list, which for a Cleric past level 9 is
 * something like a hundred spells — so this is a picker, not a checklist. What is currently
 * prepared sits at the top where it can be read at a glance and set aside without hunting,
 * and the rest is searchable and grouped by spell level. A Wizard sees only its spellbook,
 * because that is what its list actually is.
 */
@Composable
private fun PreparationSection(character: PlayerCharacter, viewModel: SheetViewModel) {
    val preparable = CharacterSpells.preparable(character)
    if (preparable.isEmpty()) return

    val maxPrepared = CharacterCalculations.maxPreparedSpells(character)
    val prepared = preparable.filter { it.prepared }
    var query by remember { mutableStateOf("") }

    val available = preparable
        .filterNot { it.prepared }
        .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        .groupBy { it.level }
        .toSortedMap()

    Text(
        text = trf("Prepared spells  {0} / {1}", prepared.size, maxPrepared),
        style = MaterialTheme.typography.labelLarge,
        color = if (prepared.size > maxPrepared) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.secondary,
    )
    Text(
        text = tr("Choose the spells you have ready for the day. Tap one to prepare or set it aside."),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (prepared.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            prepared.forEach { spell ->
                PreparableRow(spell, prepared = true) {
                    viewModel.setSpellPrepared(spell, false)
                }
            }
        }
    }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text(tr("Search spells")) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )

    // A plain Column: this sits inside the sheet's own scroll now, and a lazy list nested in
    // a scrolling parent is what squeezed this list down to one visible row.
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        available.forEach { (level, spells) ->
            Text(
                text = trf("Level {0}", level),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
            spells.forEach { spell ->
                PreparableRow(spell, prepared = false) {
                    viewModel.setSpellPrepared(spell, true)
                }
            }
        }
    }
}

@Composable
private fun PreparableRow(
    spell: KnownSpell,
    prepared: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = spell.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (spell.level == 0) tr("Cantrip") else trf("Level {0} • {1}", spell.level, spell.school),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (prepared) tr("Prepared") else tr("Prepare"),
            style = MaterialTheme.typography.labelSmall,
            color = if (prepared) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Trading one cantrip for another over a Long Rest.
 *
 * The Artificer's rule reads *"whenever you finish a Long Rest, you can replace one of your
 * cantrips from this feature with another Artificer cantrip of your choice"* — one for one,
 * and only ever offered, never required. So this shows what is known, and swapping is two
 * taps: the one being set down, then the one being taken up.
 */
@Composable
private fun CantripSwapSection(
    character: PlayerCharacter,
    viewModel: SheetViewModel,
    classId: String,
) {
    val known = CharacterSpells.swappableCantrips(character, classId)
    if (known.isEmpty()) return

    var giving by remember(classId) { mutableStateOf<String?>(null) }
    val className = ClassData.byId(classId)?.name ?: classId

    RestCard {
        SectionHeader(trf("Swap a {0} Cantrip", className))
        Text(
            text = tr("You may trade one cantrip for another as you rest. Optional."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )

        val chosen = giving
        if (chosen == null) {
            known.forEach { spell ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { giving = spell.id }
                        .padding(vertical = 5.dp),
                ) {
                    Text(
                        text = spell.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = tr("Trade"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            val giveUp = known.first { it.id == chosen }
            Text(
                text = trf("Setting down {0}. Choose what to learn instead.", giveUp.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            TextButton(onClick = { giving = null }) { Text(tr("Keep it after all")) }

            val onSheet = character.knownSpells.map { it.id }.toSet()
            val options = CharacterSpells.cantripChoices(classId).filterNot { it.id in onSheet }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                options.forEach { spell ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.swapCantrip(giveUp.id, spell, className)
                                giving = null
                            }
                            .padding(vertical = 5.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = spell.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = spell.school,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = tr("Learn"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
    }
}
