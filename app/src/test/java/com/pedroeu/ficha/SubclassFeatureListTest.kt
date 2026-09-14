package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.SubclassData
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every subclass's feature list, held against the reference wiki's.
 *
 * `wiki-subclass-features.tsv` is 400 rows taken off dnd2024.wikidot.com — one per printed
 * feature, with the level it arrives at. Nothing in it is typed by hand, and nothing in the
 * app decides what it says, so the two lists are genuinely independent.
 *
 * The sweep that produced it found seven subclasses out of step: the Great Old One was missing
 * Thought Shield and its spell table, the Circle of the Moon its spell table — which also
 * granted no spells at all — the Warrior of Mercy its Implements of Mercy, the Diviner had The
 * Third Eye buried inside the level 6 feature four levels early, and the Reanimator still
 * carried its playtest shape after Ravenloft reprinted it with three features renamed.
 */
class SubclassFeatureListTest {

    private data class Row(val classId: String, val slug: String, val level: Int, val name: String)

    private fun wiki(): List<Row> =
        javaClass.classLoader!!.getResourceAsStream("wiki-subclass-features.tsv")!!
            .bufferedReader().readLines()
            .filter { it.isNotBlank() }
            .map { it.split("\t") }
            .map { Row(it[0], it[1], it[2].toInt(), it[3]) }

    private fun key(name: String) = name.lowercase().filter { it.isLetterOrDigit() }

    /**
     * Features the app splits out that the book folds into another one's text.
     *
     * Both are counts: a Battle Master learns three maneuvers with Combat Superiority and more
     * at 7, 10 and 15; an Arcane Archer another Arcane Shot at 7, 10, 15 and 18. The book
     * states those inside the feature's own text; the app makes each an entry of its own,
     * because that is what carries the extra pick to the level-up flow.
     */
    private val splitOutByTheApp = setOf(
        "Maneuvers", "Additional Maneuvers", "Additional Arcane Shot",
    )

    @Test
    fun `every subclass carries the features the book gives it, at the levels it gives them`() {
        val byWiki = wiki().groupBy { it.classId to it.slug }
        val problems = mutableListOf<String>()

        byWiki.forEach { (id, rows) ->
            val (classId, slug) = id
            val subclass = SubclassData.ALL.firstOrNull {
                it.classId == classId && key(it.name) == key(slug.replace("-", " "))
            } ?: run {
                problems += "no subclass in the app for $classId:$slug"
                return@forEach
            }
            val app = subclass.features.map { it.level to key(it.name) }.toSet()
            val appNames = subclass.features.map { key(it.name) }.toSet()

            rows.forEach { row ->
                val k = row.level to key(row.name)
                if (k in app) return@forEach
                if (key(row.name) in appNames) {
                    val at = subclass.features.first { key(it.name) == key(row.name) }.level
                    problems += "${subclass.id}: ${row.name} is at level $at, the book puts it at ${row.level}"
                } else {
                    problems += "${subclass.id}: missing ${row.name} (level ${row.level})"
                }
            }

            val wikiNames = rows.map { key(it.name) }.toSet()
            subclass.features
                .filter { key(it.name) !in wikiNames && it.name !in splitOutByTheApp }
                .forEach { problems += "${subclass.id}: ${it.name} is in the app and not in the book" }
        }

        assertTrue(
            "the app and the reference wiki disagree about these:\n" +
                problems.joinToString("\n") { "  $it" },
            problems.isEmpty(),
        )
    }
}
