package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.ArmorCategory
import com.pedroeu.ficha.data.model.ArmorDef
import com.pedroeu.ficha.data.model.GearDef
import com.pedroeu.ficha.data.model.StartingKit
import com.pedroeu.ficha.data.model.WeaponDef

object EquipmentData {

    val ARMOR: List<ArmorDef> = listOf(
        ArmorDef("padded", "Padded Armor", ArmorCategory.LIGHT, 11, null, stealthDisadvantage = true,
            costGp = 5.0, weightLb = 8.0,
            description = "Quilted layers of cloth and batting. Light and cheap, but it rustles enough to give you away."),
        ArmorDef("leather", "Leather Armor", ArmorCategory.LIGHT, 11, null,
            costGp = 10.0, weightLb = 10.0,
            description = "Boiled and stiffened leather over softer hide. The standard choice for anyone who values quiet movement."),
        ArmorDef("studded_leather", "Studded Leather Armor", ArmorCategory.LIGHT, 12, null,
            costGp = 45.0, weightLb = 13.0,
            description = "Tough leather reinforced with close-set rivets. The best protection you can wear without slowing down."),
        ArmorDef("hide", "Hide Armor", ArmorCategory.MEDIUM, 12, 2,
            costGp = 10.0, weightLb = 12.0,
            description = "Thick furs and pelts, common among tribes and barbarians who live far from a smith."),
        ArmorDef("chain_shirt", "Chain Shirt", ArmorCategory.MEDIUM, 13, 2,
            costGp = 50.0, weightLb = 20.0,
            description = "Interlocking metal rings worn under clothing, concealing that you are armored at all."),
        ArmorDef("scale_mail", "Scale Mail", ArmorCategory.MEDIUM, 14, 2, stealthDisadvantage = true,
            costGp = 50.0, weightLb = 45.0,
            description = "Overlapping metal scales sewn to a leather coat, with gauntlets and leggings to match."),
        ArmorDef("breastplate", "Breastplate", ArmorCategory.MEDIUM, 14, 2,
            costGp = 400.0, weightLb = 20.0,
            description = "A fitted steel chest piece worn over leather. Leaves the limbs free while guarding the vitals."),
        ArmorDef("half_plate", "Half Plate Armor", ArmorCategory.MEDIUM, 15, 2, stealthDisadvantage = true,
            costGp = 750.0, weightLb = 40.0,
            description = "Shaped plates covering most of the body, short of the full harness. Buckles and straps make it noisy."),
        ArmorDef("ring_mail", "Ring Mail", ArmorCategory.HEAVY, 14, 0, stealthDisadvantage = true,
            costGp = 30.0, weightLb = 40.0,
            description = "Heavy leather studded with sewn-on rings. Inferior to chain mail, and usually worn by those who can't afford better."),
        ArmorDef("chain_mail", "Chain Mail", ArmorCategory.HEAVY, 16, 0, strRequirement = 13, stealthDisadvantage = true,
            costGp = 75.0, weightLb = 55.0,
            description = "A full suit of interlocking rings over quilted padding, with gauntlets and a coif. Requires Strength 13."),
        ArmorDef("splint", "Splint Armor", ArmorCategory.HEAVY, 17, 0, strRequirement = 15, stealthDisadvantage = true,
            costGp = 200.0, weightLb = 60.0,
            description = "Vertical strips of metal riveted to a backing of leather and chain. Requires Strength 15."),
        ArmorDef("plate", "Plate Armor", ArmorCategory.HEAVY, 18, 0, strRequirement = 15, stealthDisadvantage = true,
            costGp = 1500.0, weightLb = 65.0,
            description = "Shaped, interlocking plates covering the entire body, custom-fitted to the wearer. Requires Strength 15."),
        ArmorDef("shield", "Shield", ArmorCategory.SHIELD, 2, null,
            costGp = 10.0, weightLb = 6.0,
            description = "A board of wood or metal carried in one hand, granting +2 AC. You can benefit from only one at a time."),
    )

