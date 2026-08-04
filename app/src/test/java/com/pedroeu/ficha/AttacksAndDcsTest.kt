package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.SaveDcData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.AttackSource
import com.pedroeu.ficha.domain.CharacterAttacks
import com.pedroeu.ficha.domain.CharacterDcs
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Attacks that aren't weapons, and save DCs that aren't the class's spellcasting. Both were
 * missing: a Warlock's Eldritch Blast never reached the Attacks section, and a Monk with no
 * spellcasting had no DC at all for Stunning Strike.
 */
class AttacksAndDcsTest {

    private fun character(
        classId: String,
        level: Int,
        subclassId: String? = null,
        speciesId: String = "human",
        featIds: List<String> = emptyList(),
        knownSpells: List<KnownSpell> = emptyList(),
        scores: Map<String, Int> = emptyMap(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = speciesId,
        classId = classId,
        subclassId = subclassId,
        backgroundId = "soldier",
        level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 } + scores,
        featIds = featIds,
        knownSpells = knownSpells,
    )

    private fun cantrip(id: String, name: String, source: String = "") = KnownSpell(
        id = id, name = name, level = 0, school = "Evocation",
        description = "", source = source,
    )

    // ------------------------------------------------------------- Damage cantrips

    @Test
    fun `a damage cantrip shows up as an attack`() {
        val warlock = character(
            classId = "warlock",
            level = 1,
            knownSpells = listOf(cantrip("eldritch_blast", "Eldritch Blast")),
        )
        val blast = CharacterAttacks.all(warlock).find { it.name == "Eldritch Blast" }

        assertNotNull("Eldritch Blast belongs in the Attacks section", blast)
        assertEquals(AttackSource.CANTRIP, blast!!.source)
        assertEquals("Force", blast.damageType)
        assertEquals("1d10", blast.damage)
    }

    @Test
    fun `cantrip damage grows at the levels the rules say`() {
        listOf(1 to "1d10", 4 to "1d10", 5 to "2d10", 11 to "3d10", 17 to "4d10")
            .forEach { (level, expected) ->
                val warlock = character(
                    classId = "warlock",
                    level = level,
                    knownSpells = listOf(cantrip("eldritch_blast", "Eldritch Blast")),
                )
                val blast = CharacterAttacks.all(warlock).first { it.name == "Eldritch Blast" }
                assertEquals("at level $level", expected, blast.damage)
            }
    }

    @Test
    fun `a cantrip with no damage is not an attack`() {
        val wizard = character(
            classId = "wizard",
            level = 1,
            knownSpells = listOf(cantrip("prestidigitation", "Prestidigitation")),
        )
        assertFalse(
            CharacterAttacks.all(wizard).any { it.name == "Prestidigitation" },
        )
    }

    @Test
    fun `a save-based cantrip shows the DC rather than an attack roll`() {
        val cleric = character(
            classId = "cleric",
            level = 1,
            knownSpells = listOf(cantrip("sacred_flame", "Sacred Flame")),
            scores = mapOf("WIS" to 16),
        )
        val flame = CharacterAttacks.all(cleric).first { it.name == "Sacred Flame" }

        assertTrue("the notes should name the save", flame.notes.contains("Dexterity save"))
        assertTrue("and give a number", flame.notes.contains("DC 13"))
    }

    // ------------------------------------------------------------- Unarmed and features

    @Test
    fun `everyone has an unarmed strike`() {
        val fighter = character("fighter", level = 1)
        val unarmed = CharacterAttacks.all(fighter).find { it.name == "Unarmed Strike" }

        assertNotNull(unarmed)
        assertEquals(AttackSource.UNARMED, unarmed!!.source)
        assertTrue("a plain strike deals 1 plus the modifier", unarmed.damage.startsWith("1 "))
    }

    @Test
    fun `a monk's unarmed strike uses the martial arts die and grows`() {
        listOf(1 to "1d6", 5 to "1d8", 11 to "1d10", 17 to "1d12").forEach { (level, die) ->
            val monk = character("monk", level = level)
            val unarmed = CharacterAttacks.all(monk).first { it.name == "Unarmed Strike" }
            assertTrue("at level $level expected $die", unarmed.damage.startsWith(die))
        }
    }

    @Test
    fun `tavern brawler upgrades the unarmed strike for anyone`() {
        val brawler = character("fighter", level = 1, featIds = listOf("tavern_brawler"))
        val unarmed = CharacterAttacks.all(brawler).first { it.name == "Unarmed Strike" }

        assertTrue(unarmed.damage.startsWith("1d4"))
        assertTrue(unarmed.notes.contains("Tavern Brawler"))
    }

    @Test
    fun `a monk uses dexterity when it beats strength`() {
        val monk = character("monk", level = 5, scores = mapOf("DEX" to 18, "STR" to 10))
        val unarmed = CharacterAttacks.all(monk).first { it.name == "Unarmed Strike" }

        // +4 Dexterity plus a +3 proficiency bonus at level 5.
        assertEquals(7, unarmed.attackBonus)
    }

    @Test
    fun `a soulknife's psychic blades appear as an attack`() {
        val rogue = character("rogue", level = 5, subclassId = "soulknife")
        val blades = CharacterAttacks.all(rogue).find { it.name == "Psychic Blades" }

        assertNotNull(blades)
        assertEquals(AttackSource.FEATURE, blades!!.source)
        assertEquals("Psychic", blades.damageType)
    }

    // ------------------------------------------------------------- Save DCs

