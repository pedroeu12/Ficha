package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.PerUseChoiceData
import com.pedroeu.ficha.data.content.SpeciesData
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every species' trait list, held against the reference wiki's.
 *
 * The same check as [SubclassFeatureListTest] and it found the same kind of gap: six species
 * were short a trait outright — the Dhampir's Necrotic Resistance, the Goliath's Large Form,
 * the Halfling's Nimbleness, the Reborn's Advantage on Death Saves, and the Flamekin's and
 * Rimekin's elemental Resistances — and the Aasimar's Celestial Revelation still offered the
 * 2014 options, Radiant Soul and Radiant Consumption, which the 2024 book renamed Heavenly
 * Wings and Inner Radiance.
 *
 * `wiki-species-traits.tsv` is 107 rows scraped from the wiki, one per named trait.
 */
class SpeciesTraitListTest {

    private fun wiki(): List<Pair<String, String>> =
        javaClass.classLoader!!.getResourceAsStream("wiki-species-traits.tsv")!!
            .bufferedReader().readLines()
            .filter { it.isNotBlank() }
            .map { it.split("\t") }
            .map { it[0] to it[1] }

    private fun key(n: String) = n.lowercase().filter { it.isLetterOrDigit() }

    /**
     * Named in the book's trait block but not a trait of its own in the app, with the reason.
     *
     * Three shapes: something the app models as a field rather than a trait (Darkvision has a
     * range on the species and a row on the sheet), the heading of a lineage table, and a
     * sub-option of a trait the app carries as choice options.
     */
    private val notATraitHere = mapOf(
        "Darkvision" to "a range on the species, shown as its own row on the sheet",
        "Draconic Ancestry" to "the heading of the Dragonborn's lineage table",
        "Elven Lineage" to "the heading of the Elf's lineage table",
        "Gnomish Lineage" to "the heading of the Gnome's lineage table",
        "Fiendish Legacy" to "the heading of the Tiefling's lineage table",
        "Giant Ancestry" to "the heading of the Goliath's lineage table",
        "Luck" to "the Halfling's, which the app carries as Lucky",
        "Heavenly Wings" to "an option of Celestial Revelation",
        "Inner Radiance" to "an option of Celestial Revelation",
        "Necrotic Shroud" to "an option of Celestial Revelation",
        "Ardor" to "an option of the Duskling's Inner Magic",
        "Mobility" to "an option of the Duskling's Inner Magic",
        "Vigor" to "an option of the Duskling's Inner Magic",
        "Drain" to "an option of the Dhampir's Vampiric Bite",
        "Strengthen" to "an option of the Dhampir's Vampiric Bite",
    )

    @Test
    fun `every species carries the traits the book gives it`() {
        val perUseOptions = PerUseChoiceData.ALL
            .flatMap { c -> c.options.map { key(it.name) } }
            .toSet()
        val missing = wiki().filter { (speciesId, trait) ->
            val species = SpeciesData.byId(speciesId) ?: return@filter true
            val names = (species.traits.map { key(it.name) } +
                species.lineageOptions.map { key(it.name) }).toSet()
            key(trait) !in names &&
                key(trait) !in perUseOptions &&
                trait !in notATraitHere
        }.map { "${it.first}: ${it.second}" }

        assertTrue(
            "the book lists these traits and the app does not:\n" +
                missing.joinToString("\n") { "  $it" },
            missing.isEmpty(),
        )
    }

    @Test
    fun `nothing is excused that the book no longer names`() {
        val named = wiki().map { it.second }.toSet()
        val stale = notATraitHere.keys - named
        assertTrue("excused but the book names no such trait: $stale", stale.isEmpty())
    }
}
