package com.pedroeu.ficha.ui.sheet

import com.pedroeu.ficha.ui.i18n.trf
import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.domain.ArtificerItems
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.ResolvedChoice
import com.pedroeu.ficha.ui.components.ChoiceSection
import com.pedroeu.ficha.ui.components.EditableText
import com.pedroeu.ficha.ui.components.ExpandableOption
import com.pedroeu.ficha.ui.components.SectionHeader
import com.pedroeu.ficha.ui.components.TextEditDialog

@Composable
fun FeaturesTab(character: PlayerCharacter, viewModel: SheetViewModel, editMode: Boolean) {
    val species = SpeciesData.byId(character.speciesId)
    val charClass = ClassData.byId(character.classId)
    val lineage = species?.lineageOptions?.find { it.id == character.lineageId }
    val progression = ProgressionData.forClass(character.classId)
    val subclass = character.subclassId?.let { SubclassData.byId(it) }

    // Selections are stored in three separate maps; the resolver reads all of them so a
    // feature's actual picks appear under it rather than only its name.
    val classChoices = ChoiceResolver.classFeatureChoices(character).groupBy { it.featureName }
    val subclassChoices = ChoiceResolver.subclassFeatureChoices(character).groupBy { it.featureName }
    // Unanswered ones stay in the list rather than being hidden: a feat gained before the
    // app knew to ask, or one added by hand, still owes a decision, and this card is the
    // only place to make it. ChoiceLine already marks an open choice and offers "Choose".
    val originChoices = ChoiceResolver.originChoices(character)

    var addingFeat by remember { mutableStateOf(false) }
    var addingFeature by remember { mutableStateOf(false) }
    var editingChoice by remember { mutableStateOf<ResolvedChoice?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (charClass != null) {
            item {
                FeatureCard(trf("Class Features — {0}", charClass.name)) {
                    charClass.level1Features.forEach { feature ->
                        FeatureEntry(
                            name = feature.name,
                            description = feature.description,
                            scope = "class",
                            character = character,
                            viewModel = viewModel,
                            editMode = editMode,
                            choices = classChoices[feature.name].orEmpty(),
                            onEditChoice = { editingChoice = it },
                        )
                    }
                    // The level-1 branch options, e.g. Fighting Style or Divine Order.
                    ChoiceResolver.levelOneClassOptions(character).forEach { (label, option) ->
                        FeatureEntry(
                            name = "$label: $option",
                            description = "",
                            scope = "class",
                            character = character,
                            viewModel = viewModel,
                            editMode = editMode,
                        )
                    }
                    // Everything gained from level 2 onward, in the order it was earned.
                    progression?.features
                        ?.filter { it.level in 2..character.level }
                        ?.sortedBy { it.level }
                        ?.forEach { feature ->
                            FeatureEntry(
                                name = "Level ${feature.level} — ${feature.name}",
                                description = feature.description,
                                scope = "class",
                                character = character,
                                viewModel = viewModel,
                                editMode = editMode,
                                choices = classChoices[feature.name].orEmpty(),
                                onEditChoice = { editingChoice = it },
                            )
                        }
                }
            }
        }

        if (subclass != null) {
            item {
                FeatureCard(trf("Subclass — {0}", subclass.name)) {
                    Text(
                        text = subclass.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (subclass.isPlaytest) {
                        Text(
                            text = trf("Playtest material — {0}", subclass.source),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    subclass.features
                        .filter { it.level <= character.level }
                        .sortedBy { it.level }
                        .forEach { feature ->
                            FeatureEntry(
                                name = "Level ${feature.level} — ${feature.name}",
                                description = feature.description,
                                scope = "subclass",
                                character = character,
                                viewModel = viewModel,
                                editMode = editMode,
                                choices = subclassChoices[feature.name].orEmpty(),
                                onEditChoice = { editingChoice = it },
                            )
                        }
                }
            }
        }

        // Which of the Artificer's plans are actually made today, which is a daily decision
        // rather than a permanent one and so doesn't belong under the feature's text.
        if (ArtificerItems.hasFeature(character)) {
            item { ArtificerItemsCard(character, viewModel) }
        }

        // Features whose option the rules have you choose at the moment of use. These used to
        // be asked once during character creation, which turned "choose each time" into a
        // permanent pick and hid two thirds of the feature.
        item { PerUseChoicesCard(character, viewModel) }

        if (species != null) {
            item {
                FeatureCard(trf("Species Traits — {0}", species.name)) {
                    FeatureEntry(
                        name = tr("Size & Speed"),
                        description = "${species.size}, ${species.speed} feet of movement." +
                            if (species.darkvisionRange > 0)
                                " Darkvision out to ${species.darkvisionRange} feet."
                            else "",
                        scope = "species",
                        character = character,
                        viewModel = viewModel,
                        editMode = editMode,
                    )
                    species.traits.forEach { trait ->
                        FeatureEntry(
                            name = trait.name,
                            description = trait.description,
                            scope = "species",
                            character = character,
                            viewModel = viewModel,
                            editMode = editMode,
                        )
                    }
                    if (lineage != null) {
                        FeatureEntry(
                            name = "${species.lineageChoiceLabel ?: "Lineage"}: ${lineage.name}",
                            description = lineage.description,
                            scope = "species",
                            character = character,
                            viewModel = viewModel,
                            editMode = editMode,
                        )
                    }
                }
            }
        }

        item {
            FeatureCard(tr("Feats")) {
                if (character.featIds.isEmpty()) {
                    Text(
                        text = tr("No feats yet."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                character.featIds.forEach { featId ->
                    val feat = FeatData.byId(featId)
                    FeatureEntry(
                        name = feat?.name ?: featId,
                        description = feat?.description.orEmpty(),
                        scope = "feat",
                        featureId = featId,
                        character = character,
                        viewModel = viewModel,
                        editMode = editMode,
                        onRemove = { viewModel.removeFeat(featId) },
                    )
                }
                TextButton(onClick = { addingFeat = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(tr("  Add feat"))
                }
            }
        }

        if (originChoices.isNotEmpty()) {
            item {
                FeatureCard(tr("Origin Choices")) {
                    Text(
                        text = tr("What you picked from your species, class, background, and feats."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        originChoices.forEach { resolved ->
                            ChoiceLine(resolved, editMode) { editingChoice = resolved }
                        }
                    }
                }
            }
        }

        item {
            FeatureCard(tr("Other Features")) {
                if (character.customFeatures.isEmpty()) {
                    Text(
                        text = tr("Anything the app doesn't already know about goes here."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                character.customFeatures.forEach { feature ->
                    FeatureEntry(
                        name = feature.name,
                        description = feature.description,
                        scope = "custom",
                        featureId = feature.id,
                        character = character,
                        viewModel = viewModel,
                        editMode = editMode,
                        onRemove = { viewModel.removeCustomFeature(feature.id) },
                    )
                }
                TextButton(onClick = { addingFeature = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(tr("  Add feature"))
                }
            }
        }
    }

    if (addingFeat) {
        FeatPickerSheet(
            character = character,
            onDismiss = { addingFeat = false },
            onAdd = { featId, selections ->
                viewModel.addFeat(featId, selections)
                addingFeat = false
            },
        )
    }

    if (addingFeature) {
        TextEditDialog(
            title = tr("New feature"),
            initial = "",
            multiline = true,
            onDismiss = { addingFeature = false },
            onConfirm = { text ->
                if (!text.isNullOrBlank()) {
                    // First line is the name, the rest is the description.
                    val name = text.lineSequence().first().trim()
                    val description = text.lineSequence().drop(1).joinToString("\n").trim()
                    viewModel.addCustomFeature(name, description, "Custom")
                }
                addingFeature = false
            },
        )
    }

    editingChoice?.let { resolved ->
        ChoiceEditDialog(
            resolved = resolved,
            onDismiss = { editingChoice = null },
            onToggle = { optionId ->
                val current = resolved.selectedIds
                val next = when {
                    current.contains(optionId) -> current - optionId
                    current.size < resolved.choice.count -> current + optionId
                    resolved.choice.count == 1 -> listOf(optionId)
                    else -> current.drop(1) + optionId
                }
                viewModel.setChoiceSelection(resolved.choice.id, resolved.level, next)
            },
        )
    }
}

/** Shows the picks made inside a feature, and lets Edit Mode change them. */
@Composable
private fun ChoiceLine(
    resolved: ResolvedChoice,
    editMode: Boolean,
    onEdit: () -> Unit,
) {
    // The options the player actually picked, paired back to their rulebook entries so the
    // sheet can show what each one does rather than only its name.
    val picked = resolved.selectedIds.mapNotNull { id ->
        resolved.choice.options.find { it.id == id }
    }
    val hasRulesText = picked.any { it.description.isNotBlank() }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                text = if (hasRulesText) resolved.choice.label else "${resolved.choice.label}: ",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = if (hasRulesText) Modifier.weight(1f) else Modifier,
            )
            if (!hasRulesText) {
                Text(
                    text = resolved.summary.ifBlank { tr("not chosen yet") },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (resolved.isAnswered) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
            }
            if (editMode || !resolved.isAnswered) {
                TextButton(onClick = onEdit) {
                    Text(if (resolved.isAnswered) tr("Change") else tr("Choose"))
                }
            }
        }

        // Each pick opens to its own full rules text; picks without any (a skill, an ability
        // score) stay flat, which is why the compact one-line form is kept for those choices.
        if (hasRulesText) {
            picked.forEach { option ->
                ExpandableOption(
                    name = option.name,
                    description = option.description,
                    subtitle = option.supporting,
                )
            }
        }
    }
}

@Composable
private fun ChoiceEditDialog(
    resolved: ResolvedChoice,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(resolved.choice.label) },
        text = {
            ChoiceSection(
                choice = resolved.choice,
                selected = resolved.selectedIds,
                onToggle = onToggle,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(tr("Done")) }
        },
    )
}

@Composable
private fun FeatureCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(title)
            content()
        }
    }
}

/**
 * One feature: its name, its text, and any sub-choices it carries. Both the name and the text
 * can be rewritten in Edit Mode without losing the ability to reset back to the rulebook.
 */
@Composable
private fun FeatureEntry(
    name: String,
    description: String,
    scope: String,
    character: PlayerCharacter,
    viewModel: SheetViewModel,
    editMode: Boolean,
    featureId: String = name,
    choices: List<ResolvedChoice> = emptyList(),
    onEditChoice: (ResolvedChoice) -> Unit = {},
    onRemove: (() -> Unit)? = null,
) {
    val nameKey = "$scope:$featureId:name"
    val descKey = "$scope:$featureId:description"
    val nameOverride = character.textOverrides[nameKey]
    val descOverride = character.textOverrides[descKey]

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EditableText(
                value = nameOverride ?: name,
                editMode = editMode,
                onChange = { viewModel.setText(nameKey, it) },
                label = tr("Feature name"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                isOverridden = nameOverride != null,
                modifier = Modifier.weight(1f),
            )
            if (editMode && onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove $name",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (description.isNotBlank() || descOverride != null || editMode) {
            EditableText(
                value = descOverride ?: description,
                editMode = editMode,
                onChange = { viewModel.setText(descKey, it) },
                label = tr("Feature text"),
                style = MaterialTheme.typography.bodySmall,
                multiline = true,
                isOverridden = descOverride != null,
                placeholder = if (editMode) tr("Tap to add a description") else "",
            )
        }

        choices.forEach { resolved ->
            ChoiceLine(resolved, editMode) { onEditChoice(resolved) }
        }
    }
}
