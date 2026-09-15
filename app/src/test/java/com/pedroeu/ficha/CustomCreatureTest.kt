package com.pedroeu.ficha

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterSummons
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.SummonEdits
import com.pedroeu.ficha.rules.ActionKind
import com.pedroeu.ficha.rules.CustomAction
import com.pedroeu.ficha.rules.CustomStatblock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Creatures the player wrote, and creatures the player changed.
 *
 * Two halves of one complaint: the sheet could show a summoned creature and could not let you
 * touch it, and it could only ever show creatures the books contain. A table that rules a wolf
 * is Large, gives a Steel Defender a shield, or plays the DM's own construct had to keep all of
 * it on paper beside the phone.
 */
class CustomCreatureTest {

    private val druid = SheetAudit.character("druid", 5, "moon")

    private val wolf = CustomStatblock(
        id = SummonEdits.newId(),
        name = "Grey",
        size = "Large",
        creatureType = "Beast",
        armorClass = 14,
        hitPoints = 26,
        speed = "40 ft.",
        abilityScores = mapOf("STR" to 16, "DEX" to 15),
        actions = listOf(
            CustomAction(
                name = "Bite",
                kind = "ACTION",
                description = "It bites.",
                toHit = "+5",
                damageDice = "2d6",
                damageType = "Piercing",
            )
        ),
        senses = "Darkvision 60 ft.",
        resistances = "Cold, Poison",
    )

    private fun withWolf(): PlayerCharacter = SummonEdits.writeCreature(druid, wolf)

    // ================================================================ Writing a creature

    @Test
    fun `a creature the player wrote can be summoned and read back`() {
        var character = withWolf()
        character = CharacterSummons.summon(character, wolf.id, "custom:creature", "Grey")

        val summon = character.activeSummons.single()
        val resolved = SummonEdits.resolve(character, summon)

        assertNotNull("the creature has no rules behind it", resolved)
        assertEquals("Grey", resolved!!.name)
        assertEquals("Large", resolved.size)
        assertEquals(26, summon.maxHp)
        assertEquals(26, summon.currentHp)
        assertEquals(14, resolved.armorClassFor(character, 0, null))
        assertEquals(listOf("Cold", "Poison"), resolved.resistances)
        assertEquals(3, resolved.modifier(Ability.STR))
    }

    @Test
    fun `a written creature's actions arrive with their numbers`() {
        val character = withWolf()
        val resolved = SummonEdits.baseOf(character, wolf.id)!!
        val bite = resolved.actions.single()

        assertEquals("Bite", bite.name)
        assertEquals(ActionKind.ACTION, bite.kind)
        assertEquals("2d6", bite.damageDice)
        assertEquals("Piercing", bite.damageType)
        assertNotNull("a written bonus should be a flat number", bite.toHit)
    }

    @Test
    fun `deleting a creature takes any of it off the table too`() {
        var character = withWolf()
        character = CharacterSummons.summon(character, wolf.id, "custom:creature", "Grey")
        assertEquals(1, character.activeSummons.size)

        character = SummonEdits.eraseCreature(character, wolf.id)

        assertTrue(character.activeSummons.isEmpty())
        assertNull(character.customStatblocks.find { it.id == wolf.id })
        assertEquals(emptyList<String>(), SheetAudit.complaints("after deleting a creature", character))
    }

    @Test
    fun `writing the same creature again replaces it`() {
        val character = SummonEdits.writeCreature(withWolf(), wolf.copy(name = "Blackfang"))

        assertEquals(1, character.customStatblocks.size)
        assertEquals("Blackfang", character.customStatblocks.single().name)
    }

    // ================================================================ Changing one on the table

    private fun summonedSpirit(): Pair<PlayerCharacter, String> {
        val character = CharacterSummons.summon(
            druid, "bestial_spirit_land", "summon_beast", "Summon Beast", spellLevel = 2,
        )
        return character to character.activeSummons.single().instanceId
    }

    @Test
    fun `a creature on the table can have its armor class corrected`() {
        var (character, id) = summonedSpirit()
        val before = SummonEdits.resolve(character, character.activeSummons.single())!!
            .armorClassFor(character, 2, null)

        character = SummonEdits.setField(character, id, "armorClass", "19")

        val after = SummonEdits.resolve(character, character.activeSummons.single())!!
            .armorClassFor(character, 2, null)
        assertEquals(19, after)
        assertTrue("the test proves nothing if it was already 19", before != 19)
    }

