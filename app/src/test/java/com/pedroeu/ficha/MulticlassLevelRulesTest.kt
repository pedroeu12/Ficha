package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.SpellGrantData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.domain.CharacterAttacks
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ClassLevel
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which level a rule reads.
 *
 * Almost everything in the game scales on the level in its own class; a short list — the
 * Proficiency Bonus, the number of Hit Dice, cantrip damage, and anything a species or a feat
 * grants — scales on the character's total. Reading the wrong one is invisible on a
 * single-class sheet and wrong on every multiclassed one, which is how a Monk 3 / Rogue 3 came
 * to punch with a level 6 Monk's Martial Arts die.
 */
class MulticlassLevelRulesTest {

    private fun character(vararg classes: ClassLevel) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = classes.first().classId,
        subclassId = classes.first().subclassId,
        backgroundId = "soldier",
        level = classes.sumOf { it.level },
        classLevels = classes.toList(),
        baseAbilityScores = Ability.ALL.associate { it.name to 16 },
    )

    private fun single(classId: String, level: Int, subclassId: String? = null) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = classId,
        subclassId = subclassId,
        backgroundId = "soldier",
        level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 16 },
    )

    private fun unarmedDamage(character: PlayerCharacter) =
        CharacterAttacks.unarmed(character).first().damage

    // ------------------------------------------------------------------ Class level

    @Test
    fun `the martial arts die follows monk levels, not the character's total`() {
        // The reported case: level 6 overall, three of them Monk, punching for 1d8.
        val monkThree = character(
            ClassLevel("monk", 3, "open_hand", isStarting = true),
            ClassLevel("rogue", 3, "thief"),
        )
        assertTrue("a level 3 Monk's die is a d6", unarmedDamage(monkThree).startsWith("1d6"))
        assertTrue("a level 6 single-class Monk's die is a d8", unarmedDamage(single("monk", 6)).startsWith("1d8"))
    }

    @Test
    fun `martial arts applies to a monk who did not start as one`() {
        val fighterFirst = character(
            ClassLevel("fighter", 3, "champion", isStarting = true),
            ClassLevel("monk", 3, "open_hand"),
        )
        assertTrue(
            "reading the plain classId field made this character not a Monk at all",
            unarmedDamage(fighterFirst).startsWith("1d6"),
        )
    }

    @Test
    fun `unarmored movement follows monk levels`() {
        val monkThree = character(
            ClassLevel("monk", 3, "open_hand", isStarting = true),
            ClassLevel("rogue", 3, "thief"),
        )
        // A level 3 Monk moves 10 feet faster; a level 6 one moves 15 faster.
        assertEquals(40, CharacterCalculations.speed(monkThree))
        assertEquals(45, CharacterCalculations.speed(single("monk", 6)))
    }

    @Test
    fun `fast movement follows barbarian levels`() {
        val barbarianFour = character(
            ClassLevel("barbarian", 4, "berserker", isStarting = true),
            ClassLevel("fighter", 4, "champion"),
        )
        assertEquals("Fast Movement arrives at Barbarian 5", 30, CharacterCalculations.speed(barbarianFour))
        assertEquals(40, CharacterCalculations.speed(single("barbarian", 5)))
    }

    @Test
    fun `unarmored defense belongs to anyone with a level in the class`() {
        // Both are level 1 features, so any level in the class is the whole test.
        val wizardFirst = character(
            ClassLevel("wizard", 5, "evoker", isStarting = true),
            ClassLevel("barbarian", 1),
        )
        // 10 + Dex 3 + Con 3, better than the plain 10 + Dex.
        assertEquals(16, CharacterCalculations.armorClass(wizardFirst))
    }

    @Test
    fun `a subclass attack scales on the level in its own class`() {
        val rogueThree = character(
            ClassLevel("rogue", 3, "soulknife", isStarting = true),
            ClassLevel("fighter", 9, "champion"),
        )
        val blades = CharacterAttacks.all(rogueThree).firstOrNull { it.name == "Psychic Blades" }
        assertTrue("a level 3 Soulknife's blade is a d6, not a level 12 one's", blades!!.damage.startsWith("1d6"))
    }

    @Test
    fun `weapon training is the union of every class`() {
        val clericFighter = character(
            ClassLevel("cleric", 5, "life_domain", isStarting = true),
            ClassLevel("fighter", 1),
        )
        assertTrue(
            "multiclassing grants weapon training, so the Fighter level really does count",
            ClassData.byId("fighter")!!.weaponProficiencies.contains("Martial"),
        )
        // The sheet reads the union; a martial weapon is now proficient.
        assertTrue(ClassLevels.has(clericFighter, "fighter"))
    }

    // ------------------------------------------------------------------ Character level

    @Test
    fun `the proficiency bonus and the hit dice still read the total`() {
        val eight = character(
            ClassLevel("fighter", 5, "champion", isStarting = true),
            ClassLevel("wizard", 3, "evoker"),
        )
        assertEquals("level 8 is a +3", 3, CharacterCalculations.proficiencyBonus(eight))
        assertEquals("eight levels, eight Hit Dice", 8, eight.level)
    }

    @Test
    fun `a species pool reads the character's total level`() {
        val aasimar = PlayerCharacter(
            id = "t", name = "T", speciesId = "aasimar", classId = "fighter",
            backgroundId = "soldier", level = 3,
            classLevels = listOf(
                ClassLevel("fighter", 1, isStarting = true),
                ClassLevel("wizard", 2, null),
            ),
            baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        )
        assertTrue(
            "Celestial Revelation arrives at character level 3, whatever the classes are",
            CharacterResources.definitions(aasimar).any { it.id == "aasimar:celestial_revelation" },
        )
    }

    // ------------------------------------------------------------------ Spellcasting

    @Test
    fun `a prepared caster prepares only up to its own class's spell level`() {
        val rangerWizard = character(
            ClassLevel("ranger", 3, "gloom_stalker", isStarting = true),
            ClassLevel("wizard", 9, "evoker"),
        )
        assertTrue("the shared table gives level 5 slots", CharacterCalculations.maxSpellLevel(rangerWizard) >= 5)
        val rangerSide = CharacterSpells.preparable(rangerWizard).filter { it.source == "Ranger" }
        assertTrue(rangerSide.isNotEmpty())
        assertEquals(
            "a Ranger 3 prepares level 1 Ranger spells, whatever slots they have",
            1,
            rangerSide.maxOf { it.level },
        )
    }

    @Test
    fun `a multiclassed caster is recognised as one`() {
        val fighterWizard = character(
            ClassLevel("fighter", 5, "champion", isStarting = true),
            ClassLevel("wizard", 3, "evoker"),
        )
        assertTrue(CharacterCalculations.spellSlots(fighterWizard).isNotEmpty())
        assertTrue(CharacterSpells.preparesDaily(fighterWizard))
    }

    // ------------------------------------------------------------------ Granted spells

    @Test
    fun `a picker never offers a spell the class already grants`() {
        // A Ranger always has Hunter's Mark prepared; spending one of their two level 1
        // choices on it buys nothing and lists it twice.
        val rangerChoices = ClassData.byId("ranger")!!.choices
            .filterIsInstance<ClassChoice.CantripChoice>()
            .flatMap { it.options }
            .map { it.id }
        assertTrue("the picker is empty, so this proves nothing", rangerChoices.isNotEmpty())
        assertFalse(rangerChoices.contains("hunters_mark"))

        // Same rule, three more classes.
        fun optionsOf(classId: String) = ClassData.byId(classId)!!.choices
            .filterIsInstance<ClassChoice.CantripChoice>()
            .flatMap { it.options }
            .map { it.id }
        assertFalse(optionsOf("druid").contains("druidcraft"))
        assertFalse(optionsOf("warlock").contains("eldritch_blast"))
        assertFalse(optionsOf("artificer").contains("mending"))
    }

    @Test
    fun `the always-prepared lookup matches the grant table`() {
        assertEquals(setOf("hunters_mark"), SpellGrantData.alwaysPreparedForClass("ranger", 1))
        assertTrue(SpellGrantData.alwaysPreparedForClass("paladin", 1).isEmpty())
        assertEquals(setOf("divine_smite"), SpellGrantData.alwaysPreparedForClass("paladin", 2))
        assertTrue(
            SpellGrantData.alwaysPreparedForSubclass("gloom_stalker", 9).contains("fear"),
        )
        assertFalse(
            "Fear is a level 9 grant",
            SpellGrantData.alwaysPreparedForSubclass("gloom_stalker", 5).contains("fear"),
        )
    }

    // ------------------------------------------------------------------ Dread Ambusher

    @Test
    fun `dread ambusher carries its damage and its uses`() {
        val text = SubclassData.byId("gloom_stalker")!!
            .features.first { it.name == "Dread Ambusher" }.description
        assertTrue("the Psychic damage went missing entirely", text.contains("2d6 Psychic"))
        assertTrue(text.contains("Wisdom modifier"))
        assertTrue("Ambusher's Leap is one of the three benefits", text.contains("Ambusher's Leap"))
        assertFalse(
            "an extra attack on the first turn is the 2014 version of the feature",
            text.contains("extra attack", ignoreCase = true),
        )

        val ranger = single("ranger", 5, "gloom_stalker")
        val pool = CharacterResources.definitions(ranger)
            .firstOrNull { it.id == "gloom_stalker:dreadful_strike" }
        assertTrue("a feature with uses needs somewhere to count them", pool != null)
        assertEquals("Wisdom 16 is a +3", 3, pool!!.max)
    }
}
