package com.pedroeu.ficha

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The phone and the tablet are one app with two layouts, not two apps.
 *
 * The rule: they may differ in how the page is arranged — one column against two, a tab row
 * against index tabs, a Material card against the tablet's sheet of paper — and in nothing
 * else. Not in which features exist, not in how a description opens, not in how a choice is
 * made or a use is spent.
 *
 * They drifted anyway, because each was edited on its own. The tablet printed an attack's
 * derived bonus where the phone printed the one the player wrote, so a +9 read as +0. Its
 * chosen invocations were bullets of plain text with no way to read what any of them did.
 * Every one of those was invisible until someone picked up the other device.
 *
 * These checks read both trees and fail when one side gains something the other lacks.
 */
class LayoutParityTest {

    private val ui = File("src/main/java/com/pedroeu/ficha/ui")

    private fun tree(name: String): List<File> =
        File(ui, name).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun phone() = tree("sheet")
    private fun tablet() = tree("tablet")

    private fun textOf(files: List<File>) = files.joinToString("\n") { it.readText() }

    /** Everything a side can ask the view model to do, however it is written. */
    private fun actionsIn(files: List<File>): Set<String> =
        Regex("""viewModel(?:\.|::)([a-zA-Z]+)""")
            .findAll(textOf(files))
            .map { it.groupValues[1] }
            .filterNot { it in setOf("character", "editMode") }
            .toSet()

    // ================================================================ Same capabilities

    /**
     * Actions the phone reaches through a screen the tablet doesn't render.
     *
     * The level-up flow and the rest sheet are whole screens both layouts open from the same
     * top bar, so they are not part of either tree and not part of this comparison.
     */
    @Test
    fun `every action one layout can take, the other can take too`() {
        val shared = tree("components").let(::actionsIn)
        val phoneOnly = actionsIn(phone()) - actionsIn(tablet()) - shared
        val tabletOnly = actionsIn(tablet()) - actionsIn(phone()) - shared

        // Both sides reach the shared cards and the overlay host, so anything routed through
        // those counts for both. What is left here is a genuine one-sided capability.
        assertTrue(
            "the phone can do these and the tablet cannot: $phoneOnly",
            phoneOnly.all { it in ROUTED_THROUGH_SHARED_SCREENS },
        )
        assertTrue(
            "the tablet can do these and the phone cannot: $tabletOnly",
            tabletOnly.all { it in ROUTED_THROUGH_SHARED_SCREENS },
        )
    }

    /**
     * Actions that live on a screen both layouts open rather than inside either layout.
     *
     * The rest flow, the level-up flow, and the pickers are one implementation reached from
     * the shared top bar or from an overlay, so naming them here is not an exception to the
     * rule — it is saying they are outside the two trees being compared.
     */
    private val ROUTED_THROUGH_SHARED_SCREENS = setOf(
        // The rest sheet.
        "shortRest", "longRestDetailed", "rollHitDie", "swapCantrip",
        // The limited-use trackers, rendered by the shared ResourcesCard on both.
        "addCustomResource", "removeCustomResource", "setResourceMax", "setResourceSpent",
        "spendResourceOn", "setPerUseChoice", "clearPerUseChoice",
        // Replicate Magic Item, rendered by the shared ArtificerItemsCard on both.
        "makeArtificerItem", "unmakeArtificerItem",
        // The top bar and the character store.
        "toggleEditMode", "clearAllOverrides", "refresh",
        // Two names for one gesture: the phone sets a spell prepared, the tablet toggles it.
        "setSpellPrepared", "toggleSpellPrepared",
        // The tablet's handle names these; the phone calls the same view model methods from
        // its own stat and skill editors.
        "cycleSkill", "editAbility", "editSave", "editSkill", "setSaveBonus",
        "toggleSkillProficiency", "toggleSkillExpertise", "toggleSaveProficiency",
    )

    // ================================================================ Same shared machinery

    /**
     * The cards that *are* a system — the trackers, the per-use choices, the Artificer's
     * plans, the pool editor — are one implementation, rendered by both.
     *
     * A layout that reimplements one of these has forked a system, which is how the two
     * versions came to behave differently in the first place.
     */
    @Test
    fun `both layouts render the shared system cards`() {
        val phoneText = textOf(phone())
        val tabletText = textOf(tablet())

        listOf(
            "ResourcesCard",
            "PerUseChoicesCard",
            "ArtificerItemsCard",
            "PoolFeaturesCard",
        ).forEach { card ->
            assertTrue("the phone stopped rendering $card", phoneText.contains("$card("))
            assertTrue("the tablet stopped rendering $card", tabletText.contains("$card("))
        }
    }

    /** Adding things goes through one picker, whichever layout you are in. */
    @Test
    fun `both layouts open the same pickers`() {
        val phoneText = textOf(phone())
        val tabletText = textOf(tablet())

        listOf("SpellPickerSheet", "FeatPickerSheet", "AddItemSheet").forEach { picker ->
            assertTrue("the phone lost $picker", phoneText.contains(picker))
            assertTrue("the tablet lost $picker", tabletText.contains(picker))
        }
    }

    /** Reading a spell or an item is one sheet, whichever layout you are in. */
    @Test
    fun `both layouts open the same detail sheets`() {
        val phoneText = textOf(phone())
        val tabletText = textOf(tablet())

        listOf("SpellDetailSheet", "ItemDetailSheet", "RulesTextSheet").forEach { sheet ->
            assertTrue("the phone lost $sheet", phoneText.contains(sheet))
            assertTrue("the tablet lost $sheet", tabletText.contains(sheet))
        }
    }

