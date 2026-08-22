package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SpellGrantData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.SourceFiltering
import com.pedroeu.ficha.data.model.Sourcebook
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Oathbreaker from Unearthed Arcana 2025: Updated Subclasses.
 *
 * It has to hang off the Paladin machinery already here — Channel Divinity pays for two of its
 * features, its oath spells arrive on the same schedule as every other oath's, and its
 * Frightened save reads the Paladin's own Charisma DC.
 */
class OathbreakerTest {

    private fun paladin(level: Int, charisma: Int = 18) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = "paladin",
        subclassId = "oathbreaker",
        backgroundId = "soldier",
        level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 10 } + ("CHA" to charisma),
    )

    @Test
    fun `the subclass exists and is marked playtest`() {
        val oathbreaker = SubclassData.byId("oathbreaker")
        assertNotNull(oathbreaker)
        assertEquals("paladin", oathbreaker!!.classId)
        assertEquals(Sourcebook.UA_SUBCLASSES, oathbreaker.book)
        assertTrue("it is playtest material, and must say so", oathbreaker.isPlaytest)
        assertTrue(SubclassData.forClass("paladin").any { it.id == "oathbreaker" })
    }

    @Test
    fun `it is only offered when its book is switched on`() {
        val withUa = SourceFiltering.available(
            SubclassData.forClass("paladin"), setOf(Sourcebook.PHB, Sourcebook.UA_SUBCLASSES),
        )
        assertTrue(withUa.any { it.id == "oathbreaker" })

        val coreOnly = SourceFiltering.available(SubclassData.forClass("paladin"), Sourcebook.CORE)
        assertTrue("a core-only Paladin must not see it", coreOnly.none { it.id == "oathbreaker" })
    }

    @Test
    fun `every feature lands on the level the article gives it`() {
        val levels = SubclassData.byId("oathbreaker")!!.features.map { it.level to it.name }
        assertTrue(levels.contains(3 to "Conjure Undead"))
        assertTrue(levels.contains(3 to "Dreadful Aspect"))
        assertTrue(levels.contains(3 to "Oathbreaker Spells"))
        assertTrue(levels.contains(7 to "Aura of Hate"))
        assertTrue(levels.contains(15 to "Supernatural Resistance"))
        assertTrue(levels.contains(20 to "Dread Lord"))
    }

    @Test
    fun `the oath spells arrive on the same schedule as every other oath`() {
        val grants = SpellGrantData.allGrantedSpellIds()
        listOf(
            "hellish_rebuke", "witch_bolt", "crown_of_madness", "darkness", "fear",
            "summon_undead", "blight", "phantasmal_killer", "contagion", "steel_wind_strike",
        ).forEach { id ->
            assertTrue("$id must be a real catalogue entry", SpellData.ALL.any { it.id == id })
            assertTrue("$id should be granted by the oath", grants.contains(id))
        }
        assertTrue("the oath is a grant source", SpellGrantData.sourceIds().contains("oathbreaker"))
    }

    @Test
    fun `a level 3 oathbreaker has its first two spells and not its later ones`() {
        val known = CharacterSpells.all(paladin(3)).map { it.id }
        assertTrue(known.contains("hellish_rebuke"))
        assertTrue(known.contains("witch_bolt"))
        assertTrue("Fear is a level 9 grant", !known.contains("fear"))

        val nine = CharacterSpells.all(paladin(9)).map { it.id }
        assertTrue(nine.contains("fear"))
        assertTrue(nine.contains("summon_undead"))
    }

    @Test
    fun `dreadful aspect uses the paladin's own save DC`() {
        val character = paladin(3, charisma = 18)
        assertEquals(
            "Charisma 18 and a +2 proficiency bonus",
            8 + 2 + 4,
            CharacterCalculations.spellSaveDc(character),
        )
    }

    @Test
    fun `channel divinity is the shared paladin pool, not a second one`() {
        val pools = CharacterResources.definitions(paladin(3)).map { it.id }
        assertTrue("Conjure Undead spends the class pool", pools.contains("paladin:channel_divinity"))
        assertTrue(
            "the subclass must not invent its own Channel Divinity",
            pools.none { it.startsWith("oathbreaker:channel") },
        )
    }

    @Test
    fun `dread lord is tracked from level 20 and says how it comes back`() {
        assertTrue(
            "nothing to track before level 20",
            CharacterResources.definitions(paladin(19)).none { it.id == "oathbreaker:dread_lord" },
        )
        val pool = CharacterResources.definitions(paladin(20))
            .firstOrNull { it.id == "oathbreaker:dread_lord" }
        assertNotNull("Dread Lord is a limited use", pool)
        assertEquals(1, pool!!.max)
        assertTrue(
            "the level 5 slot recharge has to be written down somewhere",
            pool.notes.contains("level 5 spell slot"),
        )
    }
}
