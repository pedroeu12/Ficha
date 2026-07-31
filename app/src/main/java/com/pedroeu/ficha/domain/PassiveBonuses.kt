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
        val level = character.level

        fun scale(bonus: PassiveBonusData.Bonus) = AppliedBonus(
            label = bonus.label,
            target = bonus.target,
            amount = if (bonus.perLevel) bonus.amount * level else bonus.amount,
        )

        return buildList {
            PassiveBonusData.forSpecies(character.speciesId).forEach { add(scale(it)) }
            PassiveBonusData.forLineage(character.lineageId).forEach { add(scale(it)) }
            PassiveBonusData.forClass(character.classId).forEach { add(scale(it)) }
            character.featIds.forEach { featId ->
                PassiveBonusData.forFeat(featId).forEach { add(scale(it)) }
            }
        }
    }

    /** The total to add to one stat, and the sources that make it up. */
    fun totalFor(character: PlayerCharacter, target: Target): Int =
        all(character).filter { it.target == target }.sumOf { it.amount }

    fun sourcesFor(character: PlayerCharacter, target: Target): List<AppliedBonus> =
        all(character).filter { it.target == target }
}
