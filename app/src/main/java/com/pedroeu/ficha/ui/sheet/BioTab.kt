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
import com.pedroeu.ficha.ui.components.EditableText
import com.pedroeu.ficha.ui.components.SectionHeader

@Composable
fun BioTab(character: PlayerCharacter, viewModel: SheetViewModel, editMode: Boolean) {
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
                    EditableBioLine(
                        label = "Name",
                        value = character.name,
                        editMode = editMode,
                        onChange = { viewModel.setName(it.orEmpty()) },
                    )
                    EditableBioLine(
                        label = "Species",
                        value = character.textOverrides["bio:species"] ?: species?.name.orEmpty(),
                        editMode = editMode,
                        overridden = character.textOverrides.containsKey("bio:species"),
                        onChange = { viewModel.setText("bio:species", it) },
                    )
                    species?.lineageOptions?.find { it.id == character.lineageId }?.let {
                        EditableBioLine(
                            label = species.lineageChoiceLabel ?: "Lineage",
                            value = character.textOverrides["bio:lineage"] ?: it.name,
                            editMode = editMode,
                            overridden = character.textOverrides.containsKey("bio:lineage"),
                            onChange = { text -> viewModel.setText("bio:lineage", text) },
                        )
                    }
                    EditableBioLine(
                        label = "Class",
                        value = character.textOverrides["bio:class"] ?: charClass?.name.orEmpty(),
                        editMode = editMode,
                        overridden = character.textOverrides.containsKey("bio:class"),
                        onChange = { viewModel.setText("bio:class", it) },
                    )
                    BioLine("Level", "${character.level}")
                    EditableBioLine(
                        label = "Origin",
                        value = character.textOverrides["bio:origin"] ?: background?.name.orEmpty(),
                        editMode = editMode,
                        overridden = character.textOverrides.containsKey("bio:origin"),
                        onChange = { viewModel.setText("bio:origin", it) },
                    )
                    EditableBioLine(
                        label = "Alignment",
                        value = character.alignment,
                        editMode = editMode,
                        onChange = { viewModel.setAlignment(it.orEmpty()) },
                        placeholder = "Not set",
                    )
                    EditableBioLine(
                        label = "Languages",
                        value = character.textOverrides["bio:languages"]
                            ?: character.languages.joinToString(),
                        editMode = editMode,
                        overridden = character.textOverrides.containsKey("bio:languages"),
                        onChange = { viewModel.setText("bio:languages", it) },
                    )
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

/** An identity line the player can rewrite while Edit Mode is on. */
@Composable
private fun EditableBioLine(
    label: String,
    value: String,
    editMode: Boolean,
    onChange: (String?) -> Unit,
    overridden: Boolean = false,
    placeholder: String = "",
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.width(110.dp),
        )
        EditableText(
            value = value,
            editMode = editMode,
            onChange = onChange,
            label = label,
            isOverridden = overridden,
            placeholder = placeholder,
            modifier = Modifier.weight(1f),
        )
    }
}
