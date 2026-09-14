package com.pedroeu.ficha

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterSummons
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.RestEngine
import com.pedroeu.ficha.rules.ActiveSummon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A summoned creature is edited and rested the way the character is.
 *
 * Its temporary hit points were stored and never settable, its maximum was fixed at what the
 * stat block worked out, and a Long Rest left last night's spirit standing on the table with
 * the concentration that held it up long gone.
 */
class SummonEditTest {

    private val wizard = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = "wizard",
        subclassId = "evoker", backgroundId = "sage", level = 5,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        activeSummons = listOf(
            ActiveSummon(
                instanceId = "spirit", statblockId = "bestial_spirit", sourceId = "summon_beast",
                name = "Bear", currentHp = 30, maxHp = 30, tempHp = 5, spellLevel = 2,
                concentration = true,
            ),
            ActiveSummon(
                instanceId = "defender", statblockId = "steel_defender", sourceId = "steel_defender",
                name = "Defender", currentHp = 12, maxHp = 20, tempHp = 4,
                concentration = false,
            ),
        ),
    )

    @Test
    fun `temporary hit points can be set and absorb damage first`() {
        val given = CharacterSummons.setHitPoints(wizard, "defender", 12, temp = 6)
        assertEquals(6, given.activeSummons.first { it.instanceId == "defender" }.tempHp)

        val hit = CharacterSummons.damage(given, "defender", 8)
        val defender = hit.activeSummons.first { it.instanceId == "defender" }
        assertEquals(0, defender.tempHp)
        assertEquals(10, defender.currentHp)
    }

    @Test
    fun `the maximum can be corrected and the current never exceeds it`() {
        val lowered = CharacterSummons.setMaxHitPoints(wizard, "spirit", 20)
        val spirit = lowered.activeSummons.first { it.instanceId == "spirit" }
        assertEquals(20, spirit.maxHp)
        assertEquals(20, spirit.currentHp)

        val raised = CharacterSummons.setMaxHitPoints(wizard, "spirit", 40)
        assertEquals(30, raised.activeSummons.first { it.instanceId == "spirit" }.currentHp)
        assertEquals(1, CharacterSummons.setMaxHitPoints(wizard, "spirit", 0).activeSummons.first().maxHp)
    }

    @Test
    fun `a long rest ends a concentration summon and keeps a companion`() {
        val rested = RestEngine.longRest(wizard).character
        val left = rested.activeSummons.map { it.instanceId }
        assertEquals(listOf("defender"), left)
        assertEquals(
            "a companion loses its temporary hit points overnight like the character does",
            0,
            rested.activeSummons.single().tempHp,
        )
        assertTrue("its own hit points are its own business", rested.activeSummons.single().currentHp == 12)
    }
}
