package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.ItemRarity
import com.pedroeu.ficha.data.model.MagicItem

/**
 * The magic items a DM is likely to hand out, and the ones an Artificer can replicate.
 *
 * Entries carry a functional summary rather than the rulebook's full prose — enough to play
 * the item from the sheet without reaching for the book. Items flagged with an
 * [MagicItem.artificerPlanLevel] appear in the Replicate Magic Item plan lists at that level,
 * so the class feature and the inventory picker can never disagree about what exists.
 */
object MagicItemData {

    private fun item(
        id: String,
        name: String,
        rarity: ItemRarity,
        kind: String,
        description: String,
        attunement: Boolean = false,
        attunementNote: String = "",
        weightLb: Double = 0.0,
        plan: Int? = null,
    ) = MagicItem(
        id = id,
        name = name,
        rarity = rarity,
        kind = kind,
        description = description,
        requiresAttunement = attunement,
        attunementNote = attunementNote,
        weightLb = weightLb,
        artificerPlanLevel = plan,
    )

    private const val WONDROUS = "Wondrous Item"

    // ------------------------------------------------------------------ Armor and shields

    private val ARMOR_AND_SHIELDS = listOf(
        item("adamantine_armor", "Adamantine Armor", ItemRarity.UNCOMMON,
            "Armor (Medium or Heavy, except Hide)",
            "This armor is reinforced with adamantine. While you wear it, any Critical Hit " +
                "against you becomes a normal hit."),
        item("armor_plus_1", "Armor, +1", ItemRarity.RARE, "Armor (any)",
            "You have a +1 bonus to Armor Class while wearing this armor.", plan = 6),
        item("armor_plus_2", "Armor, +2", ItemRarity.VERY_RARE, "Armor (any)",
            "You have a +2 bonus to Armor Class while wearing this armor.", plan = 14),
        item("armor_plus_3", "Armor, +3", ItemRarity.LEGENDARY, "Armor (any)",
            "You have a +3 bonus to Armor Class while wearing this armor."),
        item("armor_of_resistance", "Armor of Resistance", ItemRarity.RARE, "Armor (any)",
            "You have Resistance to one type of damage while you wear this armor. The damage " +
                "type is chosen when the armor is created — Acid, Cold, Fire, Force, Lightning, " +
                "Necrotic, Poison, Psychic, Radiant, or Thunder.",
            attunement = true, plan = 10),
        item("armor_invulnerability", "Armor of Invulnerability", ItemRarity.LEGENDARY,
            "Armor (Plate)",
            "You have Resistance to Bludgeoning, Piercing, and Slashing damage. As a Magic " +
                "action you can gain Immunity to that damage for 10 minutes, once per day.",
            attunement = true, weightLb = 65.0),
        item("arrow_catching_shield", "Arrow-Catching Shield", ItemRarity.RARE, "Armor (Shield)",
            "You gain a +2 bonus to Armor Class against ranged attacks while holding this " +
                "Shield. In addition, you can take a Reaction to become the target of a ranged " +
                "attack aimed at another creature within 5 feet of you.",
            attunement = true, weightLb = 6.0, plan = 14),
        item("elven_chain", "Elven Chain", ItemRarity.RARE, "Armor (Chain Shirt)",
            "You gain a +1 bonus to Armor Class while wearing this fine mesh, and you count as " +
                "proficient with it even if you lack proficiency with Medium armor.",
            weightLb = 20.0, plan = 10),
        item("mithral_armor", "Mithral Armor", ItemRarity.UNCOMMON, "Armor (Medium or Heavy)",
            "This light, flexible armor has no Strength requirement and doesn't impose " +
                "Disadvantage on Dexterity (Stealth) checks."),
        item("repulsion_shield", "Repulsion Shield", ItemRarity.RARE, "Armor (Shield)",
            "This Shield has 4 charges and regains 1d4 expended charges daily at dawn. When " +
                "you are hit by a melee attack while holding it, you can expend a charge to " +
                "push the attacker up to 15 feet away from you.",
            weightLb = 6.0, plan = 6),
        item("sentinel_shield", "Sentinel Shield", ItemRarity.UNCOMMON, "Armor (Shield)",
            "While holding this Shield you have Advantage on Initiative rolls and Wisdom " +
                "(Perception) checks.",
            weightLb = 6.0, plan = 6),
        item("shield_plus_1", "Shield, +1", ItemRarity.UNCOMMON, "Armor (Shield)",
            "You have a +1 bonus to Armor Class beyond the Shield's normal bonus while holding it.",
            weightLb = 6.0, plan = 2),
        item("shield_plus_2", "Shield, +2", ItemRarity.RARE, "Armor (Shield)",
            "You have a +2 bonus to Armor Class beyond the Shield's normal bonus while holding it.",
            weightLb = 6.0, plan = 10),
        item("shield_plus_3", "Shield, +3", ItemRarity.VERY_RARE, "Armor (Shield)",
            "You have a +3 bonus to Armor Class beyond the Shield's normal bonus while holding it.",
            weightLb = 6.0),
        item("spellguard_shield", "Spellguard Shield", ItemRarity.VERY_RARE, "Armor (Shield)",
            "While holding this Shield you have Advantage on saving throws against spells and " +
                "other magical effects, and spell attack rolls against you have Disadvantage.",
            attunement = true, weightLb = 6.0),
    )

    // ------------------------------------------------------------------ Weapons

    private val WEAPONS = listOf(
        item("weapon_plus_1", "Weapon, +1", ItemRarity.UNCOMMON, "Weapon (any)",
            "You have a +1 bonus to attack rolls and damage rolls made with this magic weapon.",
            plan = 2),
        item("weapon_plus_2", "Weapon, +2", ItemRarity.RARE, "Weapon (any)",
            "You have a +2 bonus to attack rolls and damage rolls made with this magic weapon.",
            plan = 10),
        item("weapon_plus_3", "Weapon, +3", ItemRarity.VERY_RARE, "Weapon (any)",
            "You have a +3 bonus to attack rolls and damage rolls made with this magic weapon."),
        item("adamantine_weapon", "Adamantine Weapon", ItemRarity.UNCOMMON,
            "Weapon (any) or Ammunition",
            "Whenever this weapon or piece of ammunition hits an object, the hit is a Critical Hit."),
        item("berserker_axe", "Berserker Axe", ItemRarity.RARE, "Weapon (Axe)",
            "You gain a +1 bonus to attack and damage rolls, and your Hit Point maximum " +
                "increases by 1 per character level. The axe is cursed: while attuned you have " +
                "Disadvantage on attack rolls with other weapons.",
            attunement = true),
        item("dagger_of_venom", "Dagger of Venom", ItemRarity.RARE, "Weapon (Dagger)",
            "You gain a +1 bonus to attack and damage rolls with this dagger. As a Bonus Action " +
                "you can coat it in poison once per day; the next creature hit takes an extra " +
                "2d10 Poison damage and must succeed on a DC 15 Constitution saving throw or " +
                "have the Poisoned condition for 1 minute.",
            weightLb = 1.0, plan = 10),
        item("dazzling_weapon", "Dazzling Weapon", ItemRarity.RARE, "Weapon (any)",
            "You gain a +1 bonus to attack and damage rolls with this weapon. Once per turn " +
                "when you hit with it, you can dazzle the target, giving it Disadvantage on its " +
                "next attack roll before the start of your next turn.",
            attunement = true, plan = 6),
        item("defender", "Defender", ItemRarity.LEGENDARY, "Weapon (any Sword)",
            "You gain a +3 bonus to attack and damage rolls with this sword, and on each of " +
                "your turns you can transfer any part of that bonus to your Armor Class instead.",
            attunement = true),
        item("dragon_slayer", "Dragon Slayer", ItemRarity.RARE, "Weapon (any Sword)",
            "You gain a +1 bonus to attack and damage rolls with this sword. When you hit a " +
                "Dragon with it, the Dragon takes an extra 3d6 damage of the weapon's type.",
            weightLb = 3.0),
        item("flame_tongue", "Flame Tongue", ItemRarity.RARE, "Weapon (any Sword)",
            "As a Bonus Action you can cause flames to erupt from this sword, shedding Bright " +
                "Light in a 40-foot radius. While ablaze it deals an extra 2d6 Fire damage on a " +
                "hit. A second Bonus Action extinguishes it.",
            attunement = true, weightLb = 3.0, plan = 14),
        item("frost_brand", "Frost Brand", ItemRarity.VERY_RARE, "Weapon (any Sword)",
            "When you hit with this sword, the target takes an extra 1d6 Cold damage. You also " +
                "have Resistance to Fire damage while holding it, and it can extinguish " +
                "nonmagical flames within 30 feet.",
            attunement = true, weightLb = 3.0),
        item("repeating_shot", "Repeating Shot", ItemRarity.UNCOMMON,
            "Weapon (any Ammunition weapon)",
            "You gain a +1 bonus to attack and damage rolls with this weapon, which produces " +
                "its own magic ammunition when you fire it. It also ignores the Loading property.",
            attunement = true, plan = 2),
        item("returning_weapon", "Returning Weapon", ItemRarity.UNCOMMON,
            "Weapon (any weapon with the Thrown property)",
            "You gain a +1 bonus to attack and damage rolls with this weapon, and it returns to " +
                "your hand immediately after you use it to make a ranged attack.",
            plan = 2),
        item("sun_blade", "Sun Blade", ItemRarity.RARE, "Weapon (Longsword)",
            "This hilt produces a blade of pure radiance. You gain a +2 bonus to attack and " +
                "damage rolls, it deals Radiant damage instead of Slashing, and it deals an " +
                "extra 1d8 Radiant damage to Undead.",
            attunement = true, weightLb = 3.0),
        item("vicious_weapon", "Vicious Weapon", ItemRarity.RARE, "Weapon (any)",
            "When you roll a 20 on an attack roll with this weapon, the target takes an extra " +
                "2d6 damage of the weapon's type."),
        item("wraps_unarmed_1", "Wraps of Unarmed Power, +1", ItemRarity.UNCOMMON, WONDROUS,
            "While wearing these wraps your Unarmed Strikes count as magical and gain a +1 " +
                "bonus to attack rolls and damage rolls.",
            plan = 2),
        item("wraps_unarmed_2", "Wraps of Unarmed Power, +2", ItemRarity.RARE, WONDROUS,
            "While wearing these wraps your Unarmed Strikes count as magical and gain a +2 " +
                "bonus to attack rolls and damage rolls.",
            plan = 10),
    )

    // ------------------------------------------------------------------ Rings

