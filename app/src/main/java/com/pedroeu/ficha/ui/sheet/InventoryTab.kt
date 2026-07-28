package com.pedroeu.ficha.ui.sheet

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.pedroeu.ficha.domain.Coins
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.SectionHeader

@Composable
fun InventoryTab(character: PlayerCharacter, viewModel: SheetViewModel) {
    var newItemName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { CoinsCard(character.coins, viewModel::setCoins) }

        item {
            SectionHeader("Equipment", trailing = "${character.inventory.size} items")
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    label = { Text("Add an item") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        viewModel.addInventoryItem(newItemName, 1)
                        newItemName = ""
                    },
                    enabled = newItemName.isNotBlank(),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add item",
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }

        itemsIndexed(character.inventory) { index, item ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val equippable = item.armorDefId != null || item.weaponDefId != null
                    if (equippable) {
                        Checkbox(
                            checked = item.equipped,
                            onCheckedChange = { viewModel.toggleEquipped(index) },
                        )
                    } else {
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = if (equippable) 0.dp else 12.dp, top = 8.dp, bottom = 8.dp),
                    ) {
                        Text(
                            text = if (item.quantity > 1) "${item.name} ×${item.quantity}" else item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (equippable) {
                            Text(
                                text = if (item.equipped) "Equipped" else "Carried",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (item.equipped) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.removeInventoryItem(index) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove ${item.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoinsCard(coins: Coins, onChange: (Coins) -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("Coins")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CoinField("GP", coins.gp, Modifier.weight(1f)) { onChange(coins.copy(gp = it)) }
                CoinField("SP", coins.sp, Modifier.weight(1f)) { onChange(coins.copy(sp = it)) }
                CoinField("CP", coins.cp, Modifier.weight(1f)) { onChange(coins.copy(cp = it)) }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CoinField("PP", coins.pp, Modifier.weight(1f)) { onChange(coins.copy(pp = it)) }
                CoinField("EP", coins.ep, Modifier.weight(1f)) { onChange(coins.copy(ep = it)) }
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
            shape = RoundedCornerShape(8.dp),
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
