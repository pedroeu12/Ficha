package com.pedroeu.ficha

import com.pedroeu.ficha.ui.i18n.AppLanguage
import com.pedroeu.ficha.ui.i18n.Language
import com.pedroeu.ficha.ui.i18n.PortugueseStrings
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.i18n.trf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The interface in two languages.
 *
 * The table is keyed by the English string itself, which makes an untranslated phrase harmless
 * — it falls through and reads correctly in English — but also makes it invisible. These tests
 * are what keeps that from turning into a half-translated screen: the phrases reached
 * indirectly, through an enum's label or a list of tab names, are the ones a reader would
 * never notice were missing.
 */
class LanguageTest {

    @After
    fun reset() {
        Language.current = AppLanguage.ENGLISH
    }

    private fun inPortuguese(text: String): String {
        Language.current = AppLanguage.PORTUGUESE
        return tr(text)
    }

    // ------------------------------------------------------------- The lookup

    @Test
    fun `English is the text as written`() {
        Language.current = AppLanguage.ENGLISH
        assertEquals("Inventory", tr("Inventory"))
        assertEquals("anything at all", tr("anything at all"))
    }

    @Test
    fun `Portuguese translates what it knows`() {
        assertEquals("Inventário", inPortuguese("Inventory"))
        assertEquals("Perícias", inPortuguese("Skills"))
        assertEquals("Pontos de Vida", inPortuguese("Hit Points"))
    }

    @Test
    fun `an untranslated phrase falls back to English rather than to a blank`() {
        val unknown = "A phrase nobody has translated yet"
        assertEquals(unknown, inPortuguese(unknown))
    }

