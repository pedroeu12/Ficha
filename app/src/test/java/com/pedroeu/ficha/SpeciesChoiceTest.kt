package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a species asks, it asks on the sheet.
 *
 * A species trait had nowhere to put a question, so the ones worded "of your choice" were
 * squeezed through the one bonus-skill field on the species — which fits a trait granting a
 * skill and nothing else — or simply went unasked. A Reborn picked no skill and named no
 * resistance, a Warforged was short a tool, and five species that cast spells never asked
 * which ability casts them.
 */
class SpeciesChoiceTest {

    private fun character(speciesId: String, lineageId: String? = null) = PlayerCharacter(
        id = "t", name = "T", speciesId = speciesId, lineageId = lineageId,
        classId = "fighter", backgroundId = "soldier", level = 1,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
    )

    private fun asked(speciesId: String): List<String> =
        ChoiceResolver.originChoices(character(speciesId)).map { it.choice.id }

    // ------------------------------------------------------------------ What was never asked

    @Test
    fun `a reborn picks the skill their past life left them`() {
        assertTrue(asked("reborn").contains("species:reborn:past_life_skill"))
    }

    @Test
    fun `a reborn names the damage their undying body shrugs off`() {
        val choice = ChoiceResolver.originChoices(character("reborn"))
            .single { it.choice.id == "species:reborn:endurance_resistance" }
        assertEquals(
            listOf("Cold", "Necrotic", "Poison"),
            choice.choice.options.map { it.name },
        )
    }

    /** "You gain one skill proficiency and one tool proficiency of your choice." */
    @Test
    fun `a warforged picks a tool as well as a skill`() {
        assertTrue(asked("warforged").contains("species:warforged:design_tool"))
        assertEquals(1, SpeciesData.byId("warforged")!!.bonusSkillChoiceCount)
    }

    /**
     * Five species say "choose when you select this species" about the ability that casts
     * their magic, and none of them asked.
     */
    @Test
    fun `a species whose magic names an ability asks which one`() {
        listOf("khoravar", "faerie", "flamekin", "hexblood", "rimekin").forEach { id ->
            val choice = ChoiceResolver.originChoices(character(id))
                .singleOrNull { it.choice.id == "species:$id:casting_ability" }
            assertTrue("$id never asks which ability casts its magic", choice != null)
            assertEquals(ChoiceKind.ABILITY_SCORE, choice!!.choice.kind)
            assertEquals(
                listOf("Intelligence", "Wisdom", "Charisma"),
                choice.choice.options.map { it.name },
            )
        }
    }

    // ------------------------------------------------------------------ What was over-asked

    /**
     * A list the rules restrict was offered whole.
     *
     * The count was right and the list was not, which is the quieter half of the same fault:
     * the player is asked, and allowed to answer in a way the rules don't permit.
     */
    @Test
    fun `a species that names its skills offers only those`() {
        val changeling = SpeciesData.byId("changeling")!!
        assertEquals(2, changeling.bonusSkillChoiceCount)
        assertEquals(
            listOf("Deception", "Insight", "Intimidation", "Performance", "Persuasion"),
            changeling.bonusSkillOptions.map { it.displayName },
        )

        val shifter = SpeciesData.byId("shifter")!!
        assertEquals(
            listOf("Acrobatics", "Athletics", "Intimidation", "Survival"),
            shifter.bonusSkillOptions.map { it.displayName },
        )
    }

    // ------------------------------------------------------------------ The general shape

    /**
     * A species asks nothing it cannot be answered on, and asks nobody else's questions.
     */
    @Test
    fun `every question a trait raises belongs to its own species and can be answered`() {
        SpeciesData.ALL.forEach { species ->
            species.traits.flatMap { it.choices }.forEach { choice ->
                assertTrue(
                    "${species.name}'s '${choice.label}' has nothing to pick from",
                    choice.options.isNotEmpty(),
                )
                assertTrue(
                    "${species.name}'s '${choice.label}' wants ${choice.count} of " +
                        "${choice.options.size}",
                    choice.count <= choice.options.size,
                )
                assertTrue(
                    "${choice.id} does not name the species that asks it",
                    choice.id.startsWith("species:${species.id}:"),
                )
            }
        }
    }

    @Test
    fun `a species is never asked another species' question`() {
        val human = asked("human")
        assertTrue(
            "a human was asked something belonging to another species: $human",
            human.none { it.startsWith("species:") && !it.startsWith("species:human:") },
        )
    }
}