    private val RINGS = listOf(
        item("ring_free_action", "Ring of Free Action", ItemRarity.RARE, "Ring",
            "Difficult Terrain costs you no extra movement, and neither magic nor a Grappled " +
                "condition can reduce your Speed or give you the Paralyzed or Restrained condition.",
            attunement = true, plan = 14),
        item("ring_feather_falling", "Ring of Feather Falling", ItemRarity.RARE, "Ring",
            "When you fall, you descend 60 feet per round and take no damage from the fall.",
            attunement = true, plan = 10),
        item("ring_of_jumping", "Ring of Jumping", ItemRarity.UNCOMMON, "Ring",
            "You can cast Jump on yourself at will with this ring, without a spell slot or " +
                "components.",
            attunement = true, plan = 10),
        item("ring_mind_shielding", "Ring of Mind Shielding", ItemRarity.UNCOMMON, "Ring",
            "You are immune to magic that reads your thoughts, determines whether you are " +
                "lying, or detects your creature type. You can also become Invisible at will.",
            attunement = true, plan = 10),
        item("ring_of_protection", "Ring of Protection", ItemRarity.RARE, "Ring",
            "You gain a +1 bonus to Armor Class and to all saving throws while wearing this ring.",
            attunement = true, plan = 14),
        item("ring_of_the_ram", "Ring of the Ram", ItemRarity.RARE, "Ring",
            "This ring has 3 charges and regains 1d3 daily at dawn. As a Magic action you can " +
                "expend 1 to 3 charges to make a ranged spell attack against a creature within " +
                "60 feet, dealing 2d10 Force damage and pushing it 5 feet per charge spent.",
            attunement = true, plan = 14),
        item("ring_of_swimming", "Ring of Swimming", ItemRarity.UNCOMMON, "Ring",
            "You have a Swim Speed of 40 feet while wearing this ring.", plan = 6),
        item("ring_spell_storing", "Ring of Spell Storing", ItemRarity.RARE, "Ring",
            "The ring stores spells of a combined level of 5 or lower, cast into it by any " +
                "creature. You can then cast a stored spell from the ring using the original " +
                "caster's ability modifier and save DC.",
            attunement = true),
        item("ring_water_walking", "Ring of Water Walking", ItemRarity.UNCOMMON, "Ring",
            "You can stand on and move across any liquid surface as if it were solid ground.",
            plan = 6),
        item("spell_refueling_ring", "Spell-Refueling Ring", ItemRarity.RARE, "Ring",
            "Once per day as a Bonus Action you can recover one expended spell slot of level 3 " +
                "or lower.",
            attunement = true, plan = 6),
    )

    // ------------------------------------------------------------------ Wands, rods, staffs

    private val WANDS_AND_STAFFS = listOf(
        item("wand_magic_detection", "Wand of Magic Detection", ItemRarity.UNCOMMON, "Wand",
            "The wand has 3 charges and regains 1d3 daily at dawn. Expend a charge to cast " +
                "Detect Magic from it without using a spell slot.",
            weightLb = 1.0, plan = 2),
        item("wand_magic_missiles", "Wand of Magic Missiles", ItemRarity.UNCOMMON, "Wand",
            "The wand has 7 charges and regains 1d6 + 1 daily at dawn. Expend one or more " +
                "charges to cast Magic Missile at a level equal to the number of charges spent.",
            weightLb = 1.0, plan = 6),
        item("wand_of_secrets", "Wand of Secrets", ItemRarity.UNCOMMON, "Wand",
            "The wand has 3 charges and regains 1d3 daily at dawn. Expend a charge as a Magic " +
                "action to learn the location of secret doors and traps within 30 feet.",
            weightLb = 1.0, plan = 2),
        item("wand_of_web", "Wand of Web", ItemRarity.UNCOMMON, "Wand",
            "The wand has 7 charges and regains 1d6 + 1 daily at dawn. Expend a charge to cast " +
                "Web (save DC 15) from it.",
            attunement = true, attunementNote = "by a Spellcaster", weightLb = 1.0, plan = 6),
        item("wand_war_mage_1", "Wand of the War Mage, +1", ItemRarity.UNCOMMON, "Wand",
            "While holding this wand you gain a +1 bonus to spell attack rolls, and you ignore " +
                "Half Cover when making a spell attack.",
            attunement = true, attunementNote = "by a Spellcaster", weightLb = 1.0, plan = 2),
        item("wand_war_mage_2", "Wand of the War Mage, +2", ItemRarity.RARE, "Wand",
            "While holding this wand you gain a +2 bonus to spell attack rolls, and you ignore " +
                "Half Cover when making a spell attack.",
            attunement = true, attunementNote = "by a Spellcaster", weightLb = 1.0, plan = 10),
        item("immovable_rod", "Immovable Rod", ItemRarity.UNCOMMON, "Rod",
            "As a Utilize action you can press the button, fixing the rod in place until the " +
                "button is pressed again. It holds up to 8,000 pounds.",
            weightLb = 2.0),
        item("rod_of_the_pact_keeper_1", "Rod of the Pact Keeper, +1", ItemRarity.UNCOMMON, "Rod",
            "You gain a +1 bonus to spell attack rolls and to the saving throw DCs of your " +
                "Warlock spells, and you can regain one expended Pact Magic slot once per day.",
            attunement = true, attunementNote = "by a Warlock", weightLb = 2.0),
        item("staff_of_healing", "Staff of Healing", ItemRarity.RARE, "Staff",
            "The staff has 10 charges and regains 1d6 + 4 daily at dawn. You can expend charges " +
                "to cast Cure Wounds, Lesser Restoration, or Mass Cure Wounds from it.",
            attunement = true, attunementNote = "by a Spellcaster", weightLb = 4.0),
        item("staff_of_fire", "Staff of Fire", ItemRarity.VERY_RARE, "Staff",
            "You have Resistance to Fire damage while holding this staff. It has 10 charges and " +
                "regains 1d6 + 4 daily at dawn; you can expend charges to cast Burning Hands, " +
                "Fireball, or Wall of Fire from it.",
            attunement = true, attunementNote = "by a Spellcaster", weightLb = 4.0),
    )

    // ------------------------------------------------------------------ Wondrous items

    private val WONDROUS_ITEMS = listOf(
        item("alchemy_jug", "Alchemy Jug", ItemRarity.UNCOMMON, WONDROUS,
            "As a Magic action you can name one liquid — acid, beer, honey, mayonnaise, oil, " +
                "vinegar, fresh water, salt water, or wine — and the jug produces it. The " +
                "quantity depends on the liquid, and the jug can be used once per day.",
            weightLb = 12.0, plan = 2),
        item("amulet_of_health", "Amulet of Health", ItemRarity.RARE, WONDROUS,
            "Your Constitution score is 19 while you wear this amulet. It has no effect if your " +
                "Constitution is already 19 or higher.",
            attunement = true, weightLb = 1.0),
        item("bag_of_holding", "Bag of Holding", ItemRarity.UNCOMMON, WONDROUS,
            "The bag's interior is far larger than its outside, holding up to 500 pounds in 64 " +
                "cubic feet. It always weighs 15 pounds regardless of its contents. Overloading " +
                "it or piercing it destroys the bag and scatters what it held into the Astral Plane.",
            weightLb = 15.0, plan = 2),
        item("bag_of_beans", "Bag of Beans", ItemRarity.RARE, WONDROUS,
            "This bag holds 3d4 dry beans. Dumping beans out makes them explode in a 10-foot " +
                "Sphere; planting one produces a random wondrous — or dangerous — effect.",
            weightLb = 0.5),
        item("bag_of_tricks", "Bag of Tricks", ItemRarity.UNCOMMON, WONDROUS,
            "As a Magic action you can pull a fuzzy object from the bag and throw it up to 20 " +
                "feet, where it becomes a creature that acts as your ally until it drops to 0 " +
                "Hit Points or after 1 hour. Three uses per day.",
            weightLb = 0.5),
        item("boots_of_elvenkind", "Boots of Elvenkind", ItemRarity.UNCOMMON, WONDROUS,
            "Your steps make no sound regardless of the surface, and you have Advantage on " +
                "Dexterity (Stealth) checks made to move silently.",
            weightLb = 1.0, plan = 6),
        item("boots_of_speed", "Boots of Speed", ItemRarity.RARE, WONDROUS,
            "As a Bonus Action you can click the boots' heels together to double your Speed and " +
                "impose Disadvantage on Opportunity Attacks against you, for up to 10 minutes per day.",
            attunement = true, weightLb = 1.0),
        item("boots_striding_springing", "Boots of Striding and Springing", ItemRarity.UNCOMMON,
            WONDROUS,
            "Your Speed becomes 30 feet unless it was already higher, and it can't be reduced. " +
                "Your Long and High Jump distances are tripled.",
            attunement = true, weightLb = 1.0),
        item("boots_winding_path", "Boots of the Winding Path", ItemRarity.RARE, WONDROUS,
            "As a Bonus Action you can teleport back to a space you occupied earlier on the " +
                "same turn, up to 15 feet away.",
            attunement = true, weightLb = 1.0, plan = 6),
        item("bracers_of_defense", "Bracers of Defense", ItemRarity.RARE, WONDROUS,
            "While wearing these bracers and using no armor or Shield, you gain a +2 bonus to " +
                "Armor Class.",
            attunement = true, weightLb = 1.0),
        item("brooch_of_shielding", "Brooch of Shielding", ItemRarity.UNCOMMON, WONDROUS,
            "You have Resistance to Force damage and Immunity to damage from the Magic Missile " +
                "spell while wearing this brooch."),
        item("cap_of_water_breathing", "Cap of Water Breathing", ItemRarity.UNCOMMON, WONDROUS,
            "While underwater you can take a Magic action to create a bubble of air around your " +
                "head, letting you breathe normally. The bubble lasts until you leave the water " +
                "or dismiss it.",
            plan = 2),
        item("cloak_of_displacement", "Cloak of Displacement", ItemRarity.RARE, WONDROUS,
            "This cloak projects an illusion of you standing nearby, giving attack rolls against " +
                "you Disadvantage. The property stops working until the start of your next turn " +
                "whenever you take damage.",
            attunement = true, weightLb = 1.0),
        item("cloak_of_elvenkind", "Cloak of Elvenkind", ItemRarity.UNCOMMON, WONDROUS,
            "With the hood up, Wisdom (Perception) checks to see you have Disadvantage, and you " +
                "have Advantage on Dexterity (Stealth) checks made to hide.",
            attunement = true, weightLb = 1.0, plan = 6),
        item("cloak_of_protection", "Cloak of Protection", ItemRarity.UNCOMMON, WONDROUS,
            "You gain a +1 bonus to Armor Class and to all saving throws while wearing this cloak.",
            attunement = true, weightLb = 1.0),
        item("cloak_manta_ray", "Cloak of the Manta Ray", ItemRarity.UNCOMMON, WONDROUS,
            "With the hood up you can breathe underwater and gain a Swim Speed of 60 feet.",
            attunement = true, weightLb = 1.0, plan = 6),
        item("decanter_endless_water", "Decanter of Endless Water", ItemRarity.UNCOMMON, WONDROUS,
            "Speaking a command word makes the decanter pour out fresh or salt water — a stream, " +
                "a fountain, or a geyser powerful enough to knock a creature Prone.",
            weightLb = 2.0),
        item("driftglobe", "Driftglobe", ItemRarity.UNCOMMON, WONDROUS,
            "This glass sphere can cast Light or Daylight, and you can command it to hover in " +
                "the air and follow you at a distance of up to 60 feet.",
            weightLb = 1.0),
        item("eyes_of_charming", "Eyes of Charming", ItemRarity.UNCOMMON, WONDROUS,
            "These lenses have 3 charges and regain all of them daily at dawn. Expend a charge " +
                "as a Magic action to cast Charm Person (save DC 13) on a Humanoid within 30 feet.",
            attunement = true, plan = 6),
        item("eyes_minute_seeing", "Eyes of Minute Seeing", ItemRarity.UNCOMMON, WONDROUS,
            "You have Advantage on Intelligence (Investigation) checks that rely on sight while " +
                "searching an area or studying an object within 1 foot of you.",
            plan = 6),
        item("gauntlets_ogre_power", "Gauntlets of Ogre Power", ItemRarity.UNCOMMON, WONDROUS,
            "Your Strength score is 19 while you wear these gauntlets. They have no effect if " +
                "your Strength is already 19 or higher.",
            attunement = true, weightLb = 2.0),
        item("gloves_of_thievery", "Gloves of Thievery", ItemRarity.UNCOMMON, WONDROUS,
            "These invisible gloves grant a +5 bonus to Dexterity (Sleight of Hand) checks and " +
                "to checks made to pick locks and disarm traps.",
            plan = 6),
        item("goggles_of_night", "Goggles of Night", ItemRarity.UNCOMMON, WONDROUS,
            "While wearing these dark lenses you have Darkvision out to 60 feet, or an extra 60 " +
                "feet if you already have Darkvision.",
            plan = 2),
        item("hat_of_disguise", "Hat of Disguise", ItemRarity.UNCOMMON, WONDROUS,
            "While wearing this hat you can cast Disguise Self from it at will.",
            attunement = true),
        item("headband_of_intellect", "Headband of Intellect", ItemRarity.UNCOMMON, WONDROUS,
            "Your Intelligence score is 19 while you wear this headband. It has no effect if " +
                "your Intelligence is already 19 or higher.",
            attunement = true),
        item("helm_of_awareness", "Helm of Awareness", ItemRarity.RARE, WONDROUS,
            "You have Advantage on Initiative rolls while wearing this helm, and it can't be " +
                "removed against your will.",
            weightLb = 3.0, plan = 6),
        item("horn_of_blasting", "Horn of Blasting", ItemRarity.RARE, WONDROUS,
            "As a Magic action you can sound the horn, forcing each creature in a 30-foot Cone " +
                "to make a DC 15 Constitution saving throw, taking 5d6 Thunder damage and " +
                "possibly the Deafened condition.",
            weightLb = 2.0),
        item("immovable_lantern_revealing", "Lantern of Revealing", ItemRarity.UNCOMMON, WONDROUS,
            "While lit, this hooded lantern reveals Invisible creatures and objects within 30 " +
                "feet of it as translucent shapes.",
            weightLb = 2.0, plan = 6),
        item("manifold_tool", "Manifold Tool", ItemRarity.UNCOMMON, WONDROUS,
            "This set of Artisan's Tools can transform into any other set of Artisan's Tools as " +
                "a Bonus Action, and you gain a +1 bonus to ability checks made with it.",
            attunement = true, weightLb = 5.0, plan = 2),
        item("mind_sharpener", "Mind Sharpener", ItemRarity.UNCOMMON, WONDROUS,
            "This item has 4 charges and regains 1d4 daily at dawn. When you fail a Constitution " +
                "saving throw to maintain Concentration, you can expend a charge to succeed instead.",
            attunement = true, plan = 6),
        item("necklace_of_adaptation", "Necklace of Adaptation", ItemRarity.UNCOMMON, WONDROUS,
            "You can breathe normally in any environment, and you have Advantage on saving " +
                "throws against harmful gases and vapors.",
            attunement = true, weightLb = 1.0, plan = 6),
        item("pearl_of_power", "Pearl of Power", ItemRarity.UNCOMMON, WONDROUS,
            "Once per day as a Magic action you can recover one expended spell slot of level 3 " +
                "or lower.",
            attunement = true, attunementNote = "by a Spellcaster"),
        item("pipes_of_haunting", "Pipes of Haunting", ItemRarity.UNCOMMON, WONDROUS,
            "These pipes have 3 charges and regain all of them daily at dawn. Playing them " +
                "forces each creature within 30 feet to succeed on a DC 15 Wisdom saving throw " +
                "or have the Frightened condition for 1 minute.",
            weightLb = 2.0, plan = 6),
        item("portable_hole", "Portable Hole", ItemRarity.RARE, WONDROUS,
            "This cloth unfolds into a 6-foot-diameter circle that opens onto an extradimensional " +
                "space 10 feet deep. Folding the cloth closes the hole with its contents inside.",
            weightLb = 0.5),
        item("rope_of_climbing", "Rope of Climbing", ItemRarity.UNCOMMON, WONDROUS,
            "This 60-foot rope animates on command, moving toward a destination, fastening " +
                "itself, knotting to make climbing easier, or untying at your word.",
            weightLb = 3.0, plan = 2),
        item("sending_stones", "Sending Stones", ItemRarity.UNCOMMON, WONDROUS,
            "A matched pair of stones. Touching one and speaking the command word lets you cast " +
                "Sending to the holder of the other, once per day.",
            weightLb = 1.0, plan = 2),
        item("slippers_spider_climbing", "Slippers of Spider Climbing", ItemRarity.UNCOMMON,
            WONDROUS,
            "You can move up, down, and across vertical surfaces and along ceilings while " +
                "leaving your hands free, with a Climb Speed equal to your Speed.",
            attunement = true, weightLb = 0.5),
        item("weapon_of_warning", "Weapon of Warning", ItemRarity.UNCOMMON, "Weapon (any)",
            "While you hold this weapon, neither you nor your allies within 30 feet of you can " +
                "be surprised, and you have Advantage on Initiative rolls.",
            attunement = true, plan = 6),
        item("winged_boots", "Winged Boots", ItemRarity.UNCOMMON, WONDROUS,
            "You have a Fly Speed equal to your Speed while wearing these boots, for up to 4 " +
                "hours per day, spent in increments of at least 1 minute.",
            attunement = true, weightLb = 1.0),
    )

