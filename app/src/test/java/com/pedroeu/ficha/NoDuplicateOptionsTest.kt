package com.pedroeu.ficha

import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.ChoiceOption
import com.pedroeu.ficha.data.model.ChoiceOptions
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.domain.Owned
import com.pedroeu.ficha.domain.OwnedOptions
import com.pedroeu.ficha.ui.creation.CreationState
import com.pedroeu.ficha.ui.creation.CreationStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A skill from a background and the same skill from a class list are one proficiency, so
 * taking both wastes a pick. These tests cover the general rule — nothing already owned is
 * ever offered again — and the creation order that makes it work for backgrounds.
 */
class NoDuplicateOptionsTest {

    private fun skillChoice(vararg skills: Skill) = Choice(
        id = "skills",
        label = "Skill Proficiencies",
        count = 2,
        kind = ChoiceKind.SKILL,
        options = ChoiceOptions.fromSkills(skills.toList()),
    )

    // ------------------------------------------------------------- Creation order

    @Test
    fun `background is chosen before class, so its grants are known in time`() {
        val order = CreationStep.ORDER
        assertTrue(
            "Background must come before Class for its skills to be filterable",
            order.indexOf(CreationStep.BACKGROUND) < order.indexOf(CreationStep.CLASS),
        )
    }

    @Test
    fun `origin options still come after class options, which some of them depend on`() {
        val order = CreationStep.ORDER
        assertTrue(
            order.indexOf(CreationStep.ORIGIN_CHOICES) > order.indexOf(CreationStep.CLASS_CHOICES),
        )
    }

    @Test
    fun `the wizard still starts at species and ends at details`() {
        assertEquals(CreationStep.SPECIES, CreationStep.ORDER.first())
        assertEquals(CreationStep.DETAILS, CreationStep.ORDER.last())
    }

    // ------------------------------------------------------------- The general rule

    @Test
    fun `a skill already owned is not offered again`() {
        val owned = Owned(skills = setOf(Skill.ATHLETICS.name))
        val disabled = OwnedOptions.disabledFor(
            skillChoice(Skill.ATHLETICS, Skill.STEALTH, Skill.ARCANA),
            owned,
        )

        assertTrue(disabled.contains(Skill.ATHLETICS.name))
        assertFalse(disabled.contains(Skill.STEALTH.name))
    }

    @Test
    fun `what you just picked in this very choice stays selectable so you can undo it`() {
        val owned = Owned(skills = setOf(Skill.ATHLETICS.name, Skill.STEALTH.name))
        val disabled = OwnedOptions.disabledFor(
            choice = skillChoice(Skill.ATHLETICS, Skill.STEALTH),
            owned = owned,
            currentSelection = setOf(Skill.STEALTH.name),
        )

        assertTrue("owned elsewhere, so blocked", disabled.contains(Skill.ATHLETICS.name))
        assertFalse("picked here, so still tappable", disabled.contains(Skill.STEALTH.name))
    }

    @Test
    fun `expertise needs a proficiency and can't be doubled`() {
        val owned = Owned(
            skills = setOf(Skill.ATHLETICS.name, Skill.STEALTH.name),
            expertise = setOf(Skill.STEALTH.name),
        )
        val choice = skillChoice(Skill.ATHLETICS, Skill.STEALTH, Skill.ARCANA)
            .copy(kind = ChoiceKind.EXPERTISE)
        val disabled = OwnedOptions.disabledFor(choice, owned)

        assertFalse("proficient and no expertise yet", disabled.contains(Skill.ATHLETICS.name))
        assertTrue("already has expertise here", disabled.contains(Skill.STEALTH.name))
        assertTrue("not proficient, so nothing to double", disabled.contains(Skill.ARCANA.name))
    }

    @Test
    fun `a tool already owned is not offered again, whatever the spelling`() {
        val owned = Owned(tools = setOf("thieves' tool"))
        val choice = Choice(
            id = "tools", label = "Tools", kind = ChoiceKind.TOOL,
            options = listOf(
                ChoiceOption("t1", "Thieves' Tools"),
                ChoiceOption("t2", "Smith's Tools"),
            ),
        )
        val disabled = OwnedOptions.disabledFor(choice, owned)

        assertTrue("case and plural shouldn't matter", disabled.contains("t1"))
        assertFalse(disabled.contains("t2"))
    }