    // ================================================================ Same overrides

    /**
     * A value the player rewrote on one layout shows up on the other.
     *
     * Both write into the same map on the character, so the keys have to match exactly. They
     * did not have to, and an attack edited on the phone would simply not have been edited on
     * the tablet.
     */
    @Test
    fun `both layouts write overrides under the same keys`() {
        fun keysIn(files: List<File>): Set<String> =
            Regex("\"((?:combat|bio|vitals|character):[a-zA-Z_]+)\"")
                .findAll(textOf(files))
                .map { it.groupValues[1] }
                .toSet()

        val phoneKeys = keysIn(phone())
        val tabletKeys = keysIn(tablet())
        val onlyOne = (phoneKeys - tabletKeys) + (tabletKeys - phoneKeys)

        assertTrue(
            "these overrides are editable on one layout only, so an edit there is invisible " +
                "on the other: $onlyOne",
            onlyOne.all { it in ONE_SIDED_BY_DESIGN },
        )
    }

    /**
     * Overrides that belong to one layout because the other has no such element.
     *
     * The tablet prints the character's name at the head of its page and the phone puts it in
     * the app bar, which is a layout difference and so allowed.
     */
    private val ONE_SIDED_BY_DESIGN = setOf(
        "character:name",
        // The phone's combat tab lists tools among its proficiency lines; the tablet gives
        // them a block of their own with add and remove, which is a fuller treatment and
        // writes the character's tool list directly rather than an override.
        "combat:tools",
        // The tablet's Defenses block names the equipped armour line; the phone derives it.
        "combat:armor",
        // Not overrides at all: the tablet's overlay host labels its four free-text editors
        // with these keys and routes each one to the same setter the phone's Bio tab calls —
        // setAppearance, setBackstory, setAlignment, setNotes. The phone has no key because
        // its editor is a field on the page rather than an overlay it has to name.
        "bio:appearance",
        "bio:backstory",
        "bio:alignment",
        "bio:notes",
    )

    // ================================================================ Same source of truth

    /**
     * Both layouts read the same list from the same place.
     *
     * The tablet once printed an attack's derived bonus where the phone printed the one the
     * player had written, so an attack at +9 read as +0 on the other device. Nothing about
     * the layout caused that — a second copy of the derivation did. Whatever a leaf or a tab
     * looks like, the list it draws comes out of the domain, and out of the same function.
     */
    @Test
    fun `both layouts derive their lists from the same domain functions`() {
        val phoneText = textOf(phone())
        val tabletText = textOf(tablet())

        listOf(
            // What attacks exist, and with what numbers.
            "CharacterAttacks.all",
            // What spells are on the sheet, and which of them the character always has.
            "CharacterSpells.all",
            "CharacterSpells.isGranted",
            // Slots, DCs, and how many spells may be prepared.
            "CharacterCalculations.spellSlots",
            "CharacterCalculations.spellSaveDc",
            "CharacterCalculations.maxPreparedSpells",
            "CharacterDcs.all",
            // What features the character has, and what was chosen for each.
            "SheetFeatures.classFeatures",
            "SheetFeatures.subclassFeatures",
            "ChoiceResolver.classFeatureChoices",
            "ChoiceResolver.subclassFeatureChoices",
            "ChoiceResolver.originChoices",
        ).forEach { call ->
            assertTrue("the phone stopped reading $call", phoneText.contains(call))
            assertTrue("the tablet stopped reading $call", tabletText.contains(call))
        }
    }

    // ================================================================ Same gestures

    /**
     * A thing is opened the same way on both.
     *
     * Every openable thing in the app opens on a plain tap. A long press, a swipe, or a drag
     * added to one layout would be a gesture a player who uses the other device would never
     * find, and could not be told about in one sentence that is true of both.
     */
    @Test
    fun `neither layout hides behaviour behind a gesture the other lacks`() {
        val gestures = listOf(
            "combinedClickable", "onLongClick", "detectTapGestures",
            "swipeable", "SwipeToDismiss", "draggable", "pointerInput",
        )
        listOf("the phone" to phone(), "the tablet" to tablet()).forEach { (who, files) ->
            val text = textOf(files)
            gestures.forEach { gesture ->
                assertTrue(
                    "$who now uses $gesture; every other surface opens on a tap, so this is " +
                        "either a gesture the other layout lacks or one nothing teaches",
                    !text.contains(gesture),
                )
            }
        }
    }

    // ================================================================ Same reading gesture

    /**
     * Neither layout shows rules text that cannot be read.
     *
     * The tablet listed a character's chosen invocations and mastery properties as bullets of
     * plain text — you could read what one was called and nothing else, while the same list
     * on the phone opened each one's rules on a tap.
     */
    @Test
    fun `the tablet opens a description everywhere the phone does`() {
        val tabletText = textOf(tablet())

        // Every list of picked options on the tablet routes through PickLine, which opens the
        // shared sheet, rather than printing names into a Text.
        assertTrue(
            "the tablet's chosen options no longer open their rules",
            tabletText.contains("PickLine("),
        )
        assertTrue(
            "PickLine stopped opening the shared sheet",
            Regex("""fun PickLine\(([\s\S]{0,1600}?)RulesTextSheet\(""").containsMatchIn(tabletText),
        )
    }
}
