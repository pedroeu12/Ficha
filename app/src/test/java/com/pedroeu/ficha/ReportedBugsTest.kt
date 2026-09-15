package com.pedroeu.ficha

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.domain.CharacterSummons
import com.pedroeu.ficha.domain.ChoiceGrants
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.ui.components.SummonSheet
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.theme.FichaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Two things a player reported still broken, each written down as a rule before being fixed.
 *
 * Both had passed every check the project had, which is the whole reason they are here: a
 * question the sheet raises and never shows is invisible to a test of the rules, and a button
 * that is drawn but wired to nothing is invisible to a test of the wiring.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ReportedBugsTest {

    @get:Rule
    val compose = createComposeRule()

    // ================================================================ The summon's hit points

    /**
     * Edit Mode can correct a creature's maximum hit points.
     *
     * Reported as "still can't change a summon's Hit Points in Edit Mode". The button exists,
     * so the question is whether a player can reach it and whether it reaches the character.
     */
    @Test
    fun `edit mode can set a summoned creature's maximum hit points`() {
        val druid = SheetAudit.character("druid", 5, "moon")
        val character = CharacterSummons.summon(
            druid, "bestial_spirit_land", "summon_beast", "Summon Beast", spellLevel = 2,
        )
        val summon = character.activeSummons.single()
        val startingHp = summon.maxHp
        var newMax = 0

        compose.setContent {
            FichaTheme {
                SummonSheet(
                    character = character,
                    summon = summon,
                    editMode = true,
                    onRename = {}, onDamage = {}, onHeal = {}, onSetHitPoints = { _, _ -> },
                    onDismiss = {}, onSpend = { _, _ -> }, onNotes = {},
                    onSetMaxHitPoints = { newMax = it },
                    onSetField = { _, _ -> },
                )
            }
        }

        // The number itself is the way in, the same as on the character's own sheet.
        compose.onAllNodesWithText("$startingHp / $startingHp").onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        // The field arrives holding the creature's current maximum, which is what a player
        // wants to see; the test has to clear it the way they would.
        compose.onAllNodesWithText(tr("Maximum")).onFirst().performTextClearance()
        compose.onAllNodesWithText(tr("Maximum")).onFirst().performTextInput("42")
        compose.onAllNodesWithText(tr("Save")).onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals("tapping the number never reached the character", 42, newMax)

        // And the buttons below still work, for a player who learned them first.
        newMax = 0
        compose.onAllNodesWithText(tr("Amount")).onFirst().performTextInput("42")
        compose.onAllNodes(hasScrollAction()).onFirst()
            .performScrollToNode(hasText(tr("Set max")))
        compose.onAllNodes(hasText(tr("Set max"))).onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)

        assertEquals("the creature's maximum never reached the character", 42, newMax)
    }

    // ================================================================ Agonizing Blast

    /**
     * Taking an invocation that asks a question of its own makes that question visible.
     *
     * Agonizing Blast names one of your damaging cantrips. The rules raise the question and the
     * engine answers it — but a question nobody is shown is a feature that does nothing, which
     * is what "Agonizing Blast still doesn't work" means from the player's side.
     */
    @Test
    fun `an invocation that asks its own question raises one that can be found`() {
        var warlock = SheetAudit.character("warlock", 5, "fiend")
        val row = ChoiceResolver.all(warlock)
            .first { it.choice.id == ProgressionData.INVOCATION_CHOICE_ID }
        warlock = ChoiceGrants.toggle(
            warlock, row.choice, row.level, "agonizing_blast", unbounded = true,
        )

        val raised = ChoiceResolver.all(warlock)
            .filter { it.choice.id == "invocation:agonizing_blast:cantrip" }

        assertEquals("the cantrip question was not raised", 1, raised.size)
        assertTrue(
            "the question has nothing to answer it with",
            raised.single().choice.options.isNotEmpty(),
        )
        assertTrue(
            "the question is not attached to anything the sheet groups features under",
            raised.single().featureName.isNotBlank(),
        )

        // And it reaches the list the Features tab actually draws. A question that exists in
        // the resolver and not in the list a screen reads is a question nobody is ever asked,
        // which from the player's side is the invocation doing nothing at all.
        val onTheTab = ChoiceResolver.originChoices(warlock)
            .filter { it.choice.id == "invocation:agonizing_blast:cantrip" }
        assertEquals("the Features tab never shows the question", 1, onTheTab.size)
    }

}
