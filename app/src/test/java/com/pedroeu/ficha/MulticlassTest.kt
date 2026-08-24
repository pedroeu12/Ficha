package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.MulticlassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterDcs
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.ClassLevel
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.Multiclassing
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Multiclassing touches nearly every derived number, and gets most of them wrong if it just
 * adds levels together. These tests pin the parts that are easy to get backwards: which level
 * a feature reads, which total the slots read, and the Artificer's rounding.
 */
class MulticlassTest {

    private fun character(
        classId: String,
        level: Int,
        subclassId: String? = null,
        classLevels: List<ClassLevel> = emptyList(),
        scores: Map<String, Int> = emptyMap(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = classId,
        subclassId = subclassId,
        backgroundId = "soldier",
        level = level,
        classLevels = classLevels,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 } + scores,
    )

    private fun multiclass(vararg entries: Pair<String, Int>, scores: Map<String, Int> = emptyMap()) =
        character(
            classId = entries.first().first,
            level = entries.sumOf { it.second },
            classLevels = entries.mapIndexed { index, (id, lvl) ->
                ClassLevel(id, lvl, isStarting = index == 0)
            },
            scores = scores,
        )

    // ------------------------------------------------------------- The model

    @Test
    fun `a single class character needs no stored class list`() {
        val fighter = character("fighter", level = 5)
        val classes = ClassLevels.of(fighter)

        assertEquals(1, classes.size)
        assertEquals("fighter", classes.first().classId)
        assertEquals(5, classes.first().level)
        assertFalse(ClassLevels.isMulticlassed(fighter))
    }

    @Test
    fun `levels are tracked per class and total correctly`() {
        val pc = multiclass("fighter" to 5, "wizard" to 3)

        assertEquals(5, ClassLevels.levelIn(pc, "fighter"))
        assertEquals(3, ClassLevels.levelIn(pc, "wizard"))
        assertEquals(0, ClassLevels.levelIn(pc, "rogue"))
        assertEquals(8, ClassLevels.totalLevel(pc))
        assertTrue(ClassLevels.isMulticlassed(pc))
        assertEquals("Fighter 5 / Wizard 3", ClassLevels.label(pc))
    }

    @Test
    fun `proficiency bonus uses total level, not the level in one class`() {
        val pc = multiclass("fighter" to 5, "wizard" to 3)
        // Total level 8 gives +3, where a level 5 Fighter alone would give +3 and a level 3
        // Wizard +2; the total is what counts.
        assertEquals(3, CharacterCalculations.proficiencyBonus(pc))

        val higher = multiclass("fighter" to 5, "wizard" to 4)
        assertEquals(4, CharacterCalculations.proficiencyBonus(higher))
    }

    // ------------------------------------------------------------- Hit dice

    @Test
    fun `hit dice are tracked separately per class`() {
        val pc = multiclass("fighter" to 5, "wizard" to 3)
        val dice = ClassLevels.hitDice(pc)

        assertEquals(3, dice[6])
        assertEquals(5, dice[10])
        assertEquals("5d10 + 3d6", ClassLevels.hitDiceLabel(pc))
    }

    @Test
    fun `hit points roll each class's own die`() {
        // Constitution 14 gives +2 per level.
        val pc = multiclass("fighter" to 2, "wizard" to 2)
        // Fighter d10 maximised at level 1, then average d10 (6), then two average d6 (4).
        val expected = (10 + 2) + (6 + 2) + (4 + 2) + (4 + 2)
        assertEquals(expected, CharacterCalculations.maxHitPoints(pc))
    }

    // ------------------------------------------------------------- Spell slots

    @Test
    fun `two full casters add their levels together for slots`() {
        val pc = multiclass("cleric" to 3, "wizard" to 2)
        assertEquals(5, ClassLevels.casterLevel(pc))

        val slots = CharacterCalculations.spellSlots(pc)
        assertEquals("a level 5 caster has four level 1 slots", 4, slots[1])
        assertEquals(3, slots[2])
        assertEquals(2, slots[3])
    }

    @Test
    fun `half casters contribute half their levels, rounded down`() {
        assertEquals(2, ClassLevels.casterLevel(multiclass("paladin" to 5)))
        assertEquals(3, ClassLevels.casterLevel(multiclass("ranger" to 7)))
        assertEquals(0, ClassLevels.casterLevel(multiclass("paladin" to 1)))
    }

