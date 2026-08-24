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
        CharacterHitDice.remaining(character)

    /** One Hit Die spent: which class's pool it came out of, and what it rolled. */
    data class DieSpent(val classId: String, val roll: Int)

    /**
     * Spends [diceSpent] Hit Dice. Each entry names the class whose die it was and what it
     * rolled; the Constitution modifier is added per die and a die never heals less than 1.
     *
     * Naming the class matters for a multiclassed character: their d10s and their d6s are
     * different pools, and spending one is not spending the other.
     */
    fun shortRest(character: PlayerCharacter, diceSpent: List<DieSpent>): RestOutcome {
        val conMod = CharacterCalculations.abilityModifiers(character)[Ability.CON] ?: 0
        val max = CharacterCalculations.maxHitPoints(character)

        // Take each die off its own pool, dropping any the character can no longer pay for.
        var spending = character
        val rolls = diceSpent.filter { die ->
            val before = CharacterHitDice.pools(spending).firstOrNull { it.classId == die.classId }
            if (before == null || before.remaining <= 0) return@filter false
            spending = CharacterHitDice.spendOne(spending, die.classId)
            true
        }

        val healing = rolls.sumOf { (it.roll + conMod).coerceAtLeast(1) }
        val newHp = (character.currentHitPoints + healing).coerceAtMost(max)
        val actuallyHealed = newHp - character.currentHitPoints

        var rested = spending.copy(currentHitPoints = newHp)

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
        (CharacterHitDice.totalDice(character) / 2).coerceAtLeast(1)

    fun longRest(character: PlayerCharacter): RestOutcome {
        val max = CharacterCalculations.maxHitPoints(character)
        val recovered = hitDiceRecoveredOnLongRest(character)

        val restored = CharacterResources.states(character)
            .filter { it.spent > 0 && it.def.recharge.refilledBy(Recharge.LONG_REST) }
            .map { it.def.name }

        var rested = CharacterHitDice.withRecovered(character, recovered).copy(
            currentHitPoints = max,
            temporaryHitPoints = 0,
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
