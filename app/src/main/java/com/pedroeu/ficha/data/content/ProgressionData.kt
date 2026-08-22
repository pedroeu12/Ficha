package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.CasterType
import com.pedroeu.ficha.data.model.ClassFeature
import com.pedroeu.ficha.data.model.ClassProgression
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.ChoiceOption
import com.pedroeu.ficha.data.model.ChoiceOptions
import com.pedroeu.ficha.data.model.Skill

/**
 * Level 1-20 tables for every class: features gained, ability score improvements, subclass
 * timing, cantrips known, and prepared spell counts. Spell slots come from
 * [com.pedroeu.ficha.data.model.SpellSlotTables] via the class's caster type.
 */
object ProgressionData {

    /** Ability Score Improvement levels shared by most classes. */
    private val STANDARD_ASI = setOf(4, 8, 12, 16)

    private fun feature(level: Int, name: String, description: String, vararg choices: Choice) =
        ClassFeature(level, name, description, choices.toList())

    // Full casters share this prepared-spell curve in the 2024 rules.
    private val FULL_CASTER_PREPARED = listOf(
        4, 5, 6, 7, 9, 10, 11, 12, 14, 15, 16, 16, 17, 17, 18, 18, 19, 20, 21, 22,
    )
    private val HALF_CASTER_PREPARED = listOf(
        2, 3, 4, 5, 6, 6, 7, 7, 9, 9, 10, 10, 11, 11, 12, 12, 14, 14, 15, 15,
    )
    private val WARLOCK_KNOWN = listOf(
        2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15,
    )

    /**
     * Arcane plans for the Artificer's Replicate Magic Item feature. The options come straight
     * from [MagicItemData], so a plan and the item a DM hands out are always the same entry.
     */
    private fun replicateChoice(count: Int, level: Int) = Choice(
        // One id across every tier, because each tier restates the whole set rather than
        // adding to it — reaching level 6 is a chance to give up a plan chosen at level 2,
        // not merely to learn a fifth. The level-up flow ticks the current plans for you.
        id = PLAN_CHOICE_ID,
        label = "Magic Item Plans",
        prompt = "Choose your $count plans. You can keep the ones you have or trade any of them now.",
        count = count,
        kind = ChoiceKind.OPTION,
        options = MagicItemData.artificerPlans(level).map { magicItem ->
            ChoiceOption(
                id = magicItem.id,
                name = magicItem.name,
                description = magicItem.description,
                supporting = magicItem.subtitle,
            )
        },
        source = "Level $level",
    )

    /** The single key every tier of the Artificer's plan choice is stored under. */
    const val PLAN_CHOICE_ID = "replicate_plans"

    /**
     * Every Eldritch Invocation on offer, including the two that came with the Primordial
     * Patron. Both of those name a damage type, which is asked separately once the invocation
     * is held — see [OriginChoices.forClass].
     */
    private val INVOCATION_OPTIONS = listOf(
        ChoiceOption("agonizing_blast", "Agonizing Blast", "Choose one of your known Warlock cantrips that deals damage. You add your Charisma modifier to that spell's damage against any target it hits. Prerequisite: a Warlock cantrip that deals damage."),
        ChoiceOption("armor_of_shadows", "Armor of Shadows", "You can cast Mage Armor on yourself without expending a spell slot or material components."),
        ChoiceOption("devils_sight", "Devil's Sight", "You can see normally in Dim Light and Darkness, both magical and nonmagical, within 120 feet of yourself."),
        ChoiceOption("eldritch_mind", "Eldritch Mind", "You have Advantage on Constitution saving throws that you make to maintain Concentration."),
        ChoiceOption("mask_of_many_faces", "Mask of Many Faces", "You can cast Disguise Self without expending a spell slot."),
        ChoiceOption("pact_blade", "Pact of the Blade", "As a Bonus Action, you can conjure a pact weapon in your hand — a Simple or Martial Melee weapon of your choice with which you bond. You have proficiency with it while you wield it, and it counts as Magical. You can use your Charisma modifier instead of Strength or Dexterity for its attack and damage rolls."),
        ChoiceOption("pact_chain", "Pact of the Chain", "You learn Find Familiar and can cast it as a Magic action without expending a spell slot. Your familiar can take the form of an Imp, Pseudodragon, Quasit, Skeleton, Slaad Tadpole, Sphinx of Wonder, Sprite, or Venomous Snake, and when you take the Attack action you can forgo one attack to let your familiar make one attack of its own."),
        ChoiceOption("pact_tome", "Pact of the Tome", "Choose three cantrips and one level 1 spell with the Ritual tag from any class's spell list. They are written in a Book of Shadows, and you can cast the spells in it as Rituals. The book is your Spellcasting Focus, and if it is lost you can perform a 1-hour ceremony to replace it."),
        ChoiceOption("elemental_overflow", "Elemental Overflow", "Choose a damage type: Acid, Cold, Fire, Lightning, or Thunder. Whenever you cast a spell that deals the chosen damage type, you can cause elemental energy to wreathe you until the end of your next turn. For the duration, whenever a creature within 5 feet of you hits you with a melee attack roll, that creature takes 1d4 damage of the chosen damage type. Repeatable: you can gain this invocation more than once, choosing a different damage type each time — hold one damage type per taking in the Elemental Overflow choice this raises. Prerequisite: Level 5+ Warlock."),
        ChoiceOption("elemental_transmutation", "Elemental Transmutation", "Choose a damage type: Acid, Cold, Fire, Lightning, or Thunder. Once per turn, whenever you deal damage of any of those types, you can deal the chosen damage type instead. Prerequisite: Level 2+ Warlock."),
    )