    @Test
    fun `the artificer rounds up, unlike every other half caster`() {
        // One Artificer level already contributes a caster level; one Paladin level does not.
        assertEquals(1, ClassLevels.casterLevel(multiclass("artificer" to 1)))
        assertEquals(0, ClassLevels.casterLevel(multiclass("paladin" to 1)))

        assertEquals(2, ClassLevels.casterLevel(multiclass("artificer" to 3)))
        assertEquals(1, ClassLevels.casterLevel(multiclass("paladin" to 3)))

        assertEquals(3, ClassLevels.casterLevel(multiclass("artificer" to 5)))
        assertEquals(2, ClassLevels.casterLevel(multiclass("paladin" to 5)))
    }

    @Test
    fun `a third caster subclass contributes a third of its levels`() {
        val pc = character(
            classId = "fighter",
            level = 9,
            classLevels = listOf(
                ClassLevel("fighter", 6, subclassId = "eldritch_knight", isStarting = true),
                ClassLevel("wizard", 3),
            ),
        )
        // Six Fighter levels as an Eldritch Knight give two, plus three full Wizard levels.
        assertEquals(5, ClassLevels.casterLevel(pc))
    }

    @Test
    fun `pact magic stays separate from the shared slots`() {
        val pc = multiclass("warlock" to 3, "wizard" to 3)

        assertEquals("the Warlock contributes nothing to the shared table", 3, ClassLevels.casterLevel(pc))
        assertEquals(3, ClassLevels.pactLevel(pc))

        val slots = CharacterCalculations.spellSlots(pc)
        // A level 3 caster has 4/2 shared slots, and Pact Magic adds two level 2 slots.
        assertEquals(4, slots[1])
        assertEquals("2 shared plus 2 pact", 4, slots[2])
    }

    @Test
    fun `a single class caster still reads its own table`() {
        val warlock = character("warlock", level = 5)
        val slots = CharacterCalculations.spellSlots(warlock)

        assertEquals("Pact Magic gives two level 3 slots and nothing else", mapOf(3 to 2), slots)
    }

    @Test
    fun `a non caster multiclass has no slots`() {
        val pc = multiclass("fighter" to 3, "barbarian" to 2)
        assertEquals(0, ClassLevels.casterLevel(pc))
        assertTrue(CharacterCalculations.spellSlots(pc).isEmpty())
    }

    // ------------------------------------------------------------- Prerequisites

    @Test
    fun `a class you lack the score for is blocked, with the reason`() {
        val fighter = character("fighter", 5, scores = mapOf("STR" to 16, "INT" to 10))
        val wizard = Multiclassing.options(fighter).first { it.classId == "wizard" }

        assertFalse(wizard.allowed)
        assertTrue("the reason should name the score", wizard.reason.contains("Intelligence 13"))
    }

    @Test
    fun `leaving a class needs its score too`() {
        // Intelligence 13 qualifies for Wizard, but Strength 10 fails the Fighter requirement
        // that must be met to leave it.
        val fighter = character("fighter", 5, scores = mapOf("STR" to 10, "DEX" to 10, "INT" to 16))
        val wizard = Multiclassing.options(fighter).first { it.classId == "wizard" }

        assertFalse("you can't leave a class you no longer qualify for", wizard.allowed)
        assertTrue(wizard.reason.contains("current class"))
    }

    @Test
    fun `the fighter accepts either strength or dexterity`() {
        val strong = character("barbarian", 3, scores = mapOf("STR" to 16, "DEX" to 8))
        val nimble = character("barbarian", 3, scores = mapOf("STR" to 16, "DEX" to 16))

        assertTrue(Multiclassing.options(strong).first { it.classId == "fighter" }.allowed)
        assertTrue(Multiclassing.options(nimble).first { it.classId == "fighter" }.allowed)

        val weak = character("barbarian", 3, scores = mapOf("STR" to 16, "DEX" to 8))
        val fighterOption = Multiclassing.options(weak).first { it.classId == "fighter" }
        assertTrue("Strength alone is enough", fighterOption.allowed)
    }

