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

    val ALL: List<MagicItem> =
        (ARMOR_AND_SHIELDS + WEAPONS + RINGS + WANDS_AND_STAFFS + WONDROUS_ITEMS + POTIONS)
            .sortedBy { it.name }

    private val byIdMap: Map<String, MagicItem> = ALL.associateBy { it.id }

    fun byId(id: String): MagicItem? = byIdMap[id]

    /**
     * Everything an Artificer of [level] can learn as a Replicate Magic Item plan. A plan
     * unlocked at a lower tier stays available, so this is cumulative.
     */
    fun artificerPlans(level: Int): List<MagicItem> = ALL.filter { magicItem ->
        magicItem.artificerPlanLevel?.let { it <= level } == true
    }

    /** The tiers the Replicate Magic Item tables are split into. */
    val PLAN_LEVELS: List<Int> = listOf(2, 6, 10, 14)
}
