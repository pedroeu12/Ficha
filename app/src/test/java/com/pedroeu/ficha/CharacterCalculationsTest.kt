package com.pedroeu.ficha

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterCalculationsTest {

    private fun character(
        classId: String = "fighter",
        speciesId: String = "human",
        scores: Map<Ability, Int> = mapOf(
            Ability.STR to 16,
            Ability.DEX to 14,
            Ability.CON to 14,
            Ability.INT to 10,
            Ability.WIS to 12,
            Ability.CHA to 8,
        ),
        inventory: List<InventoryItem> = emptyList(),
        skillProficiencies: Set<Skill> = emptySet(),
        skillExpertise: Set<Skill> = emptySet(),
        featIds: List<String> = emptyList(),
    ) = PlayerCharacter(
        id = "test",
        name = "Test",
        speciesId = speciesId,
        classId = classId,
        backgroundId = "soldier",
        baseAbilityScores = scores.mapKeys { it.key.name },
        skillProficiencies = skillProficiencies.map { it.name }.toSet(),
        skillExpertise = skillExpertise.map { it.name }.toSet(),
        featIds = featIds,
        inventory = inventory,
    )

    @Test
    fun `modifier follows the standard table`() {
        assertEquals(-1, CharacterCalculations.modifier(8))
        assertEquals(0, CharacterCalculations.modifier(10))
        assertEquals(0, CharacterCalculations.modifier(11))
        assertEquals(3, CharacterCalculations.modifier(16))
        assertEquals(5, CharacterCalculations.modifier(20))
    }

    @Test
    fun `proficiency bonus scales with level`() {
        assertEquals(2, CharacterCalculations.proficiencyBonus(1))
        assertEquals(2, CharacterCalculations.proficiencyBonus(4))
        assertEquals(3, CharacterCalculations.proficiencyBonus(5))
        assertEquals(4, CharacterCalculations.proficiencyBonus(9))
        assertEquals(6, CharacterCalculations.proficiencyBonus(17))
    }

    @Test
    fun `background bonuses raise the final ability score`() {
        val base = character()
        val withBonus = base.copy(
            backgroundAbilityBonuses = mapOf(Ability.STR.name to 2, Ability.CON.name to 1)
        )
        val finals = CharacterCalculations.finalAbilityScores(withBonus)
        assertEquals(18, finals[Ability.STR])
        assertEquals(15, finals[Ability.CON])
        assertEquals(14, finals[Ability.DEX])
    }

    @Test
    fun `level 1 hit points use the full hit die plus constitution`() {
        // Fighter d10 with CON 14 (+2) -> 12
        assertEquals(12, CharacterCalculations.maxHitPoints(character()))
    }

    @Test
    fun `dwarven toughness adds a hit point per level`() {
        val dwarf = character(speciesId = "dwarf")
        val human = character(speciesId = "human")
        assertEquals(
            CharacterCalculations.maxHitPoints(human) + 1,
            CharacterCalculations.maxHitPoints(dwarf),
        )
    }

    @Test
    fun `tough feat adds twice the level in hit points`() {
        val plain = character()
        val tough = character(featIds = listOf("tough"))
        assertEquals(
            CharacterCalculations.maxHitPoints(plain) + 2,
            CharacterCalculations.maxHitPoints(tough),
        )
    }

    @Test
    fun `unarmored armor class is ten plus dexterity`() {
        // DEX 14 (+2) with no armor -> 12
        assertEquals(12, CharacterCalculations.armorClass(character()))
    }

    @Test
    fun `heavy armor ignores dexterity and adds a shield`() {
        val armored = character(
            inventory = listOf(
                InventoryItem(name = "Chain Mail", armorDefId = "chain_mail", equipped = true),
                InventoryItem(name = "Shield", armorDefId = "shield", equipped = true),
            )
        )
        // Chain mail 16 (no Dex) + shield 2 = 18
        assertEquals(18, CharacterCalculations.armorClass(armored))
    }

    @Test
    fun `medium armor caps the dexterity bonus at two`() {
        val nimble = character(
            scores = mapOf(
                Ability.STR to 10, Ability.DEX to 18, Ability.CON to 10,
                Ability.INT to 10, Ability.WIS to 10, Ability.CHA to 10,
            ),
            inventory = listOf(
                InventoryItem(name = "Half Plate", armorDefId = "half_plate", equipped = true)
            ),
        )
        // Half plate 15 + min(Dex +4, cap 2) = 17
        assertEquals(17, CharacterCalculations.armorClass(nimble))
    }

    @Test
    fun `barbarian unarmored defense uses constitution`() {
        val barbarian = character(
            classId = "barbarian",
            scores = mapOf(
                Ability.STR to 16, Ability.DEX to 14, Ability.CON to 16,
                Ability.INT to 8, Ability.WIS to 12, Ability.CHA to 10,
            ),
        )
        // 10 + Dex 2 + Con 3 = 15
        assertEquals(15, CharacterCalculations.armorClass(barbarian))
    }

    @Test
    fun `unequipped armor does not count toward armor class`() {
        val carried = character(
            inventory = listOf(
                InventoryItem(name = "Chain Mail", armorDefId = "chain_mail", equipped = false)
            )
        )
        assertEquals(12, CharacterCalculations.armorClass(carried))
    }

    @Test
    fun `skill bonus applies proficiency and expertise`() {
        val plain = character()
        val proficient = character(skillProficiencies = setOf(Skill.ATHLETICS))
        val expert = character(
            skillProficiencies = setOf(Skill.ATHLETICS),
            skillExpertise = setOf(Skill.ATHLETICS),
        )
        // STR 16 -> +3 base, +2 proficiency, +4 expertise
        assertEquals(3, CharacterCalculations.skillBonus(plain, Skill.ATHLETICS))
        assertEquals(5, CharacterCalculations.skillBonus(proficient, Skill.ATHLETICS))
        assertEquals(7, CharacterCalculations.skillBonus(expert, Skill.ATHLETICS))
    }

    @Test
    fun `saving throws add proficiency only for the class list`() {
        val fighter = character()
        // Fighter is proficient in STR and CON.
        assertEquals(5, CharacterCalculations.savingThrowBonus(fighter, Ability.STR))
        assertEquals(4, CharacterCalculations.savingThrowBonus(fighter, Ability.CON))
        assertEquals(2, CharacterCalculations.savingThrowBonus(fighter, Ability.DEX))
    }

    @Test
    fun `passive perception is ten plus the perception bonus`() {
        val perceptive = character(skillProficiencies = setOf(Skill.PERCEPTION))
        // WIS 12 -> +1, plus proficiency +2 = +3, so 13
        assertEquals(13, CharacterCalculations.passivePerception(perceptive))
    }

    @Test
    fun `alert feat adds proficiency bonus to initiative`() {
        val plain = character()
        val alert = character(featIds = listOf("alert"))
        assertEquals(2, CharacterCalculations.initiative(plain))
        assertEquals(4, CharacterCalculations.initiative(alert))
    }

    @Test
    fun `wood elf lineage raises walking speed`() {
        val woodElf = character(speciesId = "elf").copy(lineageId = "wood_elf")
        val highElf = character(speciesId = "elf").copy(lineageId = "high_elf")
        assertEquals(35, CharacterCalculations.speed(woodElf))
        assertEquals(30, CharacterCalculations.speed(highElf))
    }

    @Test
    fun `finesse weapons use the higher of strength or dexterity`() {
        val rogue = character(
            classId = "rogue",
            scores = mapOf(
                Ability.STR to 8, Ability.DEX to 18, Ability.CON to 12,
                Ability.INT to 14, Ability.WIS to 10, Ability.CHA to 12,
            ),
            inventory = listOf(
                InventoryItem(name = "Rapier", weaponDefId = "rapier", equipped = true)
            ),
        )
        val attack = CharacterCalculations.attacks(rogue).single()
        // Dex +4 plus proficiency +2; rogues are proficient with rapiers.
        assertEquals(6, attack.attackBonus)
        assertTrue(attack.damage.contains("+4"))
    }

    @Test
    fun `spell save dc uses the class spellcasting ability`() {
        val wizard = character(
            classId = "wizard",
            scores = mapOf(
                Ability.STR to 8, Ability.DEX to 14, Ability.CON to 14,
                Ability.INT to 16, Ability.WIS to 12, Ability.CHA to 10,
            ),
        )
        // 8 + proficiency 2 + INT 3 = 13
        assertEquals(13, CharacterCalculations.spellSaveDc(wizard))
        assertEquals(5, CharacterCalculations.spellAttackBonus(wizard))
    }

    @Test
    fun `non casters have no spell save dc`() {
        assertEquals(null, CharacterCalculations.spellSaveDc(character(classId = "barbarian")))
    }
}
