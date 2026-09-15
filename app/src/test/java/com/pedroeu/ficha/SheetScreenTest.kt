package com.pedroeu.ficha

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedroeu.ficha.domain.ClassLevel
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.layout.LayoutController
import com.pedroeu.ficha.ui.layout.LayoutMode
import com.pedroeu.ficha.ui.layout.LocalLayoutController
import com.pedroeu.ficha.ui.sheet.CharacterSheetScreen
import com.pedroeu.ficha.ui.theme.FichaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The sheet itself, opened.
 *
 * Every other test in this repository asks the rules a question. This one opens the app: a real
 * repository, the real [CharacterSheetScreen], the real view model, seven characters, and all
 * seven tabs walked with Edit Mode off and then on.
 *
 * That is the gap the whole complaint lived in. "I make a character with a subclass I have not
 * tried and find thirty bugs" is not a statement about the rules engine — the rules engine is
 * covered by [EveryContentSweepTest] — it is a statement about what happens when a particular
 * character meets a particular screen, and nothing in the project had ever done that without a
 * person holding a phone. A tab that throws on a Warlock with three invocations, a card that
 * divides by a level a multiclassed character does not have, a list that renders empty because
 * the answer is filed under a key this screen does not read: all of it is invisible to a domain
 * test, and all of it is one line of this one.
 *
 * It is deliberately not clever. It opens tabs and looks at them. The bugs it is meant to catch
 * are the ones that do not survive being looked at.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SheetScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `every tab of every kind of character opens`() {
        val complaints = SheetTheatre(compose, cast()).run { who, _ ->
            walkPages(who, SheetTheatre.PHONE_TABS)
        }
        assertTrue(complaints.joinToString("\n") { "  - $it" }, complaints.isEmpty())
    }

    /**
     * And again with Edit Mode on, which is where the reports come from.
     *
     * Edit Mode swaps calculated displays for controls across every tab at once. It is the
     * part of the app with the most wiring and the least of it shared, and every screen bug
     * reported this year has been in it.
     */
    @Test
    fun `every tab of every kind of character opens in edit mode`() {
        val complaints = SheetTheatre(compose, cast()).run { who, _ ->
            turnOnEditMode(who)
            walkPages("$who in Edit Mode", SheetTheatre.PHONE_TABS)
        }
        assertTrue(complaints.joinToString("\n") { "  - $it" }, complaints.isEmpty())
    }

    /**
     * The same characters on a tablet, which is a different layout with its own four pages.
     *
     * The wide layout was broken for a month without anyone noticing, because checking it
     * meant finding a tablet. It is the same code path length as the phone and half as often
     * looked at, which is the definition of where a bug goes to live.
     */
    @Test
    @Config(sdk = [34], qualifiers = "w1280dp-h900dp")
    fun `every leaf of the wide layout opens for every kind of character`() {
        val complaints = SheetTheatre(compose, cast(), LayoutMode.TABLET).run { who, _ ->
            walkPages("$who on a tablet", SheetTheatre.TABLET_LEAVES)
        }
        assertTrue(complaints.joinToString("\n") { "  - $it" }, complaints.isEmpty())
    }

    /** And the wide layout in Edit Mode, which is two rarely-trodden paths crossing. */
    @Test
    @Config(sdk = [34], qualifiers = "w1280dp-h900dp")
    fun `every leaf of the wide layout opens in edit mode`() {
        val complaints = SheetTheatre(compose, cast(), LayoutMode.TABLET).run { who, _ ->
            turnOnEditMode(who)
            walkPages("$who on a tablet in Edit Mode", SheetTheatre.TABLET_LEAVES)
        }
        assertTrue(complaints.joinToString("\n") { "  - $it" }, complaints.isEmpty())
    }

    /**
     * Characters worth opening: one of each shape that has ever broken a screen.
     *
     * A Warlock because that is where this started. A Wizard for a spellbook, a Cleric for
     * spells that are prepared rather than chosen, a Rogue for expertise, a Druid for summons,
     * a Fighter for maneuvers and a pile of pools, and a multiclassed pair because half the
     * arithmetic on a sheet reads a level, and on a multiclassed character that is never the
     * level it meant.
     */
    private fun cast(): List<Pair<String, PlayerCharacter>> = listOf(
        "Warlock 5 (Fiend)" to SheetAudit.character("warlock", 5, "fiend"),
        "Wizard 9 (Evoker)" to SheetAudit.character("wizard", 9, "evoker"),
        "Cleric 7 (Life)" to SheetAudit.character("cleric", 7, "life"),
        "Rogue 11 (Thief)" to SheetAudit.character("rogue", 11, "thief"),
        "Druid 6 (Moon)" to SheetAudit.character("druid", 6, "moon"),
        "Fighter 12 (Battle Master)" to SheetAudit.character("fighter", 12, "battle_master"),
        "Fighter 5 / Wizard 4" to SheetAudit.character(
            classId = "fighter",
            level = 9,
            subclassId = "champion",
            classLevels = listOf(
                ClassLevel("fighter", 5, "champion"),
                ClassLevel("wizard", 4, "evoker"),
            ),
        ),
    ).map { (who, pc) ->
        // Its own id and its own name: the store is keyed by id, so a shared one would leave
        // the repository holding a single character, and the name is how the test can tell
        // whose sheet it is actually looking at.
        who to SheetAudit.fullyAnswered(pc).copy(id = who.filter { it.isLetterOrDigit() }, name = who)
    }
}

/**
 * A stage the sheet is put on, one character at a time.
 *
 * A Compose test may only set its content once, so the character on show is state and the
 * screen is rebuilt under [key] — which gives each character its own view model, exactly as
 * navigating to a different sheet does in the app.
 */
