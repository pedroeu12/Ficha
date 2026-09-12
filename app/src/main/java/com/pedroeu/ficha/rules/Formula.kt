package com.pedroeu.ficha.rules

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.FeatBonuses
import com.pedroeu.ficha.domain.PlayerCharacter

/**
 * Which level a number counts against.
 *
 * The distinction the app kept re-deriving, differently, in five places. A subclass's spell
 * list advances on the level in *its* class, so a Cleric 3 / Fighter 9 has a level 3 domain
 * list; a species trait and a feat count the character's total instead. Getting this wrong is
 * how a Warlock 5 / Fighter 6 came to be offered level 11 invocations.
 */
enum class LevelScope { OWNING_CLASS, CHARACTER }

/**
 * An ability named by a rule — either fixed, or whichever one an earlier answer chose.
 *
 * [ChosenIn] is the piece that used to need bespoke code. Infernal Bulwark's Armor Class is
 * "10 plus your Dexterity modifier plus the modifier of the ability increased by this feat",
 * and *which* ability that is only exists as an answer the player gave. That was a one-off
 * patch; here it is a field any rule can use.
 */
sealed interface AbilityRef {
    data class Fixed(val ability: Ability) : AbilityRef
    data class ChosenIn(val choiceId: String) : AbilityRef

    /**
     * "The ability increased by this feat", written the way the rules write it.
     *
     * Not the same as [ChosenIn]: a feat that raises one named score asks nothing, so there is
     * no answer to look up, and the rule still has an ability. Going through the feat means
     * both shapes work and neither needs the caller to know which it is.
     */
    data class RaisedByFeat(val featId: String) : AbilityRef

    /** Null when the choice hasn't been answered yet, which reads as "no bonus yet". */
    fun resolve(character: PlayerCharacter): Ability? = when (this) {
        is Fixed -> ability
        is ChosenIn -> ChoiceResolver
            .latestSelectionFor(character, choiceId)
            .firstOrNull()
            ?.let { id -> Ability.ALL.find { it.name == id } }
        is RaisedByFeat -> FeatBonuses.all(character)
            .firstOrNull { it.featId == featId }
            ?.ability
    }
}

/**
 * A number a rule computes rather than states.
 *
 * "Uses equal to your Proficiency Bonus", "Hit Points equal to four times your Warlock level",
 * "AC 13 plus your Charisma modifier" — all of these were hand-computed integers written out
 * per level, or arithmetic buried in whichever function happened to need it. A formula is
 * data, so the same expression serves a resource maximum, a summon's hit points, and a bonus
 * to Armor Class without being written three times.
 */
sealed interface Formula {

    data class Flat(val amount: Int) : Formula

    /** [amount] per level, counted against [scope]. */
    data class PerLevel(
        val amount: Int = 1,
        val scope: LevelScope = LevelScope.CHARACTER,
    ) : Formula

    data class AbilityMod(val ability: AbilityRef) : Formula

    data class ProficiencyBonus(val times: Int = 1) : Formula

    data class Sum(val parts: List<Formula>) : Formula

    /** The larger of two, for "whichever is higher" wording. */
    data class Larger(val parts: List<Formula>) : Formula

    /** "…(minimum of one)", which the rules attach to most ability-modifier counts. */
    data class AtLeast(val of: Formula, val floor: Int) : Formula

    /**
     * The level of the spell slot spent, for a summon that grows with it.
     *
     * Every 2024 "Summon" spell scales this way — a Bestial Spirit has "5 + 5 per spell level"
     * Hit Points — and it is the reason a summoned creature has to be computed rather than
     * printed on a card. Zero outside a spell, which reads as no contribution.
     */
    data object SpellLevel : Formula

    data class PerSpellLevel(val amount: Int) : Formula

