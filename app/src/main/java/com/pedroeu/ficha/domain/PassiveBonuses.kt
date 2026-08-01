package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.PassiveBonusData
import com.pedroeu.ficha.data.content.PassiveBonusData.Target

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

    fun all(character: PlayerCharacter): List<AppliedBonus> {
        /**
         * A per-level bonus multiplies by whichever level it belongs to. Species and feat
         * bonuses grow with the whole character; a class or subclass bonus grows only with
         * levels in that class, so a Sorcerer 5 / Fighter 3 gets five hit points from
         * Draconic Resilience rather than eight.
         */
        fun scale(bonus: PassiveBonusData.Bonus, level: Int) = AppliedBonus(
            label = bonus.label,
            target = bonus.target,
            amount = if (bonus.perLevel) bonus.amount * level else bonus.amount,
        )

        return buildList {
            val total = character.level
            PassiveBonusData.forSpecies(character.speciesId).forEach { add(scale(it, total)) }
            PassiveBonusData.forLineage(character.lineageId).forEach { add(scale(it, total)) }
            character.featIds.forEach { featId ->
                PassiveBonusData.forFeat(featId).forEach { add(scale(it, total)) }
            }

            // Every class the character has levels in, and the subclass attached to each.
            ClassLevels.of(character).forEach { entry ->
                PassiveBonusData.forClass(entry.classId)
                    .forEach { add(scale(it, entry.level)) }
                PassiveBonusData.forSubclass(entry.subclassId)
                    .forEach { add(scale(it, entry.level)) }
            }
        }
    }

    /** The total to add to one stat, and the sources that make it up. */
    fun totalFor(character: PlayerCharacter, target: Target): Int =
        all(character).filter { it.target == target }.sumOf { it.amount }

    fun sourcesFor(character: PlayerCharacter, target: Target): List<AppliedBonus> =
        all(character).filter { it.target == target }
}
