package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.domain.ClassLevel
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.Owned
import com.pedroeu.ficha.domain.OwnedOptions
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eldritch Invocations, which the rules gate three ways: how many you know, what level you
 * need for each, and which ones need another invocation first.
 *
 * The app enforced none of it. Eight of the twenty-eight were on offer, the number a Warlock
 * knows was clamped to that — so a level 18 Warlock chose ten out of ten and had no decision
 * at all — and the picker that was supposed to grey out what you don't qualify for was
 * throwing that answer away for every list drawn as cards rather than chips.
 */
class InvocationTest {

    private fun warlockChoiceAt(level: Int): Choice =
        ProgressionData.forClass("warlock")!!.features
            .filter { it.level <= level }
            .flatMap { it.choices }
            .last { it.id == ProgressionData.INVOCATION_CHOICE_ID }

    private fun warlock(level: Int) = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = "warlock",
        subclassId = if (level >= 3) "fiend" else null,
        backgroundId = "soldier", level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
    )

    private fun offered(level: Int, held: Set<String> = emptySet()): Set<String> {
        val choice = warlockChoiceAt(level)
        val disabled = OwnedOptions.disabledFor(
            choice = choice,
            owned = Owned(options = held),
            classLevels = mapOf("warlock" to level),
        )
        return choice.options.map { it.id }.toSet() - disabled
    }

    // ------------------------------------------------------------------ The list

    @Test
    fun `every invocation in the book is on offer`() {
        val options = warlockChoiceAt(20).options
        // Twenty-eight in the Player's Handbook, plus the two the Primordial Patron adds.
        assertEquals(30, options.size)
        assertEquals("ids must be unique", options.size, options.map { it.id }.distinct().size)

        listOf(
            "agonizing_blast", "armor_of_shadows", "ascendant_step", "devils_sight",
            "devouring_blade", "eldritch_mind", "eldritch_smite", "eldritch_spear",
            "fiendish_vigor", "gaze_of_two_minds", "gift_of_the_depths",
            "gift_of_the_protectors", "investment_of_the_chain_master",
            "lessons_of_the_first_ones", "lifedrinker", "mask_of_many_faces",
            "master_of_myriad_forms", "misty_visions", "one_with_shadows",
            "otherworldly_leap", "pact_blade", "pact_chain", "pact_tome", "repelling_blast",
            "thirsting_blade", "visions_of_distant_realms", "whispers_of_the_grave",
            "witch_sight",
        ).forEach { id ->
            assertTrue("$id is missing from the list", options.any { it.id == id })
        }
    }

    @Test
    fun `the ids of the ones that were already here have not moved`() {
        // A saved Warlock holds these strings; renaming one would silently drop an invocation.
        val options = warlockChoiceAt(20).options.map { it.id }
        listOf("agonizing_blast", "armor_of_shadows", "devils_sight", "eldritch_mind",
            "mask_of_many_faces", "pact_blade", "pact_chain", "pact_tome")
            .forEach { assertTrue("$it was renamed", it in options) }
    }

    @Test
    fun `every option with a level prerequisite says so in the book's words`() {
        warlockChoiceAt(20).options
            .filter { it.minLevel > 0 }
            .forEach {
                assertTrue(
                    "${it.name} is gated at level ${it.minLevel} and doesn't say why",
                    it.prerequisite.contains("${it.minLevel}+"),
                )
            }
    }

    // ------------------------------------------------------------------ How many

    @Test
    fun `the count is the class table's, not the size of the list`() {
        // 1, 3, 5, 6, 7, 8, 9, 10 across the levels the column grows.
        assertEquals(1, warlockChoiceAt(1).count)
        assertEquals(3, warlockChoiceAt(2).count)
        assertEquals(3, warlockChoiceAt(4).count)
        assertEquals(5, warlockChoiceAt(5).count)
        assertEquals(6, warlockChoiceAt(7).count)
        assertEquals(7, warlockChoiceAt(9).count)
        assertEquals(8, warlockChoiceAt(12).count)
        assertEquals(9, warlockChoiceAt(15).count)
        assertEquals(10, warlockChoiceAt(18).count)
    }

    @Test
    fun `a level 20 warlock still has a choice to make`() {
        val choice = warlockChoiceAt(20)
        assertTrue(
            "knowing ten of ten would not be a choice",
            choice.count < choice.options.size,
        )
    }

    // ------------------------------------------------------------------ Level gates

    @Test
    fun `a level 1 warlock sees only the five with no prerequisite`() {
        assertEquals(
            setOf(
                "armor_of_shadows", "eldritch_mind",
                "pact_blade", "pact_chain", "pact_tome",
            ),
            offered(1),
        )
    }

    @Test
    fun `the list opens up as the warlock levels`() {
        assertTrue("Devil's Sight arrives at 2", "devils_sight" in offered(2))
        assertFalse("Ascendant Step is a level 5 invocation", "ascendant_step" in offered(2))
        assertTrue("ascendant_step" in offered(5))
        assertFalse("Witch Sight is a level 15 invocation", "witch_sight" in offered(9))
        assertTrue("witch_sight" in offered(15))

        assertTrue("the list only ever grows", offered(5).containsAll(offered(2)))
        assertTrue(offered(20).containsAll(offered(15)))
    }

    // ------------------------------------------------------------------ Chains

    @Test
    fun `an invocation that needs another is refused until you have it`() {
        // Thirsting Blade needs Pact of the Blade, at Warlock 5.
        assertFalse("thirsting_blade" in offered(5))
        assertTrue("thirsting_blade" in offered(5, held = setOf("pact_blade")))

        // And Devouring Blade needs Thirsting Blade in turn, at 12.
        assertFalse("devouring_blade" in offered(12, held = setOf("pact_blade")))
        assertTrue(
            "devouring_blade" in offered(12, held = setOf("pact_blade", "thirsting_blade")),
        )
    }

    @Test
    fun `a chain can be built in one sitting`() {
        // What is ticked right now counts as held, so a level 12 Warlock rebuilding their
        // whole set can take all three in the same step.
        val choice = warlockChoiceAt(12)
        val disabled = OwnedOptions.disabledFor(
            choice = choice,
            owned = Owned(),
            currentSelection = setOf("pact_blade", "thirsting_blade"),
            classLevels = mapOf("warlock" to 12),
        )
        assertFalse("devouring_blade" in disabled)
    }

    @Test
    fun `you cannot pick the same invocation twice`() {
        val choice = warlockChoiceAt(12)
        val disabled = OwnedOptions.disabledFor(
            choice = choice,
            owned = Owned(options = setOf("armor_of_shadows")),
            classLevels = mapOf("warlock" to 12),
        )
        assertTrue("armor_of_shadows" in disabled)
    }

    // ------------------------------------------------------------------ Multiclass

    @Test
    fun `the level that counts is the warlock level, not the character's`() {
        val fighterWarlock = warlock(1).copy(
            level = 12,
            classLevels = listOf(
                ClassLevel("fighter", 11, "champion", isStarting = true),
                ClassLevel("warlock", 1, null),
            ),
        )
        assertEquals("warlock", warlockChoiceAt(1).prerequisiteClassId)

        val choice = warlockChoiceAt(1)
        val disabled = OwnedOptions.disabledFor(
            choice = choice,
            owned = Owned(),
            classLevels = ClassLevels.levelMap(fighterWarlock),
        )
        assertTrue(
            "a Warlock 1 does not get a level 5 invocation for having eleven Fighter levels",
            "ascendant_step" in disabled,
        )
    }

    // ------------------------------------------------------------------ Unenforceable ones

    @Test
    fun `a prerequisite the app cannot check is printed rather than enforced`() {
        // "a Warlock cantrip that deals damage" depends on cantrips that may not be chosen
        // yet, so Agonizing Blast is offered from level 2 with the condition written on it.
        val blast = warlockChoiceAt(20).options.first { it.id == "agonizing_blast" }
        assertEquals(2, blast.minLevel)
        assertTrue(blast.requiresOptions.isEmpty())
        assertTrue(blast.prerequisite.contains("Cantrip", ignoreCase = true))
        assertTrue("agonizing_blast" in offered(2))
    }
}
