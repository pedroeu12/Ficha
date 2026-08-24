package com.pedroeu.ficha

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterHitDice
import com.pedroeu.ficha.domain.ClassLevel
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.RestEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hit Dice, one pool per class.
 *
 * A Fighter 5 / Wizard 3 has 5d10 and 3d6 — not eight of anything. The sheet used to show
 * eight dice and roll a d10 for every one of them, which handed a multiclassed character the
 * best die in their build on every Short Rest.
 */
class HitDiceTest {

    private fun fighterWizard(fighter: Int = 5, wizard: Int = 3) = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = "fighter",
        subclassId = "champion", backgroundId = "soldier", level = fighter + wizard,
        classLevels = listOf(
            ClassLevel("fighter", fighter, "champion", isStarting = true),
            ClassLevel("wizard", wizard, "evoker"),
        ),
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
    )

    private fun fighter(level: Int = 5) = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = "fighter",
        subclassId = "champion", backgroundId = "soldier", level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
    )

    // ------------------------------------------------------------------ The pools

    @Test
    fun `each class brings its own die`() {
        val pools = CharacterHitDice.pools(fighterWizard())
        assertEquals(2, pools.size)
        // Largest die first: the one you would reach for is the one printed first.
        assertEquals(listOf(10, 6), pools.map { it.die })
        assertEquals(listOf(5, 3), pools.map { it.total })
        assertEquals("5d10 + 3d6", ClassLevels.hitDiceLabel(fighterWizard()))
    }

    @Test
    fun `a single-class character has one pool and reads as it always did`() {
        val pools = CharacterHitDice.pools(fighter(level = 5))
        assertEquals(1, pools.size)
        assertEquals(10, pools.single().die)
        assertEquals(5, pools.single().total)
        assertEquals(5, CharacterHitDice.remaining(fighter(level = 5)))
    }

    @Test
    fun `a sheet saved before this existed keeps its spent count`() {
        // One number and no record of which class it came from, so it comes off in class
        // order — which is the same answer for the single-class character it will be.
        val old = fighter(level = 5).copy(hitDiceSpent = 2)
        assertEquals(3, CharacterHitDice.remaining(old))
        assertEquals(2, CharacterHitDice.pools(old).single().spent)
    }

    // ------------------------------------------------------------------ Spending

    @Test
    fun `spending one pool leaves the other alone`() {
        var character = fighterWizard()
        repeat(5) { character = CharacterHitDice.spendOne(character, "fighter") }

        val pools = CharacterHitDice.pools(character).associateBy { it.classId }
        assertEquals("every d10 is gone", 0, pools["fighter"]!!.remaining)
        assertEquals("the d6s are untouched", 3, pools["wizard"]!!.remaining)
        assertEquals(3, CharacterHitDice.remaining(character))
    }

    @Test
    fun `a pool cannot go past its own levels`() {
        var character = fighterWizard()
        repeat(20) { character = CharacterHitDice.spendOne(character, "wizard") }
        assertEquals(0, CharacterHitDice.pools(character).first { it.classId == "wizard" }.remaining)
        assertEquals("the Fighter's dice are still there", 5, CharacterHitDice.remaining(character))
    }

    // ------------------------------------------------------------------ Resting

    @Test
    fun `a short rest takes each die off the pool it came from`() {
        val character = fighterWizard().copy(currentHitPoints = 1)
        val outcome = RestEngine.shortRest(
            character,
            listOf(RestEngine.DieSpent("fighter", 8), RestEngine.DieSpent("wizard", 4)),
        )

        assertEquals(2, outcome.hitDiceSpent)
        val pools = CharacterHitDice.pools(outcome.character).associateBy { it.classId }
        assertEquals(1, pools["fighter"]!!.spent)
        assertEquals(1, pools["wizard"]!!.spent)
        // Constitution 14 is a +2, applied to each die.
        assertEquals((8 + 2) + (4 + 2), outcome.hitPointsRegained)
    }

    @Test
    fun `a short rest refuses dice the character does not have`() {
        var character = fighterWizard().copy(currentHitPoints = 1)
        repeat(3) { character = CharacterHitDice.spendOne(character, "wizard") }

        val outcome = RestEngine.shortRest(
            character,
            List(4) { RestEngine.DieSpent("wizard", 4) },
        )
        assertEquals("the Wizard had nothing left to spend", 0, outcome.hitDiceSpent)
        assertEquals(0, outcome.hitPointsRegained)
    }

    @Test
    fun `a long rest gives back half the total, smallest die first`() {
        // Eight dice total, so four come back. Keeping the d10s available is the point.
        var character = fighterWizard()
        repeat(5) { character = CharacterHitDice.spendOne(character, "fighter") }
        repeat(3) { character = CharacterHitDice.spendOne(character, "wizard") }
        assertEquals(0, CharacterHitDice.remaining(character))

        val rested = RestEngine.longRest(character).character
        assertEquals(4, CharacterHitDice.remaining(rested))
        val pools = CharacterHitDice.pools(rested).associateBy { it.classId }
        assertEquals("all three d6s came back first", 3, pools["wizard"]!!.remaining)
        assertEquals("then one d10", 1, pools["fighter"]!!.remaining)
    }

    @Test
    fun `hit dice recovered on a long rest counts every class`() {
        assertEquals(4, RestEngine.hitDiceRecoveredOnLongRest(fighterWizard()))
        assertEquals(2, RestEngine.hitDiceRecoveredOnLongRest(fighter(level = 5)))
        assertEquals("never fewer than one", 1, RestEngine.hitDiceRecoveredOnLongRest(fighter(level = 1)))
    }

    @Test
    fun `the flat count stays in step with the pools`() {
        // Anything still reading the old field has to agree with the new one.
        var character = fighterWizard()
        character = CharacterHitDice.spendOne(character, "fighter")
        character = CharacterHitDice.spendOne(character, "wizard")
        assertEquals(2, character.hitDiceSpent)
        assertEquals(2, CharacterHitDice.spent(character))
    }

    @Test
    fun `putting every die back clears both records`() {
        var character = fighterWizard()
        repeat(4) { character = CharacterHitDice.spendOne(character, "fighter") }
        val reset = CharacterHitDice.withAllRecovered(character)
        assertEquals(0, reset.hitDiceSpent)
        assertTrue(reset.hitDiceSpentByClass.isEmpty())
        assertEquals(8, CharacterHitDice.remaining(reset))
    }
}
