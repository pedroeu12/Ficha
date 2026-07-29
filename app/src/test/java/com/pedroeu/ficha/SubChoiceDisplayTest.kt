package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the bug where a choice made inside a feature was saved but only the
 * parent feature's name was ever shown. The Monk's tattoos are the reported case; the same
 * pattern covers maneuvers, Metamagic, Fighting Styles taken after level 1, and the rest.
 */
class SubChoiceDisplayTest {

    private fun character(
        classId: String,
        level: Int,
        subclassId: String? = null,
        levelSelections: Map<String, List<String>> = emptyMap(),
        classChoiceSelections: Map<String, List<String>> = emptyMap(),
        originChoiceSelections: Map<String, List<String>> = emptyMap(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = classId,
        subclassId = subclassId,
        backgroundId = "soldier",
        level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        levelSelections = levelSelections,
        classChoiceSelections = classChoiceSelections,
        originChoiceSelections = originChoiceSelections,
    )

    @Test
    fun `a monk's chosen tattoos are resolved for display, not just the feature name`() {
        val monk = character(
            classId = "monk",
            level = 3,
            subclassId = "tattooed_warrior",
            levelSelections = mapOf("3:beast_tattoos" to listOf("bat", "crane")),
        )

        val resolved = ChoiceResolver.subclassFeatureChoices(monk)
            .first { it.choice.id == "beast_tattoos" }

        assertTrue("the choice must read as answered", resolved.isAnswered)
        assertEquals(listOf("Bat", "Crane"), resolved.selectedNames)
        assertEquals("Bat, Crane", resolved.summary)
        assertEquals("Beast Tattoos", resolved.featureName)
    }

    @Test
    fun `an unanswered sub-choice is reported as unanswered rather than silently dropped`() {
        val monk = character("monk", level = 3, subclassId = "tattooed_warrior")
        val resolved = ChoiceResolver.subclassFeatureChoices(monk)
            .first { it.choice.id == "beast_tattoos" }
        assertFalse(resolved.isAnswered)
        assertTrue(resolved.selectedNames.isEmpty())
    }

    @Test
    fun `later tattoo choices resolve at their own levels`() {
        val monk = character(
            classId = "monk",
            level = 11,
            subclassId = "tattooed_warrior",
            levelSelections = mapOf(
                "3:beast_tattoos" to listOf("horse", "tortoise"),
                "6:celestial_tattoo" to listOf("eclipse"),
                "11:nature_tattoo" to listOf("volcano"),
            ),
        )
        val byId = ChoiceResolver.subclassFeatureChoices(monk).associateBy { it.choice.id }

        assertEquals(listOf("Horse", "Tortoise"), byId["beast_tattoos"]?.selectedNames)
        assertEquals(listOf("Eclipse"), byId["celestial_tattoo"]?.selectedNames)
        assertEquals(listOf("Volcano"), byId["nature_tattoo"]?.selectedNames)
        // Level 17's tattoo isn't reached yet, so it isn't offered.
        assertEquals(null, byId["monster_tattoo"])
    }

    @Test
    fun `the same pattern resolves for other classes, not just the monk`() {
        val battleMaster = character(
            classId = "fighter",
            level = 3,
            subclassId = "battle_master",
            levelSelections = mapOf(
                "3:bm_maneuvers_3" to listOf("riposte", "trip_attack", "parry")
            ),
        )
        val maneuvers = ChoiceResolver.subclassFeatureChoices(battleMaster)
            .first { it.choice.id == "bm_maneuvers_3" }
        assertEquals(listOf("Riposte", "Trip Attack", "Parry"), maneuvers.selectedNames)

        val sorcerer = character(
            classId = "sorcerer",
            level = 2,
            levelSelections = mapOf("2:metamagic_2" to listOf("quickened", "subtle")),
        )
        val metamagic = ChoiceResolver.classFeatureChoices(sorcerer)
            .first { it.choice.id == "metamagic_2" }
        assertEquals(listOf("Quickened Spell", "Subtle Spell"), metamagic.selectedNames)

        val paladin = character(
            classId = "paladin",
            level = 2,
            levelSelections = mapOf("2:fighting_style" to listOf("dueling")),
        )
        val style = ChoiceResolver.classFeatureChoices(paladin)
            .first { it.choice.id == "fighting_style" }
        assertEquals(listOf("Dueling"), style.selectedNames)
    }

    @Test
    fun `expertise and skill choices resolve to readable skill names`() {
        val rogue = character(
            classId = "rogue",
            level = 1,
            levelSelections = mapOf("1:rogue_expertise_1" to listOf("STEALTH", "SLEIGHT_OF_HAND")),
        )
        val expertise = ChoiceResolver.classFeatureChoices(rogue)
            .first { it.choice.id == "rogue_expertise_1" }
        assertEquals(listOf("Stealth", "Sleight of Hand"), expertise.selectedNames)
    }

    @Test
    fun `origin choices resolve too, including magic initiate spells`() {
        val sage = PlayerCharacter(
            id = "t",
            name = "Test",
            speciesId = "human",
            classId = "fighter",
            backgroundId = "sage",
            baseAbilityScores = Ability.ALL.associate { it.name to 14 },
            originChoiceSelections = mapOf(
                "feat:magic_initiate_wizard:cantrips" to listOf("fire_bolt", "mage_hand"),
                "feat:magic_initiate_wizard:spell" to listOf("magic_missile"),
            ),
        )
        val resolved = ChoiceResolver.originChoices(sage).associateBy { it.choice.id }

        assertEquals(
            listOf("Fire Bolt", "Mage Hand"),
            resolved["feat:magic_initiate_wizard:cantrips"]?.selectedNames,
        )
        assertEquals(
            listOf("Magic Missile"),
            resolved["feat:magic_initiate_wizard:spell"]?.selectedNames,
        )
    }

    @Test
    fun `level one class branches still resolve`() {
        val fighter = character(
            classId = "fighter",
            level = 1,
            classChoiceSelections = mapOf("fighting_style" to listOf("archery")),
        )
        val options = ChoiceResolver.levelOneClassOptions(fighter)
        assertEquals(1, options.size)
        assertEquals("Fighting Style" to "Archery", options.first())
    }

    @Test
    fun `rest changeable choices are surfaced for the rest screens`() {
        val monk = character(
            classId = "monk",
            level = 6,
            subclassId = "tattooed_warrior",
            levelSelections = mapOf(
                "3:beast_tattoos" to listOf("bat", "crane"),
                "6:celestial_tattoo" to listOf("comet"),
            ),
        )
        val changeable = ChoiceResolver.restChangeable(monk).map { it.choice.id }
        assertTrue("tattoos are reshaped on a long rest", "beast_tattoos" in changeable)
        assertTrue("celestial_tattoo" in changeable)

        // A Fighting Style is permanent, so it must not appear.
        val paladin = character(
            classId = "paladin",
            level = 2,
            levelSelections = mapOf("2:fighting_style" to listOf("dueling")),
        )
        assertFalse(
            "fighting_style" in ChoiceResolver.restChangeable(paladin).map { it.choice.id }
        )
    }

    @Test
    fun `every choice any class can reach resolves without throwing`() {
        ClassData.ALL.forEach { charClass ->
            SubclassData.forClass(charClass.id).forEach { subclass ->
                val maxed = character(charClass.id, 20, subclass.id)
                val all = ChoiceResolver.all(maxed)
                all.forEach { resolved ->
                    assertNotNull(resolved.choice.label)
                    // Names resolve for every option the choice offers.
                    resolved.choice.options.forEach { option ->
                        val name = ChoiceResolver.nameFor(resolved.choice, option.id)
                        assertTrue(
                            "${resolved.choice.id}/${option.id} resolved to a blank name",
                            name.isNotBlank(),
                        )
                    }
                }
            }
        }
    }
}
