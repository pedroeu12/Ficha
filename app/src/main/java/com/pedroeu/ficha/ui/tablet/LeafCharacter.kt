package com.pedroeu.ficha.ui.tablet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterAttacks
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.CharacterHitDice
import com.pedroeu.ficha.domain.OverridableStat
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.i18n.trf
import com.pedroeu.ficha.ui.sheet.PerUseChoicesCard
import com.pedroeu.ficha.ui.sheet.ResourcesCard

/**
 * The front of the sheet: who the character is and what they can do about it.
 *
 * Two columns rather than four. The left is the ability blocks, which on paper is the tall
 * ribbon down the side of the page and the thing a player's eye goes to first; the right is
 * the body of the sheet — condition, attacks, and everything with a limited number of uses.
 * The proportions are the paper's, not a grid's: the abilities column is as wide as it needs
 * to be and no wider.
 */
@Composable
fun LeafCharacter(handle: SheetHandle) {
    Row(Modifier.fillMaxWidth()) {
        LazyColumn(
            Modifier.weight(0.38f),
            contentPadding = PaddingValues(
                start = 20.dp, end = 10.dp, top = 12.dp, bottom = 40.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { ConditionBlock(handle) }
            item { EngravedHeading(tr("Abilities")) }
            Ability.ALL.forEach { ability ->
                item(key = ability.name) {
                    AbilityBlock(ability, handle.character, handle, handle.editMode)
                }
            }
        }

        // The fold: a ruled line down the page where a printed sheet's columns meet.
        Fold()

        LazyColumn(
            Modifier.weight(0.62f),
            contentPadding = PaddingValues(
                start = 10.dp, end = 20.dp, top = 12.dp, bottom = 40.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { AttacksLedger(handle) }
            item { SaveDcsBlock(handle) }
            item { DefensesBlock(handle) }
            item { ResourcesCard(handle.character, handle.viewModel, handle.editMode, framed = false) }
            item { PerUseChoicesCard(handle.character, handle.viewModel, framed = false) }
        }
    }
}

/** A ruled crease between the sheet's columns. */
@Composable
fun Fold(modifier: Modifier = Modifier) {
    val v = LocalVellum.current
    Box(
        modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(v.rule)
    )
}

/**
 * Hit points, hit dice, death saves and inspiration — the state of the character right now.
 *
 * Grouped because they are used together and only together: nothing here matters until
 * something has gone wrong. On paper these sit in one boxed corner of the sheet, and keeping
 * that grouping is what makes the rest of the page calm.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConditionBlock(handle: SheetHandle) {
    val v = LocalVellum.current
    val character = handle.character
    val max = CharacterCalculations.maxHitPoints(character)
    // Each class brings its own die, so this is a list of pools rather than one number.
    val pools = CharacterHitDice.pools(character)
    val diceLeft = CharacterHitDice.remaining(character)
    val diceTotal = CharacterHitDice.totalDice(character)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EngravedHeading(tr("Hit Points"))

        // The number, big, with the two buttons that change it most often either side.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Nib(tr("Take 1 damage"), "−") { handle.viewModel.adjustHitPoints(-1) }
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (handle.editMode) {
                            Modifier.clickable { handle.editStat(OverridableStat.MAX_HIT_POINTS) }
                        } else {
                            Modifier
                        }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${character.currentHitPoints}",
                    style = NumeralLarge.copy(fontSize = 38.sp),
                    color = when {
                        character.currentHitPoints == 0 -> v.danger
                        character.currentHitPoints <= max / 4 -> v.crimson
                        else -> v.ink
                    },
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(v.ruleStrong.copy(alpha = 0.5f)))
                Spacer(Modifier.height(3.dp))
                Caption(trf("of {0} maximum", max), align = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            }
            Nib(tr("Heal 1"), "+") { handle.viewModel.adjustHitPoints(1) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stone(modifier = Modifier.weight(1f), onClick = {
                handle.viewModel.setTemporaryHitPoints(character.temporaryHitPoints + 1)
            }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${character.temporaryHitPoints}",
                        style = NumeralMedium,
                        color = if (character.temporaryHitPoints > 0) v.accent else v.inkFaint,
                    )
                    Caption(tr("Temp HP"), align = TextAlign.Center)
                }
            }
            // Not a button: it was wired to clear the Temp HP beside it, which is what the
            // tile above does and the last thing this one should.
            Stone(modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = ClassLevels.hitDiceLabel(character),
                        style = NumeralMedium.copy(
                            fontSize = if (pools.size > 1) 15.sp else 20.sp,
                        ),
                        color = v.ink,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Caption(tr("Hit Dice"), align = TextAlign.Center)
                }
            }
        }

        // Hit dice, as pips you spend — the same gesture as everything else on the sheet, but
        // one row per class: a d10 and a d6 are different dice and spending one is not
        // spending the other.
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Caption(trf("Hit Dice — {0} of {1} left", diceLeft, diceTotal))
            pools.forEach { pool ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pools.size > 1) {
                        Caption(pool.label, modifier = Modifier.width(52.dp))
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        (1..pool.total).forEach { index ->
                            Pip(
                                filled = index <= pool.spent,
                                tint = v.inkSoft,
                                onClick = {
                                    handle.viewModel.setHitDiceSpent(
                                        pool.classId,
                                        if (pool.spent == index) index - 1 else index,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        InkRule()

        // Death saves and inspiration, side by side: both are things that are either there or
        // they aren't, and neither needs a number.
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Caption(tr("Death Saves"))
                DeathRow(
                    label = tr("Successes"),
                    marked = character.deathSaves.successes,
                    tint = v.accent,
                ) { handle.viewModel.setDeathSaves(it, character.deathSaves.failures) }
                DeathRow(
                    label = tr("Failures"),
                    marked = character.deathSaves.failures,
                    tint = v.danger,
                ) { handle.viewModel.setDeathSaves(character.deathSaves.successes, it) }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Caption(tr("Inspiration"), align = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (character.heroicInspiration) v.accent
                            else androidx.compose.ui.graphics.Color.Transparent
                        )
                        .border(
                            1.5.dp,
                            if (character.heroicInspiration) v.accent else v.ruleStrong.copy(alpha = 0.6f),
                            CircleShape,
                        )
                        .clickable { handle.viewModel.toggleHeroicInspiration() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "✦",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (character.heroicInspiration) v.page else v.inkFaint,
                    )
                }
            }
        }
    }
}

/** One row of death save marks. */
@Composable
private fun DeathRow(
    label: String,
    marked: Int,
    tint: androidx.compose.ui.graphics.Color,
    onSet: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Caption(label, modifier = Modifier.width(78.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..3).forEach { index ->
                Pip(
                    filled = index <= marked,
                    tint = tint,
                    onClick = { onSet(if (marked == index) index - 1 else index) },
                )
            }
        }
    }
}

/**
 * A small round button in the sheet's own hand, for the two things a player taps constantly.
 *
 * Deliberately not a Material button: a filled slab in the middle of a page of paper is the
 * single loudest thing this design has to avoid.
 */
@Composable
private fun Nib(description: String, glyph: String, onClick: () -> Unit) {
    val v = LocalVellum.current
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(v.wellDeep.copy(alpha = if (v.isDark) 0.8f else 0.6f))
            .border(1.dp, v.rule, CircleShape)
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
            color = v.inkSoft,
        )
    }
}