    @Test
    fun `a spell already known is not offered again, even under a different id`() {
        // Class cantrip lists use ids like "wiz_mage_hand"; the catalog uses "mage_hand".
        val owned = Owned(spells = setOf("mage_hand"), spellNames = setOf("mage hand"))
        val choice = Choice(
            id = "cantrips", label = "Cantrips", kind = ChoiceKind.SPELL,
            options = listOf(
                ChoiceOption("wiz_mage_hand", "Mage Hand"),
                ChoiceOption("wiz_light", "Light"),
            ),
        )
        val disabled = OwnedOptions.disabledFor(choice, owned)

        assertTrue("matched by name across id schemes", disabled.contains("wiz_mage_hand"))
        assertFalse(disabled.contains("wiz_light"))
    }

    @Test
    fun `a language already known is not offered again`() {
        val owned = Owned(languages = setOf("elvish"))
        val choice = Choice(
            id = "langs", label = "Languages", kind = ChoiceKind.LANGUAGE,
            options = listOf(ChoiceOption("Elvish", "Elvish"), ChoiceOption("Dwarvish", "Dwarvish")),
        )
        val disabled = OwnedOptions.disabledFor(choice, owned)

        assertTrue(disabled.contains("Elvish"))
        assertFalse(disabled.contains("Dwarvish"))
    }

    @Test
    fun `a feat already taken is not offered again`() {
        val owned = Owned(feats = setOf("alert"))
        val choice = Choice(
            id = "feat", label = "Feat", kind = ChoiceKind.FEAT,
            options = listOf(ChoiceOption("alert", "Alert"), ChoiceOption("tough", "Tough")),
        )
        val disabled = OwnedOptions.disabledFor(choice, owned)

        assertTrue(disabled.contains("alert"))
        assertFalse(disabled.contains("tough"))
    }

    @Test
    fun `a feature option already taken is not offered again`() {
        // Metamagic comes back at several levels, and the same one can't be taken twice.
        val owned = Owned(options = setOf("quickened"))
        val choice = Choice(
            id = "metamagic_10", label = "Metamagic", kind = ChoiceKind.OPTION,
            options = listOf(
                ChoiceOption("quickened", "Quickened Spell"),
                ChoiceOption("subtle", "Subtle Spell"),
            ),
        )
        val disabled = OwnedOptions.disabledFor(choice, owned)

        assertTrue(disabled.contains("quickened"))
        assertFalse(disabled.contains("subtle"))
    }

    @Test
    fun `repeatable kinds are never disabled`() {
        val owned = Owned(
            skills = setOf("STR"), options = setOf("Fire"), feats = setOf("champion"),
        )
        listOf(ChoiceKind.ABILITY_SCORE, ChoiceKind.DAMAGE_TYPE, ChoiceKind.SUBCLASS)
            .forEach { kind ->
                val choice = Choice(
                    id = "c", label = "c", kind = kind,
                    options = listOf(ChoiceOption("STR", "Strength"), ChoiceOption("Fire", "Fire")),
                )
                assertTrue(
                    "$kind should never grey anything out",
                    OwnedOptions.disabledFor(choice, owned).isEmpty(),
                )
            }
    }

    @Test
    fun `nothing is disabled when the character owns nothing`() {
        val choice = skillChoice(Skill.ATHLETICS, Skill.STEALTH)
        assertTrue(OwnedOptions.disabledFor(choice, Owned()).isEmpty())
    }

    // ------------------------------------------------- The wizard's own owned set

