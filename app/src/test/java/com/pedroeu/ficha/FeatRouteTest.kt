package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterDcs
import com.pedroeu.ficha.domain.CharacterFeats
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.FeatBonuses
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A feat works the same whichever way it arrived.
 *
 * A feat can reach a character six ways: chosen at creation, granted by a background, offered
 * by a species trait, taken instead of an Ability Score Improvement, granted by another feat,
 * or handed over by an Eldritch Invocation. Only the first four ever reached
 * `PlayerCharacter.featIds`, and thirteen places read that field directly to decide what the
 * character could do.
 *
 * So Lessons of the First Ones asked which Origin feat you wanted, asked that feat's own
 * questions, recorded the answers — and then applied none of it. No spells, no limited uses,
 * no save DC, no ability score, no passive bonus. Half a feat, which is worse than none: the
 * sheet looked like it had worked.
 */
class FeatRouteTest {

    /** A Warlock who took Lessons of the First Ones and spent it on Magic Initiate. */
    private fun viaInvocation(featId: String) = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = "warlock", subclassId = "fiend",
        backgroundId = "soldier", level = 5,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        levelSelections = mapOf(
            "5:${ProgressionData.INVOCATION_CHOICE_ID}" to listOf(
                "lessons_of_the_first_ones", "devils_sight", "armor_of_shadows",
                "eldritch_mind", "fiendish_vigor",
            ),
            "5:invocation:lessons_of_the_first_ones:feat" to listOf(featId),
        ),
    )

    /** The same feat, taken the ordinary way. */
    private fun heldDirectly(featId: String) = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = "warlock", subclassId = "fiend",
        backgroundId = "soldier", level = 5,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        featIds = listOf(featId),
    )

    // ------------------------------------------------------------------ Held at all

    @Test
    fun `a feat granted by an invocation counts as held`() {
        val pc = viaInvocation("magic_initiate_wizard")
        assertTrue(
            "the character stores no feat, so only a derived answer can find it",
            pc.featIds.isEmpty(),
        )
        assertTrue(
            "Lessons of the First Ones granted a feat the character does not have",
            "magic_initiate_wizard" in CharacterFeats.heldBy(pc),
        )
        assertTrue(CharacterFeats.has(pc, "magic_initiate_wizard"))
    }

    // ------------------------------------------------------------------ Applied the same

    /**
     * The route must not change the result. Everything a feat does is checked against the same
     * feat held the ordinary way, so this cannot pass by both being equally broken — the
     * direct route is the one that always worked.
     */
    @Test
    fun `a feat reached through an invocation does everything it normally does`() {
        listOf("magic_initiate_wizard", "fey_touched", "tough").forEach { featId ->
            val viaChoice = viaInvocation(featId)
            val direct = heldDirectly(featId)

            fun dcsOf(pc: PlayerCharacter) =
                CharacterDcs.all(pc).map { it.id }.filter { it.contains(featId) }.toSet()
            fun poolsOf(pc: PlayerCharacter) =
                CharacterResources.definitions(pc).map { it.id }
                    .filter { it.contains(featId) }.toSet()
            fun grantsOf(pc: PlayerCharacter) =
                CharacterSpells.granted(pc).map { it.spell.id }.toSet()
            fun asksOf(pc: PlayerCharacter) =
                ChoiceResolver.all(pc).map { it.choice.id }
                    .filter { it.startsWith("feat:$featId") }.toSet()

            assertEquals("$featId: save DCs differ by route", dcsOf(direct), dcsOf(viaChoice))
            assertEquals("$featId: limited uses differ by route", poolsOf(direct), poolsOf(viaChoice))
            assertEquals("$featId: questions differ by route", asksOf(direct), asksOf(viaChoice))
            assertTrue(
                "$featId: the spells it grants are missing when it came from an invocation",
                grantsOf(direct).all { it in grantsOf(viaChoice) },
            )
            assertEquals(
                "$featId: ability increases differ by route",
                FeatBonuses.byAbility(direct),
                FeatBonuses.byAbility(viaChoice),
            )
        }
    }

    /** Fey-Touched grants Misty Step, which is the visible half of the same failure. */
    @Test
    fun `a spell-granting feat from an invocation puts its spell on the sheet`() {
        val pc = viaInvocation("fey_touched")
        assertTrue(
            "misty_step" in CharacterSpells.all(pc).map { it.id },
        )
    }

    /** Tough is +2 Hit Points per level, and it was reaching no number. */
    @Test
    fun `a feat from an invocation reaches the numbers it changes`() {
        val direct = heldDirectly("tough")
        val viaChoice = viaInvocation("tough")
        assertEquals(
            com.pedroeu.ficha.domain.CharacterCalculations.maxHitPoints(direct),
            com.pedroeu.ficha.domain.CharacterCalculations.maxHitPoints(viaChoice),
        )
    }

    // ------------------------------------------------------------------ Every route

    /** A feat from a background or a species trait is held too, and was before. */
    @Test
    fun `the other routes still work`() {
        val fromBackground = PlayerCharacter(
            id = "t", name = "T", speciesId = "elf", classId = "fighter",
            backgroundId = "soldier", level = 3,
            baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        )
        assertTrue(
            "the Soldier background grants Savage Attacker",
            "savage_attacker" in CharacterFeats.heldBy(fromBackground),
        )

        val fromSpecies = PlayerCharacter(
            id = "t", name = "T", speciesId = "human", classId = "fighter",
            backgroundId = "soldier", level = 3,
            baseAbilityScores = Ability.ALL.associate { it.name to 14 },
            originChoiceSelections = mapOf("species:human:origin_feat" to listOf("skilled")),
        )
        assertTrue(
            "a Human's Versatile feat is not held",
            "skilled" in CharacterFeats.heldBy(fromSpecies),
        )
    }

    /** A feat nobody took is not held, whatever else is answered. */
    @Test
    fun `a feat that was not chosen is not held`() {
        val pc = viaInvocation("magic_initiate_wizard")
        assertTrue("tough" !in CharacterFeats.heldBy(pc))
        assertTrue("lucky" !in CharacterFeats.heldBy(pc))
    }
}
