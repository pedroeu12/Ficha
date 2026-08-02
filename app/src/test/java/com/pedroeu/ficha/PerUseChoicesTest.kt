package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.PerUseChoiceData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.ResourceData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.ClassLevel
import com.pedroeu.ficha.domain.PerUseChoices
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.RestEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Choices the rules make you take when you use a feature, not when you build the character.
 *
 * The two look alike in a data table and are nothing alike at the table. An Artillerist picks
 * the cannon's mode every time the cannon fires; an Aasimar picks which revelation to take on
 * every time they transform. Asking either question during character creation answers it
 * forever, which silently removes two thirds of the feature — and, worse, reads as though the
 * app knows something about the rules that isn't true.
 */
class PerUseChoicesTest {

    private fun character(
        classId: String,
        level: Int,
        subclassId: String? = null,
        speciesId: String = "human",
        classLevels: List<ClassLevel> = emptyList(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = speciesId,
        classId = classId,
        subclassId = subclassId,
        backgroundId = "soldier",
        level = level,
        classLevels = classLevels,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
    )

    // ------------------------------------------------------------- Nothing is asked up front

    @Test
    fun `no choice anywhere is worded as one you make each time you use it`() {
        // The guard for the whole class of bug: if a choice's own prompt says the player
        // chooses again each time, it does not belong in a flow that asks once.
        val perUseWording = listOf(
            "each time", "choose again", "you choose freely",
            "whenever you create", "whenever you use", "whenever you assume",
        )

        val offenders = everyChoice()
            .filter { choice ->
                perUseWording.any { choice.prompt.contains(it, ignoreCase = true) }
            }
            .map { "${it.id}: ${it.prompt}" }

        assertTrue(
            "these are asked once but the rules ask every time — they belong in " +
                "PerUseChoiceData: $offenders",
            offenders.isEmpty(),
        )
    }

    @Test
    fun `the Artillerist is never asked which cannon mode to take`() {
        val artillerist = character("artificer", 5, "artillerist")
        val ids = ChoiceResolver.all(artillerist).map { it.choice.id }

        assertFalse("cannon_activation" in ids)
        assertTrue(
            "no creation-time choice may mention the cannon's modes",
            ChoiceResolver.all(artillerist).none { it.choice.label == "Activate Cannon" },
        )
    }

    @Test
    fun `an Aasimar is never asked which revelation to take`() {
        val aasimar = SpeciesData.byId("aasimar")!!

        assertTrue(
            "Celestial Revelation is not a lineage — it is chosen each time you transform",
            aasimar.lineageOptions.isEmpty(),
        )
        assertNull(aasimar.lineageChoiceLabel)
        assertTrue(
            "the trait itself should still describe it",
            aasimar.traits.any { it.name == "Celestial Revelation" },
        )
    }

    @Test
    fun `a rest-changeable choice is still a different thing and still offered`() {
        // Not everything that can change is per-use. A Tattooed Warrior reshapes a tattoo on a
        // rest, and that choice must survive this change untouched.
        val monk = character("monk", 6, "tattooed_warrior")
        assertTrue(
            "rest-changeable choices are still offered when resting",
            ChoiceResolver.restChangeable(monk).isNotEmpty(),
        )
    }

    // ------------------------------------------------------------- Who has what

    @Test
    fun `the cannon's modes arrive with the cannon, not before`() {
        assertTrue(
            PerUseChoices.all(character("artificer", 2, "artillerist"))
                .none { it.choice.id == "artillerist:cannon_activation" }
        )
        assertTrue(
            PerUseChoices.all(character("artificer", 3, "artillerist"))
                .any { it.choice.id == "artillerist:cannon_activation" }
        )
        assertTrue(
            "another subclass must not inherit it",
            PerUseChoices.all(character("artificer", 5, "alchemist"))
                .none { it.choice.id == "artillerist:cannon_activation" }
        )
    }

    @Test
    fun `Celestial Revelation unlocks at level 3`() {
        assertTrue(
            PerUseChoices.all(character("fighter", 2, speciesId = "aasimar"))
                .none { it.choice.id == "aasimar:celestial_revelation" }
        )
        assertTrue(
            PerUseChoices.all(character("fighter", 3, speciesId = "aasimar"))
                .any { it.choice.id == "aasimar:celestial_revelation" }
        )
        assertTrue(
            "a human doesn't get it",
            PerUseChoices.all(character("fighter", 5)).isEmpty()
        )
    }

    @Test
    fun `a subclass feature follows the level in its own class`() {
        // Druid 3 / Rogue 5 reaches Starry Form; Druid 1 / Rogue 7 does not, even though both
        // are level 8 characters.
        val reaches = character(
            "druid", 8, "stars",
            classLevels = listOf(ClassLevel("druid", 3, "stars"), ClassLevel("rogue", 5)),
        )
        val doesNot = character(
            "druid", 8, "stars",
            classLevels = listOf(ClassLevel("druid", 1, "stars"), ClassLevel("rogue", 7)),
        )

        assertTrue(PerUseChoices.all(reaches).any { it.choice.id == "stars:starry_form" })
        assertTrue(PerUseChoices.all(doesNot).none { it.choice.id == "stars:starry_form" })
    }

    // ------------------------------------------------------------- Choosing

    @Test
    fun `choosing records what is happening now`() {
        val before = character("artificer", 3, "artillerist")
        assertTrue(PerUseChoices.all(before).single().selected == null)

        val after = PerUseChoices.choose(before, "artillerist:cannon_activation", "flamethrower")
        val active = PerUseChoices.all(after).single()

        assertEquals("Flamethrower", active.selected?.name)
        assertTrue(active.isSet)
    }

    @Test
    fun `choosing again simply replaces it, at no cost`() {
        var character = character("artificer", 3, "artillerist")
        character = PerUseChoices.choose(character, "artillerist:cannon_activation", "flamethrower")
        character = PerUseChoices.choose(character, "artillerist:cannon_activation", "protector")

        assertEquals("Protector", PerUseChoices.all(character).single().selected?.name)
        assertEquals(1, character.perUseChoices.size)
    }

    @Test
    fun `an option the feature doesn't have is refused`() {
        val character = PerUseChoices.choose(
            character("artificer", 3, "artillerist"),
            "artillerist:cannon_activation",
            "necrotic_shroud",
        )
        assertTrue(character.perUseChoices.isEmpty())
    }

    @Test
    fun `clearing puts it back to nothing in particular`() {
        var character = character("artificer", 3, "artillerist")
        character = PerUseChoices.choose(character, "artillerist:cannon_activation", "protector")
        character = PerUseChoices.clear(character, "artillerist:cannon_activation")

        assertNull(PerUseChoices.all(character).single().selected)
    }

    @Test
    fun `nothing chosen at the moment of use survives a rest`() {
        // Every one of these lasts a minute or an hour. Showing yesterday's answer as current
        // would be worse than showing none.
        var character = character("artificer", 3, "artillerist")
        character = PerUseChoices.choose(character, "artillerist:cannon_activation", "protector")

        assertTrue(RestEngine.longRest(character).character.perUseChoices.isEmpty())
        assertTrue(RestEngine.shortRest(character, emptyList()).character.perUseChoices.isEmpty())
    }

    @Test
    fun `the pool a feature draws on is the one its tracker asks about`() {
        val artillerist = PerUseChoices.choose(
            character("artificer", 3, "artillerist"),
            "artillerist:cannon_activation",
            "force_ballista",
        )

        val onTheTracker = PerUseChoices.forResource(artillerist, "artillerist:eldritch_cannon")
        assertEquals(1, onTheTracker.size)
        assertEquals("Force Ballista", onTheTracker.single().selected?.name)

        assertTrue(
            "an unrelated pool asks nothing",
            PerUseChoices.forResource(artillerist, "artificer:flash_of_genius").isEmpty(),
        )
    }

    // ------------------------------------------------------------- The table

    @Test
    fun `every subclass, species and pool the table names is real`() {
        val subclasses = SubclassData.ALL.map { it.id }.toSet()
        val species = SpeciesData.ALL.map { it.id }.toSet()

        PerUseChoiceData.sourceIds().forEach { (kind, id) ->
            when (kind) {
                "subclass" -> assertTrue("no subclass '$id'", id in subclasses)
                "species" -> assertTrue("no species '$id'", id in species)
                "resource" -> assertTrue("no pool '$id'", id in everyResourceId())
            }
        }
    }

    @Test
    fun `every per-use choice is a real choice with rules text behind it`() {
        PerUseChoiceData.ALL.forEach { choice ->
            assertTrue("${choice.id} has nothing to choose between", choice.options.size >= 2)
            assertTrue("${choice.id} needs a prompt", choice.prompt.isNotBlank())
            assertTrue("${choice.id} needs a label", choice.label.isNotBlank())
            choice.options.forEach { option ->
                assertTrue(
                    "${choice.id}/${option.id} is a name with no rule behind it",
                    option.description.length > 40,
                )
            }
            assertEquals(
                "${choice.id} has two options with the same id",
                choice.options.size,
                choice.options.map { it.id }.distinct().size,
            )
        }
    }

    @Test
    fun `no id is used twice`() {
        assertEquals(
            PerUseChoiceData.ALL.size,
            PerUseChoiceData.ALL.map { it.id }.distinct().size,
        )
    }

    @Test
    fun `every per-use prompt says that the choice is made again`() {
        // The prompt is the only thing telling the player this isn't permanent, which is the
        // misunderstanding the whole change exists to correct.
        PerUseChoiceData.ALL.forEach { choice ->
            assertTrue(
                "${choice.id}'s prompt doesn't say the choice repeats: '${choice.prompt}'",
                choice.prompt.contains("each time", ignoreCase = true) ||
                    choice.prompt.contains("again", ignoreCase = true),
            )
        }
    }

    @Test
    fun `the feature's own text mentions that you choose as you go`() {
        // A player reading the feature on the sheet should learn this without finding the
        // chips first.
        val featureTexts = SubclassData.ALL.flatMap { subclass ->
            subclass.features.map { it.name to it.description }
        } + SpeciesData.ALL.flatMap { species ->
            species.traits.map { it.name to it.description }
        }

        listOf("Eldritch Cannon", "Arcane Jolt", "Starry Form", "Celestial Revelation")
            .forEach { name ->
                val text = featureTexts.firstOrNull { it.first == name }?.second
                assertNotNull("$name is missing from the rules text", text)
                assertTrue(
                    "$name should say the option is chosen as you go: $text",
                    text!!.contains("each time", ignoreCase = true),
                )
            }
    }

    // ------------------------------------------------------------- Helpers

    /** Every choice the creation and level-up flows can put in front of the player. */
    private fun everyChoice(): List<Choice> =
        ProgressionData.ALL.flatMap { progression ->
            progression.features.flatMap { it.choices }
        } + SubclassData.ALL.flatMap { subclass ->
            subclass.features.flatMap { it.choices }
        }

    private fun everyResourceId(): Set<String> {
        val contexts = buildList {
            ProgressionData.ALL.forEach { progression ->
                SubclassData.forClass(progression.classId).forEach { subclass ->
                    add(context(progression.classId, subclassId = subclass.id))
                }
                add(context(progression.classId))
            }
            SpeciesData.ALL.forEach { species ->
                add(context("fighter", speciesId = species.id))
            }
        }

        return contexts.flatMap { ResourceData.forContext(it) }.map { it.id }.toSet()
    }

    private fun context(
        classId: String,
        subclassId: String? = null,
        speciesId: String = "",
    ) = ResourceData.Context(
        classId = classId,
        subclassId = subclassId,
        speciesId = speciesId,
        lineageId = null,
        featIds = emptyList(),
        level = 20,
        proficiencyBonus = 6,
        abilityModifiers = Ability.ALL.associateWith { 3 },
        characterLevel = 20,
    )
}