    @Test
    fun `a class needing two scores blocks when either is short`() {
        val one = character("fighter", 3, scores = mapOf("STR" to 16, "DEX" to 16, "WIS" to 8))
        assertFalse(Multiclassing.options(one).first { it.classId == "monk" }.allowed)

        val both = character("fighter", 3, scores = mapOf("STR" to 16, "DEX" to 16, "WIS" to 14))
        assertTrue(Multiclassing.options(both).first { it.classId == "monk" }.allowed)
    }

    @Test
    fun `every class has a multiclass entry and its prerequisites are real abilities`() {
        val classIds = ClassData.ALL.map { it.id }.toSet()
        assertEquals(
            "every class should be multiclassable",
            classIds,
            MulticlassData.sourceIds(),
        )
        MulticlassData.all().forEach { entry ->
            assertTrue(
                "${entry.classId} should state a prerequisite",
                entry.prerequisites.isNotEmpty() || entry.orPrerequisites.isNotEmpty(),
            )
        }
    }

    // ------------------------------------------------------------- Taking a level

    @Test
    fun `taking a level in a new class creates its entry`() {
        val fighter = character("fighter", 5)
        val updated = Multiclassing.withLevelIn(fighter, "wizard")

        assertEquals(6, updated.level)
        assertEquals(5, ClassLevels.levelIn(updated, "fighter"))
        assertEquals(1, ClassLevels.levelIn(updated, "wizard"))
    }

    @Test
    fun `taking a level in a class you have raises that class`() {
        val pc = multiclass("fighter" to 5, "wizard" to 1)
        val updated = Multiclassing.withLevelIn(pc, "wizard")

        assertEquals(7, updated.level)
        assertEquals(2, ClassLevels.levelIn(updated, "wizard"))
        assertEquals(5, ClassLevels.levelIn(updated, "fighter"))
    }

    @Test
    fun `multiclass proficiencies are narrower than a starting class's`() {
        val wizard = MulticlassData.forClass("wizard")!!
        assertTrue("a Wizard grants nothing on multiclassing", wizard.armorTraining.isEmpty())
        assertTrue(wizard.weaponProficiencies.isEmpty())

        val fighter = MulticlassData.forClass("fighter")!!
        assertTrue(fighter.armorTraining.contains("Light"))
        assertTrue(fighter.weaponProficiencies.contains("Martial"))

        val rogue = MulticlassData.forClass("rogue")!!
        assertTrue("a multiclass Rogue gets Light armor only", rogue.armorTraining == listOf("Light"))
        assertTrue(rogue.toolProficiencies.contains("Thieves' Tools"))
    }

    // ------------------------------------------------------------- Derived systems

    @Test
    fun `each class's features advance on its own level`() {
        // A Fighter 5 / Cleric 3 has a level 5 Fighter's Second Wind and a level 3 Cleric's
        // Channel Divinity, not a level 8 version of either.
        val pc = multiclass("fighter" to 5, "cleric" to 3)
        val pools = CharacterResources.definitions(pc)

        val secondWind = pools.first { it.id == "fighter:second_wind" }
        assertEquals("a level 5 Fighter has three uses", 3, secondWind.max)

        val channelDivinity = pools.first { it.id == "cleric:channel_divinity" }
        assertEquals("a level 3 Cleric has two uses", 2, channelDivinity.max)
    }

    @Test
    fun `a low level in a class does not unlock its higher features`() {
        val pc = multiclass("fighter" to 1, "wizard" to 10)
        val pools = CharacterResources.definitions(pc)

        assertTrue(
            "Action Surge needs Fighter 2, and this character has Fighter 1",
            pools.none { it.id == "fighter:action_surge" },
        )
    }

    @Test
    fun `each spellcasting class brings its own save DC`() {
        val pc = multiclass(
            "cleric" to 3, "wizard" to 2,
            scores = mapOf("WIS" to 16, "INT" to 10),
        )
        val dcs = CharacterDcs.all(pc)

        val cleric = dcs.first { it.label == "Cleric" }
        val wizard = dcs.first { it.label == "Wizard" }

        assertEquals(Ability.WIS, cleric.ability)
        assertEquals(Ability.INT, wizard.ability)
        assertTrue("two different abilities give two different DCs", cleric.dc != wizard.dc)
    }

    @Test
    fun `the starting class keeps the primary DC`() {
        val pc = multiclass("cleric" to 3, "wizard" to 2)
        val primary = CharacterDcs.primary(pc)

        assertNotNull(primary)
        assertEquals("Cleric", primary!!.label)
    }
}
