package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.CharClass
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.data.model.FeatureChoiceOption
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.data.model.SpellStub
import com.pedroeu.ficha.data.model.Trait

object ClassData {

    private fun skillChoice(count: Int, options: List<Skill>) =
        ClassChoice.SkillProficiencyChoice("skills", "Skill Proficiencies", count, options)

    private fun cantrips(id: String, label: String, count: Int, options: List<SpellStub>) =
        ClassChoice.CantripChoice(id, label, count, options)

    val ALL: List<CharClass> = listOf(
        CharClass(
            id = "barbarian",
            name = "Barbarian",
            hitDie = 12,
            primaryAbility = listOf(Ability.STR),
            savingThrows = listOf(Ability.STR, Ability.CON),
            armorProficiencies = listOf("Light", "Medium", "Shields"),
            weaponProficiencies = listOf("Simple", "Martial"),
            toolProficiencies = emptyList(),
            level1Features = listOf(
                Trait("Rage", "As a bonus action, enter a rage that grants bonus melee damage, resistance to bludgeoning, piercing, and slashing damage, and advantage on Strength checks and saves. Lasts up to 1 minute."),
                Trait("Unarmored Defense", "While not wearing armor, your AC equals 10 + your Dexterity modifier + your Constitution modifier."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.ANIMAL_HANDLING, Skill.ATHLETICS, Skill.INTIMIDATION, Skill.NATURE, Skill.PERCEPTION, Skill.SURVIVAL)),
            ),
            isSpellcaster = false,
            summary = "A fierce warrior who channels primal rage into devastating attacks and unmatched resilience.",
        ),
        CharClass(
            id = "bard",
            name = "Bard",
            hitDie = 8,
            primaryAbility = listOf(Ability.CHA),
            savingThrows = listOf(Ability.DEX, Ability.CHA),
            armorProficiencies = listOf("Light"),
            weaponProficiencies = listOf("Simple", "Hand Crossbows", "Longswords", "Rapiers", "Shortswords"),
            toolProficiencies = listOf("Three musical instruments of your choice"),
            level1Features = listOf(
                Trait("Bardic Inspiration", "As a bonus action, give a creature within 60 feet a d6 Bardic Inspiration die to add to one ability check, attack roll, or saving throw. Uses equal to your Charisma modifier per long rest."),
                Trait("Spellcasting", "You cast Bard spells using Charisma as your spellcasting ability."),
            ),
            choices = listOf(
                skillChoice(3, Skill.ALL),
                cantrips("cantrips", "Cantrips Known", 2, listOf(
                    SpellStub("bard_vicious_mockery", "Vicious Mockery", 0, "Enchantment", "Unleash a string of insults that deals psychic damage and gives disadvantage on the target's next attack."),
                    SpellStub("bard_minor_illusion", "Minor Illusion", 0, "Illusion", "Create a small sound or image illusion within range."),
                    SpellStub("bard_dancing_lights", "Dancing Lights", 0, "Evocation", "Create up to four torch-sized lights that you can move around the battlefield."),
                    SpellStub("bard_mage_hand", "Mage Hand", 0, "Conjuration", "Conjure a spectral hand to manipulate objects at range."),
                    SpellStub("bard_prestidigitation", "Prestidigitation", 0, "Transmutation", "A minor magical trick: clean, chill, warm, flavor, or light a small object."),
                    SpellStub("bard_true_strike", "True Strike", 0, "Divination", "Briefly glimpse the future to gain insight into your next attack."),
                )),
                cantrips("spells1", "1st-Level Spells Known", 4, listOf(
                    SpellStub("bard_healing_word", "Healing Word", 1, "Evocation", "A word of power that heals a creature at range."),
                    SpellStub("bard_faerie_fire", "Faerie Fire", 1, "Evocation", "Outline creatures in colorful light, making them easier to hit."),
                    SpellStub("bard_dissonant_whispers", "Dissonant Whispers", 1, "Enchantment", "Whisper a discordant melody that deals psychic damage and forces the target to flee."),
                    SpellStub("bard_charm_person", "Charm Person", 1, "Enchantment", "Attempt to charm a humanoid into regarding you as a friend."),
                    SpellStub("bard_tashas", "Tasha's Hideous Laughter", 1, "Enchantment", "Send a creature into fits of laughter, incapacitating it."),
                    SpellStub("bard_thunderwave", "Thunderwave", 1, "Evocation", "A wave of thunderous force pushes creatures away from you."),
                    SpellStub("bard_comprehend", "Comprehend Languages", 1, "Divination", "Understand any spoken or written language for the duration."),
                    SpellStub("bard_disguise_self", "Disguise Self", 1, "Illusion", "Change your appearance, including clothing and gear."),
                )),
            ),
            isSpellcaster = true,
            spellcastingAbility = Ability.CHA,
            summary = "A charismatic performer who weaves magic through music, words, and inspiration.",
        ),
        CharClass(
            id = "cleric",
            name = "Cleric",
            hitDie = 8,
            primaryAbility = listOf(Ability.WIS),
            savingThrows = listOf(Ability.WIS, Ability.CHA),
            armorProficiencies = listOf("Light", "Medium", "Shields"),
            weaponProficiencies = listOf("Simple"),
            toolProficiencies = emptyList(),
            level1Features = listOf(
                Trait("Spellcasting", "You cast Cleric spells using Wisdom. You prepare a number of spells from the full Cleric spell list each day equal to your Wisdom modifier + your Cleric level."),
                Trait("Channel Divinity", "You can channel divine energy to fuel magical effects, such as Turn Undead. Usable once per short or long rest."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.HISTORY, Skill.INSIGHT, Skill.MEDICINE, Skill.PERSUASION, Skill.RELIGION)),
                ClassChoice.FeatureOption("divine_order", "Divine Order", listOf(
                    FeatureChoiceOption("protector", "Protector", "You gain proficiency with martial weapons and heavy armor."),
                    FeatureChoiceOption("thaumaturge", "Thaumaturge", "You learn an extra cantrip from the Cleric list and add your Wisdom modifier to Religion checks."),
                )),
                cantrips("cantrips", "Cantrips Known", 3, listOf(
                    SpellStub("cleric_guidance", "Guidance", 0, "Divination", "Touch a creature to add a d4 to one ability check."),
                    SpellStub("cleric_light", "Light", 0, "Evocation", "Cause an object to shed bright light in a 20-foot radius."),
                    SpellStub("cleric_sacred_flame", "Sacred Flame", 0, "Evocation", "Radiant flame descends on a target, ignoring cover."),
                    SpellStub("cleric_spare_dying", "Spare the Dying", 0, "Necromancy", "Stabilize a dying creature with a touch."),
                    SpellStub("cleric_thaumaturgy", "Thaumaturgy", 0, "Transmutation", "Manifest a minor, wondrous sign of divine power."),
                    SpellStub("cleric_toll_dead", "Toll the Dead", 0, "Necromancy", "A dolorous bell tolls, dealing necrotic damage."),
                )),
            ),
            isSpellcaster = true,
            spellcastingAbility = Ability.WIS,
            summary = "A conduit for divine power, blending healing magic with the favor of a deity.",
        ),
        CharClass(
            id = "druid",
            name = "Druid",
            hitDie = 8,
            primaryAbility = listOf(Ability.WIS),
            savingThrows = listOf(Ability.INT, Ability.WIS),
            armorProficiencies = listOf("Light (nonmetal)", "Medium (nonmetal)", "Shields (nonmetal)"),
            weaponProficiencies = listOf("Clubs", "Daggers", "Darts", "Javelins", "Maces", "Quarterstaffs", "Scimitars", "Sickles", "Slings", "Spears"),
            toolProficiencies = listOf("Herbalism Kit"),
            level1Features = listOf(
                Trait("Spellcasting", "You cast Druid spells using Wisdom. You prepare a number of spells from the full Druid spell list each day equal to your Wisdom modifier + your Druid level."),
                Trait("Wild Shape", "As a bonus action, transform into a beast you've seen before. Usable twice per short or long rest."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.ANIMAL_HANDLING, Skill.INSIGHT, Skill.MEDICINE, Skill.NATURE, Skill.PERCEPTION, Skill.RELIGION, Skill.SURVIVAL)),
                ClassChoice.FeatureOption("primal_order", "Primal Order", listOf(
                    FeatureChoiceOption("magician", "Magician", "You learn an extra cantrip from the Wizard list and add your Wisdom modifier to Arcana or Nature checks."),
                    FeatureChoiceOption("warden", "Warden", "You gain proficiency with martial weapons and medium armor."),
                )),
                cantrips("cantrips", "Cantrips Known", 2, listOf(
                    SpellStub("druid_druidcraft", "Druidcraft", 0, "Transmutation", "A small, harmless nature-themed effect: predict weather, sprout a seed, or clean a small object."),
                    SpellStub("druid_guidance", "Guidance", 0, "Divination", "Touch a creature to add a d4 to one ability check."),
                    SpellStub("druid_poison_spray", "Poison Spray", 0, "Conjuration", "Project a puff of noxious gas that deals poison damage."),
                    SpellStub("druid_produce_flame", "Produce Flame", 0, "Conjuration", "Conjure a flickering flame that lights the way or can be hurled at a foe."),
                    SpellStub("druid_shillelagh", "Shillelagh", 0, "Transmutation", "Imbue a club or quarterstaff with power, using Wisdom for its attacks."),
                    SpellStub("druid_thorn_whip", "Thorn Whip", 0, "Transmutation", "A vine-like whip drags a creature closer to you and deals damage."),
                )),
            ),
            isSpellcaster = true,
            spellcastingAbility = Ability.WIS,
            summary = "A guardian of the natural world, wielding primal magic and the ability to take animal form.",
        ),
        CharClass(
            id = "fighter",
            name = "Fighter",
            hitDie = 10,
            primaryAbility = listOf(Ability.STR, Ability.DEX),
            savingThrows = listOf(Ability.STR, Ability.CON),
            armorProficiencies = listOf("Light", "Medium", "Heavy", "Shields"),
            weaponProficiencies = listOf("Simple", "Martial"),
            toolProficiencies = emptyList(),
            level1Features = listOf(
                Trait("Second Wind", "As a bonus action, regain hit points equal to 1d10 + your Fighter level. Usable twice per long rest."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.ACROBATICS, Skill.ANIMAL_HANDLING, Skill.ATHLETICS, Skill.HISTORY, Skill.INSIGHT, Skill.INTIMIDATION, Skill.PERCEPTION, Skill.SURVIVAL)),
                ClassChoice.FeatureOption("fighting_style", "Fighting Style", listOf(
                    FeatureChoiceOption("archery", "Archery", "You gain a +2 bonus to attack rolls you make with ranged weapons."),
                    FeatureChoiceOption("defense", "Defense", "While wearing armor, you gain a +1 bonus to AC."),
                    FeatureChoiceOption("dueling", "Dueling", "While wielding a melee weapon in one hand and no other weapons, you gain +2 to damage rolls with it."),
                    FeatureChoiceOption("great_weapon", "Great Weapon Fighting", "When you roll a 1 or 2 on a damage die for a two-handed melee weapon, you can reroll it."),
                    FeatureChoiceOption("protection", "Protection", "When a creature you can see attacks a target other than you within 5 feet, you can impose disadvantage on the attack."),
                    FeatureChoiceOption("two_weapon", "Two-Weapon Fighting", "You can add your ability modifier to the damage of your off-hand attack."),
                )),
            ),
            isSpellcaster = false,
            summary = "A master of martial combat, skilled with a variety of weapons and armor.",
        ),
        CharClass(
            id = "monk",
            name = "Monk",
            hitDie = 8,
            primaryAbility = listOf(Ability.DEX, Ability.WIS),
            savingThrows = listOf(Ability.STR, Ability.DEX),
            armorProficiencies = emptyList(),
            weaponProficiencies = listOf("Simple", "Shortswords"),
            toolProficiencies = listOf("One artisan's tool or musical instrument of your choice"),
            level1Features = listOf(
                Trait("Martial Arts", "You can use Dexterity instead of Strength for unarmed strikes and monk weapons, and your unarmed strikes deal 1d6 damage."),
                Trait("Unarmored Defense", "While not wearing armor or a shield, your AC equals 10 + your Dexterity modifier + your Wisdom modifier."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.ACROBATICS, Skill.ATHLETICS, Skill.HISTORY, Skill.INSIGHT, Skill.RELIGION, Skill.STEALTH)),
            ),
            isSpellcaster = false,
            summary = "A disciplined martial artist who channels inner energy into supernatural feats of speed and strikes.",
        ),
        CharClass(
            id = "paladin",
            name = "Paladin",
            hitDie = 10,
            primaryAbility = listOf(Ability.STR, Ability.CHA),
            savingThrows = listOf(Ability.WIS, Ability.CHA),
            armorProficiencies = listOf("Light", "Medium", "Heavy", "Shields"),
            weaponProficiencies = listOf("Simple", "Martial"),
            toolProficiencies = emptyList(),
            level1Features = listOf(
                Trait("Lay on Hands", "You have a pool of healing power equal to 5 times your Paladin level, which you can touch a creature to restore as hit points, refreshing on a long rest."),
                Trait("Spellcasting (from level 2)", "Starting at 2nd level, you can cast spells using Charisma as your spellcasting ability."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.ATHLETICS, Skill.INSIGHT, Skill.INTIMIDATION, Skill.MEDICINE, Skill.PERSUASION, Skill.RELIGION)),
            ),
            isSpellcaster = false,
            spellcastingAbility = Ability.CHA,
            summary = "A holy warrior bound by a sacred oath, blending martial prowess with divine magic.",
        ),
        CharClass(
            id = "ranger",
            name = "Ranger",
            hitDie = 10,
            primaryAbility = listOf(Ability.DEX, Ability.WIS),
            savingThrows = listOf(Ability.STR, Ability.DEX),
            armorProficiencies = listOf("Light", "Medium", "Shields"),
            weaponProficiencies = listOf("Simple", "Martial"),
            toolProficiencies = emptyList(),
            level1Features = listOf(
                Trait("Favored Enemy", "You have advantage on Survival checks to track and Intelligence checks to recall information about creatures you hunt."),
                Trait("Spellcasting (from level 2)", "Starting at 2nd level, you can cast spells using Wisdom as your spellcasting ability."),
            ),
            choices = listOf(
                skillChoice(3, listOf(Skill.ANIMAL_HANDLING, Skill.ATHLETICS, Skill.INSIGHT, Skill.INVESTIGATION, Skill.NATURE, Skill.PERCEPTION, Skill.STEALTH, Skill.SURVIVAL)),
            ),
            isSpellcaster = false,
            spellcastingAbility = Ability.WIS,
            summary = "A skilled hunter and survivalist at home in the wilderness, blending martial skill with nature magic.",
        ),
        CharClass(
            id = "rogue",
            name = "Rogue",
            hitDie = 8,
            primaryAbility = listOf(Ability.DEX),
            savingThrows = listOf(Ability.DEX, Ability.INT),
            armorProficiencies = listOf("Light"),
            weaponProficiencies = listOf("Simple", "Hand Crossbows", "Longswords", "Rapiers", "Shortswords"),
            toolProficiencies = listOf("Thieves' Tools"),
            level1Features = listOf(
                Trait("Expertise", "Choose two of your skill proficiencies (or one skill and Thieves' Tools). Your proficiency bonus is doubled for checks using them."),
                Trait("Sneak Attack", "Once per turn, deal an extra 1d6 damage to a creature you hit with an attack if you have advantage or an ally is within 5 feet of the target."),
                Trait("Thieves' Cant", "You know a secret mix of dialect, jargon, and code that lets you hide messages in ordinary conversation."),
            ),
            choices = listOf(
                skillChoice(4, listOf(Skill.ACROBATICS, Skill.ATHLETICS, Skill.DECEPTION, Skill.INSIGHT, Skill.INTIMIDATION, Skill.INVESTIGATION, Skill.PERCEPTION, Skill.PERFORMANCE, Skill.PERSUASION, Skill.SLEIGHT_OF_HAND, Skill.STEALTH)),
            ),
            isSpellcaster = false,
            summary = "A cunning expert in stealth, precision strikes, and getting out of (or into) trouble.",
        ),
        CharClass(
            id = "sorcerer",
            name = "Sorcerer",
            hitDie = 6,
            primaryAbility = listOf(Ability.CHA),
            savingThrows = listOf(Ability.CON, Ability.CHA),
            armorProficiencies = emptyList(),
            weaponProficiencies = listOf("Daggers", "Darts", "Slings", "Quarterstaffs", "Light Crossbows"),
            toolProficiencies = emptyList(),
            level1Features = listOf(
                Trait("Spellcasting", "You cast Sorcerer spells using Charisma as your spellcasting ability."),
                Trait("Innate Sorcery", "As a bonus action, surge with power for a minute, gaining +1 to spell save DC and advantage on spell attack rolls. Usable twice per long rest."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.ARCANA, Skill.DECEPTION, Skill.INSIGHT, Skill.INTIMIDATION, Skill.PERSUASION, Skill.RELIGION)),
                cantrips("cantrips", "Cantrips Known", 4, listOf(
                    SpellStub("sorc_fire_bolt", "Fire Bolt", 0, "Evocation", "Hurl a mote of fire that deals fire damage on a hit."),
                    SpellStub("sorc_mage_hand", "Mage Hand", 0, "Conjuration", "Conjure a spectral hand to manipulate objects at range."),
                    SpellStub("sorc_minor_illusion", "Minor Illusion", 0, "Illusion", "Create a small sound or image illusion within range."),
                    SpellStub("sorc_prestidigitation", "Prestidigitation", 0, "Transmutation", "A minor magical trick: clean, chill, warm, flavor, or light a small object."),
                    SpellStub("sorc_ray_of_frost", "Ray of Frost", 0, "Evocation", "A ray of cold deals damage and reduces the target's speed."),
                    SpellStub("sorc_shocking_grasp", "Shocking Grasp", 0, "Evocation", "Melee spell attack that deals lightning damage and prevents reactions."),
                )),
                cantrips("spells1", "1st-Level Spells Known", 2, listOf(
                    SpellStub("sorc_magic_missile", "Magic Missile", 1, "Evocation", "Three darts of magical force automatically strike their targets."),
                    SpellStub("sorc_shield", "Shield", 1, "Abjuration", "An invisible barrier grants +5 AC until your next turn as a reaction."),
                    SpellStub("sorc_chromatic_orb", "Chromatic Orb", 1, "Evocation", "Hurl an orb of energy in a damage type of your choice."),
                    SpellStub("sorc_burning_hands", "Burning Hands", 1, "Evocation", "A thin sheet of flame sweeps out from your hands."),
                    SpellStub("sorc_feather_fall", "Feather Fall", 1, "Transmutation", "Slow the descent of falling creatures."),
                    SpellStub("sorc_mage_armor", "Mage Armor", 1, "Abjuration", "A protective magical field sets your base AC to 13 + Dexterity modifier."),
                )),
            ),
            isSpellcaster = true,
            spellcastingAbility = Ability.CHA,
            summary = "A wielder of magic drawn from an innate, often mysterious source deep within their blood.",
        ),
        CharClass(
            id = "warlock",
            name = "Warlock",
            hitDie = 8,
            primaryAbility = listOf(Ability.CHA),
            savingThrows = listOf(Ability.WIS, Ability.CHA),
            armorProficiencies = listOf("Light"),
            weaponProficiencies = listOf("Simple"),
            toolProficiencies = emptyList(),
            level1Features = listOf(
                Trait("Pact Magic", "You cast Warlock spells using Charisma. Your spell slots recharge on a short or long rest."),
                Trait("Eldritch Invocations (from level 2)", "Starting at 2nd level, you learn magical invocations that grant passive and active powers."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.ARCANA, Skill.DECEPTION, Skill.HISTORY, Skill.INTIMIDATION, Skill.INVESTIGATION, Skill.NATURE, Skill.RELIGION)),
                cantrips("cantrips", "Cantrips Known", 2, listOf(
                    SpellStub("wlk_eldritch_blast", "Eldritch Blast", 0, "Evocation", "A beam of crackling energy deals force damage to a target."),
                    SpellStub("wlk_chill_touch", "Chill Touch", 0, "Necromancy", "A ghostly skeletal hand deals necrotic damage and prevents healing."),
                    SpellStub("wlk_minor_illusion", "Minor Illusion", 0, "Illusion", "Create a small sound or image illusion within range."),
                    SpellStub("wlk_prestidigitation", "Prestidigitation", 0, "Transmutation", "A minor magical trick: clean, chill, warm, flavor, or light a small object."),
                    SpellStub("wlk_poison_spray", "Poison Spray", 0, "Conjuration", "Project a puff of noxious gas that deals poison damage."),
                    SpellStub("wlk_toll_dead", "Toll the Dead", 0, "Necromancy", "A dolorous bell tolls, dealing necrotic damage."),
                )),
                cantrips("spells1", "1st-Level Spells Known", 2, listOf(
                    SpellStub("wlk_hex", "Hex", 1, "Enchantment", "Curse a creature, dealing extra necrotic damage to it with your attacks."),
                    SpellStub("wlk_armor_agathys", "Armor of Agathys", 1, "Abjuration", "Frosty magic gives you temporary hit points and damages attackers who hit you in melee."),
                    SpellStub("wlk_charm_person", "Charm Person", 1, "Enchantment", "Attempt to charm a humanoid into regarding you as a friend."),
                    SpellStub("wlk_arms_hadar", "Arms of Hadar", 1, "Conjuration", "Tendrils of dark energy erupt from you, damaging nearby foes."),
                    SpellStub("wlk_witch_bolt", "Witch Bolt", 1, "Evocation", "A beam of lightning strikes a target and can be sustained for continued damage."),
                    SpellStub("wlk_comprehend", "Comprehend Languages", 1, "Divination", "Understand any spoken or written language for the duration."),
                )),
            ),
            isSpellcaster = true,
            spellcastingAbility = Ability.CHA,
            summary = "A spellcaster who draws magic from a bargain struck with an otherworldly patron.",
        ),
        CharClass(
            id = "wizard",
            name = "Wizard",
            hitDie = 6,
            primaryAbility = listOf(Ability.INT),
            savingThrows = listOf(Ability.INT, Ability.WIS),
            armorProficiencies = emptyList(),
            weaponProficiencies = listOf("Daggers", "Darts", "Slings", "Quarterstaffs", "Light Crossbows"),
            toolProficiencies = emptyList(),
            level1Features = listOf(
                Trait("Spellcasting", "You cast Wizard spells using Intelligence. You prepare a number of spells from your spellbook each day equal to your Intelligence modifier + your Wizard level."),
                Trait("Ritual Adept", "You can cast any spell in your spellbook that has the ritual tag as a ritual, without expending a spell slot."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.ARCANA, Skill.HISTORY, Skill.INSIGHT, Skill.INVESTIGATION, Skill.MEDICINE, Skill.RELIGION)),
                cantrips("cantrips", "Cantrips Known", 3, listOf(
                    SpellStub("wiz_fire_bolt", "Fire Bolt", 0, "Evocation", "Hurl a mote of fire that deals fire damage on a hit."),
                    SpellStub("wiz_mage_hand", "Mage Hand", 0, "Conjuration", "Conjure a spectral hand to manipulate objects at range."),
                    SpellStub("wiz_minor_illusion", "Minor Illusion", 0, "Illusion", "Create a small sound or image illusion within range."),
                    SpellStub("wiz_prestidigitation", "Prestidigitation", 0, "Transmutation", "A minor magical trick: clean, chill, warm, flavor, or light a small object."),
                    SpellStub("wiz_ray_of_frost", "Ray of Frost", 0, "Evocation", "A ray of cold deals damage and reduces the target's speed."),
                    SpellStub("wiz_shocking_grasp", "Shocking Grasp", 0, "Evocation", "Melee spell attack that deals lightning damage and prevents reactions."),
                )),
                cantrips("spellbook", "Spellbook (1st-Level Spells)", 6, listOf(
                    SpellStub("wiz_magic_missile", "Magic Missile", 1, "Evocation", "Three darts of magical force automatically strike their targets."),
                    SpellStub("wiz_shield", "Shield", 1, "Abjuration", "An invisible barrier grants +5 AC until your next turn as a reaction."),
                    SpellStub("wiz_detect_magic", "Detect Magic", 1, "Divination", "Sense the presence of magic within 30 feet."),
                    SpellStub("wiz_identify", "Identify", 1, "Divination", "Learn the properties of a magic item or effect."),
                    SpellStub("wiz_mage_armor", "Mage Armor", 1, "Abjuration", "A protective magical field sets your base AC to 13 + Dexterity modifier."),
                    SpellStub("wiz_sleep", "Sleep", 1, "Enchantment", "Weak-willed creatures nearby fall into a magical slumber."),
                    SpellStub("wiz_burning_hands", "Burning Hands", 1, "Evocation", "A thin sheet of flame sweeps out from your hands."),
                    SpellStub("wiz_comprehend", "Comprehend Languages", 1, "Divination", "Understand any spoken or written language for the duration."),
                    SpellStub("wiz_feather_fall", "Feather Fall", 1, "Transmutation", "Slow the descent of falling creatures."),
                    SpellStub("wiz_thunderwave", "Thunderwave", 1, "Evocation", "A wave of thunderous force pushes creatures away from you."),
                )),
            ),
            isSpellcaster = true,
            spellcastingAbility = Ability.INT,
            summary = "A scholarly spellcaster who masters magic through study, logic, and a well-kept spellbook.",
        ),
    )

    fun byId(id: String): CharClass? = ALL.find { it.id == id }
}
