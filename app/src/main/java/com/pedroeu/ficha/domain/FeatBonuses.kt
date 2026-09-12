package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.FeatChoiceData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.model.Ability

/** One ability score increase a feat hands over, and the feat it came from. */
data class FeatAbilityBonus(
    val featId: String,
    val featName: String,
    val ability: Ability,
    val amount: Int = 1,
    /** The highest the score can go this way — 20 normally, 30 for an Epic Boon. */
    val cap: Int,
)

/**
 * The ability score increases that come with a feat.
 *
 * Almost every feat from level 4 on raises a score by 1, and the app was recording the feat
 * without ever applying that. Where the feat names a single ability the increase is simply
 * derived; where it offers a choice, the player's answer is read back out of the choice they
 * were asked. Both go through here, so the sheet's numbers match the feats listed on it.
 *
 * Derived rather than written into the character, like every other grant, so a sheet built
 * before this existed picks up the increases it should always have had.
 */
object FeatBonuses {

    /** The ability increases the character's feats add, one entry per feat. */
    fun all(character: PlayerCharacter): List<FeatAbilityBonus> =
        CharacterFeats.heldBy(character).distinct().mapNotNull { featId ->
            val options = FeatChoiceData.ABILITY_OPTIONS[featId] ?: return@mapNotNull null
            val name = FeatData.byId(featId)?.name ?: featId
            val cap = if (featId.startsWith("boon_")) 30 else 20

            val ability = if (options.size == 1) {
                // No decision to make, so it applies whether or not anyone was asked.
                options.first()
            } else {
                // The player was asked; if they haven't answered yet, nothing applies.
                val picked = ChoiceResolver
                    .selectionsFor(character, "feat:$featId:ability")
                    .firstOrNull()
                    ?: return@mapNotNull null
                options.find { it.name == picked } ?: return@mapNotNull null
            }

            FeatAbilityBonus(featId, name, ability, cap = cap)
        }

    /** Totals per ability, for folding into the final scores. */
    fun byAbility(character: PlayerCharacter): Map<Ability, Int> =
        all(character).groupBy { it.ability }.mapValues { (_, list) -> list.sumOf { it.amount } }

    /** The highest a score may reach given the feats raising it. */
    fun capFor(character: PlayerCharacter, ability: Ability): Int =
        all(character).filter { it.ability == ability }.maxOfOrNull { it.cap } ?: 20
}
