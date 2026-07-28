package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.ArmorCategory
import com.pedroeu.ficha.data.model.ArmorDef
import com.pedroeu.ficha.data.model.StartingKit
import com.pedroeu.ficha.data.model.WeaponDef

object EquipmentData {

    val ARMOR: List<ArmorDef> = listOf(
        ArmorDef("padded", "Padded Armor", ArmorCategory.LIGHT, 11, null, stealthDisadvantage = true),
        ArmorDef("leather", "Leather Armor", ArmorCategory.LIGHT, 11, null),
        ArmorDef("studded_leather", "Studded Leather Armor", ArmorCategory.LIGHT, 12, null),
        ArmorDef("hide", "Hide Armor", ArmorCategory.MEDIUM, 12, 2),
        ArmorDef("chain_shirt", "Chain Shirt", ArmorCategory.MEDIUM, 13, 2),
        ArmorDef("scale_mail", "Scale Mail", ArmorCategory.MEDIUM, 14, 2, stealthDisadvantage = true),
        ArmorDef("breastplate", "Breastplate", ArmorCategory.MEDIUM, 14, 2),
        ArmorDef("half_plate", "Half Plate Armor", ArmorCategory.MEDIUM, 15, 2, stealthDisadvantage = true),
        ArmorDef("ring_mail", "Ring Mail", ArmorCategory.HEAVY, 14, 0, stealthDisadvantage = true),
        ArmorDef("chain_mail", "Chain Mail", ArmorCategory.HEAVY, 16, 0, strRequirement = 13, stealthDisadvantage = true),
        ArmorDef("splint", "Splint Armor", ArmorCategory.HEAVY, 17, 0, strRequirement = 15, stealthDisadvantage = true),
        ArmorDef("plate", "Plate Armor", ArmorCategory.HEAVY, 18, 0, strRequirement = 15, stealthDisadvantage = true),
        ArmorDef("shield", "Shield", ArmorCategory.SHIELD, 2, null),
    )

    val WEAPONS: List<WeaponDef> = listOf(
        // Simple melee
        WeaponDef("club", "Club", "1d4", "Bludgeoning", listOf("Light")),
        WeaponDef("dagger", "Dagger", "1d4", "Piercing", listOf("Finesse", "Light", "Thrown"), usesDexOption = true),
        WeaponDef("greatclub", "Greatclub", "1d8", "Bludgeoning", listOf("Two-Handed")),
        WeaponDef("handaxe", "Handaxe", "1d6", "Slashing", listOf("Light", "Thrown")),
        WeaponDef("javelin", "Javelin", "1d6", "Piercing", listOf("Thrown")),
        WeaponDef("light_hammer", "Light Hammer", "1d4", "Bludgeoning", listOf("Light", "Thrown")),
        WeaponDef("mace", "Mace", "1d6", "Bludgeoning", emptyList()),
        WeaponDef("quarterstaff", "Quarterstaff", "1d6", "Bludgeoning", listOf("Versatile (1d8)")),
        WeaponDef("sickle", "Sickle", "1d4", "Slashing", listOf("Light")),
        WeaponDef("spear", "Spear", "1d6", "Piercing", listOf("Thrown", "Versatile (1d8)")),
        // Simple ranged
        WeaponDef("light_crossbow", "Light Crossbow", "1d8", "Piercing", listOf("Ammunition", "Loading", "Two-Handed"), isRanged = true, usesDexOption = true),
        WeaponDef("dart", "Dart", "1d4", "Piercing", listOf("Finesse", "Thrown"), isRanged = true, usesDexOption = true),
        WeaponDef("shortbow", "Shortbow", "1d6", "Piercing", listOf("Ammunition", "Two-Handed"), isRanged = true, usesDexOption = true),
        WeaponDef("sling", "Sling", "1d4", "Bludgeoning", listOf("Ammunition"), isRanged = true, usesDexOption = true),
        // Martial melee
        WeaponDef("battleaxe", "Battleaxe", "1d8", "Slashing", listOf("Versatile (1d10)")),
        WeaponDef("flail", "Flail", "1d8", "Bludgeoning", emptyList()),
        WeaponDef("glaive", "Glaive", "1d10", "Slashing", listOf("Heavy", "Reach", "Two-Handed")),
        WeaponDef("greataxe", "Greataxe", "1d12", "Slashing", listOf("Heavy", "Two-Handed")),
        WeaponDef("greatsword", "Greatsword", "2d6", "Slashing", listOf("Heavy", "Two-Handed")),
        WeaponDef("halberd", "Halberd", "1d10", "Slashing", listOf("Heavy", "Reach", "Two-Handed")),
        WeaponDef("longsword", "Longsword", "1d8", "Slashing", listOf("Versatile (1d10)")),
        WeaponDef("maul", "Maul", "2d6", "Bludgeoning", listOf("Heavy", "Two-Handed")),
        WeaponDef("morningstar", "Morningstar", "1d8", "Piercing", emptyList()),
        WeaponDef("rapier", "Rapier", "1d8", "Piercing", listOf("Finesse"), usesDexOption = true),
        WeaponDef("scimitar", "Scimitar", "1d6", "Slashing", listOf("Finesse", "Light"), usesDexOption = true),
        WeaponDef("shortsword", "Shortsword", "1d6", "Piercing", listOf("Finesse", "Light"), usesDexOption = true),
        WeaponDef("trident", "Trident", "1d8", "Piercing", listOf("Thrown", "Versatile (1d10)")),
        WeaponDef("warhammer", "Warhammer", "1d8", "Bludgeoning", listOf("Versatile (1d10)")),
        WeaponDef("war_pick", "War Pick", "1d8", "Piercing", emptyList()),
        // Martial ranged
        WeaponDef("hand_crossbow", "Hand Crossbow", "1d6", "Piercing", listOf("Ammunition", "Light", "Loading"), isRanged = true, usesDexOption = true),
        WeaponDef("heavy_crossbow", "Heavy Crossbow", "1d10", "Piercing", listOf("Ammunition", "Heavy", "Loading", "Two-Handed"), isRanged = true, usesDexOption = true),
        WeaponDef("longbow", "Longbow", "1d8", "Piercing", listOf("Ammunition", "Heavy", "Two-Handed"), isRanged = true, usesDexOption = true),
    )

