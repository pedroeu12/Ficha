package com.pedroeu.ficha.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterSummons
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.SummonEdits
import com.pedroeu.ficha.rules.ActionKind
import com.pedroeu.ficha.rules.ActiveSummon
import com.pedroeu.ficha.rules.CustomAction
import com.pedroeu.ficha.rules.FormulaEval
import com.pedroeu.ficha.ui.design.Corner
import com.pedroeu.ficha.ui.design.Space
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.i18n.trf

/**
 * A summoned creature's own sheet.
 *
 * Deliberately the same components the character's sheet is built from — [DetailRow] for a
 * readable line, [EditableText] for a name, the stat pips — because a summon is a thing on the
 * table like anything else and a second visual language for it would be one more place for the
 * two to drift.
 *
 * Shared between the phone and the tablet: the switcher that leads here lives above the point
 * where the two layouts part, so there is one implementation and no way for one device to gain
 * a creature the other cannot see.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummonSheet(
    character: PlayerCharacter,
    summon: ActiveSummon,
    editMode: Boolean,
    onRename: (String) -> Unit,
    onDamage: (Int) -> Unit,
    onHeal: (Int) -> Unit,
    onSetHitPoints: (Int, Int?) -> Unit,
    onDismiss: () -> Unit,
    onSpend: (String, Int) -> Unit,
    onNotes: (String) -> Unit,
    /** Edit Mode's way to correct a creature's maximum, for a rolled or house-ruled one. */
    onSetMaxHitPoints: (Int) -> Unit = {},
    /**
     * Edit Mode's way to change anything else about this creature: its Armor Class, its
     * speed, its size, an ability score, what one of its actions says.
     *
     * Keyed by field rather than a callback per line, because the alternative is twenty
     * parameters and a screen that has to be edited every time a stat block gains a row.
     */
    onSetField: (key: String, value: String?) -> Unit = { _, _ -> },
    /** Edit Mode's way to give this creature something new to do, or take one away. */
    onAddAction: (CustomAction) -> Unit = {},
    onRemoveAction: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Which kind of action is being written, if any.
    var adding by rememberSaveable { mutableStateOf<ActionKind?>(null) }

    // The creature as it stands, not as the book printed it: whatever has been changed about
    // this one is already folded in, so nothing below has to know an edit happened.
    val statblock = CharacterSummons.statblockFor(character, summon)
    if (statblock == null) {
        Column(modifier.fillMaxSize().padding(Space.screenEdge)) {
            Text(
                text = tr("This creature's rules are no longer in the app."),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    val owningClassId = null as String?

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Space.screenEdge),
        verticalArrangement = Arrangement.spacedBy(Space.betweenCards),
    ) {
        item {
            Card(
                shape = Corner.card,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    Modifier.padding(Space.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(Space.inline),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            EditableText(
                                value = summon.name.ifBlank { statblock.name },
                                editMode = editMode,
                                onChange = { onRename(it.orEmpty()) },
                                label = tr("Name"),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            EditableText(
                                value = "${statblock.size} ${statblock.creatureType}".trim() +
                                    if (summon.sourceLabel.isNotBlank()) {
                                        " · ${summon.sourceLabel}"
                                    } else "",
                                editMode = editMode,
                                onChange = { onSetField("creatureType", it) },
                                label = tr("Size and type"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                isOverridden = SummonEdits.isEdited(summon, "creatureType"),
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = tr("Dismiss this creature"),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    HitPointBar(
                        current = summon.currentHp,
                        max = summon.maxHp,
                        temp = summon.tempHp,
                        editMode = editMode,
                        onDamage = onDamage,
                        onHeal = onHeal,
                        onSet = onSetHitPoints,
                        onSetMax = onSetMaxHitPoints,
                    )

                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalArrangement = Arrangement.spacedBy(Space.inline),
                    ) {
                        EditableStone(
                            label = tr("AC"),
                            value = statblock
                                .armorClassFor(character, summon.spellLevel, owningClassId)
                                .toString(),
                            editMode = editMode,
                            edited = SummonEdits.isEdited(summon, "armorClass"),
                            onChange = { onSetField("armorClass", it) },
                        )
                        EditableStone(
                            label = tr("Speed"),
                            value = statblock.speed,
                            editMode = editMode,
                            edited = SummonEdits.isEdited(summon, "speed"),
                            onChange = { onSetField("speed", it) },
                        )
                        CharacterSummons.attackBonus(character, summon)?.let {
                            StatStone(tr("Attack"), if (it >= 0) "+$it" else "$it")
                        }
                    }
                }
            }
        }

        item {
            Card(shape = Corner.card) {
                Column(
                    Modifier.padding(Space.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(Space.inline),
                ) {
                    Text(
                        text = tr("Ability Scores"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalArrangement = Arrangement.spacedBy(Space.inline),
                    ) {
                        Ability.ALL.forEach { ability ->
                            val score = statblock.abilityScores[ability] ?: 10
                            val mod = statblock.modifier(ability)
                            EditableStone(
                                label = ability.abbreviation,
                                value = "$score (${if (mod >= 0) "+$mod" else "$mod"})",
                                editMode = editMode,
                                edited = SummonEdits.isEdited(summon, "ability:${ability.name}"),
                                // Typed as a score; the modifier beside it is the app's job.
                                editValue = score.toString(),
                                onChange = { onSetField("ability:${ability.name}", it) },
                            )
                        }
                    }
                }
            }
        }

        // Every line a stat block can carry, whether or not this creature has one — in Edit
        // Mode an empty row is the only way to add what the book left out, and a creature that
        // gained resistance to fire at the table has nowhere else to say so.
        val defences = listOf(
            Triple(tr("Resistances"), "resistances", statblock.resistances.joinToString(", ")),
            Triple(
                tr("Vulnerabilities"), "vulnerabilities",
                statblock.vulnerabilities.joinToString(", "),
            ),
            Triple(tr("Immunities"), "immunities", statblock.immunities.joinToString(", ")),
            Triple(
                tr("Condition Immunities"), "conditionImmunities",
                statblock.conditionImmunities.joinToString(", "),
            ),
            Triple(tr("Senses"), "senses", statblock.senses),
            Triple(tr("Languages"), "languages", statblock.languages),
        ).filter { editMode || it.third.isNotBlank() }

        if (defences.isNotEmpty()) {
            item {
                Card(shape = Corner.card) {
                    Column(
                        Modifier.padding(Space.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(Space.tight),
                    ) {
                        defences.forEach { (label, key, value) ->
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    text = "$label: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                EditableText(
                                    value = value,
                                    editMode = editMode,
                                    onChange = { onSetField(key, it) },
                                    label = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    isOverridden = SummonEdits.isEdited(summon, key),
                                    placeholder = tr("None"),
                                )
                            }
                        }
                    }
                }
            }
        }

        val grouped = statblock.actions.groupBy { it.kind }
        listOf(
            ActionKind.TRAIT to tr("Traits"),
            ActionKind.ACTION to tr("Actions"),
            ActionKind.BONUS_ACTION to tr("Bonus Actions"),
            ActionKind.REACTION to tr("Reactions"),
        ).forEach { (kind, heading) ->
            val actions = grouped[kind].orEmpty()
            if (actions.isEmpty() && !editMode) return@forEach
            item {
                Card(shape = Corner.card) {
                    Column(Modifier.padding(Space.cardPadding)) {
                        Text(
                            text = heading,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = Space.inline),
                        )
                        actions.forEachIndexed { index, action ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                            val numbers = buildList {
                                action.toHit?.let {
                                    CharacterSummons.attackBonus(character, summon)?.let { b ->
                                        add(trf("{0} to hit", if (b >= 0) "+$b" else "$b"))
                                    }
                                }
                                if (action.damageDice.isNotBlank()) {
                                    val bonus = action.damageBonus?.let {
                                        FormulaEval.eval(
                                            it, character, owningClassId, summon.spellLevel,
                                        )
                                    } ?: 0
                                    add(
                                        action.damageDice +
                                            (if (bonus != 0) " + $bonus" else "") +
                                            (if (action.damageType.isNotBlank()) {
                                                " ${action.damageType}"
                                            } else "")
                                    )
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    DetailRow(
                                        title = action.name,
                                        supporting = numbers.joinToString(" · "),
                                        onClick = null,
                                    )
                                }
                                if (editMode) {
                                    IconButton(onClick = { onRemoveAction(action.name) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = trf(
                                                "Remove {0}", action.name,
                                            ),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            // A trait the rules ration — "Repair (3/Day)", "Healing Touch
                            // (1/Day)" — gets its own counter on the creature, because a
                            // summon's uses are its own and not the summoner's.
                            usesPerDay(action.name)?.let { perDay ->
                                UseCounter(
                                    spent = summon.spent[action.name] ?: 0,
                                    max = perDay,
                                    onSpend = { onSpend(action.name, 1) },
                                    onRestore = { onSpend(action.name, -1) },
                                )
                            }
                            EditableText(
                                value = action.description,
                                editMode = editMode,
                                onChange = {
                                    onSetField("action:${action.name}:description", it)
                                },
                                label = action.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                isOverridden = SummonEdits.isEdited(
                                    summon, "action:${action.name}:description",
                                ),
                                modifier = Modifier.padding(bottom = Space.inline),
                            )
                        }
                        if (editMode) {
                            OutlinedButton(
                                onClick = { adding = kind },
                                shape = Corner.row,
                            ) { Text(trf("Add a {0}", heading.trimEnd('s').lowercase())) }
                        }
                    }
                }
            }
        }

        item {
            Card(shape = Corner.card) {
                Column(Modifier.padding(Space.cardPadding)) {
                    Text(
                        text = tr("Notes"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = Space.inline),
                    )
                    EditableText(
                        value = summon.notes,
                        editMode = true,
                        onChange = { onNotes(it.orEmpty()) },
                        label = tr("Anything worth remembering about this one"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (editMode && summon.overrides.isNotEmpty()) {
            item {
                OutlinedButton(
                    onClick = { onSetField(RESET, null) },
                    shape = Corner.row,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(tr("Put this creature back the way the book has it")) }
            }
        }
    }

    adding?.let { kind ->
        CreatureActionDialog(
            kind = kind,
            existing = null,
            onDismiss = { adding = null },
            onSave = {
                onAddAction(it)
                adding = null
            },
        )
    }
}

/** The key that means "undo everything", rather than any field of the creature. */
const val RESET = "__reset__"

/**
 * A stat the player can correct, drawn the same way as one they cannot.
 *
 * The value shown and the value typed are not always the same string — an ability score reads
 * "14 (+2)" and is edited as "14" — so the editor takes the raw one and the stone keeps the
 * readable one. Putting that distinction here rather than at each call site is what stops a
 * dialog from offering to change a modifier the app works out for itself.
 */
@Composable
private fun EditableStone(
    label: String,
    value: String,
    editMode: Boolean,
    edited: Boolean,
    onChange: (String?) -> Unit,
    editValue: String = value,
) {
    if (!editMode) {
        StatStone(label, value)
        return
    }
    var editing by rememberSaveable { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { editing = true },
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (edited) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
    if (editing) {
        TextEditDialog(
            title = label,
            initial = editValue,
            canReset = edited,
            onDismiss = { editing = false },
            onConfirm = {
                onChange(it)
                editing = false
            },
        )
    }
}

/**
 * One labelled number, the same shape the character's own stats are drawn in.
 */
@Composable
private fun StatStone(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Hit points, with the one gesture that matters in play: take some, get some back.
 *
 * A creature on the table is damaged far more often than it is examined, so the buttons are
 * the first thing on it rather than something to find. Temporary hit points are set the same
 * way the character's are, and Edit Mode can correct the maximum for a creature whose hit
 * points were rolled or ruled at the table.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HitPointBar(
    current: Int,
    max: Int,
    temp: Int,
    editMode: Boolean,
    onDamage: (Int) -> Unit,
    onHeal: (Int) -> Unit,
    onSet: (Int, Int?) -> Unit,
    onSetMax: (Int) -> Unit,
) {
    var typed by rememberSaveable { mutableStateOf("") }
    val amount = typed.toIntOrNull() ?: 1

    Column(verticalArrangement = Arrangement.spacedBy(Space.tight)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tr("Hit Points"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = buildString {
                    append("$current / $max")
                    if (temp > 0) append(" (+$temp)")
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (current <= 0) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
            )
        }
        LinearProgressIndicator(
            progress = { if (max > 0) (current.toFloat() / max).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth(),
            color = if (current * 2 <= max) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
            value = typed,
            onValueChange = { entry -> typed = entry.filter { c -> c.isDigit() }.take(3) },
            label = { Text(tr("Amount")) },
            singleLine = true,
            shape = Corner.row,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.inline),
            verticalArrangement = Arrangement.spacedBy(Space.tight),
        ) {
            OutlinedButton(
                onClick = { onDamage(amount) },
                shape = Corner.row,
            ) { Text(tr("Damage")) }
            OutlinedButton(
                onClick = { onHeal(amount) },
                shape = Corner.row,
            ) { Text(tr("Heal")) }
            // Setting the number outright is for correcting a mistake, or for a creature
            // that arrived with fewer hit points than its stat block says.
            OutlinedButton(
                onClick = { typed.toIntOrNull()?.let { onSet(it, null) } },
                enabled = typed.isNotBlank(),
                shape = Corner.row,
            ) { Text(tr("Set")) }
            // Temporary hit points from a spell or a feature, tracked on the creature the
            // way they are tracked on the character.
            OutlinedButton(
                onClick = { typed.toIntOrNull()?.let { onSet(current, it) } },
                enabled = typed.isNotBlank(),
                shape = Corner.row,
            ) { Text(tr("Temp HP")) }
            if (editMode) {
                OutlinedButton(
                    onClick = { typed.toIntOrNull()?.let { onSetMax(it) } },
                    enabled = typed.isNotBlank(),
                    shape = Corner.row,
                ) { Text(tr("Set max")) }
            }
        }
    }
}


/** "(3/Day)" in an action's name, which is how the stat blocks ration a trait. */
private fun usesPerDay(name: String): Int? =
    Regex("""\((\d+)\s*/\s*Day\)""", RegexOption.IGNORE_CASE)
        .find(name)
        ?.groupValues
        ?.get(1)
        ?.toIntOrNull()

/**
 * A row of pips for a rationed trait, tapped to spend and tapped again to give back.
 *
 * The same gesture the character's own trackers use, so a summon is worked the way everything
 * else on the sheet is.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UseCounter(spent: Int, max: Int, onSpend: () -> Unit, onRestore: () -> Unit) {
    FlowRow(
        Modifier.padding(bottom = Space.inline),
        horizontalArrangement = Arrangement.spacedBy(Space.tight),
        verticalArrangement = Arrangement.spacedBy(Space.tight),
    ) {
        repeat(max) { index ->
            val used = index < spent
            androidx.compose.material3.FilterChip(
                selected = used,
                onClick = { if (used) onRestore() else onSpend() },
                label = { Text(if (used) tr("Used") else tr("Ready")) },
                shape = Corner.small,
            )
        }
    }
}
