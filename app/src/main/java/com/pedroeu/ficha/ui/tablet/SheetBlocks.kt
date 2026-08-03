package com.pedroeu.ficha.ui.tablet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.EquipmentData
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterDcs
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.i18n.trf

/**
 * A line the player can write on, backed by a text override.
 *
 * The sheet has a handful of these — resistances, conditions, the proficiency lines — where
 * the rules produce a starting value and the table may want something else written in. The
 * override is what is shown once it exists, and the gold tint is how the page admits that the
 * line is no longer what the rules said.
 */
@Composable
fun WrittenLine(
    label: String,
    key: String,
    fallback: String,
    handle: SheetHandle,
) {
    val v = LocalVellum.current
    val override = handle.character.textOverrides[key]
    val value = override ?: fallback

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { handle.editText(key, label, override ?: "") }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Caption(label, modifier = Modifier.padding(end = 10.dp).weight(0.9f))
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
            color = if (override != null) v.accent else v.inkSoft,
            modifier = Modifier.weight(2f),
        )
    }
}

/**
 * Armour class, what the character shrugs off, and what is currently wrong with them.
 *
 * The printed sheet keeps resistances and conditions as blank lines because no two tables
 * track them the same way, and that is worth preserving: these are written, not computed.
 */
@Composable
fun DefensesBlock(handle: SheetHandle) {
    val character = handle.character
    val equipped = character.inventory
        .filter { it.equipped && it.armorDefId != null }
        .mapNotNull { EquipmentData.armorById(it.armorDefId!!) }

    Leaf(tr("Defenses")) {
        WrittenLine(
            label = tr("Equipped Armor"),
            key = "combat:armor",
            fallback = equipped.joinToString { it.name }.ifBlank { tr("None") },
            handle = handle,
        )
        WrittenLine(
            label = tr("Resistances"),
            key = "combat:resistances",
            fallback = tr("None recorded"),
            handle = handle,
        )
        WrittenLine(
            label = tr("Conditions"),
            key = "combat:conditions",
            fallback = tr("None"),
            handle = handle,
        )
    }
}

/**
 * Every save DC the character imposes, not just the spellcasting one.
 *
 * A Monk's Stunning Strike and a Dragonborn's breath weapon each have their own, and a player
 * who has to derive one mid-turn will get it wrong. They belong on the front of the sheet,
 * beside the attacks they accompany.
 */
@Composable
fun SaveDcsBlock(handle: SheetHandle) {
    val v = LocalVellum.current
    val dcs = CharacterDcs.all(handle.character)
    if (dcs.isEmpty()) return

    Leaf(tr("Save DCs by source")) {
        dcs.forEach { dc ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    InkedValue(dc.label, emphasis = true)
                    Caption(
                        trf(
                            "{0} • Attack {1}",
                            tr(dc.ability.fullName),
                            CharacterCalculations.formatModifier(dc.attackBonus),
                        )
                    )
                }
                Text(
                    text = trf("DC {0}", dc.dc),
                    style = NumeralMedium,
                    color = v.accent,
                )
            }
        }
    }
}

/**
 * Tools and other proficiencies the player keeps by hand.
 *
 * The rules grant some and the table grants others, and there is no telling them apart once
 * they are on the sheet — so this stays a plain list you add to and take from.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolsBlock(handle: SheetHandle) {
    val v = LocalVellum.current
    val tools = handle.character.toolProficiencies

    Leaf(
        title = tr("Tool Proficiencies"),
        trailing = { PenMark(tr("Add tool")) { handle.open(SheetOverlay.AddTool) } },
    ) {
        if (tools.isEmpty()) {
            SheetText(tr("None"), soft = true)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tools.forEach { tool ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Stone(corner = 8.dp) {
                        Text(
                            text = tool,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = v.ink,
                        )
                    }
                    if (handle.editMode) {
                        PenMark(tr("Delete"), glyph = "×") {
                            handle.viewModel.removeToolProficiency(tool)
                        }
                    }
                }
            }
        }
    }
}