    /** Default level-1 gear granted by each class, keyed by class id. */
    val STARTING_KITS: Map<String, StartingKit> = mapOf(
        "barbarian" to StartingKit(
            weaponIds = listOf("greataxe", "handaxe", "handaxe"),
            otherGear = listOf("Explorer's Pack", "Javelin (4)"),
            goldPieces = 15,
        ),
        "bard" to StartingKit(
            armorIds = listOf("leather"),
            weaponIds = listOf("rapier", "dagger"),
            otherGear = listOf("Entertainer's Pack", "Musical Instrument"),
            goldPieces = 19,
        ),
        "cleric" to StartingKit(
            armorIds = listOf("chain_shirt", "shield"),
            weaponIds = listOf("mace"),
            otherGear = listOf("Priest's Pack", "Holy Symbol"),
            goldPieces = 7,
        ),
        "druid" to StartingKit(
            armorIds = listOf("leather", "shield"),
            weaponIds = listOf("sickle"),
            otherGear = listOf("Explorer's Pack", "Druidic Focus (Quarterstaff)", "Herbalism Kit"),
            goldPieces = 9,
        ),
        "fighter" to StartingKit(
            armorIds = listOf("chain_mail"),
            weaponIds = listOf("greatsword", "handaxe", "handaxe"),
            otherGear = listOf("Dungeoneer's Pack", "Javelin (4)"),
            goldPieces = 4,
        ),
        "monk" to StartingKit(
            weaponIds = listOf("spear", "dagger", "dagger", "dagger", "dagger", "dagger"),
            otherGear = listOf("Explorer's Pack", "Artisan's Tools or Musical Instrument"),
            goldPieces = 11,
        ),
        "paladin" to StartingKit(
            armorIds = listOf("chain_mail", "shield"),
            weaponIds = listOf("longsword", "javelin", "javelin", "javelin", "javelin", "javelin", "javelin"),
            otherGear = listOf("Priest's Pack", "Holy Symbol"),
            goldPieces = 9,
        ),
        "ranger" to StartingKit(
            armorIds = listOf("studded_leather"),
            weaponIds = listOf("scimitar", "shortsword", "longbow"),
            otherGear = listOf("Druidic Focus (Sprig of Mistletoe)", "Explorer's Pack", "Quiver", "Arrows (20)"),
            goldPieces = 7,
        ),
        "rogue" to StartingKit(
            armorIds = listOf("leather"),
            weaponIds = listOf("dagger", "dagger", "shortsword", "shortbow"),
            otherGear = listOf("Burglar's Pack", "Thieves' Tools", "Quiver", "Arrows (20)"),
            goldPieces = 8,
        ),
        "sorcerer" to StartingKit(
            weaponIds = listOf("spear", "dagger", "dagger"),
            otherGear = listOf("Arcane Focus (Crystal)", "Dungeoneer's Pack"),
            goldPieces = 28,
        ),
        "warlock" to StartingKit(
            armorIds = listOf("leather"),
            weaponIds = listOf("sickle", "dagger", "dagger"),
            otherGear = listOf("Arcane Focus (Orb)", "Book (occult lore)", "Scholar's Pack"),
            goldPieces = 15,
        ),
        "wizard" to StartingKit(
            weaponIds = listOf("quarterstaff", "dagger", "dagger"),
            otherGear = listOf("Arcane Focus (Staff)", "Robe", "Scholar's Pack", "Spellbook"),
            goldPieces = 5,
        ),
    )

    fun armorById(id: String): ArmorDef? = ARMOR.find { it.id == id }
    fun weaponById(id: String): WeaponDef? = WEAPONS.find { it.id == id }
}
