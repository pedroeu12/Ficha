package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.domain.ChoiceGrants
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.OwnedOptions
import com.pedroeu.ficha.ui.levelup.LevelUpState
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Levelling a Warlock who already has invocations.
 *
 * Reported as "I can't tick or untick Agonizing Blast". It was every invocation the character
 * already held, at every level up: the rules ask the question again and each asking restates
 * the whole list, so the ones already taken are the current answer — and the app was greying
 * them out as duplicates. The seeding that was meant to prevent that copied the answer into
 * the level-up state once, and choosing a subclass cleared the map.
 */
class LevelUpInvocationTest {

    @Test
    fun `an invocation the warlock already has can still be untaken at a level up`() {
        var warlock = SheetAudit.character("warlock", 4, "fiend")
        val row = ChoiceResolver.all(warlock)
            .first { it.choice.id == ProgressionData.INVOCATION_CHOICE_ID }
        listOf("agonizing_blast", "devils_sight").forEach {
            warlock = ChoiceGrants.toggle(warlock, row.choice, row.level, it, unbounded = true)
        }
        val state = LevelUpState(character = warlock)
        val choice = state.featureChoices.firstOrNull {
            it.id == ProgressionData.INVOCATION_CHOICE_ID
        }
        assertTrue("the level up never asks the invocation question", choice != null)
        if (choice == null) return

        val disabled = OwnedOptions.disabledFor(
            choice = choice,
            owned = OwnedOptions.of(state.character),
            currentSelection = state.selectionFor(choice.id).toSet(),
            classLevels = ClassLevels.levelMap(state.leveledCharacter),
        )

        assertTrue(
            "the level up does not show the invocations the Warlock already has",
            state.selectionFor(choice.id).containsAll(listOf("agonizing_blast", "devils_sight")),
        )
        assertTrue(
            "Agonizing Blast is greyed out, so it can be neither taken nor given up",
            "agonizing_blast" !in disabled,
        )
        assertTrue(
            "Devil's Sight is greyed out, so it can be neither taken nor given up",
            "devils_sight" !in disabled,
        )
    }
}
