package com.pedroeu.ficha.ui.sheet

import com.pedroeu.ficha.ui.i18n.trf
import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Recharge
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.RestEngine
import com.pedroeu.ficha.domain.RestOutcome
import com.pedroeu.ficha.ui.components.ChoiceSection
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
    var rolls by remember { mutableStateOf<List<Int>>(emptyList()) }
    var outcome by remember { mutableStateOf<RestOutcome?>(null) }

    val hitDie = CharacterCalculations.hitDie(character)
    val conMod = CharacterCalculations.abilityModifiers(character)[Ability.CON] ?: 0
    val available = RestEngine.availableHitDice(character) - rolls.size
    val projectedHealing = rolls.sumOf { (it + conMod).coerceAtLeast(1) }

    val restChangeable = ChoiceResolver.restChangeable(character)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
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
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(tr("Done")) }
                return@Column
            }

            if (kind == RestKind.SHORT) {
                RestCard {
                    SectionHeader(
                        tr("Hit Dice"),
                        trailing = "${available} of ${character.level} left",
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
                            text = rolls.joinToString(" + ") { "$it${
                                if (conMod >= 0) "+$conMod" else "$conMod"
                            }" } + "  =  $projectedHealing hit points",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { rolls = rolls + viewModel.rollHitDie() },
                            enabled = available > 0,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = null)
                            Text(trf("  Roll d{0}", hitDie))
                        }
                        OutlinedButton(
                            onClick = { rolls = rolls + (hitDie / 2 + 1) },
                            enabled = available > 0,
                            modifier = Modifier.weight(1f),
                        ) { Text(tr("Average")) }
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
                val preparable = CharacterSpells.preparable(character)
                if (preparable.isNotEmpty()) {
                    val maxPrepared = CharacterCalculations.maxPreparedSpells(character)
                    val preparedNow = preparable.count { it.prepared }
                    Text(
                        text = trf("Prepared spells  {0} / {1}", preparedNow, maxPrepared),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (preparedNow > maxPrepared) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = tr("Tap to swap which spells you have ready for the day."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.heightIn(max = 240.dp),
                    ) {
                        items(preparable.size, key = { preparable[it].id }) { index ->
                            val spell = preparable[index]
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleSpellPrepared(spell.id) }
                                    .padding(vertical = 4.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = spell.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = trf("Level {0}", spell.level),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                                Text(
                                    text = if (spell.prepared) tr("Prepared") else tr("Not prepared"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (spell.prepared) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (restChangeable.isNotEmpty()) {
                Text(
                    text = tr("You may change these while you rest"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 280.dp),
                ) {
                    items(restChangeable.size, key = { restChangeable[it].choice.id }) { index ->
                        val resolved = restChangeable[index]
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

            Button(
                onClick = {
                    outcome = if (kind == RestKind.SHORT) {
                        viewModel.shortRest(rolls)
                    } else {
                        viewModel.longRestDetailed()
                    }
                },
                shape = RoundedCornerShape(10.dp),
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
        shape = RoundedCornerShape(14.dp),
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