    val WEAPONS: List<WeaponDef> = listOf(
        // ---- Simple melee
        WeaponDef("club", "Club", "1d4", "Bludgeoning", listOf("Light"), mastery = "Slow",
            costGp = 0.1, weightLb = 2.0,
            description = "A simple length of hard wood. Cheap, legal almost everywhere, and effective enough in a brawl."),
        WeaponDef("dagger", "Dagger", "1d4", "Piercing", listOf("Finesse", "Light", "Thrown (20/60)"),
            usesDexOption = true, mastery = "Nick", costGp = 2.0, weightLb = 1.0, range = "20/60 ft",
            description = "A short blade that can be thrown or concealed. The most common sidearm in the world."),
        WeaponDef("greatclub", "Greatclub", "1d8", "Bludgeoning", listOf("Two-Handed"), mastery = "Push",
            costGp = 0.2, weightLb = 10.0,
            description = "A heavy, roughly shaped log swung with both hands."),
        WeaponDef("handaxe", "Handaxe", "1d6", "Slashing", listOf("Light", "Thrown (20/60)"),
            mastery = "Vex", costGp = 5.0, weightLb = 2.0, range = "20/60 ft",
            description = "A single-handed axe balanced for both chopping and throwing."),
        WeaponDef("javelin", "Javelin", "1d6", "Piercing", listOf("Thrown (30/120)"),
            mastery = "Slow", costGp = 0.5, weightLb = 2.0, range = "30/120 ft",
            description = "A light throwing spear with a long iron head."),
        WeaponDef("light_hammer", "Light Hammer", "1d4", "Bludgeoning", listOf("Light", "Thrown (20/60)"),
            mastery = "Nick", costGp = 2.0, weightLb = 2.0, range = "20/60 ft",
            description = "A compact hammer, as at home on a workbench as in a fight."),
        WeaponDef("mace", "Mace", "1d6", "Bludgeoning", emptyList(), mastery = "Sap",
            costGp = 5.0, weightLb = 4.0,
            description = "A flanged metal head on a short haft, made to defeat armor that a blade would skid off."),
        WeaponDef("quarterstaff", "Quarterstaff", "1d6", "Bludgeoning", listOf("Versatile (1d8)"),
            mastery = "Topple", costGp = 0.2, weightLb = 4.0,
            description = "A stout wooden pole. Unassuming, and a favorite of travelers and spellcasters alike."),
        WeaponDef("sickle", "Sickle", "1d4", "Slashing", listOf("Light"), mastery = "Nick",
            costGp = 1.0, weightLb = 2.0,
            description = "A curved harvesting blade, pressed into service as a weapon."),
        WeaponDef("spear", "Spear", "1d6", "Piercing", listOf("Thrown (20/60)", "Versatile (1d8)"),
            mastery = "Sap", costGp = 1.0, weightLb = 3.0, range = "20/60 ft",
            description = "A wooden shaft tipped with iron. The oldest and most widespread weapon there is."),
        // ---- Simple ranged
        WeaponDef("light_crossbow", "Light Crossbow", "1d8", "Piercing",
            listOf("Ammunition", "Loading", "Two-Handed"), isRanged = true, usesDexOption = true,
            mastery = "Slow", costGp = 25.0, weightLb = 5.0, range = "80/320 ft",
            description = "A shoulder-braced crossbow that trades rate of fire for power anyone can use."),
        WeaponDef("dart", "Dart", "1d4", "Piercing", listOf("Finesse", "Thrown (20/60)"),
            isRanged = true, usesDexOption = true, mastery = "Vex",
            costGp = 0.05, weightLb = 0.25, range = "20/60 ft",
            description = "A weighted throwing spike, easy to carry by the handful."),
        WeaponDef("shortbow", "Shortbow", "1d6", "Piercing", listOf("Ammunition", "Two-Handed"),
            isRanged = true, usesDexOption = true, mastery = "Vex",
            costGp = 25.0, weightLb = 2.0, range = "80/320 ft",
            description = "A compact bow that can be drawn from horseback or in close quarters."),
        WeaponDef("sling", "Sling", "1d4", "Bludgeoning", listOf("Ammunition"),
            isRanged = true, usesDexOption = true, mastery = "Slow",
            costGp = 0.1, weightLb = 0.0, range = "30/120 ft",
            description = "A leather cradle on two cords. Nearly weightless, and its ammunition is underfoot everywhere."),
        // ---- Martial melee
        WeaponDef("battleaxe", "Battleaxe", "1d8", "Slashing", listOf("Versatile (1d10)"),
            mastery = "Topple", costGp = 10.0, weightLb = 4.0,
            description = "A broad crescent blade on a stout haft, usable in one hand or two."),
        WeaponDef("flail", "Flail", "1d8", "Bludgeoning", emptyList(), mastery = "Sap",
            costGp = 10.0, weightLb = 2.0,
            description = "A spiked head on a chain. Awkward to master, but it swings around a raised shield."),
        WeaponDef("glaive", "Glaive", "1d10", "Slashing", listOf("Heavy", "Reach", "Two-Handed"),
            mastery = "Graze", costGp = 20.0, weightLb = 6.0,
            description = "A single-edged blade on a long pole, striking at ten feet."),
        WeaponDef("greataxe", "Greataxe", "1d12", "Slashing", listOf("Heavy", "Two-Handed"),
            mastery = "Cleave", costGp = 30.0, weightLb = 7.0,
            description = "A massive double-bitted axe. Nothing subtle about it."),
        WeaponDef("greatsword", "Greatsword", "2d6", "Slashing", listOf("Heavy", "Two-Handed"),
            mastery = "Graze", costGp = 50.0, weightLb = 6.0,
            description = "A blade as tall as its wielder, swung in wide arcs that cut through several foes."),
        WeaponDef("halberd", "Halberd", "1d10", "Slashing", listOf("Heavy", "Reach", "Two-Handed"),
            mastery = "Cleave", costGp = 20.0, weightLb = 6.0,
            description = "An axe head, a spike, and a hook on a six-foot pole."),
        WeaponDef("lance", "Lance", "1d10", "Piercing", listOf("Heavy", "Reach", "Two-Handed"),
            mastery = "Topple", costGp = 10.0, weightLb = 6.0,
            description = "A long cavalry spear. Devastating from a charging mount, unwieldy on foot."),
        WeaponDef("longsword", "Longsword", "1d8", "Slashing", listOf("Versatile (1d10)"),
            mastery = "Sap", costGp = 15.0, weightLb = 3.0,
            description = "A straight double-edged blade with a crossguard, the mark of a trained warrior."),
        WeaponDef("maul", "Maul", "2d6", "Bludgeoning", listOf("Heavy", "Two-Handed"),
            mastery = "Topple", costGp = 10.0, weightLb = 10.0,
            description = "A sledgehammer built for war, meant to crush armor and the body inside it."),
        WeaponDef("morningstar", "Morningstar", "1d8", "Piercing", emptyList(), mastery = "Sap",
            costGp = 15.0, weightLb = 4.0,
            description = "A spiked ball on a shaft, punching through mail where a blade would not."),
        WeaponDef("pike", "Pike", "1d10", "Piercing", listOf("Heavy", "Reach", "Two-Handed"),
            mastery = "Push", costGp = 5.0, weightLb = 18.0,
            description = "An enormous spear used in formation to hold cavalry at bay."),
        WeaponDef("rapier", "Rapier", "1d8", "Piercing", listOf("Finesse"), usesDexOption = true,
            mastery = "Vex", costGp = 25.0, weightLb = 2.0,
            description = "A slender thrusting sword rewarding speed and precision over strength."),
        WeaponDef("scimitar", "Scimitar", "1d6", "Slashing", listOf("Finesse", "Light"),
            usesDexOption = true, mastery = "Nick", costGp = 25.0, weightLb = 3.0,
            description = "A curved blade built for fast, sweeping cuts."),
        WeaponDef("shortsword", "Shortsword", "1d6", "Piercing", listOf("Finesse", "Light"),
            usesDexOption = true, mastery = "Vex", costGp = 10.0, weightLb = 2.0,
            description = "A short stabbing blade, ideal in a shield wall or a narrow corridor."),
        WeaponDef("trident", "Trident", "1d8", "Piercing", listOf("Thrown (20/60)", "Versatile (1d10)"),
            mastery = "Topple", costGp = 5.0, weightLb = 4.0, range = "20/60 ft",
            description = "A three-pronged spear, favored by coastal peoples and gladiators."),
        WeaponDef("warhammer", "Warhammer", "1d8", "Bludgeoning", listOf("Versatile (1d10)"),
            mastery = "Push", costGp = 15.0, weightLb = 5.0,
            description = "A blunt head balanced by a spike, made to stave in plate armor."),
        WeaponDef("war_pick", "War Pick", "1d8", "Piercing", emptyList(), mastery = "Sap",
            costGp = 5.0, weightLb = 2.0,
            description = "A narrow spike on a haft, concentrating every blow into a single point."),
        WeaponDef("whip", "Whip", "1d4", "Slashing", listOf("Finesse", "Reach"),
            usesDexOption = true, mastery = "Slow", costGp = 2.0, weightLb = 3.0,
            description = "A braided leather lash that strikes from ten feet away."),
        // ---- Martial ranged
        WeaponDef("blowgun", "Blowgun", "1", "Piercing", listOf("Ammunition", "Loading"),
            isRanged = true, usesDexOption = true, mastery = "Vex",
            costGp = 10.0, weightLb = 1.0, range = "25/100 ft",
            description = "A hollow tube firing a needle. Almost silent, and usually paired with poison."),
        WeaponDef("hand_crossbow", "Hand Crossbow", "1d6", "Piercing",
            listOf("Ammunition", "Light", "Loading"), isRanged = true, usesDexOption = true,
            mastery = "Vex", costGp = 75.0, weightLb = 3.0, range = "30/120 ft",
            description = "A one-handed crossbow small enough to conceal under a cloak."),
        WeaponDef("heavy_crossbow", "Heavy Crossbow", "1d10", "Piercing",
            listOf("Ammunition", "Heavy", "Loading", "Two-Handed"), isRanged = true, usesDexOption = true,
            mastery = "Push", costGp = 50.0, weightLb = 18.0, range = "100/400 ft",
            description = "A windlass-drawn crossbow that punches through plate at range."),
        WeaponDef("longbow", "Longbow", "1d8", "Piercing", listOf("Ammunition", "Heavy", "Two-Handed"),
            isRanged = true, usesDexOption = true, mastery = "Slow",
            costGp = 50.0, weightLb = 2.0, range = "150/600 ft",
            description = "A tall bow of yew or elm. Years of training buy you six hundred feet of reach."),

        // ---- Firearms, renaissance
        // These two are Martial Ranged Weapons in the 2024 Player's Handbook, which is why
        // the Dungeon Master's Guide's firearm section covers only modern and futuristic.
        // A campaign has to allow them, but nothing else about them is special: Martial
        // weapon proficiency covers both, and each carries a mastery property like any other.
        WeaponDef("pistol", "Pistol", "1d10", "Piercing",
            listOf("Ammunition (Bullet)", "Loading"), isRanged = true, usesDexOption = true,
            mastery = "Vex", costGp = 250.0, weightLb = 3.0, range = "30/90 ft",
            description = "A renaissance firearm: a single ball driven by black powder. Loud, slow to reload, and it does not care how good your armor is."),
        WeaponDef("musket", "Musket", "1d12", "Piercing",
            listOf("Ammunition (Bullet)", "Loading", "Two-Handed"), isRanged = true,
            usesDexOption = true, mastery = "Slow", costGp = 500.0, weightLb = 10.0,
            range = "40/120 ft",
            description = "A long-barrelled renaissance firearm. A minute to load, a moment to fire, and a hole through most things in between."),

        // ---- Firearms, modern
        // From the Dungeon Master's Guide, which says to treat these as Rare magic items if
        // they are ever for sale — so they carry no price here. Reload gives a shot count
        // before you must spend an action or a Bonus Action reloading.
        WeaponDef("semiautomatic_pistol", "Semiautomatic Pistol", "2d6", "Piercing",
            listOf("Ammunition (Bullet)", "Reload (15 shots)"), isRanged = true,
            usesDexOption = true, mastery = "Vex", weightLb = 3.0, range = "50/150 ft",
            description = "A modern sidearm holding fifteen rounds. Treat as a Rare magic item if it is ever for sale."),
        WeaponDef("revolver", "Revolver", "2d8", "Piercing",
            listOf("Ammunition (Bullet)", "Reload (6 shots)"), isRanged = true,
            usesDexOption = true, mastery = "Sap", weightLb = 3.0, range = "40/120 ft",
            description = "Six chambers on a rotating cylinder. Treat as a Rare magic item if it is ever for sale."),
        WeaponDef("hunting_rifle", "Hunting Rifle", "2d10", "Piercing",
            listOf("Ammunition (Bullet)", "Reload (5 shots)", "Two-Handed"), isRanged = true,
            usesDexOption = true, mastery = "Slow", weightLb = 8.0, range = "80/240 ft",
            description = "A bolt-action rifle built for a single decisive shot. Treat as a Rare magic item if it is ever for sale."),
        WeaponDef("automatic_rifle", "Automatic Rifle", "2d8", "Piercing",
            listOf("Ammunition (Bullet)", "Burst Fire", "Reload (30 shots)", "Two-Handed"),
            isRanged = true, usesDexOption = true, mastery = "Slow", weightLb = 8.0,
            range = "80/240 ft",
            description = "Thirty rounds, and the option to spend ten of them spraying a 10-foot Cube. Treat as a Rare magic item if it is ever for sale."),
        WeaponDef("shotgun", "Shotgun", "2d8", "Piercing",
            listOf("Ammunition (Bullet)", "Reload (2 shots)", "Two-Handed"), isRanged = true,
            usesDexOption = true, mastery = "Push", weightLb = 7.0, range = "30/90 ft",
            description = "Two barrels and a short, brutal range. Treat as a Rare magic item if it is ever for sale."),

        // ---- Firearms, futuristic
        // Very Rare rather than Rare, and they burn Energy Cells instead of Bullets.
        WeaponDef("laser_pistol", "Laser Pistol", "3d6", "Radiant",
            listOf("Ammunition (Energy Cell)", "Reload (50 shots)"), isRanged = true,
            usesDexOption = true, mastery = "Vex", weightLb = 2.0, range = "40/120 ft",
            description = "A beam weapon drawing on a rechargeable cell. Treat as a Very Rare magic item if it is ever for sale."),
        WeaponDef("laser_rifle", "Laser Rifle", "3d8", "Radiant",
            listOf("Ammunition (Energy Cell)", "Reload (30 shots)", "Two-Handed"),
            isRanged = true, usesDexOption = true, mastery = "Slow", weightLb = 7.0,
            range = "100/300 ft",
            description = "A shouldered beam weapon with three hundred feet of reach. Treat as a Very Rare magic item if it is ever for sale."),
        WeaponDef("antimatter_rifle", "Antimatter Rifle", "6d8", "Necrotic",
            listOf("Ammunition (Energy Cell)", "Reload (2 shots)", "Two-Handed"),
            isRanged = true, usesDexOption = true, mastery = "Sap", weightLb = 10.0,
            range = "120/360 ft",
            description = "Two shots that unmake what they touch. Treat as a Very Rare magic item if it is ever for sale."),
    )

