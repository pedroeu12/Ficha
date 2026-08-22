package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.model.Ability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A spell's rules text and its mechanical fields have to agree.
 *
 * The sheet rolls attacks and prints save DCs off those fields, so a spell whose text says
 * "make a ranged spell attack" while `needsAttackRoll` is false is a spell the combat tab
 * cannot roll — and nothing would ever say so. Over a hundred entries were in that state.
 */
class SpellMechanicsTest {

    private val attackText = Regex("(melee|ranged) spell attack", RegexOption.IGNORE_CASE)
    private val saveText =
        Regex("(Strength|Dexterity|Constitution|Intelligence|Wisdom|Charisma) saving throw")
    private val abbreviations = mapOf(
        "Strength" to Ability.STR, "Dexterity" to Ability.DEX, "Constitution" to Ability.CON,
        "Intelligence" to Ability.INT, "Wisdom" to Ability.WIS, "Charisma" to Ability.CHA,
    )

    /**
     * Spells whose text names a save the spell does not force.
     *
     * Both grant Advantage on someone else's saving throws, which reads the same to a regex
     * and means the opposite. They are named rather than the check being loosened, because
     * loosening it is how the original hundred slipped through.
     */
    private val grantsAdvantageInstead = setOf("beacon_of_hope", "haste", "conjure_animals")

    @Test
    fun `a spell that says it is a spell attack can be rolled as one`() {
        val wrong = SpellData.ALL
            .filter { attackText.containsMatchIn(it.description) && !it.needsAttackRoll }
            .map { it.id }
        assertTrue("these say they are spell attacks but can't be rolled: $wrong", wrong.isEmpty())
    }

    @Test
    fun `a spell that forces a save records which ability`() {
        val wrong = SpellData.ALL
            .filterNot { it.id in grantsAdvantageInstead }
            .filter { saveText.containsMatchIn(it.description) && it.saveAbility == null }
            .map { it.id }
        assertTrue("these force a save with no ability recorded: $wrong", wrong.isEmpty())
    }

    @Test
    fun `the recorded save ability is the one the text names`() {
        SpellData.ALL
            .filterNot { it.id in grantsAdvantageInstead }
            .forEach { spell ->
                val named = saveText.find(spell.description)?.groupValues?.get(1) ?: return@forEach
                val expected = abbreviations.getValue(named)
                if (spell.saveAbility != null) {
                    assertEquals(
                        "${spell.id} names a $named save but records ${spell.saveAbility}",
                        expected,
                        spell.saveAbility,
                    )
                }
            }
    }

    @Test
    fun `damage dice come with a damage type, and vice versa`() {
        SpellData.ALL.forEach { spell ->
            if (spell.damage.isNotBlank()) {
                assertTrue(
                    "${spell.id} has dice but no damage type",
                    spell.damageType.isNotBlank(),
                )
                assertTrue(
                    "${spell.id}'s damage should look like dice: ${spell.damage}",
                    Regex("^\\d+d\\d+").containsMatchIn(spell.damage),
                )
            }
        }
    }

    /**
     * Spells that really do both: an attack roll on the target and a save for everyone near
     * it. Anything else with both is usually a 2014 entry that never got updated — that is
     * how Poison Spray and Grasping Vine were caught still carrying saves they lost in 2024.
     */
    private val attacksAndSaves = setOf("ice_knife", "bigbys_hand", "searing_orb")

    @Test
    fun `only the spells that really resolve both ways carry both`() {
        val both = SpellData.ALL
            .filter { it.needsAttackRoll && it.saveAbility != null }
            .map { it.id }
            .filterNot { it in attacksAndSaves }
        assertTrue("a spell resolves one way or the other: $both", both.isEmpty())
    }

    @Test
    fun `the catalogue actually carries this data, so the checks above mean something`() {
        assertTrue("damage dice", SpellData.ALL.count { it.damage.isNotBlank() } >= 110)
        assertTrue("save abilities", SpellData.ALL.count { it.saveAbility != null } >= 150)
        assertTrue("spell attacks", SpellData.ALL.count { it.needsAttackRoll } >= 30)
    }

    // ------------------------------------------------------------- 2014 numbers found

    @Test
    fun `spells the 2024 rules renumbered carry the new dice`() {
        // Each of these was still on its 2014 value, which is a wrong number on the sheet
        // rather than a wrong word in a description.
        fun spell(id: String) = SpellData.ALL.first { it.id == id }
        assertEquals("2d10", spell("ice_storm").damage)
        assertEquals("2d8", spell("phantasmal_force").damage)
        assertEquals("4d8", spell("wind_wall").damage)
        assertEquals("3d10", spell("conjure_animals").damage)
    }

    @Test
    fun `spells that changed how they resolve are not still resolving both ways`() {
        fun spell(id: String) = SpellData.ALL.first { it.id == id }
        // Poison Spray became a ranged spell attack; Inflict Wounds became a Constitution
        // save. Both were carrying their 2014 resolution alongside the 2024 one.
        assertTrue(spell("poison_spray").needsAttackRoll)
        assertEquals(null, spell("poison_spray").saveAbility)
        assertTrue(!spell("inflict_wounds").needsAttackRoll)
        assertEquals(Ability.CON, spell("inflict_wounds").saveAbility)
        assertEquals(null, spell("grasping_vine").saveAbility)
    }

    @Test
    fun `shillelagh is a d8 that grows, not a flat d8`() {
        val shillelagh = SpellData.ALL.first { it.id == "shillelagh" }
        assertEquals("1d8", shillelagh.damage)
        assertTrue("it steps up at 5, 11 and 17", shillelagh.scalesWithLevel)
    }
}
