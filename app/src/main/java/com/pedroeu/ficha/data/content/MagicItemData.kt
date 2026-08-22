package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.ItemRarity
import com.pedroeu.ficha.data.model.MagicItem
import com.pedroeu.ficha.data.model.Sourcebook

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
        book: Sourcebook = Sourcebook.DMG,
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
        book = book,
        // The numbers are filled in from the tables at the bottom of this file rather than
        // repeated at each call site, so a test can hold them against the rules text.
        attackBonus = ATTACK_BONUSES[id] ?: 0,
        acBonus = AC_BONUSES[id] ?: 0,
    )

    private const val WONDROUS = "Wondrous Item"

    /**
     * The bonus each item gives to attack and damage rolls made with it.
     *
     * Kept as a table rather than parsed back out of the description, because an attack line
     * that reads its numbers out of prose is one rewording away from being wrong. A test holds
     * every entry here against what the item's own text says.
     */
    private val ATTACK_BONUSES: Map<String, Int> = mapOf(
        "weapon_plus_1" to 1,
        "weapon_plus_2" to 2,
        "weapon_plus_3" to 3,
        "berserker_axe" to 1,
        "dagger_of_venom" to 1,
        "dazzling_weapon" to 1,
        "defender" to 3,
        "dragon_slayer" to 1,
        "repeating_shot" to 1,
        "returning_weapon" to 1,
        "sun_blade" to 2,
        "wraps_unarmed_1" to 1,
        "wraps_unarmed_2" to 2,
        "dwarven_thrower" to 3,
        "giant_slayer" to 1,
        "holy_avenger" to 3,
        "luck_blade" to 1,
        "mace_of_smiting" to 1,
        "nine_lives_stealer" to 1,
        "scimitar_of_speed" to 2,
        "vorpal_sword" to 3,
        "rod_of_lordly_might" to 3,
        "staff_of_striking" to 3,
        "staff_thunder_lightning" to 2,
        "oil_of_sharpness" to 3,
    )

    /**
     * The bonus each item adds to Armor Class on top of whatever the character is already
     * wearing.
     *
     * Deliberately narrower than the set of items whose text mentions Armor Class. A Dwarven
     * Plate *is* the armor rather than an addition to it, and this app has no way to link one
     * to the plate underneath, so counting it would inflate the number. The same goes for the
     * conditional ones — Bracers of Defense want you unarmored, an Arrow-Catching Shield only
     * helps against ranged attacks — which a single figure on the sheet cannot express. Those
     * stay at zero and remain the player's own adjustment, which is what they were before.
     */
    private val AC_BONUSES: Map<String, Int> = mapOf(
        "armor_plus_1" to 1,
        "armor_plus_2" to 2,
        "armor_plus_3" to 3,
        "shield_plus_1" to 1,
        "shield_plus_2" to 2,
        "shield_plus_3" to 3,
        "elven_chain" to 1,
        "ring_of_protection" to 1,
        "cloak_of_protection" to 1,
    )

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
            weightLb = 6.0, plan = 6, book = Sourcebook.EBERRON),
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
            attunement = true, plan = 6, book = Sourcebook.EBERRON),
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
            attunement = true, plan = 2, book = Sourcebook.EBERRON),
        item("returning_weapon", "Returning Weapon", ItemRarity.UNCOMMON,
            "Weapon (any weapon with the Thrown property)",
            "You gain a +1 bonus to attack and damage rolls with this weapon, and it returns to " +
                "your hand immediately after you use it to make a ranged attack.",
            plan = 2, book = Sourcebook.EBERRON),
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
            attunement = true, plan = 6, book = Sourcebook.EBERRON),
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
            attunement = true, weightLb = 1.0, plan = 6, book = Sourcebook.EBERRON),
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
            weightLb = 3.0, plan = 6, book = Sourcebook.EBERRON),
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
            attunement = true, weightLb = 5.0, plan = 2, book = Sourcebook.EBERRON),
        item("mind_sharpener", "Mind Sharpener", ItemRarity.UNCOMMON, WONDROUS,
            "This item has 4 charges and regains 1d4 daily at dawn. When you fail a Constitution " +
                "saving throw to maintain Concentration, you can expend a charge to succeed instead.",
            attunement = true, plan = 6, book = Sourcebook.EBERRON),
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
            weightLb = 0.5, book = Sourcebook.PHB),
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

    /** Everything the 2024 books carry that the hand-written lists above did not. */
    private val IMPORTED: List<MagicItem> = listOf(
        item("adventurer_s_ring", "Adventurer's Ring", ItemRarity.COMMON, "Ring",
            "\"Lamps are a liability on adventures. Delicate and prone to oil spillage, lamps fill a hand that should otherwise be wielding a weapon or casting a spell. Once you try one of our elegant and practical rings, you'll never go back to cumbersome flint and fire.\" While the cover on this ring is open, the ring produces a flame that creates no heat and consumes no fuel. It sheds Bright Light in a 20-foot radius and Dim Light for an additional 20 feet. As a Bonus Action, you can close the cover, smothering the flame, or open it again.", book = Sourcebook.HEROES_OF_FAERUN),
        item("alarm_pylon", "Alarm Pylon", ItemRarity.UNCOMMON, "Wondrous Item",
            "This 5-foot-tall post is made of stone and etched with runes. As a Magic action, you can activate the pylon by touching the runes and specifying one of the following triggers: A creature or item casts a spell from a specific school of magic. A creature takes 5 or more damage. An object takes 10 or more damage. If the chosen trigger occurs within 300 feet of the pylon while the pylon is activated, the pylon emits a ringing alarm for 10 seconds, which is audible up to 300 feet away. You can take a Magic action to deactivate the pylon.", book = Sourcebook.DDB_DROPS),
        item("ammunition_1_2_or_3", "Ammunition, +1, +2, or +3", ItemRarity.UNCOMMON, "Weapon (Any Ammunition)",
            "You have a bonus to attack rolls and damage rolls made with this piece of magic ammunition. The bonus is determined by the rarity of the ammunition. Once it hits a target, the ammunition is no longer magical. This ammunition is typically found or sold in quantities of ten or twenty pieces. Ten pieces of this ammunition are equivalent in value to a potion of the same rarity."),
        item("ammunition_of_slaying", "Ammunition of Slaying", ItemRarity.VERY_RARE, "Weapon (Any Ammunition)",
            "This magic ammunition is meant to slay creatures of a particular type, which the DM chooses or determines randomly by rolling on the table below. If a creature of that type takes damage from the ammunition, the creature makes a DC 17 Constitution saving throw, taking an extra 6d10 Force damage on a failed save or half as much extra damage on a successful one. After dealing its extra damage to a creature, the ammunition becomes nonmagical. 1d100 Creature Type 01-10 Aberrations 11-15 Beasts 16-20 Celestials 21-25 Constructs 26-35 Dragons 36-45 Elementals 46-50 Humanoids 51-60 Fey 61-70 Fiends 71-75 Giants 76-80 Monstrosities 81-85 Oozes 86-90 Plants 91-00 Undead"),
        item("amulet_of_retributive_healing", "Amulet of Retributive Healing", ItemRarity.RARE, "Wondrous Item",
            "This amulet has 3 charges and regains 1d4 charges daily at dawn. When you restore Hit Points to one other creature, you can expend 1 charge to regain the same amount of Hit Points.", attunement = true, book = Sourcebook.DDB_DROPS),
        item("arcane_battery", "Arcane Battery", ItemRarity.UNCOMMON, "Wondrous Item",
            "This smooth, oval stone is etched with faintly glowing, magical symbols. While holding the battery, you can take a Magic action to touch the battery to one magic item. If that magic item normally regains expended charges daily, it immediately regains 1d4 + 1 expended charges, and the battery loses its magic.", book = Sourcebook.NETHERILS_FALL),
        item("armor_1_2_or_3", "Armor, +1, +2, or +3", ItemRarity.RARE, "Armor (Any Light, Medium, or Heavy)",
            "You have a bonus to Armor Class while wearing this armor. The bonus is determined by its rarity."),
        item("armor_of_gleaming", "Armor of Gleaming", ItemRarity.COMMON, "Armor (Any Light, Medium, or Heavy)",
            "This armor never gets dirty."),
        item("axe_of_the_dwarvish_lords", "Axe of the Dwarvish Lords", ItemRarity.ARTIFACT, "Weapon (Battleaxe)",
            "A young dwarf prince set out to forge a weapon that would be regarded as a symbol of unity among his people. Venturing deep under the mountains, deeper than any dwarf had ever delved, the prince came to the blazing heart of a great volcano. With the aid of Moradin, a god of creation, he first crafted four mighty tools: the Starmetal Pick, the Earthheart Forge, the Anvil of Songs, and the Shaping Hammer. With these tools, he forged the Axe of the Dwarvish Lords. Armed with the Artifact, the prince brought peace to the dwarf clans, ending grudges and answering slights. The clans became allies, and they threw back their enemies and enjoyed an era of prosperity. This young dwarf is remembered as the First King. When he became old, he passed the weapon, which had become his badge of office, to his heir. The rightful inheritors passed the axe on for many generations. [...]", attunement = true),
        item("baba_yaga_s_dancing_broom", "Baba Yaga's Dancing Broom", ItemRarity.UNCOMMON, "Wondrous Item",
            "The archfey Baba Yaga crafted many of these magic brooms. No two appear exactly alike. While holding the broom, you can take a Magic action to transform it into an Animated Broom under your control. The broom then moves into an unoccupied space as close to you as possible. The broom acts immediately after you on your Initiative count and remains animate until you take a Bonus Action and use a command word to render it inanimate. On your turn, you can mentally command the animated broom if it is within 30 feet of you and you don't have the Incapacitated condition (no action required). You decide what action the broom takes and where it moves during its next turn, or you can issue it a general command, such as to attack your enemies or guard a location. If the broom is reduced to 0 Hit Points, it shatters and is destroyed. [...]", attunement = true),
        item("bead_of_nourishment", "Bead of Nourishment", ItemRarity.COMMON, "Wondrous Item",
            "This flavorless, gelatinous bead dissolves on your tongue and provides as much nourishment as 1 day of Rations."),
        item("bead_of_refreshment", "Bead of Refreshment", ItemRarity.COMMON, "Wondrous Item",
            "This flavorless, gelatinous bead dissolves in liquid, transforming up to a pint of the liquid into fresh, cold drinking water. The bead has no effect on magical liquids or harmful substances such as poison."),
        item("blackrazor", "Blackrazor", ItemRarity.ARTIFACT, "Weapon (Greatsword)",
            "Hidden in the dungeon of White Plume Mountain, Blackrazor shines like a piece of night sky filled with stars. Its black scabbard is decorated with pieces of cut obsidian. You gain a +3 bonus to attack rolls and damage rolls made with this magic weapon. If you hit an Undead with this weapon, you take 1d10 Necrotic damage, and the target regains 1d10 Hit Points. If this Necrotic damage reduces you to 0 Hit Points, Blackrazor devours your soul (see \"Devour Soul\" below). While you hold this weapon, you have Immunity to the Charmed and Frightened conditions, and you have Blindsight with a range of 30 feet. Devour Soul. Whenever you use Blackrazor to reduce a creature to 0 Hit Points, the sword slays the creature and devours its soul unless it is a Construct or an Undead. A creature whose soul has been devoured by Blackrazor can be restored to life only by a Wish spell. [...]", attunement = true),
        item("book_of_exalted_deeds", "Book of Exalted Deeds", ItemRarity.ARTIFACT, "Wondrous Item",
            "The definitive treatise on all that is good in the multiverse, the Book of Exalted Deeds figures prominently in many religions. Rather than being a scripture devoted to a particular faith, the book's authors filled the pages with their own visions of true virtue, providing guidance for defeating evil. The Book of Exalted Deeds rarely lingers in one place. As soon as the book is read, it vanishes to some other corner of the multiverse where its moral guidance can bring hope to an endangered world. Although attempts have been made to copy the work, efforts to do so fail to capture its magical nature or translate the benefits it offers to those pure of heart and firm of purpose. A heavy clasp, wrought to look like angel wings, keeps the book's contents secure. Only a creature that is attuned to the book can release the clasp that holds it shut. [...]", attunement = true),
        item("book_of_vile_darkness", "Book of Vile Darkness", ItemRarity.ARTIFACT, "Wondrous Item",
            "The contents of this foul manuscript are the meat and drink of the wicked. It contains knowledge so horrid that to even glimpse the scrawled pages invites doom. Most believe the lich-god Vecna authored the Book of Vile Darkness . He recorded in its pages every horrid idea, every corrupt thought, and every example of foul magic he came across or devised. Other practitioners of evil have added their own input to the book's catalog of vile knowledge. Their additions are clear, for the writers of later works stitched whatever they were writing into the tome or, in some cases, made notations and additions to existing text. There are places where pages are missing, torn, or covered so completely with ink, blood, and scratches that the original text can't be divined. Nature can't abide the book's presence. [...]", attunement = true),
        item("boots_of_false_tracks", "Boots of False Tracks", ItemRarity.COMMON, "Wondrous Item",
            "While wearing these boots, you can have them leave tracks like those of any kind of Humanoid of your size.", attunement = true),
        item("brooch_of_the_elements", "Brooch of the Elements", ItemRarity.RARE, "Wondrous Item",
            "The central jewel of this butterfly-shaped brooch swirls with multicolored iridescence. The brooch has 3 charges and regains all expended charges daily at dawn. As a Reaction when a creature you can see within 60 feet of you hits a target with an attack roll and deals damage, you can expend 1 charge and change one damage type the attack deals to one of the following damage types: Acid, Cold, Fire, Lightning, Poison, or Thunder.", attunement = true, book = Sourcebook.NETHERILS_FALL),
        item("calimemnon_crystal", "Calimemnon Crystal", ItemRarity.ARTIFACT, "Wondrous Item",
            "This immaculately cut sixty-sided diamond fits in one hand, yet it contains an entire genie empire. The Calimemnon Crystal was made by high elf magic to imprison the notorious genie lords Calim and Memnon, who once ruled and warred over what is now Calimshan. When viewed by a creature with Truesight, the normally beautiful diamond is a grotesque sight: trapped within the gemstone are more than a hundred genies, including the physical forms of Calim and Memnon. The genies scream and strain in agonized fury, pressing their belligerent faces against the walls of their crystalline prison. Properties of the Crystal. While attuned to the crystal, you gain the following benefits: Arcane Focus. You can use the Calimemnon Crystal as an Arcane Focus. Flight. You have a Fly Speed of 30 feet and can hover. Spells. [...]", attunement = true, book = Sourcebook.ADVENTURES_IN_FAERUN),
        item("candle_of_the_deep", "Candle of the Deep", ItemRarity.COMMON, "Wondrous Item",
            "The flame of this candle isn't extinguished when immersed in water. It gives off light and heat like a normal candle."),
        item("cap_of_vanishing", "Cap of Vanishing", ItemRarity.UNCOMMON, "Wondrous item",
            "This cap has 3 charges and regains all expended charges daily at dawn. While wearing the cap, you can take a Magic action and expend 1 charge to give yourself the Invisible condition for 10 minutes. The effect ends early if the hat is removed, or immediately after you make an attack roll, deal damage, or cast a spell.", attunement = true, book = Sourcebook.HELLFIRE_CLUB),
        item("cauldron_of_rebirth", "Cauldron of Rebirth", ItemRarity.VERY_RARE, "Wondrous Item",
            "This Tiny pot bears relief scenes of heroes on its cast-iron sides. You can use the cauldron as a Spellcasting Focus for your spells, and it functions as a suitable component for the Scrying spell. Brew Potion. When you finish a Long Rest, you can use the cauldron to create a Potion of Healing (greater) , which takes 1 minute. The potion lasts for 24 hours, then loses its magic if not consumed. Raise Dead. As a Magic action, you can cause the cauldron to grow large enough for a Medium creature to crouch within. You can revert the cauldron to its normal size as a Magic action, harmlessly shunting anything that can't fit inside to the nearest unoccupied space. [...]", attunement = true, attunementNote = "by a Druid or Warlock"),
        item("charlatan_s_die", "Charlatan's Die", ItemRarity.COMMON, "Wondrous Item",
            "Whenever you roll this six-sided die, you can control which number it rolls.", attunement = true),
        item("climber_s_ammunition", "Climber's Ammunition", ItemRarity.UNCOMMON, "Weapon (any ammunition)",
            "When you hit a solid surface with this piece of magic ammunition, the ammunition attaches to the surface, and a magic rope trails out from behind it. The rope remains in place for 1 hour or until you dismiss it (no action required). It can support up to 500 pounds at once, breaking if that limit is exceeded. Once the rope vanishes, the ammunition becomes nonmagical. If you hit a creature with this ammunition, the ammunition immediately breaks on impact and deals no damage to the target.", book = Sourcebook.DDB_DROPS),
        item("cloak_of_billowing", "Cloak of Billowing", ItemRarity.COMMON, "Wondrous Item",
            "While wearing this cloak, you can take a Bonus Action to make it billow dramatically for 1 minute."),
        item("cloak_of_many_fashions", "Cloak of Many Fashions", ItemRarity.COMMON, "Wondrous Item",
            "While wearing this cloak, you can take a Bonus Action to change the style, color, and apparent quality of the garment. The cloak's weight doesn't change. Regardless of its appearance, the cloak can't be anything but a cloak. Although it can duplicate the appearance of other magic cloaks, it doesn't gain their magical properties."),
        item("clockwork_amulet", "Clockwork Amulet", ItemRarity.COMMON, "Wondrous Item",
            "This copper amulet contains tiny interlocking gears and is powered by magic from Mechanus, a plane of clockwork predictability. Faint ticking and whirring noises emanate from within. When you make an attack roll while wearing the amulet, you can forgo rolling the d20 to get a 10 on the die. Once used, this property can't be used again until the next dawn. common wondrous-item Help | Terms of Service | Privacy | Report a bug | Flag as objectionable | Update cookie settings if (window[\"nitroAds\"] && window[\"nitroAds\"].loaded) { document.getElementById(\"consent-box\").style.display = window[\"__tcfapi\"] ? \"\" : \"none\"; } else { document.addEventListener( \"nitroAds.loaded\", () => (document.getElementById(\"consent-box\").style.display = window[\"__tcfapi\"] ? \"\" : \"none\") ); } Powered by Wikidot.com Unless otherwise stated, the content of this page is licensed under Creative Commons Attribution-Sha [...]"),
        item("clothes_of_mending", "Clothes of Mending", ItemRarity.COMMON, "Wondrous Item",
            "This elegant outfit magically mends itself to counteract daily wear and tear. Pieces of the outfit that are destroyed can't be repaired in this way."),
        item("crown_of_horns", "Crown Of Horns", ItemRarity.ARTIFACT, "Wondrous Item",
            "The Crown of Horns contains the essence and intelligence of Myrkul, one of the Dead Three. This ghastly crown is a pale silver circlet with four curved bones set around its rim. On the crown's brow is set a black diamond whose depths swirl with weird, malignant energy. Myrkul created the Crown of Horns at the height of his power. When Myrkul was slain by Mystra, he imbued the crown with his dying essence. Myrkul has since returned to become one of the Dead Three, and the Crown of Horns continues to further his gruesome will by seeking out mortal agents and gradually transforming them into powerful Undead scions. Should a wearer prove unworthy of this honor, the crown teleports away to find a new mortal to corrupt for its master. Cursed. The crown is cursed. Attuning to the crown extends the curse to you. Only a Wish spell or the will of Myrkul can remove this curse. [...]", attunement = true, book = Sourcebook.ADVENTURES_IN_FAERUN),
        item("crystal_ball_of_mind_reading", "Crystal Ball of Mind Reading", ItemRarity.LEGENDARY, "Wondrous Item",
            "While touching this crystal orb, you can cast Scrying (save DC 17) with it. In addition, you can cast Detect Thoughts (save DC 17) targeting creatures you can see within 30 feet of the spell's sensor. You don't need to concentrate on this Detect Thoughts spell to maintain it during its duration, but it ends if the Scrying spell ends.", attunement = true),
        item("crystal_ball_of_telepathy", "Crystal Ball of Telepathy", ItemRarity.LEGENDARY, "Wondrous Item",
            "While touching this crystal orb, you can cast Scrying (save DC 17) with it. In addition, you can communicate telepathically with creatures you can see within 30 feet of the spell's sensor. You can also cast Suggestion (save DC 17) through the sensor on one of those creatures. You don't need to concentrate on this Suggestion to maintain it during its duration, but it ends if Scrying ends. You can't cast Suggestion in this way again until the next dawn.", attunement = true),
        item("crystal_ball_of_true_seeing", "Crystal Ball of True Seeing", ItemRarity.LEGENDARY, "Wondrous Item",
            "While touching this crystal orb, you can cast Scrying (save DC 17) with it. In addition, you have Truesight with a range of 120 feet centered on the spell's sensor.", attunement = true),
        item("cube_of_summoning", "Cube of Summoning", ItemRarity.RARE, "Wondrous Item",
            "This Tiny cube looks like a jack-in-the-box. When you wind its crank as a Magic action, a merry tune emits from the box, the lid pops open, a creature appears in the nearest unoccupied space, and the lid closes. The lid can't otherwise be opened. Roll on the Cube of Summoning table to determine which spell the cube casts to summon the creature. The spell is cast at level 5 (save DC 17, +9 attack bonus) and doesn't require Concentration, but you otherwise function as the spell's caster. Once the cube summons a creature, the cube can't do so again until the next dawn. Cube of Summoning 1d6 Spell 1 Summon Aberration 2 Summon Beast 3 Summon Construct 4 Summon Dragon 5 Summon Elemental 6 Summon Fey"),
        item("daern_s_instant_fortress", "Daern's Instant Fortress", ItemRarity.RARE, "Wondrous Item",
            "As a Magic action, you can place this 1-inch adamantine statuette on the ground and, using a command word, cause it to grow rapidly into a square adamantine tower. Repeating the command word causes the tower to revert to statuette form, which works only if the tower is empty. Each creature in the area where the tower appears is pushed to an unoccupied space outside but next to the tower. Objects in the area that aren't being worn or carried are also pushed clear of the tower. The tower is 20 feet on a side and 30 feet high, with arrow slits on all sides and a battlement atop it. Its interior is divided into two floors, with a ladder, staircase, or ramp (your choice) connecting them. This ladder, staircase, or ramp ends at a trapdoor leading to the roof. When created, the tower has a single door at ground level on the side facing you. [...]", attunement = true),
        item("dark_shard_amulet", "Dark Shard Amulet", ItemRarity.COMMON, "Wondrous Item",
            "This amulet is fashioned from a shard of resilient material originating from an otherworldly realm. While you are wearing it, you gain the following benefits. Spellcasting Focus . You can use the amulet as a Spellcasting Focus for your Warlock spells. Unknown Spell. As a Magic action, you can try to cast a cantrip that you don't know. The cantrip must be on the Warlock spell list and have a casting time of an action, and you make a DC 10 Intelligence (Arcana) check. On a successful check, you cast the spell. On a failed check, the spell fails, and the action used to cast it is wasted. In either case, you can't use this property again until you finish a Long Rest.", attunement = true, attunementNote = "by a Warlock"),
        item("demonomicon_of_iggwilv", "Demonomicon of Iggwilv", ItemRarity.ARTIFACT, "Wondrous Item",
            "This treatise, composed by Iggwilv the archmage, documents the Abyss's layers and inhabitants and is widely regarded as the most thorough and blasphemous tome of demonology in the multiverse. The tome recounts both the oldest and most current profanities of the Abyss and demons. Demons have attempted to censor the text, and while sections have been ripped from the book's spine, the general chapters remain, ever revealing demonic secrets. Caged behind lines of script roils a secret piece of the Abyss itself, which keeps the book up-to-date, no matter how many pages are removed, and it longs to be more than mere reference material. Abyssal Lore. You can reference the Demonomicon whenever you make an Intelligence check to discern information about demons or a Wisdom (Survival) check related to the Abyss. When you do so, you gain Advantage on the check. Containment. [...]", attunement = true),
        item("dread_helm", "Dread Helm", ItemRarity.COMMON, "Wondrous Item",
            "While you're wearing this fearsome steel helm, your eyes glow red and the rest of your face is hidden in shadow."),
        item("ear_horn_of_hearing", "Ear Horn of Hearing", ItemRarity.COMMON, "Wondrous Item",
            "While held up to your ear, this horn suppresses the effects of the Deafened condition on you."),
        item("ebonbane", "Ebonbane", ItemRarity.ARTIFACT, "Weapon (Longsword)",
            "The Darklord of the Domain of Dread known as the Shadowlands is Ebonbane, a sapient sword. Ebonbane uses its monster stat block while in Shadowborn Manor. Bound to the Shadowlands. When you enter a space outside of the Shadowlands, your Attunement immediately ends and Ebonbane teleports to somewhere in Shadowborn Manor. Insatiable Rage. The sword demands destruction. If the sword hasn't slain a Celestial or a Humanoid for 3 days, you make a DC 17 Charisma saving throw at the next dawn. On a successful save, you take 8d8 Force damage. On a failed save, you are dominated by the sword, as if by the Dominate Monster spell, and the sword demands the blood of a Celestial or a Humanoid. The spell effect ends when the sword's demand is met. Magic Weapon. You gain a +3 bonus to attack rolls and damage rolls made with this magic weapon. [...]", attunement = true, book = Sourcebook.RAVENLOFT),
        item("enduring_spellbook", "Enduring Spellbook", ItemRarity.COMMON, "Wondrous Item",
            "This spellbook, along with anything written on its pages, can't be damaged by fire or water. In addition, the spellbook doesn't deteriorate with age."),
        item("energy_bow", "Energy Bow", ItemRarity.VERY_RARE, "Weapon (Longbow or Shortbow)",
            "You gain a +1 bonus to attack rolls and damage rolls made with this magic weapon, which has no string. Each time you pull your arm back in a firing motion, a magical arrow made of golden energy appears nocked and ready to fire. An arrow produced by this weapon deals Force damage instead of Piercing damage on a hit, and it disappears after it hits or misses its target. Until it disappears, the arrow emits Bright Light in a 20-foot radius and Dim Light for an additional 20 feet. This weapon has the following additional properties. Arrow of Restraint. Whenever you use this weapon to make a ranged attack against a creature, you can try to restrain the target instead of dealing damage to it. If the arrow hits, the target must succeed on a DC 15 Strength saving throw or have the Restrained condition for 1 minute. [...]", attunement = true),
        item("ersatz_eye", "Ersatz Eye", ItemRarity.COMMON, "Wondrous Item",
            "This magical eye replaces a real one that was lost or removed. While the Ersatz Eye is embedded in your eye socket, you can see through the tiny orb as though it were your natural eye. You can insert or remove the Ersatz Eye as a Magic action, and it can't be removed against your will while you are alive."),
        item("eternal_chalk", "Eternal Chalk", ItemRarity.COMMON, "Wondrous Item",
            "A stick of Eternal Chalk never breaks or wears down with normal use. When using this chalk you can choose the color of its marks and whether they emit a faint glow. Any marks you make with this chalk can't be erased for 7 days by anyone except you.", book = Sourcebook.DDB_DROPS),
        item("executioner_s_axe", "Executioner's Axe", ItemRarity.VERY_RARE, "Weapon (Battleaxe, Greataxe, Halberd, or Handaxe)",
            "You gain a +1 bonus to attack rolls and damage rolls made with this magic weapon. Any Humanoid you hit with the weapon takes an extra 2d6 Slashing damage, and you gain Temporary Hit Points equal to the extra damage dealt."),
        item("eye_and_hand_of_vecna", "Eye and Hand of Vecna", ItemRarity.ARTIFACT, "Wondrous Item",
            "Vecna was a mighty wizard who, through magic and conquest, forged a terrible empire. For all his power, however, Vecna feared death and took steps to prevent his demise by becoming a lich. A treacherous lieutenant named Kas brought Vecna's rule to an end in a terrible battle. Of Vecna, all that remained were one hand and one eye, grisly Artifacts that still seek to work Vecna's will in the world. The Eye of Vecna and the Hand of Vecna are separate Artifacts that might be found together or separately. The eye looks like a bloodshot organ torn free from the socket. The hand is a shriveled left extremity. Random Properties of the Eye and Hand. The Eye of Vecna and the Hand of Vecna each have the following random properties (see \"Artifacts\"): 1 minor beneficial property 1 major beneficial property 1 minor detrimental property Attuning to the Eye. [...]", attunement = true),
        item("fork_of_eddy_summoning", "Fork of Eddy Summoning", ItemRarity.RARE, "Wondrous Item",
            "This Tiny tuning fork crackles with harmless, multicolored magical energy. As a Magic action, you can tap the fork against any object to summon an Eldritch Eddy (see Netheril's Fall). The eddy appears in an unoccupied space as close to you as possible. The eddy is Friendly to you and your allies, and it obeys your commands. If you fail to command it, the eddy defends itself against attackers but takes no other actions. It takes its turn immediately after you on your Initiative count. The eddy disappears after 1 hour, when it dies, or when you dismiss it as a Bonus Action. The fork can't be used this way again until the next dawn.", book = Sourcebook.NETHERILS_FALL),
        item("goggles_of_foe_finding", "Goggles of Foe-Finding", ItemRarity.RARE, "Wondrous Item",
            "While you're wearing these goggles, your ranged attacks with weapons ignore Half Cover and Three-Quarters Cover.", attunement = true, book = Sourcebook.DDB_DROPS),
        item("hag_eye", "Hag Eye", ItemRarity.UNCOMMON, "Wondrous Item",
            "A Hag Eye has 3 charges. While wearing or holding this item, you can expend 1 charge to cast Darkvision (targeting yourself only) or See Invisibility. The Hag Eye regains all expended charges daily at dawn. Coven Sensor. The Hag Eye is usually entrusted to a hag's minion for safekeeping and transport. As a Magic action, a hag who belongs to the coven that created the Hag Eye can see what the Hag Eye sees if the hag and the Hag Eye are on the same plane of existence. This effect lasts as long as the hag maintains Concentration. Multiple hags in the coven can see through the Hag Eye simultaneously. Creating a Hag Eye. Only a hag coven can craft this item, which is made from a real eye coated in varnish and often fitted to a pendant or another wearable item. [...]"),
        item("hammer_of_thunderbolts", "Hammer of Thunderbolts", ItemRarity.LEGENDARY, "Weapon (Maul or Warhammer)",
            "You gain a +1 bonus to attack rolls and damage rolls made with this magic weapon. The weapon has 5 charges. You can expend 1 charge and make a ranged attack with the weapon, hurling it as if it had the Thrown property with a normal range of 20 feet and a long range of 60 feet. If the attack hits, the weapon unleashes a thunderclap audible out to 300 feet. The target and every creature within 30 feet of it other than you must succeed on a DC 17 Constitution saving throw or have the Stunned condition until the end of your next turn. Immediately after hitting or missing, the weapon flies back to your hand. The weapon regains 1d4 + 1 expended charges daily at dawn. Giant's Bane. While you are attuned to the weapon and wearing either a Belt of Giant Strength or Gauntlets of Ogre Power to which you are also attuned, you gain the following benefits: Giants' Bane. [...]", attunement = true),
        item("harkon_s_bite", "Harkon's Bite", ItemRarity.UNCOMMON, "Wondrous Item",
            "A dire wolf's tooth dangles from this simple cord necklace. You gain a +1 bonus to ability checks and saving throws while you wear this necklace. Curse. This necklace is cursed. Attuning to the necklace curses you; this curse can't be removed until Harkon Lukas dies. As long as you remain cursed, you become a Werewolf serving Harkon Lukas under the DM's control during the night of a full moon.", attunement = true, attunementNote = "by a Humanoid", book = Sourcebook.RAVENLOFT),
        item("harper_pin", "Harper Pin", ItemRarity.UNCOMMON, "Wondrous Item",
            "When you attune to this pin, choose a Harper persona (see the Forgotten Realms: Heroes of Faerûn for examples), including an alignment and a creature type. While wearing this pin, you register as that persona when targeted by magic to determine your creature type, alignment, or location. The type of pin determines its rarity and effects. Silver Harper Pin. When you attune to this pin, you can devise a general line of thought of 25 words or fewer. While you wear this pin, a creature that reads your thoughts detects your preprogrammed line of thought instead. A creature that takes the Study action while reading your thoughts makes a DC 13 Intelligence (Investigation) check. On a successful check, it becomes aware that the detected thoughts are preprogrammed. Golden Harper Pin. While wearing this pin, you can cast the Nondetection spell on yourself. [...]", attunement = true, book = Sourcebook.ADVENTURES_IN_FAERUN),
        item("hat_of_vermin", "Hat of Vermin", ItemRarity.COMMON, "Wondrous Item",
            "This hat has 3 charges. While holding the hat, you can take a Magic action to expend 1 charge and summon your choice of a Bat, a Frog, or a Rat. The summoned creature magically appears in the hat and tries to get away from you as quickly as possible. The creature is Indifferent toward you and other creatures, and it isn't under your control. It behaves as an ordinary creature of its kind and disappears after 1 hour or when it drops to 0 Hit Points. The hat regains all expended charges daily at dawn."),
        item("hat_of_vortexes", "Hat of Vortexes", ItemRarity.UNCOMMON, "Wondrous Item",
            "This hat has 3 charges and regains all expended charges daily at dawn. You can take a Magic action and expend 1 charge while holding the hat to release a magical vortex from it. The vortex fills a 10-foot Cube originating from you and lasts for 1 hour. While the vortex is present, its area is Difficult Terrain. You decide a vortex's visual details when you create it. For instance, the vortex might be multicolored or glittery.", attunement = true, book = Sourcebook.NETHERILS_FALL),
        item("hat_of_wizardry", "Hat of Wizardry", ItemRarity.COMMON, "Wondrous Item",
            "This cone-shaped hat is adorned with moons and stars. While you are wearing it, you gain the following benefits. Spellcasting Focus. You can use the hat as a Spellcasting Focus for your Wizard spells. Unknown Spell. As a Magic action, you can try to cast a cantrip that you don't know. The cantrip must be on the Wizard spell list and have a casting time of an action, and you make a DC 10 Intelligence (Arcana) check. On a successful check, you cast the spell. On a failed check, the spell fails, and the action used to cast the spell is wasted. In either case, you can't use this property again until you finish a Long Rest.", attunement = true, attunementNote = "by a Wizard"),
        item("heward_s_handy_haversack", "Heward's Handy Haversack", ItemRarity.RARE, "Wondrous Item",
            "This backpack has a central pouch and two side pouches, each of which is an extradimensional space. Each side pouch can hold up to 200 pounds of material, not exceeding a volume of 25 cubic feet. The central pouch can hold up to 500 pounds of material, not exceeding a volume of 64 cubic feet. The haversack always weighs 5 pounds, regardless of its contents. Retrieving an item from the haversack requires a Utilize action or a Bonus Action (your choice). When you reach into the haversack for a specific item, the item is always magically on top. If any of its pouches is overloaded, pierced, or torn, the haversack ruptures and is destroyed. If the haversack is destroyed, its contents are lost forever, although an Artifact always turns up again somewhere. If the haversack is turned inside out, its contents spill forth unharmed, and the haversack must be put right before it can be used again. [...]"),
        item("heward_s_handy_spice_pouch", "Heward's Handy Spice Pouch", ItemRarity.COMMON, "Wondrous Item",
            "This belt pouch appears empty and has 10 charges. While holding the pouch, you can take a Magic action to expend 1 charge, name any nonmagical food seasoning (such as salt, pepper, saffron, or cilantro), and remove a pinch of the desired seasoning from the pouch. A pinch is enough to season a single meal. The pouch regains 1d6 + 4 expended charges daily at dawn."),
        item("horn_of_silent_alarm", "Horn of Silent Alarm", ItemRarity.COMMON, "Wondrous Item",
            "This horn has 4 charges and regains 1d4 expended charges daily at dawn. As a Magic action, you can blow the horn while expending 1 charge. One creature of your choice hears the horn's blare, provided that creature is within 600 feet of the horn. No other creature hears the horn."),
        item("instrument_of_scribing", "Instrument of Scribing", ItemRarity.COMMON, "Wondrous Item",
            "This musical instrument has 3 charges and regains all expended charges daily at dawn. While you are playing it, you can take a Magic action to expend 1 charge and write a magical message on a nonmagical object or surface that you can see within 30 feet of yourself. The message can be up to six words long and is written in a language you know. If you are a Bard, you can scribe an additional seven words and make the message glow faintly, allowing it to be seen in nonmagical Darkness. Casting the Dispel Magic spell on the message erases it. Otherwise, the message fades away after 24 hours."),
        item("lock_of_trickery", "Lock of Trickery", ItemRarity.COMMON, "Wondrous Item",
            "This lock appears to be an ordinary Lock (of the type described in chapter 6 of the Player's Handbook) and comes with a single key. The tumblers in this lock magically adjust to thwart burglars. Dexterity checks made to pick the lock have Disadvantage."),
        item("lute_of_thunderous_thumping", "Lute of Thunderous Thumping", ItemRarity.VERY_RARE, "Weapon (Club)",
            "This reinforced lute can be wielded as a magic Club that deals an extra 2d8 Thunder damage on a hit. Sing and Swing. If you're a Bard, you can use your Charisma modifier instead of your Strength modifier when making a melee attack roll with the lute, provided you sing or hum while making the attack."),
        item("magen_handbell", "Magen Handbell", ItemRarity.RARE, "Wondrous Item",
            "While holding this brass handbell, you can take a Magic action to ring it and summon a Terran Magen (see Netheril's Fall). The magen appears in an unoccupied space you choose within 30 feet of yourself, understands your languages, obeys your commands, and takes its turn immediately after you on your Initiative count. The magen disappears after 1 hour, when it dies, or when you dismiss it as a Bonus Action. The bell can't be used this way again for 1d6 days. Berserk Magen . Whenever the magen summoned by this item starts its turn Bloodied, roll 1d6. On a 6, the magen goes berserk. While berserk, the magen no longer obeys your commands, and you can't dismiss it as a Bonus Action. On each of its turns, the berserk magen attacks the nearest creature it can see. If no creature is near enough for the magen to move to and attack, the magen attacks an object. [...]", book = Sourcebook.NETHERILS_FALL),
        item("mariner_s_armor", "Mariner's Armor", ItemRarity.UNCOMMON, "Armor (Any Light, Medium, or Heavy)",
            "While wearing this armor, you have a Swim Speed equal to your Speed. In addition, if you start your turn underwater with 0 Hit Points, you immediately regain 1d4 Hit Points. The armor can't heal anyone again until the next dawn. The armor is decorated with fish and shell motifs."),
        item("mask_of_changed_appearance", "Mask of Changed Appearance", ItemRarity.COMMON, "Wondrous Item",
            "This jeweled mask has 3 charges. As a Magic action, you can expend 1 charge and change your face's appearance. You can't make yourself look like a different person, but you can smooth or deepen your wrinkles, whiten your teeth, hide or accentuate bags under your eyes, or perform other minor cosmetic changes. While your appearance is changed, the mask has the Invisible condition. Your changed appearance lasts 1 hour. Regaining Charges. The mask regains 1d3 expended charges daily at dawn. If you expend the last charge, roll 1d20. On a 1, the mask explodes in a harmless cloud of sweet-swelling powder and is destroyed.", book = Sourcebook.NETHERILS_FALL),
        item("moonblade", "Moonblade", ItemRarity.LEGENDARY, "Weapon (Greatsword, Longsword, Rapier, Scimitar, or Shortsword)",
            "Of all the magic items created by elves, one of the most prized and jealously guarded is a Moonblade . In ancient times, nearly all elven noble houses claimed one such weapon. Over the centuries, some of these weapons have faded from the world, their magic lost as family lines have become extinct. Others have vanished with their bearers during great quests. Thus, only a few of these weapons remain. Every Moonblade longs for a bearer whose disposition and goals are compatible with its own. If you try to attune to a Moonblade that doesn't want you as its bearer, the weapon not only rejects you but also places a curse on you, causing you to make D20 Tests with Disadvantage for 24 hours or until the curse is ended by a Remove Curse spell or similar magic. If you're accepted by the weapon and try to attune to it, you become attuned to it instantly, and a new rune appears on it. [...]", attunement = true, attunementNote = "by a Creature of the Weapon's Choice"),
        item("mystery_key", "Mystery Key", ItemRarity.COMMON, "Wondrous Item",
            "A question mark is worked into the head of this key. The key has a 5 percent chance of unlocking any lock into which it's inserted. Once it unlocks something, the key disappears."),
        item("mythallar_bracelet", "Mythallar Bracelet", ItemRarity.COMMON, "Wondrous Item",
            "Three small, crystal beads made from a decommissioned mythallar are strung on this leather bracelet. As a Magic action, you can pluck one bead from the bracelet to gain Advantage on Strength (Athletics) checks for 1 minute. A bead disappears immediately after it's plucked. Once all three beads have been plucked, the bracelet loses its magic.", book = Sourcebook.NETHERILS_FALL),
        item("mythallar_cloak", "Mythallar Cloak", ItemRarity.RARE, "Wondrous Item",
            "This electric-blue cloak is studded with sewn-in crystals that are shards of a decommissioned mythallar. The cloak has 10 charges and regains 1d10 expended charges daily at dawn. While wearing the cloak, you can take a Bonus Action and expend 1 charge to activate the cloak's magic. The magic lasts for 1 minute or until you end it early (no action required). While the cloak's magic is active, you gain a Fly Speed of 30 feet and can hover. Additionally, once on each of your turns when you hit a creature with an attack roll and deal damage, you can cause the target to take an extra 1d4 Radiant damage. If you are aloft when the cloak's magic ends, you fall.", attunement = true, book = Sourcebook.NETHERILS_FALL),
        item("nature_s_mantle", "Nature's Mantle", ItemRarity.UNCOMMON, "Wondrous Item",
            "This cloak shifts color and texture to blend with the terrain surrounding you. While wearing the cloak, you can use it as a Spellcasting Focus for your Druid and Ranger spells. While you are in an area that is Lightly Obscured, you can Hide as a Bonus Action even if you are being directly observed.", attunement = true, attunementNote = "by a Druid or Ranger"),
        item("niko_s_mace", "Niko's Mace", ItemRarity.VERY_RARE, "Weapon (Mace)",
            "This Mace has 6 charges and regains 1d6 expended charges daily at dawn. While holding the Mace, you can expend 1 of its charges to cast Summon Celestial (+9 to hit with spell attacks).", attunement = true, attunementNote = "by a Spellcaster", book = Sourcebook.UNI_LOST_HORN),
        item("orb_of_damara", "Orb of Damara", ItemRarity.ARTIFACT, "Wondrous Item",
            "A wizard named Damara created this orb while devising a way to become a dragon. Aided by a mysterious magician (actually an aspect of Tiamat in disguise), Damara eventually created an Artifact that would enable him to achieve his dream. Yet the potential power of his creation terrified Damara so much that he dared not use the orb. Instead, the wizard entombed himself with the Artifact to seal it away. The Orb of Damara is about 6 inches in diameter and is made of a glassy, iridescent material that's like obsidian but incredibly hard. If held to one's own lips with the intention of consuming it, the orb shrinks to the size of a grape. Properties of the Orb. While attuned to the orb, you gain the following benefits: Dragon's Breath. You can take a Magic action to exhale a 15-foot Cone. When you do, choose Acid, Cold, Fire, Lightning, or Poison. [...]", attunement = true, book = Sourcebook.ADVENTURES_IN_FAERUN),
        item("orb_of_direction", "Orb of Direction", ItemRarity.COMMON, "Wondrous Item",
            "This orb can be used as an Arcane Focus. While holding this orb, you can take a Magic action to determine which way is magnetic north. Nothing happens if the orb is used in a location that has no magnetic north."),
        item("orb_of_dragonkind", "Orb of Dragonkind", ItemRarity.ARTIFACT, "Wondrous Item",
            "Long ago, in the Dragonlance setting, elves and humans waged a terrible war against chromatic dragons. When the world seemed doomed, the wizards of the Towers of High Sorcery came together and forged five Orbs of Dragonkind to help defeat the dragons. One orb was taken to each of the five towers, and there they were used to speed the war toward a victorious end. The wizards used the orbs to lure dragons to them, then destroyed the dragons with powerful magic. As the Towers of High Sorcery fell in later ages, the orbs were destroyed or faded into legend, and only three are thought to survive. Their magic has been warped over the centuries. Their primary purpose of calling dragons still functions, but they also allow some measure of control over dragons. Each orb contains the essence of an evil dragon, a presence that resents any attempt to coax magic from it. [...]", attunement = true),
        item("orb_of_time", "Orb of Time", ItemRarity.COMMON, "Wondrous Item",
            "This orb can be used as an Arcane Focus. While holding the orb, you can take a Magic action to determine whether it is morning, afternoon, evening, or nighttime. This property functions only on the Material Plane."),
        item("perfume_of_bewitching", "Perfume of Bewitching", ItemRarity.COMMON, "Wondrous Item",
            "This tiny vial contains magic perfume, enough for one use. You can take a Magic action to apply the perfume to yourself, and its effect lasts 1 hour. For the duration, you have Advantage on all Charisma (Deception and Persuasion) checks made to influence a creature within 5 feet of yourself."),
        item("pipe_of_smoke_monsters", "Pipe of Smoke Monsters", ItemRarity.COMMON, "Wondrous Item",
            "While smoking this pipe, you can take a Magic action to exhale a puff of smoke that takes the form of a creature, such as a dragon, a flumph, or a slaad. The form must be small enough to fit in a 1-foot cube and loses its shape after a few seconds, becoming an ordinary puff of smoke."),
        item("pipes_of_pestilence", "Pipes Of Pestilence", ItemRarity.UNCOMMON, "Wondrous item",
            "If you play these pipes as a Magic action, you summon one Swarm of Corrupted Rats in an unoccupied space within 60 feet of yourself. The swarm is Friendly to you and your allies for as long as you continue to play the pipes each round as a Magic action. While Friendly, the swarm is under your control, obeys your commands, and takes no action other than to defend itself if you issue it no commands. Once your control ends, the swarm vanishes, and the pipes can't be used again until the next dawn.", attunement = true, book = Sourcebook.HELLFIRE_CLUB),
        item("poison_soaked_kukri", "Poison Soaked Kukri", ItemRarity.UNCOMMON, "Weapon",
            "You can take a Bonus Action to magically coat the blade of this Dagger with poison. The poison remains for 1 minute or until an attack using this weapon hits a creature. That creature must succeed on a DC 13 Constitution saving throw or take 2d8 Poison damage and have the Poisoned condition for 1 minute. This Bonus Action can't be used again this way until the next dawn.", attunement = true, book = Sourcebook.HELLFIRE_CLUB),
        item("pole_of_angling", "Pole of Angling", ItemRarity.COMMON, "Wondrous Item",
            "This item functions as a Pole. While holding it, you can take a Magic action to cause it to transform into a fishing pole with a hook, a line, and a reel, or have the fishing pole revert to a Pole."),
        item("pole_of_collapsing", "Pole of Collapsing", ItemRarity.COMMON, "Wondrous Item",
            "This item functions as a Pole. While holding it, you can take a Magic action to collapse it into a 1-foot-long rod for ease of storage (the pole's weight doesn't change) or cause the 1-foot-long rod to revert to a Pole. The rod elongates only as far as the surrounding space allows."),
        item("pot_of_awakening", "Pot of Awakening", ItemRarity.COMMON, "Wondrous Item",
            "If you plant an ordinary shrub in this 10-pound clay pot and let it grow for 30 days, the shrub magically transforms into an Awakened Shrub at the end of that time. When the shrub awakens, its roots break the pot, destroying it. The awakened shrub is Friendly toward you and obeys your commands. Absent commands from you, it does nothing."),
        item("potion_of_comprehension", "Potion of Comprehension", ItemRarity.COMMON, "Potion",
            "When you drink this potion, you gain the effect of the Comprehend Languages spell for 1 hour. This potion's liquid is a clear concoction with bits of salt and soot swirling in it."),
        item("potion_of_gaseous_form", "Potion of Gaseous Form", ItemRarity.RARE, "Potion",
            "When you drink this potion, you gain the effect of the Gaseous Form spell for 1 hour (no Concentration required) or until you end the effect as a Bonus Action. This potion's container seems to hold fog that moves and pours like water."),
        item("potion_of_greater_invisibility", "Potion of Greater Invisibility", ItemRarity.VERY_RARE, "Potion",
            "This potion's container looks empty but feels as though it holds liquid. When you drink the potion, you have the Invisible condition for 1 hour."),
        item("potion_of_pugilism", "Potion of Pugilism", ItemRarity.UNCOMMON, "Potion",
            "After you drink this potion, each Unarmed Strike you make deals an extra 1d6 Force damage on a hit. This effect lasts 10 minutes. This potion is a thick green fluid that tastes like spinach."),
        item("prosthetic_limb", "Prosthetic Limb", ItemRarity.COMMON, "Wondrous Item",
            "This magic item replaces a lost limb - a hand, an arm, a foot, a leg, or a similar body part. While the prosthetic is attached, it functions identically to the part it replaces. You can detach or reattach it as a Magic action, and it can't be removed against your will while you are alive."),
        item("quarterstaff_of_the_acrobat", "Quarterstaff of the Acrobat", ItemRarity.VERY_RARE, "Weapon (Quarterstaff)",
            "You have a +2 bonus to attack rolls and damage rolls made with this magic weapon. While holding this weapon, you can cause it to emit green Dim Light out to 10 feet, either as a Bonus Action or after you roll Initiative, or you can extinguish the light as a Bonus Action. While holding this weapon, you can take a Bonus Action to alter its form, turning it into a 6-inch rod (for ease of storage) or a 10-foot pole, or reverting it a Quarterstaff; the weapon will elongate only as far as the surrounding space allows. In certain forms, the weapon has the following additional properties. Acrobatic Assist (Quarterstaff and 10-Foot Pole Forms Only). While holding this weapon, you have Advantage on Dexterity (Acrobatics) checks. Attack Deflection (Quarterstaff Form Only). [...]", attunement = true),
        item("quiver_of_ehlonna", "Quiver of Ehlonna", ItemRarity.UNCOMMON, "Wondrous Item",
            "Each of the quiver's three compartments connects to an extradimensional space that allows the quiver to hold numerous items while never weighing more than 2 pounds. The shortest compartment can hold up to 60 Arrows, Bolts, or similar objects. The midsize compartment holds up to 18 Javelins or similar objects. The longest compartment holds up to 6 long objects, such as bows, Quarterstaffs, or Spears. You can draw any item the quiver contains as if doing so from a regular quiver or scabbard."),
        item("reliquary_of_dawn", "Reliquary of Dawn", ItemRarity.UNCOMMON, "Wondrous Item",
            "This talisman contains a discarded claw, scale, or fang from Eirdu, which many believe is the incarnation of the sun in Lorwyn. While you wear a Reliquary of Dawn, you are immune to the effects of Eventide.", book = Sourcebook.LORWYN),
        item("reliquary_of_twilight", "Reliquary of Twilight", ItemRarity.UNCOMMON, "Wondrous Item",
            "This talisman contains a discarded claw, scale, or fang from Isilu, which many believe is the incarnation of the moon in Shadowmoor. While you wear a Reliquary of Twilight, you are immune to the effects of Morningtide.", book = Sourcebook.LORWYN),
        item("ring_of_elemental_command", "Ring of Elemental Command", ItemRarity.LEGENDARY, "Ring",
            "Each Ring of Elemental Command is linked to one of the four Elemental Planes. The DM chooses or randomly determines the linked plane. For example, a Ring of Elemental Command (air) is linked to the Elemental Plane of Air. Every Ring of Elemental Command has the following two properties: Elemental Bane. While wearing the ring, you have Advantage on attack rolls against Elementals and they have Disadvantage on attack rolls against you. Elemental Compulsion. While wearing the ring, you can take a Magic action to try to compel an Elemental you see within 60 feet of yourself. The Elemental makes a DC 18 Wisdom saving throw. On a failed save, the Elemental has the Charmed condition until the start your next turn, and you determine what it does with its move and action on its next turn. Elemental Focus. [...]", attunement = true),
        item("rival_coin", "Rival Coin", ItemRarity.COMMON, "Wondrous Item",
            "This gold coin has a creature embossed on each side. The two depicted creatures must be famous rivals or enemies of each other. For example, a Rival Coin might show Iggwilv on one side and Mordenkainen on the other, or Venger on one side and Tiamat on the other. One of these figures is on the \"heads\" side of the coin, the other on the \"tails\" side. The coin has 1 charge and regains its expended charge daily at dawn. You can take a Magic action to toss the coin, expending its charge. Roll any die to determine whether the coin comes up heads (on an even number) or tails (on an odd number). The roll also determines the effect: Heads. Target one creature you can see within 60 feet of yourself. The target makes a DC 13 Wisdom saving throw. On a failed save, the target takes 2d4 Psychic damage and has Disadvantage on the next attack roll it makes before the end of its next turn. [...]"),
        item("rod_of_resurrection", "Rod of Resurrection", ItemRarity.LEGENDARY, "Rod",
            "The rod has 5 charges. While you hold it, you can cast one of the following spells from it: Heal (expends 1 charge) or Resurrection (expends 5 charges). The rod regains 1 expended charge daily at dawn. If you expend the last charge, roll 1d20. On a 1, the rod disappears in a harmless burst of radiance.", attunement = true),
        item("rod_of_the_pact_keeper", "Rod of the Pact Keeper", ItemRarity.UNCOMMON, "Rod",
            "While holding this rod, you gain a bonus to spell attack rolls and to the saving throw DCs of your Warlock spells. The bonus is determined by the rod's rarity. In addition, you can regain one spell slot as a Magic action while holding the rod. You can't use this property again until you finish a Long Rest.", attunement = true, attunementNote = "by a Warlock"),
        item("rope_of_mending", "Rope of Mending", ItemRarity.COMMON, "Wondrous Item",
            "This 50-foot coil of rope can repair itself when cut into any number of smaller pieces. As a Magic action, you can cause all pieces of the rope that are in contact with each other and not otherwise in use to knit back together. A Rope of Mending is forever shortened if a section of it is lost or destroyed."),
        item("ruby_of_the_war_mage", "Ruby of the War Mage", ItemRarity.COMMON, "Wondrous Item",
            "Etched with eldritch runes, this 1-inch-diameter ruby allows you to use a Simple or Martial weapon as a Spellcasting Focus for your spells. For this property to work, you must attach the ruby to the weapon by pressing the ruby against it for at least 10 minutes. Thereafter, the ruby can't be removed unless you detach it as a Magic action, the weapon is destroyed, or your Attunement to the ruby ends.", attunement = true, attunementNote = "by a Spellcaster"),
        item("salubrious_armor", "Salubrious Armor", ItemRarity.RARE, "Armor (Plate Armor or Scale Mail)",
            "While wearing this armor, you gain a +1 bonus to Armor Class. Whenever you regain Hit Points, the armor takes on a reddish tint, and this bonus increases to +2 until the end of your next turn.", attunement = true, book = Sourcebook.DDB_DROPS),
        item("scroll_of_protection", "Scroll of Protection", ItemRarity.RARE, "Scroll",
            "Each Scroll of Protection works against creatures of a specific creature type chosen by the DM or determined by rolling on the following table. 1d100 Creature Type 01-10 Aberrations 11-15 Beasts 16-20 Celestials 21-25 Constructs 26-35 Dragons 36-45 Elementals 46-50 Humanoids 51-60 Fey 61-70 Fiends 71-75 Giants 76-80 Monstrosities 81-85 Oozes 86-90 Plants 91-00 Undead Using a Magic action to read the scroll creates a 5-foot Emanation originating from you. For 5 minutes, creatures of the specified type can't enter or affect anything in the area. However, if you move in such a way that a creature of the specified type would be inside the area, the effect ends. As a Magic action, a creature within 5 feet of the Emanation can attempt to overcome it, which forces the creature to make a DC 15 Charisma saving throw. On a successful save, the creature ceases to be affected by the Emanation."),
        item("scroll_of_titan_summoning", "Scroll of Titan Summoning", ItemRarity.LEGENDARY, "Scroll",
            "When you take a Magic action to read this scroll, a particular titan named in the scroll appears in an unoccupied space on the ground or in water that you can see within 1 mile of yourself. The DM picks a suitable titan or determines it randomly by rolling on the table below (see the Monster Manual for the creature's stat block). The titan is Hostile toward all other creatures and disappears when it drops to 0 Hit Points. If the titan is summoned into a space that isn't large enough to contain it, the summoning fails, and the scroll is wasted. 1d100 Titan 01-15 Animal Lord 16-30 Blob of Annihilation 31-45 Colossus 46-60 Elemental Cataclysm 61-75 Empyrean 76-90 Kraken (a kraken requires a body of water large enough to contain it, or the summoning fails and the scroll is wasted) 91-00 Tarrasque"),
        item("shield_1_2_or_3", "Shield, +1, +2, or +3", ItemRarity.UNCOMMON, "Armor (Shield)",
            "While holding this Shield, you have a bonus to Armor Class determined by the Shield's rarity, in addition to the Shield's normal bonus to AC."),
        item("shield_of_expression", "Shield of Expression", ItemRarity.COMMON, "Armor (Shield)",
            "The front of this Shield is shaped in the likeness of a face. While bearing the Shield, you can take a Bonus Action to alter the face's expression."),
        item("shield_of_the_cavalier", "Shield of the Cavalier", ItemRarity.VERY_RARE, "Armor (Shield)",
            "While holding this Shield, you have a +2 bonus to Armor Class. This bonus is in addition to the Shield's normal bonus to AC. The Shield has the following additional properties that you can use while holding it. Forceful Bash. When you take the Attack, you can make one of the attack rolls using the Shield against a target within 5 feet of yourself. Apply your Proficiency Bonus and Strength modifier to the attack roll. On a hit, the Shield deals Force damage to the target equal to 2d6 + 2 plus your Strength modifier, and if the target is a creature, you can push it up to 10 feet directly away from yourself. If the creature is your size or smaller, you can also knock it down, giving it the Prone condition. Protective Field. [...]", attunement = true),
        item("silencing_satchel", "Silencing Satchel", ItemRarity.UNCOMMON, "Wondrous Item",
            "This magic satchel has 3 charges and regains 1d3 expended charges daily at dawn. As a Magic action, you can expend 1 charge and tighten the satchel's drawstring to inflict a silencing hex on a creature you can see within 60 feet of yourself. The target must succeed on a DC 15 Charisma saving throw or be cursed for 1 minute. While the target is cursed, its mouth is sealed shut. The target can't cast any spell with a Verbal component, consume potions, or take in food or drink. The target makes a DC 15 Charisma save at the end of each of its turns, ending the curse early on a success.", book = Sourcebook.DDB_DROPS),
        item("silvered_weapon", "Silvered Weapon", ItemRarity.COMMON, "Weapon (Any Simple or Martial)",
            "An alchemical process has bonded silver to this magic weapon. When you score a Critical Hit with it against a creature that is shape-shifted, the weapon deals one additional die of damage."),
        item("smoldering_armor", "Smoldering Armor", ItemRarity.COMMON, "Armor (Any Heavy, Medium, or Light)",
            "Wisps of harmless, odorless smoke rise from this armor while it is worn."),
        item("spiked_shield", "Spiked Shield", ItemRarity.UNCOMMON, "Armor (shield)",
            "Crude metal spikes adorn this magic Shield. It has the following properties that you can use while holding it. Shield Bash. When you take the Attack action, you can make one of the attack rolls using the Shield against a target within 5 feet of yourself. Apply your Proficiency Bonus and Strength modifier to the attack roll. On a hit, the Shield deals Piercing damage equal to 2d6 plus your Strength modifier. Spike Salvo. As a Magic action, you can cause spikes to erupt from the Shield in a 30-foot Cone. Each creature in the Cone makes a DC 13 Dexterity saving throw, taking 3d6 Piercing damage on a failed save or half as much damage on a successful one. Once this property is used, it can't be used again until the next dawn.", attunement = true, book = Sourcebook.HELLFIRE_CLUB),
        item("spirit_board", "Spirit Board", ItemRarity.VERY_RARE, "Wondrous Item",
            "This ornate wooden board has the letters of the Common alphabet printed on one side, alongside the words \"Yes\" and \"No\" and symbols representing \"Weal\" and \"Woe.\" The board comes with a heart-shaped, wooden planchette. This planchette must be resting on the lettered side of the board for the board's magic to function. This board has 3 charges and regains 1 expended charge daily at dawn. While touching the planchette, you can take 1 minute to cast one of the spells on the table below. The table indicates how many charges you must expend to cast the spell. As you cast the spell, you call on the spirits of the dead to guide the planchette across the board's surface, answering your questions by pointing to the letters or words on the board. Spell Charge Cost Augury 1 Commune 3"),
        item("staff_of_adornment", "Staff of Adornment", ItemRarity.COMMON, "Staff",
            "If you place a Tiny object weighing no more than 1 pound (such as a shard of crystal, an egg, or a stone) above the tip of this staff while holding it, the object floats an inch from the staff's tip and remains there until it is removed or until the staff is no longer in your possession. The staff can have up to three such objects floating over its tip at any given time. While holding the staff, you can make one or more of the objects slowly spin or turn in place."),
        item("staff_of_birdcalls", "Staff of Birdcalls", ItemRarity.COMMON, "Staff",
            "This wooden staff is decorated with bird carvings. It has 10 charges. While holding it, you can take a Magic action to expend 1 charge from the staff and cause it to create one of the following sounds, which can be heard out to 120 feet: a finch's chirp, a raven's caw, a duck's quack, a chicken's cluck, a goose's honk, a loon's call, a turkey's gobble, a seagull's cry, an owl's hoot, or an eagle's shriek. Regaining Charges. The staff regains 1d6 + 4 expended charges daily at dawn. If you expend the last charge, roll 1d20. On a 1, the staff explodes in a harmless cloud of bird feathers and is lost forever."),
        item("staff_of_flowers", "Staff of Flowers", ItemRarity.COMMON, "Staff",
            "This wooden staff has 10 charges. While holding it, you can take a Magic action to expend 1 charge from the staff and cause a flower to sprout from a patch of earth or soil within 5 feet of yourself, or from the staff itself. Unless you choose a specific kind of flower, the staff creates a mild-scented daisy. The flower is harmless and nonmagical, and it grows or withers as a normal flower would. Regaining Charges. The staff regains 1d6 + 4 expended charges daily at dawn. If you expend the last charge, roll 1d20. On a 1, the staff turns into flower petals and is lost forever."),
        item("staff_of_the_adder", "Staff of the Adder", ItemRarity.UNCOMMON, "Staff",
            "As a Bonus Action, you can turn the head of this staff into that of an animate, venomous snake for 1 minute or revert the staff to its inanimate form. When you take the Attack action, you can make one of the attack rolls using the animated snake head, which has a reach of 5 feet. Apply your Proficiency Bonus and Wisdom modifier to the attack roll. On a hit, the target takes 1d6 Piercing damage and 3d6 Poison damage. The snake head can be attacked while it is animate. It has AC 15, HP 20, and Immunity to Poison and Psychic damage. If the head drops to 0 Hit Points, the staff is destroyed. As long as it's not destroyed, the staff regains all lost Hit Points when it reverts to its inanimate form.", attunement = true),
        item("stone_of_good_luck_luckstone", "Stone of Good Luck (Luckstone)", ItemRarity.UNCOMMON, "Wondrous Item",
            "While this polished agate is on your person, you gain a +1 bonus to ability checks and saving throws.", attunement = true),
        item("stormwalker_s_cloak", "Stormwalker's Cloak", ItemRarity.RARE, "Wondrous Item",
            "While wearing this cloak, you gain the following benefits. Shocking Feedback. The cloak has 3 charges and regains all expended charges daily at dawn. You can expend 1 charge to cast Hellish Rebuke from the cloak (DC 13), and the spell deals your choice of Lightning or Thunder damage instead of the usual Fire damage. Storm Resistance. You have Resistance to Lightning and Thunder damage.", attunement = true, book = Sourcebook.DDB_DROPS),
        item("sword_of_answering", "Sword of Answering", ItemRarity.LEGENDARY, "Weapon (Longsword)",
            "You gain a +3 bonus to attack rolls and damage rolls made with this sword. In addition, while you hold the sword, you can take a Reaction to make one melee attack with it against any creature in your reach that deals damage to you. You have Advantage on the attack roll, and any damage dealt with this special attack ignores any Immunity or Resistance the target has to that damage.", attunement = true),
        item("sword_of_kas", "Sword of Kas", ItemRarity.ARTIFACT, "Weapon (Longsword)",
            "Kas was a powerful warrior who served Vecna and whose loyalty was rewarded with this sword. As Kas's power grew, so did his hubris. The sword urged Kas to destroy Vecna and usurp his throne. Legend says Vecna's destruction came at Kas's hand, but Vecna also wrought his rebellious lieutenant's doom, leaving only Kas's sword behind. Bloodthirst. The sword thirsts for blood. If the sword doesn't taste blood on its blade within 1 minute of being drawn from its scabbard, its wielder makes a DC 15 Charisma saving throw. On a successful save, the wielder takes 3d6 Psychic damage. On a failed save, the wielder is dominated by the sword, as if by the Dominate Monster spell, and the sword demands blood. The spell effect ends when the sword's demand is met. Magic Weapon. [...]", attunement = true),
        item("sword_of_vengeance", "Sword of Vengeance", ItemRarity.UNCOMMON, "Weapon (Glaive, Greatsword, Longsword, Rapier, Scimitar, or Shortsword)",
            "You gain a +1 bonus to attack rolls and damage rolls made with this magic weapon. Curse. This weapon is cursed and possessed by a vengeful spirit. Becoming attuned to it extends the curse to you. As long as you remain cursed, you are unwilling to part with the weapon, keeping it on your person at all times. While attuned to this weapon, you have Disadvantage on attack rolls made with weapons other than this one. In addition, while the weapon is on your person, you must succeed on a DC 15 Wisdom saving throw whenever you take damage from another creature in combat. On a failed save, you must attack the creature that damaged you until you drop to 0 Hit Points or it does or until you can't reach the creature to make a melee attack against it. You can break the curse in the usual ways. Alternatively, casting Banishment on the weapon forces the vengeful spirit to leave it. [...]", attunement = true),
        item("sylvan_talon", "Sylvan Talon", ItemRarity.COMMON, "Weapon (Dagger, Rapier, Scimitar, Shortsword, Sickle, or Spear)",
            "While this weapon is on your person, you understand the nonwritten communication of all Fey, and they understand yours. Secret Message. As a Magic action, you can use the weapon to cast Message . Once this property is used, it can't be used again until the next dawn.", attunement = true),
        item("sync_ring", "Sync Ring", ItemRarity.UNCOMMON, "Ring",
            "When you attune to this ring, choose a blank book you are touching. Thereafter, whenever you write something using the hand that wears the ring, a copy of your writing magically appears in the chosen book. Magical writing, such as a spell copied from a Spell Scroll or a glyph inscribed for the Glyph of Warding spell, is transcribed literally but is nonmagical. If no blank pages are left in the book or the book is on a different plane of existence than you for more than 24 hours, your Attunement to the ring ends.", attunement = true, book = Sourcebook.DDB_DROPS),
        item("talking_doll", "Talking Doll", ItemRarity.COMMON, "Wondrous Item",
            "While this doll is within 5 feet of you, you can spend a Short Rest telling it to say up to six phrases, none of which can be more than six words long, and set a condition under which the doll speaks each phrase. You can also replace old phrases with new ones. Whatever the condition, it must occur within 5 feet of the doll to make it speak. For example, whenever someone picks up the doll, it might say, \"I want a piece of candy.\" The doll's phrases are lost when your Attunement to the doll ends.", attunement = true),
        item("tankard_of_sobriety", "Tankard of Sobriety", ItemRarity.COMMON, "Wondrous Item",
            "This tankard has a stern face sculpted into one side. You can drink ale, wine, or any other nonmagical alcoholic beverage poured into it without becoming inebriated. The tankard has no effect on magical liquids or harmful substances such as poison."),
        item("thayan_spell_tattoo", "Thayan Spell Tattoo", ItemRarity.UNCOMMON, "Wondrous Item",
            "This magical tattoo contains the essence of a level 1-3 spell chosen by the tattoo's creator. While the tattoo is on your body, you always have the associated spell prepared, and you can cast the spell using any spell slots you have of the appropriate level. If you have the Spellcasting or Pact Magic feature, the spell uses your spellcasting ability; otherwise, if the spell requires a saving throw or an attack roll, the spell save DC is 13 and the attack bonus is +5. If your Attunement to the tattoo ends, the tattoo vanishes. You can also cast the spell once without a spell slot and spell components. Once the spell is cast in this way, the tattoo vanishes. The Thayan art of magical tattooing is highly guarded by its practitioners, and thus, Thayan Spell Tattoos can't be crafted like other magic items. [...]", attunement = true, book = Sourcebook.HEROES_OF_FAERUN),
        item("thunderous_greatclub", "Thunderous Greatclub", ItemRarity.VERY_RARE, "Weapon (Greatclub)",
            "While you are attuned to this magic weapon, your Strength is 20 unless your Strength is already equal to or greater than that score. The weapon deals an extra 1d8 Thunder damage to any creature it hits and an extra 3d8 Thunder damage to objects it hits that aren't being worn or carried. The weapon has the following additional properties. Clap of Thunder. As a Magic action, you can strike the weapon against a hard surface to create a loud clap of thunder audible out to 300 feet. You also create a 30-foot Cone of thunderous energy. Each creature in the Cone must succeed on a DC 15 Strength saving throw or have the Prone condition. Nonmagical objects in the Cone that aren't being worn or carried take 3d8 Thunder damage. Earthquake. [...]", attunement = true),
        item("tome_of_the_dragon", "Tome of the Dragon", ItemRarity.LEGENDARY, "Wondrous Item",
            "This heavy tome is bound in dragonhide and reinforced with thick bands of cold iron. The original copies were made by wyrmspeakers of the Cult of the Dragon and bear that cult's symbol on their cover. The Tome of the Dragon contains the secret ritual to transform a dragon or a dead dragon's body into a dracolich. To decipher and use the Tome of the Dragon , you must be either a spellcaster with at least two level 5 spell slots or a Dragon. A single assistant (or more, at the DM's discretion) can help you conduct the ritual, so long as that assistant also meets the requirements to use the tome. To create a dracolich using a Tome of the Dragon , you must have continuous access to the body of an adult or ancient dragon (either living or dead), as well as 80,000 GP worth of supplies (which includes a gem that will serve as the anchor for the dracolich's spirit). [...]", book = Sourcebook.ADVENTURES_IN_FAERUN),
        item("unraveling_cloak", "Unraveling Cloak", ItemRarity.COMMON, "Wondrous Item",
            "When you take a Magic action to remove the button from the hood of this cloak, the cloak's fabric unravels behind you wherever you go, leaving a continuous thread. Other creatures can see the thread only if they take the Search action to look for it and succeed on a DC 15 Wisdom (Perception) check. The cloak ceases to unravel after 100 miles or if you take a Magic action to rebutton the cloak. If you teleport while the cloak is unraveling, any unraveled thread is immediately destroyed.", book = Sourcebook.DDB_DROPS),
        item("veteran_s_cane", "Veteran's Cane", ItemRarity.COMMON, "Wondrous Item",
            "As a Bonus Action, you can transform this walking cane into an ordinary Longsword or change the Longsword back into a walking cane. In either case, you must be holding the item."),
        item("walloping_ammunition", "Walloping Ammunition", ItemRarity.COMMON, "Weapon (Any Ammunition)",
            "A creature hit by this ammunition must succeed on a DC 10 Strength saving throw or have the Prone condition."),
        item("wand_of_conducting", "Wand of Conducting", ItemRarity.COMMON, "Wand",
            "This wand has 3 charges. While holding it, you can take a Magic action to expend 1 charge and create orchestral music by waving it around. The music can be heard out to 120 feet and ends when you stop waving the wand. Regaining Charges. The wand regains all expended charges daily at dawn. If you expend the wand's last charge, roll 1d20. On a 1, a sad tuba sound plays as the wand crumbles into dust and is destroyed."),
        item("wand_of_misdirection", "Wand of Misdirection", ItemRarity.RARE, "Wand",
            "This wand has 4 charges and regains 1d4 expended charges daily at dawn. While holding the wand, you can expend 1 charge to cast Mislead from it.", attunement = true, attunementNote = "by a Spellcaster", book = Sourcebook.DDB_DROPS),
        item("wand_of_orcus", "Wand of Orcus", ItemRarity.ARTIFACT, "Wand",
            "Crafted and wielded by Orcus, this ghastly wand slips from the demon lord's grasp from time to time. When it does, it magically appears wherever the demon lord senses an opportunity to achieve some fell goal. The wand is topped with a skull that once belonged to a human hero slain by Orcus. The wand can magically change in size to better conform to the grip of its user. All Holy Water within 10 feet of the wand is destroyed. Any creature besides Orcus that tries to attune to the wand makes a DC 17 Constitution saving throw. On a successful save, the creature takes 10d6 Necrotic damage. On a failed save, the creature dies and, if it is a Humanoid, turns into a Zombie. Magic Weapon. You can wield the wand as a magic Mace that grants a +3 bonus to attack rolls and damage rolls made with it. The wand deals an extra 2d12 Necrotic damage on a hit. Random Properties. [...]", attunement = true),
        item("wand_of_pyrotechnics", "Wand of Pyrotechnics", ItemRarity.COMMON, "Wand",
            "This wand has 7 charges. While holding it, you can take a Magic action to expend 1 charge and create a harmless burst of multicolored light at a point you can see up to 120 feet away. The burst of light is accompanied by a crackling noise that can be heard up to 300 feet away. The light is as bright as a torch flame but lasts only a second. Regaining Charges. The wand regains 1d6 + 1 expended charges daily at dawn. If you expend the wand's last charge, roll 1d20. On a 1, the wand erupts in a harmless pyrotechnic display and is destroyed."),
        item("wand_of_the_war_mage_1_2_or_3", "Wand of the War Mage, +1, +2 or +3", ItemRarity.UNCOMMON, "Wand",
            "While holding this wand, you gain a bonus to spell attack rolls determined by the wand's rarity. In addition, you ignore Half Cover when making a spell attack roll.", attunement = true, attunementNote = "by a Spellcaster"),
        item("wave", "Wave", ItemRarity.ARTIFACT, "Weapon (Trident)",
            "Held in the dungeon of White Plume Mountain, Wave is engraved with images of waves, shells, and sea creatures. You gain a +3 bonus to attack rolls and damage rolls made with this magic weapon. When you roll a 20 on the d20 for an attack roll with this weapon, the target takes an extra 21 Necrotic damage. While holding Wave , you gain the following benefits: Combat Ready. You have Advantage on Initiative rolls. Underwater Adaptation. A bubble of air forms around your head while you are underwater, allowing you to breathe normally in that environment. Aquatic Command. Wave has 3 charges and regains 1d3 expended charges daily at dawn. While you carry it, you can expend 1 charge to cast Dominate Beast (save DC 20) from it on a Beast that has a Swim Speed. Globe of Invulnerability. While holding Wave , you can cast the level 9 version of Globe of Invulnerability from it. [...]", attunement = true),
        item("weapon_1_2_or_3", "Weapon, +1, +2 or +3", ItemRarity.UNCOMMON, "Weapon (Any Simple or Martial)",
            "You have a bonus to attack rolls and damage rolls made with this magic weapon. The bonus is determined by the weapon's rarity."),
        item("whelm", "Whelm", ItemRarity.ARTIFACT, "Weapon (Warhammer)",
            "Whelm is a powerful weapon forged by dwarves and lost in the dungeon of White Plume Mountain. You gain a +3 bonus to attack rolls and damage rolls made with this magic weapon. Hurl. Whelm has the Thrown property with a normal range of 60 feet and a long range of 180 feet. When you hit with a ranged attack roll using Whelm , the target takes an extra 1d8 Force damage, or an extra 4d8 Force damage if the target is a Construct, an Elemental, or a Giant. Immediately after hitting or missing, the weapon flies back to your hand. Shock Wave. You can take a Magic action to strike the ground with Whelm and send a shock wave out from the point of impact. Each creature of your choice on the ground within 60 feet of that point must succeed on a DC 20 Constitution saving throw or have the Stunned condition for 1 minute. [...]", attunement = true, attunementNote = "by a Dwarf or a Creature Attuned to a Belt of Dwarvenkind"),
        item("windskiff", "Windskiff", ItemRarity.RARE, "Wondrous Item",
            "This small piece of jewelry has 3 charges and regains all expended charges daily at dawn. While you are holding it, you can take a Magic action to expend 1 charge, which causes the jewelry to transform into a sail-powered personal vehicle. The vehicle is roughly the size and shape of a household door with a 10-foot-tall sail extending from one side. After 1 hour or until you use the item's command word (no action required), the transformation ends and the Windskiff reverts to its jewelry form. In vehicle form, a Windskiff is a Medium object with the following statistics: AC 12, HP 30, and Speed 40 ft. The Windskiff hovers a few inches above whatever surface it's on and can glide; it moves 5 feet horizontally for every 1 foot it descends in the air. A Windskiff and its riders take no damage from falling.", book = Sourcebook.HEROES_OF_FAERUN),
        item("wraps_of_unarmed_power", "Wraps of Unarmed Power", ItemRarity.UNCOMMON, "Wondrous Item",
            "While wearing these wraps, you have a bonus to attack rolls and damage rolls made with your Unarmed Strikes. The bonus is determined by the wraps' rarity, and those strikes deal your choice of Force damage or their normal damage type."),
    )

    val ALL: List<MagicItem> =
        (ARMOR_AND_SHIELDS + WEAPONS + RINGS + WANDS_AND_STAFFS + WONDROUS_ITEMS + POTIONS +
            MORE_ARMOR + MORE_WEAPONS + MORE_RINGS + MORE_WANDS_AND_STAFFS + MORE_WONDROUS +
            IMPORTED)
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
