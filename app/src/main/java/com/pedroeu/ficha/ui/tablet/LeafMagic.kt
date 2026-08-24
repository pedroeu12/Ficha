package com.pedroeu.ficha.ui.tablet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.OverridableStat
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.i18n.trf

/**
 * The spellcasting side of the sheet.
 *
 * On paper this is a page of its own, and it earns it: the numbers at the top, the slot
 * boxes beneath them, and then a long ruled table of everything prepared. The table is the
 * point — a caster's whole turn is choosing a line from it — so it gets the width, and the
 * numbers sit in a band above rather than in a column beside it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeafMagic(handle: SheetHandle) {
    val v = LocalVellum.current
    val character = handle.character
    val ability = CharacterCalculations.spellcastingAbility(character)
    val spells = CharacterSpells.all(character).sortedWith(
        compareBy({ it.level }, { it.name })
    )
    val slots = CharacterCalculations.spellSlots(character)

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Leaf(tr("Spellcasting")) {
                if (ability == null) {
                    SheetText(
                        tr("This character has no spellcasting from their class. You can still " +
                            "add spells from feats, items, or anywhere else."),
                        soft = true,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Stone(modifier = Modifier.widthIn(min = 96.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = ability.abbreviation,
                                    style = NumeralMedium,
                                    color = v.ink,
                                )
                                Caption(tr("Ability"), align = TextAlign.Center)
                            }
                        }
                        SpellStone(
                            handle, OverridableStat.SPELL_SAVE_DC,
                            "${CharacterCalculations.spellSaveDc(character) ?: 0}",
                            tr("Save DC"),
                        )
                        SpellStone(
                            handle, OverridableStat.SPELL_ATTACK_BONUS,
                            CharacterCalculations.formatModifier(
                                CharacterCalculations.spellAttackBonus(character) ?: 0
                            ),
                            tr("Attack"),
                        )
                        SpellStone(
                            handle, OverridableStat.MAX_PREPARED_SPELLS,
                            trf(
                                "{0} / {1}",
                                CharacterSpells.preparedCount(character),
                                CharacterCalculations.maxPreparedSpells(character),
                            ),
                            tr("Prepared"),
                        )
                        SpellStone(
                            handle, OverridableStat.CANTRIPS_KNOWN,
                            "${CharacterCalculations.maxCantripsKnown(character)}",
                            tr("Cantrips known"),
                        )
                    }
                }
            }
        }

        if (slots.isNotEmpty()) {
            item {
                Leaf(
                    title = tr("Spell Slots"),
                    trailing = {
                        // The same escape hatch the phone has: a table running a variant can
                        // set its own totals, and this puts the class table back.
                        if (handle.editMode && handle.character.spellSlotOverrides.isNotEmpty()) {
                            PenMark(tr("Reset to the class table"), glyph = "↺") {
                                handle.viewModel.clearSpellSlotOverrides()
                            }
                        }
                    },
                ) {
                    slots.toSortedMap().forEach { (level, total) ->
                        val expended = character.spellSlotsExpended[level.toString()] ?: 0
                        SlotRow(
                            level = level,
                            total = total,
                            expended = expended,
                            onSet = { handle.viewModel.setSpellSlotsExpended(level, it) },
                            onEditTotal = { handle.viewModel.setSpellSlotTotal(level, it) },
                            editMode = handle.editMode,
                        )
                    }
                }
            }
        }

        item {
            Leaf(
                title = tr("Prepared & Known Spells"),
                trailing = { PenMark(tr("Add spell")) { handle.open(SheetOverlay.AddSpell) } },
            ) {
                if (spells.isEmpty()) {
                    SheetText(
                        tr("No spells recorded yet. Level up to learn some, or add them here in Edit Mode."),
                        soft = true,
                    )
                } else {
                    LedgerHeader(
                        listOf(
                            tr("Name") to 2f,
                            tr("Level") to 0.7f,
                            tr("From") to 1.2f,
                            tr("Prepared") to 0.8f,
                        )
                    )
                }
            }
        }

        itemsIndexedSpells(spells) { index, spell ->
            val granted = CharacterSpells.isGranted(character, spell.id)
            LedgerRow(index, onClick = { handle.openSpell(spell) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InkedValue(spell.name, Modifier.weight(2f), emphasis = spell.prepared)
                    SheetText(
                        text = if (spell.level == 0) tr("Cantrip") else "${spell.level}",
                        modifier = Modifier.weight(0.7f),
                    )
                    SheetText(
                        text = spell.source.ifBlank { "—" },
                        soft = true,
                        modifier = Modifier.weight(1.2f),
                    )
                    Row(
                        Modifier.weight(0.8f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Pip(
                            filled = spell.prepared || granted,
                            onClick = if (granted || spell.level == 0) null else {
                                { handle.viewModel.toggleSpellPrepared(spell.id) }
                            },
                        )
                        if (granted) Caption(tr("Always prepared"))
                        if (handle.editMode) {
                            PenMark(tr("Delete"), glyph = "×") {
                                handle.viewModel.removeSpell(spell.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A boxed spellcasting number, overridable like every other calculated value. */
@Composable
private fun SpellStone(
    handle: SheetHandle,
    stat: OverridableStat,
    value: String,
    label: String,
) {
    val v = LocalVellum.current
    Stone(
        modifier = Modifier.widthIn(min = 96.dp),
        // Edit Mode only, like every other overridable number on the sheet.
        onClick = if (handle.editMode) ({ handle.editStat(stat) }) else null,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = NumeralMedium,
                color = if (handle.isAdjusted(stat)) v.accent else v.ink,
            )
            Caption(label, align = TextAlign.Center)
        }
    }
}

/**
 * One level's slots, drawn as the row of boxes a sheet prints them as.
 *
 * Tapping a box burns a slot; tapping the last burnt one gives it back. In Edit Mode the two
 * marks at the end change how many there are, for a table running a variant.
 */
@Composable
private fun SlotRow(
    level: Int,
    total: Int,
    expended: Int,
    onSet: (Int) -> Unit,
    onEditTotal: (Int) -> Unit,
    editMode: Boolean,
) {
    val v = LocalVellum.current
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = trf("Level {0}", level),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
            color = v.inkSoft,
            modifier = Modifier.width(74.dp),
        )
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (1..total).forEach { index ->
                Pip(
                    filled = index <= expended,
                    onClick = { onSet(if (expended == index) index - 1 else index) },
                )
            }
        }
        if (editMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PenMark(tr("Give back"), glyph = "−") { onEditTotal((total - 1).coerceAtLeast(0)) }
                PenMark(tr("Add")) { onEditTotal(total + 1) }
            }
        }
    }
}

/** A LazyColumn helper so the ledger rows keep their alternating band across items. */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedSpells(
    spells: List<com.pedroeu.ficha.domain.KnownSpell>,
    row: @Composable (Int, com.pedroeu.ficha.domain.KnownSpell) -> Unit,
) {
    spells.forEachIndexed { index, spell ->
        item(key = "spell:${spell.id}") { row(index, spell) }
    }
}