    /**
     * A number that steps up at set levels rather than growing evenly.
     *
     * A Monk's Unarmored Movement is 10 feet at level 2 and 30 at level 18, with three stops
     * in between and nothing linear about it. Written as a step table it is the same kind of
     * data as everything else here; written as arithmetic it was a `when` block inside the
     * Speed calculation that only that calculation could see.
     */
    data class AtLevels(
        /** level to value, in any order. The highest level reached wins. */
        val steps: List<Pair<Int, Int>>,
        val scope: LevelScope = LevelScope.OWNING_CLASS,
    ) : Formula

    companion object {
        val ZERO: Formula = Flat(0)
        fun of(vararg parts: Formula): Formula = Sum(parts.toList())
    }
}

/** Works a [Formula] out for a character, in the context of one class where that matters. */
object FormulaEval {

    fun eval(
        formula: Formula,
        character: PlayerCharacter,
        /** The class whose level [LevelScope.OWNING_CLASS] means, when there is one. */
        owningClassId: String? = null,
        /** The level of the slot spent, for a summon that scales with it. */
        spellLevel: Int = 0,
    ): Int = when (formula) {
        is Formula.Flat -> formula.amount

        is Formula.PerLevel -> formula.amount * levelFor(character, formula.scope, owningClassId)

        is Formula.AbilityMod -> formula.ability.resolve(character)?.let { ability ->
            CharacterCalculations.abilityModifiers(character)[ability] ?: 0
        } ?: 0

        is Formula.ProficiencyBonus ->
            formula.times * CharacterCalculations.proficiencyBonus(character)

        is Formula.Sum -> formula.parts.sumOf { eval(it, character, owningClassId, spellLevel) }

        is Formula.Larger ->
            formula.parts.maxOfOrNull { eval(it, character, owningClassId, spellLevel) } ?: 0

        is Formula.AtLeast ->
            eval(formula.of, character, owningClassId, spellLevel).coerceAtLeast(formula.floor)

        is Formula.SpellLevel -> spellLevel

        is Formula.PerSpellLevel -> formula.amount * spellLevel

        is Formula.AtLevels -> {
            val level = levelFor(character, formula.scope, owningClassId)
            formula.steps.filter { level >= it.first }.maxOfOrNull { it.second } ?: 0
        }
    }

    fun levelFor(
        character: PlayerCharacter,
        scope: LevelScope,
        owningClassId: String?,
    ): Int = when (scope) {
        LevelScope.CHARACTER -> character.level
        LevelScope.OWNING_CLASS ->
            owningClassId?.let { ClassLevels.levelIn(character, it) } ?: character.level
    }

    /**
     * How a formula reads on the sheet, for a tooltip or a tracker's caption.
     *
     * Worth having because a number with no explanation is the thing players mistrust: "5"
     * says nothing, "Proficiency Bonus" says where to look when it changes.
     */
    fun describe(formula: Formula): String = when (formula) {
        is Formula.Flat -> formula.amount.toString()
        is Formula.PerLevel -> if (formula.amount == 1) "your level" else "${formula.amount} × your level"
        is Formula.AbilityMod -> when (val a = formula.ability) {
            is AbilityRef.Fixed -> "your ${a.ability.fullName} modifier"
            is AbilityRef.ChosenIn -> "your chosen ability's modifier"
            is AbilityRef.RaisedByFeat -> "the modifier of the ability the feat raised"
        }
        is Formula.ProficiencyBonus ->
            if (formula.times == 1) "your Proficiency Bonus" else "${formula.times} × your Proficiency Bonus"
        is Formula.Sum -> formula.parts.joinToString(" + ") { describe(it) }
        is Formula.Larger -> formula.parts.joinToString(" or ") { describe(it) } + ", whichever is higher"
        is Formula.AtLeast -> describe(formula.of) + " (minimum ${formula.floor})"
        is Formula.SpellLevel -> "the spell's level"
        is Formula.PerSpellLevel -> "${formula.amount} per spell level"
        is Formula.AtLevels -> formula.steps
            .sortedBy { it.first }
            .joinToString(", ") { "${it.second} from level ${it.first}" }
    }
}
