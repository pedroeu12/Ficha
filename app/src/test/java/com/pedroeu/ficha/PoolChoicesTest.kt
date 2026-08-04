package com.pedroeu.ficha

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The "pick some from a pool" features Edit Mode offers to rearrange.
 *
 * The user's complaint was concrete — an invocation, once chosen, was awkward to change —
 * but the fix has to be general, because the same shape turns up in half the classes. These
 * tests name the ones that must be reachable, so a feature added later that forgets to be
 * one of these gets caught by the sweep at the bottom rather than by a player.
 */
class PoolChoicesTest {

    private fun character(
        classId: String,
        level: Int = 12,
        subclassId: String? = null,
        featIds: List<String> = emptyList(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = classId,
        subclassId = subclassId,
        backgroundId = "soldier",
        level = level,
        featIds = featIds,
        baseAbilityScores = Ability.ALL.associate { it.name to 16 },
    )

    private fun labels(character: PlayerCharacter) =
        ChoiceResolver.poolChoices(character).map { it.choice.label }

    @Test
    fun `a warlock can reach its invocations`() {
        val found = labels(character("warlock"))
        assertTrue(
            "Eldritch Invocations should be one of the pools: $found",
            found.any { it.contains("Invocation", ignoreCase = true) },
        )
    }

    @Test
    fun `an artificer can reach its plans`() {
        val found = labels(character("artificer"))
        assertTrue(
            "Magic Item Plans should be one of the pools: $found",
            found.any { it.contains("Plan", ignoreCase = true) },
        )
    }

    @Test
    fun `a fighter can reach its fighting style and its masteries`() {
        val found = labels(character("fighter"))
        assertTrue(
            "Fighting Style should be one of the pools: $found",
            found.any { it.contains("Fighting Style", ignoreCase = true) },
        )
        assertTrue(
            "Weapon Mastery should be one of the pools: $found",
            found.any { it.contains("Mastery", ignoreCase = true) || it.contains("Master", ignoreCase = true) },
        )
    }

    @Test
    fun `a sorcerer can reach its metamagic`() {
        val found = labels(character("sorcerer"))
        assertTrue(
            "Metamagic should be one of the pools: $found",
            found.any { it.contains("Metamagic", ignoreCase = true) },
        )
    }

    @Test
    fun `choosing a subclass is not treated as a pool`() {
        // Picking a subclass opens a flow of its own — features, spells, sub-choices — and
        // swapping one from a chip row would leave all of that behind.
        listOf("fighter", "warlock", "artificer", "cleric").forEach { id ->
            val kinds = ChoiceResolver.poolChoices(character(id)).map { it.choice.kind }
            assertTrue("$id offers a subclass swap in the pool list", ChoiceKind.SUBCLASS !in kinds)
        }
    }

    @Test
    fun `a choice asked at several levels appears once, at its newest form`() {
        // Weapon Mastery is asked again each time the count grows. Showing every asking would
        // give a level 12 Fighter four rows for one decision.
        val fighter = character("fighter")
        val masteryRows = ChoiceResolver.poolChoices(fighter)
            .filter { it.choice.label.contains("Master", ignoreCase = true) }
        assertEquals("one row for one decision", 1, masteryRows.size)
    }

    @Test
    fun `every pool offers more than it asks for`() {
        // A "choice" with as many options as picks isn't one, and putting it in the editor
        // just gives the player a row they can only get wrong.
        listOf("artificer", "barbarian", "bard", "cleric", "druid", "fighter", "monk",
            "paladin", "ranger", "rogue", "sorcerer", "warlock", "wizard").forEach { id ->
            ChoiceResolver.poolChoices(character(id)).forEach { resolved ->
                assertTrue(
                    "$id's ${resolved.choice.label} offers ${resolved.choice.options.size} " +
                        "for ${resolved.choice.count} picks",
                    resolved.choice.options.size > resolved.choice.count,
                )
            }
        }
    }
}
