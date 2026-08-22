package com.pedroeu.ficha.ui.creation

import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.pedroeu.ficha.ui.components.SourceSectionHeader
import com.pedroeu.ficha.ui.components.SourceGrouping
import com.pedroeu.ficha.ui.components.PickerSearchField
import com.pedroeu.ficha.ui.components.NoSearchResults
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.model.CharClass
import com.pedroeu.ficha.ui.components.SectionHeader
import com.pedroeu.ficha.ui.components.SelectableCard

@Composable
fun ClassStep(state: CreationState, viewModel: CreationViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var collapsed by rememberSaveable { mutableStateOf(setOf<String>()) }
    val all = state.availableClasses
    val matching = SourceGrouping.matching(all, query) { it.name + " " + it.summary }
    val grouped = SourceGrouping.worthGrouping(all) { it.book }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = tr("Your class is what you do in the world: how you fight, what magic you wield, and what you're trained in. You'll pick its options on the next step."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            if (all.size >= SourceGrouping.SEARCH_THRESHOLD) {
                PickerSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = tr("Search classes"),
                )
            }
        }

        if (matching.isEmpty()) {
            item { NoSearchResults(query) }
        }

        // A search is already a filter; grouping its results as well buries the matches.
        val sections =
            if (grouped && query.isBlank()) SourceGrouping.byBook(matching) { it.book }
            else listOf(null to matching)

        sections.forEach { (book, entries) ->
            val sectionKey = book?.id ?: "all"
            val open = sectionKey !in collapsed
            if (book != null) {
                item(key = "header:$sectionKey") {
                    SourceSectionHeader(
                        book = book,
                        count = entries.size,
                        expanded = open,
                        onToggle = {
                            collapsed = if (open) collapsed + sectionKey else collapsed - sectionKey
                        },
                    )
                }
            }
            if (open) items(entries, key = { it.id }) { charClass ->
            SelectableCard(
                title = charClass.name,
                subtitle = charClass.summary,
                selected = state.classId == charClass.id,
                onClick = { viewModel.selectClass(charClass.id) },
                trailingLabel = "d${charClass.hitDie} Hit Die • " +
                    charClass.primaryAbility.joinToString("/") { it.abbreviation },
                expandedContent = { ClassDetails(charClass) },
            )
            }
        }
    }
}

@Composable
private fun ClassDetails(charClass: CharClass) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SectionHeader(tr("Level 1 Features"))
        charClass.level1Features.forEach { feature ->
            Column {
                Text(
                    text = feature.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SectionHeader(tr("Proficiencies"))
        LabeledLine(tr("Saving Throws"), charClass.savingThrows.joinToString { it.fullName })
        LabeledLine(
            tr("Armor"),
            charClass.armorProficiencies.takeIf { it.isNotEmpty() }?.joinToString() ?: tr("None"),
        )
        LabeledLine(tr("Weapons"), charClass.weaponProficiencies.joinToString())
        if (charClass.toolProficiencies.isNotEmpty()) {
            LabeledLine(tr("Tools"), charClass.toolProficiencies.joinToString())
        }
    }
}

@Composable
private fun LabeledLine(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
