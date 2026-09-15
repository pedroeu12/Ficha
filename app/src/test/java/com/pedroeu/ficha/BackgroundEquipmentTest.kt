package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.ui.creation.CharacterBuilder
import com.pedroeu.ficha.ui.creation.CreationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Choose A or B: this equipment, or fifty gold pieces."
 *
 * Every 2024 background words its gear that way and the app only ever offered A — so a player
 * who wanted to buy their own kit started with ten items to delete by hand and the wrong purse.
 * The comparison against the books found it in every background at once, which is the shape of
 * a rule that was never implemented rather than a typo that slipped through.
 */
class BackgroundEquipmentTest {

    private fun state(coins: Boolean) = CreationState(
        speciesId = "human",
        classId = "fighter",
        backgroundId = "acolyte",
        name = "T",
        takeCoinsInstead = coins,
        directScores = Ability.ALL.associateWith { 14 },
    )

    @Test
    fun `every background the books ship offers coins instead`() {
        val silent = BackgroundData.ALL.filter { it.coinsInstead <= 0 }
        assertTrue(
            "these offer no alternative to their gear: ${silent.map { it.name }}",
            silent.isEmpty(),
        )
    }

    @Test
    fun `taking the gear is what it always was`() {
        val character = CharacterBuilder.build(state(coins = false))
        val acolyte = BackgroundData.byId("acolyte")!!

        acolyte.equipment.forEach { item ->
            assertTrue(
                "the $item the background grants is missing",
                character.inventory.any { it.name == item },
            )
        }
        assertTrue(
            "the background's own coins are missing",
            character.coins.gp >= acolyte.startingGold,
        )
    }

    @Test
    fun `taking the coins takes the whole package, gear and small change alike`() {
        val acolyte = BackgroundData.byId("acolyte")!!
        val withGear = CharacterBuilder.build(state(coins = false))
        val withCoins = CharacterBuilder.build(state(coins = true))

        acolyte.equipment.forEach { item ->
            assertFalse(
                "$item came along with the coins",
                withCoins.inventory.any { it.name == item },
            )
        }
        assertEquals(
            "the coins should replace the background's gear and its few gold, and nothing else",
            withGear.coins.gp - acolyte.startingGold + acolyte.coinsInstead,
            withCoins.coins.gp,
        )
    }

    /** The class's own kit is a separate grant; the rules do not trade it away. */
    @Test
    fun `the class's starting kit survives taking the coins`() {
        val withCoins = CharacterBuilder.build(state(coins = true))
        assertTrue(
            "a Fighter who bought their own gear has nothing to fight with",
            withCoins.inventory.any { it.weaponDefId != null || it.armorDefId != null },
        )
    }

    @Test
    fun `a character who took the coins still passes the audit`() {
        assertEquals(
            emptyList<String>(),
            SheetAudit.complaints("bought their own gear", CharacterBuilder.build(state(true))),
        )
    }
}
