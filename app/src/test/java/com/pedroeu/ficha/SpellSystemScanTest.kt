package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.model.SpellSlotTables
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SpellGrantData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.CasterType
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.levelup.LevelUpState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A sweep of the whole spell system, rather than one more fix for one more missing spell.
 *
 * The reports that led here were always the same shape — a spell that isn't offered, a prompt
 * that never appears — and always found by a player rather than by a build. Each test below
 * is the general form of one of those: *every* caster is asked at *every* level, *every*
 * option offered is real, and *every* spell a class should have is reachable from the picker
 * and not merely present in a data file.
 */
class SpellSystemScanTest {

    private fun character(
        classId: String,
        level: Int,
        subclassId: String? = null,
        known: List<KnownSpell> = emptyList(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = classId,
        subclassId = subclassId,
        backgroundId = "sage",
        level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 16 },
        knownSpells = known,
    )

    /** A character holding exactly what its tables say it should at [level]. */
    private fun fullyStocked(classId: String, level: Int, subclassId: String? = null): PlayerCharacter {
        val name = ClassData.byId(classId)?.name ?: classId
        val subclass = subclassId?.let { SubclassData.byId(it) }
        val cantrips = (ProgressionData.forClass(classId)?.cantripsKnownAt(level) ?: 0) +
            (subclass?.cantripsKnownAt(level) ?: 0)
        val spells = (ProgressionData.forClass(classId)?.preparedSpellsAt(level) ?: 0) +
            (subclass?.preparedSpellsAt(level) ?: 0)
        return character(
            classId, level, subclassId,
            (0 until cantrips).map { KnownSpell("c$it", "C$it", 0, "", "", true, name) } +
                (0 until spells).map { KnownSpell("s$it", "S$it", 1, "", "", true, name) },
        )
    }

    // ------------------------------------------------------- Every caster is asked something

    /** Class id to the subclass that grants it spellcasting, where the class has none. */
    private val subclassCasters = mapOf(
        "fighter" to "eldritch_knight",
        "rogue" to "arcane_trickster",
    )

    @Test
    fun `every spellcasting class is asked for spells at the levels its table grows`() {
        val casters = ClassData.ALL.filter { it.isSpellcaster }.map { it.id }
        casters.forEach { classId ->
            val progression = ProgressionData.forClass(classId)!!
            (1..19).forEach { level ->
                val state = LevelUpState(
                    character = fullyStocked(classId, level),
                    levellingClassId = classId,
                )
                val cantripsGrew = progression.cantripsKnownAt(level + 1) >
                    progression.cantripsKnownAt(level)
                val spellsGrew = progression.preparedSpellsAt(level + 1) >
                    progression.preparedSpellsAt(level)

                if (cantripsGrew) {
                    assertTrue(
                        "a $classId gains a cantrip at level ${level + 1} and isn't asked for it",
                        state.cantripsToLearn > 0,
                    )
                }
                // The Wizard is the exception: its spellbook grows by two every level
                // regardless of what the prepared column does.
                if (spellsGrew || classId == "wizard") {
                    assertTrue(
                        "a $classId gains a spell at level ${level + 1} and isn't asked for it",
                        state.spellsToLearn > 0,
                    )
                }
                if (state.cantripsToLearn > 0 || state.spellsToLearn > 0) {
                    assertTrue(
                        "a $classId levelling to ${level + 1} has spells to pick but no step " +
                            "to pick them in",
                        com.pedroeu.ficha.ui.levelup.LevelUpStep.SPELLS in state.steps,
                    )
                }
            }
        }
    }

