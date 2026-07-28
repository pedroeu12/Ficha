package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.Feat

object FeatData {

    /** Feats granted by a background at character creation. */
    val ORIGIN_FEATS: List<Feat> = listOf(
        Feat("alert", "Alert", "You gain a bonus to Initiative equal to your Proficiency Bonus, and you can swap your Initiative with a willing ally's."),
        Feat("crafter", "Crafter", "You gain proficiency with three Artisan's Tools, get a 20% discount on nonmagical goods, and can craft items faster during a long rest."),
        Feat("healer", "Healer", "You can use a Healer's Kit as a Utilize action to restore hit points, and rerolling a 1 on any healing die is allowed."),
        Feat("lucky", "Lucky", "You have Luck Points equal to your Proficiency Bonus, which you can spend to gain Advantage on a d20 Test or impose Disadvantage on an attack against you."),
        Feat("magic_initiate_cleric", "Magic Initiate (Cleric)", "You learn two cantrips and one level 1 spell from the Cleric spell list. You can cast the level 1 spell once per long rest without a slot. Wisdom is your spellcasting ability for them."),
        Feat("magic_initiate_druid", "Magic Initiate (Druid)", "You learn two cantrips and one level 1 spell from the Druid spell list. You can cast the level 1 spell once per long rest without a slot. Wisdom is your spellcasting ability for them."),
        Feat("magic_initiate_wizard", "Magic Initiate (Wizard)", "You learn two cantrips and one level 1 spell from the Wizard spell list. You can cast the level 1 spell once per long rest without a slot. Intelligence is your spellcasting ability for them."),
        Feat("musician", "Musician", "You gain proficiency with three Musical Instruments, and after a rest you can grant Heroic Inspiration to allies equal to your Proficiency Bonus."),
        Feat("savage_attacker", "Savage Attacker", "Once per turn when you hit with a weapon, you can reroll the weapon's damage dice and use either total."),
        Feat("skilled", "Skilled", "You gain proficiency in any combination of three skills or tools of your choice."),
        Feat("tavern_brawler", "Tavern Brawler", "Your Unarmed Strike deals 1d4 damage, you can reroll a 1 on that die, and you can push a creature 5 feet once per turn."),
        Feat("tough", "Tough", "Your hit point maximum increases by twice your character level, and increases by 2 each time you gain a level."),
    )

