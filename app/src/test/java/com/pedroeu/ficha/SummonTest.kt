package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.StatblockData
import com.pedroeu.ficha.data.content.SummonData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterSummons
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.rules.SummonPick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Summoning, as one system rather than a spell's worth of special cases.
 *
 * The two shapes the rules use are both here: a spell that names one creature and a spell that
 * offers a set. What makes this need an engine rather than a printed card is that almost none
 * of a summon's numbers are its own — a Steel Defender's Armor Class is its Artificer's
 * Intelligence, a spirit's Hit Points are the slot it was called with — so the same stat block
 * is a different creature depending on who summoned it and how.
 */
class SummonTest {

    private fun caster(
        classId: String = "wizard",
        level: Int = 9,
        subclassId: String? = null,
        spells: List<String> = emptyList(),
        scores: Map<String, Int> = Ability.ALL.associate { it.name to 16 },
    ) = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = classId, subclassId = subclassId,
        backgroundId = "soldier", level = level,
        baseAbilityScores = scores,
        knownSpells = spells.map {
            KnownSpell(id = it, name = it, level = 2, school = "Conjuration", description = "")
        },
    )

    // ------------------------------------------------------------------ What can be summoned

    @Test
    fun `a caster is offered only the summons they actually have`() {
        val withBeast = caster(spells = listOf("summon_beast"))
        val ids = CharacterSummons.available(withBeast).map { it.summons.summonId }
        assertTrue("summon_beast" in ids)
        assertFalse("a spell they don't have is offered", "summon_fey" in ids)

        val withNone = caster()
        assertTrue(CharacterSummons.available(withNone).isEmpty())
    }

    /** A feature can summon without a spell being involved. */
    @Test
    fun `a subclass feature offers its companion with no spell`() {
        val smith = caster("artificer", 9, "battle_smith")
        val available = CharacterSummons.available(smith)
        val defender = available.firstOrNull { it.summons.summonId == "steel_defender" }
        assertNotNull("the Battle Smith is not offered their Steel Defender", defender)
        assertFalse("a companion is not cast from a slot", defender!!.needsAChoice)
        assertEquals("artificer", defender.owningClassId)
    }

    // ------------------------------------------------------------------ Fixed versus chosen

    @Test
    fun `a summon that names one creature asks nothing`() {
        val steed = SummonData.forSpell("find_steed")!!
        assertTrue(steed.pick is SummonPick.Fixed)
        assertEquals(1, SummonData.statblockIdsOf(steed.pick).size)
    }

    @Test
    fun `a summon that offers a set asks which one`() {
        val beast = SummonData.forSpell("summon_beast")!!
        assertTrue(beast.pick is SummonPick.FromList)
        val options = StatblockData.idsMatching(SummonData.statblockIdsOf(beast.pick))
        assertEquals(3, options.size)
        assertTrue(options.all { it.name.startsWith("Bestial Spirit") })
    }

    // ------------------------------------------------------------------ Numbers that follow you

    /**
     * "Hit Points: 5 + 5 per spell level." The same spirit is a different creature at level 3
     * and at level 7, which is the reason a printed card cannot serve.
     */
    @Test
    fun `a spirit's hit points follow the slot it was called with`() {
        val wizard = caster(spells = listOf("summon_beast"))
        val atThree = CharacterSummons.summon(
            wizard, "bestial_spirit_land", "summon_beast", "Summon Beast", spellLevel = 3,
        ).activeSummons.single()
        val atSeven = CharacterSummons.summon(
            wizard, "bestial_spirit_land", "summon_beast", "Summon Beast", spellLevel = 7,
        ).activeSummons.single()

        assertEquals(5 + 5 * 3, atThree.maxHp)
        assertEquals(5 + 5 * 7, atSeven.maxHp)
    }

    /**
     * "Armor Class: 12 + your Intelligence modifier. Hit Points: 5 + five times your Artificer
     * level."
     */
    @Test
    fun `a companion's numbers come from its summoner`() {
        val smith = caster(
            "artificer", 10, "battle_smith",
            scores = Ability.ALL.associate { it.name to if (it == Ability.INT) 20 else 10 },
        )
        val defender = StatblockData.byId("steel_defender")!!
        assertEquals(12 + 5, defender.armorClassFor(smith, 0, "artificer"))
        assertEquals(5 + 5 * 10, defender.hitPointsFor(smith, 0, "artificer"))
    }

    /** The Vestige Companion, whose hit points count Warlock levels and not the character's. */
    @Test
    fun `a companion counts its own class's level`() {
        val multi = caster("warlock", 5, "vestige_patron").copy(
            classLevels = listOf(
                com.pedroeu.ficha.domain.ClassLevel("warlock", 5, "vestige_patron", true),
                com.pedroeu.ficha.domain.ClassLevel("fighter", 6, "champion"),
            ),
            level = 11,
        )
        val vestige = StatblockData.byId("vestige_celestial")!!
        assertEquals(
            "a Warlock 5 / Fighter 6 has a level 5 Warlock's vestige",
            4 + 4 * 5,
            vestige.hitPointsFor(multi, 0, "warlock"),
        )
    }

    // ------------------------------------------------------------------ Several at once

    /**
     * Animate Dead keeps every corpse it has raised, which is why one slot was never enough.
     */
    @Test
    fun `several creatures can be out at the same time`() {
        var pc = caster(spells = listOf("animate_dead"))
        repeat(3) {
            pc = CharacterSummons.summon(pc, "skeleton", "animate_dead", "Animate Dead")
        }
        pc = CharacterSummons.summon(pc, "zombie", "animate_dead", "Animate Dead")

        assertEquals(4, pc.activeSummons.size)
        assertEquals(
            "each needs its own name to be told apart",
            4,
            pc.activeSummons.map { it.name }.distinct().size,
        )
        assertEquals(
            "each needs its own instance",
            4,
            pc.activeSummons.map { it.instanceId }.distinct().size,
        )
    }

    @Test
    fun `one summon takes damage without touching the others`() {
        var pc = caster(spells = listOf("animate_dead"))
        repeat(2) { pc = CharacterSummons.summon(pc, "skeleton", "animate_dead", "Animate Dead") }
        val first = pc.activeSummons.first()

        pc = CharacterSummons.damage(pc, first.instanceId, 5)
        assertEquals(first.maxHp - 5, pc.activeSummons.first().currentHp)
        assertEquals(
            "the other skeleton was not touched",
            first.maxHp,
            pc.activeSummons.last().currentHp,
        )
    }

    @Test
    fun `damage lands on temporary hit points first and never below zero`() {
        var pc = caster(spells = listOf("summon_beast"))
        pc = CharacterSummons.summon(
            pc, "bestial_spirit_land", "summon_beast", "Summon Beast", spellLevel = 2,
        )
        val id = pc.activeSummons.single().instanceId
        pc = CharacterSummons.setHitPoints(pc, id, current = 15, temp = 5)
        pc = CharacterSummons.damage(pc, id, 8)

        val summon = pc.activeSummons.single()
        assertEquals(0, summon.tempHp)
        assertEquals(12, summon.currentHp)

        pc = CharacterSummons.damage(pc, id, 100)
        assertEquals(0, pc.activeSummons.single().currentHp)
        assertTrue(pc.activeSummons.single().isDown)
    }

    // ------------------------------------------------------------------ Ending

    @Test
    fun `a summon can be dismissed on its own`() {
        var pc = caster(spells = listOf("animate_dead"))
        repeat(3) { pc = CharacterSummons.summon(pc, "skeleton", "animate_dead", "Animate Dead") }
        val doomed = pc.activeSummons[1].instanceId

        pc = CharacterSummons.dismiss(pc, doomed)
        assertEquals(2, pc.activeSummons.size)
        assertTrue(pc.activeSummons.none { it.instanceId == doomed })
    }

    /** Losing concentration ends what concentration was holding up, and nothing else. */
    @Test
    fun `ending concentration ends only what it was holding`() {
        var pc = caster(spells = listOf("summon_beast", "animate_dead"))
        pc = CharacterSummons.summon(
            pc, "bestial_spirit_land", "summon_beast", "Summon Beast",
            spellLevel = 2, concentration = true,
        )
        pc = CharacterSummons.summon(pc, "skeleton", "animate_dead", "Animate Dead")

        pc = CharacterSummons.endConcentration(pc)
        assertEquals(1, pc.activeSummons.size)
        assertEquals("skeleton", pc.activeSummons.single().statblockId)
    }

    // ------------------------------------------------------------------ The catalogue holds

    @Test
    fun `every summon points at a stat block that exists`() {
        SummonData.all().forEach { summons ->
            val ids = SummonData.statblockIdsOf(summons.pick)
            assertTrue("${summons.label} names no creature", ids.isNotEmpty())
            ids.forEach { id ->
                assertNotNull(
                    "${summons.label} names a stat block that does not exist: $id",
                    StatblockData.byId(id),
                )
            }
        }
    }

    @Test
    fun `every stat block can be worked out for a real character`() {
        val pc = caster("wizard", 20)
        StatblockData.ALL.forEach { statblock ->
            assertTrue(
                "${statblock.name} has no Armor Class",
                statblock.armorClassFor(pc, 9, "wizard") > 0,
            )
            assertTrue(
                "${statblock.name} has no Hit Points",
                statblock.hitPointsFor(pc, 9, "wizard") > 0,
            )
            assertTrue("${statblock.name} has no name", statblock.name.isNotBlank())
        }
    }

    @Test
    fun `every feature-driven summon names an element the engine actually produces`() {
        val smith = caster("artificer", 9, "battle_smith")
        val warlock = caster("warlock", 5, "vestige_patron")
        val elementIds = (
            com.pedroeu.ficha.rules.RulesEngine.elementsFor(smith) +
                com.pedroeu.ficha.rules.RulesEngine.elementsFor(warlock)
            ).map { it.id }.toSet()

        SummonData.BY_FEATURE.keys.forEach { id ->
            assertTrue(
                "$id is not an element id the engine produces, so its summon is unreachable",
                id in elementIds,
            )
        }
    }

    // ------------------------------------------------------------------ A creature's own uses

    /**
     * A rationed trait belongs to the creature, not the summoner.
     *
     * Two Steel Defenders would each have their own three Repairs, and spending one must not
     * touch the other — the same separation their hit points have.
     */
    @Test
    fun `a summon spends its own uses`() {
        var pc = caster("artificer", 10, "battle_smith")
        repeat(2) {
            pc = CharacterSummons.summon(pc, "steel_defender", "steel_defender", "Steel Defender")
        }
        val first = pc.activeSummons.first().instanceId

        pc = CharacterSummons.spend(pc, first, "Repair (3/Day)", 1)
        assertEquals(1, pc.activeSummons.first().spent["Repair (3/Day)"])
        assertEquals(
            "the other defender's uses were touched",
            null,
            pc.activeSummons.last().spent["Repair (3/Day)"],
        )

        pc = CharacterSummons.spend(pc, first, "Repair (3/Day)", -1)
        assertEquals(0, pc.activeSummons.first().spent["Repair (3/Day)"])
        pc = CharacterSummons.spend(pc, first, "Repair (3/Day)", -1)
        assertEquals("a use count never goes negative", 0, pc.activeSummons.first().spent["Repair (3/Day)"])
    }

    /** The stat blocks that ration a trait say so in its name, which is what the sheet reads. */
    @Test
    fun `rationed traits are named so the sheet can count them`() {
        val perDay = Regex("""\((\d+)\s*/\s*Day\)""", RegexOption.IGNORE_CASE)
        val rationed = StatblockData.ALL.flatMap { it.actions }.filter {
            perDay.containsMatchIn(it.name)
        }
        assertTrue("no stat block rations anything, so the counter is unreachable",
            rationed.isNotEmpty())
        rationed.forEach {
            assertTrue(
                "${it.name} says it is rationed but the count cannot be read",
                perDay.find(it.name)!!.groupValues[1].toIntOrNull()?.let { n -> n > 0 } == true,
            )
        }
    }

    /**
     * A spell always has a level to be cast at, even with no slots left to spend.
     *
     * A spirit's Hit Points are "5 + 5 per spell level", so summoning at level 0 would put a
     * creature with five Hit Points on the table rather than ten.
     */
    @Test
    fun `a summoning spell is never cast at level zero`() {
        val noSlots = PlayerCharacter(
            id = "t", name = "T", speciesId = "human", classId = "fighter",
            backgroundId = "soldier", level = 1,
            baseAbilityScores = Ability.ALL.associate { it.name to 12 },
            knownSpells = listOf(
                KnownSpell(
                    id = "summon_beast", name = "Summon Beast", level = 2,
                    school = "Conjuration", description = "",
                )
            ),
        )
        val entry = CharacterSummons.available(noSlots)
            .single { it.summons.summonId == "summon_beast" }
        assertTrue("a known spell offers no level at all", entry.castableAt.isNotEmpty())
        assertEquals(2, entry.castableAt.first())
    }
}
