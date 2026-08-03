package com.pedroeu.ficha.ui.tablet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.ResolvedChoice
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.i18n.trf
import com.pedroeu.ficha.ui.sheet.ArtificerItemsCard

/**
 * Everything the character can do that isn't an attack or a spell.
 *
 * The hardest part of the sheet to make pleasant: a level 12 character has forty features and
 * every one of them is a paragraph. On paper you get a box and small print; here each is a
 * line that opens when you want it, so the page reads as a table of contents until you ask it
 * a question. Two columns, split by where the feature came from — class on the left, everything
 * you brought with you on the right.
 */
@Composable
fun LeafFeatures(handle: SheetHandle) {
    val character = handle.character
    val charClass = ClassData.byId(character.classId)
    val progression = ProgressionData.forClass(character.classId)
    val subclass = character.subclassId?.let { SubclassData.byId(it) }
    val species = SpeciesData.byId(character.speciesId)

    val classChoices = ChoiceResolver.classFeatureChoices(character).groupBy { it.featureName }
    val subclassChoices = ChoiceResolver.subclassFeatureChoices(character).groupBy { it.featureName }
    val originChoices = ChoiceResolver.originChoices(character)

    Row(Modifier.fillMaxWidth()) {
        LazyColumn(
            Modifier.weight(0.5f),
            contentPadding = PaddingValues(start = 20.dp, end = 10.dp, top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (charClass != null) {
                item {
                    Leaf(trf("Class Features — {0}", charClass.name)) {
                        charClass.level1Features.forEach { feature ->
                            FeatureLine(
                                name = feature.name,
                                body = feature.description,
                                choices = classChoices[feature.name].orEmpty(),
                                handle = handle,
                            )
                        }
                        ChoiceResolver.levelOneClassOptions(character).forEach { (label, option) ->
                            FeatureLine("$label: $option", "", emptyList(), handle)
                        }
                        progression?.features
                            ?.filter { it.level in 2..character.level }
                            ?.sortedBy { it.level }
                            ?.forEach { feature ->
                                FeatureLine(
                                    name = trf("Level {0} — {1}", feature.level, feature.name),
                                    body = feature.description,
                                    choices = classChoices[feature.name].orEmpty(),
                                    handle = handle,
                                )
                            }
                    }
                }
            }

            if (subclass != null) {
                item {
                    Leaf(trf("Subclass — {0}", subclass.name)) {
                        SheetText(subclass.summary, soft = true)
                        if (subclass.isPlaytest) {
                            Caption(trf("Playtest material — {0}", subclass.source))
                        }
                        Spacer(Modifier.height(4.dp))
                        subclass.features
                            .filter { it.level <= character.level }
                            .sortedBy { it.level }
                            .forEach { feature ->
                                FeatureLine(
                                    name = trf("Level {0} — {1}", feature.level, feature.name),
                                    body = feature.description,
                                    choices = subclassChoices[feature.name].orEmpty(),
                                    handle = handle,
                                )
                            }
                    }
                }
            }

            item { ArtificerItemsCard(character, handle.viewModel, framed = false) }
        }

        Fold()

        LazyColumn(
            Modifier.weight(0.5f),
            contentPadding = PaddingValues(start = 10.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (species != null) {
                item {
                    Leaf(trf("Species Traits — {0}", species.name)) {
                        FeatureLine(
                            name = tr("Size & Speed"),
                            body = "${species.size}, ${species.speed} feet of movement." +
                                if (species.darkvisionRange > 0) {
                                    " Darkvision out to ${species.darkvisionRange} feet."
                                } else "",
                            choices = emptyList(),
                            handle = handle,
                        )
                        species.traits.forEach { trait ->
                            FeatureLine(trait.name, trait.description, emptyList(), handle)
                        }
                    }
                }
            }

            item {
                Leaf(
                    title = tr("Feats"),
                    trailing = { PenMark(tr("Add feat")) { handle.open(SheetOverlay.AddFeat) } },
                ) {
                    if (character.featIds.isEmpty()) {
                        SheetText(tr("No feats yet."), soft = true)
                    }
                    character.featIds.forEach { featId ->
                        val feat = FeatData.byId(featId)
                        FeatureLine(
                            name = feat?.name ?: featId,
                            body = feat?.description.orEmpty(),
                            choices = emptyList(),
                            handle = handle,
                            onRemove = if (handle.editMode) {
                                { handle.viewModel.removeFeat(featId) }
                            } else null,
                        )
                    }
                }
            }

            if (originChoices.isNotEmpty()) {
                item {
                    Leaf(tr("Origin Choices")) {
                        SheetText(
                            tr("What you picked from your species, class, background, and feats."),
                            soft = true,
                        )
                        originChoices.forEach { resolved ->
                            ChoiceLine(resolved, handle)
                        }
                    }
                }
            }

            item { ProficienciesBlock(handle) }
            item { ToolsBlock(handle) }

            item {
                Leaf(
                    title = tr("Other Features"),
                    trailing = { PenMark(tr("Add feature")) { handle.open(SheetOverlay.AddFeature) } },
                ) {
                    if (character.customFeatures.isEmpty()) {
                        SheetText(
                            tr("Anything the app doesn't already know about goes here."),
                            soft = true,
                        )
                    }
                    character.customFeatures.forEach { feature ->
                        FeatureLine(
                            name = feature.name,
                            body = feature.description,
                            choices = emptyList(),
                            handle = handle,
                            onRemove = if (handle.editMode) {
                                { handle.viewModel.removeCustomFeature(feature.id) }
                            } else null,
                        )
                    }
                }
            }
        }
    }
}

/**
 * One feature: a line you can open.
 *
 * Closed by default, because a page of open paragraphs is the thing that makes a long sheet
 * unreadable. The name carries the weight; the rules text arrives when asked for.
 */
@Composable
private fun FeatureLine(
    name: String,
    body: String,
    choices: List<ResolvedChoice>,
    handle: SheetHandle,
    onRemove: (() -> Unit)? = null,
    /** The scope its text override is filed under, matching the phone's keys exactly. */
    scope: String = "feature",
) {
    val v = LocalVellum.current
    var open by remember(name) { mutableStateOf(false) }

    // A table that has rewritten a feature sees its own words, here and on the phone: the
    // override is the same key either way, so editing on one shows up on the other.
    val nameKey = "$scope:$name:name"
    val bodyKey = "$scope:$name:description"
    val shownName = handle.character.textOverrides[nameKey] ?: name
    val shownBody = handle.character.textOverrides[bodyKey] ?: body
    val hasBody = shownBody.isNotBlank()

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .let { if (hasBody) it.clickable { open = !open } else it }
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (hasBody) (if (open) "▾  " else "▸  ") + shownName else "    $shownName",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = v.ink,
                modifier = Modifier.weight(1f),
            )
            if (handle.editMode) {
                PenMark(tr("Feature text"), glyph = "✎") {
                    handle.editText(bodyKey, shownName, shownBody)
                }
            }
            if (onRemove != null) PenMark(tr("Delete"), glyph = "×", onClick = onRemove)
        }

        AnimatedVisibility(visible = open) {
            Column(Modifier.padding(start = 18.dp, bottom = 8.dp)) {
                SheetText(
                    shownBody,
                    soft = handle.character.textOverrides[bodyKey] == null,
                )
            }
        }

        choices.forEach { resolved -> ChoiceLine(resolved, handle) }
    }
}

