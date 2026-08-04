package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.CasterType
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The 2024 spellcasting tables, written down where they can be checked.
 *
 * Every one of these numbers exists twice in the app — once in [ProgressionData] and once in
 * a book — and the only thing keeping them equal is somebody remembering. These tests are
 * that somebody. The Artificer's figures are transcribed from the Artificer Features table
 * in *Eberron: Forge of the Artificer*; the rest are the class tables in the 2024 Player's
 * Handbook.
 */
class SpellcastingRulesTest {

    private fun character(classId: String, level: Int = 5, spells: List<KnownSpell> = emptyList()) =
        PlayerCharacter(
            id = "t",
            name = "Test",
            speciesId = "human",
            classId = classId,
            backgroundId = "soldier",
            level = level,
            baseAbilityScores = Ability.ALL.associate { it.name to 14 },
            knownSpells = spells,
        )

    // ------------------------------------------------------------ Prepared-spell tables

    /** Class id to its prepared/known count at each level 1..20. */
    private val preparedByLevel = mapOf(
        "bard" to listOf(4, 5, 6, 7, 9, 10, 11, 12, 14, 15, 16, 16, 17, 17, 18, 18, 19, 20, 21, 22),
        "cleric" to listOf(4, 5, 6, 7, 9, 10, 11, 12, 14, 15, 16, 16, 17, 17, 18, 18, 19, 20, 21, 22),
        "druid" to listOf(4, 5, 6, 7, 9, 10, 11, 12, 14, 15, 16, 16, 17, 17, 18, 18, 19, 20, 21, 22),
        "wizard" to listOf(4, 5, 6, 7, 9, 10, 11, 12, 14, 15, 16, 16, 17, 17, 18, 18, 19, 20, 21, 22),
        // The Sorcerer starts a step behind and catches up at level 5.
        "sorcerer" to listOf(2, 4, 6, 7, 9, 10, 11, 12, 14, 15, 16, 16, 17, 17, 18, 18, 19, 20, 21, 22),
        "warlock" to listOf(2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15),
        "paladin" to listOf(2, 3, 4, 5, 6, 6, 7, 7, 9, 9, 10, 10, 11, 11, 12, 12, 14, 14, 15, 15),
        "ranger" to listOf(2, 3, 4, 5, 6, 6, 7, 7, 9, 9, 10, 10, 11, 11, 12, 12, 14, 14, 15, 15),
        // Forge of the Artificer, Artificer Features table, Prepared Spells column.
        "artificer" to listOf(2, 3, 4, 5, 6, 6, 7, 7, 9, 9, 10, 10, 11, 11, 12, 12, 14, 14, 15, 15),
    )

    /** Class id to its cantrip count at each level 1..20. */
    private val cantripsByLevel = mapOf(
        "bard" to listOf(2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4),
        "cleric" to listOf(3, 3, 3, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5),
        "druid" to listOf(2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4),
        "sorcerer" to listOf(4, 4, 4, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6),
        "warlock" to listOf(2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4),
        "wizard" to listOf(3, 3, 3, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5),
        // Forge of the Artificer, Cantrips column: 2 from level 1, 3 at 10, 4 at 14.
        "artificer" to listOf(2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4),
        // Half casters with no cantrips at all.
        "paladin" to List(20) { 0 },
        "ranger" to List(20) { 0 },
    )

    @Test
    fun `prepared spell counts match the class tables`() {
        preparedByLevel.forEach { (classId, expected) ->
            val progression = ProgressionData.forClass(classId)
                ?: throw AssertionError("$classId has no progression")
            val actual = (1..20).map { progression.preparedSpellsAt(it) }
            assertEquals("$classId prepared spells by level", expected, actual)
        }
    }

    @Test
    fun `cantrip counts match the class tables`() {
        cantripsByLevel.forEach { (classId, expected) ->
            val progression = ProgressionData.forClass(classId)
                ?: throw AssertionError("$classId has no progression")
            val actual = (1..20).map { progression.cantripsKnownAt(it) }
            assertEquals("$classId cantrips by level", expected, actual)
        }
    }

    @Test
    fun `every class that casts has a table for it`() {
        ClassData.ALL.filter { it.isSpellcaster }.forEach { charClass ->
            assertTrue(
                "${charClass.name} casts spells but has no prepared-spell table",
                preparedByLevel.containsKey(charClass.id),
            )
            val progression = ProgressionData.forClass(charClass.id)
            assertTrue(
                "${charClass.name} casts spells but its progression says otherwise",
                progression?.casterType != null && progression.casterType != CasterType.NONE,
            )
        }
    }

    // ------------------------------------------------------------ Preparing vs knowing

    @Test
    fun `the prepared casters rebuild their list on a long rest`() {
        listOf("artificer", "cleric", "druid", "paladin", "ranger", "wizard").forEach { id ->
            assertTrue("$id prepares daily", CharacterSpells.preparesDaily(character(id)))
        }
    }

