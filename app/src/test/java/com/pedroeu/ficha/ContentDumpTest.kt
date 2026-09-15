package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.MagicItemData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SubclassData
import java.io.File
import org.junit.Test

/**
 * Writes everything the app ships as JSON, so it can be compared against the books.
 *
 * Not a test — a tool that runs in the test harness because that is the only place with the
 * content on the classpath. It exists because "check the app against the reference site" is
 * otherwise a person reading two windows side by side, which is how a truncated description
 * and a renamed feature survived several passes: the eye slides over the ninety-nine entries
 * that are right to find the one that is not.
 *
 * With both sides as data, the comparison is a diff. It is repeatable, it takes a minute, and
 * it says exactly which entries differ and how — which is the whole argument of
 * `docs/testing.md` applied to content rather than to code.
 *
 * Output: one JSON file per table under `app/build/content-dump`.
 */
class ContentDumpTest {

    private val out = File("build/content-dump").also { it.mkdirs() }

    private fun String.q() = "\"" + this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "")
        .replace("\t", "\\t") + "\""

    private fun write(name: String, rows: List<Map<String, Any?>>) {
        val json = rows.joinToString(",\n", "[\n", "\n]") { row ->
            "  {" + row.entries.joinToString(", ") { (k, v) ->
                val value = when (v) {
                    null -> "null"
                    is String -> v.q()
                    is Number, is Boolean -> v.toString()
                    is Collection<*> -> v.joinToString(", ", "[", "]") { it.toString().q() }
                    else -> v.toString().q()
                }
                "${k.q()}: $value"
            } + "}"
        }
        File(out, "$name.json").writeText(json)
        println("dumped ${rows.size} to ${File(out, "$name.json").absolutePath}")
    }

    @Test
    fun `dump every table the app ships`() {
        write("spells", SpellData.ALL.map {
            mapOf(
                "id" to it.id, "name" to it.name, "level" to it.level, "school" to it.school,
                "castingTime" to it.castingTime, "range" to it.range,
                "components" to it.components, "duration" to it.duration,
                "classes" to it.classes.sorted(), "ritual" to it.ritual,
                "concentration" to it.concentration, "book" to it.book.id,
                "description" to it.description,
            )
        })

        write("feats", FeatData.ALL.map {
            mapOf(
                "id" to it.id, "name" to it.name, "category" to it.category.name,
                "book" to it.book.id, "description" to it.description,
            )
        })

        write("species", SpeciesData.ALL.map {
            mapOf(
                "id" to it.id, "name" to it.name, "size" to it.size, "speed" to it.speed,
                "darkvision" to it.darkvisionRange, "book" to it.book.id,
                "summary" to it.summary,
                "traits" to it.traits.map { t -> "${t.name}: ${t.description}" },
                "lineages" to it.lineageOptions.map { l -> "${l.name}: ${l.description}" },
            )
        })

        write("backgrounds", BackgroundData.ALL.map {
            mapOf(
                "id" to it.id, "name" to it.name, "book" to it.book.id,
                "abilities" to it.abilityOptions.map { a -> a.name },
                "skills" to it.skillProficiencies.map { s -> s.displayName },
                "tool" to it.toolProficiency, "feat" to it.featId,
                "gold" to it.startingGold, "equipment" to it.equipment,
                "summary" to it.summary,
            )
        })

        write("subclasses", SubclassData.ALL.map {
            mapOf(
                "id" to it.id, "classId" to it.classId, "name" to it.name, "book" to it.book.id,
                "summary" to it.summary,
                "features" to it.features.map { f -> "${f.level}|${f.name}|${f.description}" },
            )
        })

        write("classFeatures", ProgressionData.ALL.flatMap { progression ->
            progression.features.map { f ->
                mapOf(
                    "classId" to progression.classId, "level" to f.level,
                    "name" to f.name, "description" to f.description,
                )
            }
        })

        write("magicItems", MagicItemData.ALL.map {
            mapOf(
                "id" to it.id, "name" to it.name, "rarity" to it.rarity.name,
                "kind" to it.kind, "attunement" to it.requiresAttunement,
                "attunementNote" to it.attunementNote, "book" to it.book.id,
                "description" to it.description,
            )
        })
    }
}
