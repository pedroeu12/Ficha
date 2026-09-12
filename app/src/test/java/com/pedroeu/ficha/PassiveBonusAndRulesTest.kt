package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.content.PassiveBonusData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.FeatCategory
import com.pedroeu.ficha.data.model.Sourcebook
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.rules.Effect
import com.pedroeu.ficha.rules.FormulaEval
import com.pedroeu.ficha.rules.RulesEngine
import com.pedroeu.ficha.rules.StatTarget
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Passive bonuses that only ever appeared as trait text, and rules the app had wrong.
 *
 * The pattern behind the Warforged bug is what matters: a flat numeric bonus described in a
 * feature has to reach the number, not sit in prose next to it.
 */
class PassiveBonusAndRulesTest {

    private fun character(
        speciesId: String = "human",
        classId: String = "fighter",
        level: Int = 1,
        lineageId: String? = null,
        featIds: List<String> = emptyList(),
        scores: Map<String, Int> = emptyMap(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = speciesId,
        lineageId = lineageId,
        classId = classId,
        backgroundId = "soldier",
        level = level,
        featIds = featIds,
        baseAbilityScores = Ability.ALL.associate { it.name to 10 } + scores,
    )

    // ------------------------------------------------------------- Passive bonuses

    @Test
    fun `a warforged's plating reaches the armor class`() {
        val warforged = character(speciesId = "warforged")
        val human = character(speciesId = "human")

        assertEquals(
            "Integrated Protection is a flat +1 to AC",
            CharacterCalculations.armorClass(human) + 1,
            CharacterCalculations.armorClass(warforged),
        )
    }

    @Test
    fun `the warforged bonus is named so the sheet can explain it`() {
        val warforged = character(speciesId = "warforged")
        val sources = RulesEngine.statSources(warforged, StatTarget.ARMOR_CLASS)

        assertEquals(1, sources.size)
        assertEquals("Integrated Protection", sources.first().effect.label)
        assertEquals(1, FormulaEval.eval(sources.first().effect.amount, warforged))
    }

    @Test
    fun `dwarven toughness and the tough feat both scale with level`() {
        val dwarf = character(speciesId = "dwarf", level = 5)
        val human = character(speciesId = "human", level = 5)

        assertEquals(
            "Dwarven Toughness is one hit point per level",
            CharacterCalculations.maxHitPoints(human) + 5,
            CharacterCalculations.maxHitPoints(dwarf),
        )

        val tough = character(level = 5, featIds = listOf("tough"))
        assertEquals(
            "Tough is two hit points per level",
            CharacterCalculations.maxHitPoints(human) + 10,
            CharacterCalculations.maxHitPoints(tough),
        )
    }

    @Test
    fun `a passive speed bonus reaches the speed`() {
        val plain = character()
        val courier = character(featIds = listOf("mark_of_passage"))

        assertEquals(
            "Courier's Speed is a flat +5 feet",
            CharacterCalculations.speed(plain) + 5,
            CharacterCalculations.speed(courier),
        )
    }

    @Test
    fun `a character with no passive bonuses is unaffected`() {
        assertTrue(RulesEngine.view<Effect.ModifyStat>(character()).isEmpty())
    }

    @Test
    fun `every passive bonus key matches a real species, lineage, feat, class, or subclass`() {
        val known = SpeciesData.ALL.map { it.id }.toSet() +
            SpeciesData.ALL.flatMap { s -> s.lineageOptions.map { it.id } }.toSet() +
            FeatData.ALL.map { it.id }.toSet() +
            ClassData.ALL.map { it.id }.toSet() +
            SubclassData.ALL.map { it.id }.toSet()

        val unknown = PassiveBonusData.sourceIds() - known
        assertTrue("these bonus keys match nothing in the data: $unknown", unknown.isEmpty())
    }

    // ------------------------------------------------------------- Rules corrections

    @Test
    fun `humans are offered an origin feat`() {
        val human = SpeciesData.byId("human")!!
        assertTrue("Versatile grants an Origin feat", human.grantsOriginFeat)

        val choices = OriginChoices.forSpecies("human")
        assertEquals(1, choices.size)
        assertEquals(ChoiceKind.FEAT, choices.first().kind)
        // Origin feats only: the Dragonmarks and Dark Gifts are declared in the same list,
        // because that is when they may be taken, but Versatile does not offer them.
        val originOnly = FeatData.ALL.count { it.category == FeatCategory.ORIGIN }
        assertEquals("it should draw from the Origin feats", originOnly, choices.first().options.size)
    }

    @Test
    fun `the origin feat a species grants respects the character's books`() {
        val core = OriginChoices.forSpecies("human", Sourcebook.CORE).first()
        assertTrue("core books alone still offer something", core.options.isNotEmpty())
        assertTrue(
            "a supplement feat was offered to a core-only character",
            core.options.all { FeatData.byId(it.id)!!.book in Sourcebook.CORE },
        )
        assertTrue(
            "opening more books should offer more",
            OriginChoices.forSpecies("human", Sourcebook.EVERYTHING).first().options.size >
                core.options.size,
        )
    }

    @Test
    fun `a species without the trait is offered no feat`() {
        assertTrue(OriginChoices.forSpecies("dwarf").isEmpty())
        assertTrue(OriginChoices.forSpecies(null).isEmpty())
    }

    @Test
    fun `goliath giant ancestry is usable proficiency bonus times, not once`() {
        // Every ancestry, not just Cloud's Jaunt, which was the only one scaling before.
        listOf("cloud", "fire", "frost", "hill", "stone", "storm").forEach { ancestry ->
            val goliath = character(speciesId = "goliath", level = 5, lineageId = ancestry)
            val pool = CharacterResources.definitions(goliath)
                .first { it.id == "goliath:giant_ancestry" }

            assertEquals(
                "$ancestry should have Proficiency Bonus uses",
                CharacterCalculations.proficiencyBonus(goliath),
                pool.max,
            )
        }
    }

    @Test
    fun `a dwarf can track stonecunning`() {
        val dwarf = character(speciesId = "dwarf", level = 5)
        val pool = CharacterResources.definitions(dwarf)
            .find { it.id == "dwarf:stonecunning" }

        assertNotNull("Stonecunning is a limited-use trait in the 2024 rules", pool)
        assertEquals(CharacterCalculations.proficiencyBonus(dwarf), pool!!.max)
    }

    @Test
    fun `the elf chooses keen senses from three skills`() {
        val elf = SpeciesData.byId("elf")!!
        assertEquals(1, elf.bonusSkillChoiceCount)
        assertEquals(3, elf.bonusSkillOptions.size)
        assertTrue(
            "the choice is restricted, not any skill",
            elf.bonusSkillOptions.map { it.displayName }
                .containsAll(listOf("Insight", "Perception", "Survival")),
        )
    }

    @Test
    fun `the human's traits match the current rules`() {
        val human = SpeciesData.byId("human")!!
        val names = human.traits.map { it.name }

        assertTrue(names.contains("Resourceful"))
        assertTrue(names.contains("Skillful"))
        assertTrue(names.contains("Versatile"))
    }
}
