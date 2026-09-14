package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ChoiceGrants
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.OwnedOptions
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.ResolvedChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reported bug, walked the way the screen walks it.
 *
 * "In Edit Mode the Eldritch Invocations simply do not work." Everything below is what
 * `PoolFeaturesCard` and `FeaturesTab` actually do, in the order they do it: resolve the pools
 * from the character, open one, tap an option, and resolve again from the character the tap
 * produced. That last step is the whole of the fix — the dialogs used to hold the resolved
 * choice they opened with, so the second tap was worked out from the answer as it stood before
 * the first, and the list the player was looking at never moved.
 *
 * Tested here rather than through the view model because the view model does two things: it
 * calls [ChoiceGrants.toggle] and it saves. The first is the rule and is what these check; the
 * second is a database.
 */
class InvocationEditTest {

    private val startingFive = listOf(
        "agonizing_blast", "devils_sight", "armor_of_shadows", "eldritch_mind", "misty_visions",
    )

    private fun warlock(level: Int = 5, invocations: List<String> = startingFive) = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = "warlock", subclassId = "fiend",
        backgroundId = "soldier", level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        levelSelections = mapOf("$level:${ProgressionData.INVOCATION_CHOICE_ID}" to invocations),
    )

    /** What the pool card shows: the row, resolved from the character as it is now. */
    private fun pool(pc: PlayerCharacter): ResolvedChoice =
        ChoiceResolver.poolChoices(pc).first { it.choice.id == ProgressionData.INVOCATION_CHOICE_ID }

    /** One tap in the pool editor, which never enforces the count. */
    private fun tapInEditor(pc: PlayerCharacter, optionId: String): PlayerCharacter {
        val live = pool(pc)
        return ChoiceGrants.toggle(pc, live.choice, live.level, optionId, unbounded = true)
    }

    /** What the picker greys out, with the same arguments the dialog passes. */
    private fun disabledIn(pc: PlayerCharacter): Set<String> {
        val live = pool(pc)
        return OwnedOptions.disabledFor(
            choice = live.choice,
            owned = OwnedOptions.of(pc),
            currentSelection = live.selectedIds.toSet(),
            classLevels = ClassLevels.levelMap(pc),
        )
    }

    // ------------------------------------------------------------------ The report itself

    @Test
    fun `taking an invocation off the sheet takes it off the sheet`() {
        val before = warlock()
        assertTrue("devils_sight" in pool(before).selectedIds)

        val after = tapInEditor(before, "devils_sight")
        assertFalse(
            "the invocation was tapped and stayed: ${pool(after).selectedIds}",
            "devils_sight" in pool(after).selectedIds,
        )
        assertEquals(4, pool(after).selectedIds.size)
    }

    @Test
    fun `adding one adds it`() {
        val after = tapInEditor(warlock(), "eldritch_spear")
        assertTrue("eldritch_spear" in pool(after).selectedIds)
        assertEquals(6, pool(after).selectedIds.size)
    }

    /**
     * Two taps in a row, each resolved from the character the last one produced.
     *
     * This is the exact shape that failed. A dialog holding its own copy computed the second
     * tap from the first tap's *input*, so it wrote the same list back and the list on screen
     * never changed — which is what "simply does not work" looked like.
     */
    @Test
    fun `a second tap lands on top of the first`() {
        var pc = warlock()
        pc = tapInEditor(pc, "devils_sight")
        pc = tapInEditor(pc, "eldritch_mind")
        pc = tapInEditor(pc, "eldritch_spear")

        val held = pool(pc).selectedIds
        assertFalse("the first tap was undone by the second", "devils_sight" in held)
        assertFalse("the second tap did not land", "eldritch_mind" in held)
        assertTrue("the third tap did not land", "eldritch_spear" in held)
        assertEquals(setOf("agonizing_blast", "armor_of_shadows", "misty_visions", "eldritch_spear"), held.toSet())
    }

    /** Tapping the same one twice puts it back, rather than getting stuck. */
    @Test
    fun `tapping one twice leaves the sheet where it started`() {
        val before = warlock()
        val twice = tapInEditor(tapInEditor(before, "devils_sight"), "devils_sight")
        assertEquals(pool(before).selectedIds.toSet(), pool(twice).selectedIds.toSet())
    }

    // ------------------------------------------------------------------ And what follows it

    /** An invocation carries rules, and removing it has to take those too. */
    @Test
    fun `removing an invocation removes the spell it granted`() {
        val before = warlock()
        assertTrue(
            "Armor of Shadows grants Mage Armor",
            "mage_armor" in CharacterSpells.all(before).map { it.id },
        )
        val after = tapInEditor(before, "armor_of_shadows")
        assertFalse(
            "the spell outlived the invocation that granted it",
            "mage_armor" in CharacterSpells.all(after).map { it.id },
        )
    }

    /** And the question it raised stops being asked. */
    @Test
    fun `removing an invocation stops it asking its question`() {
        val before = warlock()
        val question = "invocation:agonizing_blast:cantrip"
        assertTrue(
            "Agonizing Blast should be asking which cantrip",
            ChoiceResolver.all(before).any { it.choice.id == question },
        )
        val after = tapInEditor(before, "agonizing_blast")
        assertFalse(
            "the invocation is gone and its question is still on the sheet",
            ChoiceResolver.all(after).any { it.choice.id == question },
        )
    }

    /** Adding one back brings its rules with it, immediately. */
    @Test
    fun `adding an invocation brings its rules with it`() {
        val pc = tapInEditor(warlock(), "pact_chain")
        assertTrue(
            "Pact of the Chain says you learn Find Familiar",
            "find_familiar" in CharacterSpells.all(pc).map { it.id },
        )
    }

    // ------------------------------------------------------------------ What it refuses

    /**
     * Edit Mode does not argue about the count — a DM who hands out an extra invocation is
     * allowed to — but it does not offer what the rules gate behind a level either.
     */
    @Test
    fun `the editor never offers an invocation the character cannot take`() {
        val early = warlock(level = 2, invocations = listOf("devils_sight", "armor_of_shadows", "eldritch_mind"))
        val disabled = disabledIn(early)

        assertTrue("Lifedrinker needs Warlock 9", "lifedrinker" in disabled)
        assertTrue("Devouring Blade needs Thirsting Blade", "devouring_blade" in disabled)
        assertFalse("Devil's Sight is held and must stay tappable", "devils_sight" in disabled)
        assertFalse("Agonizing Blast is legal at level 2", "agonizing_blast" in disabled)
    }

    /** Going over the count is marked, never blocked. */
    @Test
    fun `the editor lets a table hand out more than the table allows`() {
        var pc = warlock()
        listOf("eldritch_spear", "gaze_of_two_minds", "otherworldly_leap").forEach {
            pc = tapInEditor(pc, it)
        }
        val row = pool(pc)
        assertEquals(8, row.selectedIds.size)
        assertTrue("the row should mark it as over", row.selectedIds.size > row.choice.count)
    }

    // ------------------------------------------------------------------ The other editor

    /**
     * The Features tab opens the same question through a different dialog, and that one keeps
     * the count: at the limit the oldest pick gives way rather than the tap doing nothing.
     */
    @Test
    fun `the features tab editor swaps rather than refusing when the list is full`() {
        val before = warlock()
        val live = ChoiceResolver.all(before)
            .first { it.choice.id == ProgressionData.INVOCATION_CHOICE_ID }
        assertEquals("a level 5 Warlock knows five", 5, live.choice.count)

        val after = ChoiceGrants.toggle(before, live.choice, live.level, "eldritch_spear")
        val held = pool(after).selectedIds
        assertEquals("the count is kept", 5, held.size)
        assertTrue("the new pick did not land", "eldritch_spear" in held)
        assertFalse("the oldest pick should have given way", "agonizing_blast" in held)
    }

    /**
     * Every answer the sheet shows is the answer the rules read. A Warlock is the shape that
     * broke it: the same question is asked again at eight levels, so it lives under eight
     * keys, and a write that left the others behind was read back as the old answer.
     */
    @Test
    fun `after an edit the sheet and the rules agree`() {
        var pc = warlock(level = 12, invocations = startingFive)
        // An older answer from a lower level, the way a real sheet accumulates them.
        pc = pc.copy(
            levelSelections = pc.levelSelections +
                ("5:${ProgressionData.INVOCATION_CHOICE_ID}" to listOf("devils_sight")),
        )
        pc = tapInEditor(pc, "eldritch_spear")

        val answers = ChoiceResolver.answers(pc)
        val shown = pool(pc)
        assertEquals(
            "the sheet shows one answer and the rules read another",
            answers[ProgressionData.INVOCATION_CHOICE_ID].orEmpty().toSet(),
            shown.selectedIds.toSet(),
        )
        assertTrue("eldritch_spear" in shown.selectedIds)
        assertEquals(
            "one question, one home",
            1,
            pc.levelSelections.keys.count { it.endsWith(":${ProgressionData.INVOCATION_CHOICE_ID}") },
        )
    }
}