    @Test
    fun `the wizard counts a background's skills as already owned`() {
        // Soldier grants Athletics and Intimidation outright.
        val state = CreationState(
            speciesId = "human",
            speciesSkillChoices = setOf(Skill.ARCANA),
            backgroundId = "soldier",
            classId = "fighter",
        )

        assertTrue(state.owned.skills.contains(Skill.ATHLETICS.name))
        assertTrue(state.owned.skills.contains(Skill.INTIMIDATION.name))
        assertTrue("the species pick counts too", state.owned.skills.contains(Skill.ARCANA.name))
    }

    @Test
    fun `a class skill list greys out what the background already granted`() {
        val state = CreationState(
            speciesId = "human",
            speciesSkillChoices = setOf(Skill.ARCANA),
            backgroundId = "soldier",
            classId = "fighter",
        )
        val fighterSkills = state.charClass!!.choices
            .filterIsInstance<com.pedroeu.ficha.data.model.ClassChoice.SkillProficiencyChoice>()
            .first()

        // Athletics and Intimidation are on both the Soldier background and the Fighter list.
        val blocked = fighterSkills.options.filter { it in state.grantedSkills }
        assertTrue("the overlap should be real for this pairing", blocked.isNotEmpty())
        assertTrue(blocked.contains(Skill.ATHLETICS))
    }

    // ------------------------------------------------- Going back and changing an earlier step

    @Test
    fun `changing the background drops a class skill it now grants for free`() {
        // Picked Athletics as a Fighter, then went back and chose Soldier, which grants it.
        val state = CreationState(
            speciesId = "human",
            speciesSkillChoices = setOf(Skill.ARCANA),
            classId = "fighter",
            classSkillChoices = setOf(Skill.ATHLETICS, Skill.SURVIVAL),
            backgroundId = "soldier",
        )

        val cleaned = state.withoutDuplicateSkills()
        assertFalse(
            "Athletics comes free from Soldier, so the class pick is released",
            cleaned.classSkillChoices.contains(Skill.ATHLETICS),
        )
        assertTrue(
            "the pick that isn't duplicated is kept",
            cleaned.classSkillChoices.contains(Skill.SURVIVAL),
        )
    }

    @Test
    fun `expertise riding on a released skill is kept when the skill is still owned`() {
        val state = CreationState(
            speciesId = "human",
            classId = "rogue",
            classSkillChoices = setOf(Skill.ATHLETICS),
            expertiseChoices = setOf(Skill.ATHLETICS),
            backgroundId = "soldier",
        )

        val cleaned = state.withoutDuplicateSkills()
        assertFalse(cleaned.classSkillChoices.contains(Skill.ATHLETICS))
        assertTrue(
            "the character still has Athletics from the background, so Expertise stands",
            cleaned.expertiseChoices.contains(Skill.ATHLETICS),
        )
    }

    @Test
    fun `expertise on a skill the character no longer has anywhere is dropped`() {
        val state = CreationState(
            speciesId = "human",
            classId = "rogue",
            classSkillChoices = emptySet(),
            expertiseChoices = setOf(Skill.ACROBATICS),
        )

        assertTrue(state.withoutDuplicateSkills().expertiseChoices.isEmpty())
    }

    @Test
    fun `cleaning up is safe to run when there is nothing to clean`() {
        val state = CreationState(
            speciesId = "dwarf",
            classId = "fighter",
            classSkillChoices = setOf(Skill.SURVIVAL),
            backgroundId = "soldier",
        )

        assertEquals(state.classSkillChoices, state.withoutDuplicateSkills().classSkillChoices)
    }

    @Test
    fun `the wizard counts the background's feat as already owned`() {
        val state = CreationState(backgroundId = "soldier")
        val featId = state.background!!.featId
        assertTrue(state.owned.feats.contains(featId))
    }

    @Test
    fun `tools granted by class and background both count as owned`() {
        val state = CreationState(classId = "artificer", backgroundId = "soldier")
        assertTrue(
            "the Artificer's fixed tools are owned",
            state.owned.tools.any { it.contains("Tinker", ignoreCase = true) },
        )
        assertFalse(
            "an open-ended 'of your choice' entry isn't a specific tool yet",
            state.owned.tools.any { it.contains("of your choice", ignoreCase = true) },
        )
    }
}
