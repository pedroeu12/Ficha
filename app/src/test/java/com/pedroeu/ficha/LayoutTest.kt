package com.pedroeu.ficha

import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.ui.i18n.AppLanguage
import com.pedroeu.ficha.ui.i18n.Language
import com.pedroeu.ficha.ui.layout.LayoutMode
import com.pedroeu.ficha.ui.layout.distributeInto
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Choosing between the tabbed sheet and the full one.
 *
 * The interesting part isn't the switch, it's what happens at the edges: a phone held in
 * landscape, a tablet in portrait, a folding phone halfway open. The rule has to be a width
 * rather than a device, and it has to be overridable, because someone with a small tablet may
 * well prefer the tabs and someone with a large phone may not.
 */
class LayoutTest {

    @After
    fun reset() {
        Language.current = AppLanguage.ENGLISH
    }

    // ------------------------------------------------------------- Deciding

    @Test
    fun `automatic follows the width of the window`() {
        // A phone, in either orientation.
        assertFalse(LayoutMode.AUTOMATIC.isWide(392.dp))
        assertFalse(LayoutMode.AUTOMATIC.isWide(780.dp))
        // A tablet.
        assertTrue(LayoutMode.AUTOMATIC.isWide(840.dp))
        assertTrue(LayoutMode.AUTOMATIC.isWide(1280.dp))
    }

    @Test
    fun `an explicit choice wins over the width`() {
        assertFalse("a tablet still gets tabs if that's what was asked for",
            LayoutMode.PHONE.isWide(1280.dp))
        assertTrue("and a phone gets the full sheet if that's what was asked for",
            LayoutMode.TABLET.isWide(392.dp))
    }

    @Test
    fun `the preference survives a launch, and an unset one means automatic`() {
        LayoutMode.entries.forEach { mode ->
            assertEquals(mode, LayoutMode.fromName(mode.name))
        }
        assertEquals(LayoutMode.AUTOMATIC, LayoutMode.fromName(null))
        assertEquals("a value from a build with more modes", LayoutMode.AUTOMATIC,
            LayoutMode.fromName("HOLOGRAM"))
    }

    @Test
    fun `every mode names itself, in both languages`() {
        LayoutMode.entries.forEach { mode ->
            assertTrue("${mode.name} has no label", mode.label.isNotBlank())
        }

        Language.current = AppLanguage.PORTUGUESE
        LayoutMode.entries.forEach { mode ->
            assertTrue(
                "${mode.name} is untranslated: '${mode.label}'",
                mode.label != mode.name,
            )
        }
    }

    // ------------------------------------------------------------- Filling the columns

    @Test
    fun `the front of the sheet fills whatever columns there are`() {
        val front = listOf("Stats", "Skills", "Combat", "Features")

        assertEquals(listOf(front), front.distributeInto(1))
        assertEquals(
            listOf(listOf("Stats", "Skills"), listOf("Combat", "Features")),
            front.distributeInto(2),
        )
        assertEquals(
            listOf(listOf("Stats", "Skills"), listOf("Combat"), listOf("Features")),
            front.distributeInto(3),
        )
        assertEquals(
            front.map { listOf(it) },
            front.distributeInto(4),
        )
    }

    @Test
    fun `the leftmost column takes the extra section`() {
        // The front of the sheet is read left to right, so the odd one out goes where the
        // player is already looking rather than at the far edge.
        val back = listOf("Spells", "Inventory", "Bio")
        assertEquals(
            listOf(listOf("Spells", "Inventory"), listOf("Bio")),
            back.distributeInto(2),
        )
    }

    @Test
    fun `no section is ever lost or repeated`() {
        val sections = listOf("a", "b", "c", "d", "e", "f", "g")
        (1..9).forEach { columns ->
            val distributed = sections.distributeInto(columns)
            assertEquals(
                "$columns columns dropped or duplicated a section",
                sections,
                distributed.flatten(),
            )
            assertTrue("$columns columns left one empty", distributed.none { it.isEmpty() })
        }
    }

    @Test
    fun `asking for more columns than there are sections gives one each`() {
        val two = listOf("Spells", "Bio")
        assertEquals(listOf(listOf("Spells"), listOf("Bio")), two.distributeInto(5))
    }

    @Test
    fun `an empty sheet doesn't crash the layout`() {
        assertEquals(listOf(emptyList<String>()), emptyList<String>().distributeInto(3))
    }
}
