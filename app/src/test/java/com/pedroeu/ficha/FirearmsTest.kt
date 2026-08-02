package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.EquipmentData
import com.pedroeu.ficha.data.content.MasteryData
import com.pedroeu.ficha.data.content.WeaponPropertyData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.domain.CharacterAttacks
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Firearms, and the properties that come with them.
 *
 * A firearm is not a special case in this app — it is a weapon like any other, which is the
 * point of these tests. If one is in your inventory it has to produce an attack line with the
 * right bonus, its mastery property has to resolve like a longsword's, and the properties
 * nobody has memorised (Burst Fire, Reload) have to arrive with their rules attached rather
 * than as bare words on a line.
 */
class FirearmsTest {

    /** The renaissance pair from the Player's Handbook and the DMG's modern and futuristic tables. */
    private val firearmIds = listOf(
        "pistol", "musket",
        "semiautomatic_pistol", "revolver", "hunting_rifle", "automatic_rifle", "shotgun",
        "laser_pistol", "laser_rifle", "antimatter_rifle",
    )

    private fun firearms() = firearmIds.map { id ->
        requireNotNull(EquipmentData.weaponById(id)) { "$id is missing from the catalog" }
    }

    private fun gunslinger(vararg weaponIds: String) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = "fighter",
        backgroundId = "soldier",
        level = 5,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 } + ("DEX" to 18),
        inventory = weaponIds.map { id ->
            InventoryItem(name = id, weaponDefId = id, equipped = true)
        },
    )

    // ------------------------------------------------------------- The catalog

    @Test
    fun `every firearm is in the weapon catalog`() {
        firearmIds.forEach { id ->
            assertNotNull("$id should be a weapon the picker can find", EquipmentData.weaponById(id))
        }
    }

    @Test
    fun `firearms carry damage, a range and a weight`() {
        firearms().forEach { weapon ->
            assertTrue("${weapon.id} needs damage dice", weapon.damageDice.matches(Regex("\\d+d\\d+")))
            assertTrue("${weapon.id} needs a damage type", weapon.damageType.isNotBlank())
            assertTrue("${weapon.id} needs a range", weapon.range.contains("/"))
            assertTrue("${weapon.id} needs a weight", weapon.weightLb > 0.0)
            assertTrue("${weapon.id} is a ranged weapon", weapon.isRanged)
            assertTrue("${weapon.id} should allow Dexterity", weapon.usesDexOption)
        }
    }

    @Test
    fun `the renaissance firearms are the only ones with a price`() {
        // The DMG says to treat modern and futuristic firearms as Rare and Very Rare magic
        // items rather than something with a shelf price, so only the Player's Handbook pair
        // costs gold.
        assertEquals(250.0, EquipmentData.weaponById("pistol")!!.costGp, 0.001)
        assertEquals(500.0, EquipmentData.weaponById("musket")!!.costGp, 0.001)

        firearms().filter { it.id !in setOf("pistol", "musket") }.forEach { weapon ->
            assertEquals("${weapon.id} is a magic item, not a purchase", 0.0, weapon.costGp, 0.001)
        }
    }

    @Test
    fun `every firearm names the ammunition it takes`() {
        firearms().forEach { weapon ->
            val ammunition = weapon.properties.single { it.startsWith("Ammunition") }
            assertTrue(
                "${weapon.id} should say what it fires, not just 'Ammunition'",
                ammunition.contains("Bullet") || ammunition.contains("Energy Cell"),
            )
        }
    }

    @Test
    fun `the futuristic firearms run on energy cells`() {
        listOf("laser_pistol", "laser_rifle", "antimatter_rifle").forEach { id ->
            val weapon = EquipmentData.weaponById(id)!!
            assertTrue(
                "$id should not take bullets",
                weapon.properties.any { it.contains("Energy Cell") },
            )
        }
    }

    @Test
    fun `modern and futuristic firearms reload rather than load`() {
        // Loading is the renaissance limitation — one shot per action. Reload is a magazine.
        firearms().filter { it.id in setOf("pistol", "musket") }.forEach { weapon ->
            assertTrue("${weapon.id} should have Loading", weapon.properties.contains("Loading"))
        }

        firearms().filter { it.id !in setOf("pistol", "musket") }.forEach { weapon ->
            val reload = weapon.properties.single { it.startsWith("Reload") }
            assertTrue(
                "${weapon.id} should say how many shots: '$reload'",
                Regex("Reload \\(\\d+ shots?\\)").matches(reload),
            )
            assertTrue(
                "${weapon.id} should not also have Loading",
                !weapon.properties.contains("Loading"),
            )
        }
    }

    // ------------------------------------------------------------- Weapon mastery

    @Test
    fun `every firearm has a mastery property that actually exists`() {
        firearms().forEach { weapon ->
            assertTrue("${weapon.id} needs a mastery property", weapon.mastery.isNotBlank())
            assertNotNull(
                "${weapon.id} claims mastery '${weapon.mastery}', which is not a real property",
                MasteryData.byName(weapon.mastery),
            )
        }
    }

    @Test
    fun `firearms can be chosen when a class picks its mastered weapons`() {
        val options = MasteryData.weaponOptions().map { it.id }.toSet()
        firearmIds.forEach { id ->
            assertTrue("$id should be selectable as a mastered weapon", options.contains(id))
        }
    }

    // ------------------------------------------------------------- The properties glossary

    @Test
    fun `the properties firearms introduce come with their rules`() {
        assertTrue(
            "Burst Fire has to say what it costs and what it does",
            WeaponPropertyData.describe("Burst Fire").contains("10 pieces"),
        )
        assertTrue(
            "Reload has to survive the shot count in brackets",
            WeaponPropertyData.describe("Reload (30 shots)").isNotBlank(),
        )
        assertTrue(
            "the ammunition kind in brackets must not hide the rule",
            WeaponPropertyData.describe("Ammunition (Energy Cell)").isNotBlank(),
        )
    }

    @Test
    fun `no weapon in the catalog carries a property with no explanation`() {
        val unexplained = EquipmentData.WEAPONS
            .flatMap { it.properties }
            .filter { WeaponPropertyData.describe(it).isBlank() }
            .distinct()

        assertTrue(
            "these properties are printed on attack lines with nothing behind them: $unexplained",
            unexplained.isEmpty(),
        )
    }

    @Test
    fun `the glossary has no entry for a property no weapon uses`() {
        val used = EquipmentData.WEAPONS.flatMap { it.properties }
        val unused = WeaponPropertyData.knownNames().filter { name ->
            used.none { it.startsWith(name, ignoreCase = true) }
        }

        assertTrue("dead glossary entries: $unused", unused.isEmpty())
    }

    // ------------------------------------------------------------- On the sheet

    @Test
    fun `a firearm in the inventory becomes an attack`() {
        val attacks = CharacterAttacks.all(gunslinger("revolver"))
        val revolver = attacks.single { it.name == "Revolver" }

        // Dex 18 is +4, a level 5 Fighter's proficiency is +3, and a Fighter has Martial
        // weapon proficiency, which is what firearms need.
        assertEquals(7, revolver.attackBonus)
        assertEquals("2d8 +4", revolver.damage)
        assertEquals("Piercing", revolver.damageType)
    }

    @Test
    fun `the attack line explains Burst Fire and Reload where they are used`() {
        val attacks = CharacterAttacks.all(gunslinger("automatic_rifle"))
        val rifle = attacks.single { it.name == "Automatic Rifle" }
        val explained = rifle.explainedProperties.toMap()

        assertTrue("Burst Fire should be readable at the attack", explained.containsKey("Burst Fire"))
        assertTrue(explained.containsKey("Reload (30 shots)"))
        assertTrue(explained.getValue("Reload (30 shots)").contains("reload"))
        assertTrue(
            "the bare property names stay on the line as well",
            rifle.notes.contains("Burst Fire"),
        )
    }

    @Test
    fun `every firearm produces a usable attack line`() {
        val attacks = CharacterAttacks.all(gunslinger(*firearmIds.toTypedArray()))

        firearms().forEach { weapon ->
            val line = attacks.single { it.name == weapon.name }
            assertEquals("${weapon.id} should use Dexterity", 7, line.attackBonus)
            assertEquals("${weapon.damageDice} +4", line.damage)
            assertTrue(
                "${weapon.id} should explain its properties",
                line.explainedProperties.isNotEmpty(),
            )
        }
    }
}