    @Test
    fun `a subclass that grants spellcasting is asked too`() {
        // The Eldritch Knight and the Arcane Trickster read their subclass's table, because
        // their class's says NONE — which is exactly why they were asked nothing at all.
        subclassCasters.forEach { (classId, subclassId) ->
            val subclass = SubclassData.byId(subclassId)!!
            (3..19).forEach { level ->
                val state = LevelUpState(
                    character = fullyStocked(classId, level, subclassId),
                    subclassId = subclassId,
                    levellingClassId = classId,
                )
                if (subclass.preparedSpellsAt(level + 1) > subclass.preparedSpellsAt(level)) {
                    assertTrue(
                        "$subclassId gains a spell at level ${level + 1} and isn't asked",
                        state.spellsToLearn > 0,
                    )
                }
                if (subclass.cantripsKnownAt(level + 1) > subclass.cantripsKnownAt(level)) {
                    assertTrue(
                        "$subclassId gains a cantrip at level ${level + 1} and isn't asked",
                        state.cantripsToLearn > 0,
                    )
                }
            }
        }
    }

    @Test
    fun `a subclass caster has slots, a prepared count and a save DC`() {
        subclassCasters.forEach { (classId, subclassId) ->
            val third = character(classId, 3, subclassId)
            assertTrue(
                "$subclassId has no spell slots at level 3",
                CharacterCalculations.spellSlots(third).isNotEmpty(),
            )
            assertTrue(
                "$subclassId can prepare nothing at level 3",
                CharacterCalculations.maxPreparedSpells(third) > 0,
            )
            assertTrue(
                "$subclassId knows no cantrips at level 3",
                CharacterCalculations.maxCantripsKnown(third) > 0,
            )
            assertTrue(
                "$subclassId has no spell save DC, so nothing it casts can be resisted",
                CharacterCalculations.spellSaveDc(third) != null,
            )
            // And a level 2 Fighter, before the subclass exists, still has none of it.
            assertTrue(CharacterCalculations.spellSlots(character(classId, 2)).isEmpty())
        }
    }

