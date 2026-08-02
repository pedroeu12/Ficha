package com.pedroeu.ficha.ui.sheet

import com.pedroeu.ficha.ui.i18n.trf
import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.CatalogItem
import com.pedroeu.ficha.data.content.ItemCatalog
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.ui.components.ChoiceChip
import com.pedroeu.ficha.ui.components.SelectableCard

/**
 * Two ways to add gear: browse the rulebook catalog, which fills in stats and description
 * automatically, or type a custom entry for anything homebrewed.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddItemSheet(
    onDismiss: () -> Unit,
    onAdd: (InventoryItem) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tabIndex by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.secondary,
            ) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    text = { Text(tr("From the rulebook")) },
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    text = { Text(tr("Custom item")) },
                )
            }

            when (tabIndex) {
                0 -> CatalogBrowser(onAdd = onAdd)
                else -> CustomItemForm(onAdd = onAdd)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatalogBrowser(onAdd: (InventoryItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var filtersOpen by remember { mutableStateOf(false) }

    val results = remember(query, category) { ItemCatalog.search(query, category) }

    Column(Modifier.padding(horizontal = 20.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(tr("Search the rulebook")) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )

        // The catalog has enough categories that showing every chip would push the results
        // off the sheet, so the filters fold away and say what they're currently set to.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        ) {
            TextButton(
                onClick = { filtersOpen = !filtersOpen },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Icon(
                    imageVector = if (filtersOpen) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = trf("  Filter: {0}", category ?: tr("All")),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.weight(1f))
            if (category != null) {
                TextButton(onClick = { category = null }) { Text(tr("Clear")) }
            }
        }

        if (filtersOpen) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                ChoiceChip(
                    label = tr("All"),
                    selected = category == null,
                    onClick = {
                        category = null
                        filtersOpen = false
                    },
                )
                ItemCatalog.CATEGORIES.forEach { name ->
                    ChoiceChip(
                        label = name,
                        selected = category == name,
                        onClick = {
                            category = if (category == name) null else name
                            // Picking a filter is the whole point of opening the list, so
                            // fold it away again and give the results the room back.
                            filtersOpen = false
                        },
                    )
                }
            }
        }

        if (results.isEmpty()) {
            Text(
                text = "Nothing matches \"$query\". Use the Custom item tab to add it by hand.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 420.dp),
        ) {
            items(results.size, key = { results[it].id }) { index ->
                val entry = results[index]
                CatalogRow(entry, onAdd = { onAdd(entry.toInventoryItem()) })
            }
        }
    }
}

@Composable
private fun CatalogRow(entry: CatalogItem, onAdd: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    SelectableCard(
        title = entry.name,
        subtitle = entry.description,
        selected = expanded,
        onClick = { expanded = !expanded },
        trailingLabel = buildString {
            append(entry.category)
            if (entry.costGp > 0) append(" • ${entry.costLabel}")
        },
        expandedContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entry.stats.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Button(
                    onClick = onAdd,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(tr("Add to inventory"))
                }
            }
        },
    )
}

@Composable
private fun CustomItemForm(onAdd: (InventoryItem) -> Unit) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var weight by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Column(
        Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(tr("Item name")) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text(tr("Quantity")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                label = { Text(tr("Weight (lb)")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(tr("Description or notes")) },
            minLines = 3,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                onAdd(
                    InventoryItem(
                        name = name.trim(),
                        quantity = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                        weightLb = weight.toDoubleOrNull() ?: 0.0,
                        notes = notes.trim(),
                    )
                )
            },
            enabled = name.isNotBlank(),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(tr("Add to inventory"))
        }
    }
}