/** What was picked inside a feature, and the mark that changes it. */
@Composable
private fun ChoiceLine(resolved: ResolvedChoice, handle: SheetHandle) {
    val v = LocalVellum.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Caption(resolved.choice.label, modifier = Modifier.padding(end = 8.dp))
        Text(
            text = resolved.summary.ifBlank { tr("not chosen yet") },
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
            color = if (resolved.isAnswered) v.inkSoft else v.danger,
            modifier = Modifier.weight(1f),
        )
        if (handle.editMode || !resolved.isAnswered) {
            PenMark(
                description = if (resolved.isAnswered) tr("Change") else tr("Choose"),
                glyph = "✎",
            ) { handle.open(SheetOverlay.EditChoice(resolved)) }
        }
    }
}

/** Armour, weapons, tools, languages — what the character is trained with. */
@Composable
private fun ProficienciesBlock(handle: SheetHandle) {
    val character = handle.character
    val charClass = ClassData.byId(character.classId)

    Leaf(tr("Equipment Training & Proficiencies")) {
        WrittenLine(
            label = tr("Armor Training"),
            key = "combat:armor_training",
            fallback = character.armorTraining
                .ifEmpty { charClass?.armorProficiencies.orEmpty() }
                .joinToString(", ").ifBlank { tr("None") },
            handle = handle,
        )
        WrittenLine(
            label = tr("Weapons"),
            key = "combat:weapons",
            fallback = character.weaponProficiencies
                .ifEmpty { charClass?.weaponProficiencies.orEmpty() }
                .joinToString(", ").ifBlank { tr("None") },
            handle = handle,
        )
        WrittenLine(
            label = tr("Languages"),
            key = "combat:languages",
            fallback = character.languages.joinToString(", ").ifBlank { tr("None") },
            handle = handle,
        )
    }
}
