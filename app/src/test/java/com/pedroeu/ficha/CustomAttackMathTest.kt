package com.pedroeu.ficha

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterAttacks
import com.pedroeu.ficha.domain.CustomAttack
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A hand-written attack works its own numbers out.
 *
 * The bonus used to be one free-text box, so adding a +1 Longsword meant doing the addition
 * yourself and redoing it every time your Strength or Proficiency Bonus moved.
 */
class CustomAttackMathTest {

    private fun character(level: Int = 5, scores: Map<String, Int> = emptyMap()) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = "fighter",
        backgroundId = "soldier",
        level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 10 } + scores,
    )

    private fun numbers(attack: CustomAttack, character: PlayerCharacter = character()) =
        CharacterAttacks.customAttackNumbers(character, attack)

    @Test
    fun `ability and proficiency reach both the attack and the damage`() {
        // Strength 18 is +4, and a level 5 Fighter has a +3 Proficiency Bonus.
        val (toHit, damage) = numbers(
            CustomAttack(id = "a", name = "Longsword", damageDice = "1d8", abilityName = "STR"),
            character(scores = mapOf("STR" to 18)),
        )
        assertEquals("+7", toHit)
        assertEquals("1d8 + 4", damage)
    }

    @Test
    fun `a magic bonus adds to both`() {
        val (toHit, damage) = numbers(
            CustomAttack(
                id = "a", name = "+2 Longsword", damageDice = "1d8",
                abilityName = "STR", magicBonus = 2,
            ),
            character(scores = mapOf("STR" to 18)),
        )
        assertEquals("+9", toHit)
        assertEquals("1d8 + 6", damage)
    }

    @Test
    fun `dropping proficiency drops it from the attack only`() {
        val (toHit, damage) = numbers(
            CustomAttack(
                id = "a", name = "Improvised", damageDice = "1d4",
                abilityName = "STR", proficient = false,
            ),
            character(scores = mapOf("STR" to 18)),
        )
        assertEquals("+4", toHit)
        assertEquals("1d4 + 4", damage)
    }

    @Test
    fun `a negative modifier reads as a subtraction, not a stray plus`() {
        val (toHit, damage) = numbers(
            CustomAttack(id = "a", name = "Club", damageDice = "1d4", abilityName = "STR"),
            character(scores = mapOf("STR" to 6)),
        )
        assertEquals("+1", toHit)
        assertEquals("1d4 - 2", damage)
    }

    @Test
    fun `no ability means proficiency and magic alone`() {
        val (toHit, damage) = numbers(
            CustomAttack(id = "a", name = "Trap", damageDice = "2d6", magicBonus = 1),
        )
        assertEquals("+4", toHit)
        assertEquals("2d6 + 1", damage)
    }

    @Test
    fun `a typed bonus is left exactly as written`() {
        // The free-text field is the escape hatch for anything the maths cannot express.
        val (toHit, damage) = numbers(
            CustomAttack(
                id = "a", name = "Eldritch Blast", damageDice = "1d10",
                bonus = "spell attack", abilityName = "STR", magicBonus = 3,
            ),
            character(scores = mapOf("STR" to 18)),
        )
        assertEquals("spell attack", toHit)
        assertEquals("the dice are left alone too", "1d10", damage)
    }

    @Test
    fun `a zero total still shows a sign`() {
        val (toHit, _) = numbers(
            CustomAttack(id = "a", name = "Odd", damageDice = "1d4", proficient = false),
        )
        assertEquals("+0", toHit)
    }
}