    /**
     * How many invocations a Warlock knows, from the 2024 Eldritch Invocations column.
     *
     * Capped by how many the app actually offers: promising ten and listing eight would leave
     * the level-up step unable to complete.
     */
    private fun invocationsKnownAt(level: Int): Int = when {
        level >= 18 -> 10
        level >= 15 -> 9
        level >= 12 -> 8
        level >= 9 -> 7
        level >= 7 -> 6
        level >= 5 -> 5
        level >= 2 -> 3
        else -> 1
    }.coerceAtMost(INVOCATION_OPTIONS.size)

    /**
     * The invocation question, asked again at each level the count grows.
     *
     * One id across every level, on the same footing as Weapon Mastery and the Artificer's
     * plans: each asking restates the whole set, the newest answer wins, and the level-up
     * flow arrives with the current invocations already ticked.
     */
    private fun invocationChoice(level: Int) = Choice(
        id = INVOCATION_CHOICE_ID,
        label = "Eldritch Invocations",
        prompt = "Choose your ${invocationsKnownAt(level)} Eldritch Invocation" +
            "${if (invocationsKnownAt(level) == 1) "" else "s"}. You can keep the ones you " +
            "have or trade any of them now.",
        count = invocationsKnownAt(level),
        kind = ChoiceKind.OPTION,
        options = INVOCATION_OPTIONS,
        source = "Level $level",
    )

    /** The single key every level of the Warlock's invocation choice is stored under. */
    const val INVOCATION_CHOICE_ID = "invocations"

    private val METAMAGIC_OPTIONS = listOf(
        ChoiceOption("careful", "Careful Spell", "When you cast a spell that forces other creatures to make a saving throw, you can protect some of them. You spend 1 Sorcery Point and choose a number of those creatures up to your Charisma modifier (minimum of one). A chosen creature automatically succeeds on its saving throw against the spell, and it takes no damage if it would normally take half damage on a successful save.", "1 Sorcery Point"),
        ChoiceOption("distant", "Distant Spell", "When you cast a spell that has a range of 5 feet or greater, you can spend 1 Sorcery Point to double the spell's range. Or when you cast a spell that has a range of Touch, you can spend 1 Sorcery Point to make its range 30 feet.", "1 Sorcery Point"),
        ChoiceOption("empowered", "Empowered Spell", "When you roll damage for a spell, you can spend 1 Sorcery Point to reroll a number of the damage dice up to your Charisma modifier (minimum of one), and you must use the new rolls. You can use this Metamagic even if you have already used another one during the casting of the spell.", "1 Sorcery Point"),
        ChoiceOption("extended", "Extended Spell", "When you cast a spell that has a duration of 1 minute or longer, you can spend 1 Sorcery Point to double its duration, to a maximum duration of 24 hours. If the spell requires Concentration, you have Advantage on any saving throw you make to maintain that Concentration.", "1 Sorcery Point"),
        ChoiceOption("heightened", "Heightened Spell", "When you cast a spell that forces a creature to make a saving throw, you can spend 2 Sorcery Points to give one target of the spell Disadvantage on saves against the spell.", "2 Sorcery Points"),
        ChoiceOption("quickened", "Quickened Spell", "When you cast a spell that has a casting time of an action, you can spend 2 Sorcery Points to change the casting time to a Bonus Action for this casting. You can't modify a spell in this way if you have already cast a level 1+ spell on the current turn, nor can you cast a level 1+ spell on this turn after modifying a spell in this way.", "2 Sorcery Points"),
        ChoiceOption("seeking", "Seeking Spell", "If you make an attack roll for a spell and miss, you can spend 1 Sorcery Point to reroll the d20, and you must use the new roll. You can use this Metamagic even if you have already used another one during the casting of the spell.", "1 Sorcery Point"),
        ChoiceOption("subtle", "Subtle Spell", "When you cast a spell, you can spend 1 Sorcery Point to cast it without any Verbal, Somatic, or Material components, except Material components that are consumed by the spell or that have a cost specified in the spell.", "1 Sorcery Point"),
        ChoiceOption("transmuted", "Transmuted Spell", "When you cast a spell that deals a type of damage from the following list, you can spend 1 Sorcery Point to change that damage type to one of the other listed types: Acid, Cold, Fire, Lightning, Poison, Thunder.", "1 Sorcery Point"),
        ChoiceOption("twinned", "Twinned Spell", "When you cast a spell such as Charm Person that can be cast with a higher-level spell slot to target an additional creature, you can spend a number of Sorcery Points equal to the spell's level to increase its effective level by 1. If the spell is a cantrip, it costs 1 Sorcery Point.", "Spell level in points"),
    )

    private fun metamagicChoice(id: String, count: Int, level: Int) = Choice(
        id = id,
        label = "Metamagic",
        prompt = "Choose $count Metamagic option${if (count == 1) "" else "s"}.",
        count = count,
        kind = ChoiceKind.OPTION,
        options = METAMAGIC_OPTIONS,
        source = "Level $level",
        resourceId = "sorcerer:sorcery_points",
    )

