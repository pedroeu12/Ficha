package com.pedroeu.ficha

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedroeu.ficha.data.content.FeatChoiceData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.OwnedOptions
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.ResolvedChoice
import com.pedroeu.ficha.ui.components.ChoiceEditDialog
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.theme.FichaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The interface, drawn and then asked questions about itself.
 *
 * Reading the source catches the shape of a mistake; drawing the screen catches the mistake.
 * "Every option in this list can be reached" is not a fact about the code, it is a fact about
 * the layout, and the bug that started all of this — twenty-eight Eldritch Invocations poured
 * into a dialog that clips at the fold — reads perfectly well as source. There was nothing to
 * see until something rendered it and tried to scroll to the bottom.
 *
 * So this renders the real components with real content and asserts what a player would check:
 * the last thing in the list is reachable, tapping it does something, and something the rules
 * forbid does not.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ScreenInvariantTest {

    @get:Rule
    val compose = createComposeRule()

    private val warlock = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = "warlock", subclassId = "fiend",
        backgroundId = "soldier", level = 5,
        baseAbilityScores = Ability.ALL.associate { it.name to 15 },
        levelSelections = mapOf(
            "5:${ProgressionData.INVOCATION_CHOICE_ID}" to
                listOf("agonizing_blast", "devils_sight", "armor_of_shadows"),
        ),
    )

    private fun resolvedInvocations(): ResolvedChoice =
        ChoiceResolver.all(warlock).first { it.choice.id == ProgressionData.INVOCATION_CHOICE_ID }

    /** A choice with nothing picked yet, as the editor sees one the player has not answered. */
    private fun unanswered(choice: Choice) = ResolvedChoice(
        choice = choice,
        selectedIds = emptyList(),
        selectedNames = emptyList(),
        featureName = choice.label,
        level = 0,
    )

    /** Renders one choice in the editor the sheet actually opens. */
    private fun show(
        character: PlayerCharacter,
        resolved: ResolvedChoice,
        onToggle: (String) -> Unit = {},
    ) {
        compose.setContent {
            FichaTheme {
                ChoiceEditDialog(
                    character = character,
                    resolved = resolved,
                    onDismiss = {},
                    onToggle = onToggle,
                )
            }
        }
    }

    // ================================================================ The reported bug

    /**
     * A Warlock has twenty-eight invocations to choose between, and the twenty-eighth has to
     * be reachable. It was not: a Material dialog bounds its body and does not scroll it, so
     * everything past the fold was drawn off the bottom with nothing to scroll and no way to
     * reach it. The list the player could see never contained the thing they were looking for.
     */
    @Test
    fun `every invocation in the editor can be scrolled to`() {
        val resolved = resolvedInvocations()
        val options = resolved.choice.options
        assertTrue("this is only a test if the list is long", options.size > 20)

        show(warlock, resolved)

        listOf(options.first(), options[options.size / 2], options.last()).forEach { option ->
            compose.onAllNodesWithText(option.name).onFirst()
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    /** And tapping one reports the option the player actually tapped. */
    @Test
    fun `tapping an option in the editor reports that option`() {
        val resolved = resolvedInvocations()
        val tapped = mutableListOf<String>()
        val wanted = resolved.choice.options.last { it.minLevel <= 5 && it.requiresOptions.isEmpty() }

        show(warlock, resolved) { tapped += it }

        compose.onAllNodesWithText(wanted.name).onFirst().performScrollTo().performClick()
        assertEquals(listOf(wanted.id), tapped)
    }

    /**
     * An option the rules gate is shown — a player choosing a path needs to see where it
     * leads — and does nothing when tapped.
     */
    @Test
    fun `an option the character cannot take does not answer when tapped`() {
        val early = warlock.copy(
            level = 2,
            levelSelections = mapOf(
                "2:${ProgressionData.INVOCATION_CHOICE_ID}" to listOf("devils_sight"),
            ),
        )
        val resolved = ChoiceResolver.all(early)
            .first { it.choice.id == ProgressionData.INVOCATION_CHOICE_ID }
        val tapped = mutableListOf<String>()
        // Lifedrinker wants Warlock 9 and the Pact of the Blade.
        val gated = resolved.choice.options.first { it.id == "lifedrinker" }

        show(early, resolved) { tapped += it }

        compose.onAllNodesWithText(gated.name).onFirst().performScrollTo().performClick()
        assertTrue("a level 2 Warlock was allowed to take Lifedrinker", tapped.isEmpty())
        // Its condition is printed rather than hidden, so the player knows why.
        compose.onAllNodesWithText(gated.prerequisite).onFirst().assertExists()
    }

    // ================================================================ The whole game

    /**
     * Every long list in the game, rendered, with its last option scrolled to and tapped.
     *
     * The invocations were the list somebody happened to open. Metamagic, maneuvers, the
     * Artificer's plans, a feat's spell list and every cantrip picker are the same shape, and
     * the only reason they were not reported is that nobody had built that character yet —
     * which is the loop this whole sweep exists to end.
     *
     * Scrolling to the option proves it can be reached; tapping it proves nothing is drawn
     * over the top of it; the dialog's own button being displayed afterwards proves the list
     * did not push the way out of the dialog off the bottom of the screen, which is the same
     * bug wearing a different coat — it is what made the long rest sheet unusable.
     */
    @Test
    fun `the last option of every long list in the game can be reached`() {
        val longest = everyChoice()
            .distinctBy { it.id }
            .filter { it.options.size >= LONG_LIST }
            .sortedByDescending { it.options.size }

        assertTrue("nothing long enough to be worth checking", longest.size > 20)

        // One content block per test is all the rule allows, so the choice on show is state
        // and every list in the game passes through the same dialog.
        val showing = mutableStateOf(longest.first())
        val tapped = mutableListOf<String>()
        compose.setContent {
            FichaTheme {
                ChoiceEditDialog(
                    character = warlock,
                    resolved = unanswered(showing.value),
                    onDismiss = {},
                    onToggle = { tapped += it },
                )
            }
        }

        val unreachable = mutableListOf<String>()
        longest.forEach { choice ->
            compose.runOnIdle { showing.value = choice }
            compose.waitForIdle()
            tapped.clear()

            val last = choice.options.last()
            // An option the rules forbid this character is still drawn, and still has to be
            // reachable — but it will not answer when tapped, so the tap is checked on the
            // last one they could actually take.
            val disabled = OwnedOptions.disabledFor(
                choice = choice,
                owned = OwnedOptions.of(warlock),
                classLevels = ClassLevels.levelMap(warlock),
            )
            val tappable = choice.options.lastOrNull { it.id !in disabled }

            try {
                compose.onAllNodesWithText(last.name).onFirst()
                    .performScrollTo()
                    .assertIsDisplayed()
                // The way out of the dialog survived the list being poured into it.
                compose.onNodeWithText(tr("Done")).assertIsDisplayed()

                if (tappable != null) {
                    compose.onAllNodesWithText(tappable.name).onFirst()
                        .performScrollTo()
                        .performClick()
                    compose.waitForIdle()
                    if (tapped != listOf(tappable.id)) {
                        unreachable += "${choice.id}: tapping \"${tappable.name}\" " +
                            "reported $tapped"
                    }
                }
            } catch (e: Throwable) {
                val why = e.message.orEmpty().lineSequence().first()
                unreachable += "${choice.id} (${choice.options.size} options), " +
                    "last is \"${last.name}\": $why"
            }
        }

        assertTrue(
            "The last option of these lists cannot be reached or tapped:\n" +
                unreachable.joinToString("\n") { "  - $it" } + "\n",
            unreachable.isEmpty(),
        )
    }

    /** Every choice the app can ask, from every table that holds one. */
    private fun everyChoice(): List<Choice> = buildList {
        fun walk(choices: List<Choice>) {
            choices.forEach { choice ->
                add(choice)
                choice.options.forEach { walk(it.grants) }
            }
        }
        ProgressionData.ALL.forEach { p -> p.features.forEach { walk(it.choices) } }
        SubclassData.ALL.forEach { s -> s.features.forEach { walk(it.choices) } }
        SpeciesData.ALL.forEach { species ->
            species.traits.forEach { walk(it.choices) }
            species.lineageOptions.forEach { walk(it.choices) }
        }
        FeatData.ALL.forEach { walk(FeatChoiceData.choicesFor(it.id, it.name)) }
    }

    private companion object {
        /** Long enough that it will not fit in a dialog, which is where the trouble starts. */
        const val LONG_LIST = 12
    }
}

