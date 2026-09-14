package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.FeatEdits
import com.pedroeu.ficha.domain.OverridableStat
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * What an edit dialog calls "the rules value" has to be the value without your edit in it.
 *
 * Every dialog that pins a number shows what it is pinning over, and offers to put it back.
 * Four screens worked that out four ways: the Skills tab cleared the skill maps, the Spells
 * tab the stat maps, the Stats tab the stat maps again, and the tablet cleared nothing — so
 * pinning Stealth to 17 and reopening the dialog showed "rules value 17" and offered to reset
 * it to itself. Once is enough, and this is what says so.
 */
class UnpinnedValueTest {

    private val fighter = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = "fighter",
        subclassId = "champion", backgroundId = "soldier", level = 8,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        skillProficiencies = setOf(Skill.ATHLETICS.name, Skill.PERCEPTION.name),
        inventory = listOf(
            InventoryItem("Chain Shirt", armorDefId = "chain_shirt", equipped = true),
            InventoryItem("Longsword", weaponDefId = "longsword", equipped = true),
        ),
    )

    @Test
    fun `a pinned stat does not become its own rules value`() {
        OverridableStat.entries.forEach { stat ->
            val rules = CharacterCalculations.unpinnedStat(fighter, stat)
            val pinned = fighter.copy(
                statOverrides = mapOf(stat.name to 99),
                statBonuses = mapOf(stat.name to 7),
            )
            assertEquals(
                "$stat read its own pin back as the rules value",
                rules,
                CharacterCalculations.unpinnedStat(pinned, stat),
            )
        }
    }

    @Test
    fun `a pinned skill does not become its own rules value`() {
        val rules = CharacterCalculations.unpinnedSkill(fighter, Skill.STEALTH)
        val pinned = fighter.copy(
            skillOverrides = mapOf(Skill.STEALTH.name to 17),
            skillBonuses = mapOf(Skill.STEALTH.name to 3),
        )
        assertEquals(17, CharacterCalculations.skillBonus(pinned, Skill.STEALTH))
        assertEquals(rules, CharacterCalculations.unpinnedSkill(pinned, Skill.STEALTH))
        assertNotEquals(17, CharacterCalculations.unpinnedSkill(pinned, Skill.STEALTH))
    }

    @Test
    fun `a pinned saving throw does not become its own rules value`() {
        val rules = CharacterCalculations.unpinnedSavingThrow(fighter, Ability.WIS)
        val pinned = fighter.copy(
            saveOverrides = mapOf(Ability.WIS.name to 19),
            saveBonuses = mapOf(Ability.WIS.name to 4),
        )
        assertEquals(19, CharacterCalculations.savingThrowBonus(pinned, Ability.WIS))
        assertEquals(rules, CharacterCalculations.unpinnedSavingThrow(pinned, Ability.WIS))
    }

    @Test
    fun `a pinned ability score does not become its own rules value`() {
        val rules = CharacterCalculations.unpinnedAbilityScore(fighter, Ability.STR)
        val pinned = fighter.copy(
            abilityScoreOverrides = mapOf(Ability.STR.name to 20),
            abilityScoreBonuses = mapOf(Ability.STR.name to 2),
        )
        assertEquals(20, CharacterCalculations.finalAbilityScores(pinned)[Ability.STR])
        assertEquals(rules, CharacterCalculations.unpinnedAbilityScore(pinned, Ability.STR))
    }

    /**
     * A pin on one number is a real input to another, and clearing it would answer a question
     * nobody asked: a Strength pinned by hand is the Strength the rules use for Athletics.
     */
    @Test
    fun `a pin on another number is left alone`() {
        val strong = fighter.copy(abilityScoreOverrides = mapOf(Ability.STR.name to 20))
        assertEquals(
            CharacterCalculations.skillBonus(strong, Skill.ATHLETICS),
            CharacterCalculations.unpinnedSkill(strong, Skill.ATHLETICS),
        )
    }

    /** And an increase a feat granted is the rules', not the player's, so it stays. */
    @Test
    fun `a feat's increase counts as the rules value`() {
        val choices = OriginChoices.forFeat("skill_expert")
        val expert = FeatEdits.add(
            fighter, "skill_expert",
            choices.associate { choice ->
                choice.id to when (choice.kind) {
                    ChoiceKind.ABILITY_SCORE -> listOf(Ability.DEX.name)
                    ChoiceKind.EXPERTISE -> listOf(Skill.ATHLETICS.name)
                    else -> listOf(Skill.STEALTH.name)
                }
            },
        )
        assertEquals(15, CharacterCalculations.finalAbilityScores(expert)[Ability.DEX])
        assertEquals(
            "resetting the score would have quietly removed the feat's increase",
            15,
            CharacterCalculations.unpinnedAbilityScore(expert, Ability.DEX),
        )
    }
}
