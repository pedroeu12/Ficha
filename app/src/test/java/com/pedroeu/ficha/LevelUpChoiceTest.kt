package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.levelup.LevelUpState
import com.pedroeu.ficha.ui.levelup.LevelUpStep
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reported bug, in the place it was reported.
 *
 * "Eldritch Invocations that grant a choice are not opening any selection interface — the
 * invocation is just added with no way to make the choice it requires."
 *
 * The question existed; it was only ever raised on the sheet, by a branch that ran over a
 * finished character. During the level up — the moment the invocation is actually taken — the
 * flow offered the invocation list and nothing else, then let the player finish. The cantrip
 * Agonizing Blast names was left blank on a card they would find days later, if ever.
 */
class LevelUpChoiceTest {

    /** A level 2 Warlock knows three invocations, so a valid answer names three. */
    private val THREE = listOf("agonizing_blast", "devils_sight", "armor_of_shadows")

    private val warlock = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = "warlock",
        backgroundId = "soldier", level = 1,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
    )

    private fun levellingToTwo(selections: Map<String, List<String>>) =
        LevelUpState(
            character = warlock,
            selections = selections,
            requestedStep = LevelUpStep.FEATURES,
        )

    @Test
    fun `taking an invocation that names a cantrip raises the cantrip question there and then`() {
        val before = levellingToTwo(emptyMap())
        assertTrue(
            "the invocation list should be on offer at level 2",
            before.featureChoices.any { it.id == ProgressionData.INVOCATION_CHOICE_ID },
        )
        assertFalse(
            "nothing should ask about a cantrip before an invocation is picked",
            before.featureChoices.any { it.id == "invocation:agonizing_blast:cantrip" },
        )

        val after = levellingToTwo(
            mapOf(ProgressionData.INVOCATION_CHOICE_ID to THREE),
        )
        assertTrue(
            "picking Agonizing Blast raised no question about which cantrip",
            after.featureChoices.any { it.id == "invocation:agonizing_blast:cantrip" },
        )
    }

    /**
     * And the level cannot be finished while it is unanswered, which is the half that makes
     * it a rule rather than a suggestion.
     */
    @Test
    fun `a level cannot be finished with the raised question unanswered`() {
        val unanswered = levellingToTwo(
            mapOf(ProgressionData.INVOCATION_CHOICE_ID to THREE),
        )
        assertFalse(
            "the flow let the player past an unanswered question",
            unanswered.canAdvance,
        )

        val answered = levellingToTwo(
            mapOf(
                ProgressionData.INVOCATION_CHOICE_ID to THREE,
                "invocation:agonizing_blast:cantrip" to listOf("eldritch_blast"),
            ),
        )
        assertTrue(
            "the flow refused a fully answered level",
            answered.canAdvance,
        )
    }

    /** Pact of the Tome asks for five spells, and all five gate the level. */
    @Test
    fun `an invocation that names five spells asks for all five`() {
        val state = levellingToTwo(
            mapOf(
                ProgressionData.INVOCATION_CHOICE_ID to
                    listOf("pact_tome", "devils_sight", "armor_of_shadows"),
            ),
        )
        val raised = state.featureChoices.filter { it.id.startsWith("invocation:pact_tome") }
        assertTrue("the Book of Shadows raised nothing", raised.size == 2)
        assertTrue("it asks for the wrong number of spells", raised.sumOf { it.count } == 5)
        assertFalse(state.canAdvance)
    }

    /**
     * A feat taken in place of an Ability Score Improvement asks what the feat asks, and a
     * feat that grants a feat asks that one's questions too.
     */
    @Test
    fun `a feat taken at level up asks its own questions`() {
        val fighter = PlayerCharacter(
            id = "t", name = "T", speciesId = "human", classId = "fighter",
            backgroundId = "soldier", level = 3,
            baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        )
        val state = LevelUpState(
            character = fighter,
            featId = "magic_initiate_wizard",
            requestedStep = LevelUpStep.FEAT_CHOICES,
        )
        assertTrue(
            "Magic Initiate asked nothing when taken at level up",
            state.featChoices.isNotEmpty(),
        )
        assertFalse(
            "the flow let the player past a feat's unanswered questions",
            state.canAdvance,
        )
    }
}
