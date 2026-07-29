package com.pedroeu.ficha.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.CatalogItem
import com.pedroeu.ficha.data.content.ItemCatalog
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.ui.components.EditableText
import com.pedroeu.ficha.ui.components.SectionHeader

/**
 * Full detail for one inventory line: its rulebook description and stats when the item is a
 * known one, or whatever the player typed when it isn't.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailSheet(
    item: InventoryItem,
    onDismiss: () -> Unit,
    editMode: Boolean = false,
    onRename: (String) -> Unit = {},
    onNotesChange: (String) -> Unit = {},
) {
    val catalogItem: CatalogItem? = ItemCatalog.resolve(item)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                EditableText(
                    value = item.name,
                    editMode = editMode,
                    onChange = { onRename(it.orEmpty()) },
                    label = "Item name",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = catalogItem?.category ?: "Custom item",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                DetailStat("Quantity", "${item.quantity}")
                catalogItem?.let {
                    DetailStat("Cost", it.costLabel)
                    DetailStat("Weight", it.weightLabel)
                }
                if (catalogItem == null && item.weightLb > 0) {
                    DetailStat("Weight", "${item.weightLb} lb")
                }
            }

            if (catalogItem != null && catalogItem.stats.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader("Statistics")
                    catalogItem.stats.forEach { (label, value) ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            val description = catalogItem?.description?.takeIf { it.isNotBlank() }
                ?: item.notes.takeIf { it.isNotBlank() }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeader("Description")
                Text(
                    text = description
                        ?: "No description recorded for this item. Add one in Edit Mode, or " +
                        "replace it with an entry from the rulebook list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (editMode || (item.notes.isNotBlank() && item.notes != catalogItem?.description)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader("Your notes")
                    EditableText(
                        value = item.notes,
                        editMode = editMode,
                        onChange = { onNotesChange(it.orEmpty()) },
                        label = "Item notes",
                        multiline = true,
                        placeholder = if (editMode) "Tap to add your own notes" else "",
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
