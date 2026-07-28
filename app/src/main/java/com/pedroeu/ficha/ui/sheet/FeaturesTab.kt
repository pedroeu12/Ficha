package com.pedroeu.ficha.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.SectionHeader

@Composable
fun FeaturesTab(character: PlayerCharacter) {
    val species = SpeciesData.byId(character.speciesId)
    val charClass = ClassData.byId(character.classId)
    val lineage = species?.lineageOptions?.find { it.id == character.lineageId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (charClass != null) {
            item {
                FeatureCard("Class Features — ${charClass.name}") {
                    charClass.level1Features.forEach { feature ->
                        FeatureEntry(feature.name, feature.description)
                    }
                    // Surface the options the player picked during creation alongside the fixed features.
                    charClass.choices.filterIsInstance<ClassChoice.FeatureOption>()
                        .forEach { choice ->
                            val selectedId = character.classChoiceSelections[choice.id]?.firstOrNull()
                            val option = choice.options.find { it.id == selectedId }
                            if (option != null) {
                                FeatureEntry("${choice.label}: ${option.name}", option.description)
                            }
                        }
                }
            }
        }

        if (species != null) {
            item {
                FeatureCard("Species Traits — ${species.name}") {
                    FeatureEntry(
                        "Size & Speed",
                        "${species.size}, ${species.speed} feet of movement." +
                            if (species.darkvisionRange > 0)
                                " Darkvision out to ${species.darkvisionRange} feet."
                            else "",
                    )
                    species.traits.forEach { trait ->
                        FeatureEntry(trait.name, trait.description)
                    }
                    if (lineage != null) {
                        FeatureEntry(
                            "${species.lineageChoiceLabel ?: "Lineage"}: ${lineage.name}",
                            lineage.description,
                        )
                    }
                }
            }
        }

        if (character.featIds.isNotEmpty()) {
            item {
                FeatureCard("Feats") {
                    character.featIds.forEach { featId ->
                        FeatData.byId(featId)?.let { feat ->
                            FeatureEntry(feat.name, feat.description)
                        }
                    }
                }
            }
        }
    }
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

@Composable
private fun FeatureEntry(name: String, description: String) {
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
