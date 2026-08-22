package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.SourceFiltering
import com.pedroeu.ficha.data.model.Sourcebook
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.creation.CreationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A character is built from a chosen set of books, and nothing outside that set may ever be
 * offered to it — at creation, at level up, or from the sheet's own pickers.
 *
 * The rule these protect is the one a table actually cares about: if you said "core only" when
 * you made the character, level 12 must not quietly offer you a Lorwyn feat.
 */
class SourcebookTest {

    // ------------------------------------------------------------------ The book list

    @Test
    fun `book ids are unique and stable-looking`() {
        val ids = Sourcebook.ALL.map { it.id }
        assertEquals("two books share an id, so saved characters would collide", ids.size, ids.toSet().size)
        assertTrue(
            "ids are written to saved characters, so keep them lowercase and plain",
            ids.all { it.matches(Regex("[a-z0-9_]+")) },
        )
    }

    @Test
    fun `the core set is exactly the two rulebooks a table is assumed to own`() {
        assertEquals(setOf(Sourcebook.PHB, Sourcebook.DMG), Sourcebook.CORE)
    }

    // ------------------------------------------------------------------ Reading it back

    @Test
    fun `a character saved before books existed gets all of them, not none`() {
        // The alternative empties every picker on an existing sheet.
        val old = PlayerCharacter(id = "old")
        assertEquals(Sourcebook.EVERYTHING, old.enabledSources)
    }

    @Test
    fun `an unknown book id is dropped rather than failing the load`() {
        val character = PlayerCharacter(id = "c", enabledSourceIds = setOf("phb", "a_book_we_removed"))
        assertEquals(setOf(Sourcebook.PHB), character.enabledSources)
        assertNull(Sourcebook.byId("a_book_we_removed"))
    }

    @Test
    fun `chosen books survive the round trip through ids`() {
        val chosen = setOf(Sourcebook.PHB, Sourcebook.LORWYN, Sourcebook.UA_VILLAINOUS)
        val character = PlayerCharacter(id = "c", enabledSourceIds = chosen.map { it.id }.toSet())
        assertEquals(chosen, character.enabledSources)
    }

    // ------------------------------------------------------------------ Filtering

    @Test
    fun `filtering keeps only what the chosen books allow`() {
        val coreOnly = SourceFiltering.available(SpeciesData.ALL, setOf(Sourcebook.PHB))
        assertTrue(coreOnly.isNotEmpty())
        assertTrue("a non-PHB species slipped through", coreOnly.all { it.book == Sourcebook.PHB })
    }

    @Test
    fun `no books means no options, which is why the first step demands one`() {
        assertTrue(SourceFiltering.available(SpeciesData.ALL, emptySet()).isEmpty())
        assertFalse(CreationState(enabledSources = emptySet()).canAdvance)
        assertTrue(CreationState(enabledSources = Sourcebook.CORE).canAdvance)
    }

    // ------------------------------------------------------------------ Every catalogue

    @Test
    fun `every catalogue is filterable and every entry names a book`() {
        // Naming the catalogues explicitly so a new one has to be added here to be forgotten.
        val everything = SpeciesData.ALL.map { it.book } +
            BackgroundData.ALL.map { it.book } +
            ClassData.ALL.map { it.book } +
            SubclassData.ALL.map { it.book } +
            FeatData.ALL.map { it.book } +
            SpellData.ALL.map { it.book }
        assertTrue("a catalogue is empty, so this test proves nothing", everything.size > 500)
        assertTrue("a book outside the enum reached a catalogue", everything.all { it in Sourcebook.EVERYTHING })
    }

    @Test
    fun `filtering a catalogue by everything is the catalogue itself`() {
        assertEquals(
            SpellData.ALL.size,
            SourceFiltering.available(SpellData.ALL, Sourcebook.EVERYTHING).size,
        )
    }

    // ------------------------------------------------------------------ Creation wiring

    @Test
    fun `the creation state offers only what its books allow`() {
        val core = CreationState(enabledSources = setOf(Sourcebook.PHB))
        assertTrue("species outside the PHB were offered", core.availableSpecies.all { it.book == Sourcebook.PHB })
        assertTrue("classes outside the PHB were offered", core.availableClasses.all { it.book == Sourcebook.PHB })
        assertTrue("backgrounds outside the PHB were offered", core.availableBackgrounds.all { it.book == Sourcebook.PHB })
    }
}