    @Test
    fun `the language is remembered by its tag`() {
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromTag("pt-BR"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals("an unset preference means English", AppLanguage.ENGLISH, AppLanguage.fromTag(null))
        assertEquals("so does a tag from a build that had more", AppLanguage.ENGLISH, AppLanguage.fromTag("fr"))
    }

    @Test
    fun `each language names itself, so the menu is readable either way`() {
        assertEquals("English", AppLanguage.ENGLISH.label)
        assertEquals("Português (Brasil)", AppLanguage.PORTUGUESE.label)
    }

    // ------------------------------------------------------------- The table itself

    @Test
    fun `no entry is blank`() {
        PortugueseStrings.knownKeys().forEach { key ->
            assertTrue("the key '$key' is empty", key.isNotBlank())
            assertTrue("'$key' translates to nothing", PortugueseStrings.of(key).isNotBlank())
        }
    }

    @Test
    fun `no entry is the English text repeated`() {
        // A key mapped to itself is an entry someone meant to translate and didn't. The few
        // words that are genuinely the same in both languages are named here so the check
        // stays meaningful for everything else.
        val sameInBoth = setOf("Ficha", "OK", "Ritual", "Backup", "mod")

        PortugueseStrings.knownKeys()
            .filterNot { it in sameInBoth }
            .forEach { key ->
                assertNotEquals("'$key' was never translated", key, PortugueseStrings.of(key))
            }
    }

    @Test
    fun `the phrases reached through an enum are all covered`() {
        // These never appear as tr("...") in a screen — they are constructor arguments read
        // back through a property — so nothing else would notice them going missing.
        val throughEnums = listOf(
            // ThemeMode
            "Follow system", "Daylight", "Candlelight",
            // CreationStep
            "Choose a Species", "Species", "Choose an Origin", "Origin",
            "Choose a Class", "Class", "Class Options", "Options",
            "Origin Options", "Grants", "Ability Scores", "Abilities",
            "Name & Details", "Details",
            // LevelUpStep
            "Hit Points", "HP", "Choose a Subclass", "Subclass",
            "New Features", "Features", "Ability Scores or Feat", "Improve",
            "Feat Options", "Feat", "New Spells", "Spells", "Review",
            // HitPointMethod and AsiMode
            "Take the average", "Roll the die", "Enter it yourself",
            "+2 to one ability", "+1 to two abilities", "Take a feat instead",
            // RestKind
            "Short Rest", "Long Rest",
        )

        throughEnums.forEach { key ->
            assertNotEquals("'$key' is shown on a step header and has no translation",
                key, PortugueseStrings.of(key))
        }
    }

    @Test
    fun `every tab on the character sheet is translated`() {
        listOf("Stats", "Skills", "Combat", "Features", "Spells", "Inventory", "Bio")
            .forEach { tab ->
                assertNotEquals("the '$tab' tab is untranslated", tab, PortugueseStrings.of(tab))
            }
    }

    @Test
    fun `every alignment is translated`() {
        // Stored in English on the character, translated only where the chip is drawn, so
        // both halves have to line up.
        listOf(
            "Lawful Good", "Neutral Good", "Chaotic Good",
            "Lawful Neutral", "True Neutral", "Chaotic Neutral",
            "Lawful Evil", "Neutral Evil", "Chaotic Evil",
        ).forEach { alignment ->
            assertNotEquals(alignment, PortugueseStrings.of(alignment))
        }
    }

    @Test
    fun `the game's own vocabulary uses the words players use`() {
        // Not a literal rendering of the English: a Brazilian table says perícia, talento,
        // truque, and espaço de magia, and a sheet that said otherwise would read as a
        // machine translation.
        assertEquals("Perícias", inPortuguese("Skills"))
        assertEquals("Talentos", inPortuguese("Feats"))
        assertEquals("Truque", inPortuguese("Cantrip"))
        assertEquals("Espaços de Magia", inPortuguese("Spell Slots"))
        assertEquals("Classe de Armadura", inPortuguese("Armor Class"))
        assertEquals("Testes de Resistência", inPortuguese("Saving Throws"))
        assertEquals("Descanso Longo", inPortuguese("Long Rest"))
        assertEquals("Dados de Vida", inPortuguese("Hit Dice"))
    }

    @Test
    fun `the coin abbreviations don't collide`() {
        // Silver is PP in Portuguese and platinum is PP in English, which is exactly the kind
        // of clash that turns into a wrong number on the sheet.
        val coins = listOf("CP", "SP", "EP", "GP", "PP").map { inPortuguese(it) }
        assertEquals(listOf("PC", "PP", "PE", "PO", "PL"), coins)
        assertEquals("each coin needs its own abbreviation", coins.size, coins.distinct().size)
    }

    @Test
    fun `a phrase with values keeps its placeholders`() {
        Language.current = AppLanguage.PORTUGUESE
        assertEquals("Nível 5 — Extra Attack", trf("Level {0} — {1}", 5, "Extra Attack"))
        assertEquals("Excluir Varek?", trf("Delete {0}?", "Varek"))
        assertEquals("2 de 2 criados", trf("{0} of {1} made", 2, 2))
    }

    @Test
    fun `every translated phrase carries the placeholders its English has`() {
        // A translation that dropped a {0} would silently swallow a number the player needs.
        val placeholder = Regex("\\{\\d}")
        PortugueseStrings.knownKeys().forEach { key ->
            assertEquals(
                "'$key' and its translation disagree about placeholders",
                placeholder.findAll(key).map { it.value }.toSet(),
                placeholder.findAll(PortugueseStrings.of(key)).map { it.value }.toSet(),
            )
        }
    }

    @Test
    fun `rules text is left in English on purpose`() {
        // The table covers the interface. A spell's description, a feat's wording, and an
        // item's effect are quotations from the rulebook, and a loose translation of a rule
        // is worse than the rule in the words the book uses.
        val rulesText = "You have a +1 bonus to attack rolls and damage rolls made with " +
            "this magic weapon."
        assertEquals(rulesText, inPortuguese(rulesText))
        assertFalse(PortugueseStrings.knownKeys().any { it.length > 400 })
    }
}