/**
 * Attacks, ruled like the table they are on paper.
 *
 * Every attack the character has — weapons, unarmed, the ones features conjure, damage
 * cantrips, and anything written by hand — in one list, because in play they are one list.
 * Tapping a line opens what it does; the pencil at the head adds one of your own.
 */
@Composable
private fun AttacksLedger(handle: SheetHandle) {
    val v = LocalVellum.current
    val attacks = CharacterAttacks.all(handle.character)

    Leaf(
        title = tr("Attacks"),
        trailing = {
            PenMark(tr("Add attack")) { handle.editAttack(null) }
        },
    ) {
        LedgerHeader(
            listOf(
                tr("Name") to 1.6f,
                tr("Atk") to 0.5f,
                tr("Damage") to 0.9f,
                tr("Type") to 0.7f,
                tr("Notes") to 1.6f,
            )
        )

        attacks.forEachIndexed { index, attack ->
            LedgerRow(
                index,
                // Out of Edit Mode a tap opens the one the player wrote, which is where its
                // arithmetic lives. In Edit Mode a tap belongs to whichever cell it landed on.
                onClick = if (!handle.editMode && attack.isCustom) {
                    { handle.character.customAttacks.find { it.id == attack.id }
                        ?.let(handle::editAttack) }
                } else {
                    null
                },
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AttackCell(handle, attack, "name", attack.name, 1.6f, emphasis = true)
                        AttackCell(handle, attack, "bonus", attack.shownBonus, 0.5f, emphasis = true)
                        AttackCell(handle, attack, "damage", attack.damage, 0.9f)
                        AttackCell(handle, attack, "damageType", attack.damageType, 0.7f, soft = true)
                        AttackCell(handle, attack, "notes", attack.notes, 1.6f, soft = true)
                    }

                    if (attack.masteryProperty.isNotBlank()) {
                        Caption(trf("Mastery: {0}", attack.masteryProperty))
                    }

                    // Reordering and taking a line off the sheet change the sheet's shape
                    // rather than what happens at the table, so they are Edit Mode work.
                    if (handle.editMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            PenMark(tr("Move up"), glyph = "↑") {
                                handle.viewModel.moveAttack(attack.id, up = true)
                            }
                            PenMark(tr("Move down"), glyph = "↓") {
                                handle.viewModel.moveAttack(attack.id, up = false)
                            }
                            if (attack.isCustom) {
                                PenMark(tr("Edit"), glyph = "✎") {
                                    handle.character.customAttacks.find { it.id == attack.id }
                                        ?.let(handle::editAttack)
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            PenMark(
                                // A derived line comes back on the next read, so "delete"
                                // would be a lie for anything but a written one.
                                if (attack.isCustom) tr("Delete") else tr("Take off the sheet"),
                                glyph = "×",
                            ) { handle.viewModel.hideAttack(attack.id) }
                        }
                    }
                }
            }
        }

        // Hidden lines are listed in Edit Mode only: they are off the sheet on purpose, and
        // one hidden by mistake still has to be findable.
        val hidden = handle.character.hiddenAttackIds
        if (handle.editMode && hidden.isNotEmpty()) {
            Caption(tr("Hidden"))
            hidden.sorted().forEach { id ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SheetText(
                        text = handle.character.textOverrides["attack:$id:name"] ?: id,
                        soft = true,
                        modifier = Modifier.weight(1f),
                    )
                    PenMark(tr("Put back"), glyph = "+") { handle.viewModel.showAttack(id) }
                }
            }
        }

        if (attacks.isEmpty()) {
            SheetText(
                tr("Nothing to attack with yet. Add a weapon on the Inventory tab."),
                soft = true,
            )
        }
    }
}

/**
 * One cell of the attacks ledger, written over by an override keyed on the line.
 *
 * The same keys the phone uses, so a bonus rewritten on one shows up on the other — the sheet
 * is the character's, not the screen's.
 */
@Composable
private fun RowScope.AttackCell(
    handle: SheetHandle,
    attack: com.pedroeu.ficha.domain.AttackLine,
    field: String,
    value: String,
    weight: Float,
    emphasis: Boolean = false,
    soft: Boolean = false,
) {
    val key = "attack:${attack.id}:$field"
    InkField(
        value = value,
        editMode = handle.editMode,
        overridden = handle.character.textOverrides.containsKey(key),
        onEdit = { handle.editText(key, field, handle.character.textOverrides[key] ?: value) },
        emphasis = emphasis,
        soft = soft,
        modifier = Modifier.weight(weight),
    )
}

/**
 * The small mark that stands in for a button on this sheet: a glyph on the page, not a slab.
 */
@Composable
fun PenMark(description: String, glyph: String = "+", onClick: () -> Unit) {
    val v = LocalVellum.current
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(v.wellDeep.copy(alpha = if (v.isDark) 0.8f else 0.55f))
            .border(1.dp, v.rule, CircleShape)
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Serif),
            color = v.accent,
        )
    }
}
