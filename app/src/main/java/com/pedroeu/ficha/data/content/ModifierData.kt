package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.rules.AbilityRef
import com.pedroeu.ficha.rules.Condition
import com.pedroeu.ficha.rules.Effect
import com.pedroeu.ficha.rules.Formula
import com.pedroeu.ficha.rules.LevelScope
import com.pedroeu.ficha.rules.StatTarget

/**
 * Numbers a feature changes only under some circumstance.
 *
 * [PassiveBonusData] holds the ones that always apply; these are the ones that used to be
 * exempt from being data at all. A Barbarian's Unarmored Defense, a Monk's Speed, the Alert
 * feat's Initiative — each lived as a line inside whichever calculation happened to need it,
 * which meant the sheet's Armor Class knew about five features by name and nothing else in
 * the app knew about any of them. Adding the sixth meant editing that function, and getting
 * one of the five subtly wrong went unnoticed because there was nothing to compare against.
 *
 * Keyed by the rules engine's id for the feature that grants it, so the level it arrives at,
 * the class it counts against and the subclass or feat it depends on all come from the
 * feature itself rather than being restated here. A wrong key grants nothing and a test
 * checks every key resolves.
 */
object ModifierData {

    private val DEX = Formula.AbilityMod(AbilityRef.Fixed(Ability.DEX))

    /** "Your base Armor Class equals 10 plus your Dexterity and X modifiers." */
    private fun unarmoredDefense(
        label: String,
        second: Ability,
        condition: Condition,
    ): Effect = Effect.SetStatBase(
        target = StatTarget.ARMOR_CLASS,
        amount = Formula.of(Formula.Flat(10), DEX, Formula.AbilityMod(AbilityRef.Fixed(second))),
        label = label,
        condition = condition,
    )

    /** The wording three features share: no armor *and* no Shield. */
    private val UNARMORED_AND_NO_SHIELD = Condition.all(Condition.Unarmored, Condition.NoShield)

    private val BY_ELEMENT: Map<String, List<Effect>> = mapOf(

        // ---------------------------------------------------------- Unarmored Defense
        // The Barbarian's is the one that allows a Shield; the rest say so or don't.

        "class:barbarian:1:unarmored_defense" to listOf(
            unarmoredDefense("Unarmored Defense", Ability.CON, Condition.Unarmored),
        ),
        "class:monk:1:unarmored_defense" to listOf(
            unarmoredDefense("Unarmored Defense", Ability.WIS, UNARMORED_AND_NO_SHIELD),
        ),
        "subclass:draconic:3:draconic_resilience" to listOf(
            unarmoredDefense("Draconic Resilience", Ability.CHA, Condition.Unarmored),
        ),
        "subclass:dance:3:dazzling_footwork" to listOf(
            unarmoredDefense("Dazzling Footwork", Ability.CHA, UNARMORED_AND_NO_SHIELD),
        ),
        "subclass:noble_genies:3:genie_s_splendor" to listOf(
            // "You can use a Shield and still gain this benefit", so no Shield clause.
            unarmoredDefense("Genie's Splendor", Ability.CHA, Condition.Unarmored),
        ),
        "feat:infernal_bulwark" to listOf(
            Effect.SetStatBase(
                target = StatTarget.ARMOR_CLASS,
                // "…plus the modifier of the ability increased by this feat", which is
                // Constitution or Charisma depending on an answer the player gave.
                amount = Formula.of(
                    Formula.Flat(10),
                    DEX,
                    Formula.AbilityMod(AbilityRef.RaisedByFeat("infernal_bulwark")),
                ),
                label = "Devil's Flesh",
                condition = UNARMORED_AND_NO_SHIELD,
            ),
        ),

        // ---------------------------------------------------------- Speed

        "class:barbarian:5:fast_movement" to listOf(
            Effect.ModifyStat(
                target = StatTarget.SPEED,
                amount = Formula.Flat(10),
                label = "Fast Movement",
                condition = Condition.NotInHeavyArmor,
            ),
        ),
        "class:monk:2:unarmored_movement" to listOf(
            Effect.ModifyStat(
                target = StatTarget.SPEED,
                // The Monk table's own column, which stops at 30 rather than growing evenly.
                amount = Formula.AtLevels(
                    listOf(2 to 10, 6 to 15, 10 to 20, 14 to 25, 18 to 30),
                    LevelScope.OWNING_CLASS,
                ),
                label = "Unarmored Movement",
                condition = UNARMORED_AND_NO_SHIELD,
            ),
        ),
        "class:ranger:6:roving" to listOf(
            Effect.ModifyStat(
                target = StatTarget.SPEED,
                amount = Formula.Flat(10),
                label = "Roving",
            ),
        ),
        "lineage:wood_elf" to listOf(
            Effect.SetStatBase(
                target = StatTarget.SPEED,
                amount = Formula.Flat(35),
                label = "Fleet of Foot",
            ),
        ),

        // ---------------------------------------------------------- Initiative

        "subclass:gloom_stalker:3:dread_ambusher" to listOf(
            // "Initiative Bonus. When you roll Initiative, you can add your Wisdom modifier
            // to the roll." Printed in the middle of a feature that also does three other
            // things, which is how it went four versions without ever reaching the number.
            Effect.ModifyStat(
                target = StatTarget.INITIATIVE,
                amount = Formula.AbilityMod(AbilityRef.Fixed(Ability.WIS)),
                label = "Dread Ambusher",
            ),
        ),
        "feat:alert" to listOf(
            Effect.ModifyStat(
                target = StatTarget.INITIATIVE,
                amount = Formula.ProficiencyBonus(),
                label = "Initiative Proficiency",
            ),
        ),
    )

    /**
     * Modifiers carried by an *option* rather than by the feature offering it.
     *
     * Keyed by the option's own id, because the same option is offered by several questions:
     * the Defense Fighting Style appears under a Fighter's choice at level 1, a Champion's
     * second at level 7 and a handful of others, and it is the same +1 each time. Keying it to
     * one of those questions would have applied it to a Fighter and not to a Champion.
     */
    private val BY_OPTION: Map<String, List<Effect>> = mapOf(
        "defense" to listOf(
            // The most-picked Fighting Style in the game, and its +1 reached nothing: the
            // option was recorded, printed in the feature list, and never added to Armor Class.
            Effect.ModifyStat(
                target = StatTarget.ARMOR_CLASS,
                amount = Formula.Flat(1),
                label = "Defense",
                condition = Condition.WearingArmor,
            ),
        ),
    )

    fun forElement(elementId: String): List<Effect> = BY_ELEMENT[elementId].orEmpty()

    fun forOption(optionId: String): List<Effect> = BY_OPTION[optionId].orEmpty()

    fun optionIds(): Set<String> = BY_OPTION.keys

    /** Every key, so a test can check each one names a feature that exists. */
    fun elementIds(): Set<String> = BY_ELEMENT.keys

    fun all(): List<Effect> = BY_ELEMENT.values.flatten() + BY_OPTION.values.flatten()
}
