package com.pedroeu.ficha.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.SectionHeader

@Composable
fun BioTab(character: PlayerCharacter, viewModel: SheetViewModel) {
    val species = SpeciesData.byId(character.speciesId)
    val charClass = ClassData.byId(character.classId)
    val background = BackgroundData.byId(character.backgroundId)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader("Identity")
                    BioLine("Name", character.name)
                    BioLine("Species", species?.name.orEmpty())
                    species?.lineageOptions?.find { it.id == character.lineageId }?.let {
                        BioLine(species.lineageChoiceLabel ?: "Lineage", it.name)
                    }
                    BioLine("Class", charClass?.name.orEmpty())
                    BioLine("Level", "${character.level}")
                    BioLine("Origin", background?.name.orEmpty())
                    if (character.alignment.isNotBlank()) {
                        BioLine("Alignment", character.alignment)
                    }
                    BioLine("Languages", character.languages.joinToString())
                }
            }
        }

        item {
            OutlinedTextField(
                value = character.appearance,
                onValueChange = viewModel::setAppearance,
                label = { Text("Appearance") },
                minLines = 3,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            OutlinedTextField(
                value = character.backstory,
                onValueChange = viewModel::setBackstory,
                label = { Text("Backstory & Personality") },
                minLines = 5,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            OutlinedTextField(
                value = character.notes,
                onValueChange = viewModel::setNotes,
                label = { Text("Session Notes") },
                minLines = 5,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BioLine(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