    // ------------------------------------------------------------------ Potions and scrolls

    private val POTIONS = listOf(
        item("potion_healing", "Potion of Healing", ItemRarity.COMMON, "Potion",
            "As a Bonus Action you can drink this potion or administer it to another creature " +
                "within 5 feet, regaining 2d4 + 2 Hit Points.",
            weightLb = 0.5),
        item("potion_greater_healing", "Potion of Greater Healing", ItemRarity.UNCOMMON, "Potion",
            "As a Bonus Action you can drink this potion or administer it to another creature " +
                "within 5 feet, regaining 4d4 + 4 Hit Points.",
            weightLb = 0.5),
        item("potion_superior_healing", "Potion of Superior Healing", ItemRarity.RARE, "Potion",
            "As a Bonus Action you can drink this potion or administer it to another creature " +
                "within 5 feet, regaining 8d4 + 8 Hit Points.",
            weightLb = 0.5),
        item("potion_supreme_healing", "Potion of Supreme Healing", ItemRarity.VERY_RARE, "Potion",
            "As a Bonus Action you can drink this potion or administer it to another creature " +
                "within 5 feet, regaining 10d4 + 20 Hit Points.",
            weightLb = 0.5),
        item("potion_climbing", "Potion of Climbing", ItemRarity.COMMON, "Potion",
            "For 1 hour after drinking this potion you gain a Climb Speed equal to your Speed " +
                "and have Advantage on Strength (Athletics) checks made to climb.",
            weightLb = 0.5),
        item("potion_fire_breath", "Potion of Fire Breath", ItemRarity.UNCOMMON, "Potion",
            "For 1 hour you can take a Magic action to exhale fire at a target within 30 feet, " +
                "which makes a DC 13 Dexterity saving throw and takes 4d6 Fire damage on a " +
                "failure. You can do this up to three times.",
            weightLb = 0.5),
        item("potion_giant_strength_hill", "Potion of Hill Giant Strength", ItemRarity.UNCOMMON,
            "Potion",
            "Your Strength score becomes 21 for 1 hour. The potion has no effect if your " +
                "Strength is already 21 or higher.",
            weightLb = 0.5),
        item("potion_invisibility", "Potion of Invisibility", ItemRarity.VERY_RARE, "Potion",
            "You have the Invisible condition for 1 hour. The effect ends early if you attack, " +
                "deal damage, or force a creature to make a saving throw.",
            weightLb = 0.5),
        item("potion_resistance", "Potion of Resistance", ItemRarity.UNCOMMON, "Potion",
            "For 1 hour you have Resistance to one type of damage, chosen when the potion is " +
                "created.",
            weightLb = 0.5),
        item("potion_speed", "Potion of Speed", ItemRarity.VERY_RARE, "Potion",
            "You gain the effect of the Haste spell for 1 minute, requiring no Concentration.",
            weightLb = 0.5),
        item("spell_scroll", "Spell Scroll", ItemRarity.COMMON, "Scroll",
            "A scroll bearing one spell. If the spell is on your spell list you can cast it from " +
                "the scroll without a spell slot or components; otherwise you must succeed on an " +
                "Intelligence (Arcana) check. The scroll crumbles once used. Its rarity rises " +
                "with the level of the spell it holds."),
    )

    // ------------------------------------------------------------------ More armor

    private val MORE_ARMOR = listOf(
        item("animated_shield", "Animated Shield", ItemRarity.VERY_RARE, "Armor (Shield)",
            "As a Bonus Action you can speak the command word and cause the Shield to animate, " +
                "leaping into the air and hovering to protect you as though you were wielding " +
                "it, leaving your hands free. It lasts 1 minute, until you use a Bonus Action " +
                "to end it, or until you have the Incapacitated condition or die.",
            attunement = true, weightLb = 6.0),
        item("armor_of_vulnerability", "Armor of Vulnerability", ItemRarity.RARE, "Armor (Plate)",
            "While wearing this armor you have Resistance to one of Bludgeoning, Piercing, or " +
                "Slashing damage. It is cursed: once identified by wearing it, you have " +
                "Vulnerability to the other two damage types and can't doff it without Remove Curse.",
            attunement = true, weightLb = 65.0),
        item("cast_off_armor", "Cast-Off Armor", ItemRarity.COMMON, "Armor (Light, Medium, Heavy)",
            "You can doff this armor as a Bonus Action."),
        item("demon_armor", "Demon Armor", ItemRarity.VERY_RARE, "Armor (Plate)",
            "You gain a +1 bonus to Armor Class, can understand and speak Abyssal, and your " +
                "Unarmed Strikes become claws that deal 1d8 Slashing damage with a +1 bonus. " +
                "The armor is cursed and binds you to it.",
            attunement = true, weightLb = 65.0),
        item("dragon_scale_mail", "Dragon Scale Mail", ItemRarity.VERY_RARE, "Armor (Scale Mail)",
            "You gain a +1 bonus to Armor Class, Advantage on saving throws against the " +
                "Frightful Presence and breath weapons of Dragons, and Resistance to the damage " +
                "type of the dragon the scales came from. You can also sense the direction of " +
                "the nearest such dragon within 30 miles, once per day.",
            attunement = true, weightLb = 45.0),
        item("dwarven_plate", "Dwarven Plate", ItemRarity.VERY_RARE, "Armor (Plate)",
            "You gain a +2 bonus to Armor Class. If an effect would move you against your will " +
                "along the ground, you can take a Reaction to reduce that distance by up to 10 feet.",
            weightLb = 65.0),
        item("efreeti_chain", "Efreeti Chain", ItemRarity.LEGENDARY, "Armor (Chain Mail)",
            "You gain a +3 bonus to Armor Class, Immunity to Fire damage, and the ability to " +
                "stand on and walk across molten rock as though it were solid ground. You can " +
                "also speak and understand Primordial.",
            attunement = true, weightLb = 55.0),
        item("elven_studded_leather", "Glamoured Studded Leather", ItemRarity.RARE,
            "Armor (Studded Leather)",
            "You gain a +1 bonus to Armor Class. As a Bonus Action you can make the armor " +
                "assume the appearance of any normal clothing or other armor you have seen.",
            weightLb = 13.0),
        item("plate_of_etherealness", "Plate Armor of Etherealness", ItemRarity.LEGENDARY,
            "Armor (Plate)",
            "While wearing this armor you can speak its command word as a Magic action to gain " +
                "the effect of the Etherealness spell for 10 minutes. It can't be used again " +
                "until the next dawn.",
            attunement = true, weightLb = 65.0),
        item("shield_missile_attraction", "Shield of Missile Attraction", ItemRarity.RARE,
            "Armor (Shield)",
            "You have Resistance to damage from ranged weapon attacks while wielding this " +
                "Shield. It is cursed: ranged attacks aimed at a target within 10 feet of you " +
                "are redirected to hit you instead.",
            attunement = true, weightLb = 6.0),
    )

