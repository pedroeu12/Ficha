package com.pedroeu.ficha

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.domain.ScoreMethod
import com.pedroeu.ficha.ui.creation.BonusSpread
import com.pedroeu.ficha.ui.creation.CreationState
import com.pedroeu.ficha.ui.creation.CreationStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreationStateTest {

    @Test
    fun `species step requires a lineage when the species offers one`() {
        val elfNoLineage = CreationState(step = CreationStep.SPECIES, speciesId = "elf")
        assertFalse(elfNoLineage.canAdvance)

        // Keen Senses is a choice of Insight, Perception, or Survival, so the Elf also needs
        // that pick before the step is complete.
        val elfWithLineage = elfNoLineage.copy(lineageId = "wood_elf")
        assertFalse("the Keen Senses skill is still unchosen", elfWithLineage.canAdvance)

        assertTrue(elfWithLineage.copy(speciesSkillChoices = setOf(Skill.PERCEPTION)).canAdvance)
    }

    @Test
    fun `species step requires the human bonus skill`() {
        val human = CreationState(step = CreationStep.SPECIES, speciesId = "human")
        assertFalse(human.canAdvance)
        assertTrue(human.copy(speciesSkillChoices = setOf(Skill.ARCANA)).canAdvance)
    }

    @Test
    fun `dwarf needs no extra choices`() {
        assertTrue(CreationState(step = CreationStep.SPECIES, speciesId = "dwarf").canAdvance)
    }

    @Test
    fun `class choices require every skill pick and feature option`() {
        val fighter = CreationState(step = CreationStep.CLASS_CHOICES, classId = "fighter")
        assertFalse(fighter.canAdvance)

        val withSkills = fighter.copy(
            classSkillChoices = setOf(Skill.ATHLETICS, Skill.PERCEPTION)
        )
        // Fighting Style is still unchosen.
        assertFalse(withSkills.canAdvance)

        val complete = withSkills.copy(
            classSelections = mapOf("fighting_style" to listOf("defense"))
        )
        assertTrue(complete.canAdvance)
    }

    @Test
    fun `wizard must pick the right number of cantrips and spellbook spells`() {
        val base = CreationState(
            step = CreationStep.CLASS_CHOICES,
            classId = "wizard",
            classSkillChoices = setOf(Skill.ARCANA, Skill.HISTORY),
        )
        assertFalse(base.canAdvance)

        val partial = base.copy(
            classSelections = mapOf(
                "cantrips" to listOf("wiz_fire_bolt", "wiz_mage_hand", "wiz_minor_illusion"),
                "spellbook" to listOf("wiz_magic_missile"),
            )
        )
        assertFalse(partial.canAdvance)

        val complete = base.copy(
            classSelections = mapOf(
                "cantrips" to listOf("wiz_fire_bolt", "wiz_mage_hand", "wiz_minor_illusion"),
                "spellbook" to listOf(
                    "wiz_magic_missile", "wiz_shield", "wiz_detect_magic",
                    "wiz_identify", "wiz_mage_armor", "wiz_sleep",
                ),
            )
        )
        assertTrue(complete.canAdvance)
    }

    @Test
    fun `rogue must choose two expertise skills`() {
        val rogue = CreationState(
            step = CreationStep.CLASS_CHOICES,
            classId = "rogue",
            classSkillChoices = setOf(
                Skill.STEALTH, Skill.PERCEPTION, Skill.INVESTIGATION, Skill.ACROBATICS
            ),
        )
        assertFalse(rogue.canAdvance)
        assertTrue(
            rogue.copy(expertiseChoices = setOf(Skill.STEALTH, Skill.PERCEPTION)).canAdvance
        )
    }

    @Test
    fun `granted skills combine species and background proficiencies`() {
        val state = CreationState(
            speciesId = "elf",
            // The Elf's Keen Senses is a pick among three skills rather than a fixed grant.
            speciesSkillChoices = setOf(Skill.PERCEPTION),
            backgroundId = "soldier",
        )
        assertTrue(state.grantedSkills.contains(Skill.PERCEPTION))
        assertTrue(state.grantedSkills.contains(Skill.ATHLETICS))
        assertTrue(state.grantedSkills.contains(Skill.INTIMIDATION))
    }

    @Test
    fun `background step requires the full bonus spread to be assigned`() {
        val acolyte = CreationState(step = CreationStep.BACKGROUND, backgroundId = "acolyte")
        assertFalse(acolyte.canAdvance)

        val partial = acolyte.copy(backgroundBonuses = mapOf(Ability.WIS to 2))
        assertFalse(partial.canAdvance)

        val complete = acolyte.copy(
            backgroundBonuses = mapOf(Ability.WIS to 2, Ability.INT to 1)
        )
        assertTrue(complete.canAdvance)
    }

    @Test
    fun `the three ones spread needs three abilities assigned`() {
        val state = CreationState(
            step = CreationStep.BACKGROUND,
            backgroundId = "acolyte",
            bonusSpread = BonusSpread.THREE_ONES,
            backgroundBonuses = mapOf(Ability.WIS to 1, Ability.INT to 1),
        )
        assertFalse(state.canAdvance)
        assertTrue(
            state.copy(
                backgroundBonuses = mapOf(Ability.WIS to 1, Ability.INT to 1, Ability.CHA to 1)
            ).canAdvance
        )
    }

    @Test
    fun `unassigned bonuses shrink as abilities are assigned`() {
        val state = CreationState(backgroundId = "acolyte", bonusSpread = BonusSpread.TWO_ONE)
        assertEquals(listOf(2, 1), state.unassignedBonuses())
        assertEquals(
            listOf(1),
            state.copy(backgroundBonuses = mapOf(Ability.WIS to 2)).unassignedBonuses(),
        )
        assertTrue(
            state.copy(backgroundBonuses = mapOf(Ability.WIS to 2, Ability.INT to 1))
                .unassignedBonuses().isEmpty()
        )
    }

    @Test
    fun `available pool removes assigned standard array values`() {
        val state = CreationState(scoreMethod = ScoreMethod.STANDARD_ARRAY)
        assertEquals(listOf(15, 14, 13, 12, 10, 8), state.availablePool())

        val assigned = state.copy(
            assignedScores = state.assignedScores + (Ability.STR to 15) + (Ability.DEX to 14)
        )
        assertEquals(listOf(13, 12, 10, 8), assigned.availablePool())
    }

    @Test
    fun `duplicate rolled values are consumed one at a time`() {
        val state = CreationState(
            scoreMethod = ScoreMethod.ROLL,
            rolledPool = listOf(15, 15, 12, 11, 10, 9),
            assignedScores = CreationState().assignedScores + (Ability.STR to 15),
        )
        // Only one of the two 15s should be consumed.
        assertEquals(listOf(15, 12, 11, 10, 9), state.availablePool())
    }

    @Test
    fun `abilities step needs every score assigned`() {
        val state = CreationState(
            step = CreationStep.ABILITIES,
            scoreMethod = ScoreMethod.STANDARD_ARRAY,
        )
        assertFalse(state.canAdvance)

        val filled = state.copy(
            assignedScores = mapOf(
                Ability.STR to 15, Ability.DEX to 14, Ability.CON to 13,
                Ability.INT to 12, Ability.WIS to 10, Ability.CHA to 8,
            )
        )
        assertTrue(filled.canAdvance)
    }

    @Test
    fun `preview scores fold in the background bonuses`() {
        val state = CreationState(
            scoreMethod = ScoreMethod.STANDARD_ARRAY,
            assignedScores = mapOf(
                Ability.STR to 15, Ability.DEX to 14, Ability.CON to 13,
                Ability.INT to 12, Ability.WIS to 10, Ability.CHA to 8,
            ),
            backgroundBonuses = mapOf(Ability.STR to 2, Ability.CON to 1),
        )
        val finals = state.previewFinalScores()
        assertEquals(17, finals[Ability.STR])
        assertEquals(14, finals[Ability.CON])
        assertEquals(14, finals[Ability.DEX])
    }

    @Test
    fun `details step needs a non blank name`() {
        val state = CreationState(step = CreationStep.DETAILS)
        assertFalse(state.canAdvance)
        assertFalse(state.copy(name = "   ").canAdvance)
        assertTrue(state.copy(name = "Aria").canAdvance)
    }
}
