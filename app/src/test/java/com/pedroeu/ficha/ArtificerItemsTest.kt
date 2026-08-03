package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.EquipmentData
import com.pedroeu.ficha.data.content.MagicItemData
import com.pedroeu.ficha.data.content.ReplicaData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.ItemRarity
import com.pedroeu.ficha.domain.ArtificerItems
import com.pedroeu.ficha.domain.CharacterAttacks
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Replicate Magic Item: knowing a plan, and actually having the thing.
 *
 * The app recorded which plans an Artificer had learned and stopped there, which left the two
 * halves of the feature disconnected — the player had to remember what they had made and type
 * it into their own inventory. These tests cover the join: the daily limit from the class
 * table, the pick a category-shaped plan still owes, and the item arriving somewhere it can
 * actually be used.
 */
class ArtificerItemsTest {

    private fun artificer(
        level: Int,
        plans: List<String> = emptyList(),
        planLevel: Int = 2,
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = "artificer",
        backgroundId = "artisan",
        level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 } + ("DEX" to 16),
        levelSelections = if (plans.isEmpty()) emptyMap()
        else mapOf("$planLevel:replicate_plans_$planLevel" to plans),
    )

    // ------------------------------------------------------------- The Infused Items table

    @Test
    fun `the number of items you can have follows the class table`() {
        // 2 at level 2, then one more at 6, 10, 14, and 18.
        assertEquals(0, ArtificerItems.maxMadeItems(1))
        assertEquals(2, ArtificerItems.maxMadeItems(2))
        assertEquals(2, ArtificerItems.maxMadeItems(5))
        assertEquals(3, ArtificerItems.maxMadeItems(6))
        assertEquals(3, ArtificerItems.maxMadeItems(9))
        assertEquals(4, ArtificerItems.maxMadeItems(10))
        assertEquals(5, ArtificerItems.maxMadeItems(14))
        assertEquals(6, ArtificerItems.maxMadeItems(18))
        assertEquals(6, ArtificerItems.maxMadeItems(20))
    }

    @Test
    fun `a class without the feature has no allowance at all`() {
        val fighter = artificer(5).copy(classId = "fighter")
        assertFalse(ArtificerItems.hasFeature(fighter))
        assertEquals(0, ArtificerItems.allowance(fighter))
        assertTrue(ArtificerItems.knownPlans(fighter).isEmpty())
    }

    @Test
    fun `no other class ever has Replicate Magic Item`() {
        // A Monk was shown the Artificer's Replicate Magic Item because one screen rendered
        // the section without asking whether the character had it. The section refuses now,
        // and this is the sweep across every class rather than the one that was reported.
        com.pedroeu.ficha.data.content.ClassData.ALL
            .filterNot { it.id == "artificer" }
            .forEach { charClass ->
                val character = artificer(20).copy(
                    classId = charClass.id,
                    subclassId = null,
                    levelSelections = mapOf(
                        // Even carrying a stored selection from a mis-built character, which
                        // is the state a real sheet would be in after the bug.
                        "2:replicate_plans_2" to listOf("bag_of_holding"),
                    ),
                )

                assertFalse(
                    "${charClass.name} must not have Replicate Magic Item",
                    ArtificerItems.hasFeature(character),
                )
                assertEquals(0, ArtificerItems.allowance(character))
                assertTrue(
                    "${charClass.name} is being offered Artificer plans",
                    ArtificerItems.knownPlans(character).isEmpty(),
                )
            }
    }

    @Test
    fun `the allowance follows Artificer levels, not total levels`() {
        // A level 12 character with only 2 levels of Artificer still makes 2 items.
        val multiclass = artificer(12).copy(
            classLevels = listOf(
                com.pedroeu.ficha.domain.ClassLevel("fighter", 10),
                com.pedroeu.ficha.domain.ClassLevel("artificer", 2),
            )
        )
        assertEquals(2, ArtificerItems.allowance(multiclass))
    }

    // ------------------------------------------------------------- Known plans

    @Test
    fun `plans chosen at level up are the ones offered`() {
        val character = artificer(2, listOf("bag_of_holding", "weapon_plus_1"))
        val names = ArtificerItems.knownPlans(character).map { it.plan.name }

        assertEquals(listOf("Bag of Holding", "Weapon, +1"), names)
    }

    @Test
    fun `plans accumulate across levels rather than replacing each other`() {
        val character = artificer(10, listOf("bag_of_holding")).copy(
            levelSelections = mapOf(
                "2:replicate_plans_2" to listOf("bag_of_holding", "weapon_plus_1"),
                "6:replicate_plans_6" to listOf("dazzling_weapon"),
                "10:replicate_plans_10" to listOf("weapon_plus_2"),
            )
        )

        assertEquals(4, ArtificerItems.knownPlans(character).size)
    }

    @Test
    fun `a plan above your level is kept but not offered`() {
        // Weapon, +2 unlocks at 10; a level 2 character holding it shouldn't be able to make it.
        val character = artificer(2, listOf("weapon_plus_1", "weapon_plus_2"))
        val offered = ArtificerItems.knownPlans(character).map { it.plan.id }

        assertEquals(listOf("weapon_plus_1"), offered)
    }

    // ------------------------------------------------------------- Making an item

    @Test
    fun `making a plan puts the item in the inventory`() {
        val before = artificer(2, listOf("bag_of_holding"))
        val after = ArtificerItems.make(before, "bag_of_holding")

        val line = after.inventory.single()
        assertEquals("Bag of Holding", line.name)
        assertEquals("bag_of_holding", line.craftedFromPlanId)
        assertTrue("the item's own rules travel with it", line.notes.isNotBlank())
    }

    @Test
    fun `the day's limit is respected`() {
        var character = artificer(2, listOf("bag_of_holding", "alchemy_jug", "sending_stones"))
        assertEquals(2, ArtificerItems.allowance(character))

        character = ArtificerItems.make(character, "bag_of_holding")
        character = ArtificerItems.make(character, "alchemy_jug")
        assertEquals(0, ArtificerItems.remaining(character))

        character = ArtificerItems.make(character, "sending_stones")
        assertEquals(
            "a third item at level 2 must not be made",
            2,
            ArtificerItems.madeItems(character).size,
        )
    }

    @Test
    fun `setting an item aside frees its place and removes the inventory line`() {
        var character = artificer(2, listOf("bag_of_holding", "alchemy_jug"))
        character = ArtificerItems.make(character, "bag_of_holding")
        character = ArtificerItems.make(character, "alchemy_jug")

        character = ArtificerItems.unmake(character, "bag_of_holding")

        assertEquals(1, ArtificerItems.remaining(character))
        assertEquals(listOf("Alchemy Jug"), character.inventory.map { it.name })
    }

    @Test
    fun `setting an item aside leaves everything else in the pack alone`() {
        val bought = com.pedroeu.ficha.data.model.InventoryItem(name = "Rope, 50 ft", weightLb = 5.0)
        var character = artificer(2, listOf("bag_of_holding")).copy(inventory = listOf(bought))
        character = ArtificerItems.make(character, "bag_of_holding")
        character = ArtificerItems.unmake(character, "bag_of_holding")

        assertEquals(listOf(bought), character.inventory)
    }

    @Test
    fun `a plan can't be made twice`() {
        var character = artificer(6, listOf("bag_of_holding"))
        character = ArtificerItems.make(character, "bag_of_holding")
        character = ArtificerItems.make(character, "bag_of_holding")

        assertEquals(1, ArtificerItems.madeItems(character).size)
    }

    @Test
    fun `a plan you never learned can't be made`() {
        val character = ArtificerItems.make(artificer(2), "bag_of_holding")
        assertTrue(character.inventory.isEmpty())
    }

    // ------------------------------------------------------------- Plans that need a base

    @Test
    fun `a plan naming a category asks which item it applies to`() {
        val base = ReplicaData.baseChoiceFor(MagicItemData.byId("weapon_plus_1")!!)

        assertNotNull("Weapon, +1 is a +1 something", base)
        assertEquals(ReplicaData.BaseKind.WEAPON, base!!.kind)
        assertEquals(EquipmentData.WEAPONS.size, base.options.size)
    }

    @Test
    fun `a plan naming one item asks nothing`() {
        assertNull(ReplicaData.baseChoiceFor(MagicItemData.byId("bag_of_holding")!!))
        assertNull(ReplicaData.baseChoiceFor(MagicItemData.byId("alchemy_jug")!!))
    }

    @Test
    fun `Repeating Shot offers only weapons that fire ammunition`() {
        val base = ReplicaData.baseChoiceFor(MagicItemData.byId("repeating_shot")!!)!!
        val ids = base.options.map { it.id }

        assertTrue("a hand crossbow qualifies", "hand_crossbow" in ids)
        assertTrue("so does a firearm", "revolver" in ids)
        assertFalse("a longsword does not", "longsword" in ids)
        assertTrue(
            "every option must actually have the Ammunition property",
            ids.all { id ->
                EquipmentData.weaponById(id)!!.properties.any { it.startsWith("Ammunition") }
            },
        )
    }

    @Test
    fun `Returning Weapon offers only thrown weapons`() {
        val ids = ReplicaData.baseChoiceFor(MagicItemData.byId("returning_weapon")!!)!!
            .options.map { it.id }

        assertTrue("javelin" in ids)
        assertTrue("handaxe" in ids)
        assertFalse("longbow" in ids)
    }

    @Test
    fun `a plan for armor offers armor, and one for a shield offers a shield`() {
        val armor = ReplicaData.baseChoiceFor(MagicItemData.byId("armor_plus_1")!!)!!
        assertEquals(ReplicaData.BaseKind.ARMOR, armor.kind)
        assertFalse("a Shield is not body armor", armor.options.any { it.id == "shield" })
        assertTrue(armor.options.any { it.id == "plate" })

        val shield = ReplicaData.baseChoiceFor(MagicItemData.byId("shield_plus_1")!!)!!
        assertEquals(listOf("shield"), shield.options.map { it.id })
    }

    @Test
    fun `Adamantine Armor excludes Hide, as its own type line says`() {
        val ids = ReplicaData.eligibleArmor("Medium or Heavy, except Hide").map { it.id }

        assertFalse("hide" in ids)
        assertTrue("chain_shirt" in ids)
        assertTrue("plate" in ids)
        assertFalse("leather is Light armor", "leather" in ids)
    }

    @Test
    fun `a sword plan offers swords and nothing else`() {
        val ids = ReplicaData.eligibleWeapons("any Sword").map { it.id }.toSet()

        assertEquals(
            setOf("greatsword", "longsword", "rapier", "scimitar", "shortsword"),
            ids,
        )
    }

    @Test
    fun `every plan that needs a base has something to choose from`() {
        // A plan whose filter matched nothing would be a button that does nothing at all.
        MagicItemData.artificerPlans(20).forEach { plan ->
            val base = ReplicaData.baseChoiceFor(plan) ?: return@forEach
            assertTrue(
                "${plan.id} (${plan.kind}) asks for a base but offers no options",
                base.options.isNotEmpty(),
            )
        }
    }

    // ------------------------------------------------------------- Open-ended plans

    @Test
    fun `Any Common Magic Item offers the Common items and no potions`() {
        val options = ReplicaData.openEndedOptions(MagicItemData.byId("plan_any_common")!!)!!

        assertTrue(options.isNotEmpty())
        assertTrue(options.all { it.rarity == ItemRarity.COMMON })
        assertFalse(options.any { it.kind.startsWith("Potion") })
    }

    @Test
    fun `Any Rare Wondrous Item offers only Rare Wondrous Items`() {
        val options = ReplicaData.openEndedOptions(MagicItemData.byId("plan_any_rare_wondrous")!!)!!

        assertTrue(options.isNotEmpty())
        assertTrue(options.all { it.rarity == ItemRarity.RARE && it.kind == "Wondrous Item" })
    }

    @Test
    fun `an open-ended plan makes the item that was picked, under its own name`() {
        val chosen = ReplicaData
            .openEndedOptions(MagicItemData.byId("plan_any_rare_wondrous")!!)!!
            .first()

        val character = ArtificerItems.make(
            artificer(14, listOf("plan_any_rare_wondrous"), planLevel = 14),
            "plan_any_rare_wondrous",
            chosen.id,
        )

        val line = character.inventory.single()
        assertEquals(chosen.name, line.name)
        assertEquals(chosen.id, line.magicItemId)
        assertEquals("plan_any_rare_wondrous", line.craftedFromPlanId)
    }

    @Test
    fun `a base that the plan doesn't allow is refused`() {
        val character = ArtificerItems.make(
            artificer(2, listOf("repeating_shot")),
            "repeating_shot",
            "longsword",
        )
        assertTrue("a longsword fires no ammunition", character.inventory.isEmpty())
    }

    // ------------------------------------------------------------- Reaching the rest of the sheet

    @Test
    fun `a made weapon becomes an attack, with its bonus`() {
        // A Dagger, because the Artificer is proficient with Simple weapons only and a bonus
        // the character can't add is a poor test of whether the bonus is added.
        val character = ArtificerItems.make(
            artificer(2, listOf("weapon_plus_1")),
            "weapon_plus_1",
            "dagger",
        )

        val attack = CharacterAttacks.all(character).single { it.name.contains("Dagger") }
        assertEquals("Dagger, +1", attack.name)
        // Dex 16 is +3, a level 2 Artificer's proficiency is +2, and the weapon adds +1.
        assertEquals(6, attack.attackBonus)
        assertEquals("1d4 +4", attack.damage)
        assertTrue(
            "what the magic does belongs on the attack line",
            attack.explainedProperties.any { it.first == "Weapon, +1" },
        )
    }

    @Test
    fun `a plain weapon and a made one are two separate attacks`() {
        val plain = com.pedroeu.ficha.data.model.InventoryItem(
            name = "Dagger",
            weaponDefId = "dagger",
        )
        val character = ArtificerItems.make(
            artificer(2, listOf("weapon_plus_1")).copy(inventory = listOf(plain)),
            "weapon_plus_1",
            "dagger",
        )

        val daggers = CharacterAttacks.all(character).filter { it.name.contains("Dagger") }
        assertEquals(2, daggers.size)
        assertEquals(setOf("Dagger", "Dagger, +1"), daggers.map { it.name }.toSet())
    }

    @Test
    fun `a made shield raises Armor Class once equipped`() {
        val character = ArtificerItems.make(
            artificer(6, listOf("shield_plus_1"), planLevel = 6),
            "shield_plus_1",
            "shield",
        )
        val unequipped = CharacterCalculations.armorClass(character)

        val equipped = character.copy(
            inventory = character.inventory.map { it.copy(equipped = true) }
        )
        // A Shield is +2, and the plan makes it a +1 Shield.
        assertEquals(unequipped + 3, CharacterCalculations.armorClass(equipped))
    }

    @Test
    fun `an item made from a category plan is named after what it was made from`() {
        val character = ArtificerItems.make(
            artificer(2, listOf("weapon_plus_1")),
            "weapon_plus_1",
            "longbow",
        )
        assertEquals("Longbow, +1", character.inventory.single().name)
    }

    @Test
    fun `a plan with a name of its own keeps it and notes the base`() {
        val character = ArtificerItems.make(
            artificer(2, listOf("repeating_shot")),
            "repeating_shot",
            "hand_crossbow",
        )
        assertEquals("Repeating Shot (Hand Crossbow)", character.inventory.single().name)
    }

    // ------------------------------------------------------------- The bonus tables

    @Test
    fun `every item that claims an attack bonus says so in its own rules text`() {
        MagicItemData.ALL.filter { it.attackBonus != 0 }.forEach { magicItem ->
            assertTrue(
                "${magicItem.id} carries +${magicItem.attackBonus} but its text doesn't say so",
                magicItem.description.contains("+${magicItem.attackBonus} bonus to attack"),
            )
        }
    }

    @Test
    fun `every item that claims an AC bonus says so in its own rules text`() {
        MagicItemData.ALL.filter { it.acBonus != 0 }.forEach { magicItem ->
            assertTrue(
                "${magicItem.id} carries +${magicItem.acBonus} AC but its text doesn't say so",
                magicItem.description.contains("+${magicItem.acBonus} bonus to Armor Class"),
            )
        }
    }

    @Test
    fun `the plus-N weapons and armor all carry their number`() {
        // The obvious ones, spelled out, because a table of ids is easy to get wrong quietly.
        assertEquals(1, MagicItemData.byId("weapon_plus_1")!!.attackBonus)
        assertEquals(2, MagicItemData.byId("weapon_plus_2")!!.attackBonus)
        assertEquals(3, MagicItemData.byId("weapon_plus_3")!!.attackBonus)
        assertEquals(1, MagicItemData.byId("armor_plus_1")!!.acBonus)
        assertEquals(2, MagicItemData.byId("shield_plus_2")!!.acBonus)
        assertEquals(0, MagicItemData.byId("bag_of_holding")!!.attackBonus)
    }
}
