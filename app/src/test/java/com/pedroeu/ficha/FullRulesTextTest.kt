package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.MagicItemData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.ui.components.LONG_TEXT_THRESHOLD
import com.pedroeu.ficha.ui.components.firstSentenceOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The catalogues carry the books' text in full, and the lists show a name until you ask for
 * the rest.
 *
 * The truncation these replace was mine: the import capped descriptions at 1200 characters
 * and appended an ellipsis, which quietly cut the back half off Wish, Simulacrum and 52
 * others — the half with the restrictions in it.
 */
class FullRulesTextTest {

    @Test
    fun `nothing in the catalogues is cut off mid-rule`() {
        val truncated = buildList {
            addAll(SpellData.ALL.filter { it.description.contains("[...]") }.map { it.id })
            addAll(FeatData.ALL.filter { it.description.contains("[...]") }.map { it.id })
            addAll(MagicItemData.ALL.filter { it.description.contains("[...]") }.map { it.id })
        }
        assertTrue("these are still truncated: $truncated", truncated.isEmpty())
    }

    @Test
    fun `the long entries really are long, so the sheet has something to do`() {
        val long = SpellData.ALL.count { it.description.length > LONG_TEXT_THRESHOLD }
        assertTrue("most spells should need the full-text sheet, got $long", long > 300)
    }

    @Test
    fun `a spell whose text ran past the old cap is now complete`() {
        // Wish is the longest entry in the books and was the worst casualty of the cap.
        val wish = SpellData.ALL.first { it.id == "wish" }
        assertTrue("Wish is pages long", wish.description.length > 1200)
        assertFalse(wish.description.endsWith("[...]"))
    }

    // ------------------------------------------------------------- The preview line

    @Test
    fun `a short description is its own preview`() {
        assertEquals("You gain Darkvision.", firstSentenceOf("You gain Darkvision."))
    }

    @Test
    fun `a long description previews its first sentence`() {
        val text = "You hurl a mote of fire. Make a ranged spell attack against the target, " +
            "and on a hit it takes 1d10 Fire damage and any flammable object it carries " +
            "that is not being worn begins to burn."
        val preview = firstSentenceOf(text)
        assertEquals("You hurl a mote of fire.", preview)
        assertTrue("the preview must be shorter than what it previews", preview.length < text.length)
    }

    @Test
    fun `a long first sentence is cut at a word, not mid-word`() {
        val text = "A wall of strong wind rises from the ground at a point you choose within " +
            "range and you can make the wall up to fifty feet long and fifteen feet high " +
            "and one foot thick, shaped however you like."
        val preview = firstSentenceOf(text)
        assertTrue("it has to end in an ellipsis", preview.endsWith("…"))
        assertFalse("and not split a word", preview.dropLast(1).endsWith("-"))
        assertTrue(text.startsWith(preview.dropLast(1).trimEnd()))
    }
}