    /** General feats, available in place of an Ability Score Improvement from level 4 on. */
    val GENERAL_FEATS: List<Feat> = listOf(
        Feat("ability_score_improvement", "Ability Score Improvement", "Increase one ability score by 2, or two ability scores by 1 each, to a maximum of 20."),
        Feat("actor", "Actor", "Increase Charisma by 1. You have Advantage on Deception and Performance checks to pass as someone else, and can mimic voices you've heard."),
        Feat("athlete", "Athlete", "Increase Strength or Dexterity by 1. Standing from Prone costs less movement, and you can climb at your normal Speed."),
        Feat("charger", "Charger", "Increase Strength or Dexterity by 1. After a Dash, you can make one attack with a bonus to damage or shove a creature 10 feet."),
        Feat("chef", "Chef", "Increase Constitution or Wisdom by 1. During a rest you can cook food that grants extra hit points or Temporary Hit Points."),
        Feat("crossbow_expert", "Crossbow Expert", "Increase Dexterity by 1. You ignore the Loading property and don't have Disadvantage from being within 5 feet of an enemy."),
        Feat("crusher", "Crusher", "Increase Strength or Constitution by 1. Once per turn when you deal Bludgeoning damage you can push the target 5 feet."),
        Feat("defensive_duelist", "Defensive Duelist", "Increase Dexterity by 1. As a Reaction while wielding a Finesse weapon, add your Proficiency Bonus to your AC against one attack."),
        Feat("dual_wielder", "Dual Wielder", "Increase Strength or Dexterity by 1. You can draw two weapons at once and make an extra attack with a non-Light weapon."),
        Feat("durable", "Durable", "Increase Constitution by 1. When you roll Hit Dice to regain hit points, the minimum result equals twice your Constitution modifier."),
        Feat("elemental_adept", "Elemental Adept", "Increase an ability by 1. Your spells of a chosen damage type ignore Resistance and treat 1s on damage dice as 2s."),
        Feat("fey_touched", "Fey-Touched", "Increase Intelligence, Wisdom, or Charisma by 1. You learn Misty Step and one level 1 Divination or Enchantment spell, castable free once per Long Rest."),
        Feat("grappler", "Grappler", "Increase Strength or Dexterity by 1. You have Advantage on attacks against creatures you have Grappled and can move them more easily."),
        Feat("great_weapon_master", "Great Weapon Master", "Increase Strength by 1. Heavy weapon Critical Hits let you attack again as a Bonus Action, and you can trade accuracy for damage."),
        Feat("heavily_armored", "Heavily Armored", "Increase Strength by 1 and gain training with Heavy armor."),
        Feat("heavy_armor_master", "Heavy Armor Master", "Increase Strength by 1. While in Heavy armor, reduce Bludgeoning, Piercing, and Slashing damage by your Proficiency Bonus."),
        Feat("inspiring_leader", "Inspiring Leader", "Increase Wisdom or Charisma by 1. After a rest you can grant allies Temporary Hit Points."),
        Feat("keen_mind", "Keen Mind", "Increase Intelligence by 1. You can take the Study action as a Bonus Action and always know which way is north."),
        Feat("lightly_armored", "Lightly Armored", "Increase Strength or Dexterity by 1 and gain training with Light armor."),
        Feat("mage_slayer", "Mage Slayer", "Increase Strength or Dexterity by 1. You have Advantage on saves against spells cast within 30 feet and can punish nearby casters."),
        Feat("martial_weapon_training", "Martial Weapon Training", "Increase Strength or Dexterity by 1 and gain proficiency with Martial weapons."),
        Feat("medium_armor_master", "Medium Armor Master", "Increase Strength or Dexterity by 1. Medium armor no longer imposes Stealth Disadvantage and allows a Dex bonus of up to 3."),
        Feat("moderately_armored", "Moderately Armored", "Increase Strength or Dexterity by 1 and gain training with Medium armor and Shields."),
        Feat("mounted_combatant", "Mounted Combatant", "Increase Strength, Dexterity, or Wisdom by 1. You gain Advantage on attacks from a mount and can shield it from damage."),
        Feat("observant", "Observant", "Increase Intelligence or Wisdom by 1. You can take the Search action as a Bonus Action and read lips."),
        Feat("piercer", "Piercer", "Increase Strength or Dexterity by 1. Once per turn reroll a Piercing damage die, and Critical Hits deal an extra die."),
        Feat("poisoner", "Poisoner", "Increase Dexterity or Intelligence by 1. Your poisons ignore Resistance and you can coat a weapon as a Bonus Action."),
        Feat("polearm_master", "Polearm Master", "Increase Strength or Dexterity by 1. You gain a Bonus Action butt-end attack and can strike creatures entering your reach."),
        Feat("resilient", "Resilient", "Increase one ability score by 1 and gain proficiency in saving throws with that ability."),
        Feat("ritual_caster", "Ritual Caster", "Increase Intelligence, Wisdom, or Charisma by 1. You gain a ritual book and can learn ritual spells from a chosen class list."),
        Feat("sentinel", "Sentinel", "Increase Strength or Dexterity by 1. Your Opportunity Attacks reduce a target's Speed to 0, and you can strike foes attacking your allies."),
        Feat("shadow_touched", "Shadow Touched", "Increase Intelligence, Wisdom, or Charisma by 1. You learn Invisibility and one level 1 Illusion or Necromancy spell, castable free once per Long Rest."),
        Feat("sharpshooter", "Sharpshooter", "Increase Dexterity by 1. Your ranged attacks ignore cover, long range doesn't impose Disadvantage, and you can trade accuracy for damage."),
        Feat("shield_master", "Shield Master", "Increase Strength by 1. You can shove with your shield as a Bonus Action and use it to shield yourself from area damage."),
        Feat("skill_expert", "Skill Expert", "Increase one ability score by 1, gain one skill proficiency, and gain Expertise in one skill."),
        Feat("skulker", "Skulker", "Increase Dexterity by 1. You can Hide when only Lightly Obscured, and missing with a ranged attack doesn't reveal you."),
        Feat("slasher", "Slasher", "Increase Strength or Dexterity by 1. Once per turn reduce a target's Speed with Slashing damage, and Critical Hits impose Disadvantage."),
        Feat("speedy", "Speedy", "Increase Dexterity or Constitution by 1. Your Speed increases by 10 feet and Dashing over Difficult Terrain costs no extra movement."),
        Feat("spell_sniper", "Spell Sniper", "Increase Intelligence, Wisdom, or Charisma by 1. Your attack-roll spells gain range and ignore cover, and you learn a cantrip."),
        Feat("telekinetic", "Telekinetic", "Increase Intelligence, Wisdom, or Charisma by 1. You learn Mage Hand and can shove a creature 5 feet as a Bonus Action."),
        Feat("telepathic", "Telepathic", "Increase Intelligence, Wisdom, or Charisma by 1. You can speak telepathically to creatures within 60 feet and cast Detect Thoughts once per Long Rest."),
        Feat("war_caster", "War Caster", "Increase Intelligence, Wisdom, or Charisma by 1. You gain Advantage on Concentration saves and can cast spells as Opportunity Attacks."),
        Feat("weapon_master", "Weapon Master", "Increase Strength or Dexterity by 1 and gain the mastery property of one additional kind of weapon."),
    )

