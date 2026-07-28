package com.pedroeu.ficha.ui.creation

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.model.Background
import com.pedroeu.ficha.ui.components.ChoiceChip
import com.pedroeu.ficha.ui.components.SectionHeader
import com.pedroeu.ficha.ui.components.SelectableCard

@Composable
fun BackgroundStep(state: CreationState, viewModel: CreationViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Your origin is the life you led before adventuring. It grants ability score increases, two skills, a tool, and an origin feat.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(BackgroundData.ALL, key = { it.id }) { background ->
            SelectableCard(
                title = background.name,
                subtitle = background.summary,
                selected = state.backgroundId == background.id,
                onClick = { viewModel.selectBackground(background.id) },
                trailingLabel = background.abilityOptions.joinToString("/") { it.abbreviation },
                expandedContent = { BackgroundDetails(background, state, viewModel) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackgroundDetails(
    background: Background,
    state: CreationState,
    viewModel: CreationViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SectionHeader("Ability Scores", trailing = state.bonusSpread.label)
        Text(
            text = "Choose how to spread the increase across this origin's three abilities, then tap the abilities to assign.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BonusSpread.entries.forEach { spread ->
                ChoiceChip(
                    label = spread.label,
                    selected = state.bonusSpread == spread,
                    onClick = { viewModel.setBonusSpread(spread) },
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            background.abilityOptions.forEach { ability ->
                val assigned = state.backgroundBonuses[ability]
                ChoiceChip(
                    label = ability.fullName,
                    supporting = assigned?.let { "+$it" } ?: "—",
                    selected = assigned != null,
                    onClick = { viewModel.toggleBackgroundBonus(ability) },
                )
            }
        }
        val remaining = state.unassignedBonuses()
        if (remaining.isNotEmpty()) {
            Text(
                text = "Unassigned: ${remaining.joinToString { "+$it" }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SectionHeader("What You Gain")
        DetailLine("Skills", background.skillProficiencies.joinToString { it.displayName })
        DetailLine("Tool", background.toolProficiency)
        FeatData.byId(background.featId)?.let { feat ->
            Column {
                Text(
                    text = "Origin Feat: ${feat.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = feat.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DetailLine("Equipment", background.equipment.joinToString())
        DetailLine("Starting Gold", "${background.startingGold} gp")
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}