    private val FIGHTING_STYLE_OPTIONS = listOf(
        ChoiceOption("archery", "Archery", "You gain a +2 bonus to attack rolls you make with Ranged weapons."),
        ChoiceOption("blind_fighting", "Blind Fighting", "You have Blindsight with a range of 10 feet. Within that range, you can see anything that isn't behind Total Cover even if you have the Blinded condition or are in Darkness. Moreover, in that range you can see a creature that has the Invisible condition."),
        ChoiceOption("defense", "Defense", "While you're wearing Light, Medium, or Heavy armor, you gain a +1 bonus to Armor Class."),
        ChoiceOption("dueling", "Dueling", "When you're wielding a Melee weapon in one hand and no other weapons, you gain a +2 bonus to damage rolls with that weapon."),
        ChoiceOption("great_weapon", "Great Weapon Fighting", "When you hit with a weapon that has the Two-Handed property, you can treat any roll of 1 or 2 on a damage die as a 3. The weapon must be a Melee weapon to gain this benefit."),
        ChoiceOption("interception", "Interception", "When a creature you can see hits a target, other than you, within 5 feet of you with an attack, you can take a Reaction to reduce the damage the target takes by 1d10 plus your Proficiency Bonus. You must be wielding a Shield or a Simple or Martial weapon to use this Reaction."),
        ChoiceOption("protection", "Protection", "When a creature you can see attacks a target other than you that is within 5 feet of you, you can take a Reaction to interpose your Shield if you're holding one. You impose Disadvantage on the triggering attack roll."),
        ChoiceOption("thrown_weapon", "Thrown Weapon Fighting", "When you hit with a Ranged attack using a weapon that has the Thrown property, you gain a +2 bonus to the damage roll."),
        ChoiceOption("two_weapon", "Two-Weapon Fighting", "When you make an extra attack as a result of the Light property, you can add your ability modifier to the damage of that extra attack."),
        // D&D Beyond Drops, July 2026
        ChoiceOption("pack_fighting", "Pack Fighting", "When you make a melee attack with a weapon or an Unarmed Strike against a creature, you gain a +1 bonus to the damage roll if at least one of your allies is within 5 feet of the creature and that ally isn't Incapacitated."),
        ChoiceOption("prone_fighting", "Prone Fighting", "While you have the Prone condition, you don't have Disadvantage due to it on attack rolls, and it doesn't grant Advantage on attack rolls made against you."),
        ChoiceOption("unarmed", "Unarmed Fighting", "Your Unarmed Strikes can deal Bludgeoning damage equal to 1d6 plus your Strength modifier on a hit. If you're not wielding any weapons or a Shield when you make the attack roll, the d6 becomes a d8. At the start of each of your turns, you can deal 1d4 Bludgeoning damage to one creature you're Grappling."),
    )

    private fun fightingStyleChoice(level: Int, id: String = "fighting_style") = Choice(
        id = id,
        label = "Fighting Style",
        prompt = "Choose a Fighting Style feat.",
        count = 1,
        kind = ChoiceKind.OPTION,
        options = FIGHTING_STYLE_OPTIONS,
        source = "Level $level",
    )

    /**
     * Which weapons the character has mastery with. The list of options and the counts live
     * in [MasteryData], so a class's table and the property text stay in one place.
     */
    private fun masteryChoice(classId: String, count: Int, level: Int) =
        MasteryData.choiceFor(classId, count, level)

    /** The feature text for a level where the mastery count goes up. */
    private fun masteryGrowth(classId: String, level: Int, count: Int) = feature(
        level,
        "Weapon Mastery",
        "The number of weapons you have mastery with increases to $count.",
        masteryChoice(classId, count, level),
    )

    private fun expertiseChoice(level: Int, id: String, count: Int = 2) = Choice(
        id = id,
        label = "Expertise",
        prompt = "Choose $count skill proficiencies to double your proficiency bonus with.",
        count = count,
        kind = ChoiceKind.EXPERTISE,
        options = ChoiceOptions.fromSkills(Skill.ALL),
        source = "Level $level",
    )

