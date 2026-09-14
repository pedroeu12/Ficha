package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every subclass, species, lineage, background and feat the app ships, held against the rules
 * that must be true of any character.
 *
 * This is the answer to "every time I make a character with a subclass I have not tried, I
 * find thirty bugs I already asked you to fix." They were real and they were all findable: the
 * app has 13 classes, 90-odd subclasses, 40-odd species and 200-odd feats, and the tests only
 * ever walked the handful that had been reported. So a fix landed on the Warlock and the same
 * mistake sat untouched in the Druid, waiting for somebody to roll one.
 *
 * Nothing here knows about any particular feature. It builds a character out of each piece of
 * content in turn, answers every question that character is asked, and runs [SheetAudit] over
 * the result — so a new invariant, written once, is checked against the whole game, and a new
 * subclass is checked against every rule ever broken by an old one.
 *
 * When this fails it prints every complaint at once, because finding them one run at a time is
 * exactly the loop this exists to end.
 */
class EveryContentSweepTest {

    private fun report(kind: String, complaints: List<String>) {
        assertTrue(
            "${complaints.size} problems across every $kind in the game:\n\n" +
                complaints.joinToString("\n") + "\n",
            complaints.isEmpty(),
        )
    }

    /** The levels worth checking a subclass at: where it arrives, and where it finishes. */
    private fun levelsFor(subclassId: String): List<Int> {
        val levels = SubclassData.byId(subclassId)?.features?.map { it.level }?.distinct().orEmpty()
        if (levels.isEmpty()) return listOf(3)
        return listOfNotNull(levels.min(), levels.max()).distinct()
    }

    // ================================================================ Subclasses

    @Test
    fun `every subclass in the game holds together`() {
        val complaints = SubclassData.ALL.flatMap { subclass ->
            levelsFor(subclass.id).flatMap { level ->
                val who = "${subclass.name} (${subclass.classId} $level)"
                val pc = SheetAudit.character(subclass.classId, level, subclass.id)
                SheetAudit.complaints(who, pc)
            }
        }
        report("subclass", complaints)
    }

    /**
     * And again with every question answered, which is where most of it goes wrong: a choice
     * only grants its spell, raises its own follow-up or spends its pool once it has an answer.
     */
    @Test
    fun `every subclass holds together once its questions are answered`() {
        val complaints = SubclassData.ALL.flatMap { subclass ->
            val level = levelsFor(subclass.id).max()
            val who = "${subclass.name} (${subclass.classId} $level, answered)"
            val pc = SheetAudit.fullyAnswered(
                SheetAudit.character(subclass.classId, level, subclass.id)
            )
            SheetAudit.complaints(who, pc)
        }
        report("subclass, answered", complaints)
    }

    // ================================================================ Classes

    @Test
    fun `every class holds together at every level it can reach`() {
        val complaints = ClassData.ALL.flatMap { charClass ->
            listOf(1, 5, 11, 20).flatMap { level ->
                val subclass = SubclassData.forClass(charClass.id).firstOrNull()
                    ?.takeIf { level >= 3 }
                val who = "${charClass.name} $level"
                SheetAudit.complaints(who, SheetAudit.character(charClass.id, level, subclass?.id))
            }
        }
        report("class and level", complaints)
    }

    // ================================================================ Species and lineages

    @Test
    fun `every species and lineage holds together`() {
        val complaints = SpeciesData.ALL.flatMap { species ->
            val lineages: List<String?> = listOf(null) + species.lineageOptions.map { it.id }
            lineages.flatMap { lineage ->
                // On a caster and on a martial, because a species trait that grants a spell or
                // a cantrip meets a different half of the sheet in each.
                listOf("wizard" to 5, "fighter" to 5).flatMap { (classId, level) ->
                    val who = "${species.name}${lineage?.let { " / $it" }.orEmpty()} as $classId"
                    SheetAudit.complaints(
                        who,
                        SheetAudit.character(classId, level, speciesId = species.id, lineageId = lineage),
                    )
                }
            }
        }
        report("species and lineage", complaints)
    }

    // ================================================================ Backgrounds

    @Test
    fun `every background holds together`() {
        val complaints = BackgroundData.ALL.flatMap { background ->
            val who = "${background.name} background"
            val pc = SheetAudit.character("fighter", 4).copy(backgroundId = background.id)
            SheetAudit.complaints(who, pc) + SheetAudit.complaints(
                "$who, answered",
                SheetAudit.fullyAnswered(pc),
            )
        }
        report("background", complaints)
    }

    // ================================================================ Feats

    @Test
    fun `every feat holds together on a character that has it`() {
        val complaints = FeatData.ALL.flatMap { feat ->
            // A Fighter reaches most prerequisites; a Wizard covers the ones wanting magic.
            listOf("fighter", "wizard").flatMap { classId ->
                val who = "${feat.name} on a $classId"
                val pc = SheetAudit.character(classId, 12, featIds = listOf(feat.id))
                SheetAudit.complaints(who, SheetAudit.fullyAnswered(pc))
            }
        }
        report("feat", complaints)
    }

    // ================================================================ Multiclass shapes

    /**
     * The shape that breaks anything re-deriving which level a rule counts against. Every pair
     * of classes, because the pair that breaks it is never the pair anybody tried.
     */
    @Test
    fun `every pair of classes holds together`() {
        val ids = ClassData.ALL.map { it.id }
        val complaints = ids.flatMap { first ->
            ids.filter { it != first }.flatMap { second ->
                val who = "$first 5 / $second 4"
                val subclass = SubclassData.forClass(first).firstOrNull()?.id
                val pc = SheetAudit.character(
                    first, 9, subclass,
                    classLevels = listOf(
                        com.pedroeu.ficha.domain.ClassLevel(first, 5, subclass, isStarting = true),
                        com.pedroeu.ficha.domain.ClassLevel(second, 4, null),
                    ),
                )
                SheetAudit.complaints(who, pc)
            }
        }
        report("pair of classes", complaints)
    }
}
