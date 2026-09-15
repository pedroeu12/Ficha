package com.pedroeu.ficha.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.SummonEdits
import com.pedroeu.ficha.rules.ActionKind
import com.pedroeu.ficha.rules.CustomAction
import com.pedroeu.ficha.rules.CustomStatblock
import com.pedroeu.ficha.ui.design.Corner
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.i18n.trf

/** One field, the way every form in this file asks for one. */
@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    numeric: Boolean = false,
    lines: Int = 1,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = lines == 1,
        minLines = lines,
        shape = Corner.row,
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Writing something a creature can do.
 *
 * Numbers as text, deliberately. A summoned spirit's attack bonus is the summoner's, and the
 * app works that out; a creature invented at the table has whatever bonus the table said, and
 * asking the player to express "+7" as a formula would be asking them to learn the engine
 * before they can write down a wolf.
 */
@Composable
fun CreatureActionDialog(
    kind: ActionKind,
    existing: CustomAction?,
    onDismiss: () -> Unit,
    onSave: (CustomAction) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var toHit by remember { mutableStateOf(existing?.toHit.orEmpty()) }
    var damage by remember { mutableStateOf(existing?.damageDice.orEmpty()) }
    var damageType by remember { mutableStateOf(existing?.damageType.orEmpty()) }

    val heading = when (kind) {
        ActionKind.TRAIT -> tr("Trait")
        ActionKind.ACTION -> tr("Action")
        ActionKind.BONUS_ACTION -> tr("Bonus Action")
        ActionKind.REACTION -> tr("Reaction")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(trf("New {0}", heading)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Field(tr("Name"), name, { name = it })
                Field(tr("What it does"), description, { description = it }, lines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field(tr("To hit"), toHit, { toHit = it }, modifier = Modifier.weight(1f))
                    Field(tr("Damage"), damage, { damage = it }, modifier = Modifier.weight(1f))
                }
                Field(tr("Damage type"), damageType, { damageType = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        CustomAction(
                            name = name.trim(),
                            kind = kind.name,
                            description = description.trim(),
                            toHit = toHit.trim(),
                            damageDice = damage.trim(),
                            damageType = damageType.trim(),
                        )
                    )
                },
            ) { Text(tr("Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

/**
 * Writing a creature.
 *
 * The books cover the summoning spells. They do not cover the construct the DM built for this
 * campaign, the familiar from a third-party book, or the animal companion a table agreed on
 * between sessions — and until this existed, every one of those meant summoning the closest
 * printed thing and keeping the differences on paper beside the phone.
 *
 * Asked for in the order a stat block is read: what it is, what it takes to hurt it, what it
 * can do. Its actions are written one at a time through [CreatureActionDialog] rather than as
 * one blob of text, because an action with its own name and damage is a thing the sheet can
 * lay out, count uses of, and roll — and a paragraph is a paragraph.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatureDialog(
    existing: CustomStatblock?,
    onDismiss: () -> Unit,
    onSave: (CustomStatblock) -> Unit,
    onErase: ((CustomStatblock) -> Unit)? = null,
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var size by remember { mutableStateOf(existing?.size ?: "Medium") }
    var type by remember { mutableStateOf(existing?.creatureType.orEmpty()) }
    var ac by remember { mutableStateOf((existing?.armorClass ?: 12).toString()) }
    var hp by remember { mutableStateOf((existing?.hitPoints ?: 10).toString()) }
    var speed by remember { mutableStateOf(existing?.speed ?: "30 ft.") }
    var senses by remember { mutableStateOf(existing?.senses.orEmpty()) }
    var languages by remember { mutableStateOf(existing?.languages.orEmpty()) }
    var resistances by remember { mutableStateOf(existing?.resistances.orEmpty()) }
    var immunities by remember { mutableStateOf(existing?.immunities.orEmpty()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var scores by remember {
        mutableStateOf(
            Ability.ALL.associate { it.name to (existing?.abilityScores?.get(it.name) ?: 10) }
        )
    }
    var actions by remember { mutableStateOf(existing?.actions.orEmpty()) }
    var writingAction by remember { mutableStateOf<ActionKind?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) tr("Write a creature") else tr("Edit creature")) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Field(tr("Name"), name, { name = it })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field(tr("Size"), size, { size = it }, modifier = Modifier.weight(1f))
                    Field(tr("Type"), type, { type = it }, modifier = Modifier.weight(1.4f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field(tr("AC"), ac, { ac = it }, numeric = true, modifier = Modifier.weight(1f))
                    Field(
                        tr("Hit Points"), hp, { hp = it }, numeric = true,
                        modifier = Modifier.weight(1f),
                    )
                    Field(tr("Speed"), speed, { speed = it }, modifier = Modifier.weight(1.3f))
                }

                Text(
                    text = tr("Ability Scores"),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Ability.ALL.forEach { ability ->
                        OutlinedTextField(
                            value = (scores[ability.name] ?: 10).toString(),
                            onValueChange = { typed ->
                                scores = scores + (ability.name to (typed.toIntOrNull() ?: 10))
                            },
                            label = { Text(ability.abbreviation) },
                            singleLine = true,
                            shape = Corner.row,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.heightIn(min = 56.dp).fillMaxWidth(0.3f),
                        )
                    }
                }

                Field(tr("Senses"), senses, { senses = it })
                Field(tr("Languages"), languages, { languages = it })
                Field(tr("Resistances"), resistances, { resistances = it })
                Field(tr("Immunities"), immunities, { immunities = it })
                Field(tr("Notes"), notes, { notes = it }, lines = 2)

                Text(
                    text = trf("What it can do ({0})", actions.size),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
                actions.forEach { action ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            text = "· ${action.name}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { actions = actions - action }) {
                            Text(tr("Remove"))
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionKind.entries.forEach { kind ->
                        TextButton(onClick = { writingAction = kind }) {
                            Text(
                                when (kind) {
                                    ActionKind.TRAIT -> tr("+ Trait")
                                    ActionKind.ACTION -> tr("+ Action")
                                    ActionKind.BONUS_ACTION -> tr("+ Bonus Action")
                                    ActionKind.REACTION -> tr("+ Reaction")
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        CustomStatblock(
                            id = existing?.id ?: SummonEdits.newId(),
                            name = name.trim(),
                            size = size.trim().ifBlank { "Medium" },
                            creatureType = type.trim(),
                            armorClass = ac.toIntOrNull() ?: 12,
                            hitPoints = hp.toIntOrNull()?.coerceAtLeast(1) ?: 10,
                            speed = speed.trim(),
                            abilityScores = scores,
                            actions = actions,
                            senses = senses.trim(),
                            languages = languages.trim(),
                            resistances = resistances.trim(),
                            immunities = immunities.trim(),
                            notes = notes.trim(),
                        )
                    )
                },
            ) { Text(tr("Save")) }
        },
        dismissButton = {
            Row {
                if (existing != null && onErase != null) {
                    TextButton(onClick = { onErase(existing) }) { Text(tr("Delete")) }
                }
                TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
            }
        },
    )

    writingAction?.let { kind ->
        CreatureActionDialog(
            kind = kind,
            existing = null,
            onDismiss = { writingAction = null },
            onSave = {
                actions = actions.filterNot { a -> a.name == it.name } + it
                writingAction = null
            },
        )
    }
}
