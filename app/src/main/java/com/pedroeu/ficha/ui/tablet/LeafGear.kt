package com.pedroeu.ficha.ui.tablet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.Coins
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.i18n.trf

/**
 * The back of the sheet: what the character carries and who they are.
 *
 * Two columns, and deliberately unequal. Equipment is a working list that gets read during
 * play, so it takes the width; appearance, backstory and notes are read between sessions and
 * sit in a narrower column beside it, closer to the margin the paper sheet gives them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeafGear(handle: SheetHandle) {
    val v = LocalVellum.current
    val character = handle.character

    Row(Modifier.fillMaxWidth()) {
        LazyColumn(
            Modifier.weight(0.58f),
            contentPadding = PaddingValues(start = 20.dp, end = 10.dp, top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Leaf(tr("Coins")) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CoinStone(tr("CP"), character.coins.cp) {
                            handle.viewModel.setCoins(character.coins.copy(cp = it))
                        }
                        CoinStone(tr("SP"), character.coins.sp) {
                            handle.viewModel.setCoins(character.coins.copy(sp = it))
                        }
                        CoinStone(tr("EP"), character.coins.ep) {
                            handle.viewModel.setCoins(character.coins.copy(ep = it))
                        }
                        CoinStone(tr("GP"), character.coins.gp) {
                            handle.viewModel.setCoins(character.coins.copy(gp = it))
                        }
                        CoinStone(tr("PP"), character.coins.pp) {
                            handle.viewModel.setCoins(character.coins.copy(pp = it))
                        }
                    }
                }
            }

            item {
                Leaf(
                    title = tr("Equipment"),
                    trailing = { PenMark(tr("Add to inventory")) { handle.open(SheetOverlay.AddItem) } },
                ) {
                    Caption(
                        trf(
                            "{0} items • {1} lb",
                            character.inventory.size,
                            CharacterCalculations.carriedWeight(character).toInt(),
                        )
                    )
                    if (character.inventory.isEmpty()) {
                        SheetText(
                            tr("Nothing carried yet. Add gear from the rulebook, or write in your own."),
                            soft = true,
                        )
                    } else {
                        LedgerHeader(
                            listOf(
                                tr("Name") to 2.2f,
                                tr("Quantity") to 0.7f,
                                tr("Equipped") to 0.9f,
                            )
                        )
                    }
                }
            }

            character.inventory.forEachIndexed { index, item ->
                item(key = "gear:$index:${item.name}") {
                    LedgerRow(index, onClick = { handle.openItem(index) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(2.2f)) {
                                InkedValue(item.name, emphasis = item.equipped)
                                if (item.craftedFromPlanId != null) Caption(tr("Made"))
                            }
                            Row(
                                Modifier.weight(0.7f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                PenMark(tr("Give back"), glyph = "−") {
                                    handle.viewModel.setInventoryQuantity(index, item.quantity - 1)
                                }
                                SheetText("${item.quantity}")
                                PenMark(tr("Add")) {
                                    handle.viewModel.setInventoryQuantity(index, item.quantity + 1)
                                }
                            }
                            Row(
                                Modifier.weight(0.9f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                val equippable = item.weaponDefId != null || item.armorDefId != null
                                if (equippable) {
                                    Pip(
                                        filled = item.equipped,
                                        onClick = { handle.viewModel.toggleEquipped(index) },
                                    )
                                }
                                if (handle.editMode) {
                                    PenMark(tr("Delete"), glyph = "×") {
                                        handle.viewModel.removeInventoryItem(index)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Fold()

        LazyColumn(
            Modifier.weight(0.42f),
            contentPadding = PaddingValues(start = 10.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Leaf(tr("Identity")) {
                    WrittenLine(
                        label = tr("Species"),
                        key = "bio:species",
                        fallback = com.pedroeu.ficha.data.content.SpeciesData
                            .byId(character.speciesId)?.name.orEmpty(),
                        handle = handle,
                    )
                    WrittenLine(
                        label = tr("Class"),
                        key = "bio:class",
                        fallback = com.pedroeu.ficha.domain.ClassLevels.label(character),
                        handle = handle,
                    )
                    WrittenLine(
                        label = tr("Origin"),
                        key = "bio:origin",
                        fallback = com.pedroeu.ficha.data.content.BackgroundData
                            .byId(character.backgroundId)?.name.orEmpty(),
                        handle = handle,
                    )
                    WrittenLine(
                        label = tr("Languages"),
                        key = "bio:languages",
                        fallback = character.languages.joinToString(", "),
                        handle = handle,
                    )
                }
            }
            item {
                Prose(
                    title = tr("Appearance"),
                    value = character.appearance,
                    placeholder = tr("Tap to add your own notes"),
                    onEdit = {
                        handle.editText("bio:appearance", tr("Appearance"), character.appearance)
                    },
                )
            }
            item {
                Prose(
                    title = tr("Backstory & Personality"),
                    value = character.backstory,
                    placeholder = tr("Tap to add your own notes"),
                    onEdit = {
                        handle.editText("bio:backstory", tr("Backstory & Personality"), character.backstory)
                    },
                )
            }
            item {
                Prose(
                    title = tr("Alignment"),
                    value = character.alignment,
                    placeholder = tr("Not set"),
                    onEdit = {
                        handle.editText("bio:alignment", tr("Alignment"), character.alignment)
                    },
                )
            }
            item {
                Prose(
                    title = tr("Session Notes"),
                    value = character.notes,
                    placeholder = tr("Tap to add your own notes"),
                    onEdit = { handle.editText("bio:notes", tr("Session Notes"), character.notes) },
                )
            }
        }
    }
}

/** A pouch of one coin, tapped to count up and long-pressed nowhere — kept deliberately plain. */
@Composable
private fun CoinStone(label: String, amount: Int, onChange: (Int) -> Unit) {
    val v = LocalVellum.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        PenMark(tr("Give back"), glyph = "−") { onChange((amount - 1).coerceAtLeast(0)) }
        Stone(modifier = Modifier.widthIn(min = 66.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$amount", style = NumeralMedium, color = v.ink)
                Caption(label, align = TextAlign.Center)
            }
        }
        PenMark(tr("Add")) { onChange(amount + 1) }
    }
}

/**
 * A block of the player's own writing, on ruled lines.
 *
 * The lines are drawn under the text rather than around it, which is what a lined page does
 * and what stops four of these reading as four boxes.
 */
@Composable
private fun Prose(
    title: String,
    value: String,
    placeholder: String,
    onEdit: () -> Unit,
) {
    val v = LocalVellum.current
    Leaf(title = title, trailing = { PenMark(tr("Edit"), glyph = "✎", onClick = onEdit) }) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onEdit)
                .padding(vertical = 2.dp),
        ) {
            Text(
                text = value.ifBlank { placeholder },
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                color = if (value.isBlank()) v.inkFaint else v.ink,
            )
            Spacer(Modifier.height(6.dp))
            // A few ruled lines beneath, so an empty block still reads as somewhere to write.
            repeat(if (value.isBlank()) 3 else 1) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(v.rule))
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