    @Test
    fun `the fixed-list casters trade a spell on levelling instead`() {
        listOf("bard", "sorcerer", "warlock").forEach { id ->
            assertFalse("$id doesn't re-prepare daily", CharacterSpells.preparesDaily(character(id)))
            assertTrue("$id trades one spell per level", CharacterSpells.swapsSpellOnLevelUp(id))
        }
        listOf("artificer", "cleric", "druid", "paladin", "ranger", "wizard").forEach { id ->
            assertFalse(
                "$id has no need of a level-up trade, it re-prepares every day",
                CharacterSpells.swapsSpellOnLevelUp(id),
            )
        }
    }

    @Test
    fun `cantrips are traded on levelling, except the artificer's`() {
        listOf("bard", "cleric", "druid", "sorcerer", "warlock", "wizard").forEach { id ->
            assertTrue("$id trades a cantrip on levelling", CharacterSpells.swapsCantripOnLevelUp(id))
        }
        // "Whenever you finish a Long Rest, you can replace one of your cantrips from this
        // feature with another Artificer cantrip of your choice."
        assertFalse(CharacterSpells.swapsCantripOnLevelUp("artificer"))
        assertEquals(
            listOf("artificer"),
            CharacterSpells.classesSwappingCantripsOnLongRest(character("artificer")),
        )
        assertTrue(
            "a Cleric's cantrips don't change overnight",
            CharacterSpells.classesSwappingCantripsOnLongRest(character("cleric")).isEmpty(),
        )
        // A class with no cantrips has nothing to trade either way.
        listOf("paladin", "ranger", "fighter").forEach { id ->
            assertFalse("$id has no cantrips", CharacterSpells.swapsCantripOnLevelUp(id))
        }
    }

    // ------------------------------------------------------------ What there is to prepare

    @Test
    fun `a prepared caster may prepare anything on its class list`() {
        // A level 5 Cleric has level 3 slots, so the whole Cleric list up to level 3 is on
        // the table each morning — not merely the handful written on the sheet.
        val cleric = character("cleric", level = 5)
        val offered = CharacterSpells.preparable(cleric).map { it.name }.toSet()
        val expected = SpellData.forClassUpTo("cleric", 3)
            .filter { it.level > 0 }
            .map { it.name }

        assertTrue("the Cleric list should be long", expected.size > 20)
        val missing = expected.filterNot { it in offered }
        assertTrue("these Cleric spells were never offered: $missing", missing.isEmpty())
        assertTrue("cantrips aren't prepared", CharacterSpells.preparable(cleric).none { it.level == 0 })
    }

    @Test
    fun `a prepared caster is not offered spells it has no slots for`() {
        val cleric = character("cleric", level = 1)
        val offered = CharacterSpells.preparable(cleric)
        assertTrue(
            "a level 1 Cleric has only level 1 slots",
            offered.all { it.level == 1 },
        )
    }

    @Test
    fun `a fixed-list caster is only offered what it knows`() {
        val sorcerer = character(
            "sorcerer",
            level = 5,
            spells = listOf(KnownSpell("magic_missile", "Magic Missile", 1, "Evocation", "")),
        )
        val offered = CharacterSpells.preparable(sorcerer).map { it.name }
        assertEquals(listOf("Magic Missile"), offered)
    }

    @Test
    fun `a wizard prepares from its spellbook, not the whole wizard list`() {
        val wizard = character(
            "wizard",
            level = 5,
            spells = listOf(KnownSpell("shield", "Shield", 1, "Abjuration", "")),
        )
        val offered = CharacterSpells.preparable(wizard).map { it.name }
        assertEquals(
            "the spellbook is the list; copying into it is a separate act",
            listOf("Shield"),
            offered,
        )
    }

    // ------------------------------------------------------------ Choosing at creation

    @Test
    fun `every caster picks its first spells from the catalog`() {
        // A curated handful written into ClassData is how the Artificer ended up unable to
        // choose Mage Hand. Every option offered at creation has to be a catalog entry on
        // that class's list, which is only true if the list came from the catalog.
        ClassData.ALL.filter { it.isSpellcaster }.forEach { charClass ->
            charClass.choices.filterIsInstance<ClassChoice.CantripChoice>().forEach { choice ->
                assertTrue(
                    "${charClass.name} offers no options for ${choice.id}",
                    choice.options.isNotEmpty(),
                )
                val strangers = choice.options.filter { stub ->
                    val spell = SpellData.byId(stub.id)
                    spell == null || charClass.id !in spell.classes
                }
                assertTrue(
                    "${charClass.name}'s ${choice.id} offers things that aren't on its " +
                        "catalog list: ${strangers.map { it.name }}",
                    strangers.isEmpty(),
                )
            }
        }
    }

    @Test
    fun `the artificer can choose mage hand`() {
        val artificer = ClassData.byId("artificer")!!
        val cantrips = artificer.choices
            .filterIsInstance<ClassChoice.CantripChoice>()
            .first { it.id == "cantrips" }
        assertTrue(cantrips.options.any { it.name == "Mage Hand" })
    }
}