    @Test
    fun `an edit belongs to one creature, not to the kind`() {
        var character = CharacterSummons.summon(
            druid, "bestial_spirit_land", "summon_beast", "Summon Beast", spellLevel = 2, howMany = 2,
        )
        val (first, second) = character.activeSummons

        character = SummonEdits.setField(character, first.instanceId, "speed", "80 ft.")

        val live = character.activeSummons
        assertEquals("80 ft.", SummonEdits.resolve(character, live[0])!!.speed)
        assertTrue(
            "editing one wolf changed the other",
            SummonEdits.resolve(character, live[1])!!.speed != "80 ft.",
        )
        assertEquals(second.instanceId, live[1].instanceId)
    }

    @Test
    fun `every field of a creature can be changed`() {
        var (character, id) = summonedSpirit()
        val edits = mapOf(
            "size" to "Huge",
            "creatureType" to "Elemental",
            "speed" to "10 ft., fly 60 ft.",
            "senses" to "Truesight 120 ft.",
            "languages" to "Primordial",
            "resistances" to "Fire, Cold",
            "immunities" to "Poison",
            "conditionImmunities" to "Charmed",
            "ability:STR" to "22",
        )
        edits.forEach { (key, value) ->
            character = SummonEdits.setField(character, id, key, value)
        }

        val block = SummonEdits.resolve(character, character.activeSummons.single())!!
        assertEquals("Huge", block.size)
        assertEquals("Elemental", block.creatureType)
        assertEquals("10 ft., fly 60 ft.", block.speed)
        assertEquals("Truesight 120 ft.", block.senses)
        assertEquals("Primordial", block.languages)
        assertEquals(listOf("Fire", "Cold"), block.resistances)
        assertEquals(listOf("Poison"), block.immunities)
        assertEquals(listOf("Charmed"), block.conditionImmunities)
        assertEquals(22, block.abilityScores[Ability.STR])
    }

    @Test
    fun `an action can be rewritten, added and taken away`() {
        var (character, id) = summonedSpirit()
        val original = SummonEdits.resolve(character, character.activeSummons.single())!!
            .actions.first()

        character = SummonEdits.setField(
            character, id, "action:${original.name}:description", "It does something else.",
        )
        character = SummonEdits.addAction(
            character, id,
            CustomAction(name = "Howl", kind = "BONUS_ACTION", description = "Everyone hears."),
        )

        var block = SummonEdits.resolve(character, character.activeSummons.single())!!
        assertEquals(
            "It does something else.",
            block.actions.first { it.name == original.name }.description,
        )
        assertTrue(block.actions.any { it.name == "Howl" })

        character = SummonEdits.removeAction(character, id, original.name)
        block = SummonEdits.resolve(character, character.activeSummons.single())!!
        assertTrue("the book's action came back", block.actions.none { it.name == original.name })
        assertTrue("the written one went with it", block.actions.any { it.name == "Howl" })
    }

    @Test
    fun `putting a creature back the way the book has it undoes everything`() {
        var (character, id) = summonedSpirit()
        val before = SummonEdits.resolve(character, character.activeSummons.single())!!

        character = SummonEdits.setField(character, id, "speed", "80 ft.")
        character = SummonEdits.addAction(character, id, CustomAction(name = "Howl"))
        character = SummonEdits.removeAction(character, id, before.actions.first().name)
        character = SummonEdits.resetEdits(character, id)

        val after = SummonEdits.resolve(character, character.activeSummons.single())!!
        assertEquals(before, after)
    }

    @Test
    fun `a blank value puts the book's answer back`() {
        var (character, id) = summonedSpirit()
        val before = SummonEdits.resolve(character, character.activeSummons.single())!!.speed

        character = SummonEdits.setField(character, id, "speed", "80 ft.")
        character = SummonEdits.setField(character, id, "speed", "")

        assertEquals(before, SummonEdits.resolve(character, character.activeSummons.single())!!.speed)
    }

    @Test
    fun `an edited creature still passes the audit`() {
        var (character, id) = summonedSpirit()
        character = SummonEdits.setField(character, id, "armorClass", "19")
        character = SummonEdits.addAction(character, id, CustomAction(name = "Howl"))
        character = SummonEdits.writeCreature(character, wolf)
        character = CharacterSummons.summon(character, wolf.id, "custom:creature", "Grey")

        assertEquals(emptyList<String>(), SheetAudit.complaints("an edited menagerie", character))
    }
}
