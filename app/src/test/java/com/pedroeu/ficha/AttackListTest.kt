package com.pedroeu.ficha

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.domain.CharacterAttacks
import com.pedroeu.ficha.domain.CustomAttack
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The attacks on the sheet are one list the player owns.
 *
 * A weapon, a cantrip and something written by hand are the same kind of row: each can be
 * renamed, retyped, moved, and taken off. The one asymmetry is deletion — a derived line is
 * rebuilt from the inventory on the next read, so removing one means hiding it.
 */
class AttackListTest {

    private fun character(
        customAttacks: List<CustomAttack> = emptyList(),
        hidden: Set<String> = emptySet(),
        order: List<String> = emptyList(),
        overrides: Map<String, String> = emptyMap(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = "fighter",
        subclassId = "champion",
        backgroundId = "soldier",
        level = 5,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        inventory = listOf(
            InventoryItem(name = "Longsword", weaponDefId = "longsword", equipped = true),
        ),
        customAttacks = customAttacks,
        hiddenAttackIds = hidden,
        attackOrder = order,
        textOverrides = overrides,
    )

    private val written = CustomAttack(
        id = "attack:written",
        name = "Breath Weapon",
        damageDice = "2d6",
        damageType = "Fire",
        abilityName = "CON",
    )

    @Test
    fun `a written attack sits in the same list as the derived ones`() {
        val lines = CharacterAttacks.all(character(customAttacks = listOf(written)))
        assertTrue("the weapon is there", lines.any { it.name == "Longsword" })
        assertTrue("and so is the written one", lines.any { it.name == "Breath Weapon" })
    }

    @Test
    fun `every line has an id that survives a rebuild`() {
        val first = CharacterAttacks.all(character()).map { it.id }
        val again = CharacterAttacks.all(character()).map { it.id }
        assertEquals(first, again)
        assertTrue("ids must be unique to be edited against", first.size == first.toSet().size)
        assertTrue("nothing may be blank", first.none { it.isBlank() })
    }

    @Test
    fun `a written attack knows it can be deleted and a derived one does not`() {
        val lines = CharacterAttacks.all(character(customAttacks = listOf(written)))
        assertTrue(lines.first { it.name == "Breath Weapon" }.isCustom)
        assertFalse(lines.first { it.name == "Longsword" }.isCustom)
    }

    // ------------------------------------------------------------- Editing

    @Test
    fun `any field can be retyped, including a derived one`() {
        val longsword = CharacterAttacks.all(character()).first { it.name == "Longsword" }
        val edited = CharacterAttacks.all(
            character(
                overrides = mapOf(
                    "attack:${longsword.id}:name" to "Grandfather's Blade",
                    "attack:${longsword.id}:damage" to "1d8 + 6",
                    "attack:${longsword.id}:damageType" to "Radiant",
                    "attack:${longsword.id}:notes" to "Hums near Undead",
                    "attack:${longsword.id}:bonus" to "see notes",
                ),
            ),
        ).first { it.id == longsword.id }

        assertEquals("Grandfather's Blade", edited.name)
        assertEquals("1d8 + 6", edited.damage)
        assertEquals("Radiant", edited.damageType)
        assertEquals("Hums near Undead", edited.notes)
        assertEquals("a retyped bonus need not be a number", "see notes", edited.shownBonus)
    }

    @Test
    fun `an untouched line still shows its computed bonus`() {
        val longsword = CharacterAttacks.all(character()).first { it.name == "Longsword" }
        assertTrue("it should read as a modifier", longsword.shownBonus.startsWith("+"))
    }

    // ------------------------------------------------------------- Hiding

    @Test
    fun `a hidden line leaves the sheet`() {
        val longsword = CharacterAttacks.all(character()).first { it.name == "Longsword" }
        val after = CharacterAttacks.all(character(hidden = setOf(longsword.id)))
        assertTrue("it should be gone", after.none { it.id == longsword.id })
        assertTrue("and the rest stays", after.isNotEmpty())
    }

    // ------------------------------------------------------------- Ordering

    @Test
    fun `the player's order is what the sheet shows`() {
        val natural = CharacterAttacks.all(character(customAttacks = listOf(written)))
        assertTrue("this test needs at least two lines", natural.size >= 2)

        val reversed = natural.map { it.id }.reversed()
        val ordered = CharacterAttacks.all(
            character(customAttacks = listOf(written), order = reversed),
        )
        assertEquals(reversed, ordered.map { it.id })
    }

    @Test
    fun `a line missing from the order appears rather than vanishing`() {
        // A weapon bought after the order was set has no place in it.
        val all = CharacterAttacks.all(character(customAttacks = listOf(written)))
        val partial = listOf(all.last().id)
        val ordered = CharacterAttacks.all(
            character(customAttacks = listOf(written), order = partial),
        )
        assertEquals("nothing is lost", all.size, ordered.size)
        assertEquals("the named one leads", partial.first(), ordered.first().id)
    }

    // ------------------------------------------------------------- Everything is editable

    @Test
    fun `size is words on the sheet, and words can be retyped too`() {
        val plain = com.pedroeu.ficha.domain.CharacterCalculations.size(character())
        assertTrue(plain.isNotBlank())

        val retyped = com.pedroeu.ficha.domain.CharacterCalculations.size(
            character(overrides = mapOf("vitals:size" to "Gargantuan")),
        )
        assertEquals("Gargantuan", retyped)
    }
}