    val ALL: List<ClassProgression> = listOf(

        // ------------------------------------------------------------------ Barbarian
        ClassProgression(
            classId = "barbarian",
            casterType = CasterType.NONE,
            subclassLevel = 3,
            subclassLabel = "Barbarian Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Rage", "Enter a Rage as a Bonus Action for bonus damage, Resistance to Bludgeoning, Piercing, and Slashing damage, and Advantage on Strength checks and saves."),
                feature(1, "Unarmored Defense", "While not wearing armor, your AC equals 10 + your Dexterity modifier + your Constitution modifier."),
                feature(1, "Weapon Mastery", "You can use the mastery property of two kinds of weapons you are proficient with. Whenever you finish a Long Rest, you can swap one of those weapons for a different one.",
                    masteryChoice("barbarian", 2, 1)),
                masteryGrowth("barbarian", 4, 3),
                masteryGrowth("barbarian", 10, 4),
                feature(2, "Danger Sense", "You have Advantage on Dexterity saving throws unless you have the Incapacitated condition."),
                feature(2, "Reckless Attack", "Attack recklessly to gain Advantage on Strength-based melee attacks, at the cost of Advantage for attacks against you."),
                feature(3, "Primal Knowledge", "You gain proficiency in another skill of your choice from the Barbarian list, and can make certain checks using Strength while raging.",
                    Choice("primal_knowledge", "Primal Knowledge", "Choose one more Barbarian skill proficiency.", 1, ChoiceKind.SKILL,
                        ChoiceOptions.fromSkills(listOf(Skill.ANIMAL_HANDLING, Skill.ATHLETICS, Skill.INTIMIDATION, Skill.NATURE, Skill.PERCEPTION, Skill.SURVIVAL)), "Level 3")),
                feature(5, "Extra Attack", "You can attack twice instead of once whenever you take the Attack action."),
                feature(5, "Fast Movement", "Your Speed increases by 10 feet while you aren't wearing Heavy armor."),
                feature(7, "Feral Instinct", "You have Advantage on Initiative rolls."),
                feature(7, "Instinctive Pounce", "When you enter your Rage, you can move up to half your Speed as part of that Bonus Action."),
                feature(9, "Brutal Strike", "When you use Reckless Attack, you can forgo Advantage to deal an extra 1d10 damage and apply a Forceful or Hamstring effect."),
                feature(11, "Relentless Rage", "When you drop to 0 hit points while raging, you can make a DC 10 Constitution save to drop to 1 hit point instead."),
                feature(13, "Improved Brutal Strike", "You gain two more Brutal Strike options: Staggering Blow and Sundering Blow."),
                feature(15, "Persistent Rage", "When you roll Initiative, your Rage ends early only if you choose to end it, and it lasts up to 10 minutes."),
                feature(17, "Improved Brutal Strike", "Your Brutal Strike damage increases to 2d10, and you gain the Disorienting Blow and Staggering Blow options."),
                feature(18, "Indomitable Might", "If your total for a Strength check is less than your Strength score, use the score instead."),
                feature(20, "Primal Champion", "Your Strength and Constitution scores increase by 4, to a maximum of 25."),
            ),
        ),

        // ------------------------------------------------------------------ Bard
        ClassProgression(
            classId = "bard",
            casterType = CasterType.FULL,
            subclassLevel = 3,
            subclassLabel = "Bard Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Bardic Inspiration", "As a Bonus Action, give a creature a Bardic Inspiration die (d6) to add to a d20 Test or damage roll."),
                feature(1, "Spellcasting", "You cast Bard spells using Charisma, preparing them from the Bard spell list."),
                feature(2, "Expertise", "Choose two skill proficiencies to double your proficiency bonus with.",
                    expertiseChoice(2, "bard_expertise_2")),
                feature(2, "Jack of All Trades", "Add half your Proficiency Bonus to any ability check that doesn't already use it."),
                feature(5, "Font of Inspiration", "You regain all expended Bardic Inspiration uses on a Short or Long Rest, and can spend one to fuel certain features."),
                feature(7, "Countercharm", "As a Reaction when you or a nearby ally fails a save against being Charmed or Frightened, you can let them reroll."),
                feature(9, "Expertise", "Choose two more skill proficiencies to double your proficiency bonus with.",
                    expertiseChoice(9, "bard_expertise_9")),
                feature(10, "Magical Secrets", "When you gain Bard levels, you can choose your prepared spells from the Bard, Cleric, Druid, and Wizard lists."),
                feature(18, "Superior Inspiration", "When you roll Initiative, you regain expended uses of Bardic Inspiration until you have two."),
                feature(20, "Words of Creation", "You always have Power Word Heal and Power Word Kill prepared, and can target a second creature with them."),
            ),
            cantripsKnown = mapOf(1 to 2, 4 to 3, 10 to 4),
            preparedSpells = FULL_CASTER_PREPARED,
        ),

        // ------------------------------------------------------------------ Cleric
        ClassProgression(
            classId = "cleric",
            casterType = CasterType.FULL,
            subclassLevel = 3,
            subclassLabel = "Cleric Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Spellcasting", "You cast Cleric spells using Wisdom, preparing them from the whole Cleric spell list."),
                feature(1, "Divine Order", "Choose the role your divine calling takes.",
                    Choice("divine_order", "Divine Order", "Choose your Divine Order.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("protector", "Protector", "Gain training with Martial weapons and Heavy armor."),
                        ChoiceOption("thaumaturge", "Thaumaturge", "Learn an extra Cleric cantrip and add your Wisdom modifier to Arcana and Religion checks."),
                    ), "Level 1")),
                feature(2, "Channel Divinity", "Channel divine energy to fuel Divine Spark or Turn Undead, regaining uses on a Short or Long Rest."),
                feature(5, "Sear Undead", "When you use Turn Undead, you also deal Radiant damage equal to a roll of your Wisdom modifier in d8s."),
                feature(7, "Blessed Strikes", "Choose Divine Strike or Potent Spellcasting to add damage to your attacks or cantrips.",
                    Choice("blessed_strikes", "Blessed Strikes", "Choose how your divine power sharpens your attacks.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("divine_strike", "Divine Strike", "Once per turn, a weapon hit deals an extra 1d8 Necrotic or Radiant damage."),
                        ChoiceOption("potent_spellcasting", "Potent Spellcasting", "Add your Wisdom modifier to the damage of your Cleric cantrips."),
                    ), "Level 7")),
                feature(10, "Divine Intervention", "As a Magic action, call on your deity to cast any Cleric spell of level 5 or lower without components."),
                feature(14, "Improved Blessed Strikes", "Your Blessed Strikes option grows stronger, dealing an extra die or granting Temporary Hit Points."),
                feature(20, "Greater Divine Intervention", "You can call on Wish once when you use Divine Intervention."),
            ),
            cantripsKnown = mapOf(1 to 3, 4 to 4, 10 to 5),
            preparedSpells = FULL_CASTER_PREPARED,
        ),

        // ------------------------------------------------------------------ Druid
        ClassProgression(
            classId = "druid",
            casterType = CasterType.FULL,
            subclassLevel = 3,
            subclassLabel = "Druid Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Spellcasting", "You cast Druid spells using Wisdom, preparing them from the whole Druid spell list."),
                feature(1, "Druidic", "You know Druidic, the secret language of Druids, and can use it to leave hidden messages."),
                feature(1, "Primal Order", "Choose how you channel the natural world.",
                    Choice("primal_order", "Primal Order", "Choose your Primal Order.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("magician", "Magician", "Learn an extra Druid cantrip and add your Wisdom modifier to Arcana and Nature checks."),
                        ChoiceOption("warden", "Warden", "Gain training with Martial weapons and Medium armor."),
                    ), "Level 1")),
                feature(2, "Wild Shape", "As a Bonus Action, transform into a Beast you know, twice per Short or Long Rest."),
                feature(2, "Wild Companion", "Expend a Wild Shape use to cast Find Familiar without material components, summoning a Fey spirit."),
                feature(5, "Wild Resurgence", "Once per turn you can convert a spell slot into a Wild Shape use, or a Wild Shape use into a level 1 spell slot."),
                feature(7, "Elemental Fury", "Choose Potent Spellcasting or Primal Strike to sharpen your magic or your attacks.",
                    Choice("elemental_fury", "Elemental Fury", "Choose how your primal power expresses itself.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("potent_spellcasting", "Potent Spellcasting", "Add your Wisdom modifier to the damage of your Druid cantrips."),
                        ChoiceOption("primal_strike", "Primal Strike", "Once per turn, your attacks deal an extra 1d8 elemental damage."),
                    ), "Level 7")),
                feature(15, "Improved Elemental Fury", "Your Elemental Fury option improves, extending cantrip range or increasing the extra damage to 2d8."),
                feature(18, "Beast Spells", "You can cast Druid spells in any Wild Shape form."),
                feature(20, "Archdruid", "Your Wild Shape uses are effectively unlimited, and you can convert Wild Shape uses into spell slots."),
            ),
            cantripsKnown = mapOf(1 to 2, 4 to 3, 10 to 4),
            preparedSpells = FULL_CASTER_PREPARED,
        ),

        // ------------------------------------------------------------------ Fighter
        ClassProgression(
            classId = "fighter",
            casterType = CasterType.NONE,
            subclassLevel = 3,
            subclassLabel = "Fighter Subclass",
            asiLevels = setOf(4, 6, 8, 12, 14, 16),
            features = listOf(
                feature(1, "Fighting Style", "You gain a Fighting Style feat of your choice.",
                    fightingStyleChoice(1)),
                feature(1, "Second Wind", "As a Bonus Action, regain 1d10 + your Fighter level hit points, twice per Short or Long Rest."),
                feature(1, "Weapon Mastery", "You can use the mastery property of three kinds of weapons you are proficient with. Whenever you finish a Long Rest, you can swap one of those weapons for a different one.",
                    masteryChoice("fighter", 3, 1)),
                masteryGrowth("fighter", 4, 4),
                masteryGrowth("fighter", 10, 5),
                masteryGrowth("fighter", 16, 6),
                feature(2, "Action Surge", "Take one additional action on your turn, once per Short or Long Rest."),
                feature(2, "Tactical Mind", "When you fail an ability check, spend a use of Second Wind to add 1d10 to the roll."),
                feature(5, "Extra Attack", "You can attack twice instead of once whenever you take the Attack action."),
                feature(5, "Tactical Shift", "When you activate Second Wind, you can move up to half your Speed without provoking Opportunity Attacks."),
                feature(9, "Indomitable", "Reroll a failed saving throw with a bonus equal to your Fighter level, once per Long Rest."),
                feature(9, "Tactical Master", "When you attack with a weapon whose mastery you can use, you can replace it with Push, Sap, or Slow."),
                feature(11, "Two Extra Attacks", "You can attack three times whenever you take the Attack action."),
                feature(13, "Indomitable (two uses)", "You can use Indomitable twice per Long Rest."),
                feature(13, "Studied Attacks", "When you miss a creature with an attack, you have Advantage on your next attack against it."),
                feature(17, "Action Surge (two uses)", "You can use Action Surge twice per rest, but only once per turn."),
                feature(17, "Indomitable (three uses)", "You can use Indomitable three times per Long Rest."),
                feature(20, "Three Extra Attacks", "You can attack four times whenever you take the Attack action."),
            ),
        ),

        // ------------------------------------------------------------------ Monk
        ClassProgression(
            classId = "monk",
            casterType = CasterType.NONE,
            subclassLevel = 3,
            subclassLabel = "Monk Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Martial Arts", "Your Unarmed Strikes and Monk weapons use Dexterity and deal 1d6 damage, and you can make an extra Unarmed Strike as a Bonus Action."),
                feature(1, "Unarmored Defense", "While wearing no armor or shield, your AC equals 10 + your Dexterity modifier + your Wisdom modifier."),
                feature(2, "Monk's Focus", "You gain Focus Points to fuel Flurry of Blows, Patient Defense, and Step of the Wind."),
                feature(2, "Unarmored Movement", "Your Speed increases by 10 feet while you wear no armor or shield."),
                feature(2, "Uncanny Metabolism", "When you roll Initiative, regain all Focus Points and hit points equal to a Martial Arts die roll plus your Monk level."),
                feature(3, "Deflect Attacks", "As a Reaction, reduce damage from an attack by 1d10 + your Dexterity modifier + your Monk level."),
                feature(4, "Slow Fall", "As a Reaction when you fall, reduce the falling damage by five times your Monk level."),
                feature(5, "Extra Attack", "You can attack twice instead of once whenever you take the Attack action."),
                feature(5, "Stunning Strike", "Once per turn, spend a Focus Point to force a Constitution save or leave the target Stunned."),
                feature(6, "Empowered Strikes", "Your Unarmed Strikes can deal Force damage instead of their normal type."),
                feature(7, "Evasion", "On a Dexterity save for half damage, you take none on a success and half on a failure."),
                feature(9, "Acrobatic Movement", "While unarmored, you gain the ability to move along vertical surfaces and across liquids."),
                feature(10, "Heightened Focus", "Flurry of Blows, Patient Defense, and Step of the Wind each gain an improved effect."),
                feature(10, "Self-Restoration", "At the end of each turn you can end one condition on yourself, and you no longer suffer Exhaustion from lack of food or water."),
                feature(13, "Deflect Energy", "Deflect Attacks now works against any damage type."),
                feature(14, "Disciplined Survivor", "You gain proficiency in all saving throws, and can spend a Focus Point to reroll a failed save."),
                feature(15, "Perfect Focus", "When you roll Initiative with fewer than 4 Focus Points, you regain enough to have 4."),
                feature(18, "Superior Defense", "At the start of your turn, spend 3 Focus Points for Resistance to all damage except Force for 1 minute."),
                feature(20, "Body and Mind", "Your Dexterity and Wisdom scores increase by 4, to a maximum of 25."),
            ),
        ),

        // ------------------------------------------------------------------ Paladin
        ClassProgression(
            classId = "paladin",
            casterType = CasterType.HALF,
            subclassLevel = 3,
            subclassLabel = "Paladin Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Lay On Hands", "You have a pool of healing equal to five times your Paladin level that you can spend as a Bonus Action."),
                feature(1, "Spellcasting", "You cast Paladin spells using Charisma, preparing them from the Paladin spell list."),
                feature(1, "Weapon Mastery", "You can use the mastery property of two kinds of weapons you are proficient with. Whenever you finish a Long Rest, you can swap one of those weapons for a different one.",
                    masteryChoice("paladin", 2, 1)),
                masteryGrowth("paladin", 9, 3),
                feature(2, "Fighting Style", "You gain a Fighting Style feat of your choice.",
                    fightingStyleChoice(2)),
                feature(2, "Paladin's Smite", "You always have Divine Smite prepared, and can cast it once per Long Rest without a slot."),
                feature(3, "Channel Divinity", "You can channel divine energy to fuel Divine Sense and your subclass's Channel Divinity options."),
                feature(5, "Extra Attack", "You can attack twice instead of once whenever you take the Attack action."),
                feature(5, "Faithful Steed", "You always have Find Steed prepared and can cast it once per Long Rest without a slot."),
                feature(6, "Aura of Protection", "You and allies within 10 feet add your Charisma modifier to saving throws."),
                feature(9, "Abjure Foes", "As a Magic action, spend a Channel Divinity use to leave nearby foes Frightened and unable to act freely."),
                feature(10, "Aura of Courage", "You and allies within 10 feet are immune to the Frightened condition."),
                feature(11, "Radiant Strikes", "Your attacks deal an extra 1d8 Radiant damage."),
                feature(14, "Restoring Touch", "When you use Lay On Hands, you can also end a condition affecting the creature."),
                feature(18, "Aura Expansion", "Your auras extend to 30 feet."),
            ),
            cantripsKnown = emptyMap(),
            preparedSpells = HALF_CASTER_PREPARED,
        ),

        // ------------------------------------------------------------------ Ranger
        ClassProgression(
            classId = "ranger",
            casterType = CasterType.HALF,
            subclassLevel = 3,
            subclassLabel = "Ranger Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Spellcasting", "You cast Ranger spells using Wisdom, preparing them from the Ranger spell list."),
                feature(1, "Favored Enemy", "You always have Hunter's Mark prepared and can cast it a number of times per Long Rest without a slot."),
                feature(1, "Weapon Mastery", "You can use the mastery property of two kinds of weapons you are proficient with. Whenever you finish a Long Rest, you can swap one of those weapons for a different one.",
                    masteryChoice("ranger", 2, 1)),
                masteryGrowth("ranger", 9, 3),
                feature(2, "Deft Explorer", "You gain Expertise in one skill you are proficient with and learn two languages.",
                    expertiseChoice(2, "ranger_expertise_2", count = 1)),
                feature(2, "Fighting Style", "You gain a Fighting Style feat of your choice.",
                    fightingStyleChoice(2)),
                feature(5, "Extra Attack", "You can attack twice instead of once whenever you take the Attack action."),
                feature(6, "Roving", "Your Speed increases by 10 feet and you gain a Climb Speed and Swim Speed equal to your Speed."),
                feature(9, "Expertise", "Choose two more skill proficiencies to double your proficiency bonus with.",
                    expertiseChoice(9, "ranger_expertise_9")),
                feature(10, "Tireless", "As a Magic action, give yourself Temporary Hit Points, and your Exhaustion decreases on a Short Rest."),
                feature(13, "Relentless Hunter", "Taking damage can't break your Concentration on Hunter's Mark."),
                feature(14, "Nature's Veil", "As a Bonus Action, become Invisible until the end of your next turn."),
                feature(17, "Precise Hunter", "You have Advantage on attack rolls against the creature marked by your Hunter's Mark."),
                feature(18, "Feral Senses", "You gain Blindsight with a range of 30 feet."),
                feature(20, "Foe Slayer", "Your Hunter's Mark damage die becomes a d10."),
            ),
            preparedSpells = HALF_CASTER_PREPARED,
        ),

        // ------------------------------------------------------------------ Rogue
        ClassProgression(
            classId = "rogue",
            casterType = CasterType.NONE,
            subclassLevel = 3,
            subclassLabel = "Rogue Subclass",
            asiLevels = setOf(4, 8, 10, 12, 16),
            features = listOf(
                feature(1, "Expertise", "Choose two skill proficiencies to double your proficiency bonus with.",
                    expertiseChoice(1, "rogue_expertise_1")),
                feature(1, "Sneak Attack", "Once per turn, deal an extra 1d6 damage to a target you have Advantage against or that is next to an ally."),
                feature(1, "Thieves' Cant", "You know a secret mix of dialect, jargon, and code that hides messages in ordinary conversation."),
                feature(1, "Weapon Mastery", "You can use the mastery property of two kinds of weapons you are proficient with. Whenever you finish a Long Rest, you can swap one of those weapons for a different one.",
                    masteryChoice("rogue", 2, 1)),
                masteryGrowth("rogue", 9, 3),
                feature(2, "Cunning Action", "You can take the Dash, Disengage, or Hide action as a Bonus Action."),
                feature(3, "Steady Aim", "As a Bonus Action, give yourself Advantage on your next attack this turn if you haven't moved."),
                feature(5, "Cunning Strike", "When you deal Sneak Attack damage, trade dice for effects such as Poison, Trip, or Withdraw."),
                feature(5, "Uncanny Dodge", "As a Reaction, halve the damage of an attack that hits you."),
                feature(6, "Expertise", "Choose two more skill proficiencies to double your proficiency bonus with.",
                    expertiseChoice(6, "rogue_expertise_6")),
                feature(7, "Evasion", "On a Dexterity save for half damage, you take none on a success and half on a failure."),
                feature(7, "Reliable Talent", "Treat a d20 roll of 9 or lower as a 10 for ability checks using your proficiencies."),
                feature(11, "Improved Cunning Strike", "You can use up to two Cunning Strike effects at once."),
                feature(14, "Devious Strikes", "You gain the Daze, Knock Out, and Obscure Cunning Strike options."),
                feature(15, "Slippery Mind", "You gain proficiency in Wisdom and Charisma saving throws."),
                feature(18, "Elusive", "No attack roll has Advantage against you unless you have the Incapacitated condition."),
                feature(20, "Stroke of Luck", "Once per Short or Long Rest, turn a miss into a hit or a failed check into a 20."),
            ),
        ),

        // ------------------------------------------------------------------ Sorcerer
        ClassProgression(
            classId = "sorcerer",
            casterType = CasterType.FULL,
            subclassLevel = 3,
            subclassLabel = "Sorcerer Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Spellcasting", "You cast Sorcerer spells using Charisma, preparing them from the Sorcerer spell list."),
                feature(1, "Innate Sorcery", "As a Bonus Action, gain +1 to your spell save DC and Advantage on Sorcerer spell attacks for 1 minute."),
                feature(2, "Font of Magic", "You gain Sorcery Points and can convert them into spell slots, or slots into points."),
                feature(2, "Metamagic", "Choose two Metamagic options to bend your spells.",
                    metamagicChoice("metamagic_2", 2, 2)),
                feature(5, "Sorcerous Restoration", "When you finish a Short Rest, regain Sorcery Points equal to half your Sorcerer level."),
                feature(7, "Sorcery Incarnate", "While Innate Sorcery is active, you can use two Metamagic options on a single spell."),
                feature(10, "Metamagic", "Choose two more Metamagic options.",
                    metamagicChoice("metamagic_10", 2, 10)),
                feature(17, "Metamagic", "Choose two more Metamagic options.",
                    metamagicChoice("metamagic_17", 2, 17)),
                feature(20, "Arcane Apotheosis", "While Innate Sorcery is active, one Metamagic option each turn costs no Sorcery Points."),
            ),
            cantripsKnown = mapOf(1 to 4, 4 to 5, 10 to 6),
            preparedSpells = listOf(
                2, 4, 6, 7, 9, 10, 11, 12, 14, 15, 16, 16, 17, 17, 18, 18, 19, 20, 21, 22,
            ),
        ),

        // ------------------------------------------------------------------ Warlock
        ClassProgression(
            classId = "warlock",
            casterType = CasterType.PACT,
            subclassLevel = 3,
            subclassLabel = "Warlock Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Pact Magic", "You cast Warlock spells using Charisma. Your slots are always at the highest level you can cast and return on a Short Rest."),
                feature(1, "Eldritch Invocations", "Choose Eldritch Invocations that grant passive and active magical powers. The number you know grows as you gain Warlock levels, and each time it does you choose your whole set again — so an invocation that has stopped earning its place can be given up.",
                    invocationChoice(1)),
                feature(2, "Eldritch Invocations", "You now know three Eldritch Invocations.", invocationChoice(2)),
                feature(5, "Eldritch Invocations", "You now know five Eldritch Invocations.", invocationChoice(5)),
                feature(7, "Eldritch Invocations", "You now know six Eldritch Invocations.", invocationChoice(7)),
                feature(9, "Eldritch Invocations", "You now know seven Eldritch Invocations.", invocationChoice(9)),
                feature(12, "Eldritch Invocations", "You now know eight Eldritch Invocations.", invocationChoice(12)),
                feature(15, "Eldritch Invocations", "You now know nine Eldritch Invocations.", invocationChoice(15)),
                feature(18, "Eldritch Invocations", "You now know ten Eldritch Invocations.", invocationChoice(18)),
                feature(2, "Magical Cunning", "Once per Long Rest, spend 1 minute to regain expended Pact Magic spell slots."),
                feature(9, "Contact Patron", "You always have Contact Other Plane prepared and can cast it once per Long Rest to reach your patron."),
                feature(11, "Mystic Arcanum (Level 6)", "Choose a level 6 spell you can cast once per Long Rest without a slot."),
                feature(13, "Mystic Arcanum (Level 7)", "Choose a level 7 spell you can cast once per Long Rest without a slot."),
                feature(15, "Mystic Arcanum (Level 8)", "Choose a level 8 spell you can cast once per Long Rest without a slot."),
                feature(17, "Mystic Arcanum (Level 9)", "Choose a level 9 spell you can cast once per Long Rest without a slot."),
                feature(20, "Eldritch Master", "You can use Magical Cunning twice per Long Rest."),
            ),
            cantripsKnown = mapOf(1 to 2, 4 to 3, 10 to 4),
            preparedSpells = WARLOCK_KNOWN,
        ),

        // ------------------------------------------------------------------ Wizard
        ClassProgression(
            classId = "wizard",
            casterType = CasterType.FULL,
            subclassLevel = 3,
            subclassLabel = "Wizard Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Spellcasting", "You cast Wizard spells using Intelligence, preparing them from your spellbook."),
                feature(1, "Ritual Adept", "You can cast any ritual spell in your spellbook without expending a spell slot."),
                feature(1, "Arcane Recovery", "Once per day on a Short Rest, recover expended spell slots totalling half your Wizard level."),
                feature(2, "Scholar", "You gain Expertise in one Arcana, History, Investigation, Medicine, Nature, or Religion proficiency.",
                    Choice("scholar", "Scholar", "Choose one skill to gain Expertise in.", 1, ChoiceKind.EXPERTISE,
                        ChoiceOptions.fromSkills(listOf(Skill.ARCANA, Skill.HISTORY, Skill.INVESTIGATION, Skill.MEDICINE, Skill.NATURE, Skill.RELIGION)), "Level 2")),
                feature(5, "Memorize Spell", "On a Short Rest, swap one prepared Wizard spell for another from your spellbook."),
                feature(18, "Spell Mastery", "Choose a level 1 and a level 2 spell in your spellbook that you can cast at will."),
                feature(20, "Signature Spells", "Choose two level 3 spells that are always prepared and castable once each per Short Rest without a slot."),
            ),
            cantripsKnown = mapOf(1 to 3, 4 to 4, 10 to 5),
            preparedSpells = FULL_CASTER_PREPARED,
        ),
        ClassProgression(
            classId = "artificer",
            casterType = CasterType.ARTIFICER,
            subclassLevel = 3,
            subclassLabel = "Artificer Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Spellcasting", "You cast Artificer spells through Thieves' Tools, Tinker's Tools, or Artisan's Tools used as a Spellcasting Focus. Intelligence is your spellcasting ability, and you can change your prepared spells whenever you finish a Long Rest."),
                feature(1, "Tinker's Magic", "You know the Mending cantrip, and as a Magic action while holding Tinker's Tools you can conjure a mundane item within 5 feet of yourself that lasts until your next Long Rest."),
                feature(2, "Replicate Magic Item", "You learn arcane plans and can create magic items from them when you finish a Long Rest, so long as you have Tinker's Tools in hand. An item created this way vanishes 1d4 days after you die, and any Wand or Weapon you create can serve as a Spellcasting Focus.",
                    replicateChoice(4, 2)),
                feature(6, "Magic Item Tinker", "Your Replicate Magic Item feature gains two options: Charge Magic Item, which spends a level 1+ spell slot as a Bonus Action to recharge an item you made, and Drain Magic Item, which destroys one of your items to recover a spell slot once per Long Rest.",
                    replicateChoice(5, 6)),
                feature(7, "Flash of Genius", "When you or a creature you can see within 30 feet of you fails an ability check or a saving throw, you can take a Reaction to add your Intelligence modifier (minimum of +1) to the roll, potentially causing it to succeed. You can do this a number of times equal to your Intelligence modifier (minimum of once) per Long Rest."),
                feature(10, "Magic Item Adept", "You can now attune to up to four magic items at once.",
                    replicateChoice(6, 10)),
                feature(11, "Spell-Storing Item", "Whenever you finish a Long Rest, you can store a level 1, 2, or 3 Artificer spell with a casting time of an action in a weapon or Spellcasting Focus you touch. Any creature holding the object can take a Magic action to produce the spell's effect, using your spellcasting ability modifier. The spell lasts until used twice your Intelligence modifier times (minimum of twice) or until you store another."),
                feature(14, "Advanced Artifice", "Magic Item Savant lets you attune to up to five magic items at once, and Refreshed Genius returns one expended use of Flash of Genius whenever you finish a Short Rest.",
                    replicateChoice(7, 14)),
                feature(18, "Magic Item Master", "You can now attune to up to six magic items at once.",
                    replicateChoice(8, 18)),
                feature(20, "Soul of Artifice", "Cheat Death lets you disintegrate any number of Uncommon or Rare items you created when reduced to 0 Hit Points but not killed outright, setting your Hit Points to 20 times the number destroyed. Magical Guidance returns all expended uses of Flash of Genius on a Short Rest, provided you are attuned to at least one magic item."),
            ),
            cantripsKnown = mapOf(1 to 2, 10 to 3, 14 to 4),
            // The Artificer's prepared-spell curve is the same as the other half casters'.
            preparedSpells = HALF_CASTER_PREPARED,
        ),
    )

    private val byIdMap: Map<String, ClassProgression> = ALL.associateBy { it.classId }

    fun forClass(classId: String): ClassProgression? = byIdMap[classId]
}
