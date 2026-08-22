package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.content.ItemCatalog
import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.model.Sourcebook
import com.pedroeu.ficha.ui.components.SourceGrouping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pickers went from a dozen entries to several hundred, so they search and split by book.
 *
 * The grouping rules matter more than they look: grouping a search's own results buries them,
 * and grouping a list that came from one book is pure noise.
 */
class SourceGroupingTest {

    private data class Row(val name: String, val book: Sourcebook?)

    @Test
    fun `searching is a plain substring match, case and space insensitive at the edges`() {
        val rows = listOf(Row("Fireball", Sourcebook.PHB), Row("Fire Bolt", Sourcebook.PHB))
        assertEquals(2, SourceGrouping.matching(rows, "fire") { it.name }.size)
        assertEquals(1, SourceGrouping.matching(rows, "  BALL ") { it.name }.size)
        assertEquals("a blank query is everything", 2, SourceGrouping.matching(rows, "") { it.name }.size)
        assertTrue(SourceGrouping.matching(rows, "zzz") { it.name }.isEmpty())
    }

    @Test
    fun `groups come out in the book order, with unsourced entries last`() {
        val rows = listOf(
            Row("a", Sourcebook.LORWYN),
            Row("b", null),
            Row("c", Sourcebook.PHB),
            Row("d", Sourcebook.PHB),
        )
        val groups = SourceGrouping.byBook(rows) { it.book }
        assertEquals(
            listOf(Sourcebook.PHB, Sourcebook.LORWYN, null),
            groups.map { it.first },
        )
        assertEquals(2, groups.first().second.size)
    }

    @Test
    fun `grouping is skipped when it would not help`() {
        val oneBook = List(40) { Row("x$it", Sourcebook.PHB) }
        assertFalse("one book is not a grouping", SourceGrouping.worthGrouping(oneBook) { it.book })

        val short = listOf(Row("a", Sourcebook.PHB), Row("b", Sourcebook.LORWYN))
        assertFalse("a short list reads better straight", SourceGrouping.worthGrouping(short) { it.book })

        val long = List(20) { Row("x$it", if (it % 2 == 0) Sourcebook.PHB else Sourcebook.LORWYN) }
        assertTrue(SourceGrouping.worthGrouping(long) { it.book })
    }

    // ------------------------------------------------------------- The data behind it

    @Test
    fun `spell choice options carry the book they came from`() {
        val cantrips = OriginChoices.forSpecies("human")
        assertTrue(cantrips.isNotEmpty())

        val wizardCantrips = SpellData.forClass("wizard", 0)
        assertTrue("there are enough to be worth grouping", wizardCantrips.size >= 12)
        assertTrue(
            "every option must name a book, or it lands under Other",
            wizardCantrips.all { it.book in Sourcebook.EVERYTHING },
        )
    }

    @Test
    fun `origin feat options carry their book through to the picker`() {
        val options = OriginChoices.forSpecies("human", Sourcebook.EVERYTHING).first().options
        assertTrue("options must be book-tagged to group", options.all { it.book != null })
        assertTrue(
            "and they really do span several books",
            options.mapNotNull { it.book }.distinct().size > 1,
        )
    }

    @Test
    fun `background feat options carry their book too`() {
        val options =
            OriginChoices.forBackgroundFeat("haunted_one", Sourcebook.EVERYTHING).first().options
        assertTrue(options.all { it.book != null })
    }

    @Test
    fun `magic items in the item picker know their book, mundane gear does not`() {
        val magic = ItemCatalog.ALL.filter { it.id.startsWith("magic:") }
        assertTrue(magic.isNotEmpty())
        assertTrue("a magic item comes from a book", magic.all { it.book != null })

        val mundane = ItemCatalog.ALL.filterNot { it.id.startsWith("magic:") }
        assertTrue("ordinary gear is not book-specific", mundane.all { it.book == null })
    }

    @Test
    fun `the lists that got long are the ones grouping now covers`() {
        assertTrue(SourceGrouping.worthGrouping(BackgroundData.ALL) { it.book })
        assertTrue(SourceGrouping.worthGrouping(SpellData.ALL) { it.book })
    }
}