    /** Epic Boon feats, taken at level 19. */
    val EPIC_BOONS: List<Feat> = listOf(
        Feat("boon_combat_prowess", "Boon of Combat Prowess", "Increase one ability score by 1. Once per turn, turn a miss into a hit."),
        Feat("boon_dimensional_travel", "Boon of Dimensional Travel", "Increase one ability score by 1. After taking the Attack or Magic action, you can teleport up to 30 feet."),
        Feat("boon_energy_resistance", "Boon of Energy Resistance", "Increase one ability score by 1. Gain Resistance to two damage types and the ability to ignore Resistance."),
        Feat("boon_fate", "Boon of Fate", "Increase one ability score by 1. Add 2d4 to another creature's d20 Test once per Long Rest."),
        Feat("boon_fortitude", "Boon of Fortitude", "Increase one ability score by 1. Your hit point maximum increases by 40, and healing yourself restores extra hit points."),
        Feat("boon_irresistible_offense", "Boon of Irresistible Offense", "Increase Strength or Dexterity by 1. Your attacks ignore Resistance, and a natural 20 deals extra damage."),
        Feat("boon_recovery", "Boon of Recovery", "Increase one ability score by 1. Heal for half your hit point maximum as a Bonus Action once per Long Rest."),
        Feat("boon_skill", "Boon of Skill", "Increase one ability score by 1. Gain proficiency in all skills and Expertise in one of your choice."),
        Feat("boon_speed", "Boon of Speed", "Increase one ability score by 1. Your Speed increases by 30 feet and you can Disengage as a Bonus Action."),
        Feat("boon_night_spirit", "Boon of the Night Spirit", "Increase one ability score by 1. In Dim Light or Darkness you become Invisible and deal extra Psychic damage."),
        Feat("boon_truesight", "Boon of Truesight", "Increase one ability score by 1. You gain Truesight out to 60 feet."),
        Feat("boon_undetectability", "Boon of Undetectability", "Increase one ability score by 1. Gain +10 to Stealth and immunity to divination magic."),
    )

    val ALL: List<Feat> = ORIGIN_FEATS + GENERAL_FEATS + EPIC_BOONS

    fun byId(id: String): Feat? = ALL.find { it.id == id }
}