    /** Packs, tools, and sundry gear the picker can search alongside weapons and armor. */
    val GEAR: List<GearDef> = listOf(
        // ---- Packs
        GearDef("burglars_pack", "Burglar's Pack", "Equipment Pack", 16.0, 42.0,
            "A backpack with a ball bearing pouch, string, bell, candles, crowbar, hammer, pitons, hooded lantern, oil, rations, rope, and a tinderbox."),
        GearDef("diplomats_pack", "Diplomat's Pack", "Equipment Pack", 39.0, 39.0,
            "A chest with fine clothes, ink and pen, lamp, oil, paper, perfume, sealing wax, and soap."),
        GearDef("dungeoneers_pack", "Dungeoneer's Pack", "Equipment Pack", 12.0, 55.0,
            "A backpack with a crowbar, hammer, pitons, torches, a tinderbox, rations, a waterskin, and rope."),
        GearDef("entertainers_pack", "Entertainer's Pack", "Equipment Pack", 40.0, 38.0,
            "A backpack with a bedroll, costumes, candles, rations, a waterskin, and a disguise kit."),
        GearDef("explorers_pack", "Explorer's Pack", "Equipment Pack", 10.0, 55.0,
            "A backpack with a bedroll, mess kit, tinderbox, torches, rations, a waterskin, and 50 feet of rope."),
        GearDef("priests_pack", "Priest's Pack", "Equipment Pack", 19.0, 25.0,
            "A backpack with a blanket, candles, a tinderbox, an alms box, incense, a censer, vestments, rations, and a waterskin."),
        GearDef("scholars_pack", "Scholar's Pack", "Equipment Pack", 40.0, 22.0,
            "A backpack with a book of lore, ink and pen, parchment, and a little bag of sand."),
        // ---- Tools
        GearDef("thieves_tools", "Thieves' Tools", "Tool", 25.0, 1.0,
            "Picks, a small file, wires, and mirrors. Used to pick locks and disarm traps."),
        GearDef("herbalism_kit", "Herbalism Kit", "Tool", 5.0, 3.0,
            "Pouches, clippers, and vials for identifying plants and brewing remedies. Used to craft potions of healing."),
        GearDef("healers_kit", "Healer's Kit", "Tool", 5.0, 3.0,
            "Bandages, salves, and splints with ten uses. Spend one as a Utilize action to stabilize a dying creature."),
        GearDef("disguise_kit", "Disguise Kit", "Tool", 25.0, 3.0,
            "Cosmetics, hair dye, and props for changing your appearance."),
        GearDef("forgery_kit", "Forgery Kit", "Tool", 15.0, 5.0,
            "Papers, inks, seals, and wax for producing convincing documents."),
        GearDef("poisoners_kit", "Poisoner's Kit", "Tool", 50.0, 2.0,
            "Vials, chemicals, and gloves for crafting and applying poisons."),
        GearDef("navigators_tools", "Navigator's Tools", "Tool", 25.0, 2.0,
            "Charts, a compass, and calipers for plotting a course and avoiding getting lost."),
        GearDef("cartographers_tools", "Cartographer's Tools", "Tool", 15.0, 6.0,
            "Pens, compasses, and rulers for drawing accurate maps."),
        GearDef("calligraphers_supplies", "Calligrapher's Supplies", "Tool", 10.0, 5.0,
            "Ink, pens, and parchment for elegant writing and spotting forgeries."),
        GearDef("carpenters_tools", "Carpenter's Tools", "Tool", 8.0, 6.0,
            "A saw, hammer, chisels, and a square for building and repairing wooden structures."),
        GearDef("smiths_tools", "Smith's Tools", "Tool", 20.0, 8.0,
            "Hammers, tongs, and a whetstone for working metal and repairing armor."),
        GearDef("tinkers_tools", "Tinker's Tools", "Tool", 50.0, 10.0,
            "Small tools and spare parts for patching and building simple devices."),
        GearDef("gaming_set", "Gaming Set", "Tool", 1.0, 0.5,
            "Dice, cards, or a board game, and the social know-how that comes with playing well."),
        GearDef("musical_instrument", "Musical Instrument", "Tool", 30.0, 3.0,
            "A lute, flute, drum, or similar. Bards can use one as a Spellcasting Focus."),
        GearDef("artisans_tools", "Artisan's Tools", "Tool", 15.0, 5.0,
            "The implements of a specific trade, from brewing to leatherworking."),
        // ---- Focuses and holy items
        GearDef("holy_symbol", "Holy Symbol", "Spellcasting Focus", 5.0, 1.0,
            "An emblem of a deity, worn or held. Clerics and Paladins use it as a Spellcasting Focus."),
        GearDef("arcane_focus", "Arcane Focus", "Spellcasting Focus", 10.0, 1.0,
            "A crystal, orb, rod, staff, or wand channeling arcane magic in place of material components."),
        GearDef("druidic_focus", "Druidic Focus", "Spellcasting Focus", 10.0, 1.0,
            "A sprig of mistletoe, a totem, a yew wand, or a wooden staff used to channel Druid magic."),
        GearDef("spellbook", "Spellbook", "Spellcasting Focus", 50.0, 3.0,
            "A leather-bound tome of 100 blank vellum pages. A Wizard's spells live here."),
        GearDef("component_pouch", "Component Pouch", "Spellcasting Focus", 25.0, 2.0,
            "A watertight belt pouch holding every material component with no listed cost."),
        // ---- Adventuring gear
        GearDef("backpack", "Backpack", "Adventuring Gear", 2.0, 5.0,
            "A leather pack holding a cubic foot of gear, with straps and buckles for lashing more outside."),
        GearDef("bedroll", "Bedroll", "Adventuring Gear", 1.0, 7.0,
            "A padded sleeping roll that makes a night on cold stone survivable."),
        GearDef("rope", "Rope (50 feet)", "Adventuring Gear", 1.0, 10.0,
            "Fifty feet of hempen rope with 2 hit points, burstable with a DC 17 Strength check."),
        GearDef("torch", "Torch", "Adventuring Gear", 0.01, 1.0,
            "Burns for 1 hour, casting Bright Light in a 20-foot radius. Deals 1 Fire damage as an improvised weapon."),
        GearDef("lantern_hooded", "Hooded Lantern", "Adventuring Gear", 5.0, 2.0,
            "Burns oil for 6 hours, casting Bright Light in a 30-foot radius. The hood dims it to a 5-foot glow."),
        GearDef("oil_flask", "Oil (flask)", "Adventuring Gear", 0.1, 1.0,
            "Fuels a lamp for 6 hours, or can be thrown to douse a creature in flammable oil."),
        GearDef("tinderbox", "Tinderbox", "Adventuring Gear", 0.5, 1.0,
            "Flint, steel, and tinder for lighting a fire or a torch."),
        GearDef("rations", "Rations (1 day)", "Adventuring Gear", 0.5, 2.0,
            "Dry foods that keep on the road: jerky, hardtack, nuts, and dried fruit."),
        GearDef("waterskin", "Waterskin", "Adventuring Gear", 0.2, 5.0,
            "Holds 4 pints of liquid. Weighs 5 pounds full and next to nothing empty."),
        GearDef("crowbar", "Crowbar", "Adventuring Gear", 2.0, 5.0,
            "Grants Advantage on Strength checks where leverage would help."),
        GearDef("hammer", "Hammer", "Adventuring Gear", 1.0, 3.0,
            "A one-handed hammer for driving pitons and nails."),
        GearDef("piton", "Piton", "Adventuring Gear", 0.05, 0.25,
            "An iron spike driven into rock or wood to anchor a rope."),
        GearDef("grappling_hook", "Grappling Hook", "Adventuring Gear", 2.0, 4.0,
            "A four-pronged hook for catching a ledge with a thrown rope."),
        GearDef("caltrops", "Caltrops (bag of 20)", "Adventuring Gear", 1.0, 2.0,
            "Spread over a 5-foot square, they force a Dexterity save or deal 1 Piercing damage and halve Speed."),
        GearDef("ball_bearings", "Ball Bearings (bag of 1,000)", "Adventuring Gear", 1.0, 2.0,
            "Spread over a 10-foot square, they force a Dexterity save or a creature falls Prone."),
        GearDef("chain", "Chain (10 feet)", "Adventuring Gear", 5.0, 10.0,
            "Ten feet of iron chain with 10 hit points, useful for binding or securing."),
        GearDef("manacles", "Manacles", "Adventuring Gear", 2.0, 6.0,
            "Restrain a Small or Medium creature. Escaping needs a DC 20 Sleight of Hand or Athletics check."),
        GearDef("lock", "Lock", "Adventuring Gear", 10.0, 1.0,
            "Comes with a key. Picking it requires Thieves' Tools and a DC 15 Dexterity check."),
        GearDef("mirror_steel", "Steel Mirror", "Adventuring Gear", 5.0, 0.5,
            "A polished hand mirror, for signalling, looking around corners, and checking for a reflection."),
        GearDef("pole", "Pole (10-foot)", "Adventuring Gear", 0.05, 7.0,
            "Ten feet of wooden pole. The traditional way to find a pit trap without falling into it."),
        GearDef("shovel", "Shovel", "Adventuring Gear", 2.0, 5.0,
            "For digging out graves, tunnels, and buried treasure."),
        GearDef("iron_pot", "Iron Pot", "Adventuring Gear", 2.0, 10.0,
            "Holds a gallon. Doubles as a cooking vessel and an improvised helmet."),
        GearDef("mess_kit", "Mess Kit", "Adventuring Gear", 0.2, 1.0,
            "A tin box with a cup and cutlery that clamps shut over a plate."),
        GearDef("tent", "Tent (two-person)", "Adventuring Gear", 2.0, 20.0,
            "A simple canvas shelter that sleeps two."),
        GearDef("quiver", "Quiver", "Adventuring Gear", 1.0, 1.0,
            "Holds 20 arrows."),
        GearDef("arrows", "Arrows (20)", "Ammunition", 1.0, 1.0,
            "Ammunition for bows. Half can usually be recovered after a fight."),
        GearDef("bolts", "Crossbow Bolts (20)", "Ammunition", 1.0, 1.5,
            "Ammunition for crossbows. Half can usually be recovered after a fight."),
        GearDef("sling_bullets", "Sling Bullets (20)", "Ammunition", 0.04, 1.5,
            "Lead shot for a sling, though a smooth stone does nearly as well."),
        GearDef("needles", "Blowgun Needles (50)", "Ammunition", 1.0, 1.0,
            "Slim needles for a blowgun, often coated with something unpleasant."),
        GearDef("potion_healing", "Potion of Healing", "Consumable", 50.0, 0.5,
            "Drink or administer as a Bonus Action to regain 2d4 + 2 hit points."),
        GearDef("antitoxin", "Antitoxin", "Consumable", 50.0, 0.0,
            "Grants Advantage on saving throws against poison for 1 hour."),
        GearDef("acid_vial", "Acid (vial)", "Consumable", 25.0, 1.0,
            "Thrown as an improvised weapon for 2d6 Acid damage on a hit."),
        GearDef("alchemists_fire", "Alchemist's Fire (flask)", "Consumable", 50.0, 1.0,
            "Thrown to set a creature alight for 1d4 Fire damage each turn until doused."),
        GearDef("holy_water", "Holy Water (flask)", "Consumable", 25.0, 1.0,
            "Thrown at a Fiend or Undead for 2d8 Radiant damage."),
        GearDef("clothes_common", "Common Clothes", "Clothing", 0.5, 3.0,
            "Ordinary garments that let you pass unremarked in most towns."),
        GearDef("clothes_fine", "Fine Clothes", "Clothing", 15.0, 6.0,
            "Well-cut garments of good cloth, required at court and useful for impressing anyone."),
        GearDef("clothes_travelers", "Traveler's Clothes", "Clothing", 2.0, 4.0,
            "Sturdy boots, a wool coat, and layers built for weather and distance."),
        GearDef("costume", "Costume", "Clothing", 5.0, 4.0,
            "A disguise or performance outfit."),
        GearDef("robe", "Robe", "Clothing", 1.0, 4.0,
            "A long flowing garment favored by scholars and spellcasters."),
        GearDef("signet_ring", "Signet Ring", "Adventuring Gear", 5.0, 0.0,
            "Stamps your house's mark into wax, proving a document came from you."),
        GearDef("perfume", "Perfume (vial)", "Adventuring Gear", 5.0, 0.0,
            "A pleasant scent, sometimes enough to change how a room receives you."),
        GearDef("parchment", "Parchment (sheet)", "Adventuring Gear", 0.1, 0.0,
            "A single sheet of prepared animal skin for writing."),
        GearDef("ink_pen", "Ink Pen", "Adventuring Gear", 0.02, 0.0,
            "A wooden stylus with a metal nib."),
        GearDef("ink", "Ink (1 ounce bottle)", "Adventuring Gear", 10.0, 0.0,
            "A bottle of black ink, enough for a great deal of writing."),
        GearDef("book", "Book", "Adventuring Gear", 25.0, 5.0,
            "A bound volume of lore, poetry, ritual, or record-keeping."),
        GearDef("pouch", "Pouch", "Adventuring Gear", 0.5, 1.0,
            "A cloth or leather belt pouch holding a fifth of a cubic foot."),
        GearDef("candle", "Candle", "Adventuring Gear", 0.01, 0.0,
            "Burns for 1 hour, shedding Bright Light in a 5-foot radius."),
        GearDef("blanket", "Blanket", "Adventuring Gear", 0.5, 3.0,
            "Thick wool, warm enough for a night without a fire."),

        // ---- The rest of the Player's Handbook equipment table, plus the climate and
        // disguise gear the later books add. Forty-one rows the catalogue never had: a
        // player looking for a Bucket, a Bell or a Spyglass found nothing and typed it in
        // by hand, which is the same gap as a missing spell, only quieter.
        GearDef("ammunition", "Ammunition", "Adventuring Gear", 0.0, 0.0,
            "Ammunition is required by a weapon that has the Ammunition property. A weapon's " +
                "description specifies the type of ammunition used by the weapon. The Ammunition " +
                "table lists the different types and the amount you get when you buy them."),
        GearDef("barrel", "Barrel", "Adventuring Gear", 2.0, 70.0,
            "A Barrel holds up to 40 gallons of liquid or up to 4 cubic feet of dry goods."),
        GearDef("basket", "Basket", "Adventuring Gear", 0.4, 2.0,
            "A Basket holds up to 40 pounds within 2 cubic feet."),
        GearDef("bell", "Bell", "Adventuring Gear", 1.0, 0.0,
            "When rung as a Utilize action, a Bell produces a sound that can be heard up to 60 " +
                "feet away."),
        GearDef("block_and_tackle", "Block and Tackle", "Adventuring Gear", 1.0, 5.0,
            "A Block and Tackle allows you to hoist up to four times the weight you can normally " +
                "lift."),
        GearDef("bright_fungal_cloak", "Bright Fungal Cloak", "Adventuring Gear", 25.0, 0.0,
            "While wearing a Bright Fungal Cloak, you can take a Bonus Action to furl or unfurl " +
                "it. When the cloak is unfurled, it sheds Bright Light in a 5-foot radius and Dim " +
                "Light for an additional 5 feet. One pound of fungus is sewn into a Bright Fungal " +
                "Cloak. This fungus can be eaten as food. Once all the fungus is consumed, the cloak " +
                "becomes a mundane set of Traveler's Clothes."),
        GearDef("glass_bottle", "Glass Bottle", "Adventuring Gear", 2.0, 2.0,
            "A Glass Bottle holds up to 1 1/2 pints."),
        GearDef("bucket", "Bucket", "Adventuring Gear", 0.05, 2.0,
            "A Bucket holds up to half a cubic foot of contents."),
        GearDef("crossbow_bolt_case", "Crossbow Bolt Case", "Adventuring Gear", 1.0, 1.0,
            "A Crossbow Bolt Case holds up to 20 Bolts."),
        GearDef("map_or_scroll_case", "Map or Scroll Case", "Adventuring Gear", 1.0, 1.0,
            "A Map or Scroll Case holds up to 10 sheets of paper or 5 sheets of parchment."),
        GearDef("chest", "Chest", "Adventuring Gear", 5.0, 25.0,
            "A Chest holds up to 12 cubic feet of contents."),
        GearDef("climbers_kit", "Climber's Kit", "Adventuring Gear", 25.0, 12.0,
            "A Climber's Kit includes boot tips, gloves, pitons, and a harness. As a Utilize " +
                "action, you can use the Climber's Kit to anchor yourself; when you do, you can't " +
                "fall more than 25 feet from the anchor point, and you can't move more than 25 feet " +
                "from there without undoing the anchor as a Bonus Action."),
        GearDef("fine_clothes", "Fine Clothes", "Adventuring Gear", 15.0, 6.0,
            "Fine Clothes are made of expensive fabrics and adorned with expertly crafted " +
                "details. Some events and locations admit only people wearing these clothes."),
        GearDef("travelers_clothes", "Traveler's Clothes", "Adventuring Gear", 2.0, 4.0,
            "Traveler's Clothes are resilient garments designed for travel in various " +
                "environments."),
        GearDef("desert_clothing", "Desert Clothing", "Adventuring Gear", 5.0, 0.0,
            "When you are wearing Desert Clothing and not wearing Medium or Heavy armor, you " +
                "automatically succeed on saving throws against the effects of extreme heat. See " +
                "chapter 3 of the Dungeon Master's Guide for rules on extreme heat."),
        GearDef("devil_mask", "Devil Mask", "Adventuring Gear", 25.0, 0.0,
            "While you are wearing a Devil Mask, other creatures have Disadvantage on " +
                "Intelligence (Investigation) and Wisdom (Insight) checks made to discern your true " +
                "identity or intentions."),
        GearDef("flask", "Flask", "Adventuring Gear", 0.02, 1.0,
            "A Flask holds up to 1 pint."),
        GearDef("garb_of_light_and_shadow", "Garb of Light and Shadow", "Adventuring Gear", 50.0, 0.0,
            "This garb appeals to Fey from one Domain of Delight, such as the Gloaming Court or " +
                "the Summer Court. While wearing the garb, you have Advantage on ability checks to " +
                "influence Fey associated with that Domain of Delight."),
        GearDef("genie_robe", "Genie Robe", "Adventuring Gear", 50.0, 0.0,
            "This robe appeals to Elementals associated with a particular Elemental Plane (Air, " +
                "Earth, Fire, Water). While wearing a Genie Robe, you have Advantage on ability " +
                "checks made to influence Elementals associated with that plane."),
        GearDef("hunting_trap", "Hunting Trap", "Adventuring Gear", 5.0, 25.0,
            "As a Utilize action, you can set a Hunting Trap, which is a sawtooth steel ring that " +
                "snaps shut when a creature steps on a pressure plate in the center. The trap is " +
                "affixed by a heavy chain to an immobile object, such as a tree or a spike driven " +
                "into the ground. A creature that steps on the plate must succeed on a DC 13 " +
                "Dexterity saving throw or take 1d4 Piercing damage and have its Speed reduced to 0 " +
                "until the start of its next turn. Thereafter, until the creature breaks free of the " +
                "trap, its movement is limited by the length of the chain (typically 3 feet). A " +
                "creature can use its action to make a DC 13 Strength (Athletics) check, freeing " +
                "itself or another creature within its reach on a success. Each failed check deals 1 " +
                "Piercing damage to the trapped creature."),
        GearDef("jug", "Jug", "Adventuring Gear", 0.02, 4.0,
            "A Jug holds up to 1 gallon."),
        GearDef("ladder", "Ladder", "Adventuring Gear", 0.1, 25.0,
            "A Ladder is 10 feet tall. You must climb to move up or down it."),
        GearDef("lamp", "Lamp", "Adventuring Gear", 0.5, 1.0,
            "A Lamp burns Oil as fuel to cast Bright Light in a 15-foot radius and Dim Light for " +
                "an additional 30 feet."),
        GearDef("bullseye_lantern", "Bullseye Lantern", "Adventuring Gear", 10.0, 2.0,
            "A Bullseye Lantern burns Oil as fuel to cast Bright Light in a 60-foot Cone and Dim " +
                "Light for an additional 60 feet."),
        GearDef("locking_spellbook", "Locking Spellbook", "Adventuring Gear", 35.0, 0.0,
            "This 100-page leather-bound tome can be used as a Spellbook. It is closed with a " +
                "lock that comes with a key. As a Utilize action, a creature can try to pick the lock " +
                "using Thieves' Tools, doing so with a successful DC 15 Dexterity (Sleight of Hand) " +
                "check."),
        GearDef("magnifying_glass", "Magnifying Glass", "Adventuring Gear", 100.0, 0.0,
            "A Magnifying Glass grants Advantage on any ability check made to appraise or inspect " +
                "a highly detailed item. Lighting a fire with a Magnifying Glass requires light as " +
                "bright as sunlight to focus, tinder to ignite, and about 5 minutes for the fire to " +
                "ignite."),
        GearDef("map", "Map", "Adventuring Gear", 1.0, 0.0,
            "If you consult an accurate Map, you gain a +5 bonus to Wisdom (Survival) checks you " +
                "make to find your way in the place represented on it."),
        GearDef("mirror", "Mirror", "Adventuring Gear", 5.0, 0.5,
            "A handheld steel Mirror is useful for personal cosmetics but also for peeking around " +
                "corners and reflecting light as a signal."),
        GearDef("monster_camouflage", "Monster Camouflage", "Adventuring Gear", 50.0, 0.0,
            "A suit of Monster Camouflage looks like a Beast or Monstrosity, such as an owlbear. " +
                "To discern that you're disguised, a creature must take the Study action to inspect " +
                "your appearance and succeed on a DC 10 Intelligence (Investigation or Nature) check. " +
                "The creature has Advantage on this check if it is within 30 feet of you and " +
                "automatically succeeds on this check if you do anything the monster you're disguised " +
                "as couldn't do."),
        GearDef("net", "Net", "Adventuring Gear", 1.0, 3.0,
            "When you take the Attack action, you can replace one of your attacks with throwing a " +
                "Net. Target a creature you can see within 15 feet of yourself. The target must " +
                "succeed on a Dexterity saving throw (DC 8 plus your Dexterity modifier and " +
                "Proficiency Bonus) or have the Restrained condition until it escapes. The target " +
                "succeeds automatically if it is Huge or larger. To escape, the target or a creature " +
                "within 5 feet of it must take an action to make a DC 10 Strength (Athletics) check, " +
                "freeing the Restrained creature on a success. Destroying the Net (AC 10; 5 HP; " +
                "Immunity to Bludgeoning, Poison, and Psychic damage) also frees the target, ending " +
                "the effect."),
        GearDef("paper", "Paper", "Adventuring Gear", 0.2, 0.0,
            "One sheet of Paper can hold about 250 handwritten words."),
        GearDef("basic_poison", "Basic Poison", "Adventuring Gear", 100.0, 0.0,
            "As a Bonus Action, you can use a vial of Basic Poison to coat one weapon or up to " +
                "three pieces of ammunition. A creature that takes Piercing or Slashing damage from " +
                "the poisoned weapon or ammunition takes an extra 1d4 Poison damage. Once applied, " +
                "the poison retains potency for 1 minute or until its damage is dealt, whichever " +
                "comes first."),
        GearDef("portable_ram", "Portable Ram", "Adventuring Gear", 4.0, 35.0,
            "You can use a Portable Ram to break down doors. When doing so, you gain a +4 bonus " +
                "to the Strength check. One other character can help you use the ram, giving you " +
                "Advantage on this check."),
        GearDef("sack", "Sack", "Adventuring Gear", 0.01, 0.5,
            "A Sack holds up to 30 pounds within 1 cubic foot."),
        GearDef("signal_whistle", "Signal Whistle", "Adventuring Gear", 0.05, 0.0,
            "When blown as a Utilize action, a Signal Whistle produces a sound that can be heard " +
                "up to 600 feet away."),
        GearDef("iron_spikes", "Iron Spikes", "Adventuring Gear", 1.0, 5.0,
            "Iron Spikes come in bundles of ten. As a Utilize action, you can use a blunt object, " +
                "such as a Light Hammer, to hammer a spike into wood, earth, or a similar material. " +
                "You can do so to jam a door shut or to then tie a Rope or Chain to the Spike."),
        GearDef("spyglass", "Spyglass", "Adventuring Gear", 1000.0, 1.0,
            "Objects viewed through a Spyglass are magnified to twice their size."),
        GearDef("string", "String", "Adventuring Gear", 0.1, 0.0,
            "String is 10 feet long. You can tie a knot in it as a Utilize action."),
        GearDef("vial", "Vial", "Adventuring Gear", 1.0, 0.0,
            "A Vial holds up to 4 ounces."),
        GearDef("warm_fungal_clothing", "Warm Fungal Clothing", "Adventuring Gear", 15.0, 0.0,
            "When you're wearing Warm Fungal Clothing, you automatically succeed on saving throws " +
                "against the effects of extreme cold. See chapter 3 of the Dungeon Master's Guide for " +
                "rules on extreme cold. One pound of fungus is sewn into Fungal Clothing. This fungus " +
                "can be eaten as food. Once all the fungus is consumed, this becomes a mundane set of " +
                "Traveler's Clothes."),
        GearDef("winter_camouflage", "Winter Camouflage", "Adventuring Gear", 50.0, 0.0,
            "While you wear Winter Camouflage in an appropriate environment, you have Advantage " +
                "on Dexterity (Stealth) checks."),
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
            otherGear = listOf("Explorer's Pack", "Druidic Focus", "Herbalism Kit"),
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
            otherGear = listOf("Explorer's Pack", "Artisan's Tools"),
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
            otherGear = listOf("Druidic Focus", "Explorer's Pack", "Quiver", "Arrows (20)"),
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
            otherGear = listOf("Arcane Focus", "Dungeoneer's Pack"),
            goldPieces = 28,
        ),
        "warlock" to StartingKit(
            armorIds = listOf("leather"),
            weaponIds = listOf("sickle", "dagger", "dagger"),
            otherGear = listOf("Arcane Focus", "Book", "Scholar's Pack"),
            goldPieces = 15,
        ),
        "wizard" to StartingKit(
            weaponIds = listOf("quarterstaff", "dagger", "dagger"),
            otherGear = listOf("Arcane Focus", "Robe", "Scholar's Pack", "Spellbook"),
            goldPieces = 5,
        ),
    )

    fun armorById(id: String): ArmorDef? = ARMOR.find { it.id == id }
    fun weaponById(id: String): WeaponDef? = WEAPONS.find { it.id == id }
    fun gearById(id: String): GearDef? = GEAR.find { it.id == id }
}
