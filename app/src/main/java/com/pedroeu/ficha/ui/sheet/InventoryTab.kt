package com.pedroeu.ficha.ui.sheet

import com.pedroeu.ficha.ui.design.Corner
import com.pedroeu.ficha.ui.i18n.trf
import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.pedroeu.ficha.data.content.ItemCatalog
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.Coins
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.SectionHeader

@Composable
fun InventoryTab(character: PlayerCharacter, viewModel: SheetViewModel, editMode: Boolean) {
    var detailIndex by remember { mutableStateOf<Int?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { CoinsCard(character.coins, viewModel::setCoins) }

        item {
            SectionHeader(
                tr("Equipment"),
                trailing = trf(
                    "{0} items • {1} lb",
                    character.inventory.size,
                    CharacterCalculations.carriedWeight(character).toInt(),
                ),
            )
        }

        item {
            Button(
                onClick = { showAddSheet = true },
                shape = Corner.row,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(tr("  Add an item"), style = MaterialTheme.typography.labelLarge)
            }
        }

        itemsIndexed(character.inventory) { index, item ->
            InventoryRow(
                item = item,
                editMode = editMode,
                onOpen = { detailIndex = index },
                onToggleEquipped = { viewModel.toggleEquipped(index) },
                onRemove = { viewModel.removeInventoryItem(index) },
                onQuantityChange = { viewModel.setInventoryQuantity(index, it) },
            )
        }

        if (character.inventory.isEmpty()) {
            item {
                Text(
                    text = tr("Nothing carried yet. Add gear from the rulebook, or write in your own."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    detailIndex?.let { index ->
        character.inventory.getOrNull(index)?.let { item ->
            ItemDetailSheet(
                item = item,
                onDismiss = { detailIndex = null },
                editMode = editMode,
                onRename = { viewModel.updateInventoryItem(index, item.copy(name = it)) },
                onNotesChange = { viewModel.updateInventoryItem(index, item.copy(notes = it)) },
            )
        }
    }

    if (showAddSheet) {
        AddItemSheet(
            onDismiss = { showAddSheet = false },
            onAdd = { newItem ->
                viewModel.addInventoryItem(newItem)
                showAddSheet = false
            },
        )
    }
}

@Composable
private fun InventoryRow(
    item: InventoryItem,
    editMode: Boolean,
    onOpen: () -> Unit,
    onToggleEquipped: () -> Unit,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit,
) {
    val equippable = item.armorDefId != null || item.weaponDefId != null
    val catalogEntry = remember(item.name, item.weaponDefId, item.armorDefId) {
        ItemCatalog.resolve(item)
    }

    Card(
        shape = Corner.row,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (equippable) {
                    Checkbox(
                        checked = item.equipped,
                        onCheckedChange = { onToggleEquipped() },
                    )
                } else {
                    Spacer(Modifier.width(12.dp))
                }
                Column(
                    Modifier
                        .weight(1f)
                        .padding(
                            start = if (equippable) 0.dp else 12.dp,
                            top = 8.dp,
                            bottom = 8.dp,
                        ),
                ) {
                    Text(
                        text = if (item.quantity > 1) "${item.name} ×${item.quantity}" else item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = buildString {
                            if (equippable) {
                                append(if (item.equipped) tr("Equipped") else tr("Carried"))
                            }
                            catalogEntry?.let {
                                if (isNotEmpty()) append(" • ")
                                append(it.category)
                            }
                            // Made rather than found, so it's clear which lines the
                            // Artificer's daily allowance is holding.
                            if (item.craftedFromPlanId != null) {
                                if (isNotEmpty()) append(" • ")
                                append(tr("Made"))
                            }
                        }.ifBlank { tr("Tap for details") },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.equipped) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open ${item.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (editMode) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove ${item.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (editMode) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = tr("Quantity"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = item.quantity.toString(),
                        onValueChange = { text ->
                            onQuantityChange(text.filter { it.isDigit() }.take(3).toIntOrNull() ?: 1)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = Corner.small,
                        modifier = Modifier.width(96.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CoinsCard(coins: Coins, onChange: (Coins) -> Unit) {
    Card(
        shape = Corner.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(tr("Coins"))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CoinField(tr("GP"), coins.gp, Modifier.weight(1f)) { onChange(coins.copy(gp = it)) }
                CoinField(tr("SP"), coins.sp, Modifier.weight(1f)) { onChange(coins.copy(sp = it)) }
                CoinField(tr("CP"), coins.cp, Modifier.weight(1f)) { onChange(coins.copy(cp = it)) }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CoinField(tr("PP"), coins.pp, Modifier.weight(1f)) { onChange(coins.copy(pp = it)) }
                CoinField(tr("EP"), coins.ep, Modifier.weight(1f)) { onChange(coins.copy(ep = it)) }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CoinField(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { text ->
                onChange(text.filter { it.isDigit() }.take(6).toIntOrNull() ?: 0)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = Corner.small,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
