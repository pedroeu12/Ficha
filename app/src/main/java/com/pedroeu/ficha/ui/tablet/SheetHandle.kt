package com.pedroeu.ficha.ui.tablet

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.domain.CustomAttack
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.OverridableStat
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.sheet.SheetViewModel

/**
 * Something the sheet wants to lay on top of the page: a dialog, a picker, a detail.
 *
 * Named as intent rather than as a boolean per overlay. The tablet sheet has a lot of things
 * that can be opened and only ever one of them open at a time, and a dozen independent flags
 * is how two of them end up open at once.
 */
sealed interface SheetOverlay {
    data class AbilityScore(val ability: Ability) : SheetOverlay
    data class SavingThrow(val ability: Ability) : SheetOverlay
    data class SkillAdjust(val skill: Skill) : SheetOverlay
    data class Stat(val stat: OverridableStat) : SheetOverlay
    data class Spell(val spell: KnownSpell) : SheetOverlay
    data class Item(val index: Int) : SheetOverlay
    data class Attack(val existing: CustomAttack?) : SheetOverlay
    data class Text(val key: String, val title: String, val initial: String) : SheetOverlay
    data object AddSpell : SheetOverlay
    data object AddItem : SheetOverlay
    data object AddFeat : SheetOverlay
    data object AddFeature : SheetOverlay
    data object AddTool : SheetOverlay
    data class EditChoice(val resolved: com.pedroeu.ficha.domain.ResolvedChoice) : SheetOverlay
}

/**
 * What the paper components are allowed to do.
 *
 * The sheet's own composables are drawings — a ruled field, an ability block, a ledger row —
 * and drawings shouldn't know about a view model or hold dialog state. They get this instead:
 * every action the page offers, in the vocabulary of the page rather than of the storage
 * underneath it. The screen that hosts them owns both the view model and the one overlay.
 */
class SheetHandle(
    val character: PlayerCharacter,
    val viewModel: SheetViewModel,
    val editMode: Boolean,
    val open: (SheetOverlay) -> Unit,
) {

    // ------------------------------------------------------------------ Opening things

    fun editAbility(ability: Ability) = open(SheetOverlay.AbilityScore(ability))
    fun editSave(ability: Ability) = open(SheetOverlay.SavingThrow(ability))
    fun editSkill(skill: Skill) = open(SheetOverlay.SkillAdjust(skill))
    fun editStat(stat: OverridableStat) = open(SheetOverlay.Stat(stat))
    fun openSpell(spell: KnownSpell) = open(SheetOverlay.Spell(spell))
    fun openItem(index: Int) = open(SheetOverlay.Item(index))
    fun editAttack(attack: CustomAttack?) = open(SheetOverlay.Attack(attack))
    fun editText(key: String, title: String, initial: String) =
        open(SheetOverlay.Text(key, title, initial))

    // ------------------------------------------------------------------ Marking the page

    fun toggleSaveProficiency(ability: Ability) = viewModel.toggleSaveProficiency(ability)

    /**
     * One tap on a skill's mark cycles none → proficient → expertise → none.
     *
     * Expertise implies proficiency, so leaving it takes two steps in the model even though
     * it is one tap on the page.
     */
    fun cycleSkill(skill: Skill) {
        when {
            skill.name in character.skillExpertise -> {
                viewModel.toggleSkillExpertise(skill)
                viewModel.toggleSkillProficiency(skill)
            }
            skill.name in character.skillProficiencies -> viewModel.toggleSkillExpertise(skill)
            else -> viewModel.toggleSkillProficiency(skill)
        }
    }

    /** The rules value behind an overridable stat, so a dialog can show what it is replacing. */
    fun rulesValue(stat: OverridableStat): Int = com.pedroeu.ficha.domain.CharacterCalculations
        .let { calc ->
            when (stat) {
                OverridableStat.MAX_HIT_POINTS -> calc.maxHitPoints(character)
                OverridableStat.ARMOR_CLASS -> calc.armorClass(character)
                OverridableStat.INITIATIVE -> calc.initiative(character)
                OverridableStat.SPEED -> calc.speed(character)
                OverridableStat.PROFICIENCY_BONUS -> calc.proficiencyBonus(character)
                OverridableStat.PASSIVE_PERCEPTION -> calc.passivePerception(character)
                OverridableStat.SPELL_SAVE_DC -> calc.spellSaveDc(character) ?: 0
                OverridableStat.SPELL_ATTACK_BONUS -> calc.spellAttackBonus(character) ?: 0
                OverridableStat.MAX_PREPARED_SPELLS -> calc.maxPreparedSpells(character)
                OverridableStat.CANTRIPS_KNOWN -> calc.maxCantripsKnown(character)
            }
        }

    /** True when the player has pinned or nudged this number by hand. */
    fun isAdjusted(stat: OverridableStat): Boolean =
        character.statOverrides.containsKey(stat.name) ||
            character.statBonuses.containsKey(stat.name)
}
