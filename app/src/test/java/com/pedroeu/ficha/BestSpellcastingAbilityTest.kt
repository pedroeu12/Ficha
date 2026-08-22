package com.pedroeu.ficha

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterDcs
import com.pedroeu.ficha.domain.ClassLevel
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A character can cast on more than one ability: one per class taken, one more from a
 * subclass whose class has none, and a fixed one from any feat that grants spellcasting.
 *
 * The sheet shows a single attack bonus and save DC, so it uses the best of them. Showing the
 * worse one because that class happened to be taken first helps nobody.
 */
class BestSpellcastingAbilityTest {

    private fun character(
        classId: String,
        subclassId: String? = null,
        classLevels: List<ClassLevel> = emptyList(),
        featIds: List<String> = emptyList(),
        scores: Map<String, Int> = emptyMap(),
        level: Int = 10,
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = classId,
        subclassId = subclassId,
        classLevels = classLevels,
        backgroundId = "sage",
        level = level,
        featIds = featIds,
        baseAbilityScores = Ability.ALL.associate { it.name to 10 } + scores,
    )

    @Test
    fun `a single class caster is unaffected`() {
        val wizard = character("wizard", scores = mapOf("INT" to 18))
        assertEquals(Ability.INT, CharacterCalculations.spellcastingAbility(wizard))
    }

    @Test
    fun `a multiclass caster uses whichever ability is higher`() {
        val levels = listOf(
            ClassLevel("wizard", 5, isStarting = true),
            ClassLevel("cleric", 5),
        )
        val wisdomBetter = character(
            "wizard", classLevels = levels, scores = mapOf("INT" to 12, "WIS" to 18),
        )
        assertEquals(
            "the Cleric's Wisdom beats the starting Wizard's Intelligence",
            Ability.WIS,
            CharacterCalculations.spellcastingAbility(wisdomBetter),
        )

        val intBetter = character(
            "wizard", classLevels = levels, scores = mapOf("INT" to 18, "WIS" to 12),
        )
        assertEquals(Ability.INT, CharacterCalculations.spellcastingAbility(intBetter))
    }

    @Test
    fun `the save DC and attack bonus follow the chosen ability`() {
        val levels = listOf(
            ClassLevel("wizard", 5, isStarting = true),
            ClassLevel("cleric", 5),
        )
        val character = character(
            "wizard", classLevels = levels, scores = mapOf("INT" to 10, "WIS" to 20),
        )
        val pb = CharacterCalculations.proficiencyBonus(character)
        assertEquals(8 + pb + 5, CharacterCalculations.spellSaveDc(character))
        assertEquals(pb + 5, CharacterCalculations.spellAttackBonus(character))
    }

    @Test
    fun `a subclass ability counts where the class has none`() {
        // An Eldritch Knight casts on Intelligence; the Fighter class itself says nothing.
        val knight = character(
            "fighter", subclassId = "eldritch_knight", scores = mapOf("INT" to 16),
        )
        assertEquals(Ability.INT, CharacterCalculations.spellcastingAbility(knight))
    }

    @Test
    fun `a feat that grants spellcasting is a candidate too`() {
        // Magic Initiate (Wizard) casts on Intelligence whatever the class does.
        val cleric = character(
            "cleric",
            featIds = listOf("magic_initiate_wizard"),
            scores = mapOf("WIS" to 10, "INT" to 20),
        )
        assertTrue(
            "the feat's ability must be among the candidates",
            CharacterCalculations.spellcastingAbilities(cleric).contains(Ability.INT),
        )
        assertEquals(Ability.INT, CharacterCalculations.spellcastingAbility(cleric))
    }

    @Test
    fun `ties keep the earlier source, so nothing moves for no reason`() {
        val levels = listOf(
            ClassLevel("wizard", 5, isStarting = true),
            ClassLevel("cleric", 5),
        )
        val even = character("wizard", classLevels = levels, scores = mapOf("INT" to 16, "WIS" to 16))
        assertEquals(Ability.INT, CharacterCalculations.spellcastingAbility(even))
    }

    @Test
    fun `the DC list still shows every source, with the best marked primary`() {
        val levels = listOf(
            ClassLevel("wizard", 5, isStarting = true),
            ClassLevel("cleric", 5),
        )
        val character = character(
            "wizard", classLevels = levels, scores = mapOf("INT" to 10, "WIS" to 20),
        )
        val dcs = CharacterDcs.all(character)

        assertTrue("both classes are listed", dcs.count { it.id.startsWith("class:") } >= 2)
        val primary = dcs.filter { it.isPrimary }
        assertEquals("exactly one line is the headline", 1, primary.size)
        assertEquals("class:cleric", primary.first().id)
        assertEquals(
            "and it matches the headline number",
            CharacterCalculations.spellSaveDc(character),
            primary.first().dc,
        )
    }
}