class SheetTheatre(
    private val compose: ComposeContentTestRule,
    private val cast: List<Pair<String, PlayerCharacter>>,
    /**
     * Which shape the sheet takes, pinned rather than inferred.
     *
     * Left on AUTOMATIC the layout is decided by how wide the test window happens to be, and
     * a test that quietly renders the phone while claiming to check the tablet is worse than
     * no test at all — it was doing exactly that until this was pinned.
     */
    private val layout: LayoutMode = LayoutMode.PHONE,
) {
    private val complaints = mutableListOf<String>()

    fun run(act: SheetTheatre.(who: String, character: PlayerCharacter) -> Unit): List<String> {
        val repository = repositoryHolding(*cast.map { it.second }.toTypedArray())
        val showing = mutableStateOf(cast.first().second.id)
        // One view model store per character, which is what navigating to a second sheet gives
        // in the app. Sharing one store shares the view model, and then the screen keeps
        // showing the first character while the test believes it has moved on — the second
        // sheet is never really opened and every check passes without looking at anything.
        val stores = cast.associate { (_, pc) -> pc.id to OneCharactersStore() }

        compose.setContent {
            FichaTheme {
                key(showing.value) {
                    CompositionLocalProvider(
                        LocalViewModelStoreOwner provides stores.getValue(showing.value),
                        LocalLayoutController provides LayoutController(layout) {},
                    ) {
                        CharacterSheetScreen(
                            characterId = showing.value,
                            repository = repository,
                            onBack = {},
                            onLevelUp = {},
                        )
                    }
                }
            }
        }

        cast.forEach { (who, character) ->
            try {
                compose.runOnIdle { showing.value = character.id }
                compose.waitForIdle()
                expectSheetOf(who)
                act(who, character)
            } catch (e: Throwable) {
                complaints += "$who: the sheet will not open — ${e.firstLine()}"
            }
        }
        return complaints
    }

    /**
     * The sheet on screen belongs to the character the test asked for.
     *
     * Without this the harness lies: every character is named after its own row, and if the
     * screen is still showing the previous one the name gives it away. It caught the view
     * model being shared the first time this ran.
     */
    fun expectSheetOf(who: String) {
        val found = compose.onAllNodesWithText(who).fetchSemanticsNodes().isNotEmpty()
        if (!found) complaints += "asked for $who's sheet and got somebody else's"
    }

    /** Opens every page in turn, reporting what broke rather than stopping at the first thing. */
    fun walkPages(who: String, pages: List<String>) {
        pages.forEach { page ->
            try {
                // Case-insensitively, because the tablet engraves its leaf tabs in capitals
                // and a test that cares about the capitals is a test of the typography.
                //
                // The tab's own action rather than a tap on its pixels: the JVM has no fonts,
                // so every string here is laid out at invented widths and a tap aimed at the
                // middle of a tab is aimed at a rectangle that does not exist on any phone.
                // What is worth asserting is that opening the page works, and a tap that
                // misses proves nothing either way. Where the geometry is the point — a list
                // too long for its dialog — [ScreenInvariantTest] scrolls and taps for real.
                compose.onAllNodes(hasText(tr(page), ignoreCase = true)).onFirst()
                    .performSemanticsAction(SemanticsActions.OnClick)
                compose.waitForIdle()
                expectSomethingOnThePage("$who, the ${tr(page)} page")
            } catch (e: Throwable) {
                complaints += "$who: the ${tr(page)} page throws — ${e.firstLine()}"
            }
        }
    }

    fun turnOnEditMode(who: String) {
        try {
            compose.onAllNodesWithContentDescription(tr("Edit sheet")).onFirst()
                .performSemanticsAction(SemanticsActions.OnClick)
            compose.waitForIdle()
            // The button's own label is how the sheet says which mode it is in, so if it still
            // offers to start editing, the tap did nothing and everything after it is a test
            // of the ordinary sheet wearing Edit Mode's name.
            val stillOffering = compose
                .onAllNodesWithContentDescription(tr("Edit sheet"))
                .fetchSemanticsNodes().isNotEmpty()
            if (stillOffering) complaints += "$who: tapping Edit did not turn Edit Mode on"
        } catch (e: Throwable) {
            complaints += "$who: Edit Mode will not turn on — ${e.firstLine()}"
        }
    }

    /**
     * Something is written on the page.
     *
     * A tab that draws nothing is a tab that threw and was swallowed, or one whose content
     * resolved to nothing at all — and to a player those read the same: they tapped it and the
     * sheet went blank.
     */
    fun expectSomethingOnThePage(what: String) {
        val written = compose
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
            .fetchSemanticsNodes()
        if (written.size < MINIMUM_WORTH_DRAWING) {
            complaints += "$what draws almost nothing (${written.size} pieces of text)"
        }
    }

    private fun Throwable.firstLine() =
        message.orEmpty().replace(Regex("\\s+"), " ").take(600)

    companion object {
        /** The phone's seven tabs, as the sheet labels them. */
        val PHONE_TABS =
            listOf("Stats", "Skills", "Combat", "Features", "Spells", "Inventory", "Bio")

        /** The tablet's four leaves, in the order the paper folds. */
        val TABLET_LEAVES = listOf("Character", "Features", "Magic", "Gear & Story")

        /** The app bar alone accounts for two, so anything at or under this is a blank page. */
        const val MINIMUM_WORTH_DRAWING = 15
    }
}

/** Somewhere for one character's view model to live, and nowhere for anyone else's. */
private class OneCharactersStore : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}
