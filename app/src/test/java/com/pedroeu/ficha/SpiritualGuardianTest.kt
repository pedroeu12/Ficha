package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.PerUseChoiceData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SpellGrantData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Recharge
import com.pedroeu.ficha.data.model.SourceFiltering
import com.pedroeu.ficha.data.model.Sourcebook
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.PerUseChoices
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Path of the Ancestral Guardian, as Unearthed Arcana 2025: Updated Subclasses reprints
 * it — the article renames it the Path of the Spiritual Guardian and rebuilds three of its
 * four features, so this checks the new text rather than the Xanathar's one.
 *
 * The three things worth protecting: it hangs off Rage rather than inventing a pool, its
 * level 3 effect is chosen on every hit rather than once at creation, and Consult the Spirits
 * is the only limited use it has.
 */
class SpiritualGuardianTest {

    private fun barbarian(level: Int) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = "barbarian",
        subclassId = "spiritual_guardian",
        backgroundId = "soldier",
        level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 10 } + ("STR" to 18),
    )

    @Test
    fun `the subclass exists and is marked playtest`() {
        val subclass = SubclassData.byId("spiritual_guardian")
        assertNotNull(subclass)
        assertEquals("barbarian", subclass!!.classId)
        assertEquals(Sourcebook.UA_SUBCLASSES, subclass.book)
        assertTrue("it is playtest material, and must say so", subclass.isPlaytest)
        assertTrue(SubclassData.forClass("barbarian").any { it.id == "spiritual_guardian" })
    }

    @Test
    fun `its old name is findable, because that is what players will search for`() {
        val subclass = SubclassData.byId("spiritual_guardian")!!
        assertTrue(
            "someone looking for the Ancestral Guardian has to land on this",
            subclass.summary.contains("Ancestral Guardian"),
        )
    }

    @Test
    fun `it is only offered when its book is switched on`() {
        val withUa = SourceFiltering.available(
            SubclassData.forClass("barbarian"), setOf(Sourcebook.PHB, Sourcebook.UA_SUBCLASSES),
        )
        assertTrue(withUa.any { it.id == "spiritual_guardian" })

        val coreOnly = SourceFiltering.available(SubclassData.forClass("barbarian"), Sourcebook.CORE)
        assertTrue("a core-only Barbarian must not see it", coreOnly.none { it.id == "spiritual_guardian" })
    }

    @Test
    fun `every feature lands on the level the article gives it`() {
        val levels = SubclassData.byId("spiritual_guardian")!!.features.map { it.level to it.name }
        assertEquals(
            listOf(
                3 to "Spiritual Protectors",
                6 to "Spirit Shield",
                10 to "Consult the Spirits",
                14 to "Vengeful Spirits",
            ),
            levels,
        )
    }

    @Test
    fun `spiritual protectors is chosen on every hit, not once at creation`() {
        // Freezing it at creation would quietly delete two thirds of the feature.
        val choice = PerUseChoiceData.byId("spiritual_guardian:spiritual_protectors")
        assertNotNull(choice)
        assertEquals(
            listOf("distract", "protect", "strike"),
            choice!!.options.map { it.id },
        )
        assertEquals("it reads off the Rage tracker", "barbarian:rage", choice.resourceId)

        val creationChoices = SubclassData.byId("spiritual_guardian")!!.features.flatMap { it.choices }
        assertTrue("nothing here is answered at creation", creationChoices.isEmpty())
    }

    @Test
    fun `the hit effect is offered from level 3 and asked on the rage tracker`() {
        assertTrue(
            PerUseChoices.all(barbarian(3)).any {
                it.choice.id == "spiritual_guardian:spiritual_protectors"
            },
        )
        assertTrue(
            PerUseChoices.forResource(barbarian(3), "barbarian:rage")
                .any { it.choice.id == "spiritual_guardian:spiritual_protectors" },
        )
    }

    @Test
    fun `consult the spirits puts both its spells on the sheet at level 10`() {
        listOf("augury", "clairvoyance").forEach { id ->
            assertTrue("$id must be a real catalogue entry", SpellData.ALL.any { it.id == id })
        }
        assertTrue(SpellGrantData.sourceIds().contains("spiritual_guardian"))

        val early = CharacterSpells.all(barbarian(9)).map { it.id }
        assertTrue("nothing before level 10", early.none { it == "augury" || it == "clairvoyance" })

        val ten = CharacterSpells.all(barbarian(10)).map { it.id }
        assertTrue(ten.contains("augury"))
        assertTrue(ten.contains("clairvoyance"))
    }

    @Test
    fun `consult the spirits is the only pool it adds, and it comes back on a short rest`() {
        assertNull(
            "nothing to track before level 10",
            CharacterResources.definitions(barbarian(9))
                .firstOrNull { it.id.startsWith("spiritual_guardian:") },
        )

        val pools = CharacterResources.definitions(barbarian(10))
        val consult = pools.firstOrNull { it.id == "spiritual_guardian:consult_the_spirits" }
        assertNotNull("one casting, then it is spent", consult)
        assertEquals(1, consult!!.max)
        assertEquals(Recharge.SHORT_REST, consult.recharge)

        assertTrue("Rage stays the class's own pool", pools.any { it.id == "barbarian:rage" })
        assertEquals(
            "Spirit Shield and Vengeful Spirits are unlimited, so they add nothing",
            1,
            pools.count { it.id.startsWith("spiritual_guardian:") },
        )
    }
}
