package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.PassiveBonusData
import com.pedroeu.ficha.data.content.PassiveBonusData.Target
import com.pedroeu.ficha.rules.Condition
import com.pedroeu.ficha.rules.Effect
import com.pedroeu.ficha.rules.FormulaEval
import com.pedroeu.ficha.rules.RulesEngine
import com.pedroeu.ficha.rules.StatTarget

/** One passive bonus in effect, with the source worth naming on the sheet. */
data class AppliedBonus(
    val label: String,
    val target: Target,
    val amount: Int,
)

/**
 * Passive numeric bonuses gathered from every source the character has.
 *
 * These used to live inside whichever calculation happened to know about them, which meant a
 * new one only counted if someone remembered to add it there — that's how a Warforged ended
 * up describing +1 AC in a trait without it ever reaching the number.
 */
object PassiveBonuses {

    /**
     * Read from the rules engine rather than gathered here.
     *
     * This was the first of the nine tables to be switched over, because it is small enough to
     * check by eye and because the switch caught a real difference: the engine's first pass
     * scaled a per-level class bonus by the character's level instead of the class's, so a
     * Sorcerer 5 / Fighter 3 gained eight hit points from Draconic Resilience rather than
     * five. Having one implementation is what makes a difference like that a failing test
     * instead of a second answer nobody compares.
     */
    fun all(character: PlayerCharacter): List<AppliedBonus> =
        RulesEngine.view<Effect.ModifyStat>(character)
            .filter { it.effect.condition == Condition.Always }
            .mapNotNull { applied ->
                val target = targetOf(applied.effect.target) ?: return@mapNotNull null
                AppliedBonus(
                    label = applied.effect.label,
                    target = target,
                    amount = FormulaEval.eval(
                        applied.effect.amount,
                        character,
                        applied.element.source.owningClassId,
                    ),
                )
            }

    /** Only the targets this older type can name; the engine knows more than it does. */
    private fun targetOf(target: StatTarget): Target? = when (target) {
        StatTarget.ARMOR_CLASS -> Target.ARMOR_CLASS
        StatTarget.MAX_HIT_POINTS -> Target.MAX_HIT_POINTS
        StatTarget.SPEED -> Target.SPEED
        StatTarget.INITIATIVE -> Target.INITIATIVE
        StatTarget.ALL_SAVES -> Target.ALL_SAVES
        else -> null
    }

    /** The total to add to one stat, and the sources that make it up. */
    fun totalFor(character: PlayerCharacter, target: Target): Int =
        all(character).filter { it.target == target }.sumOf { it.amount }

    fun sourcesFor(character: PlayerCharacter, target: Target): List<AppliedBonus> =
        all(character).filter { it.target == target }
}
