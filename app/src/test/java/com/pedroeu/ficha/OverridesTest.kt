package com.pedroeu.ficha

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.OverridableStat
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Edit Mode exposes two mechanisms that must compose predictably: a *bonus* adds to whatever
 * the rules produce, and an *override* replaces it outright. Overrides always win.
 */
class OverridesTest {

    private val base = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = "fighter",
        backgroundId = "soldier",
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        skillProficiencies = setOf(Skill.ATHLETICS.name),
        inventory = listOf(
            InventoryItem("Chain Shirt", armorDefId = "chain_shirt", equipped = true),
        ),
    )

    @Test
    fun `a stat bonus adds to the rules result`() {
        val withBonus = base.copy(
            statBonuses = mapOf(OverridableStat.ARMOR_CLASS.name to 2)
        )
        assertEquals(
            CharacterCalculations.armorClass(base) + 2,
            CharacterCalculations.armorClass(withBonus),
        )
    }

    @Test
    fun `a stat override replaces the rules result entirely`() {
        val withOverride = base.copy(
            statOverrides = mapOf(OverridableStat.ARMOR_CLASS.name to 25)
        )
        assertEquals(25, CharacterCalculations.armorClass(withOverride))
    }

    @Test
    fun `an override wins over a bonus on the same stat`() {
        val both = base.copy(
            statBonuses = mapOf(OverridableStat.ARMOR_CLASS.name to 5),
            statOverrides = mapOf(OverridableStat.ARMOR_CLASS.name to 12),
        )
        assertEquals(12, CharacterCalculations.armorClass(both))
    }

    @Test
    fun `a narrative skill bonus stacks on top of proficiency`() {
        val withBonus = base.copy(skillBonuses = mapOf(Skill.STEALTH.name to 2))
        assertEquals(
            CharacterCalculations.skillBonus(base, Skill.STEALTH) + 2,
            CharacterCalculations.skillBonus(withBonus, Skill.STEALTH),
        )
        // Other skills are untouched.
        assertEquals(
            CharacterCalculations.skillBonus(base, Skill.ATHLETICS),
            CharacterCalculations.skillBonus(withBonus, Skill.ATHLETICS),
        )
    }

    @Test
    fun `a skill override pins the total`() {
        val withOverride = base.copy(skillOverrides = mapOf(Skill.ARCANA.name to 17))
        assertEquals(17, CharacterCalculations.skillBonus(withOverride, Skill.ARCANA))
    }

    @Test
    fun `saving throw proficiency can be granted and revoked independently of the class`() {
        // A Fighter is proficient in Strength and Constitution saves by default.
        assertTrue(CharacterCalculations.isSavingThrowProficient(base, Ability.STR))
        assertFalse(CharacterCalculations.isSavingThrowProficient(base, Ability.CHA))

        val edited = base.copy(
            saveProficiencyOverrides = mapOf(
                Ability.STR.name to false,
                Ability.CHA.name to true,
            )
        )
        assertFalse(CharacterCalculations.isSavingThrowProficient(edited, Ability.STR))
        assertTrue(CharacterCalculations.isSavingThrowProficient(edited, Ability.CHA))

        val pb = CharacterCalculations.proficiencyBonus(edited)
        val chaMod = CharacterCalculations.abilityModifiers(edited)[Ability.CHA]!!
        assertEquals(chaMod + pb, CharacterCalculations.savingThrowBonus(edited, Ability.CHA))
    }

    @Test
    fun `a saving throw bonus adds on top`() {
        val withBonus = base.copy(saveBonuses = mapOf(Ability.WIS.name to 3))
        assertEquals(
            CharacterCalculations.savingThrowBonus(base, Ability.WIS) + 3,
            CharacterCalculations.savingThrowBonus(withBonus, Ability.WIS),
        )
    }

    @Test
    fun `ability score bonuses and overrides both feed the modifier`() {
        val bonused = base.copy(abilityScoreBonuses = mapOf(Ability.STR.name to 2))
        assertEquals(16, CharacterCalculations.finalAbilityScores(bonused)[Ability.STR])
        assertEquals(3, CharacterCalculations.abilityModifiers(bonused)[Ability.STR])

        val overridden = base.copy(
            abilityScoreBonuses = mapOf(Ability.STR.name to 2),
            abilityScoreOverrides = mapOf(Ability.STR.name to 20),
        )
        assertEquals(20, CharacterCalculations.finalAbilityScores(overridden)[Ability.STR])
        assertEquals(5, CharacterCalculations.abilityModifiers(overridden)[Ability.STR])
    }

    @Test
    fun `ability score improvements from levelling feed the total`() {
        val improved = base.copy(
            level = 4,
            abilityScoreImprovements = mapOf(Ability.STR.name to 2),
        )
        assertEquals(16, CharacterCalculations.finalAbilityScores(improved)[Ability.STR])
    }

    @Test
    fun `spell slot overrides replace the class table per level`() {
        val wizard = base.copy(classId = "wizard", level = 3)
        val fromTable = CharacterCalculations.spellSlots(wizard)
        assertEquals(4, fromTable[1])
        assertEquals(2, fromTable[2])

        val edited = wizard.copy(spellSlotOverrides = mapOf("1" to 6, "9" to 1))
        val slots = CharacterCalculations.spellSlots(edited)
        assertEquals(6, slots[1])
        // Untouched levels still follow the table.
        assertEquals(2, slots[2])
        // And a level the table doesn't grant can be added outright.
        assertEquals(1, slots[9])
    }

    @Test
    fun `setting a slot override to zero removes that level`() {
        val wizard = base.copy(classId = "wizard", level = 3)
        val edited = wizard.copy(spellSlotOverrides = mapOf("2" to 0))
        assertFalse(CharacterCalculations.spellSlots(edited).containsKey(2))
    }

    @Test
    fun `max hit points and prepared spell counts are overridable`() {
        val edited = base.copy(
            classId = "cleric",
            level = 5,
            statOverrides = mapOf(
                OverridableStat.MAX_HIT_POINTS.name to 99,
                OverridableStat.MAX_PREPARED_SPELLS.name to 30,
            ),
        )
        assertEquals(99, CharacterCalculations.maxHitPoints(edited))
        assertEquals(30, CharacterCalculations.maxPreparedSpells(edited))
    }

    @Test
    fun `max hit points never drops below one however it is edited`() {
        val edited = base.copy(statOverrides = mapOf(OverridableStat.MAX_HIT_POINTS.name to -5))
        assertTrue(CharacterCalculations.maxHitPoints(edited) >= 1)
    }

    @Test
    fun `a character with no edits reports none`() {
        assertFalse(CharacterCalculations.hasManualAdjustments(base))
        assertTrue(
            CharacterCalculations.hasManualAdjustments(
                base.copy(skillBonuses = mapOf(Skill.STEALTH.name to 1))
            )
        )
    }
}