    // ------------------------------------------------------------------ More weapons

    private val MORE_WEAPONS = listOf(
        item("arrow_of_slaying", "Arrow of Slaying", ItemRarity.VERY_RARE, "Ammunition",
            "This arrow is meant to slay a particular kind of creature. When it hits such a " +
                "creature, the target makes a DC 17 Constitution saving throw, taking an extra " +
                "6d10 Piercing damage on a failure or half as much on a success. The arrow's " +
                "magic is spent once it deals its damage."),
        item("dancing_sword", "Dancing Sword", ItemRarity.VERY_RARE, "Weapon (any Sword)",
            "As a Bonus Action you can toss this sword into the air and speak its command word. " +
                "It hovers and attacks a creature within 30 feet of it on your turn, using your " +
                "attack roll and ability modifier, for up to 4 of your turns.",
            attunement = true),
        item("dwarven_thrower", "Dwarven Thrower", ItemRarity.VERY_RARE, "Weapon (Warhammer)",
            "You gain a +3 bonus to attack and damage rolls, and the hammer gains the Thrown " +
                "property with a range of 20/60 feet, returning to your hand after a throw. A " +
                "thrown hit deals an extra 1d8 Force damage, or 2d8 against a Giant.",
            attunement = true, attunementNote = "by a Dwarf or a creature attuned to Dwarven magic",
            weightLb = 2.0),
        item("giant_slayer", "Giant Slayer", ItemRarity.RARE, "Weapon (any Axe or Sword)",
            "You gain a +1 bonus to attack and damage rolls. When you hit a Giant with it, the " +
                "Giant takes an extra 2d6 damage and must succeed on a DC 15 Strength saving " +
                "throw or have the Prone condition."),
        item("holy_avenger", "Holy Avenger", ItemRarity.LEGENDARY, "Weapon (any Sword)",
            "You gain a +3 bonus to attack and damage rolls. When you hit a Fiend or an Undead " +
                "with it, that creature takes an extra 2d10 Radiant damage. While holding the " +
                "sword you also create a 10-foot Emanation that gives you and your allies " +
                "Advantage on saves against spells and magical effects.",
            attunement = true, attunementNote = "by a Paladin"),
        item("javelin_of_lightning", "Javelin of Lightning", ItemRarity.UNCOMMON,
            "Weapon (Javelin)",
            "As a Bonus Action you can turn the javelin into a bolt of lightning. Hurled at a " +
                "target within 120 feet, it forms a 5-foot-wide, 120-foot-long Line; each " +
                "creature in it makes a DC 13 Dexterity saving throw, taking 4d6 Lightning " +
                "damage on a failure or half as much on a success. It can't be used again until " +
                "the next dawn.",
            weightLb = 2.0),
        item("luck_blade", "Luck Blade", ItemRarity.LEGENDARY, "Weapon (any Sword)",
            "You gain a +1 bonus to attack and damage rolls and to saving throws. Luck: you can " +
                "reroll one failed attack roll or saving throw per day. Wish: the blade holds " +
                "1d4 - 1 charges of the Wish spell, expended permanently.",
            attunement = true),
        item("mace_of_disruption", "Mace of Disruption", ItemRarity.RARE, "Weapon (Mace)",
            "When you hit a Fiend or an Undead with this mace, it takes an extra 2d6 Radiant " +
                "damage and must succeed on a DC 15 Wisdom saving throw or have the Frightened " +
                "condition. If the hit reduces it below 25 Hit Points, it is destroyed instead.",
            attunement = true, weightLb = 4.0),
        item("mace_of_smiting", "Mace of Smiting", ItemRarity.RARE, "Weapon (Mace)",
            "You gain a +1 bonus to attack and damage rolls, rising to +3 against Constructs. " +
                "A Critical Hit against a Construct deals an extra 4d6 Bludgeoning damage, or " +
                "destroys the Construct if the hit leaves it below 25 Hit Points.",
            weightLb = 4.0),
        item("mace_of_terror", "Mace of Terror", ItemRarity.RARE, "Weapon (Mace)",
            "The mace has 3 charges and regains 1d3 daily at dawn. As a Magic action you can " +
                "expend a charge to radiate a 30-foot Emanation of terror; each creature of " +
                "your choice in it must succeed on a DC 15 Wisdom saving throw or have the " +
                "Frightened condition for 1 minute.",
            attunement = true, weightLb = 4.0),
        item("moon_touched_sword", "Moon-Touched Sword", ItemRarity.COMMON, "Weapon (any Sword)",
            "In darkness, the unsheathed blade sheds Bright Light in a 15-foot radius and Dim " +
                "Light for an additional 15 feet."),
        item("nine_lives_stealer", "Nine Lives Stealer", ItemRarity.VERY_RARE,
            "Weapon (any Sword)",
            "You gain a +1 bonus to attack and damage rolls. The sword has 1d8 + 1 charges; " +
                "when you score a Critical Hit against a creature with fewer than 100 Hit " +
                "Points, it must succeed on a DC 15 Constitution saving throw or die, expending " +
                "a charge.",
            attunement = true),
        item("oathbow", "Oathbow", ItemRarity.VERY_RARE, "Weapon (Longbow)",
            "As a Magic action you can name a target as your sworn enemy. Your attacks against " +
                "it have no long-range Disadvantage and deal an extra 3d6 Piercing damage on a " +
                "hit, but you have Disadvantage on attacks against every other creature until " +
                "the oath ends.",
            attunement = true, weightLb = 2.0),
        item("scimitar_of_speed", "Scimitar of Speed", ItemRarity.VERY_RARE, "Weapon (Scimitar)",
            "You gain a +2 bonus to attack and damage rolls, and you can make one attack with " +
                "it as a Bonus Action on each of your turns.",
            attunement = true, weightLb = 3.0),
        item("sword_of_life_stealing", "Sword of Life Stealing", ItemRarity.RARE,
            "Weapon (any Sword)",
            "When you roll a 20 on an attack roll with this sword, the target takes an extra " +
                "3d6 Necrotic damage if it isn't a Construct or an Undead, and you gain " +
                "Temporary Hit Points equal to that extra damage.",
            attunement = true),
        item("sword_of_sharpness", "Sword of Sharpness", ItemRarity.VERY_RARE,
            "Weapon (any Slashing Sword)",
            "When you hit an object with this sword, the hit is a Critical Hit. When you roll a " +
                "20 on an attack roll against a creature, it takes an extra 4d6 Slashing damage " +
                "and you can speak the command word to make the blade shed Bright Light.",
            attunement = true),
        item("sword_of_wounding", "Sword of Wounding", ItemRarity.RARE, "Weapon (any Sword)",
            "A creature hit by this sword takes 1d4 Necrotic damage at the start of each of its " +
                "turns as the wound refuses to close, and its Hit Point maximum is reduced by " +
                "that amount until it finishes a Short or Long Rest.",
            attunement = true),
        item("trident_of_fish_command", "Trident of Fish Command", ItemRarity.UNCOMMON,
            "Weapon (Trident)",
            "The trident has 3 charges and regains 1d3 daily at dawn. Expend a charge to cast " +
                "Dominate Beast (save DC 15) from it, targeting a Beast with a Swim Speed.",
            attunement = true, weightLb = 4.0),
        item("vorpal_sword", "Vorpal Sword", ItemRarity.LEGENDARY, "Weapon (any Slashing Sword)",
            "You gain a +3 bonus to attack and damage rolls, and the sword ignores Resistance " +
                "to Slashing damage. When you roll a 20 on an attack roll against a creature " +
                "with at least one head, you cut one of its heads off, killing it outright " +
                "unless it is immune to Slashing damage, has no head, or needs no head to live.",
            attunement = true),
    )

    // ------------------------------------------------------------------ More rings

    private val MORE_RINGS = listOf(
        item("ring_animal_influence", "Ring of Animal Influence", ItemRarity.RARE, "Ring",
            "The ring has 3 charges and regains all of them daily at dawn. Expend a charge to " +
                "cast Animal Friendship, Fear (Beasts only), or Speak with Animals from it."),
        item("ring_djinni_summoning", "Ring of Djinni Summoning", ItemRarity.LEGENDARY, "Ring",
            "While wearing this ring you can summon a djinni bound to it, which appears in an " +
                "unoccupied space within 120 feet and obeys your commands for up to 1 hour, " +
                "once per day.",
            attunement = true),
        item("ring_of_evasion", "Ring of Evasion", ItemRarity.RARE, "Ring",
            "The ring has 3 charges and regains 1d3 daily at dawn. When you fail a Dexterity " +
                "saving throw you can expend a charge to succeed instead.",
            attunement = true),
        item("ring_of_invisibility", "Ring of Invisibility", ItemRarity.LEGENDARY, "Ring",
            "As a Magic action you can give yourself the Invisible condition, which lasts until " +
                "you use another Magic action to end it, until you attack or cast a spell, or " +
                "until you doff the ring.",
            attunement = true),
        item("ring_of_regeneration", "Ring of Regeneration", ItemRarity.VERY_RARE, "Ring",
            "You regain 1d6 Hit Points every 10 minutes while you have at least 1 Hit Point. If " +
                "you lose a body part, it regrows and returns to full function after 1d6 + 1 days.",
            attunement = true),
        item("ring_of_resistance", "Ring of Resistance", ItemRarity.RARE, "Ring",
            "You have Resistance to one damage type while wearing this ring. The type is set by " +
                "the gem set into it — sapphire for Acid, jade for Poison, ruby for Fire, and so on.",
            attunement = true),
        item("ring_shooting_stars", "Ring of Shooting Stars", ItemRarity.VERY_RARE, "Ring",
            "In Dim Light or Darkness you can cast Dancing Lights and Light at will, and the " +
                "ring's 6 charges power Faerie Fire, Ball Lightning, and Shooting Stars.",
            attunement = true, attunementNote = "outdoors at night"),
        item("ring_spell_turning", "Ring of Spell Turning", ItemRarity.LEGENDARY, "Ring",
            "You have Advantage on saving throws against spells targeting only you. If you roll " +
                "a 20 on the save and the spell is level 7 or lower, it has no effect on you and " +
                "instead targets the caster.",
            attunement = true),
        item("ring_of_telekinesis", "Ring of Telekinesis", ItemRarity.VERY_RARE, "Ring",
            "You can cast Telekinesis at will while wearing this ring, but you can target only " +
                "objects that aren't being worn or carried.",
            attunement = true),
        item("ring_three_wishes", "Ring of Three Wishes", ItemRarity.LEGENDARY, "Ring",
            "The ring has 3 charges. You can expend one to cast Wish from it. The ring becomes " +
                "nonmagical once all three are spent."),
        item("ring_of_warmth", "Ring of Warmth", ItemRarity.UNCOMMON, "Ring",
            "You have Resistance to Cold damage, and you and your clothing are unharmed by " +
                "temperatures as low as -50 degrees Fahrenheit.",
            attunement = true),
        item("ring_x_ray_vision", "Ring of X-ray Vision", ItemRarity.RARE, "Ring",
            "As a Magic action you can see through solid objects out to 30 feet for 1 minute. " +
                "Using it again before a Long Rest forces a DC 15 Constitution saving throw or " +
                "you gain 1 Exhaustion level.",
            attunement = true),
    )

