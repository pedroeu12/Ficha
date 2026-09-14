package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterDcs
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.FeatEdits
import com.pedroeu.ficha.domain.Multiclassing
import com.pedroeu.ficha.domain.OverridableStat
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A number changed in one place moves every number that depends on it.
 *
 * Edit Mode pins an ability score or the Proficiency Bonus, and the whole sheet reads through
 * the same two functions — so the cascade is either complete or broken for everything. These
 * name the dependents one by one so a reader that stops going through the shared function
 * is caught by name.
 */
class CascadeTest {

    private fun character(classId: String, level: Int, subclassId: String? = null) = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = classId, subclassId = subclassId,
        backgroundId = "soldier", level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 12 },
        skillProficiencies = setOf(Skill.PERCEPTION.name),
        inventory = listOf(InventoryItem("Dagger", weaponDefId = "dagger", equipped = true)),
    )

    @Test
    fun `a pinned casting score moves the DC, the attack, the save and the skill together`() {
        val bard = character("bard", 5, "lore")
        val pinned = bard.copy(abilityScoreOverrides = mapOf(Ability.CHA.name to 20))
        val delta = 5 - 1

        assertEquals(CharacterCalculations.spellSaveDc(bard)!! + delta, CharacterCalculations.spellSaveDc(pinned))
        assertEquals(CharacterCalculations.spellAttackBonus(bard)!! + delta, CharacterCalculations.spellAttackBonus(pinned))
        assertEquals(CharacterDcs.primary(bard)!!.dc + delta, CharacterDcs.primary(pinned)!!.dc)
        assertEquals(
            CharacterCalculations.savingThrowBonus(bard, Ability.CHA) + delta,
            CharacterCalculations.savingThrowBonus(pinned, Ability.CHA),
        )
        assertEquals(
            CharacterCalculations.skillBonus(bard, Skill.PERSUASION) + delta,
            CharacterCalculations.skillBonus(pinned, Skill.PERSUASION),
        )
        // Bardic Inspiration is "a number of times equal to your Charisma modifier".
        val inspiration = { pc: PlayerCharacter ->
            CharacterResources.definitions(pc).first { it.name.contains("Bardic Inspiration") }.max
        }
        assertEquals(inspiration(bard) + delta, inspiration(pinned))
        // And the multiclass door that Charisma opens.
        assertFalse(Multiclassing.canTake(bard.copy(abilityScoreOverrides = mapOf(Ability.CHA.name to 12, Ability.INT.name to 12)), "sorcerer") &&
            Multiclassing.canTake(bard.copy(abilityScoreOverrides = mapOf(Ability.CHA.name to 12)), "wizard"))
        assertTrue(Multiclassing.canTake(pinned.copy(abilityScoreOverrides = pinned.abilityScoreOverrides + (Ability.INT.name to 13)), "wizard"))
    }

    @Test
    fun `a pinned Constitution moves hit points by the levels it touches`() {
        val fighter = character("fighter", 6, "champion")
        val pinned = fighter.copy(abilityScoreOverrides = mapOf(Ability.CON.name to 18))
        assertEquals(
            CharacterCalculations.maxHitPoints(fighter) + 6 * 3,
            CharacterCalculations.maxHitPoints(pinned),
        )
    }

    @Test
    fun `a pinned Dexterity moves armor class, initiative and a finesse attack`() {
        val rogue = character("rogue", 3, "thief")
        val pinned = rogue.copy(abilityScoreOverrides = mapOf(Ability.DEX.name to 18))
        assertEquals(CharacterCalculations.armorClass(rogue) + 3, CharacterCalculations.armorClass(pinned))
        assertEquals(CharacterCalculations.initiative(rogue) + 3, CharacterCalculations.initiative(pinned))
        val dagger = { pc: PlayerCharacter -> CharacterCalculations.attacks(pc).first { it.name == "Dagger" } }
        assertEquals(dagger(rogue).attackBonus + 3, dagger(pinned).attackBonus)
    }

    @Test
    fun `a pinned Proficiency Bonus reaches saves, skills, attacks and the DC`() {
        val cleric = character("cleric", 3, "life_domain")
        val pinned = cleric.copy(statOverrides = mapOf(OverridableStat.PROFICIENCY_BONUS.name to 6))
        val delta = 6 - 2
        assertEquals(
            CharacterCalculations.savingThrowBonus(cleric, Ability.WIS) + delta,
            CharacterCalculations.savingThrowBonus(pinned, Ability.WIS),
        )
        assertEquals(
            CharacterCalculations.skillBonus(cleric, Skill.PERCEPTION) + delta,
            CharacterCalculations.skillBonus(pinned, Skill.PERCEPTION),
        )
        assertEquals(CharacterCalculations.spellSaveDc(cleric)!! + delta, CharacterCalculations.spellSaveDc(pinned))
        val dagger = { pc: PlayerCharacter -> CharacterCalculations.attacks(pc).first { it.name == "Dagger" } }
        assertEquals(dagger(cleric).attackBonus + delta, dagger(pinned).attackBonus)
        assertEquals(CharacterCalculations.passivePerception(cleric) + delta, CharacterCalculations.passivePerception(pinned))
    }

    @Test
    fun `a feat's spell brings its own save DC, and its ability increase its own modifier`() {
        val monk = character("monk", 4)
        val magic = OriginChoices.forFeat("magic_initiate_wizard")
        val selections = magic.associate { choice ->
            choice.id to when (choice.kind) {
                ChoiceKind.ABILITY_SCORE -> listOf(Ability.INT.name)
                else -> choice.options.take(choice.count).map { it.id }
            }
        }
        val initiate = FeatEdits.add(monk, "magic_initiate_wizard", selections)
        val dcs = CharacterDcs.all(initiate)
        assertTrue(
            "the feat's cantrips force saves and the sheet should say against what: ${dcs.map { it.id }}",
            dcs.any { it.id == "feat:magic_initiate_wizard" },
        )
        assertTrue("the Monk's own DC is still there", dcs.any { it.id.startsWith("feature:") || it.id.startsWith("class:") })

        val skilled = OriginChoices.forFeat("skill_expert")
        val expert = FeatEdits.add(
            monk, "skill_expert",
            skilled.associate { choice ->
                choice.id to when (choice.kind) {
                    ChoiceKind.ABILITY_SCORE -> listOf(Ability.DEX.name)
                    ChoiceKind.EXPERTISE -> listOf(Skill.PERCEPTION.name)
                    else -> listOf(Skill.STEALTH.name)
                }
            },
        )
        assertEquals(13, CharacterCalculations.finalAbilityScores(expert)[Ability.DEX])
        assertEquals(
            "the doubled proficiency shows in the number",
            CharacterCalculations.skillBonus(monk, Skill.PERCEPTION) + 2,
            CharacterCalculations.skillBonus(expert, Skill.PERCEPTION),
        )
    }
}
