package com.pedroeu.ficha.ui.tablet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedroeu.ficha.domain.OverridableStat
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.ChoiceSection
import com.pedroeu.ficha.ui.components.StatEditDialog
import com.pedroeu.ficha.ui.components.TextEditDialog
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.i18n.trf
import com.pedroeu.ficha.ui.sheet.AddItemSheet
import com.pedroeu.ficha.ui.sheet.AttackEditorSheet
import com.pedroeu.ficha.ui.sheet.FeatPickerSheet
import com.pedroeu.ficha.ui.sheet.ItemDetailSheet
import com.pedroeu.ficha.ui.sheet.SheetViewModel
import com.pedroeu.ficha.ui.sheet.SpellDetailSheet
import com.pedroeu.ficha.ui.sheet.SpellPickerSheet
import com.pedroeu.ficha.ui.theme.LocalThemeController

/** The four leaves of the sheet, in the order the paper folds. */
private val LEAVES = listOf("Character", "Features", "Magic", "Gear & Story")

/**
 * The character sheet as a sheet of paper.
 *
 * Not the phone's screens rearranged. The page has no cards and casts no shadows; sections are
 * ruled off rather than boxed, numbers sit on lines rather than in containers, and every
 * counter — hit dice, spell slots, death saves, limited uses — is the same small circle you
 * would ink in with a pencil. What floats above the page is only ever a dialog, because a
 * dialog is genuinely a thing laid on top of a sheet.
 *
 * Every feature the phone has is here. The ones with real machinery behind them — the rest
 * system, the level-up flow, the pickers, Replicate Magic Item, the per-use choices — are the
 * same code the phone runs, opened from marks on the page instead of from Material buttons.
 */
@Composable
fun TabletSheetScreen(
    character: PlayerCharacter,
    viewModel: SheetViewModel,
    editMode: Boolean,
) {
    val dark = LocalThemeController.current.isDark
    val vellum = if (dark) CandlelightVellum else DaylightVellum

    var leaf by rememberSaveable { mutableStateOf(0) }
    var overlay by remember { mutableStateOf<SheetOverlay?>(null) }

    val handle = SheetHandle(
        character = character,
        viewModel = viewModel,
        editMode = editMode,
        open = { overlay = it },
    )

    CompositionLocalProvider(LocalVellum provides vellum) {
        Box(
            Modifier
                .fillMaxSize()
                .background(vellum.table)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .paperPage(vellum)
            ) {
                Masthead(handle)
                LeafTabs(leaf) { leaf = it }

                Box(Modifier.fillMaxSize()) {
                    when (leaf) {
                        0 -> LeafCharacter(handle)
                        1 -> LeafFeatures(handle)
                        2 -> LeafMagic(handle)
                        else -> LeafGear(handle)
                    }
                }
            }
        }
    }

    Overlays(overlay, handle) { overlay = null }
}

/**
 * The tabs at the head of the leaves, drawn as index tabs on the edge of the paper rather
 * than as a Material tab row.
 */