    // ------------------------------------------------------------- More wands, rods, staffs

    private val MORE_WANDS_AND_STAFFS = listOf(
        item("wand_of_binding", "Wand of Binding", ItemRarity.RARE, "Wand",
            "The wand has 7 charges and regains 1d6 + 1 daily at dawn. Expend charges to cast " +
                "Hold Monster or Hold Person, or as a Reaction to give a creature Disadvantage " +
                "on a check to escape a Grappled or Restrained condition you imposed.",
            attunement = true, attunementNote = "by a Spellcaster", weightLb = 1.0),
        item("wand_enemy_detection", "Wand of Enemy Detection", ItemRarity.RARE, "Wand",
            "The wand has 7 charges and regains 1d6 + 1 daily at dawn. Expend a charge to sense " +
                "the direction of every Hostile creature within 60 feet for 1 minute, even " +
                "those Invisible or disguised.",
            attunement = true, weightLb = 1.0),
        item("wand_of_fear", "Wand of Fear", ItemRarity.RARE, "Wand",
            "The wand has 7 charges and regains 1d6 + 1 daily at dawn. Expend a charge to make " +
                "a creature within 60 feet save against the Frightened condition, or to project " +
                "a 60-foot Cone of terror.",
            attunement = true, weightLb = 1.0),
        item("wand_of_fireballs", "Wand of Fireballs", ItemRarity.RARE, "Wand",
            "The wand has 7 charges and regains 1d6 + 1 daily at dawn. Expend one or more " +
                "charges to cast Fireball (save DC 15) at level 3, plus one level per extra " +
                "charge spent.",
            attunement = true, attunementNote = "by a Spellcaster", weightLb = 1.0),
        item("wand_lightning_bolts", "Wand of Lightning Bolts", ItemRarity.RARE, "Wand",
            "The wand has 7 charges and regains 1d6 + 1 daily at dawn. Expend one or more " +
                "charges to cast Lightning Bolt (save DC 15) at level 3, plus one level per " +
                "extra charge spent.",
            attunement = true, attunementNote = "by a Spellcaster", weightLb = 1.0),
        item("wand_of_paralysis", "Wand of Paralysis", ItemRarity.RARE, "Wand",
            "The wand has 7 charges and regains 1d6 + 1 daily at dawn. Expend a charge to force " +
                "a creature within 60 feet to succeed on a DC 15 Constitution saving throw or " +
                "have the Paralyzed condition for 1 minute.",
            attunement = true, attunementNote = "by a Spellcaster", weightLb = 1.0),
        item("wand_of_polymorph", "Wand of Polymorph", ItemRarity.VERY_RARE, "Wand",
            "The wand has 7 charges and regains 1d6 + 1 daily at dawn. Expend a charge to cast " +
                "Polymorph (save DC 15) from it.",
            attunement = true, attunementNote = "by a Spellcaster", weightLb = 1.0),
        item("wand_of_wonder", "Wand of Wonder", ItemRarity.RARE, "Wand",
            "The wand has 7 charges and regains 1d6 + 1 daily at dawn. Expend a charge and " +
                "choose a target; roll on the Wand of Wonder table for an effect that may be " +
                "Slow, Gust of Wind, a rain of butterflies, a summoned rhinoceros, or something " +
                "else entirely.",
            attunement = true, attunementNote = "by a Spellcaster", weightLb = 1.0),
        item("rod_of_absorption", "Rod of Absorption", ItemRarity.VERY_RARE, "Rod",
            "As a Reaction you can absorb a spell that targets only you, negating it and storing " +
                "its levels, up to 50 total. You can then spend those levels to cast your own " +
                "spells without expending slots.",
            attunement = true, weightLb = 2.0),
        item("rod_of_alertness", "Rod of Alertness", ItemRarity.VERY_RARE, "Rod",
            "You gain Advantage on Wisdom (Perception) checks and Initiative rolls, can cast " +
                "Detect Evil and Good, Detect Magic, Detect Poison and Disease, or See " +
                "Invisibility from it, and can plant it to create a protective aura.",
            attunement = true, weightLb = 2.0),
        item("rod_of_lordly_might", "Rod of Lordly Might", ItemRarity.LEGENDARY, "Rod",
            "This rod functions as a Mace with a +3 bonus to attack and damage rolls, and its " +
                "six buttons transform it into a Flame Tongue, a battleaxe, a spear, a climbing " +
                "pole, a compass, or a ladder.",
            attunement = true, weightLb = 2.0),
        item("rod_of_rulership", "Rod of Rulership", ItemRarity.RARE, "Rod",
            "As a Magic action you can force each creature of your choice within 120 feet to " +
                "succeed on a DC 15 Wisdom saving throw or have the Charmed condition for 8 " +
                "hours, once per day.",
            attunement = true, weightLb = 2.0),
        item("rod_of_security", "Rod of Security", ItemRarity.VERY_RARE, "Rod",
            "As a Magic action you and up to 199 others can be transported to an idyllic " +
                "demiplane for up to 200 days divided among the travelers, where you are " +
                "nourished and can't age.",
            weightLb = 2.0),
        item("staff_of_charming", "Staff of Charming", ItemRarity.RARE, "Staff",
            "The staff has 10 charges and regains 1d8 + 2 daily at dawn. Expend a charge to " +
                "cast Charm Person, Command, or Comprehend Languages, or as a Reaction to " +
                "reflect an enchantment cast on you back at its caster.",
            attunement = true, attunementNote = "by a Spellcaster", weightLb = 4.0),
        item("staff_of_frost", "Staff of Frost", ItemRarity.VERY_RARE, "Staff",
            "You have Resistance to Cold damage while holding this staff. It has 10 charges and " +
                "regains 1d6 + 4 daily at dawn; you can expend charges to cast Cone of Cold, " +
                "Fog Cloud, Ice Storm, or Wall of Ice from it.",
            attunement = true, attunementNote = "by a Spellcaster", weightLb = 4.0),
        item("staff_of_power", "Staff of Power", ItemRarity.VERY_RARE, "Staff",
            "You gain a +2 bonus to Armor Class, saving throws, and spell attack rolls. It has " +
                "20 charges powering Cone of Cold, Fireball, Globe of Invulnerability, Hold " +
                "Monster, Levitate, Lightning Bolt, Magic Missile, Ray of Enfeeblement, and " +
                "Wall of Force, and it can be broken for a devastating retributive strike.",
            attunement = true, attunementNote = "by a Sorcerer, Warlock, or Wizard", weightLb = 4.0),
        item("staff_of_striking", "Staff of Striking", ItemRarity.VERY_RARE, "Staff",
            "This quarterstaff grants a +3 bonus to attack and damage rolls. It has 10 charges " +
                "and regains 1d6 + 4 daily at dawn; you can expend up to 3 charges on a hit to " +
                "deal an extra 1d6 Force damage per charge.",
            attunement = true, weightLb = 4.0),
        item("staff_swarming_insects", "Staff of Swarming Insects", ItemRarity.RARE, "Staff",
            "The staff has 10 charges and regains 1d6 + 4 daily at dawn. Expend charges to cast " +
                "Giant Insect or Insect Plague, or to surround yourself with a swarm that gives " +
                "attackers Disadvantage.",
            attunement = true, attunementNote = "by a Spellcaster", weightLb = 4.0),
        item("staff_of_the_magi", "Staff of the Magi", ItemRarity.LEGENDARY, "Staff",
            "You gain a +2 bonus to spell attack rolls and can absorb spells targeting only you " +
                "to regain charges. Its 50 charges power a long list of spells from Arcane Lock " +
                "to Plane Shift, and it can be broken for a retributive strike.",
            attunement = true, attunementNote = "by a Sorcerer, Warlock, or Wizard", weightLb = 4.0),
        item("staff_of_the_python", "Staff of the Python", ItemRarity.UNCOMMON, "Staff",
            "As a Magic action you can throw the staff to the ground, where it becomes a Giant " +
                "Constrictor Snake under your control that acts on your Initiative.",
            attunement = true, attunementNote = "by a Spellcaster", weightLb = 4.0),
        item("staff_of_the_woodlands", "Staff of the Woodlands", ItemRarity.RARE, "Staff",
            "You gain a +2 bonus to spell attack rolls, and the staff's 10 charges power Animal " +
                "Friendship, Awaken, Barkskin, Locate Animals or Plants, Pass without Trace, " +
                "Speak with Animals, Speak with Plants, and Wall of Thorns. It can also be " +
                "planted to grow into a tree.",
            attunement = true, attunementNote = "by a Druid", weightLb = 4.0),
        item("staff_thunder_lightning", "Staff of Thunder and Lightning", ItemRarity.VERY_RARE,
            "Staff",
            "This quarterstaff grants a +2 bonus to attack and damage rolls and holds five " +
                "daily powers: Lightning on a hit, Thunder on a hit, a Lightning Strike Line, a " +
                "Thunderclap, and both at once.",
            attunement = true, weightLb = 4.0),
        item("staff_of_withering", "Staff of Withering", ItemRarity.RARE, "Staff",
            "This quarterstaff has 3 charges and regains 1d3 daily at dawn. On a hit you can " +
                "expend a charge to deal an extra 2d10 Necrotic damage and impose Disadvantage " +
                "on the target's Strength or Constitution checks and saves.",
            attunement = true, attunementNote = "by a Cleric, Druid, or Warlock", weightLb = 4.0),
        item("tentacle_rod", "Tentacle Rod", ItemRarity.RARE, "Rod",
            "As a Magic action you can lash out with the rod's three tentacles, each making a " +
                "melee attack against a creature within 15 feet for 1d6 Bludgeoning damage. A " +
                "creature hit by all three has its Speed halved and takes Disadvantage on " +
                "attacks and Dexterity saves.",
            attunement = true, weightLb = 2.0),
    )

    // ------------------------------------------------------------------ More wondrous items