    @Test
    fun `the third caster tables match the 2024 rules`() {
        subclassCasters.values.forEach { subclassId ->
            val subclass = SubclassData.byId(subclassId)!!
            assertEquals(CasterType.THIRD, subclass.casterType)
            // Prepared Spells column: nothing until 3, then 3, 4, 4, 4, 5, 6, 6, 7, 8, 8, 9…
            assertEquals(0, subclass.preparedSpellsAt(2))
            assertEquals(3, subclass.preparedSpellsAt(3))
            assertEquals(4, subclass.preparedSpellsAt(4))
            assertEquals(5, subclass.preparedSpellsAt(7))
            assertEquals(9, subclass.preparedSpellsAt(13))
            assertEquals(13, subclass.preparedSpellsAt(20))

            val third = character(subclass.classId, 3, subclassId)
            assertEquals(mapOf(1 to 2), CharacterCalculations.spellSlots(third))
            assertEquals(
                mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 1),
                CharacterCalculations.spellSlots(character(subclass.classId, 19, subclassId)),
            )
        }
    }

    @Test
    fun `a third caster is held to its schools, except on the levels the rules exempt`() {
        val ek = LevelUpState(
            character = fullyStocked("fighter", 4, "eldritch_knight"),
            subclassId = "eldritch_knight",
            levellingClassId = "fighter",
        )
        assertEquals(setOf("Abjuration", "Evocation"), ek.allowedSchools)
        assertTrue(
            "the picker offered a school an Eldritch Knight can't learn",
            ek.spellOptions.all { it.school in setOf("Abjuration", "Evocation") },
        )

        // Level 8 is one of the three the rules open up.
        val atEight = LevelUpState(
            character = fullyStocked("fighter", 7, "eldritch_knight"),
            subclassId = "eldritch_knight",
            levellingClassId = "fighter",
        )
        assertTrue("level 8 is exempt from the school restriction", atEight.allowedSchools.isEmpty())

        val at = LevelUpState(
            character = fullyStocked("rogue", 4, "arcane_trickster"),
            subclassId = "arcane_trickster",
            levellingClassId = "rogue",
        )
        assertEquals(setOf("Illusion", "Enchantment"), at.allowedSchools)
    }

    // ------------------------------------------------------- The Wizard's spellbook

    @Test
    fun `a wizard adds two spells to its spellbook at every level`() {
        (1..19).forEach { level ->
            val state = LevelUpState(
                character = fullyStocked("wizard", level),
                levellingClassId = "wizard",
            )
            assertEquals(
                "a Wizard copies two spells into its spellbook on reaching level ${level + 1}",
                2,
                state.spellsToLearn,
            )
            assertTrue(state.learnsIntoSpellbook)
        }
    }

    @Test
    fun `taking a first level of wizard by multiclassing copies the starting six`() {
        val fighter = character("fighter", 4)
        val state = LevelUpState(character = fighter, levellingClassId = "wizard")
        assertEquals(6, state.spellsToLearn)
    }

    @Test
    fun `no other class learns into a spellbook`() {
        ClassData.ALL.filter { it.isSpellcaster && it.id != "wizard" }.forEach { charClass ->
            val state = LevelUpState(
                character = fullyStocked(charClass.id, 5),
                levellingClassId = charClass.id,
            )
            assertFalse("${charClass.name} has no spellbook", state.learnsIntoSpellbook)
        }
    }

    // ------------------------------------------------------- Options are real and reachable

    @Test
    fun `every option a level-up picker offers is a real spell on that class's list`() {
        val casters = ClassData.ALL.filter { it.isSpellcaster }.map { it.id to null } +
            subclassCasters.toList()
        casters.forEach { (classId, subclassId) ->
            (2..20).forEach { level ->
                val state = LevelUpState(
                    character = fullyStocked(classId, level - 1, subclassId),
                    subclassId = subclassId,
                    levellingClassId = classId,
                )
                (state.cantripOptions + state.spellOptions).forEach { spell ->
                    assertTrue(
                        "$classId is offered ${spell.name}, which isn't in the catalog",
                        SpellData.byId(spell.id) != null,
                    )
                }
            }
        }
    }

    @Test
    fun `a picker is never empty when the level asks for a pick`() {
        // The failure this catches is the quietest of the lot: a step that appears, demands a
        // choice, and offers nothing to choose, so the flow can't be completed at all.
        val casters = ClassData.ALL.filter { it.isSpellcaster }.map { it.id to null } +
            subclassCasters.toList()
        casters.forEach { (classId, subclassId) ->
            (1..19).forEach { level ->
                val state = LevelUpState(
                    character = fullyStocked(classId, level, subclassId),
                    subclassId = subclassId,
                    levellingClassId = classId,
                )
                if (state.cantripsToLearn > 0) {
                    assertTrue(
                        "$classId must pick ${state.cantripsToLearn} cantrips at level " +
                            "${level + 1} with nothing on offer",
                        state.cantripOptions.isNotEmpty(),
                    )
                }
                if (state.spellsToLearn > 0 && !state.needsManualSpellEntry) {
                    assertTrue(
                        "$classId must pick ${state.spellsToLearn} spells at level " +
                            "${level + 1} with nothing on offer",
                        state.spellOptions.isNotEmpty(),
                    )
                }
            }
        }
    }

    @Test
    fun `every caster's creation picker draws on its catalog list`() {
        ClassData.ALL.filter { it.isSpellcaster }.forEach { charClass ->
            charClass.choices.filterIsInstance<ClassChoice.CantripChoice>().forEach { choice ->
                assertTrue(
                    "${charClass.name}'s ${choice.id} offers fewer options than it demands picks",
                    choice.options.size >= choice.count,
                )
                choice.options.forEach { stub ->
                    val spell = SpellData.byId(stub.id)
                    assertTrue(
                        "${charClass.name} offers ${stub.name}, which is not a catalog entry",
                        spell != null,
                    )
                    assertTrue(
                        "${charClass.name} offers ${stub.name}, which isn't on its list",
                        charClass.id in spell!!.classes,
                    )
                }
            }
        }
    }

    @Test
    fun `mage hand is reachable everywhere the 2024 lists put it`() {
        // The specific report, kept as a test so it can't come back. Mage Hand is on five
        // class lists, and being in the catalog is not the same as being offered.
        val mageHand = SpellData.byId("mage_hand")!!
        listOf("bard", "sorcerer", "warlock", "wizard", "artificer").forEach { classId ->
            assertTrue("$classId's list should include Mage Hand", classId in mageHand.classes)

            val fromCreation = ClassData.byId(classId)!!.choices
                .filterIsInstance<ClassChoice.CantripChoice>()
                .flatMap { it.options }
            assertTrue(
                "a $classId can't pick Mage Hand at character creation",
                fromCreation.any { it.id == "mage_hand" },
            )

            val fromLevelUp = LevelUpState(
                character = character(classId, 3),
                levellingClassId = classId,
            ).cantripOptions
            assertTrue(
                "a $classId can't pick Mage Hand on level up",
                fromLevelUp.any { it.id == "mage_hand" },
            )
        }

        // And the Arcane Trickster is handed it outright rather than picking it.
        assertTrue(
            CharacterSpells.granted(character("rogue", 3, "arcane_trickster"))
                .any { it.spell.id == "mage_hand" }
        )
    }

    // ------------------------------------------------------- The data itself

    @Test
    fun `no two spells share an id`() {
        val ids = SpellData.ALL.map { it.id }
        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertTrue("these spell ids appear twice: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `no two spells share a name`() {
        // Duplicate names are worse than duplicate ids, because the duplicate-detection that
        // stops a spell being picked twice falls back on matching names.
        val names = SpellData.ALL.map { it.name }
        val duplicates = names.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertTrue("these spell names appear twice: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `every spell grant names a spell the catalog has`() {
        val missing = (SpellGrantData.allGrantedSpellIds() + SpellGrantData.choiceGrantSpellIds())
            .filter { SpellData.byId(it) == null }
        assertTrue("these grants resolve to nothing: $missing", missing.isEmpty())
    }

    @Test
    fun `no source declares its spell grants twice`() {
        // A duplicate key in a mapOf is silently the last one, and that is how the Arcane
        // Trickster's Mage Hand was declared, overwritten by an empty list, and lost.
        val duplicates = SpellGrantData.duplicateSourceIds()
        assertTrue(
            "these sources are declared more than once, so only the last one counts: $duplicates",
            duplicates.isEmpty(),
        )
    }

    @Test
    fun `every class that casts has a cantrip list unless the rules give it none`() {
        // Paladins and Rangers have no cantrips in the 2024 rules; everyone else does, and an
        // empty list would mean a picker with nothing in it.
        val noCantrips = setOf("paladin", "ranger")
        ClassData.ALL.filter { it.isSpellcaster }.forEach { charClass ->
            val cantrips = SpellData.cantripsForClass(charClass.id)
            if (charClass.id in noCantrips) {
                assertTrue("${charClass.name} has no cantrips in 2024", cantrips.isEmpty())
            } else {
                assertTrue("${charClass.name} has no cantrips at all", cantrips.size >= 9)
            }
        }
    }

    @Test
    fun `every class list reaches the top slot that class can actually cast`() {
        // A class whose list stops early leaves the player with an empty picker the moment
        // they gain a slot of that level. Half casters stop at 5, so holding every class to
        // the catalogue's own ceiling would demand level 9 Paladin spells that do not exist.
        ClassData.ALL.filter { it.isSpellcaster }.forEach { charClass ->
            val top = SpellSlotTables.maxSpellLevel(
                ProgressionData.forClass(charClass.id)!!.casterType, 20,
            )
            (1..top).forEach { level ->
                assertTrue(
                    "${charClass.name} has no level $level spells in the catalog",
                    SpellData.forClass(charClass.id, level).isNotEmpty(),
                )
            }
        }
    }

    @Test
    fun `each class list is exactly the size the 2024 books give it`() {
        // Counted off dnd2024.wikidot.com. A spell added to or dropped from a list without
        // checking the book moves one of these, which is how a list silently drifts.
        val expected = mapOf(
            "artificer" to 81, "bard" to 149, "cleric" to 126, "druid" to 142,
            "paladin" to 53, "ranger" to 65, "sorcerer" to 163, "warlock" to 96,
            "wizard" to 263,
        )
        expected.forEach { (classId, size) ->
            assertEquals(
                "$classId's spell list is the wrong size",
                size,
                SpellData.ALL.count { classId in it.classes },
            )
        }
        assertEquals("the catalog is every spell in the books", 419, SpellData.ALL.size)
    }
}
