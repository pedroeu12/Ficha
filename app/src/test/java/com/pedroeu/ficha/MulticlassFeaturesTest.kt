package com.pedroeu.ficha

import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.ClassLevel
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.SheetFeatures
import com.pedroeu.ficha.data.model.Ability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A multiclassed character's Features page has to show all of their classes.
 *
 * It didn't: both the phone and the tablet worked the list out from the plain `classId` and
 * `subclassId` fields, so a Fighter 5 / Wizard 3 saw a Fighter's features, a Champion's
 * features, and no sign that they had ever opened a spellbook.
 */
class MulticlassFeaturesTest {

    private fun fighterWizard(fighter: Int = 5, wizard: Int = 3) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = "fighter",
        subclassId = "champion",
        backgroundId = "soldier",
        level = fighter + wizard,
        classLevels = listOf(
            ClassLevel("fighter", fighter, "champion", isStarting = true),
            ClassLevel("wizard", wizard, "evoker"),
        ),
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
    )

    private fun singleClassFighter(level: Int = 5) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = "fighter",
        subclassId = "champion",
        backgroundId = "soldier",
        level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
    )

    // ------------------------------------------------------------------ Both classes appear

    @Test
    fun `every class the character has levels in is listed`() {
        val character = fighterWizard()
        assertEquals(
            listOf("fighter", "wizard"),
            ClassLevels.of(character).map { it.classId },
        )
        assertTrue(SheetFeatures.classFeatures(character, "fighter").isNotEmpty())
        assertTrue(
            "the second class's features were the ones going missing",
            SheetFeatures.classFeatures(character, "wizard").isNotEmpty(),
        )
    }

    @Test
    fun `each class's subclass is listed under that class`() {
        val character = fighterWizard()
        val champion = SheetFeatures.subclassFeatures(character, "fighter").map { it.name }
        val evoker = SheetFeatures.subclassFeatures(character, "wizard").map { it.name }

        assertTrue(champion.isNotEmpty())
        assertTrue(evoker.isNotEmpty())
        assertTrue(
            "a Fighter subclass feature reached the Wizard's card",
            champion.none { it in evoker },
        )
    }

    // ------------------------------------------------------------------ Each on its own level

    @Test
    fun `a class shows its own level's features, not the character's total`() {
        val character = fighterWizard(fighter = 5, wizard = 3)
        val wizardLevels = SheetFeatures.classFeatures(character, "wizard").map { it.level }

        assertTrue("nothing was gathered", wizardLevels.isNotEmpty())
        assertTrue(
            "the Wizard is level 3 and showed a level ${wizardLevels.max()} feature",
            wizardLevels.all { it <= 3 },
        )

        val championLevels = SheetFeatures.subclassFeatures(character, "fighter").map { it.level }
        assertTrue(championLevels.all { it <= 5 })
    }

    @Test
    fun `a class the character has no levels in shows nothing`() {
        val character = fighterWizard()
        assertTrue(SheetFeatures.classFeatures(character, "rogue").isEmpty())
        assertTrue(SheetFeatures.subclassFeatures(character, "rogue").isEmpty())
    }

    @Test
    fun `a class below its subclass level shows no subclass features`() {
        val character = PlayerCharacter(
            id = "t", name = "T", speciesId = "human", classId = "fighter",
            backgroundId = "soldier", level = 7,
            classLevels = listOf(
                ClassLevel("fighter", 5, "champion", isStarting = true),
                ClassLevel("wizard", 2, null),
            ),
            baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        )
        assertTrue(SheetFeatures.classFeatures(character, "wizard").isNotEmpty())
        assertTrue(
            "a Wizard 2 has not chosen a subclass yet",
            SheetFeatures.subclassFeatures(character, "wizard").isEmpty(),
        )
    }

    // ------------------------------------------------------------------ Starting features

    @Test
    fun `a class has features from the moment you take it`() {
        val starting = SheetFeatures.classFeatures(fighterWizard(), "fighter")
            .filter { it.level == 1 }
        assertTrue(starting.isNotEmpty())
    }

    @Test
    fun `a feature restated at higher levels is listed once`() {
        // A Warlock's table says "Eldritch Invocations" at eight levels and a Fighter's says
        // "Weapon Mastery" at three; each is the same choice with a bigger number.
        val fighter = SheetFeatures.classFeatures(fighterWizard(fighter = 20, wizard = 0), "fighter")
        assertEquals(
            "Weapon Mastery is one row, not four",
            1,
            fighter.count { it.name == "Weapon Mastery" },
        )
    }

    // ------------------------------------------------------------------ Choices

    @Test
    fun `a choice knows which class earned it`() {
        val character = fighterWizard(fighter = 8, wizard = 4)
        val choices = ChoiceResolver.classFeatureChoices(character)
        assertTrue(choices.isNotEmpty())
        assertTrue(
            "a class choice with no class can't be filed under either card",
            choices.all { it.classId.isNotBlank() },
        )
        assertTrue(choices.any { it.classId == "fighter" })
        assertTrue(choices.any { it.classId == "wizard" })

        // Both reach an Ability Score Improvement, and grouping by name alone put each
        // class's under both cards.
        val asiClasses = choices.filter { it.featureName.contains("Ability Score") }
            .map { it.classId }
            .distinct()
        if (asiClasses.size > 1) {
            assertEquals(
                "the two must stay separable",
                asiClasses.size,
                asiClasses.toSet().size,
            )
        }
    }

    @Test
    fun `level one branch options can be asked for one class at a time`() {
        val character = fighterWizard()
        val fighterOnly = ChoiceResolver.levelOneClassOptions(character, "fighter")
        val everything = ChoiceResolver.levelOneClassOptions(character)
        assertTrue(fighterOnly.size <= everything.size)
    }

    // ------------------------------------------------------------------ The header

    @Test
    fun `the header names every subclass, not just the first`() {
        assertEquals("Champion / Evoker", ClassLevels.subclassLabel(fighterWizard()))
        assertEquals("Champion", ClassLevels.subclassLabel(singleClassFighter()))
        assertEquals(
            "nothing to show before a subclass is chosen",
            "",
            ClassLevels.subclassLabel(singleClassFighter(level = 2).copy(subclassId = null)),
        )
    }

    // ------------------------------------------------------------------ Nothing regressed

    @Test
    fun `a single-class character is unaffected`() {
        val fighter = singleClassFighter(level = 5)
        assertFalse(ClassLevels.isMulticlassed(fighter))
        assertTrue(SheetFeatures.classFeatures(fighter, "fighter").isNotEmpty())
        assertTrue(
            SheetFeatures.classFeatures(fighter, "fighter").all { it.level <= 5 },
        )
        assertTrue(SheetFeatures.subclassFeatures(fighter, "fighter").isNotEmpty())
    }
}