    private val MORE_WONDROUS = listOf(
        item("amulet_proof_detection", "Amulet of Proof against Detection and Location",
            ItemRarity.UNCOMMON, WONDROUS,
            "You are hidden from divination magic: you can't be targeted by it or perceived " +
                "through magical scrying sensors.",
            attunement = true, weightLb = 1.0),
        item("amulet_of_the_planes", "Amulet of the Planes", ItemRarity.VERY_RARE, WONDROUS,
            "As a Magic action you can name a location on another plane of existence and make a " +
                "DC 15 Intelligence check; on a success you cast Plane Shift, and on a failure " +
                "you and each creature travelling with you are transported to a random destination.",
            attunement = true, weightLb = 1.0),
        item("apparatus_of_kwalish", "Apparatus of Kwalish", ItemRarity.LEGENDARY, WONDROUS,
            "This sealed iron barrel unfolds into a crab-like vehicle for two, with AC 20, 200 " +
                "Hit Points, pincers, and the ability to move on land or underwater.",
            weightLb = 500.0),
        item("bag_of_devouring", "Bag of Devouring", ItemRarity.VERY_RARE, WONDROUS,
            "This bag looks like a Bag of Holding, but it is the maw of an extradimensional " +
                "creature. Anything placed inside is devoured, and a creature reaching in must " +
                "save or be pulled in and destroyed.",
            weightLb = 15.0),
        item("bead_of_force", "Bead of Force", ItemRarity.RARE, WONDROUS,
            "As a Magic action you can throw this bead up to 60 feet, where it detonates for " +
                "5d4 Force damage in a 10-foot Sphere and traps failed saves in a translucent " +
                "sphere of force for 1 minute."),
        item("belt_of_dwarvenkind", "Belt of Dwarvenkind", ItemRarity.RARE, WONDROUS,
            "Your Constitution score increases by 2 (to a maximum of 20), you gain Advantage on " +
                "Charisma (Persuasion) checks with Dwarves, Darkvision out to 60 feet, and " +
                "Advantage on saving throws against poison.",
            attunement = true, weightLb = 1.0),
        item("belt_of_giant_strength", "Belt of Giant Strength", ItemRarity.RARE, WONDROUS,
            "Your Strength score becomes a fixed value set by the belt's kind — 21 for a Hill " +
                "Giant Belt, up to 29 for a Storm Giant Belt. It has no effect if your Strength " +
                "is already that high.",
            attunement = true, weightLb = 1.0),
        item("boots_of_levitation", "Boots of Levitation", ItemRarity.RARE, WONDROUS,
            "You can cast Levitate on yourself at will while wearing these boots.",
            attunement = true, weightLb = 1.0),
        item("boots_of_the_winterlands", "Boots of the Winterlands", ItemRarity.UNCOMMON, WONDROUS,
            "You have Resistance to Cold damage, ignore Difficult Terrain created by ice or " +
                "snow, and can tolerate temperatures as low as -50 degrees Fahrenheit.",
            attunement = true, weightLb = 1.0),
        item("bowl_water_elementals", "Bowl of Commanding Water Elementals", ItemRarity.RARE,
            WONDROUS,
            "Filling this bowl with water and taking a Magic action summons a Water Elemental " +
                "that acts as your ally, once per day.",
            weightLb = 3.0),
        item("bracers_of_archery", "Bracers of Archery", ItemRarity.UNCOMMON, WONDROUS,
            "You have proficiency with the Longbow and Shortbow, and you gain a +2 bonus to " +
                "damage rolls with those weapons.",
            attunement = true, weightLb = 1.0),
        item("brazier_fire_elementals", "Brazier of Commanding Fire Elementals", ItemRarity.RARE,
            WONDROUS,
            "Lighting a fire in this brazier and taking a Magic action summons a Fire Elemental " +
                "that acts as your ally, once per day.",
            weightLb = 5.0),
        item("broom_of_flying", "Broom of Flying", ItemRarity.UNCOMMON, WONDROUS,
            "The broom has a Fly Speed of 50 feet while you ride it, or 30 feet if you weigh " +
                "more than 200 pounds with gear. It can also fly to you on command from up to " +
                "1 mile away.",
            weightLb = 3.0),
        item("candle_of_invocation", "Candle of Invocation", ItemRarity.VERY_RARE, WONDROUS,
            "Burning this candle for 4 hours lets you cast one spell you have prepared without " +
                "expending a spell slot, and while it burns it creates an aura matching a " +
                "particular alignment.",
            attunement = true),
        item("cape_of_the_mountebank", "Cape of the Mountebank", ItemRarity.RARE, WONDROUS,
            "You can cast Dimension Door from this cape once per day. When you do, you and the " +
                "space you left are wreathed in smoke that lightly obscures the area.",
            weightLb = 1.0),
        item("carpet_of_flying", "Carpet of Flying", ItemRarity.VERY_RARE, WONDROUS,
            "As a Magic action you can make the carpet hover and fly, carrying weight and at a " +
                "speed determined by its size — up to 80 feet for the smallest.",
            weightLb = 25.0),
        item("censer_air_elementals", "Censer of Controlling Air Elementals", ItemRarity.RARE,
            WONDROUS,
            "Burning incense in this censer and taking a Magic action summons an Air Elemental " +
                "that acts as your ally, once per day.",
            weightLb = 1.0),
        item("chime_of_opening", "Chime of Opening", ItemRarity.RARE, WONDROUS,
            "The chime has 10 charges. Striking it as a Magic action opens one lock, latch, " +
                "lid, or door within 120 feet, including one sealed by Arcane Lock. It becomes " +
                "nonmagical when all charges are spent.",
            weightLb = 1.0),
        item("circlet_of_blasting", "Circlet of Blasting", ItemRarity.UNCOMMON, WONDROUS,
            "You can cast Scorching Ray from this circlet once per day, with a +5 bonus to its " +
                "attack rolls.",
            weightLb = 1.0),
        item("cloak_of_arachnida", "Cloak of Arachnida", ItemRarity.VERY_RARE, WONDROUS,
            "You have Resistance to Poison damage, a Climb Speed equal to your Speed, can move " +
                "across webs unhindered, and can cast Web from the cloak once per day.",
            attunement = true, weightLb = 1.0),
        item("cloak_of_invisibility", "Cloak of Invisibility", ItemRarity.LEGENDARY, WONDROUS,
            "The cloak has 3 charges. Pulling the hood up as a Magic action expends a charge and " +
                "gives you the Invisible condition for up to 1 hour. It regains 1d3 charges " +
                "daily at dawn.",
            attunement = true, weightLb = 1.0),
        item("cloak_of_the_bat", "Cloak of the Bat", ItemRarity.RARE, WONDROUS,
            "You have Advantage on Dexterity (Stealth) checks, and in Dim Light or Darkness you " +
                "can grip the cloak's edges to gain a Fly Speed of 40 feet or turn into a bat.",
            attunement = true, weightLb = 1.0),
        item("crystal_ball", "Crystal Ball", ItemRarity.VERY_RARE, WONDROUS,
            "While touching this orb you can cast Scrying (save DC 17) from it. Rarer versions " +
                "also grant Truesight, telepathy, or the ability to cast Detect Thoughts through " +
                "the sensor.",
            attunement = true, weightLb = 3.0),
        item("cube_of_force", "Cube of Force", ItemRarity.RARE, WONDROUS,
            "The cube has 36 charges. Pressing its faces creates a barrier around you that keeps " +
                "out gases, living matter, nonliving matter, spells, or all of it, at a " +
                "different charge cost each.",
            attunement = true, weightLb = 0.5),
        item("cubic_gate", "Cubic Gate", ItemRarity.LEGENDARY, WONDROUS,
            "Each of the cube's six faces is keyed to a plane of existence. It has 3 charges " +
                "and regains 1d3 daily at dawn; pressing a face casts Gate or Plane Shift to " +
                "that plane.",
            weightLb = 0.5),
        item("deck_of_illusions", "Deck of Illusions", ItemRarity.UNCOMMON, WONDROUS,
            "Drawing a card at random and throwing it creates a moving, talking illusion of the " +
                "creature depicted, which lasts until dispelled."),
        item("deck_of_many_things", "Deck of Many Things", ItemRarity.LEGENDARY, WONDROUS,
            "Drawing from this deck immediately produces one of twenty-two effects, from a " +
                "windfall of treasure or a wish to imprisonment, the loss of your soul, or the " +
                "arrival of an avatar of death."),
        item("dimensional_shackles", "Dimensional Shackles", ItemRarity.RARE, WONDROUS,
            "A creature wearing these shackles can't use any method of extradimensional " +
                "movement, including teleportation or travel to another plane.",
            weightLb = 6.0),
        item("dust_of_disappearance", "Dust of Disappearance", ItemRarity.UNCOMMON, WONDROUS,
            "Thrown into the air, this dust gives you and every creature and object within 10 " +
                "feet the Invisible condition for 2d4 minutes."),
        item("dust_of_dryness", "Dust of Dryness", ItemRarity.UNCOMMON, WONDROUS,
            "A pinch of this dust absorbs 15 cubic feet of water, becoming a marble-sized pellet " +
                "that releases the water again when crushed."),
        item("dust_of_sneezing", "Dust of Sneezing and Choking", ItemRarity.UNCOMMON, WONDROUS,
            "Thrown into the air, this dust forces each creature within 30 feet that needs to " +
                "breathe to succeed on a DC 15 Constitution saving throw or have the " +
                "Incapacitated condition while suffocating."),
        item("efficient_quiver", "Efficient Quiver", ItemRarity.UNCOMMON, WONDROUS,
            "This quiver has three compartments, each opening onto an extradimensional space: " +
                "one for arrows and bolts, one for javelins and similar shafts, and one for " +
                "bows, staffs, and spears.",
            weightLb = 2.0),
        item("efreeti_bottle", "Efreeti Bottle", ItemRarity.VERY_RARE, WONDROUS,
            "Opening the bottle releases an efreeti, which may attack you, grant you three " +
                "wishes, or serve you for 1 hour depending on the roll.",
            weightLb = 1.0),
        item("elemental_gem", "Elemental Gem", ItemRarity.UNCOMMON, WONDROUS,
            "Breaking this gem as a Magic action summons an elemental of the matching type, " +
                "which acts as your ally for 1 hour. The gem is destroyed in the process."),
        item("elixir_of_health", "Elixir of Health", ItemRarity.RARE, "Potion",
            "Drinking this elixir cures any disease afflicting you and removes the Blinded, " +
                "Deafened, Paralyzed, and Poisoned conditions.",
            weightLb = 0.5),
        item("eversmoking_bottle", "Eversmoking Bottle", ItemRarity.UNCOMMON, WONDROUS,
            "Opening this bottle pours out a cloud of smoke that heavily obscures a 60-foot " +
                "radius, growing 10 feet each round it stays open.",
            weightLb = 1.0),
        item("eyes_of_the_eagle", "Eyes of the Eagle", ItemRarity.UNCOMMON, WONDROUS,
            "You have Advantage on Wisdom (Perception) checks that rely on sight, and in clear " +
                "conditions you can make out fine detail at distances that would be a blur to " +
                "anyone else.",
            attunement = true),
        item("feather_token", "Feather Token", ItemRarity.RARE, WONDROUS,
            "A tiny feather with a single use, which depending on its kind becomes an anchor, a " +
                "bird to carry you, a fan to drive a boat, a fully rigged ship, a swan boat, a " +
                "tree, or a whip."),
        item("figurine_wondrous_power", "Figurine of Wondrous Power", ItemRarity.RARE, WONDROUS,
            "This small statuette becomes a living creature — a bronze griffon, an ebony fly, a " +
                "golden lion, a marble elephant, an obsidian steed, an onyx dog, a serpentine " +
                "owl, or a silver raven — that obeys you until its time runs out."),
        item("folding_boat", "Folding Boat", ItemRarity.RARE, WONDROUS,
            "This wooden box unfolds on command into a 10-foot boat or a 24-foot ship, and " +
                "folds back down when you speak a second command word.",
            weightLb = 4.0),
        item("gem_of_brightness", "Gem of Brightness", ItemRarity.UNCOMMON, WONDROUS,
            "The gem has 50 charges. Expend charges to make it shed Bright Light, fire a beam " +
                "that can Blind a creature, or flare in a 30-foot Cone that Blinds every " +
                "creature that fails a save.",
            weightLb = 1.0),
        item("gem_of_seeing", "Gem of Seeing", ItemRarity.RARE, WONDROUS,
            "The gem has 3 charges and regains 1d3 daily at dawn. Expend a charge to gain " +
                "Truesight out to 120 feet while looking through it, for 10 minutes.",
            attunement = true, weightLb = 1.0),
        item("gloves_missile_snaring", "Gloves of Missile Snaring", ItemRarity.UNCOMMON, WONDROUS,
            "When you are hit by a ranged weapon attack, you can take a Reaction to reduce the " +
                "damage by 1d10 plus your Dexterity modifier, and to catch the missile if you " +
                "reduce the damage to 0.",
            attunement = true),
        item("gloves_swimming_climbing", "Gloves of Swimming and Climbing", ItemRarity.UNCOMMON,
            WONDROUS,
            "Climbing and swimming don't cost you extra movement, and you gain a +5 bonus to " +
                "Strength (Athletics) checks made to climb or swim.",
            attunement = true),
        item("hat_of_many_spells", "Hat of Many Spells", ItemRarity.RARE, WONDROUS,
            "This hat holds a number of spells appropriate to its rarity, each castable once " +
                "per day without a spell slot.",
            attunement = true),
        item("helm_of_brilliance", "Helm of Brilliance", ItemRarity.VERY_RARE, WONDROUS,
            "Set with diamonds, rubies, fire opals, and opals, this helm lets you cast Daylight, " +
                "Fireball, Prismatic Spray, and Wall of Fire by expending its gems, and it makes " +
                "your weapons blaze with Radiant damage against Undead.",
            attunement = true, weightLb = 3.0),
        item("helm_comprehending_languages", "Helm of Comprehending Languages",
            ItemRarity.UNCOMMON, WONDROUS,
            "You can cast Comprehend Languages from this helm at will.",
            weightLb = 3.0),
        item("helm_of_telepathy", "Helm of Telepathy", ItemRarity.UNCOMMON, WONDROUS,
            "You can cast Detect Thoughts from this helm, and while the spell is active you can " +
                "cast Suggestion through it at the creature whose thoughts you are reading.",
            attunement = true, weightLb = 3.0),
        item("helm_of_teleportation", "Helm of Teleportation", ItemRarity.RARE, WONDROUS,
            "This helm has 3 charges and regains 1d3 daily at dawn. Expend a charge to cast " +
                "Teleport from it.",
            attunement = true, weightLb = 3.0),
        item("horn_of_valhalla", "Horn of Valhalla", ItemRarity.RARE, WONDROUS,
            "Blowing this horn summons 3d4 + 3 berserkers who fight for you for 1 hour. Silver, " +
                "brass, bronze, and iron versions demand increasing weapon proficiency, and " +
                "blowing one you're unworthy of turns the berserkers against you."),
        item("horseshoes_of_a_zephyr", "Horseshoes of a Zephyr", ItemRarity.VERY_RARE, WONDROUS,
            "A creature wearing these four horseshoes can move normally while hovering 4 inches " +
                "above the ground, ignoring Difficult Terrain, and can travel at a normal pace " +
                "for 12 hours a day without Exhaustion.",
            weightLb = 4.0),
        item("horseshoes_of_speed", "Horseshoes of Speed", ItemRarity.RARE, WONDROUS,
            "A creature wearing these four horseshoes has its Speed increased by 30 feet.",
            weightLb = 4.0),
        item("instant_fortress", "Instant Fortress", ItemRarity.RARE, WONDROUS,
            "This one-inch metal cube grows into a 20-foot square adamantine tower on command, " +
                "with arrow slits, battlements, and a door that opens only at your word.",
            weightLb = 1.0),
        item("instrument_of_illusions", "Instrument of Illusions", ItemRarity.COMMON, WONDROUS,
            "While playing this instrument you can create harmless visual effects — flickering " +
                "lights, drifting motes, a faint image — within a 5-foot cube.",
            attunement = true),
        item("instrument_of_the_bards", "Instrument of the Bards", ItemRarity.UNCOMMON, WONDROUS,
            "You can cast a set list of spells from this instrument, one per day each, and its " +
                "music grants Advantage on Charisma (Performance) checks made with it. Grander " +
                "versions carry longer spell lists.",
            attunement = true, attunementNote = "by a Bard", weightLb = 3.0),
        item("ioun_stone", "Ioun Stone", ItemRarity.RARE, WONDROUS,
            "This stone orbits your head at a distance of 1d3 feet and grants a benefit set by " +
                "its kind — an ability score increase, Absorption of spells, Awareness, " +
                "Protection, Regeneration, Reserve, or Sustenance.",
            attunement = true),
        item("iron_bands_of_bilarro", "Iron Bands of Bilarro", ItemRarity.RARE, WONDROUS,
            "As a Magic action you can throw this rusty sphere at a Huge or smaller creature " +
                "within 60 feet; on a hit, metal bands wrap it in the Restrained condition until " +
                "it escapes with a DC 20 Strength check.",
            weightLb = 1.0),
        item("iron_flask", "Iron Flask", ItemRarity.LEGENDARY, WONDROUS,
            "As a Magic action you can trap a creature from another plane within 60 feet inside " +
                "this flask, where it stays until released to serve you for 1 hour.",
            weightLb = 1.0),
        item("keoghtoms_ointment", "Keoghtom's Ointment", ItemRarity.UNCOMMON, WONDROUS,
            "This jar holds 1d4 + 1 doses. A dose applied as a Utilize action restores 2d8 + 2 " +
                "Hit Points, ends the Poisoned condition, and cures any disease.",
            weightLb = 0.5),
        item("manual_bodily_health", "Manual of Bodily Health", ItemRarity.VERY_RARE, WONDROUS,
            "Reading this book over 48 hours across 6 days increases your Constitution score " +
                "and its maximum by 2. The book then becomes nonmagical for a century.",
            weightLb = 5.0),
        item("manual_gainful_exercise", "Manual of Gainful Exercise", ItemRarity.VERY_RARE,
            WONDROUS,
            "Reading this book over 48 hours across 6 days increases your Strength score and " +
                "its maximum by 2. The book then becomes nonmagical for a century.",
            weightLb = 5.0),
        item("manual_of_golems", "Manual of Golems", ItemRarity.VERY_RARE, WONDROUS,
            "This tome contains the instructions for building one kind of golem. Following them " +
                "takes months and a great deal of money, and produces a golem under your command.",
            weightLb = 5.0),
        item("manual_quickness_action", "Manual of Quickness of Action", ItemRarity.VERY_RARE,
            WONDROUS,
            "Reading this book over 48 hours across 6 days increases your Dexterity score and " +
                "its maximum by 2. The book then becomes nonmagical for a century.",
            weightLb = 5.0),
        item("mantle_spell_resistance", "Mantle of Spell Resistance", ItemRarity.RARE, WONDROUS,
            "You have Advantage on saving throws against spells while you wear this cloak.",
            attunement = true, weightLb = 1.0),
        item("marvelous_pigments", "Nolzur's Marvelous Pigments", ItemRarity.VERY_RARE, WONDROUS,
            "These pots of paint let you create real objects and terrain by painting them in " +
                "two dimensions, covering up to 1,000 square feet before they are used up.",
            weightLb = 1.0),
        item("medallion_of_thoughts", "Medallion of Thoughts", ItemRarity.UNCOMMON, WONDROUS,
            "The medallion has 3 charges and regains 1d3 daily at dawn. Expend a charge to cast " +
                "Detect Thoughts (save DC 13) from it.",
            attunement = true, weightLb = 1.0),
        item("mirror_life_trapping", "Mirror of Life Trapping", ItemRarity.VERY_RARE, WONDROUS,
            "This mirror has twelve extradimensional cells. A creature that sees its own " +
                "reflection within 30 feet must succeed on a DC 15 Charisma saving throw or be " +
                "trapped in one of them.",
            weightLb = 50.0),
        item("necklace_of_fireballs", "Necklace of Fireballs", ItemRarity.RARE, WONDROUS,
            "This necklace holds 1d6 + 3 beads. As a Magic action you can detach one and hurl " +
                "it up to 60 feet, where it explodes as a level 3 Fireball (save DC 15); " +
                "throwing several at once raises the spell's level."),
        item("necklace_prayer_beads", "Necklace of Prayer Beads", ItemRarity.RARE, WONDROUS,
            "This necklace has 1d4 + 2 magic beads, each holding a spell — Bless, Cure Wounds, " +
                "Greater Restoration, or another — that you can cast as a Bonus Action once per " +
                "Long Rest.",
            attunement = true, attunementNote = "by a Cleric, Druid, or Paladin", weightLb = 1.0),
        item("oil_of_etherealness", "Oil of Etherealness", ItemRarity.RARE, "Potion",
            "Applying this oil over 10 minutes gives you the effect of the Etherealness spell " +
                "for 1 hour.",
            weightLb = 0.5),
        item("oil_of_sharpness", "Oil of Sharpness", ItemRarity.VERY_RARE, "Potion",
            "Applied to a weapon or twenty pieces of ammunition, this oil grants a +3 bonus to " +
                "attack and damage rolls for 1 hour.",
            weightLb = 0.5),
        item("oil_of_slipperiness", "Oil of Slipperiness", ItemRarity.UNCOMMON, "Potion",
            "Applying this oil gives you the effect of a Freedom of Movement spell for 8 hours, " +
                "or it can be poured on the ground as a Grease spell.",
            weightLb = 0.5),
        item("periapt_of_health", "Periapt of Health", ItemRarity.UNCOMMON, WONDROUS,
            "You are immune to contracting any disease while you wear this pendant, and any " +
                "disease already afflicting you has no effect on you."),
        item("periapt_proof_poison", "Periapt of Proof against Poison", ItemRarity.RARE, WONDROUS,
            "This pendant ends the Poisoned condition on you, and you have Immunity to Poison " +
                "damage and the Poisoned condition while you wear it."),
        item("periapt_wound_closure", "Periapt of Wound Closure", ItemRarity.UNCOMMON, WONDROUS,
            "You stabilize whenever you have the Unconscious condition and 0 Hit Points, and " +
                "you regain double the usual Hit Points from spending Hit Point Dice.",
            attunement = true),
        item("philter_of_love", "Philter of Love", ItemRarity.UNCOMMON, "Potion",
            "The next creature you see within 10 minutes of drinking this potion becomes " +
                "charmed by you for 1 hour, regarding you with affection.",
            weightLb = 0.5),
        item("pipes_of_the_sewers", "Pipes of the Sewers", ItemRarity.UNCOMMON, WONDROUS,
            "These pipes have 3 charges and regain 1d3 daily at dawn. Playing them summons " +
                "swarms of rats that obey your commands.",
            attunement = true, weightLb = 2.0),
        item("potion_animal_friendship", "Potion of Animal Friendship", ItemRarity.UNCOMMON,
            "Potion",
            "For 1 hour after drinking this potion you can cast Animal Friendship at will " +
                "(save DC 13).",
            weightLb = 0.5),
        item("potion_clairvoyance", "Potion of Clairvoyance", ItemRarity.RARE, "Potion",
            "You gain the effect of the Clairvoyance spell after drinking this potion.",
            weightLb = 0.5),
        item("potion_diminution", "Potion of Diminution", ItemRarity.RARE, "Potion",
            "You gain the 'reduce' effect of the Enlarge/Reduce spell for 1d4 hours, halving " +
                "your size and imposing Disadvantage on Strength checks.",
            weightLb = 0.5),
        item("potion_flying", "Potion of Flying", ItemRarity.VERY_RARE, "Potion",
            "You gain a Fly Speed equal to your Speed and can hover for 1 hour.",
            weightLb = 0.5),
        item("potion_giant_strength_frost", "Potion of Frost Giant Strength", ItemRarity.RARE,
            "Potion",
            "Your Strength score becomes 23 for 1 hour. The potion has no effect if your " +
                "Strength is already 23 or higher.",
            weightLb = 0.5),
        item("potion_giant_strength_storm", "Potion of Storm Giant Strength",
            ItemRarity.LEGENDARY, "Potion",
            "Your Strength score becomes 29 for 1 hour. The potion has no effect if your " +
                "Strength is already 29 or higher.",
            weightLb = 0.5),
        item("potion_of_growth", "Potion of Growth", ItemRarity.UNCOMMON, "Potion",
            "You gain the 'enlarge' effect of the Enlarge/Reduce spell for 1d4 hours, doubling " +
                "your size and granting Advantage on Strength checks.",
            weightLb = 0.5),
        item("potion_of_heroism", "Potion of Heroism", ItemRarity.RARE, "Potion",
            "You gain 10 Temporary Hit Points that last 1 hour, and for that hour you are under " +
                "the effect of the Bless spell without needing Concentration.",
            weightLb = 0.5),
        item("potion_invulnerability", "Potion of Invulnerability", ItemRarity.RARE, "Potion",
            "You have Resistance to all damage for 1 minute after drinking this potion.",
            weightLb = 0.5),
        item("potion_of_longevity", "Potion of Longevity", ItemRarity.VERY_RARE, "Potion",
            "Your physical age is reduced by 1d6 + 6 years, to a minimum of 13. Each potion " +
                "after the first carries a 10 percent cumulative chance of aging you 1d6 + 6 " +
                "years instead.",
            weightLb = 0.5),
        item("potion_mind_reading", "Potion of Mind Reading", ItemRarity.RARE, "Potion",
            "You gain the effect of the Detect Thoughts spell (save DC 13) after drinking this " +
                "potion.",
            weightLb = 0.5),
        item("potion_of_poison", "Potion of Poison", ItemRarity.UNCOMMON, "Potion",
            "This potion looks and tastes like a beneficial one, but it is poison. On drinking " +
                "it you take 3d6 Poison damage and must succeed on a DC 13 Constitution saving " +
                "throw or have the Poisoned condition for 1 hour.",
            weightLb = 0.5),
        item("potion_of_vitality", "Potion of Vitality", ItemRarity.VERY_RARE, "Potion",
            "This potion removes any Exhaustion you have, cures any disease or poison affecting " +
                "you, and for the next 24 hours you regain the maximum number of Hit Points for " +
                "any Hit Point Die you spend.",
            weightLb = 0.5),
        item("potion_water_breathing", "Potion of Water Breathing", ItemRarity.UNCOMMON, "Potion",
            "You can breathe underwater for 1 hour after drinking this potion.",
            weightLb = 0.5),
        item("robe_of_eyes", "Robe of Eyes", ItemRarity.RARE, WONDROUS,
            "You have Advantage on Perception checks that rely on sight, and you gain Darkvision " +
                "out to 120 feet, see Invisible creatures, and can't be Blinded from behind.",
            attunement = true, weightLb = 4.0),
        item("robe_scintillating_colors", "Robe of Scintillating Colors", ItemRarity.VERY_RARE,
            WONDROUS,
            "The robe has 3 charges and regains 1d3 daily at dawn. Expend a charge to make the " +
                "robe blaze with shifting colors, giving attackers Disadvantage and forcing " +
                "creatures that see you to save against the Stunned condition.",
            attunement = true, weightLb = 4.0),
        item("robe_of_stars", "Robe of Stars", ItemRarity.VERY_RARE, WONDROUS,
            "You gain a +1 bonus to saving throws, can cast Magic Missile by plucking one of " +
                "the robe's six stars, and can enter the Astral Plane along with everything you " +
                "are wearing and carrying.",
            attunement = true, weightLb = 4.0),
        item("robe_of_the_archmagi", "Robe of the Archmagi", ItemRarity.LEGENDARY, WONDROUS,
            "Your base Armor Class becomes 15 plus your Dexterity modifier if you wear no armor, " +
                "you have Advantage on saving throws against spells and magical effects, and " +
                "your spell save DC and spell attack bonus each increase by 2.",
            attunement = true, attunementNote = "by a Sorcerer, Warlock, or Wizard", weightLb = 4.0),
        item("robe_of_useful_items", "Robe of Useful Items", ItemRarity.UNCOMMON, WONDROUS,
            "This robe is covered in cloth patches, each of which becomes the real object it " +
                "depicts when detached — a dagger, a lantern, a rowboat, a window, a portable " +
                "ram, and stranger things.",
            weightLb = 4.0),
        item("rope_of_entanglement", "Rope of Entanglement", ItemRarity.RARE, WONDROUS,
            "As a Magic action you can command this 30-foot rope to entangle a creature within " +
                "20 feet, giving it the Restrained condition until it escapes with a DC 15 " +
                "Strength or Dexterity check.",
            weightLb = 3.0),
        item("saddle_of_the_cavalier", "Saddle of the Cavalier", ItemRarity.UNCOMMON, WONDROUS,
            "While in this saddle you can't be dismounted against your will if you have the " +
                "Incapacitated condition, and attack rolls against your mount have Disadvantage.",
            weightLb = 25.0),
        item("scarab_of_protection", "Scarab of Protection", ItemRarity.LEGENDARY, WONDROUS,
            "You have Advantage on saving throws against spells, and the scarab's 12 charges can " +
                "each be spent to turn a failed save against a necromancy spell or an Undead's " +
                "harmful effect into a success.",
            attunement = true),
        item("sovereign_glue", "Sovereign Glue", ItemRarity.LEGENDARY, WONDROUS,
            "This viscous liquid permanently bonds two objects together. Only Universal Solvent, " +
                "Oil of Etherealness, or a Wish spell can separate them.",
            weightLb = 0.5),
        item("sphere_of_annihilation", "Sphere of Annihilation", ItemRarity.LEGENDARY, WONDROUS,
            "A 2-foot-diameter black sphere that obliterates all matter it passes through. You " +
                "can take a Magic action to try to control it with an Intelligence (Arcana) " +
                "check; failing by 5 or more sends it toward you instead."),
        item("stone_earth_elementals", "Stone of Controlling Earth Elementals", ItemRarity.RARE,
            WONDROUS,
            "Placing this stone on the ground and taking a Magic action summons an Earth " +
                "Elemental that acts as your ally, once per day.",
            weightLb = 5.0),
        item("stone_of_good_luck", "Stone of Good Luck", ItemRarity.UNCOMMON, WONDROUS,
            "You gain a +1 bonus to ability checks and saving throws while this polished agate " +
                "is on your person.",
            attunement = true),
        item("talisman_pure_good", "Talisman of Pure Good", ItemRarity.LEGENDARY, WONDROUS,
            "This talisman has 7 charges. A Cleric or Paladin wearing it gains a +2 bonus to " +
                "spell attack rolls, and expending a charge can open a flaming crack beneath an " +
                "evil creature you can see.",
            attunement = true),
        item("talisman_of_the_sphere", "Talisman of the Sphere", ItemRarity.LEGENDARY, WONDROUS,
            "While holding this talisman you double your Intelligence (Arcana) check result when " +
                "controlling a Sphere of Annihilation, and you can control one even without a " +
                "successful check.",
            attunement = true),
        item("talisman_ultimate_evil", "Talisman of Ultimate Evil", ItemRarity.LEGENDARY, WONDROUS,
            "This talisman has 6 charges. A Cleric or Paladin wearing it gains a +2 bonus to " +
                "spell attack rolls, and expending a charge can open a flaming crack beneath a " +
                "good creature you can see.",
            attunement = true),
        item("tome_of_clear_thought", "Tome of Clear Thought", ItemRarity.VERY_RARE, WONDROUS,
            "Reading this book over 48 hours across 6 days increases your Intelligence score " +
                "and its maximum by 2. The book then becomes nonmagical for a century.",
            weightLb = 5.0),
        item("tome_leadership_influence", "Tome of Leadership and Influence", ItemRarity.VERY_RARE,
            WONDROUS,
            "Reading this book over 48 hours across 6 days increases your Charisma score and its " +
                "maximum by 2. The book then becomes nonmagical for a century.",
            weightLb = 5.0),
        item("tome_of_understanding", "Tome of Understanding", ItemRarity.VERY_RARE, WONDROUS,
            "Reading this book over 48 hours across 6 days increases your Wisdom score and its " +
                "maximum by 2. The book then becomes nonmagical for a century.",
            weightLb = 5.0),
        item("tome_stilled_tongue", "Tome of the Stilled Tongue", ItemRarity.LEGENDARY, WONDROUS,
            "A grimoire with a shriveled tongue pinned to its cover. You can use it as a " +
                "Spellcasting Focus, store up to five spells in it, and inscribe a creature's " +
                "name to silence it from afar.",
            attunement = true, weightLb = 5.0),
        item("universal_solvent", "Universal Solvent", ItemRarity.LEGENDARY, WONDROUS,
            "This tube holds a milky liquid that dissolves any adhesive it touches, including " +
                "Sovereign Glue.",
            weightLb = 0.5),
        item("well_of_many_worlds", "Well of Many Worlds", ItemRarity.LEGENDARY, WONDROUS,
            "Unfolding this black cloth opens a two-way portal to another plane of existence, " +
                "chosen at random. It can be used once per day.",
            weightLb = 1.0),
        item("wind_fan", "Wind Fan", ItemRarity.UNCOMMON, WONDROUS,
            "As a Magic action you can cast Gust of Wind from this fan. Each use after the first " +
                "before the next dawn carries a 20 percent chance the fan tears and is destroyed.",
            weightLb = 1.0),
        item("wings_of_flying", "Wings of Flying", ItemRarity.RARE, WONDROUS,
            "As a Magic action you can turn this cloak into a pair of bat or bird wings, giving " +
                "you a Fly Speed of 60 feet for up to 1 hour per day.",
            attunement = true, weightLb = 1.0),
    )

