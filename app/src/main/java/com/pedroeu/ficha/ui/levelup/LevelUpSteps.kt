package com.pedroeu.ficha.ui.levelup

import com.pedroeu.ficha.ui.i18n.trf
import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.ui.components.ChoiceChip
import com.pedroeu.ficha.domain.OwnedOptions
import com.pedroeu.ficha.ui.components.ChoiceSection
import com.pedroeu.ficha.ui.components.SectionHeader
import com.pedroeu.ficha.ui.components.SelectableCard

@Composable
private fun StepColumn(content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) { content() }
    }
}

// ---------------------------------------------------------------------- Hit points

@Composable
fun HitPointsStep(state: LevelUpState, viewModel: LevelUpViewModel) {
    val conMod = CharacterCalculations.abilityModifiers(state.character)[Ability.CON] ?: 0

    StepColumn {
        item {
            InfoCard {
                SectionHeader(tr("Hit Points"), trailing = "d${state.hitDie}")
                Text(
                    text = trf(
                        "Reaching level {0} adds a Hit Die roll plus your Constitution " +
                            "modifier ({1}) to your maximum hit points.",
                        state.targetLevel,
                        CharacterCalculations.formatModifier(conMod),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = trf("+{0} max HP", state.hitPointsGained + conMod),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        items(HitPointMethod.entries.size, key = { HitPointMethod.entries[it].name }) { index ->
            val method = HitPointMethod.entries[index]
            SelectableCard(
                title = method.label,
                subtitle = when (method) {
                    HitPointMethod.AVERAGE ->
                        "Take ${state.averageHitPoints}, the fixed average for a d${state.hitDie}."
                    HitPointMethod.ROLL ->
                        "Roll a d${state.hitDie} and live with the result."
                    HitPointMethod.MANUAL ->
                        tr("Enter a number yourself, for tables with their own house rule.")
                },
                selected = state.hitPointMethod == method,
                onClick = { viewModel.setHitPointMethod(method) },
            )
        }

        if (state.hitPointMethod == HitPointMethod.ROLL) {
            item {
                InfoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = state.rolledHitPoints?.toString() ?: "—",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Text(
                                text = if (state.rolledHitPoints == null) {
                                    tr("Not rolled yet")
                                } else {
                                    "Rolled on a d${state.hitDie}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(onClick = viewModel::rollHitPoints) {
                            Icon(Icons.Default.Casino, contentDescription = null)
                            Text(tr("  Roll"), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        if (state.hitPointMethod == HitPointMethod.MANUAL) {
            item {
                InfoCard {
                    OutlinedTextField(
                        value = state.manualHitPoints.toString(),
                        onValueChange = { text ->
                            viewModel.setManualHitPoints(
                                text.filter { it.isDigit() }.take(2).toIntOrNull() ?: 1
                            )
                        },
                        label = { Text(trf("Hit points rolled (1-{0})", state.hitDie)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------- Subclass

@Composable
fun SubclassStep(state: LevelUpState, viewModel: LevelUpViewModel) {
    val options = state.subclassOptions

    StepColumn {
        item {
            Text(
                text = trf(
                    "Your {0} shapes the rest of your career. This choice is permanent.",
                    state.progression?.subclassLabel ?: tr("subclass"),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        items(options.size, key = { options[it].id }) { index ->
            val subclass = options[index]
            SelectableCard(
                title = subclass.name,
                subtitle = subclass.summary,
                selected = state.activeSubclassId == subclass.id,
                onClick = { viewModel.selectSubclass(subclass.id) },
                trailingLabel = if (subclass.isPlaytest) "Playtest • ${subclass.source}" else null,
                expandedContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        subclass.features.forEach { feature ->
                            Column {
                                Text(
                                    text = "Level ${feature.level} — ${feature.name}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                Text(
                                    text = feature.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}

// ---------------------------------------------------------------------- Features

@Composable
fun FeaturesStep(state: LevelUpState, viewModel: LevelUpViewModel) {
    val classFeatures = state.newClassFeatures
    val subclassFeatures = state.newSubclassFeatures
    val choices = state.featureChoices

    StepColumn {
        if (classFeatures.isNotEmpty()) {
            item {
                InfoCard {
                    SectionHeader(tr("New Class Features"))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        classFeatures.forEach { feature ->
                            FeatureRow(feature.name, feature.description)
                        }
                    }
                }
            }
        }

        if (subclassFeatures.isNotEmpty()) {
            item {
                InfoCard {
                    SectionHeader(tr("New Subclass Features"))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        subclassFeatures.forEach { feature ->
                            FeatureRow(feature.name, feature.description)
                        }
                    }
                }
            }
        }

        items(choices.size, key = { choices[it].id }) { index ->
            val choice = choices[index]
            // Everything the character already has is greyed out, so levelling up can't
            // hand them a second copy of a proficiency, spell, feat, or feature option.
            val disabled = OwnedOptions.disabledFor(
                choice = choice,
                owned = OwnedOptions.of(state.character),
                currentSelection = state.selections[choice.id].orEmpty().toSet(),
            )

            ChoiceSection(
                choice = choice,
                selected = state.selections[choice.id].orEmpty(),
                onToggle = { viewModel.toggleSelection(choice.id, it, choice.count) },
                disabledOptionIds = disabled,
            )
        }
    }
}

/**
 * What the feat taken this level still wants decided. A feat is not a flat grant: most raise
 * an ability score the player picks, and several name a spell, a skill, or a damage type
 * on top of that. Those prompts appear here rather than being quietly skipped.
 */
@Composable
fun FeatChoicesStep(state: LevelUpState, viewModel: LevelUpViewModel) {
    val feat = state.featId?.let { FeatData.byId(it) }
    val choices = state.featChoices

    StepColumn {
        if (feat != null) {
            item {
                InfoCard {
                    SectionHeader(feat.name)
                    Text(
                        feat.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        items(choices.size, key = { choices[it].id }) { index ->
            val choice = choices[index]
            val disabled = OwnedOptions.disabledFor(
                choice = choice,
                owned = OwnedOptions.of(state.character),
                currentSelection = state.selections[choice.id].orEmpty().toSet(),
            )

            ChoiceSection(
                choice = choice,
                selected = state.selections[choice.id].orEmpty(),
                onToggle = { viewModel.toggleSelection(choice.id, it, choice.count) },
                disabledOptionIds = disabled,
            )
        }
    }
}

@Composable
private fun FeatureRow(name: String, description: String) {
    Column {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------- ASI / feat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AbilityImprovementStep(state: LevelUpState, viewModel: LevelUpViewModel) {
    val scores = CharacterCalculations.finalAbilityScores(state.character)

    StepColumn {
        if (state.grantsEpicBoon) {
            item {
                Text(
                    text = trf("Level {0} grants an Epic Boon feat.", state.targetLevel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item {
                InfoCard {
                    SectionHeader(tr("How to spend it"))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        AsiMode.entries.forEach { mode ->
                            SelectableCard(
                                title = mode.label,
                                subtitle = when (mode) {
                                    AsiMode.PLUS_TWO -> tr("Raise a single ability by 2, up to a maximum of 20.")
                                    AsiMode.PLUS_ONE_ONE -> tr("Raise two different abilities by 1 each.")
                                    AsiMode.FEAT -> tr("Skip the increase and take a feat instead.")
                                },
                                selected = state.asiMode == mode,
                                onClick = { viewModel.setAsiMode(mode) },
                            )
                        }
                    }
                }
            }

            if (state.asiMode != AsiMode.FEAT) {
                item {
                    InfoCard {
                        SectionHeader(
                            tr("Ability Scores"),
                            trailing = "${state.asiPoints.values.sum()} / 2 assigned",
                        )
                        Text(
                            text = tr("Tap an ability to assign points; tap it again to take them back."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Ability.ALL.forEach { ability ->
                                val added = state.asiPoints[ability] ?: 0
                                val current = scores[ability] ?: 10
                                val atCap = current + added >= 20 && added == 0
                                ChoiceChip(
                                    label = ability.abbreviation,
                                    supporting = if (added > 0) {
                                        "$current → ${current + added}"
                                    } else {
                                        "$current"
                                    },
                                    selected = added > 0,
                                    enabled = !atCap,
                                    onClick = { viewModel.toggleAsiAbility(ability) },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (state.grantsEpicBoon || state.asiMode == AsiMode.FEAT) {
            val feats = state.featOptions
            item {
                SectionHeader(
                    if (state.grantsEpicBoon) tr("Epic Boons") else tr("Feats"),
                    trailing = if (state.featId == null) tr("Choose 1") else null,
                )
            }
            val blockers = state.featBlockers
            items(feats.size, key = { feats[it].id }) { index ->
                val feat = feats[index]
                // A feat whose prerequisites aren't met stays visible with the reason, so
                // the path it belongs to is legible before you're eligible for it.
                val blocked = blockers[feat.id].orEmpty()
                SelectableCard(
                    title = feat.name,
                    subtitle = feat.description,
                    selected = state.featId == feat.id,
                    enabled = blocked.isEmpty(),
                    onClick = { viewModel.selectFeat(feat.id) },
                    trailingLabel = blocked,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------- Spells

@Composable
fun NewSpellsStep(state: LevelUpState, viewModel: LevelUpViewModel) {
    var manualEntry by remember { mutableStateOf("") }

    StepColumn {
        // The trade comes first: what you give up decides how many places there are to fill,
        // and reading it the other way round means picking spells you then have to unpick.
        if (state.canReplaceCantrip) {
            item {
                ReplacementHeader(
                    title = tr("Trade a Cantrip"),
                    explanation = tr(
                        "Gaining a level lets you swap one cantrip for another. Pick the one " +
                            "to give up, or leave this alone to keep them all."
                    ),
                    chosen = state.replacedCantripId,
                    onClear = { state.replacedCantripId?.let(viewModel::toggleReplacedCantrip) },
                )
            }
            val replaceable = state.replaceableCantrips
            items(replaceable.size, key = { "drop-cantrip-${replaceable[it].id}" }) { index ->
                val spell = replaceable[index]
                SelectableCard(
                    title = spell.name,
                    subtitle = spell.description,
                    selected = state.replacedCantripId == spell.id,
                    onClick = { viewModel.toggleReplacedCantrip(spell.id) },
                    trailingLabel = tr("Give up"),
                )
            }
        }

        if (state.canReplaceSpell) {
            item {
                ReplacementHeader(
                    title = tr("Trade a Spell"),
                    explanation = tr(
                        "Gaining a level lets you replace one spell you know with another " +
                            "from your class list. Pick the one to give up, or leave this " +
                            "alone to keep them all."
                    ),
                    chosen = state.replacedSpellId,
                    onClear = { state.replacedSpellId?.let(viewModel::toggleReplacedSpell) },
                )
            }
            val replaceable = state.replaceableSpells
            items(replaceable.size, key = { "drop-spell-${replaceable[it].id}" }) { index ->
                val spell = replaceable[index]
                SelectableCard(
                    title = spell.name,
                    subtitle = spell.description,
                    selected = state.replacedSpellId == spell.id,
                    onClick = { viewModel.toggleReplacedSpell(spell.id) },
                    trailingLabel = trf("Level {0}", spell.level),
                )
            }
        }

        if (state.cantripsWanted > 0) {
            item {
                SectionHeader(
                    tr("New Cantrips"),
                    trailing = "${state.newCantrips.size} / ${state.cantripsWanted}",
                )
            }
            val cantrips = state.cantripOptions
            items(cantrips.size, key = { "cantrip-${cantrips[it].id}" }) { index ->
                val spell = cantrips[index]
                SelectableCard(
                    title = spell.name,
                    subtitle = spell.description,
                    selected = state.newCantrips.contains(spell.id),
                    onClick = { viewModel.toggleCantrip(spell.id) },
                    trailingLabel = spell.subtitle,
                )
            }
        }

        if (state.spellsWanted > 0) {
            item {
                SectionHeader(
                    tr("New Spells"),
                    trailing = "${state.newSpells.size + state.manualSpells.size} / ${state.spellsWanted}",
                )
            }

            if (state.manualSpells.isNotEmpty()) {
                item {
                    InfoCard {
                        SectionHeader(tr("Added by hand"))
                        Column(modifier = Modifier.padding(top = 6.dp)) {
                            state.manualSpells.forEach { name ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(onClick = { viewModel.removeManualSpell(name) }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove $name",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // The catalog stops at level 5, so higher-level casters get a way in by hand.
            if (state.needsManualSpellEntry) {
                item {
                    InfoCard {
                        SectionHeader(tr("Add a spell not listed"))
                        Text(
                            text = trf(
                                "You can cast up to level {0}. Spells above level 5 aren't in " +
                                    "the app's catalog yet, so type the name to add it.",
                                state.maxSpellLevel,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = manualEntry,
                                onValueChange = { manualEntry = it },
                                label = { Text(tr("Spell name")) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = {
                                    viewModel.addManualSpell(manualEntry)
                                    manualEntry = ""
                                },
                                enabled = manualEntry.isNotBlank(),
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = tr("Add spell"),
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                }
            }

            val spells = state.spellOptions
            items(spells.size, key = { "spell-${spells[it].id}" }) { index ->
                val spell = spells[index]
                SelectableCard(
                    title = spell.name,
                    subtitle = spell.description,
                    selected = state.newSpells.contains(spell.id),
                    onClick = { viewModel.toggleSpell(spell.id) },
                    trailingLabel = spell.subtitle,
                )
            }
        }
    }
}

/**
 * The heading over an optional trade, with a way out of one already made.
 *
 * Optional is the whole point: the rules offer the swap, they never require it, so the step
 * has to read as an invitation rather than a question that must be answered before you can
 * move on.
 */
@Composable
private fun ReplacementHeader(
    title: String,
    explanation: String,
    chosen: String?,
    onClear: () -> Unit,
) {
    Column {
        SectionHeader(title, trailing = if (chosen == null) tr("Optional") else tr("1 traded"))
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (chosen != null) {
            TextButton(onClick = onClear) { Text(tr("Keep it after all")) }
        }
    }
}

// ---------------------------------------------------------------------- Summary

@Composable
fun SummaryStep(state: LevelUpState) {
    val before = state.character
    val after = state.leveledCharacter

    StepColumn {
        item {
            InfoCard {
                SectionHeader(trf("Level {0}", state.targetLevel))
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    SummaryRow(
                        tr("Hit points"),
                        "+${state.hitPointsGained + (CharacterCalculations.abilityModifiers(before)[Ability.CON] ?: 0)} max",
                    )
                    SummaryRow(
                        tr("Proficiency bonus"),
                        CharacterCalculations.formatModifier(
                            CharacterCalculations.proficiencyBonus(state.targetLevel)
                        ),
                    )
                    if (state.gainsSubclass) {
                        val name = state.subclassOptions
                            .find { it.id == state.activeSubclassId }?.name.orEmpty()
                        SummaryRow(tr("Subclass"), name)
                    }
                    if (state.asiPoints.isNotEmpty()) {
                        state.asiPoints.forEach { (ability, points) ->
                            SummaryRow(ability.fullName, "+$points")
                        }
                    }
                    state.featId?.let { id ->
                        SummaryRow(tr("Feat"), FeatData.byId(id)?.name.orEmpty())
                    }
                    val slots = CharacterCalculations.spellSlots(after)
                    if (slots.isNotEmpty()) {
                        SummaryRow(
                            tr("Spell slots"),
                            slots.toSortedMap().entries.joinToString(", ") { (level, count) ->
                                "L$level ×$count"
                            },
                        )
                    }
                }
            }
        }

        val gainedFeatures = state.newClassFeatures.map { it.name to it.description } +
            state.newSubclassFeatures.map { it.name to it.description }
        if (gainedFeatures.isNotEmpty()) {
            item {
                InfoCard {
                    SectionHeader(tr("Features gained"))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        gainedFeatures.forEach { (name, description) ->
                            FeatureRow(name, description)
                        }
                    }
                }
            }
        }

        val learned = state.newCantrips + state.newSpells
        if (learned.isNotEmpty() || state.manualSpells.isNotEmpty()) {
            item {
                InfoCard {
                    SectionHeader(tr("Spells learned"))
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        learned.forEach { id ->
                            SpellData.byId(id)?.let { spell ->
                                Text(
                                    text = "• ${spell.name} (${spell.levelLabel})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        state.manualSpells.forEach { name ->
                            Text(
                                text = "• $name",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

/**
 * Which class this level goes into.
 *
 * Levelling the class you already have is the common case, so it leads. A class the
 * character's scores can't support is shown greyed out with the score it needs, rather than
 * hidden — knowing why a class isn't available is more useful than it quietly missing.
 */
@Composable
fun ChooseClassStep(state: LevelUpState, viewModel: LevelUpViewModel) {
    val options = state.multiclassOptions

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = trf(
                    "You are {0}. Continue in one of those, or start a new class if your " +
                        "scores allow it.",
                    ClassLevels.label(state.character),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val (existing, fresh) = options.partition { it.alreadyHas }

        item {
            Text(
                text = tr("Classes you already have"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        items(existing.size, key = { "have:${existing[it].classId}" }) { index ->
            val option = existing[index]
            val current = ClassLevels.levelIn(state.character, option.classId)
            SelectableCard(
                title = "${option.className} $current → ${current + 1}",
                subtitle = option.entry.note,
                selected = state.classId == option.classId,
                onClick = { viewModel.selectLevellingClass(option.classId) },
            )
        }

        if (fresh.isNotEmpty()) {
            item {
                Text(
                    text = tr("Start a new class"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(fresh.size, key = { "new:${fresh[it].classId}" }) { index ->
                val option = fresh[index]
                val gained = buildList {
                    if (option.entry.armorTraining.isNotEmpty()) {
                        add("Armor: ${option.entry.armorTraining.joinToString(", ")}")
                    }
                    if (option.entry.weaponProficiencies.isNotEmpty()) {
                        add("Weapons: ${option.entry.weaponProficiencies.joinToString(", ")}")
                    }
                    if (option.entry.toolProficiencies.isNotEmpty()) {
                        add("Tools: ${option.entry.toolProficiencies.joinToString(", ")}")
                    }
                    if (option.entry.skillCount > 0) {
                        add(tr("One skill of your choice"))
                    }
                    if (option.entry.note.isNotBlank()) add(option.entry.note)
                }
                SelectableCard(
                    title = "${option.className} 1",
                    subtitle = if (option.allowed) {
                        "Requires ${option.entry.prerequisiteLabel}. You gain: " +
                            gained.joinToString(" • ").ifBlank { tr("no extra proficiencies") }
                    } else {
                        option.reason
                    },
                    selected = state.classId == option.classId,
                    enabled = option.allowed,
                    onClick = { viewModel.selectLevellingClass(option.classId) },
                )
            }
        }
    }
}