    @Test
    fun `a monk gets a save DC even with no spellcasting`() {
        val monk = character("monk", level = 5, scores = mapOf("WIS" to 16))
        val dcs = CharacterDcs.all(monk)

        assertTrue("a Monk has no spellcasting but still forces saves", dcs.isNotEmpty())
        val monkDc = dcs.first { it.label == "Monk" }
        assertEquals(Ability.WIS, monkDc.ability)
        // 8 + proficiency bonus 3 + Wisdom modifier 3.
        assertEquals(14, monkDc.dc)
    }

    @Test
    fun `a monk with magic initiate carries two different DCs at once`() {
        val monk = character(
            classId = "monk",
            level = 5,
            featIds = listOf("magic_initiate_wizard"),
            scores = mapOf("WIS" to 16, "INT" to 10),
        )
        val dcs = CharacterDcs.all(monk)

        assertTrue("both sources must be listed", dcs.size >= 2)
        val monkDc = dcs.first { it.label == "Monk" }
        val featDc = dcs.first { it.label.contains("Magic Initiate") }

        assertEquals(Ability.WIS, monkDc.ability)
        assertEquals(Ability.INT, featDc.ability)
        assertEquals("8 + 3 + 3", 14, monkDc.dc)
        assertEquals("8 + 3 + 0", 11, featDc.dc)
        assertTrue("the two must not be conflated", monkDc.dc != featDc.dc)
        assertTrue(CharacterDcs.hasMultiple(monk))
    }

    @Test
    fun `a spellcasting class leads with its own DC`() {
        val wizard = character("wizard", level = 5, scores = mapOf("INT" to 18))
        val primary = CharacterDcs.primary(wizard)

        assertNotNull(primary)
        assertEquals("Wizard", primary!!.label)
        assertTrue(primary.isPrimary)
        assertEquals(15, primary.dc)
    }

    @Test
    fun `a character with nothing that forces a save has no DCs`() {
        val fighter = character("fighter", level = 1, subclassId = null)
        // The Fighter entry covers maneuvers, so it is expected; nothing else should appear.
        assertTrue(CharacterDcs.all(fighter).all { it.label == "Fighter" })
    }

    @Test
    fun `every save DC key matches a real class, subclass, species, lineage, or feat`() {
        val known = ClassData.ALL.map { it.id }.toSet() +
            SubclassData.ALL.map { it.id }.toSet() +
            SpeciesData.ALL.map { it.id }.toSet() +
            SpeciesData.ALL.flatMap { s -> s.lineageOptions.map { it.id } }.toSet() +
            FeatData.ALL.map { it.id }.toSet()

        val unknown = SaveDcData.sourceIds() - known
        assertTrue("these DC keys match nothing in the rulebook data: $unknown", unknown.isEmpty())
    }

    // ------------------------------------------------------------- Artificer spell list

    @Test
    fun `the artificer has a real spell list to pick from`() {
        val cantrips = SpellData.forClass("artificer", 0)
        val level1 = SpellData.forClass("artificer", 1)

        assertTrue("cantrips must be offered", cantrips.isNotEmpty())
        assertTrue("level 1 spells must be offered", level1.isNotEmpty())
        assertTrue(cantrips.any { it.name == "Mage Hand" })
        assertTrue(level1.any { it.name == "Cure Wounds" })

        // Tinker's Magic hands the Artificer Mending outright. Putting it on the list too
        // would offer, as a choice, a cantrip they already have and can't decline.
        assertTrue(
            "Mending is granted by Tinker's Magic, not chosen from the list",
            cantrips.none { it.name == "Mending" },
        )
    }

    @Test
    fun `the artificer list covers every level it can cast`() {
        (0..5).forEach { level ->
            assertTrue(
                "the Artificer should have level $level spells to choose from",
                SpellData.forClass("artificer", level).isNotEmpty(),
            )
        }
    }

    @Test
    fun `every class that casts has spells to choose from`() {
        ClassData.ALL.filter { it.isSpellcaster }.forEach { charClass ->
            assertTrue(
                "${charClass.name} has no spells in the catalog, so its picker would be empty",
                SpellData.forClassUpTo(charClass.id, 5).isNotEmpty(),
            )
        }
    }

    // ------------------------------------------------------------- Daily preparation

    @Test
    fun `prepared casters are offered a reselect on a long rest`() {
        listOf("cleric", "artificer", "druid", "paladin", "ranger", "wizard").forEach { id ->
            assertTrue("$id prepares spells daily", CharacterSpells.preparesDaily(character(id, 5)))
        }
    }

    @Test
    fun `classes with fixed known spells are not offered a reselect`() {
        listOf("bard", "sorcerer", "warlock", "fighter", "monk").forEach { id ->
            assertFalse(
                "$id doesn't rebuild its list each day",
                CharacterSpells.preparesDaily(character(id, 5)),
            )
        }
    }

    @Test
    fun `only chosen leveled spells are up for preparation`() {
        val cleric = character(
            classId = "cleric",
            level = 3,
            subclassId = "life_domain",
            knownSpells = listOf(
                cantrip("sacred_flame", "Sacred Flame"),
                KnownSpell("shield_of_faith", "Shield of Faith", 1, "Abjuration", "", true),
            ),
        )
        val preparable = CharacterSpells.preparable(cleric)

        assertTrue("cantrips aren't prepared", preparable.none { it.level == 0 })
        assertTrue("the chosen spell is", preparable.any { it.name == "Shield of Faith" })
        assertTrue(
            "domain spells are always prepared, so they aren't part of the decision",
            preparable.none { it.name == "Bless" },
        )
    }
}