    /**
     * The Replicate Magic Item tables include open-ended rows — "any Common magic item", and
     * so on. They aren't items a DM hands out, so they live outside [ALL] and only ever show
     * up in the plan lists.
     */
    private val OPEN_ENDED_PLANS = listOf(
        item("plan_any_common", "Any Common Magic Item", ItemRarity.COMMON, WONDROUS,
            "Any Common magic item that isn't a Potion, a Scroll, or cursed. You can learn this " +
                "plan more than once, choosing a different item each time; each one counts as a " +
                "separate plan.",
            plan = 2),
        item("plan_any_uncommon_wondrous", "Any Uncommon Wondrous Item", ItemRarity.UNCOMMON,
            WONDROUS,
            "Any Uncommon Wondrous Item that isn't cursed. You can learn this plan more than " +
                "once, choosing a different item each time; each one counts as a separate plan.",
            plan = 10),
        item("plan_any_rare_wondrous", "Any Rare Wondrous Item", ItemRarity.RARE, WONDROUS,
            "Any Rare Wondrous Item that isn't cursed. You can learn this plan more than once, " +
                "choosing a different item each time; each one counts as a separate plan.",
            plan = 14),
    )

    val ALL: List<MagicItem> =
        (ARMOR_AND_SHIELDS + WEAPONS + RINGS + WANDS_AND_STAFFS + WONDROUS_ITEMS + POTIONS +
            MORE_ARMOR + MORE_WEAPONS + MORE_RINGS + MORE_WANDS_AND_STAFFS + MORE_WONDROUS)
            .sortedBy { it.name }

    // Open-ended plans are looked up by id like anything else, they just aren't browsable.
    private val byIdMap: Map<String, MagicItem> = (ALL + OPEN_ENDED_PLANS).associateBy { it.id }

    fun byId(id: String): MagicItem? = byIdMap[id]

    /**
     * Everything an Artificer of [level] can learn as a Replicate Magic Item plan. A plan
     * unlocked at a lower tier stays available, so this is cumulative.
     */
    fun artificerPlans(level: Int): List<MagicItem> = (ALL + OPEN_ENDED_PLANS)
        .filter { magicItem -> magicItem.artificerPlanLevel?.let { it <= level } == true }
        .sortedBy { it.name }

    /** The tiers the Replicate Magic Item tables are split into. */
    val PLAN_LEVELS: List<Int> = listOf(2, 6, 10, 14)
}
