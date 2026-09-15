package com.pedroeu.ficha

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedroeu.ficha.domain.CharacterSummons
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.rules.ActiveSummon
import com.pedroeu.ficha.ui.components.SummonPickerSheet
import com.pedroeu.ficha.ui.components.SummonSheet
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.theme.FichaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * A creature's sheet, drawn, with Edit Mode on.
 *
 * "There is no edit mode for summons" was reported and fixed before by adding a callback that
 * nothing on screen ever called — which is a thing only a rendering test can catch, because
 * the wiring compiles perfectly either way. So these do not check that the functions exist;
 * they check that a player looking at the creature can see the way to change it.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CreatureScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val druid = SheetAudit.character("druid", 5, "moon")

    /**
     * Brings a row of the creature's sheet into view.
     *
     * The sheet is a lazy list, so a row below the fold is not merely off screen — it has not
     * been composed and does not exist to be found. Scrolling the list is what creates it,
     * which is also exactly what a player does, and the reason an assertion that skips this
     * step reports "no such row" for a row that is perfectly fine.
     */
    private fun ComposeContentTestRule.bringIntoView(matcher: SemanticsMatcher) =
        onAllNodes(hasScrollAction()).onFirst().performScrollToNode(matcher)

    private fun withSpirit(): Pair<PlayerCharacter, ActiveSummon> {
        val character = CharacterSummons.summon(
            druid, "bestial_spirit_land", "summon_beast", "Summon Beast", spellLevel = 2,
        )
        return character to character.activeSummons.single()
    }

    /** Edit Mode reaches every part of the creature, not only its name and hit points. */
    @Test
    fun `edit mode offers a way to change the creature itself`() {
        val (character, summon) = withSpirit()
        val changed = mutableListOf<String>()

        compose.setContent {
            FichaTheme {
                SummonSheet(
                    character = character,
                    summon = summon,
                    editMode = true,
                    onRename = {}, onDamage = {}, onHeal = {}, onSetHitPoints = { _, _ -> },
                    onDismiss = {}, onSpend = { _, _ -> }, onNotes = {},
                    onSetField = { key, _ -> changed += key },
                )
            }
        }

        // The rows a stat block has and a player changes: its defences and its senses are
        // offered whether or not this creature came with any, because an empty line is the
        // only way to add what the book left out.
        listOf("Resistances", "Senses", "Languages", "Condition Immunities").forEach { row ->
            val label = hasText("${tr(row)}: ")
            compose.bringIntoView(label)
            compose.onAllNodes(label).onFirst().assertIsDisplayed()
        }
    }

    /** And a way back: everything written over the book can be put back in one gesture. */
    @Test
    fun `an edited creature can be put back the way the book has it`() {
        val (character, summon) = withSpirit()
        val edited = summon.copy(overrides = mapOf("speed" to "80 ft."))
        var reset = false

        compose.setContent {
            FichaTheme {
                SummonSheet(
                    character = character.copy(activeSummons = listOf(edited)),
                    summon = edited,
                    editMode = true,
                    onRename = {}, onDamage = {}, onHeal = {}, onSetHitPoints = { _, _ -> },
                    onDismiss = {}, onSpend = { _, _ -> }, onNotes = {},
                    onSetField = { _, value -> reset = value == null },
                )
            }
        }

        val wayBack = hasText(tr("Put this creature back the way the book has it"))
        compose.bringIntoView(wayBack)
        compose.onAllNodes(wayBack).onFirst().performSemanticsAction(SemanticsActions.OnClick)

        assertTrue("the way back does nothing", reset)
    }

    /** The edited speed is what the sheet prints, not the book's. */
    @Test
    fun `the creature drawn is the creature as it stands`() {
        val (character, summon) = withSpirit()
        val edited = summon.copy(overrides = mapOf("speed" to "80 ft.", "armorClass" to "19"))

        compose.setContent {
            FichaTheme {
                SummonSheet(
                    character = character.copy(activeSummons = listOf(edited)),
                    summon = edited,
                    editMode = false,
                    onRename = {}, onDamage = {}, onHeal = {}, onSetHitPoints = { _, _ -> },
                    onDismiss = {}, onSpend = { _, _ -> }, onNotes = {},
                )
            }
        }

        compose.onAllNodesWithText("80 ft.").onFirst().assertIsDisplayed()
        compose.onAllNodesWithText("19").onFirst().assertIsDisplayed()
    }

    /** A character with no summoning spell at all can still be handed a creature. */
    @Test
    fun `the picker offers a way to write a creature even with nothing to summon`() {
        val fighter = SheetAudit.character("fighter", 3, "champion")
        assertTrue(
            "this is only a test if the character summons nothing",
            CharacterSummons.available(fighter).isEmpty(),
        )

        compose.setContent {
            FichaTheme {
                SummonPickerSheet(
                    character = fighter,
                    onDismiss = {},
                    onSummon = { _, _, _, _, _, _ -> },
                    onWriteCreature = {},
                )
            }
        }

        compose.onAllNodesWithText(tr("+ Write a creature")).onFirst()
            .performScrollTo()
            .assertIsDisplayed()
    }
}