@Composable
private fun LeafTabs(selected: Int, onSelect: (Int) -> Unit) {
    val v = LocalVellum.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LEAVES.forEachIndexed { index, title ->
            val active = index == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(
                        if (active) v.well.copy(alpha = if (v.isDark) 0.95f else 0.85f)
                        else v.wellDeep.copy(alpha = if (v.isDark) 0.4f else 0.35f)
                    )
                    .border(
                        1.dp,
                        if (active) v.ruleStrong.copy(alpha = 0.5f) else v.rule,
                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Text(
                    text = tr(title).uppercase(),
                    style = EngravedLabel.copy(fontSize = 11.sp),
                    color = if (active) v.accent else v.inkFaint,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
    InkRule(strong = true)
}

/**
 * Everything the page can put on top of itself.
 *
 * One state, one place. Each branch is either the phone's own sheet — which is how the
 * hard requirement of no feature loss is actually met, rather than promised — or a dialog
 * built from the same shared components the phone uses.
 */
@Composable
private fun Overlays(overlay: SheetOverlay?, handle: SheetHandle, onDismiss: () -> Unit) {
    val character = handle.character
    val viewModel = handle.viewModel

    when (overlay) {
        null -> Unit

        is SheetOverlay.Stat -> StatEditDialog(
            title = tr(overlay.stat.label),
            rulesValue = handle.rulesValue(overlay.stat),
            currentBonus = character.statBonuses[overlay.stat.name],
            currentOverride = character.statOverrides[overlay.stat.name],
            onDismiss = onDismiss,
            onConfirm = { bonus, override ->
                viewModel.setStatBonus(overlay.stat, bonus)
                viewModel.setStatOverride(overlay.stat, override)
                onDismiss()
            },
        )

        is SheetOverlay.AbilityScore -> StatEditDialog(
            title = tr(overlay.ability.fullName),
            // The rules figure alone: base plus origin plus improvements, before anything
            // the player has pinned on top of it.
            rulesValue = (character.baseAbilityScores[overlay.ability.name] ?: 10) +
                (character.backgroundAbilityBonuses[overlay.ability.name] ?: 0) +
                (character.abilityScoreImprovements[overlay.ability.name] ?: 0),
            currentBonus = character.abilityScoreBonuses[overlay.ability.name],
            currentOverride = character.abilityScoreOverrides[overlay.ability.name],
            onDismiss = onDismiss,
            onConfirm = { bonus, override ->
                viewModel.setAbilityBonus(overlay.ability, bonus)
                viewModel.setAbilityScore(overlay.ability, override)
                onDismiss()
            },
            allowNegative = false,
        )

        is SheetOverlay.SavingThrow -> StatEditDialog(
            title = trf("{0} — saving throw", tr(overlay.ability.fullName)),
            rulesValue = com.pedroeu.ficha.domain.CharacterCalculations
                .savingThrowBonus(character, overlay.ability),
            currentBonus = character.saveBonuses[overlay.ability.name],
            currentOverride = character.saveOverrides[overlay.ability.name],
            onDismiss = onDismiss,
            onConfirm = { bonus, _ ->
                viewModel.setSaveBonus(overlay.ability, bonus)
                onDismiss()
            },
        )

        is SheetOverlay.SkillAdjust -> StatEditDialog(
            title = tr(overlay.skill.displayName),
            rulesValue = com.pedroeu.ficha.domain.CharacterCalculations
                .skillBonus(character, overlay.skill),
            currentBonus = character.skillBonuses[overlay.skill.name],
            currentOverride = character.skillOverrides[overlay.skill.name],
            onDismiss = onDismiss,
            onConfirm = { bonus, override ->
                viewModel.setSkillBonus(overlay.skill, bonus)
                viewModel.setSkillOverride(overlay.skill, override)
                onDismiss()
            },
        )

        is SheetOverlay.Text -> TextEditDialog(
            title = overlay.title,
            initial = overlay.initial,
            multiline = overlay.key != "character:name",
            onDismiss = onDismiss,
            onConfirm = { text ->
                when (overlay.key) {
                    "character:name" -> viewModel.setName(text.orEmpty())
                    "bio:appearance" -> viewModel.setAppearance(text.orEmpty())
                    "bio:backstory" -> viewModel.setBackstory(text.orEmpty())
                    "bio:alignment" -> viewModel.setAlignment(text.orEmpty())
                    "bio:notes" -> viewModel.setNotes(text.orEmpty())
                    // Everything else is a text override: a proficiency line rewritten, a
                    // resistance written in, a feature's wording changed by the table.
                    else -> viewModel.setText(overlay.key, text)
                }
                onDismiss()
            },
        )

        is SheetOverlay.Spell -> SpellDetailSheet(overlay.spell, onDismiss)

        is SheetOverlay.Item -> character.inventory.getOrNull(overlay.index)?.let { item ->
            ItemDetailSheet(
                item = item,
                onDismiss = onDismiss,
                editMode = handle.editMode,
                onRename = { viewModel.updateInventoryItem(overlay.index, item.copy(name = it)) },
                onNotesChange = {
                    viewModel.updateInventoryItem(overlay.index, item.copy(notes = it))
                },
            )
        }

        is SheetOverlay.Attack -> AttackEditorSheet(
            character = character,
            existing = overlay.existing,
            onDismiss = onDismiss,
            onSave = { attack ->
                if (overlay.existing == null) viewModel.addCustomAttack(attack)
                else viewModel.updateCustomAttack(attack)
                onDismiss()
            },
        )

        SheetOverlay.AddSpell -> SpellPickerSheet(
            character = character,
            onDismiss = onDismiss,
            onAdd = {
                viewModel.addSpell(it)
                onDismiss()
            },
        )

        SheetOverlay.AddItem -> AddItemSheet(
            onDismiss = onDismiss,
            onAdd = {
                viewModel.addInventoryItem(it)
                onDismiss()
            },
        )

        SheetOverlay.AddFeat -> FeatPickerSheet(
            character = character,
            onDismiss = onDismiss,
            onAdd = { featId, selections ->
                viewModel.addFeat(featId, selections)
                onDismiss()
            },
        )

        SheetOverlay.AddTool -> TextEditDialog(
            title = tr("Add a tool or proficiency"),
            initial = "",
            onDismiss = onDismiss,
            onConfirm = { text ->
                if (!text.isNullOrBlank()) viewModel.addToolProficiency(text)
                onDismiss()
            },
        )

        SheetOverlay.AddFeature -> TextEditDialog(
            title = tr("New feature"),
            initial = "",
            multiline = true,
            onDismiss = onDismiss,
            onConfirm = { text ->
                if (!text.isNullOrBlank()) {
                    val name = text.lineSequence().first().trim()
                    val body = text.lineSequence().drop(1).joinToString("\n").trim()
                    viewModel.addCustomFeature(name, body, "Custom")
                }
                onDismiss()
            },
        )

        is SheetOverlay.EditChoice -> {
            val resolved = overlay.resolved
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(resolved.choice.label) },
                text = {
                    ChoiceSection(
                        choice = resolved.choice,
                        selected = resolved.selectedIds,
                        onToggle = { optionId ->
                            val current = resolved.selectedIds
                            val next = when {
                                current.contains(optionId) -> current - optionId
                                current.size < resolved.choice.count -> current + optionId
                                resolved.choice.count == 1 -> listOf(optionId)
                                else -> current.drop(1) + optionId
                            }
                            viewModel.setChoiceSelection(resolved.choice.id, resolved.level, next)
                        },
                    )
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Done")) } },
            )
        }
    }
}
