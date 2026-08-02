package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.CasterType
import com.pedroeu.ficha.data.model.Recharge
import kotlin.random.Random

/** What a rest actually did, so the sheet can report it rather than silently changing numbers. */
data class RestOutcome(
    val character: PlayerCharacter,
    val hitPointsRegained: Int = 0,
    val hitDiceSpent: Int = 0,
    val resourcesRestored: List<String> = emptyList(),
    val spellSlotsRestored: Boolean = false,
)

/**
 * Short and Long Rest handling.
 *
 * A Short Rest heals only what the player chooses to spend Hit Dice on and returns
 * short-rest resources — including a Warlock's Pact Magic slots, which is the one case where
 * spell slots come back without a Long Rest. A Long Rest restores everything and gives back
 * half the character's total Hit Dice.
 */
object RestEngine {

    /** The result of rolling one Hit Die, before the Constitution modifier is applied. */
    fun rollHitDie(hitDie: Int, random: Random = Random): Int = random.nextInt(1, hitDie + 1)

    fun availableHitDice(character: PlayerCharacter): Int =
        (character.level - character.hitDiceSpent).coerceAtLeast(0)

    /**
     * Spends [diceRolls] Hit Dice. Each entry is a die result; the Constitution modifier is
     * added per die and a die never heals less than 1.
     */
    fun shortRest(character: PlayerCharacter, diceRolls: List<Int>): RestOutcome {
        val available = availableHitDice(character)
        val rolls = diceRolls.take(available)
        val conMod = CharacterCalculations.abilityModifiers(character)[Ability.CON] ?: 0
        val max = CharacterCalculations.maxHitPoints(character)

        val healing = rolls.sumOf { (it + conMod).coerceAtLeast(1) }
        val newHp = (character.currentHitPoints + healing).coerceAtMost(max)
        val actuallyHealed = newHp - character.currentHitPoints

        var rested = character.copy(
            currentHitPoints = newHp,
            hitDiceSpent = character.hitDiceSpent + rolls.size,
        )

        val restored = CharacterResources.byRecharge(rested, Recharge.SHORT_REST)
            .filter { it.spent > 0 }
            .map { it.def.name }
        rested = CharacterResources.withRestored(rested, Recharge.SHORT_REST)
        // Nothing chosen at the moment of use outlasts a rest — a cannon burns out after an
        // hour, a Celestial Revelation after a minute — so none of those picks survive one.
        rested = PerUseChoices.clearAll(rested)

        // Pact Magic is the one spellcasting that refreshes on a Short Rest.
        val isPactCaster = CharacterCalculations.casterType(rested) == CasterType.PACT
        if (isPactCaster) rested = rested.copy(spellSlotsExpended = emptyMap())

        return RestOutcome(
            character = rested,
            hitPointsRegained = actuallyHealed,
            hitDiceSpent = rolls.size,
            resourcesRestored = restored,
            spellSlotsRestored = isPactCaster,
        )
    }

    /** Hit Dice recovered by a Long Rest: half your total, at least one. */
    fun hitDiceRecoveredOnLongRest(character: PlayerCharacter): Int =
        (character.level / 2).coerceAtLeast(1)

    fun longRest(character: PlayerCharacter): RestOutcome {
        val max = CharacterCalculations.maxHitPoints(character)
        val recovered = hitDiceRecoveredOnLongRest(character)

        val restored = CharacterResources.states(character)
            .filter { it.spent > 0 && it.def.recharge.refilledBy(Recharge.LONG_REST) }
            .map { it.def.name }

        var rested = character.copy(
            currentHitPoints = max,
            temporaryHitPoints = 0,
            hitDiceSpent = (character.hitDiceSpent - recovered).coerceAtLeast(0),
            deathSaves = DeathSaves(),
            spellSlotsExpended = emptyMap(),
        )
        rested = CharacterResources.withRestored(rested, Recharge.LONG_REST)
        rested = PerUseChoices.clearAll(rested)

        return RestOutcome(
            character = rested,
            hitPointsRegained = max - character.currentHitPoints,
            hitDiceSpent = 0,
            resourcesRestored = restored,
            spellSlotsRestored = true,
        )
    }
}
