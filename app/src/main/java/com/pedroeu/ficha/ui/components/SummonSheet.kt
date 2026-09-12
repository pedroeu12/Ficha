package com.pedroeu.ficha.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import com.pedroeu.ficha.rules.ActionKind
import com.pedroeu.ficha.rules.ActiveSummon
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
    modifier: Modifier = Modifier,
) {
    val statblock = CharacterSummons.statblockOf(summon)
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
                            Text(
                                text = "${statblock.size} ${statblock.creatureType}" +
                                    if (summon.sourceLabel.isNotBlank()) {
                                        " · ${summon.sourceLabel}"
                                    } else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        onDamage = onDamage,
                        onHeal = onHeal,
                        onSet = onSetHitPoints,
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatStone(
                            tr("AC"),
                            statblock.armorClassFor(character, summon.spellLevel, owningClassId)
                                .toString(),
                        )
                        StatStone(tr("Speed"), statblock.speed)
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
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Ability.ALL.forEach { ability ->
                            val score = statblock.abilityScores[ability] ?: 10
                            val mod = statblock.modifier(ability)
                            StatStone(
                                ability.abbreviation,
                                "$score (${if (mod >= 0) "+$mod" else "$mod"})",
                            )
                        }
                    }
                }
            }
        }

        val defences = listOfNotNull(
            statblock.resistances.takeIf { it.isNotEmpty() }
                ?.let { tr("Resistances") to it.joinToString(", ") },
            statblock.immunities.takeIf { it.isNotEmpty() }
                ?.let { tr("Immunities") to it.joinToString(", ") },
            statblock.conditionImmunities.takeIf { it.isNotEmpty() }
                ?.let { tr("Condition Immunities") to it.joinToString(", ") },
            statblock.senses.takeIf { it.isNotBlank() }?.let { tr("Senses") to it },
            statblock.languages.takeIf { it.isNotBlank() }?.let { tr("Languages") to it },
        )
        if (defences.isNotEmpty()) {
            item {
                Card(shape = Corner.card) {
                    Column(
                        Modifier.padding(Space.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(Space.tight),
                    ) {
                        defences.forEach { (label, value) ->
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    text = "$label: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                Text(value, style = MaterialTheme.typography.bodySmall)
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
            if (actions.isEmpty()) return@forEach
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
                            DetailRow(
                                title = action.name,
                                supporting = numbers.joinToString(" · "),
                                onClick = null,
                            )
                            Text(
                                text = action.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = Space.inline),
                            )
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
 * the first thing on it rather than something to find.
 */
@Composable
private fun HitPointBar(
    current: Int,
    max: Int,
    temp: Int,
    onDamage: (Int) -> Unit,
    onHeal: (Int) -> Unit,
    onSet: (Int, Int?) -> Unit,
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
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.inline),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = typed,
                onValueChange = { entry -> typed = entry.filter { c -> c.isDigit() }.take(3) },
                label = { Text(tr("Amount")) },
                singleLine = true,
                shape = Corner.row,
                modifier = Modifier.weight(1f),
            )
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
        }
    }
}
