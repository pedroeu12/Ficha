package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.CasterType
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.ChoiceOption
import com.pedroeu.ficha.data.model.ChoiceOptions
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.data.model.Sourcebook
import com.pedroeu.ficha.data.model.Subclass
import com.pedroeu.ficha.data.model.SubclassFeature

/**
 * Subclasses for every class, plus the six playtest subclasses from Unearthed Arcana 2025:
 * Arcane Archer, Tattooed Warrior, Conjurer, Enchanter, Necromancer, and Transmuter.
 */
object SubclassData {

    private val UA = Sourcebook.UA_ARCANE
    private val EFOTA = Sourcebook.EBERRON
    private val HOF = Sourcebook.HEROES_OF_FAERUN

    /**
     * The three subclasses revised in the Villainous Options Update carry the later text; the
     * Pestilence Domain was not revisited, so it keeps its original wording.
     */
    private val VILLAIN = Sourcebook.UA_VILLAINOUS
    private val HORROR = Sourcebook.UA_HORROR
    private val UA_SUB = Sourcebook.UA_SUBCLASSES

    // All three Villainous printings are one book to the selector, but the wording differs
    // between them, so each subclass still names the printing it carries.
    private const val P_VILLAIN = "Unearthed Arcana 2026: Villainous Options"
    private const val P_VILLAIN_UPDATE = "Unearthed Arcana 2026: Villainous Options Update"
    private const val P_VILLAIN_2 = "Unearthed Arcana 2026: Villainous Options 2"

    /** The choice id the Primordial Patron's element is stored under, at every level. */
    const val ELEMENT_CHOICE_ID = "primordial_element"

    /**
     * The Primordial Patron's element, and the damage type that follows from it.
     *
     * Alone among subclass choices in being re-asked at every level: *"The capricious nature
     * of elemental alliances extends to your pact. You can change your chosen element — and
     * your patron — whenever you gain a level."* [Choice.changeableOnLevelUp] is what carries
     * that, and the level-up flow shows the current answer already ticked so a Warlock who is
     * happy with fire simply moves on.
     */
    private fun elementChoice() = Choice(
        id = ELEMENT_CHOICE_ID,
        label = "Primordial Element",
        prompt = "Choose your element. It sets the damage type of your node and which spells " +
            "your patron keeps ready. You choose again every time you gain a level.",
        count = 1,
        kind = ChoiceKind.OPTION,
        options = listOf(
            ChoiceOption("air", "Air", "Patrons such as Akadi or Yan-C-Bin. Your node and your features deal Thunder damage.", "Thunder"),
            ChoiceOption("earth", "Earth", "Patrons such as Ogrémoch or Grumbar. Your node and your features deal Acid damage.", "Acid"),
            ChoiceOption("fire", "Fire", "Patrons such as Imix, Kossuth, or Zaaman Rul. Your node and your features deal Fire damage.", "Fire"),
            ChoiceOption("water", "Water", "Patrons such as Istishia or Olhydra. Your node and your features deal Cold damage.", "Cold"),
        ),
        source = "Level 3",
        changeableOnLevelUp = true,
    )

    /**
     * The Prepared Spells column both third casters share, by class level 1..20.
     *
     * Zero until level 3, which is when the subclass arrives. The two tables are identical in
     * the 2024 rules, so they are written once.
     */
    private val THIRD_CASTER_PREPARED = listOf(
        0, 0, 3, 4, 4, 4, 5, 6, 6, 7, 8, 8, 9, 10, 10, 11, 11, 11, 12, 13,
    )

    private fun f(level: Int, name: String, description: String, vararg choices: Choice) =
        SubclassFeature(level, name, description, choices.toList())

    private fun damageTypeChoice(
        id: String,
        label: String,
        source: String,
        types: List<String>,
        count: Int = 1,
        changeableOnRest: Boolean = false,
    ) = Choice(
        id = id,
        label = label,
        prompt = "Choose $count damage type${if (count == 1) "" else "s"}.",
        count = count,
        kind = ChoiceKind.DAMAGE_TYPE,
        options = ChoiceOptions.fromStrings(types),
        source = source,
        changeableOnRest = changeableOnRest,
    )

    private val BATTLE_MASTER_MANEUVERS = listOf(
        ChoiceOption(
            "ambush", "Ambush",
            "When you make a Dexterity (Stealth) check or an Initiative roll, you can expend " +
                "one Superiority Die and add the die to the roll, unless you have the " +
                "Incapacitated condition.",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "bait_and_switch", "Bait and Switch",
            "When you're within 5 feet of a creature on your turn, you can expend one " +
                "Superiority Die and switch places with that creature, provided you spend at " +
                "least 5 feet of movement and the creature is willing and doesn't have the " +
                "Incapacitated condition. This movement doesn't provoke Opportunity Attacks. " +
                "Roll the Superiority Die. Until the start of your next turn, you or the other " +
                "creature (your choice) gains a bonus to AC equal to the number rolled.",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "commanders_strike", "Commander's Strike",
            "When you take the Attack action on your turn, you can replace one of your attacks " +
                "to direct one of your companions to strike. When you do so, choose an ally " +
                "within 30 feet of yourself who can see or hear you, and expend one Superiority " +
                "Die. That ally can immediately use their Reaction to make one attack with a " +
                "weapon or an Unarmed Strike, adding the Superiority Die to the attack's damage " +
                "roll on a hit.",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "disarming_attack", "Disarming Attack",
            "When you hit a creature with an attack roll, you can expend one Superiority Die to " +
                "attempt to knock an object from the target's grasp. Add the Superiority Die " +
                "roll to the attack's damage roll. The target must succeed on a Strength saving " +
                "throw or drop an object of your choice that it's holding, with the object " +
                "landing in its space.",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "distracting_strike", "Distracting Strike",
            "When you hit a creature with an attack roll, you can expend one Superiority Die to " +
                "distract the target. Add the Superiority Die roll to the attack's damage roll. " +
                "The next attack roll against the target by an attacker other than you has " +
                "Advantage if the attack is made before the start of your next turn.",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "evasive_footwork", "Evasive Footwork",
            "As a Bonus Action, you can expend one Superiority Die and take the Disengage " +
                "action. If you do so, roll the die and add the number rolled to your AC until " +
                "the start of your next turn.",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "feinting_attack", "Feinting Attack",
            "As a Bonus Action, you can expend one Superiority Die to feint, choosing one " +
                "creature within 5 feet of yourself as your target. You have Advantage on your " +
                "next attack roll against that target this turn. If that attack hits, add the " +
                "Superiority Die to the attack's damage roll.",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "goading_attack", "Goading Attack",
            "When you hit a creature with an attack roll, you can expend one Superiority Die to " +
                "attempt to goad the target into attacking you. Add the Superiority Die roll to " +
                "the attack's damage roll. The target must succeed on a Wisdom saving throw or " +
                "have Disadvantage on attack rolls against targets other than you until the end " +
                "of your next turn.",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "lunging_attack", "Lunging Attack",
            "When you move at least 5 feet on your turn, you can expend one Superiority Die and " +
                "make a melee attack with a reach that is 5 feet greater than normal. If you " +
                "hit, add the Superiority Die to the attack's damage roll.",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "maneuvering_attack", "Maneuvering Attack",
            "When you hit a creature with an attack roll, you can expend one Superiority Die to " +
                "maneuver one of your comrades into another position. Add the Superiority Die " +
                "roll to the attack's damage roll, and choose a willing ally who can see or hear " +
                "you. That ally can use their Reaction to move up to half their Speed without " +
                "provoking an Opportunity Attack from the target of your attack.",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "menacing_attack", "Menacing Attack",
            "When you hit a creature with an attack roll, you can expend one Superiority Die to " +
                "attempt to frighten the target. Add the Superiority Die roll to the attack's " +
                "damage roll. The target must succeed on a Wisdom saving throw or have the " +
                "Frightened condition until the end of your next turn.",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "parry", "Parry",
            "When another creature damages you with a melee attack roll, you can take a " +
                "Reaction and expend one Superiority Die to reduce the damage by the number you " +
                "roll on the die plus your Strength or Dexterity modifier (your choice).",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "precision_attack", "Precision Attack",
            "When you miss with an attack roll, you can expend one Superiority Die, roll the " +
                "die, and add it to the attack roll, potentially causing the attack to hit.",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "pushing_attack", "Pushing Attack",
            "When you hit a creature with an attack using a weapon or an Unarmed Strike, you " +
                "can expend one Superiority Die to attempt to drive the target back. Add the " +
                "Superiority Die roll to the attack's damage roll. If the target is Large or " +
                "smaller, it must succeed on a Strength saving throw or be pushed up to 15 feet " +
                "directly away from you.",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "rally", "Rally",
            "As a Bonus Action, you can expend one Superiority Die to bolster the resolve of an " +
                "ally. Choose an ally who can see or hear you and who is within 30 feet of you. " +
                "That creature gains Temporary Hit Points equal to the Superiority Die roll plus " +
                "your Charisma modifier (minimum of 1 Temporary Hit Point).",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "riposte", "Riposte",
            "When a creature misses you with a melee attack roll, you can take a Reaction and " +
                "expend one Superiority Die to make a melee attack roll with a weapon or an " +
                "Unarmed Strike against the creature. If you hit, add the Superiority Die to the " +
                "attack's damage roll.",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "sweeping_attack", "Sweeping Attack",
            "When you hit a creature with a melee attack roll using a weapon or an Unarmed " +
                "Strike, you can expend one Superiority Die to attempt to damage another " +
                "creature. Choose another creature within 5 feet of the original target and " +
                "within your reach. If the original attack roll would hit the second creature, " +
                "it takes damage equal to the number you roll on your Superiority Die. The " +
                "damage is of the same type dealt by the original attack.",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "tactical_assessment", "Tactical Assessment",
            "When you make an Intelligence (History), Intelligence (Investigation), or Wisdom " +
                "(Insight) check, you can expend one Superiority Die and add the die to the " +
                "ability check.",
            "1 Superiority Die",
        ),
        ChoiceOption(
            "trip_attack", "Trip Attack",
            "When you hit a creature with an attack roll using a weapon or an Unarmed Strike, " +
                "you can expend one Superiority Die and add the die to the attack's damage roll. " +
                "If the target is Large or smaller, it must succeed on a Strength saving throw " +
                "or have the Prone condition.",
            "1 Superiority Die",
        ),
    )

    private fun maneuverChoice(id: String, count: Int, level: Int) = Choice(
        id = id,
        label = "Maneuvers",
        prompt = "Choose $count maneuver${if (count == 1) "" else "s"}.",
        count = count,
        kind = ChoiceKind.OPTION,
        options = BATTLE_MASTER_MANEUVERS,
        source = "Level $level",
        resourceId = "battle_master:superiority",
    )

    private val ARMOR_MODEL_OPTIONS = listOf(
        ChoiceOption(
            "dreadnaught", "Dreadnaught",
            "You design your armor to become a towering juggernaut in battle. Force Demolisher: " +
                "an arcane wrecking ball or sledgehammer projects from your armor, counting as a " +
                "Simple Melee weapon with the Reach property that deals 1d10 Force damage on a " +
                "hit; if you hit a creature at least one size smaller than you, you can push or " +
                "pull it up to 10 feet. Giant Stature: as a Bonus Action you enlarge the armor " +
                "for 1 minute, increasing your reach by 5 feet and making you Large if you are " +
                "smaller, a number of times equal to your Intelligence modifier per Long Rest.",
            "Force Demolisher",
        ),
        ChoiceOption(
            "guardian", "Guardian",
            "You design your armor to be in the front line of conflict. Thunder Pulse: strikes " +
                "from your armor count as a Simple Melee weapon dealing 1d8 Thunder damage, and " +
                "a creature hit by the pulse has Disadvantage on attack rolls against targets " +
                "other than you until the start of your next turn. Defensive Field: while " +
                "Bloodied, you can take a Bonus Action to gain Temporary Hit Points equal to " +
                "your Artificer level, which you lose if you doff the armor.",
            "Thunder Pulse",
        ),
        ChoiceOption(
            "infiltrator", "Infiltrator",
            "You customize your armor for subtler undertakings. Lightning Launcher: a gemlike " +
                "node counts as a Simple Ranged weapon with a range of 90/300 feet dealing 1d6 " +
                "Lightning damage, plus an extra 1d6 once on each of your turns. Powered Steps: " +
                "your Speed increases by 5 feet. Dampening Field: you have Advantage on Dexterity " +
                "(Stealth) checks.",
            "Lightning Launcher",
        ),
    )

    private fun armorModelChoice() = Choice(
        id = "armor_model",
        label = "Armor Model",
        prompt = "Choose the model your Arcane Armor takes.",
        count = 1,
        kind = ChoiceKind.OPTION,
        options = ARMOR_MODEL_OPTIONS,
        source = "Level 3",
        // The rules let you re-forge the armor into a different model on any rest.
        changeableOnRest = true,
    )

    // The Eldritch Cannon's modes are chosen when the cannon is activated, not once at
    // creation, so they live in PerUseChoiceData with the other per-use decisions.

    private val ARCANE_SHOT_OPTIONS = listOf(
        ChoiceOption(
            "banishing", "Banishing Shot",
            "You weave banishing magic into your shot. The target takes an extra 2d6 Force " +
                "damage, and it must succeed on a Charisma saving throw or have the Incapacitated " +
                "condition and be transported to a harmless demiplane until the start of your " +
                "next turn. At the end of that turn, the target reappears in the space it left " +
                "or in the nearest unoccupied space if that space is occupied.",
            "1 Arcane Shot use",
        ),
        ChoiceOption(
            "beguiling", "Beguiling Shot",
            "Your enchantment magic causes this shot to temporarily beguile its target. The " +
                "target takes an extra 2d6 Psychic damage, and it must succeed on a Wisdom " +
                "saving throw or have the Charmed condition until the start of your next turn. " +
                "While Charmed in this way, the target is Charmed by a creature of your choice " +
                "that you can see, and the condition ends early if that creature attacks the " +
                "target or deals damage to it.",
            "1 Arcane Shot use",
        ),
        ChoiceOption(
            "bursting", "Bursting Shot",
            "You imbue your shot with force energy drawn from the school of evocation. The " +
                "target and each creature within 10 feet of it take 2d6 Force damage each.",
            "1 Arcane Shot use",
        ),
        ChoiceOption(
            "enfeebling", "Enfeebling Shot",
            "You weave necromantic magic into your shot. The target takes an extra 2d6 Necrotic " +
                "damage, and it must succeed on a Constitution saving throw or have the Poisoned " +
                "condition until the start of your next turn. While Poisoned in this way, the " +
                "damage of the target's attacks is halved (round down).",
            "1 Arcane Shot use",
        ),
        ChoiceOption(
            "grasping", "Grasping Shot",
            "When this shot strikes its target, conjuration magic creates grasping, poisonous " +
                "brambles which wrap around the target. The target takes an extra 2d6 Slashing " +
                "damage, and it must succeed on a Strength saving throw or have the Restrained " +
                "condition and its Speed reduced to 0. The target repeats the save at the end of " +
                "each of its turns, ending the effect on itself on a success; it takes 2d6 " +
                "Slashing damage each time it fails.",
            "1 Arcane Shot use",
        ),
        ChoiceOption(
            "piercing", "Piercing Shot",
            "You use transmutation magic to give your shot an ethereal quality. When you use " +
                "this option, you don't make an attack roll. Instead, the shot travels in a " +
                "30-foot-long, 1-foot-wide Line, passing through creatures and objects and " +
                "ignoring Cover. Each creature in the Line makes a Dexterity saving throw, " +
                "taking the weapon's damage plus an extra 1d6 Force damage on a failed save, or " +
                "half as much damage on a successful one.",
            "1 Arcane Shot use",
        ),
        ChoiceOption(
            "seeking", "Seeking Shot",
            "Using divination magic, you grant your shot the ability to seek out a target. When " +
                "you use this option, you don't make an attack roll. Instead, choose one " +
                "creature you have seen in the past minute. The shot flies toward that creature, " +
                "moving around corners if necessary and ignoring Cover. If the target is within " +
                "the weapon's range and there is a path large enough for the shot to travel, the " +
                "target must make a Dexterity saving throw, taking the weapon's damage plus an " +
                "extra 2d6 Force damage on a failed save, or half as much damage on a successful " +
                "one. You then learn the target's current location.",
            "1 Arcane Shot use",
        ),
        ChoiceOption(
            "shadow", "Shadow Shot",
            "You weave illusion magic into your shot, causing it to occlude your foe's vision " +
                "with shadows. The target takes an extra 2d6 Psychic damage, and it must succeed " +
                "on a Wisdom saving throw or have the Blinded condition until the start of your " +
                "next turn.",
            "1 Arcane Shot use",
        ),
    )

    private fun arcaneShotChoice(id: String, count: Int, level: Int) = Choice(
        id = id,
        label = "Arcane Shot Options",
        prompt = "Choose $count Arcane Shot option${if (count == 1) "" else "s"}.",
        count = count,
        kind = ChoiceKind.OPTION,
        options = ARCANE_SHOT_OPTIONS,
        source = "Level $level",
        resourceId = "arcane_archer:arcane_shot",
    )

    val ALL: List<Subclass> = listOf(

        // ================================================================= Barbarian
        Subclass("berserker", "barbarian", "Path of the Berserker",
            "Channel Rage into a frenzy of extra attacks and raw intimidation.",
            listOf(
                f(3, "Frenzy", "While raging, deal extra damage equal to a number of d6s from your Rage Damage bonus when you use Reckless Attack."),
                f(6, "Mindless Rage", "You can't be Charmed or Frightened while raging, and such effects are paused for the duration."),
                f(10, "Retaliation", "As a Reaction when a creature within 5 feet damages you, make a melee attack against it."),
                f(14, "Intimidating Presence", "As a Bonus Action, force nearby creatures to make a Wisdom save or be Frightened of you."),
            )),
        // The article renames this one: it is the Path of the Ancestral Guardian from
        // Xanathar's, rebuilt and reprinted as the Path of the Spiritual Guardian. The old
        // name is kept in the summary so a player looking for it finds it in a search.
        Subclass("spiritual_guardian", "barbarian", "Path of the Spiritual Guardian",
            "Call ancestral, bestial, or elemental spirits into your Rage to distract, shield, and strike. Formerly the Path of the Ancestral Guardian.",
            listOf(
                f(3, "Spiritual Protectors", "Your Rage summons spectral warriors to your aid. While your Rage is active, when you hit a creature with a weapon or an Unarmed Strike, it suffers one of the following effects of your choice. Distract: until the start of your next turn, the target has Disadvantage on attack rolls against targets other than you or another Barbarian who has this feature. Protect: until the end of the target's next turn, the next time it hits a creature other than you with an attack roll, that creature has Resistance to the damage dealt by the attack. Strike: the target takes an extra 1d6 damage, which can be Acid, Cold, Fire, Force, Lightning, or Thunder damage (your choice)."),
                f(6, "Spirit Shield", "Your guardian spirits can provide supernatural protection to those you defend. While your Rage is active, when another creature you can see within 30 feet of you takes damage, you can take a Reaction to reduce that damage. To determine the amount the damage is reduced by, roll a number of d6s equal to your Rage Damage bonus, and add them together."),
                f(10, "Consult the Spirits", "You gain the ability to consult with your guardian spirits. When you do so, you cast the Augury or Clairvoyance spell, without expending a spell slot or needing Material components. Rather than creating a spherical sensor, this use of Clairvoyance invisibly summons one of your guardian spirits to the chosen location. Wisdom is your spellcasting ability for these spells. After you cast either spell in this way, you can't use this feature again until you finish a Short or Long Rest."),
                f(14, "Vengeful Spirits", "When you make an attack roll with a Melee weapon as part of the Attack action and roll 18-20 on the d20, you can make one additional attack roll with the same weapon as part of that action. Once you use this feature, you can't do so again until the start of your next turn."),
            ),
            book = UA_SUB),
        Subclass("wild_heart", "barbarian", "Path of the Wild Heart",
            "Draw on animal spirits for speed, senses, and unnatural resilience.",
            listOf(
                f(3, "Animal Speaker", "You can cast Beast Sense and Speak with Animals as rituals using Rage uses."),
                f(3, "Rage of the Wilds", "Choose an animal aspect that shapes your Rage.",
                    Choice("rage_wilds", "Rage of the Wilds", "Choose an animal spirit.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("bear", "Bear", "You have Resistance to every damage type except Force, Necrotic, Psychic, and Radiant."),
                        ChoiceOption("eagle", "Eagle", "You can take the Disengage and Dash actions as a Bonus Action."),
                        ChoiceOption("wolf", "Wolf", "Allies have Advantage on attacks against enemies within 5 feet of you."),
                    ), "Level 3")),
                f(6, "Aspect of the Wilds", "Choose a lasting animal aspect that works even outside Rage.",
                    Choice("aspect_wilds", "Aspect of the Wilds", "Choose an animal aspect.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("owl", "Owl", "You gain Darkvision out to 60 feet, or an extra 60 feet if you already have it."),
                        ChoiceOption("panther", "Panther", "You gain a Climb Speed equal to your Speed."),
                        ChoiceOption("salmon", "Salmon", "You gain a Swim Speed equal to your Speed."),
                    ), "Level 6")),
                f(10, "Nature Speaker", "You can cast Commune with Nature as a ritual using a Rage use."),
                f(14, "Power of the Wilds", "While raging, choose a further animal power: Falcon's flight, Lion's roar, or Ram's charge."),
            )),
        Subclass("world_tree", "barbarian", "Path of the World Tree",
            "Root your Rage in the cosmic tree Yggdrasil, healing and repositioning allies.",
            listOf(
                f(3, "Vitality of the Tree", "When you enter Rage, gain Temporary Hit Points and share them with an ally each turn."),
                f(6, "Branches of the Tree", "As a Reaction while raging, teleport a creature within 30 feet to a space next to you."),
                f(10, "Battering Roots", "Your melee weapons with Push or Topple gain 10 feet of reach on the Attack action."),
                f(14, "Travel along the Tree", "While raging, teleport up to 60 feet as a Bonus Action, and bring willing allies along."),
            )),
        Subclass("zealot", "barbarian", "Path of the Zealot",
            "A divine warrior whose Rage burns with holy or unholy fervor.",
            listOf(
                f(3, "Divine Fury", "While raging, your first hit each turn deals extra Necrotic or Radiant damage.",
                    damageTypeChoice("divine_fury_type", "Divine Fury Damage", "Level 3", listOf("Necrotic", "Radiant"))),
                f(3, "Warrior of the Gods", "You have a pool of d12s you can spend to heal yourself without a spell."),
                f(6, "Fanatical Focus", "Once per Rage, reroll a failed saving throw with a bonus equal to your Rage Damage."),
                f(10, "Zealous Presence", "As a Bonus Action, grant up to ten allies Advantage on attacks and saves for a round."),
                f(14, "Rage Beyond Death", "While raging, you don't fall Unconscious at 0 hit points and can keep fighting."),
            )),

        // ================================================================= Bard
        Subclass("dance", "bard", "College of Dance",
            "Movement is your instrument; you weave defense and inspiration through motion.",
            listOf(
                f(3, "Dazzling Footwork", "While unarmored, your AC uses Charisma, and your Unarmed Strikes deal Bardic Inspiration die damage."),
                f(6, "Inspiring Movement", "As a Reaction when an enemy nears an ally, move and let the ally move as well."),
                f(6, "Tandem Footwork", "When you roll Initiative, you and nearby allies add your Bardic Inspiration die to Initiative."),
                f(14, "Leading Evasion", "You and allies sharing your space take no damage on a successful Dexterity save."),
            )),
        Subclass("glamour", "bard", "College of Glamour",
            "Fey-touched charm that awes crowds and bends hostile minds.",
            listOf(
                f(3, "Beguiling Magic", "You always have Charm Person and Mirror Image prepared, and can enthrall a creature after casting an Illusion or Enchantment spell."),
                f(3, "Mantle of Inspiration", "As a Bonus Action, grant allies Temporary Hit Points and a free Reaction move."),
                f(6, "Mantle of Majesty", "Cast Command as a Bonus Action each turn for a minute without expending further slots."),
                f(14, "Unbreakable Majesty", "Attackers must make a Charisma save or lose the attack and be unable to target you."),
            )),
        Subclass("lore", "bard", "College of Lore",
            "A collector of secrets who cuts foes down with a well-timed word.",
            listOf(
                f(3, "Bonus Proficiencies", "Gain proficiency in three skills of your choice.",
                    Choice("lore_skills", "Bonus Proficiencies", "Choose three skills.", 3, ChoiceKind.SKILL,
                        ChoiceOptions.fromSkills(Skill.ALL), "Level 3")),
                f(3, "Cutting Words", "As a Reaction, spend a Bardic Inspiration die to subtract it from a foe's roll."),
                f(6, "Magical Discoveries", "Learn two spells from the Cleric, Druid, or Wizard lists that count as Bard spells for you."),
                f(14, "Peerless Skill", "Spend a Bardic Inspiration die to add it to a failed ability check or attack roll."),
            )),
        Subclass("valor", "bard", "College of Valor",
            "A battle skald who fights beside the heroes whose deeds they sing.",
            listOf(
                f(3, "Combat Inspiration", "Allies can add your Bardic Inspiration die to a damage roll or their AC against one attack."),
                f(3, "Martial Training", "You gain training with Martial weapons and Medium armor and shields, and can use an instrument as a focus."),
                f(6, "Extra Attack", "You can attack twice instead of once, and can replace one attack with a cantrip."),
                f(14, "Battle Magic", "After casting a spell with an action, make one weapon attack as a Bonus Action."),
            )),

        // ================================================================= Cleric
        Subclass("life_domain", "cleric", "Life Domain",
            "A healer whose magic mends wounds faster and more fully than any other.",
            listOf(
                f(3, "Disciple of Life", "Your healing spells restore extra hit points equal to 2 plus the spell's level."),
                f(3, "Preserve Life", "Spend Channel Divinity to distribute healing equal to five times your Cleric level."),
                f(3, "Life Domain Spells", "You always have Aid, Bless, Cure Wounds, Lesser Restoration, Mass Healing Word, Revivify, and more prepared."),
                f(6, "Blessed Healer", "When you heal another creature with a spell, you also regain hit points."),
                f(17, "Supreme Healing", "Your healing dice always roll their maximum value."),
            )),
        Subclass("light_domain", "cleric", "Light Domain",
            "Radiance made manifest: searing light that blinds and burns.",
            listOf(
                f(3, "Radiance of the Dawn", "Spend Channel Divinity to dispel magical darkness and deal Radiant damage to nearby foes."),
                f(3, "Warding Flare", "As a Reaction, impose Disadvantage on an attack roll against you or a nearby ally."),
                f(3, "Light Domain Spells", "You always have Burning Hands, Faerie Fire, Scorching Ray, Daylight, Fireball, and more prepared."),
                f(6, "Improved Warding Flare", "Warding Flare also grants the target healing, and you regain uses on a Short Rest."),
                f(17, "Corona of Light", "Emit a 60-foot aura of light that gives enemies Disadvantage on saves against your Radiant and Fire spells."),
            )),
        Subclass("trickery_domain", "cleric", "Trickery Domain",
            "A divine trickster who fights with illusions and misdirection.",
            listOf(
                f(3, "Blessing of the Trickster", "Give a creature Advantage on Stealth checks for an hour."),
                f(3, "Invoke Duplicity", "Spend Channel Divinity to create an illusory double you can cast spells through."),
                f(3, "Trickery Domain Spells", "You always have Charm Person, Disguise Self, Invisibility, Pass without Trace, Polymorph, and more prepared."),
                f(6, "Trickster's Transposition", "When your duplicate appears or moves, you can swap places with it as a Bonus Action."),
                f(17, "Improved Duplicity", "Your duplicate grants allies Advantage and heals a creature when it vanishes."),
            )),
        Subclass("war_domain", "cleric", "War Domain",
            "A frontline priest whose god blesses every strike.",
            listOf(
                f(3, "Guided Strike", "Spend Channel Divinity to add +10 to an attack roll after seeing the result."),
                f(3, "War Priest", "As a Bonus Action, make one attack with a weapon or Unarmed Strike."),
                f(3, "War Domain Spells", "You always have Divine Favor, Shield of Faith, Magic Weapon, Spiritual Weapon, Fireball, and more prepared."),
                f(6, "War God's Blessing", "Spend Channel Divinity to cast Shield of Faith or Spiritual Weapon without a slot."),
                f(17, "Avatar of Battle", "You gain Resistance to Bludgeoning, Piercing, and Slashing damage."),
            )),

        // ================================================================= Druid
        Subclass("land", "druid", "Circle of the Land",
            "Magic drawn from a specific landscape, with recovery to match.",
            listOf(
                f(3, "Circle of the Land Spells", "You learn spells tied to the terrain you bond with.",
                    Choice("land_type", "Land Type", "Choose the land your circle is bound to.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("arid", "Arid", "Blur, Burning Hands, Fire Bolt, Blight, Wall of Fire."),
                        ChoiceOption("polar", "Polar", "Fog Cloud, Hold Person, Ray of Frost, Sleet Storm, Cone of Cold."),
                        ChoiceOption("temperate", "Temperate", "Misty Step, Shocking Grasp, Sleep, Lightning Bolt, Ice Storm."),
                        ChoiceOption("tropical", "Tropical", "Acid Splash, Ray of Sickness, Web, Stinking Cloud, Polymorph."),
                    ), "Level 3")),
                f(3, "Land's Aid", "Expend a Wild Shape use to damage foes and heal an ally in a 10-foot sphere."),
                f(6, "Natural Recovery", "Cast one Circle spell without a slot per Long Rest, and recover slots on a Short Rest."),
                f(10, "Nature's Ward", "You are immune to the Poisoned condition and resistant to your circle's damage type."),
                f(14, "Nature's Sanctuary", "Spend a Wild Shape use to create a 15-foot aura granting allies Half Cover and resistance."),
            )),
        Subclass("moon", "druid", "Circle of the Moon",
            "A shapeshifter who fights in beast form.",
            listOf(
                f(3, "Circle Forms", "Your Wild Shape forms can be more powerful, with bonus Temporary Hit Points."),
                f(6, "Improved Circle Forms", "While in Wild Shape, add your Wisdom modifier to Constitution saves and deal magical damage."),
                f(10, "Moonlight Step", "As a Bonus Action, teleport 30 feet and gain Advantage on your next attack."),
                f(14, "Lunar Form", "Your Wild Shape attacks deal extra Radiant damage, and you can bring an ally when you Moonlight Step."),
            )),
        Subclass("sea", "druid", "Circle of the Sea",
            "Command the storm and tide as an extension of yourself.",
            listOf(
                f(3, "Wrath of the Sea", "Expend a Wild Shape use to emanate a 5-foot aura dealing Cold damage and pushing foes."),
                f(3, "Circle of the Sea Spells", "You always have Fog Cloud, Gust of Wind, Water Breathing, Control Water, and more prepared."),
                f(6, "Aquatic Affinity", "Your aura grows to 10 feet and you gain a Swim Speed equal to your Speed."),
                f(10, "Stormborn", "While your aura is active you gain a Fly Speed and Resistance to Cold, Lightning, and Thunder."),
                f(14, "Oceanic Gift", "You can center Wrath of the Sea on an ally instead of yourself."),
            )),
        Subclass("stars", "druid", "Circle of the Stars",
            "Read the constellations and take on their forms.",
            listOf(
                f(3, "Star Map", "You carry a map granting Guidance and Guiding Bolt without expending slots."),
                f(3, "Starry Form", "Expend a Wild Shape use to take an Archer, Chalice, or Dragon constellation form. You choose which constellation each time you assume the form."),
                f(6, "Cosmic Omen", "After a Long Rest, roll to determine a Weal or Woe omen you can invoke as a Reaction."),
                f(10, "Twinkling Constellations", "Your Starry Form improves and you can change constellation each turn."),
                f(14, "Full of Stars", "While in Starry Form you are partially incorporeal, gaining Resistance to several damage types."),
            )),

        // ================================================================= Fighter
        Subclass("battle_master", "fighter", "Battle Master",
            "A tactician who spends Superiority Dice on precise battlefield maneuvers.",
            listOf(
                f(3, "Combat Superiority", "You gain four Superiority Dice (d8) that fuel your maneuvers."),
                f(3, "Maneuvers", "Learn three maneuvers of your choice.",
                    maneuverChoice("bm_maneuvers_3", 3, 3)),
                f(3, "Student of War", "Gain proficiency with one type of Artisan's Tools and one skill from the Fighter list.",
                    Choice("student_of_war", "Student of War", "Choose a Fighter skill proficiency.", 1, ChoiceKind.SKILL,
                        ChoiceOptions.fromSkills(listOf(Skill.ACROBATICS, Skill.ANIMAL_HANDLING, Skill.ATHLETICS, Skill.HISTORY, Skill.INSIGHT, Skill.INTIMIDATION, Skill.PERCEPTION, Skill.SURVIVAL)), "Level 3")),
                f(7, "Know Your Enemy", "As a Bonus Action, learn whether a creature's defenses are stronger or weaker than yours."),
                f(7, "Additional Maneuvers", "Learn two more maneuvers.",
                    maneuverChoice("bm_maneuvers_7", 2, 7)),
                f(10, "Improved Combat Superiority", "Your Superiority Dice become d10s."),
                f(10, "Additional Maneuvers", "Learn two more maneuvers.",
                    maneuverChoice("bm_maneuvers_10", 2, 10)),
                f(15, "Relentless", "When you roll Initiative with no Superiority Dice, you regain one."),
                f(15, "Additional Maneuvers", "Learn two more maneuvers.",
                    maneuverChoice("bm_maneuvers_15", 2, 15)),
                f(18, "Ultimate Combat Superiority", "Your Superiority Dice become d12s."),
            )),
        Subclass("champion", "fighter", "Champion",
            "Raw physical excellence and a wider path to a critical hit.",
            listOf(
                f(3, "Improved Critical", "Your attack rolls score a Critical Hit on a 19 or 20."),
                f(3, "Remarkable Athlete", "You have Advantage on Initiative and Athletics checks, and can move further after a Critical Hit."),
                f(7, "Additional Fighting Style", "You gain a second Fighting Style feat.",
                    Choice("champion_style", "Additional Fighting Style", "Choose a second Fighting Style.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("archery", "Archery", "+2 to attack rolls with ranged weapons."),
                        ChoiceOption("blind_fighting", "Blind Fighting", "Blindsight with a range of 10 feet."),
                        ChoiceOption("defense", "Defense", "+1 AC while wearing armor."),
                        ChoiceOption("dueling", "Dueling", "+2 damage with a one-handed melee weapon."),
                        ChoiceOption("great_weapon", "Great Weapon Fighting", "Treat a 1 or 2 on a two-handed damage die as a 3."),
                        ChoiceOption("interception", "Interception", "Reduce damage to a nearby creature as a Reaction."),
                        ChoiceOption("protection", "Protection", "Impose Disadvantage on an attack against an adjacent creature."),
                        ChoiceOption("thrown_weapon", "Thrown Weapon Fighting", "+2 damage with thrown weapons."),
                        ChoiceOption("two_weapon", "Two-Weapon Fighting", "Add your ability modifier to the off-hand damage."),
                        ChoiceOption("unarmed", "Unarmed Fighting", "Your Unarmed Strike deals 1d6, or 1d8 unarmed and unshielded."),
                    ), "Level 7")),
                f(10, "Heroic Warrior", "During combat you can give yourself Heroic Inspiration at the start of your turn."),
                f(15, "Superior Critical", "Your attack rolls score a Critical Hit on an 18, 19, or 20."),
                f(18, "Survivor", "You gain Advantage on death saves and regain hit points each turn while Bloodied."),
            )),
        Subclass("eldritch_knight", "fighter", "Eldritch Knight",
            "A warrior who binds Wizard magic to blade and armor.",
            listOf(
                f(3, "Spellcasting", "You have learned to cast spells. See the Player's Handbook for the rules on spellcasting. Cantrips: you know two Wizard cantrips of your choice, and you learn another at Fighter level 10. Whenever you gain a Fighter level, you can replace one of these cantrips with another Wizard cantrip. Spell Slots and Prepared Spells: the Eldritch Knight Spellcasting table shows how many spell slots you have and how many level 1+ Wizard spells you can have prepared; those spells must be Abjuration or Evocation, except that the spells you gain at Fighter levels 8, 14, and 20 can be from any school. Whenever you gain a Fighter level, you can replace one prepared spell with another Wizard spell of an eligible school and level. Intelligence is your spellcasting ability."),
                f(3, "War Bond", "Bond with two weapons so you can summon them to your hand as a Bonus Action."),
                f(7, "War Magic", "When you take the Attack action, you can replace one attack with a cantrip."),
                f(10, "Eldritch Strike", "When you hit with a weapon, the target has Disadvantage on its next save against your spells."),
                f(15, "Arcane Charge", "When you use Action Surge, you can teleport up to 30 feet."),
                f(18, "Improved War Magic", "When you take the Attack action, you can replace one attack with a level 1 or 2 spell."),
            ),
            casterType = CasterType.THIRD,
            spellcastingAbility = Ability.INT,
            spellListClassId = "wizard",
            cantripsKnown = mapOf(3 to 2, 10 to 3),
            preparedSpells = THIRD_CASTER_PREPARED,
            spellSchools = setOf("Abjuration", "Evocation"),
            freeSchoolLevels = setOf(8, 14, 20)),
        Subclass("psi_warrior", "fighter", "Psi Warrior",
            "Telekinetic force amplifies every swing and shields every ally.",
            listOf(
                f(3, "Psionic Power", "You gain Psionic Energy Dice fuelling Protective Field, Psionic Strike, and Telekinetic Movement."),
                f(7, "Telekinetic Adept", "You gain Psi-Powered Leap and Telekinetic Thrust, letting you fling or topple targets."),
                f(10, "Guarded Mind", "You have Resistance to Psychic damage and can end the Charmed or Frightened condition on yourself."),
                f(15, "Bulwark of Force", "As a Bonus Action, grant yourself and allies Half Cover for a minute."),
                f(18, "Telekinetic Master", "You can cast Telekinesis and make a weapon attack as a Bonus Action while it lasts."),
            )),
        Subclass("arcane_archer", "fighter", "Arcane Archer",
            "Weave magic into your ammunition to produce supernatural effects.",
            listOf(
                f(3, "Arcane Archer Lore", "You learn magical theory and secrets of nature, gaining a cantrip and the Arcana and Nature skills.",
                    Choice("aa_cantrip", "Arcane Archer Cantrip", "Choose a cantrip. Intelligence is your spellcasting ability for it.", 1, ChoiceKind.SPELL, listOf(
                        ChoiceOption("druidcraft", "Druidcraft", "A small, harmless nature effect.", "Cantrip • Transmutation"),
                        ChoiceOption("prestidigitation", "Prestidigitation", "A minor magical trick.", "Cantrip • Transmutation"),
                    ), "Level 3")),
                f(3, "Arcane Shot", "Learn two Arcane Shot options, usable a number of times equal to your Intelligence modifier per Short or Long Rest. Your Arcane Shot Die is a d6, growing to d8 at level 10, d10 at 15, and d12 at 18.",
                    arcaneShotChoice("arcane_shot_3", 2, 3)),
                f(7, "Curving Shot", "When you miss with an Ammunition weapon, use a Bonus Action to ricochet the shot at a new target within 60 feet."),
                f(7, "Magical Ammunition", "As a Magic action, imbue ammunition with a Darkening, Unlocking, or Vine Shot effect, once per Short or Long Rest."),
                f(7, "Additional Arcane Shot", "Learn one more Arcane Shot option.",
                    arcaneShotChoice("arcane_shot_7", 1, 7)),
                f(10, "Ever-Ready Shot", "When you roll Initiative, you regain one expended use of Arcane Shot."),
                f(10, "Additional Arcane Shot", "Learn one more Arcane Shot option.",
                    arcaneShotChoice("arcane_shot_10", 1, 10)),
                f(15, "Arcane Burst", "When you use Indomitable, push creatures in a 10-foot Emanation up to 20 feet away on a failed Strength save."),
                f(15, "Additional Arcane Shot", "Learn one more Arcane Shot option.",
                    arcaneShotChoice("arcane_shot_15", 1, 15)),
                f(18, "Masterful Shots", "When a creature misses you, take a Reaction to move half your Speed away and shoot back."),
                f(18, "Additional Arcane Shot", "Learn one more Arcane Shot option.",
                    arcaneShotChoice("arcane_shot_18", 1, 18)),
            ),
            book = UA),

        // ================================================================= Monk
        Subclass("mercy", "monk", "Warrior of Mercy",
            "Manipulate the life force in others to heal or to harm.",
            listOf(
                f(3, "Hand of Harm", "Once per turn, spend a Focus Point when you hit to deal extra Necrotic damage."),
                f(3, "Hand of Healing", "Spend a Focus Point as a Magic action to restore hit points equal to a Martial Arts die roll plus your Wisdom modifier."),
                f(6, "Physician's Touch", "Hand of Healing also ends a condition, and Hand of Harm can Poison the target."),
                f(11, "Flurry of Healing and Harm", "Replace Flurry of Blows strikes with Hand of Healing uses at no Focus cost."),
                f(17, "Hand of Ultimate Mercy", "Spend 5 Focus Points to return a creature dead up to 24 hours to life."),
            )),
        Subclass("shadow", "monk", "Warrior of Shadow",
            "Step between shadows and strike from the dark.",
            listOf(
                f(3, "Shadow Arts", "You know Minor Illusion and can spend Focus Points to cast Darkness and see through it."),
                f(6, "Shadow Step", "While in Dim Light or Darkness, teleport up to 60 feet as a Bonus Action and gain Advantage on your next attack."),
                f(11, "Improved Shadow Step", "Shadow Step works in any light and lets you make an attack as part of the Bonus Action."),
                f(17, "Cloak of Shadows", "Spend 3 Focus Points to become Invisible for a minute without breaking on attacks."),
            )),
        Subclass("elements", "monk", "Warrior of the Elements",
            "Channel elemental power through reach, blasts, and mobility.",
            listOf(
                f(3, "Elemental Attunement", "Spend a Focus Point to give your Unarmed Strikes 10 feet of reach and elemental damage.",
                    damageTypeChoice("elemental_type", "Elemental Damage", "Level 3",
                        listOf("Acid", "Cold", "Fire", "Lightning", "Thunder"))),
                f(6, "Manipulate Elements", "You know the Elementalism cantrip and can push or pull creatures with your strikes."),
                f(11, "Stride of the Elements", "While Elemental Attunement is active you gain a Fly Speed and Swim Speed equal to your Speed."),
                f(17, "Elemental Epitome", "Gain Resistance to your chosen damage type, extra damage, and extra movement each turn."),
            )),
        Subclass("open_hand", "monk", "Warrior of the Open Hand",
            "The most direct expression of martial arts: overwhelming unarmed technique.",
            listOf(
                f(3, "Open Hand Technique", "Flurry of Blows strikes can also Addle, Push, or Topple the target."),
                f(6, "Wholeness of Body", "As a Bonus Action, regain hit points equal to a Martial Arts die roll plus your Wisdom modifier."),
                f(11, "Fleet Step", "When you use a Bonus Action other than Step of the Wind, you also gain its benefit."),
                f(17, "Quivering Palm", "Spend 4 Focus Points to set lethal vibrations in a creature you can end at will."),
            )),
        Subclass("tattooed_warrior", "monk", "Tattooed Warrior",
            "Magic tattoos that reshape themselves to grant a suite of physical and magical effects.",
            listOf(
                f(3, "Magic Tattoos", "Your tattoos' save DC is 8 plus your Wisdom modifier plus your Proficiency Bonus, and you can reshape one tattoo on a Long Rest."),
                f(3, "Beast Tattoos", "You gain two animal tattoos.",
                    Choice("beast_tattoos", "Beast Tattoos", "Choose two animal tattoos.", 2, ChoiceKind.OPTION, listOf(
                        ChoiceOption("bat", "Bat", "You know Dancing Lights and gain Blindsight out to 10 feet."),
                        ChoiceOption("butterfly", "Butterfly", "You know Light and can use Dexterity for High Jumps."),
                        ChoiceOption("crane", "Crane", "You know Guidance and gain Advantage after a missed Flurry of Blows attack."),
                        ChoiceOption("horse", "Horse", "You know Message and gain 10 feet of Speed when you use Step of the Wind."),
                        ChoiceOption("tortoise", "Tortoise", "You know Spare the Dying and gain +1 AC when you use Patient Defense."),
                    ), "Level 3", changeableOnRest = true)),
                f(6, "Celestial Tattoo", "You gain a tattoo depicting a celestial phenomenon.",
                    Choice("celestial_tattoo", "Celestial Tattoo", "Choose a celestial tattoo.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("comet", "Comet", "Spend a Focus Point to add a Martial Arts die to a Search check."),
                        ChoiceOption("eclipse", "Eclipse", "Spend a Focus Point to add a Martial Arts die to a Stealth check when you Hide."),
                        ChoiceOption("sunburst", "Sunburst", "Spend a Focus Point to add a Martial Arts die to a Study check."),
                    ), "Level 6", changeableOnRest = true)),
                f(11, "Nature Tattoo", "You gain a tattoo depicting a natural feature.",
                    Choice("nature_tattoo", "Nature Tattoo", "Choose a nature tattoo.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("sea_storm", "Sea Storm", "Resistance to Cold, Lightning, or Thunder damage, changeable on a rest."),
                        ChoiceOption("volcano", "Volcano", "Resistance to Acid, Fire, or Poison damage, changeable on a rest."),
                    ), "Level 11", changeableOnRest = true)),
                f(17, "Monster Tattoo", "You gain a tattoo depicting a mighty creature.",
                    Choice("monster_tattoo", "Monster Tattoo", "Choose a monster tattoo.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("beholder", "Beholder", "Gain a Fly Speed and fire four eye rays dealing Force damage."),
                        ChoiceOption("chromatic_dragon", "Chromatic Dragon", "Replace an attack with a 30-foot Cone of elemental damage."),
                        ChoiceOption("displacer_beast", "Displacer Beast", "Cast Mirror Image as part of Flurry of Blows or Step of the Wind."),
                        ChoiceOption("troll", "Troll", "Regain hit points at the start of each turn while Bloodied, and regrow severed parts."),
                    ), "Level 17", changeableOnRest = true)),
            ),
            book = UA),

        // ================================================================= Paladin
        Subclass("oathbreaker", "paladin", "Oathbreaker",
            "A Paladin who fell from grace and now wields fear and undeath as tools of power.",
            listOf(
                f(3, "Conjure Undead", "As a Bonus Action, expend one use of Channel Divinity to summon a number of Undead equal to half your Charisma modifier (round up, minimum one). Each appears in an unoccupied space you can see within 30 feet and is a Skeleton or Zombie of your choice. They are under your control for 1 minute, then dissolve into ash. Each is an ally to you and your allies, shares your Initiative count but acts immediately after you, and obeys your verbal commands; given none, it Dodges and moves to avoid danger."),
                f(3, "Dreadful Aspect", "Immediately after you cast Divine Smite, you can expend one use of Channel Divinity to channel a burst of magical menace. Each creature of your choice in a 30-foot Emanation originating from you must succeed on a Wisdom saving throw or have the Frightened condition for 1 minute, repeating the save at the end of each of its turns to end it."),
                f(3, "Oathbreaker Spells", "You always have Hellish Rebuke and Witch Bolt prepared, adding Crown of Madness and Darkness at level 5, Fear and Summon Undead at level 9, Blight and Phantasmal Killer at level 13, and Contagion and Steel Wind Strike at level 17."),
                f(7, "Aura of Hate", "When you, or any allied Fiend or Undead in your Aura of Protection, hits a creature with a melee attack, that attack deals extra Necrotic damage equal to your Charisma modifier."),
                f(15, "Supernatural Resistance", "You gain Resistance to Bludgeoning, Piercing, and Slashing damage."),
                f(20, "Dread Lord", "As a Bonus Action, imbue your Aura of Protection with unholy gloom for 10 minutes or until you end it. Darkness: magical Darkness fills the aura, and you and your allies in it can see through it. Fear: a Frightened creature that starts its turn in the aura takes 4d10 Psychic damage. Shadow Strike: as a Bonus Action, make a melee spell attack against one creature in the aura, dealing 3d10 Necrotic damage plus your Charisma modifier on a hit. Once used, you can't use it again until you finish a Long Rest, or until you expend a level 5 spell slot to restore it."),
            ),
            book = UA_SUB),
        Subclass("devotion", "paladin", "Oath of Devotion",
            "The classic knightly ideal: honesty, courage, and unwavering duty.",
            listOf(
                f(3, "Sacred Weapon", "Spend Channel Divinity to add your Charisma modifier to attack rolls and shed bright light."),
                f(3, "Oath of Devotion Spells", "You always have Protection from Evil and Good, Shield of Faith, Aid, Zone of Truth, and more prepared."),
                f(7, "Aura of Devotion", "You and allies within 10 feet can't be Charmed."),
                f(15, "Smite of Protection", "When you cast Divine Smite, allies near you gain Half Cover."),
                f(20, "Holy Nimbus", "Become a beacon of light dealing Radiant damage and granting Advantage on saves against spells."),
            )),
        Subclass("glory", "paladin", "Oath of Glory",
            "A hero in pursuit of legend, driving allies to greatness.",
            listOf(
                f(3, "Peerless Athlete", "Spend Channel Divinity for Advantage on Athletics and Acrobatics and improved jumps."),
                f(3, "Inspiring Smite", "After casting Divine Smite, distribute Temporary Hit Points to nearby creatures."),
                f(3, "Oath of Glory Spells", "You always have Guiding Bolt, Heroism, Enhance Ability, Magic Weapon, Haste, and more prepared."),
                f(7, "Aura of Alacrity", "Your Speed increases by 10 feet, and allies who come near gain the same bonus."),
                f(15, "Glorious Defense", "As a Reaction, add your Charisma modifier to a target's AC and counterattack."),
                f(20, "Living Legend", "Gain Charisma-based Advantage on checks, always-hit fallback on missed attacks, and self-revival."),
            )),
        Subclass("ancients", "paladin", "Oath of the Ancients",
            "A green knight defending light, life, and joy against the dark.",
            listOf(
                f(3, "Nature's Wrath", "Spend Channel Divinity to Restrain creatures with spectral vines."),
                f(3, "Oath of the Ancients Spells", "You always have Ensnaring Strike, Speak with Animals, Moonbeam, Plant Growth, and more prepared."),
                f(7, "Aura of Warding", "You and allies within 10 feet have Resistance to damage from spells."),
                f(15, "Undying Sentinel", "When reduced to 0 hit points you can drop to 1 instead, once per Long Rest."),
                f(20, "Elder Champion", "Assume a primal form that regenerates you and speeds your spellcasting."),
            )),
        Subclass("vengeance", "paladin", "Oath of Vengeance",
            "A relentless avenger who hunts a chosen foe to the end.",
            listOf(
                f(3, "Vow of Enmity", "Spend Channel Divinity as a Bonus Action to gain Advantage on attacks against one creature."),
                f(3, "Oath of Vengeance Spells", "You always have Bane, Hunter's Mark, Hold Person, Misty Step, Haste, and more prepared."),
                f(7, "Relentless Avenger", "After an Opportunity Attack hits, you can move up to half your Speed without provoking."),
                f(15, "Soul of Vengeance", "As a Reaction, attack a creature under your Vow of Enmity when it attacks."),
                f(20, "Avenging Angel", "Sprout wings, gain a Fly Speed, and Frighten creatures that come near you."),
            )),

        // ================================================================= Ranger
        Subclass("beast_master", "ranger", "Beast Master",
            "Bond with a primal companion that fights at your side.",
            listOf(
                f(3, "Primal Companion", "Summon a Beast of the Land, Sea, or Sky that acts on your turn.",
                    Choice("primal_companion", "Primal Companion", "Choose your companion's form.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("land", "Beast of the Land", "A sturdy companion with a charging attack that can knock foes Prone."),
                        ChoiceOption("sea", "Beast of the Sea", "An aquatic companion with a Swim Speed and a grappling bite."),
                        ChoiceOption("sky", "Beast of the Sky", "A flying companion, fast and evasive but fragile."),
                    ), "Level 3")),
                f(7, "Exceptional Training", "Your companion's attacks are magical and it can Dash, Disengage, Dodge, or Help as a Bonus Action."),
                f(11, "Bestial Fury", "Your companion makes two attacks when it takes the Attack action."),
                f(15, "Share Spells", "When you target yourself with a spell, you can also target your companion."),
            )),
        Subclass("fey_wanderer", "ranger", "Fey Wanderer",
            "A ranger touched by the Feywild, charming and unsettling by turns.",
            listOf(
                f(3, "Dreadful Strikes", "Your weapon attacks deal an extra 1d4 Psychic damage once per turn per target."),
                f(3, "Otherworldly Glamour", "Add your Wisdom modifier to Charisma checks and gain a Charisma skill proficiency.",
                    Choice("fey_glamour_skill", "Otherworldly Glamour", "Choose a Charisma skill.", 1, ChoiceKind.SKILL,
                        ChoiceOptions.fromSkills(listOf(Skill.DECEPTION, Skill.INTIMIDATION, Skill.PERFORMANCE, Skill.PERSUASION)), "Level 3")),
                f(3, "Fey Wanderer Spells", "You always have Charm Person, Misty Step, Dispel Magic, Summon Fey, and more prepared."),
                f(7, "Beguiling Twist", "You have Advantage on saves against being Charmed or Frightened and can redirect such effects."),
                f(11, "Fey Reinforcements", "You always have Summon Fey prepared and can cast it once per Long Rest without a slot."),
                f(15, "Misty Wanderer", "You can cast Misty Step without a slot a number of times per Long Rest, bringing an ally along."),
            )),
        Subclass("gloom_stalker", "ranger", "Gloom Stalker",
            "A hunter of the dark places, deadly in the first moments of a fight.",
            listOf(
                f(3, "Dread Ambusher", "Gain bonus Initiative, extra Speed, and an extra attack on your first turn of combat."),
                f(3, "Umbral Sight", "You gain Darkvision out to 60 feet and are Invisible to creatures relying on Darkvision."),
                f(3, "Gloom Stalker Spells", "You always have Disguise Self, Rope Trick, Fear, Greater Invisibility, and more prepared."),
                f(7, "Iron Mind", "You gain proficiency in Wisdom saving throws."),
                f(11, "Stalker's Flurry", "When you miss with an attack, you can make another attack or force a save."),
                f(15, "Shadowy Dodge", "As a Reaction when attacked, impose Disadvantage and teleport up to 30 feet."),
            )),
        Subclass("hunter", "ranger", "Hunter",
            "A specialist trained against the specific threats you face.",
            listOf(
                f(3, "Hunter's Lore", "Learn a creature's Immunities, Resistances, and Vulnerabilities when you mark it."),
                f(3, "Hunter's Prey", "Choose a signature technique against your prey.",
                    Choice("hunters_prey", "Hunter's Prey", "Choose your hunting technique.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("colossus_slayer", "Colossus Slayer", "Deal an extra 1d8 damage to a creature missing hit points, once per turn."),
                        ChoiceOption("horde_breaker", "Horde Breaker", "Once per turn, attack a second creature near your first target."),
                    ), "Level 3")),
                f(7, "Defensive Tactics", "Choose a defensive technique.",
                    Choice("defensive_tactics", "Defensive Tactics", "Choose a defensive technique.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("escape_horde", "Escape the Horde", "Opportunity Attacks against you have Disadvantage."),
                        ChoiceOption("multiattack_defense", "Multiattack Defense", "Gain +4 AC against a creature's follow-up attacks."),
                    ), "Level 7")),
                f(11, "Multiattack", "Choose Volley or Whirlwind Attack to strike many foes at once.",
                    Choice("hunter_multiattack", "Multiattack", "Choose a multiattack technique.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("volley", "Volley", "Make a ranged attack against any number of creatures in a 10-foot radius."),
                        ChoiceOption("whirlwind", "Whirlwind Attack", "Make a melee attack against any number of creatures within 5 feet."),
                    ), "Level 11")),
                f(15, "Superior Hunter's Defense", "You can halve the damage of an attack against you as a Reaction."),
            )),

        // ================================================================= Rogue
        Subclass("arcane_trickster", "rogue", "Arcane Trickster",
            "A thief who supplements sleight of hand with illusion and enchantment.",
            listOf(
                f(3, "Spellcasting", "You have learned to cast spells. See the Player's Handbook for the rules on spellcasting. Cantrips: you know Mage Hand plus two other Wizard cantrips of your choice, and you learn another at Rogue level 10. Whenever you gain a Rogue level, you can replace one of those chosen cantrips with another Wizard cantrip. Spell Slots and Prepared Spells: the Arcane Trickster Spellcasting table shows how many spell slots you have and how many level 1+ Wizard spells you can have prepared; those spells must be Illusion or Enchantment, except that the spells you gain at Rogue levels 8, 14, and 20 can be from any school. Whenever you gain a Rogue level, you can replace one prepared spell with another Wizard spell of an eligible school and level. Intelligence is your spellcasting ability."),
                f(3, "Mage Hand Legerdemain", "Your Mage Hand is invisible and can pick locks, pockets, and disarm traps."),
                f(9, "Magical Ambush", "A creature you are Hidden from has Disadvantage on saves against your spells."),
                f(13, "Versatile Trickster", "As a Bonus Action, use Mage Hand to distract a creature and gain Advantage."),
                f(17, "Spell Thief", "As a Reaction, steal a spell cast at you and use it yourself for 8 hours."),
            ),
            casterType = CasterType.THIRD,
            spellcastingAbility = Ability.INT,
            spellListClassId = "wizard",
            // Three cantrips, but one of them is Mage Hand, which the subclass hands over
            // rather than offering — so only two are chosen. The grant lives in SpellGrantData.
            cantripsKnown = mapOf(3 to 3, 10 to 4),
            preparedSpells = THIRD_CASTER_PREPARED,
            spellSchools = setOf("Illusion", "Enchantment"),
            freeSchoolLevels = setOf(8, 14, 20)),
        Subclass("assassin", "rogue", "Assassin",
            "Strike first, strike unseen, strike to kill.",
            listOf(
                f(3, "Assassinate", "You have Advantage on attacks against creatures that haven't taken a turn, and deal extra damage."),
                f(3, "Assassin's Tools", "You gain a Disguise Kit and Poisoner's Kit and proficiency with both."),
                f(9, "Infiltration Expertise", "You can create false identities and mimic speech and mannerisms convincingly."),
                f(13, "Envenom Weapons", "Your Poison Cunning Strike deals extra damage and leaves the target Poisoned."),
                f(17, "Death Strike", "On your first turn, a hit forces a Constitution save or the damage is doubled."),
            )),
        Subclass("soulknife", "rogue", "Soulknife",
            "Psionic blades of pure thought, silent and untraceable.",
            listOf(
                f(3, "Psionic Power", "You gain Psionic Energy Dice fuelling Psi-Bolstered Knack and Psychic Whispers."),
                f(3, "Psychic Blades", "Manifest shimmering blades that deal Psychic damage and can be thrown."),
                f(9, "Soul Blades", "Your blades gain Homing Strikes and Psychic Teleportation."),
                f(13, "Psychic Veil", "As a Magic action, become Invisible for an hour without Concentration."),
                f(17, "Rend Mind", "When you Sneak Attack with a Psychic Blade, force a Wisdom save or Stun the target."),
            )),
        Subclass("thief", "rogue", "Thief",
            "Nimble hands, quick escapes, and a knack for other people's magic items.",
            listOf(
                f(3, "Fast Hands", "Use Cunning Action to Sleight of Hand, use Thieves' Tools, or take the Utilize action."),
                f(3, "Second-Story Work", "You gain a Climb Speed and can jump further using Dexterity."),
                f(9, "Supreme Sneak", "Using Cunning Action to Hide grants Advantage on the Stealth check and lets you move further."),
                f(13, "Use Magic Device", "You can attune to more items, use any Spell Scroll, and reroll charged item uses."),
                f(17, "Thief's Reflexes", "You can take two turns during the first round of combat."),
            )),

        // ================================================================= Sorcerer
        Subclass("aberrant", "sorcerer", "Aberrant Sorcery",
            "An alien influence took root in your mind and left psionic power behind.",
            listOf(
                f(3, "Psionic Spells", "You always have Arms of Hadar, Dissonant Whispers, Calm Emotions, Detect Thoughts, and more prepared."),
                f(3, "Telepathic Speech", "Form a telepathic link with a creature you can see for a number of minutes equal to your level."),
                f(6, "Psionic Sorcery", "Cast your Psionic Spells using Sorcery Points instead of slots, without Verbal or Somatic components."),
                f(6, "Psychic Defenses", "You have Resistance to Psychic damage and Advantage on saves against being Charmed or Frightened."),
                f(14, "Revelation in Flesh", "Spend Sorcery Points to gain a Fly or Swim Speed, see invisibility, or squeeze through tiny spaces."),
                f(18, "Warping Implosion", "Teleport 120 feet and pull creatures toward your origin point for 3d10 Force damage."),
            )),
        Subclass("clockwork", "sorcerer", "Clockwork Sorcery",
            "Order itself flows through you, smoothing chaos into predictable outcomes.",
            listOf(
                f(3, "Clockwork Spells", "You always have Aid, Alarm, Lesser Restoration, Protection from Evil and Good, and more prepared."),
                f(3, "Restore Balance", "As a Reaction, cancel Advantage or Disadvantage on a creature's d20 Test."),
                f(6, "Bastion of Law", "Spend Sorcery Points to give a creature d8 dice that absorb damage."),
                f(14, "Trance of Order", "Treat d20 rolls of 9 or lower as 10 for attacks and checks, and negate Advantage against you."),
                f(18, "Clockwork Cavalcade", "Summon a 30-foot cube of spirits that heals, repairs objects, and ends spells."),
            )),
        Subclass("draconic", "sorcerer", "Draconic Sorcery",
            "Dragon blood grants resilience, wings, and elemental mastery.",
            listOf(
                f(3, "Draconic Resilience", "Your hit point maximum increases and your base AC becomes 10 + Dexterity + Charisma."),
                f(3, "Draconic Spells", "You always have Alter Self, Chromatic Orb, Command, Fear, Fly, and more prepared."),
                f(6, "Elemental Affinity", "Choose a damage type tied to your ancestry; you gain Resistance and bonus spell damage.",
                    damageTypeChoice("draconic_element", "Draconic Ancestry", "Level 6",
                        listOf("Acid", "Cold", "Fire", "Lightning", "Poison"))),
                f(14, "Dragon Wings", "As a Bonus Action, sprout wings and gain a Fly Speed equal to your Speed."),
                f(18, "Dragon Companion", "Cast Summon Dragon once per Long Rest without a slot, and command it more freely."),
            )),
        Subclass("wild_magic", "sorcerer", "Wild Magic Sorcery",
            "Raw chaos surges through your spells, for good or ill.",
            listOf(
                f(3, "Wild Magic Surge", "Your spellcasting can trigger a random magical effect from the Wild Magic table."),
                f(3, "Tides of Chaos", "Gain Advantage on a d20 Test, then trigger a Surge to regain the use."),
                f(6, "Bend Luck", "As a Reaction, spend Sorcery Points to add or subtract 1d4 from another creature's roll."),
                f(14, "Controlled Chaos", "When you roll on the Wild Magic table, roll twice and choose either result."),
                f(18, "Tamed Surge", "After casting a spell, choose a Wild Magic effect deliberately instead of rolling."),
            )),

        // ================================================================= Warlock
        Subclass("archfey", "warlock", "Archfey Patron",
            "A fey lord's bargain grants teleportation and beguiling defenses.",
            listOf(
                f(3, "Steps of the Fey", "Cast Misty Step without a slot, adding a Refreshing Step or Taunting Step benefit."),
                f(3, "Archfey Spells", "You always have Calm Emotions, Faerie Fire, Sleep, Blink, Dominate Beast, and more prepared."),
                f(6, "Misty Escape", "As a Reaction when damaged, become Invisible and teleport with Misty Step."),
                f(10, "Beguiling Defenses", "You are immune to the Charmed condition and can turn such attempts back on the caster."),
                f(14, "Bewitching Magic", "After casting an Enchantment or Illusion spell, cast Misty Step as a Bonus Action."),
            )),
        Subclass("celestial", "warlock", "Celestial Patron",
            "A being of the Upper Planes lends you healing radiance.",
            listOf(
                f(3, "Healing Light", "You have a pool of d6s you can spend as a Bonus Action to heal creatures."),
                f(3, "Celestial Spells", "You always have Cure Wounds, Guiding Bolt, Lesser Restoration, Daylight, Flame Strike, and more prepared."),
                f(6, "Radiant Soul", "You have Resistance to Radiant damage and add your Charisma modifier to Radiant or Fire spell damage."),
                f(10, "Celestial Resilience", "You and your allies gain Temporary Hit Points whenever you finish a rest."),
                f(14, "Searing Vengeance", "When you would make a death save, rise with Radiant damage bursting around you."),
            )),
        Subclass("fiend", "warlock", "Fiend Patron",
            "An infernal bargain fuels you with the strength of the Lower Planes.",
            listOf(
                f(3, "Dark One's Blessing", "When you reduce a creature to 0 hit points, gain Temporary Hit Points."),
                f(3, "Fiend Spells", "You always have Burning Hands, Command, Scorching Ray, Fireball, Wall of Fire, and more prepared."),
                f(6, "Dark One's Own Luck", "Add 1d10 to an ability check or saving throw, once per Short or Long Rest."),
                f(10, "Fiendish Resilience", "Choose a damage type to resist after each rest.",
                    damageTypeChoice("fiendish_resilience", "Fiendish Resilience", "Level 10",
                        listOf("Acid", "Cold", "Fire", "Lightning", "Necrotic", "Poison", "Psychic", "Radiant", "Thunder"), changeableOnRest = true)),
                f(14, "Hurl Through Hell", "Once per Long Rest, banish a creature you hit through the Lower Planes for 8d10 Psychic damage."),
            )),
        Subclass("great_old_one", "warlock", "Great Old One Patron",
            "A distant, unknowable entity leaves psychic power in its wake.",
            listOf(
                f(3, "Awakened Mind", "You gain telepathy with creatures within 30 feet."),
                f(3, "Psychic Spells", "You always have Detect Thoughts, Dissonant Whispers, Phantasmal Force, Clairvoyance, and more prepared."),
                f(6, "Clairvoyant Combatant", "Give a creature Disadvantage on attacks and saves against you through your telepathic bond."),
                f(10, "Eldritch Hex", "You always have Hex prepared and the target has Disadvantage on saves with the cursed ability."),
                f(14, "Create Thrall", "Cast Summon Aberration once per Long Rest without a slot, with a psychic bond."),
            )),

        // ================================================================= Wizard
        Subclass("abjurer", "wizard", "Abjurer",
            "Protective magic woven into a self-renewing ward.",
            listOf(
                f(3, "Abjuration Savant", "Add two Abjuration spells to your spellbook free, and one more at each new spell level."),
                f(3, "Arcane Ward", "Casting Abjuration spells builds a ward that absorbs damage before your hit points."),
                f(6, "Projected Ward", "As a Reaction, use your Arcane Ward to absorb damage aimed at a nearby creature."),
                f(10, "Spell Breaker", "You always have Counterspell and Dispel Magic prepared and cast them more effectively."),
                f(14, "Spell Resistance", "You have Advantage on saves against spells and Resistance to their damage."),
            )),
        Subclass("diviner", "wizard", "Diviner",
            "Glimpse the future and replace fate's rolls with your own.",
            listOf(
                f(3, "Divination Savant", "Add two Divination spells to your spellbook free, and one more at each new spell level."),
                f(3, "Portent", "After a Long Rest, roll two d20s and replace any attack, check, or save with those results."),
                f(6, "Expert Divination", "Casting a Divination spell of level 2 or higher regains a lower-level spell slot."),
                f(10, "The Third Eye", "As a Bonus Action, gain Darkvision, Ethereal Sight, Greater Comprehension, or See Invisibility."),
                f(14, "Greater Portent", "You roll three Portent dice after each Long Rest."),
            )),
        Subclass("evoker", "wizard", "Evoker",
            "Shape raw elemental force, sparing allies from your own blasts.",
            listOf(
                f(3, "Evocation Savant", "Add two Evocation spells to your spellbook free, and one more at each new spell level."),
                f(3, "Potent Cantrip", "Creatures that succeed on saves against your cantrips still take half damage."),
                f(6, "Sculpt Spells", "Choose allies to automatically succeed on saves against your Evocation spells."),
                f(10, "Empowered Evocation", "Add your Intelligence modifier to one damage roll of any Evocation spell."),
                f(14, "Overchannel", "Deal maximum damage with a spell of level 5 or lower, at increasing cost to yourself."),
            )),
        Subclass("illusionist", "wizard", "Illusionist",
            "Illusions so convincing they begin to bleed into the real.",
            listOf(
                f(3, "Illusion Savant", "Add two Illusion spells to your spellbook free, and one more at each new spell level."),
                f(3, "Improved Illusions", "Cast Illusion spells without Verbal components and extend Minor Illusion's range and versatility."),
                f(6, "Phantasmal Creatures", "You always have Summon Beast and Summon Fey prepared and can cast them as Illusions without a slot."),
                f(10, "Illusory Self", "As a Reaction when hit, interpose an illusory duplicate and cause the attack to miss."),
                f(14, "Illusory Reality", "Make one object within an illusion real for a minute."),
            )),
        Subclass("conjurer", "wizard", "Conjurer",
            "Step across space and call creatures from thin air.",
            listOf(
                f(3, "Benign Transposition", "As a Bonus Action, teleport up to 30 feet or swap places with a willing Medium or smaller creature. Uses equal your Intelligence modifier per Long Rest."),
                f(3, "Conjuration Savant", "Add two Conjuration spells of level 2 or lower to your spellbook free, and one more at each new spell level."),
                f(6, "Distant Transposition", "Benign Transposition reaches 60 feet and recharges on a Short or Long Rest."),
                f(6, "Durable Summons", "Creatures you summon gain Temporary Hit Points equal to twice your Wizard level and broad damage Resistance."),
                f(10, "Focused Conjuration", "Taking damage can't break your Concentration on Conjuration spells."),
                f(14, "Splintered Summons", "Summon Aberration, Construct, Dragon, Elemental, or Fey spells summon two creatures instead of one."),
            ),
            book = UA),
        Subclass("enchanter", "wizard", "Enchanter",
            "Entrance and beguile others with magic that clouds the mind.",
            listOf(
                f(3, "Enchanting Conversationalist", "Gain a social skill proficiency and add your Intelligence modifier to checks with it.",
                    Choice("enchanting_skill", "Enchanting Conversationalist", "Choose a social skill.", 1, ChoiceKind.SKILL,
                        ChoiceOptions.fromSkills(listOf(Skill.DECEPTION, Skill.INTIMIDATION, Skill.PERSUASION)), "Level 3")),
                f(3, "Enchantment Savant", "Add two Enchantment spells of level 2 or lower to your spellbook free, and one more at each new spell level."),
                f(3, "Hypnotic Presence", "As a Magic action, Charm a creature within 10 feet, leaving it Incapacitated with a Speed of 0."),
                f(6, "Split Enchantment", "Raise an Enchantment spell's effective level by 1 to target an additional creature."),
                f(10, "Instinctive Charm", "As a Reaction, force an attacker to miss and possibly redirect the attack at another creature."),
                f(14, "Alter Memories", "You always have Modify Memory prepared and can target a second creature with it."),
            ),
            book = UA),
        Subclass("necromancer", "wizard", "Necromancer",
            "Command the powers of death and undeath.",
            listOf(
                f(3, "Necromancy Savant", "Add two Necromancy spells of level 2 or lower to your spellbook free, and one more at each new spell level."),
                f(3, "Necromancy Spellbook", "You gain Resistance to Necrotic damage, Grim Harvest healing for your Undead, and an Undead Familiar option."),
                f(6, "Grave Power", "Arcane Recovery also reduces your Exhaustion, and your spell damage ignores Necrotic Resistance."),
                f(6, "Undead Thralls", "You always have Animate Dead prepared, cast it free once, and your Undead gain bonus hit points and Necrotic strikes."),
                f(10, "Harvest Undead", "When you become Bloodied, take a Reaction to destroy an Undead you control and regain hit points."),
                f(14, "Death's Master", "Bolster your Undead with Temporary Hit Points, and make dying Undead explode with necrotic energy."),
            ),
            book = UA),
        Subclass("transmuter", "wizard", "Transmuter",
            "Transform energy and matter at reality's forge.",
            listOf(
                f(3, "Transmutation Savant", "Add two Transmutation spells of level 2 or lower to your spellbook free, and one more at each new spell level."),
                f(3, "Transmuter's Stone", "Create a stone that grants Constitution save proficiency plus a benefit you choose.",
                    Choice("transmuters_stone", "Transmuter's Stone", "Choose the stone's benefit.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("darkvision", "Darkvision", "The bearer gains or extends Darkvision by 60 feet."),
                        ChoiceOption("speed", "Speed", "The bearer's Speed increases by 10 feet."),
                        ChoiceOption("resistance", "Resistance", "Resistance to Acid, Cold, Fire, Lightning, Poison, or Thunder damage."),
                    ), "Level 3", changeableOnRest = true)),
                f(3, "Wondrous Alteration", "You always have Alter Self prepared, cast it free once per Long Rest, and each of its options gains a bonus."),
                f(6, "Empowered Transmutation", "Cast a non-damaging Transmutation spell as if using a slot one level higher."),
                f(10, "Potent Stone", "Your Transmuter's Stone grants two benefits, adding Mighty Build and Tremorsense as options."),
                f(10, "Shapechanger", "You always have Polymorph prepared, cast it free once per Long Rest, and keep your mind and spells when targeting yourself."),
                f(14, "Master Transmuter", "Consume the stone for Major Transformation, Panacea, Restore Life, or Restore Youth."),
            ),
            book = UA),

        // ================================================================= Artificer

        Subclass("alchemist", "artificer", "Alchemist",
            "An expert at combining reagents to produce magical effects, giving life and leeching it away.",
            listOf(
                f(3, "Tools of the Trade", "You gain proficiency with Alchemist's Supplies and the Herbalism Kit; if you already have one, you gain another type of Artisan's Tools instead (or two other types if you have both). Brewing a potion also takes half the usual time."),
                f(3, "Alchemist Spells", "You always have these prepared once you reach the listed Artificer level: Healing Word and Ray of Sickness at 3, Flaming Sphere and Melf's Acid Arrow at 5, Gaseous Form and Mass Healing Word at 9, Death Ward and Vitriolic Sphere at 13, and Cloudkill and Raise Dead at 17."),
                f(3, "Experimental Elixir", "Whenever you finish a Long Rest while holding Alchemist's Supplies, you magically produce two elixirs, rolling on the Experimental Elixir table for each. A creature can drink one or administer it to another creature within 5 feet as a Bonus Action. As a Magic action you can expend a spell slot to create another elixir, choosing its effect rather than rolling. You make an additional elixir per Long Rest at Artificer levels 5, 9, and 15."),
                f(5, "Alchemical Savant", "Whenever you cast a spell using your Alchemist's Supplies as the Spellcasting Focus, you add your Intelligence modifier (minimum of +1) to one roll of the spell that restores Hit Points or deals Acid, Fire, or Poison damage."),
                f(9, "Restorative Reagents", "You can cast Lesser Restoration without a spell slot and without preparing it, provided you use Alchemist's Supplies as the Spellcasting Focus. You can do so a number of times equal to your Intelligence modifier (minimum of once) per Long Rest."),
                f(15, "Chemical Mastery", "Alchemical Eruption adds 2d8 Force damage once per turn to an Artificer spell that deals Acid, Fire, or Poison damage. Chemical Resistance grants Resistance to Acid and Poison damage and Immunity to the Poisoned condition. Conjured Cauldron lets you cast Tasha's Bubbling Cauldron free once per Long Rest."),
            ),
            book = EFOTA),

        Subclass("armorer", "artificer", "Armorer",
            "You modify armor until it works like a second skin, honing your magic and unleashing potent attacks.",
            listOf(
                f(3, "Tools of the Trade", "You gain training with Heavy armor and proficiency with Smith's Tools; if you already have the tool proficiency, you gain another type of Artisan's Tools instead. Crafting armor also takes half the usual time."),
                f(3, "Armorer Spells", "You always have these prepared once you reach the listed Artificer level: Magic Missile and Thunderwave at 3, Mirror Image and Shatter at 5, Hypnotic Pattern and Lightning Bolt at 9, Fire Shield and Greater Invisibility at 13, and Passwall and Wall of Force at 17."),
                f(3, "Arcane Armor", "As a Magic action while holding Smith's Tools, you turn a suit of armor you are wearing into Arcane Armor, which it remains until you don other armor or die. The armor has no Strength requirement for you, can be donned or doffed as a Utilize action and never removed against your will, and serves as a Spellcasting Focus for your Artificer spells."),
                f(3, "Armor Model", "You customize your Arcane Armor into one of three models, each with a special weapon you can attack with using Intelligence instead of Strength or Dexterity. You can change the model whenever you finish a Short or Long Rest with Smith's Tools in hand.",
                    armorModelChoice()),
                f(5, "Extra Attack", "You can attack twice instead of once whenever you take the Attack action on your turn."),
                f(9, "Improved Armorer", "Armor Replication grants an additional Replicate Magic Item plan that must be in the Armor category, plus an extra item created from it. Improved Arsenal gives a +1 bonus to attack and damage rolls with your armor model's special weapon."),
                f(15, "Perfected Armor", "Dreadnaught's Force Demolisher rises to 2d6, and Giant Stature extends your reach by 10 feet and can make you Huge with Advantage on Strength checks and saves. Guardian's Thunder Pulse rises to 1d10, and you can pull a Huge or smaller creature 25 feet toward you as a Reaction and attack it. Infiltrator's Lightning Launcher rises to 2d6, makes targets glimmer with revealing light, and grants a Bonus Action Fly Speed of twice your Speed."),
            ),
            book = EFOTA),

        Subclass("artillerist", "artificer", "Artillerist",
            "A specialist in hurling energy, projectiles, and explosions across the battlefield.",
            listOf(
                f(3, "Tools of the Trade", "You gain proficiency with Martial Ranged weapons and with Woodcarver's Tools; if you already have the tool proficiency, you gain another type of Artisan's Tools instead. Crafting a magic Wand also takes half the usual time."),
                f(3, "Artillerist Spells", "You always have these prepared once you reach the listed Artificer level: Shield and Thunderwave at 3, Scorching Ray and Shatter at 5, Fireball and Wind Wall at 9, Ice Storm and Wall of Fire at 13, and Cone of Cold and Wall of Force at 17."),
                f(3, "Eldritch Cannon", "Using Smith's Tools or Woodcarver's Tools you can take a Magic action to create a Small or Tiny Eldritch Cannon within 5 feet of yourself. It has AC 18 and Hit Points equal to five times your Artificer level, and it disappears at 0 Hit Points or after 1 hour. Once you create a cannon you can't do so again until you finish a Long Rest or expend a spell slot, and you can have only one at a time. You choose the cannon's mode — Flamethrower, Force Ballista, or Protector — each time you activate it as a Bonus Action."),
                f(5, "Arcane Firearm", "When you finish a Long Rest you can carve sigils into a Rod, Staff, Wand, or Martial Ranged weapon with Woodcarver's Tools, making it your Arcane Firearm. You can use it as a Spellcasting Focus, and when you cast an Artificer spell through it you roll 1d8 and add the result to one of the spell's damage rolls."),
                f(9, "Explosive Cannon", "Detonate lets you take a Reaction when your cannon takes damage to destroy it, forcing each creature within 20 feet to make a Dexterity saving throw for 3d10 Force damage, or half as much on a success. Firepower increases the cannon's damage rolls and its Protector Temporary Hit Points by 1d8."),
                f(15, "Fortified Position", "Double Firepower lets you have two cannons at once, create both with the same Magic action, and activate both with the same Bonus Action. Shimmering Field Projection gives you and your allies Half Cover while within 10 feet of a cannon."),
            ),
            book = EFOTA),

        Subclass("battle_smith", "artificer", "Battle Smith",
            "A protector and medic who repairs both materiel and personnel, aided by a Steel Defender of their own making.",
            listOf(
                f(3, "Tools of the Trade", "You gain proficiency with Smith's Tools; if you already have it, you gain another type of Artisan's Tools instead. Crafting a weapon also takes half the usual time."),
                f(3, "Battle Smith Spells", "You always have these prepared once you reach the listed Artificer level: Heroism and Shield at 3, Shining Smite and Warding Bond at 5, Aura of Vitality and Conjure Barrage at 9, Aura of Purity and Fire Shield at 13, and Banishing Smite and Mass Cure Wounds at 17."),
                f(3, "Battle Ready", "Arcane Empowerment lets you use your Intelligence modifier instead of Strength or Dexterity for attack and damage rolls with a magic weapon. Weapon Knowledge grants proficiency with Martial weapons, and any weapon you're proficient with can serve as a Spellcasting Focus for your Artificer spells."),
                f(3, "Steel Defender", "Your tinkering produces a Medium Construct companion with AC 12 plus your Intelligence modifier and Hit Points equal to 5 plus five times your Artificer level. It is Friendly to you and your allies, obeys you, and vanishes if you die. In combat it acts on your turn, taking only the Dodge action unless you spend a Bonus Action to command it. Its Force-Empowered Rend deals 1d8 + 2 plus your Intelligence modifier Force damage, it can Repair three times per day, and its Deflect Attack Reaction imposes Disadvantage on an attack against another creature."),
                f(5, "Extra Attack", "You can attack twice instead of once whenever you take the Attack action on your turn, and you can forgo one of those attacks to command your Steel Defender to use Force-Empowered Rend."),
                f(9, "Arcane Jolt", "When you hit with a magic weapon or your Steel Defender hits a target, you can channel magic through the strike, choosing Destructive Energy or Restorative Energy each time."),
                f(15, "Improved Defender", "Improved Jolt raises both the extra damage and the healing of Arcane Jolt to 4d6. Improved Deflection makes the attacker take 1d4 plus your Intelligence modifier Force damage whenever your Steel Defender uses Deflect Attack."),
            ),
            book = EFOTA),

        Subclass("cartographer", "artificer", "Cartographer",
            "A navigator and reconnaissance agent who highlights threats, safeguards allies, and carves portals to distant places.",
            listOf(
                f(3, "Tools of the Trade", "You gain proficiency with Calligrapher's Supplies and Cartographer's Tools; if you already have one, you gain another type of Artisan's Tools instead (or two other types if you have both). Scribing a Spell Scroll also takes half the usual time."),
                f(3, "Cartographer Spells", "You always have these prepared once you reach the listed Artificer level: Faerie Fire, Guiding Bolt, and Healing Word at 3, Locate Object and Mind Spike at 5, Call Lightning and Clairvoyance at 9, Banishment and Locate Creature at 13, and Scrying and Teleportation Circle at 17."),
                f(3, "Adventurer's Atlas", "Whenever you finish a Long Rest while holding Cartographer's Tools, you touch at least two creatures — up to 1 plus your Intelligence modifier — and give each a magical map that constantly updates to show the others' positions and is illegible to anyone else. A map holder adds 1d4 to Initiative rolls, always knows where the other holders are on its plane, and can target another holder with a spell regardless of sight or cover so long as the target is in range. The maps last until you die or use this feature again."),
                f(3, "Mapping Magic", "Illuminated Cartography lets you cast Faerie Fire without a spell slot a number of times equal to your Intelligence modifier (minimum of once) per Long Rest, outlining the affected creatures as if in ink. Portal Jump lets you spend half your Speed on your turn to teleport to an unoccupied space you can see within 10 feet of yourself, or within 5 feet of a creature within 30 feet that holds one of your maps."),
                f(5, "Guided Precision", "Once per turn, when you cast a spell from your Cartographer Spells list or hit a creature affected by your Faerie Fire with an attack roll, you can add your Intelligence modifier to one damage roll of the spell or attack. Taking damage also can't break your Concentration on Faerie Fire."),
                f(9, "Ingenious Movement", "When you use your Flash of Genius, you or a willing creature you can see within 30 feet can teleport up to 30 feet to an unoccupied space you can see as part of that same Reaction."),
                f(15, "Superior Atlas", "Safe Haven lets a map holder reduced to 0 Hit Points but not killed outright destroy its map, setting its Hit Points to twice your Artificer level and teleporting it within 5 feet of you or another holder. Unerring Path lets you cast Find the Path free once per Long Rest while you hold one of the maps."),
            ),
            book = EFOTA),

        // ============================================ Forgotten Realms: Heroes of Faerûn

        Subclass("college_of_the_moon", "bard", "College of the Moon",
            "A Bard trained by the druids of the Moonshae Isles, drawing on moonwells and local folktales.",
            listOf(
                f(3, "Moon's Inspiration", "Inspired Eclipse: when you take a Bonus Action to give a creature a Bardic Inspiration die, you can gain the Invisible condition and teleport up to 30 feet to an unoccupied space you can see as part of that Bonus Action; the invisibility lasts until the start of your next turn and ends early when you make an attack roll, deal damage, or cast a spell. Lunar Vitality: once per turn when you restore Hit Points with a spell, you can expend a Bardic Inspiration die to increase the Hit Points restored by a roll of the die, and the creature's Speed also increases by 10 feet until the end of its next turn."),
                f(3, "Primal Lore", "You learn Druidic and one cantrip from the Druid spell list, which counts as a Bard spell for you but doesn't count against your cantrips known. You can replace that cantrip whenever you gain a Bard level.",
                    Choice("moon_primal_skill", "Primal Lore", "Choose a skill you gain proficiency in.", 1, ChoiceKind.SKILL,
                        ChoiceOptions.fromSkills(listOf(Skill.ANIMAL_HANDLING, Skill.INSIGHT, Skill.MEDICINE, Skill.NATURE, Skill.PERCEPTION, Skill.SURVIVAL)), "Level 3")),
                f(6, "Blessing of Moonlight", "You always have the Moonbeam spell prepared. When you cast it you can modify the spell so that you glow faintly while it is active, shedding Dim Light out to 5 feet; whenever a creature fails its saving throw against that Moonbeam, another creature of your choice within 60 feet regains 2d4 Hit Points. Once you modify a casting this way, you can't do so again until you finish a Long Rest."),
                f(14, "Eventide's Splendor", "Shadow of the New Moon: when you use Inspired Eclipse, the creature who received the Bardic Inspiration die can also gain the Invisible condition and immediately take a Reaction to teleport up to 30 feet to an unoccupied space it can see, remaining Invisible until the start of its next turn. Vibrance of the Full Moon: when you use Lunar Vitality, you can roll 1d6 and use the number rolled in place of expending a Bardic Inspiration die."),
            ),
            book = HOF),

        Subclass("knowledge_domain", "cleric", "Knowledge Domain",
            "A Cleric who values learning above all, unearthing secrets and mastering the mind.",
            listOf(
                f(3, "Blessings of Knowledge", "You gain proficiency with one type of Artisan's Tools of your choice, and Expertise in two of the listed skills.",
                    Choice("knowledge_expertise", "Blessings of Knowledge", "Choose two skills to gain proficiency and Expertise in.", 2, ChoiceKind.EXPERTISE,
                        ChoiceOptions.fromSkills(listOf(Skill.ARCANA, Skill.HISTORY, Skill.NATURE, Skill.RELIGION)), "Level 3")),
                f(3, "Knowledge Domain Spells", "You always have these prepared once you reach the listed Cleric level: Command, Comprehend Languages, Detect Magic, Detect Thoughts, Identify, and Mind Spike at 3; Dispel Magic, Nondetection, and Tongues at 5; Arcane Eye, Banishment, and Confusion at 7; and Legend Lore, Scrying, and Synaptic Static at 9."),
                f(3, "Mind Magic", "As a Magic action, you can expend one use of your Channel Divinity to manifest your magical knowledge. Choose one spell from the Divination school on the Knowledge Domain Spells table that you have prepared. As part of that action, you cast that spell without expending a spell slot or needing Material components."),
                f(6, "Unfettered Mind", "You gain telepathy out to 50 feet, and when you use this telepathy you can simultaneously contact a number of creatures equal to your Wisdom modifier (minimum of one). You also gain proficiency in Intelligence saving throws, or in one ability you lack if you already have it."),
                f(17, "Divine Foreknowledge", "As a Bonus Action, you magically expand your mind into the future. For 1 hour, you have Advantage on D20 Tests. Once you use this feature you can't use it again until you finish a Long Rest, though you can also restore it by expending a level 6+ spell slot (no action required)."),
            ),
            book = HOF),

        Subclass("banneret", "fighter", "Banneret",
            "A paragon of valor and leadership who rallies fellow adventurers to the causes of justice and freedom.",
            listOf(
                f(3, "Knightly Envoy", "Comprehension: you can cast Comprehend Languages, but only as a Ritual, using Charisma as your spellcasting ability. Polyglot: you learn one language, and whenever you finish a Long Rest you can replace it with another language you have heard, seen signed, or read in the past 24 hours.",
                    Choice("banneret_skill", "Well Spoken", "Choose a skill you gain proficiency in.", 1, ChoiceKind.SKILL,
                        ChoiceOptions.fromSkills(listOf(Skill.INSIGHT, Skill.INTIMIDATION, Skill.PERSUASION, Skill.PERFORMANCE)), "Level 3")),
                f(3, "Group Recovery", "When you use your Second Wind to regain Hit Points, you can choose allies within a 30-foot Emanation originating from yourself, up to a number equal to your Charisma modifier (minimum of one). Each of those allies regains Hit Points equal to 1d4 plus your Fighter level. Once you use this ability you can't use it again until you finish a Short or Long Rest."),
                f(7, "Team Tactics", "When you use Group Recovery, each chosen ally has Advantage on D20 Tests until the start of your next turn."),
                f(10, "Rallying Surge", "When you use your Action Surge, you can choose allies within a 30-foot Emanation originating from yourself, up to a number equal to your Charisma modifier (minimum of one). Each of those allies can immediately take a Reaction either to make one attack with a weapon or an Unarmed Strike, or to move up to half its Speed without provoking Opportunity Attacks."),
                f(15, "Shared Resilience", "When an ally you can see within 60 feet of yourself fails a saving throw, you can take a Reaction to expend a use of your Indomitable feature. The ally can immediately reroll the saving throw with a bonus equal to your Fighter level, and must use the new roll."),
                f(18, "Inspiring Commander", "Bolstered Rally: the area of effect for both Group Recovery and Rallying Surge becomes a 60-foot Emanation. Unshakable Bravery: you have Immunity to the Charmed and Frightened conditions."),
            ),
            book = HOF),

        Subclass("noble_genies", "paladin", "Oath of the Noble Genies",
            "A Paladin who reveres the Elemental Planes and the four noble genies, brandishing their elemental splendor.",
            listOf(
                f(3, "Elemental Smite", "Immediately after you cast Divine Smite, you can expend one use of your Channel Divinity to invoke one of four genie effects — Dao's Crush, Djinni's Escape, Efreeti's Fury, or Marid's Surge — choosing which one each time."),
                f(3, "Genie Spells", "You always have these prepared once you reach the listed Paladin level: Chromatic Orb, Elementalism, and Thunderous Smite at 3; Mirror Image and Phantasmal Force at 5; Fly and Gaseous Form at 9; Conjure Minor Elementals and Summon Elemental at 13; and Banishing Smite and Contact Other Plane at 17."),
                f(3, "Genie's Splendor", "When you aren't wearing any armor, your base Armor Class equals 10 plus your Dexterity and Charisma modifiers. You can use a Shield and still gain this benefit.",
                    Choice("genie_skill", "Genie's Splendor", "Choose a skill you gain proficiency in.", 1, ChoiceKind.SKILL,
                        ChoiceOptions.fromSkills(listOf(Skill.ACROBATICS, Skill.INTIMIDATION, Skill.PERFORMANCE, Skill.PERSUASION)), "Level 3")),
                f(7, "Aura of Elemental Shielding", "Choose Acid, Cold, Fire, Lightning, or Thunder. You and your allies have Resistance to that damage type while in your Aura of Protection. At the start of each of your turns you can change the damage type to another of the listed options (no action required).",
                    damageTypeChoice("elemental_shielding", "Aura of Elemental Shielding", "Level 7",
                        listOf("Acid", "Cold", "Fire", "Lightning", "Thunder"), changeableOnRest = true)),
                f(15, "Elemental Rebuke", "When you are hit by an attack roll, you can take a Reaction to halve the attack's damage against yourself (round down) and force the attacker to make a Dexterity saving throw against your spell save DC. On a failed save the attacker takes 2d10 plus your Charisma modifier damage of a type you choose from Acid, Cold, Fire, Lightning, or Thunder; on a success it takes half as much. You can do this a number of times equal to your Charisma modifier (minimum of once) per Long Rest."),
                f(20, "Noble Scion", "As a Bonus Action you gain the following benefits for 10 minutes or until you end them (no action required). Flight: you have a Fly Speed of 60 feet and can hover. Minor Wish: when you or an ally in your Aura of Protection fails a D20 Test, you can take a Reaction to make that creature succeed instead. Once you use this feature you can't use it again until you finish a Long Rest, though you can also restore it by expending a level 5 spell slot (no action required)."),
            ),
            book = HOF),

        Subclass("winter_walker", "ranger", "Winter Walker",
            "A Ranger of frigid wastelands, wielding the magic of cold and ice against the horrors that haunt them.",
            listOf(
                f(3, "Frigid Explorer", "Biting Cold: damage from your weapon attacks, Ranger spells, and Ranger features ignores Resistance to Cold damage. Frost Resistance: you have Resistance to Cold damage. Polar Strikes: when you hit a creature with an attack roll using a weapon, you can deal an extra 1d4 Cold damage to the target, once per turn; this increases to 1d6 at Ranger level 11."),
                f(3, "Hunter's Rime", "When you cast Hunter's Mark, you gain Temporary Hit Points equal to 1d10 plus your Ranger level. Additionally, while a creature is marked by your Hunter's Mark, it can't take the Disengage action."),
                f(3, "Winter Walker Spells", "You always have these prepared once you reach the listed Ranger level: Ice Knife at 3, Hold Person at 5, Remove Curse at 9, Ice Storm at 13, and Cone of Cold at 17."),
                f(7, "Fortifying Soul", "As a Magic action, choose a number of creatures you can see equal to your Wisdom modifier (minimum of one). Each chosen creature regains Hit Points equal to 1d10 plus your Ranger level and has Advantage on saving throws to avoid or end the Frightened condition for 1 hour. Once you use this feature you can't use it again until you finish a Long Rest."),
                f(11, "Chilling Retribution", "When a creature hits you with an attack roll, you can take a Reaction to force it to make a Wisdom saving throw against your spell save DC. On a failed save the target has the Stunned condition until the end of your next turn, and while Stunned its Speed is reduced to 0 feet. You can do this a number of times equal to your Wisdom modifier (minimum of once) per Long Rest."),
                f(15, "Frozen Haunt", "When you cast Hunter's Mark, you can adopt a ghostly, snowy form that lasts until the spell ends. Frozen Soul: you have Immunity to Cold damage, and when you first adopt the form and at the start of each of your subsequent turns, each creature of your choice in a 15-foot Emanation originating from you takes 2d4 Cold damage. Partially Incorporeal: you have Immunity to the Grappled, Prone, and Restrained conditions and can move through creatures and objects as Difficult Terrain, taking 1d10 Force damage if you end your turn inside one. Once you use this feature you can't use it again until you finish a Long Rest unless you expend a level 4+ spell slot (no action required)."),
            ),
            book = HOF),

        Subclass("scion_of_the_three", "rogue", "Scion of the Three",
            "A Rogue who draws power from the Dead Three — Bane, Bhaal, and Myrkul — as a gift or a curse.",
            listOf(
                f(3, "Bloodthirst", "When an enemy you can see within 30 feet of yourself takes damage and is Bloodied after taking that damage but not killed outright, you can take a Reaction and teleport to an unoccupied space you can see within 5 feet of that enemy. You can then make one melee attack. You can use this feature a number of times equal to your Intelligence modifier (minimum of once) per Long Rest."),
                f(3, "Dread Allegiance", "Choose one of the Dead Three. You gain Resistance to one type of damage and the ability to cast a cantrip, with Intelligence as your spellcasting ability for it. You can change your choice whenever you finish a Long Rest.",
                    Choice("dread_allegiance", "Dread Allegiance", "Choose one of the Dead Three.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("bane", "Bane", "Bane is the god of tyranny. You gain Resistance to Psychic damage and can cast the Minor Illusion cantrip, using Intelligence as your spellcasting ability.", "Psychic"),
                        ChoiceOption("bhaal", "Bhaal", "Bhaal is the god of violence and murder. You gain Resistance to Poison damage and can cast the Blade Ward cantrip, using Intelligence as your spellcasting ability.", "Poison"),
                        ChoiceOption("myrkul", "Myrkul", "Myrkul is the god of death. You gain Resistance to Necrotic damage and can cast the Chill Touch cantrip, using Intelligence as your spellcasting ability.", "Necrotic"),
                    ), "Level 3", changeableOnRest = true)),
                f(9, "Strike Fear", "You gain the Terrify Cunning Strike option, which costs 1d6 of your Sneak Attack damage. The target must succeed on a Wisdom saving throw or have the Frightened condition for 1 minute. While the target is Frightened this way, you have Advantage on attack rolls against it. The target repeats the save at the end of each of its turns, ending the effect on itself on a success."),
                f(13, "Aura of Malevolence", "You radiate malignant power associated with one of the Dead Three. When you use Bloodthirst and teleport, each creature of your choice within 10 feet of either the space you left or your destination space (your choice) takes damage equal to your Intelligence modifier. The damage type is the same as the Resistance granted by your Dread Allegiance choice, and it ignores Resistance."),
                f(17, "Dread Incarnate", "Cutthroat: you regain one expended use of Bloodthirst whenever you finish a Short Rest. Murderous Intent: when you roll your Sneak Attack damage, you can treat a roll of 1 or 2 on a die as a 3."),
            ),
            book = HOF),

        Subclass("spellfire_sorcery", "sorcerer", "Spellfire Sorcery",
            "A Sorcerer born with the ability to manipulate spellfire, the raw radiant power of the Weave itself.",
            listOf(
                f(3, "Spellfire Burst", "When you spend at least 1 Sorcery Point as part of a Magic action or a Bonus Action on your turn, you can unleash Bolstering Flames or Radiant Fire, choosing which one each time. You can do so only once per turn."),
                f(3, "Spellfire Spells", "You always have these prepared once you reach the listed Sorcerer level: Cure Wounds, Guiding Bolt, Lesser Restoration, and Scorching Ray at 3; Aura of Vitality and Dispel Magic at 5; Fire Shield and Wall of Fire at 7; and Greater Restoration and Flame Strike at 9."),
                f(6, "Absorb Spells", "You always have the Counterspell spell prepared. Additionally, whenever a target fails the saving throw against a Counterspell you cast, you regain 1d4 Sorcery Points."),
                f(14, "Honed Spellfire", "Your Spellfire Burst improves. You add your Sorcerer level to the Temporary Hit Points gained from Bolstering Flames, and the damage of your Radiant Fire increases to 1d8."),
                f(18, "Crown of Spellfire", "When you use Innate Sorcery you can alter it and infuse yourself with the essence of spellfire, gaining these benefits while that use of Innate Sorcery is active. Burning Life Force: once per turn when you are hit by an attack roll, you can expend a number of Hit Point Dice up to your Charisma modifier (minimum of one), roll them, and reduce the attack's damage by the total. Flight: you gain a Fly Speed of 60 feet and can hover. Spell Avoidance: when a spell or magical effect allows a saving throw for half damage, you instead take no damage on a success and only half damage on a failure, unless you have the Incapacitated condition. Once you use this feature you can't use it again until you finish a Long Rest unless you spend 5 Sorcery Points (no action required)."),
            ),
            book = HOF),

        Subclass("bladesinger", "wizard", "Bladesinger",
            "A Wizard who has mastered an ancient elven tradition of wizardry that incorporates swordplay and dance.",
            listOf(
                f(3, "Bladesong", "As a Bonus Action you invoke an elven magic called the Bladesong, provided you aren't wearing armor or using a Shield. It lasts 1 minute and ends early if you have the Incapacitated condition, if you don armor or a Shield, or if you use two hands to make an attack with a weapon. While it is active you gain a bonus to your Armor Class equal to your Intelligence modifier (minimum of +1), your Speed increases by 10 feet, you have Advantage on Dexterity (Acrobatics) checks, you can use Intelligence for attack and damage rolls with weapons you're proficient with, and you can add your Intelligence modifier to Constitution saves made to maintain Concentration. You can invoke it a number of times equal to your Intelligence modifier (minimum of once) per Long Rest, and you regain one use when you use Arcane Recovery."),
                f(3, "Training in War and Song", "You gain proficiency with all Melee Martial weapons that don't have the Two-Handed or Heavy property, and you can use a Melee weapon you're proficient with as a Spellcasting Focus for your Wizard spells.",
                    Choice("bladesinger_skill", "Training in War and Song", "Choose a skill you gain proficiency in.", 1, ChoiceKind.SKILL,
                        ChoiceOptions.fromSkills(listOf(Skill.ACROBATICS, Skill.ATHLETICS, Skill.PERFORMANCE, Skill.PERSUASION)), "Level 3")),
                f(6, "Extra Attack", "You can attack twice, instead of once, whenever you take the Attack action on your turn. Moreover, you can cast one of your Wizard cantrips that has a casting time of an action in place of one of those attacks."),
                f(10, "Song of Defense", "When you take damage while your Bladesong is active, you can take a Reaction to expend one spell slot and reduce the damage taken by an amount equal to five times the spell slot's level."),
                f(14, "Song of Victory", "After you cast a spell that has a casting time of an action, you can make one attack with a weapon as a Bonus Action."),
            ),
            book = HOF),

        // ---------------------------------------- UA 2026: Villainous Options

        Subclass("pestilence_domain", "cleric", "Pestilence Domain",
            "A Cleric of gods of poison, disease, famine and rot, who wields supernatural plague with surgical precision.",
            listOf(
                f(3, "Blight Weaver", "Inoculated Soul: you have Resistance to Necrotic and Poison damage, and you can't be infected by magical contagions. Rot and Fester: damage from your Cleric spells and Cleric features ignores Resistance to Necrotic and Poison damage, and when you cast a Cleric spell or use a Cleric feature that deals either Necrotic or Poison damage, you can change that damage to the other type."),
                f(3, "Pestilence Domain Spells", "You always have these prepared once you reach the listed Cleric level: Detect Poison and Disease, Protection from Poison, Ray of Enfeeblement, and Ray of Sickness at 3; Stinking Cloud and Vampiric Touch at 5; Blight and Giant Insect at 7; and Contagion and Insect Plague at 9."),
                f(3, "Plague Blessing", "As a Magic action you present your Holy Symbol and expend a use of Channel Divinity to manifest a 5-foot Emanation of withering plague around yourself or one willing creature you touch, for 1 minute. It ends early if you dismiss it, manifest it again, or have the Incapacitated condition. Each creature of your choice that starts its turn in the Emanation must succeed on a Constitution saving throw against your spell save DC or gain 1 Exhaustion level. This feature can't raise a creature's Exhaustion above a level equal to your Wisdom modifier (minimum of 1).",
                    Choice("plague_symptom", "Plague Symptom", "The plague you spread manifests with a specific symptom. Choose it, or roll for it.", 1, ChoiceKind.OPTION,
                        listOf(
                            ChoiceOption("drained", "Drained of Colour", "While infected, a creature is drained of all colour, appearing in monochromatic grays."),
                            ChoiceOption("metallic", "Metallic Flakes", "While infected, a creature sheds metallic, rust-hued flakes and creaks while moving."),
                            ChoiceOption("mucus", "Foul Mucus", "While infected, a creature secretes foul-smelling mucus."),
                            ChoiceOption("insects", "Buzzing Insects", "While infected, a creature is surrounded by a cloud of buzzing insects."),
                            ChoiceOption("fungi", "Sprouting Fungi", "While infected, a creature sprouts fungi or other foliage from its flesh."),
                            ChoiceOption("pustules", "Glowing Pustules", "While infected, a creature is covered in glowing pustules."),
                        ), "Level 3")),
                f(6, "Virulent Burst", "When an enemy within 60 feet of you is reduced to 0 Hit Points, you can take a Reaction to cause plague to burst from that creature, spreading pestilence in a 10-foot Emanation originating from the enemy; if the enemy had at least 1 Exhaustion level, the Emanation increases to 20 feet. Each creature in it makes a Constitution saving throw against your spell save DC. On a failure, a target suffers one of the following. Putrid Shock: the target has the Incapacitated condition until the end of its next turn, and while Incapacitated its Speed is 0. Toxic Infection: the target takes 3d6 Necrotic or Poison damage (your choice). You can use this a number of times equal to your Wisdom modifier (minimum of once) per Long Rest."),
                f(17, "Vermin Form", "As a Bonus Action you shape-shift into a Medium swarm of Tiny pests, such as cockroaches, maggots, or rats, retaining your general shape, personality, memories, and the ability to speak. Your equipment doesn't transform but you can continue using it. You have Immunity to the Grappled, Paralyzed, Prone, and Restrained conditions and Resistance to Bludgeoning, Piercing, and Slashing damage. You can enter and occupy another creature's space and vice versa, and you have a Climb Speed equal to your Speed and can climb difficult surfaces, including along ceilings, without an ability check. Plague Bites: whenever you enter an enemy's space, that creature takes damage equal to your Wisdom modifier — Necrotic, Piercing, or Poison, your choice — and a creature also takes this damage when it enters your space or ends its turn there, once per turn. You revert after 10 minutes, if you end it, if you have the Incapacitated condition, or if you die. Once you use this feature you can't use it again until you finish a Long Rest unless you expend a level 5+ spell slot (no action required)."),
            ),
            book = VILLAIN, printing = P_VILLAIN),

        Subclass("circle_of_the_titan", "druid", "Circle of the Titan",
            "A Druid who assumes a towering, monstrous form to mete out cataclysmic retribution and forcibly restore the natural order.",
            listOf(
                f(3, "Circle of the Titan Spells", "You always have these prepared once you reach the listed Druid level: Enlarge/Reduce, Thaumaturgy, and Thunderwave at 3; Fear at 5; Fire Shield at 7; and Destructive Wave at 9. You can also cast these spells while you're in your Titan Form."),
                f(3, "Titan Form", "When you use Wild Shape you can adopt a Titan Form, choosing from the Behemoth, Leviathan, and Insectoid stat blocks, and you can stay in it for 10 minutes instead of a number of hours. Each form has AC 13 plus your Wisdom modifier, Temporary Hit Points equal to four times your Druid level, Strength and Dexterity equal to your Wisdom score, Darkvision 60 feet, and a Rend melee attack using your spell attack modifier with reach 10 feet for 1d8 plus your Wisdom modifier, rising to 2d8 at Druid level 6 and 3d8 at 12. From level 5 each form can Multiattack for two Rend attacks. Behemoth: Speed 40 feet and Climb 40 feet, Siege Monster, and Incandescent Breath, which expends a level 1+ slot for 2d10 Radiant damage per slot level in a 5-foot-wide, 60-foot Line (Dexterity save for half). Leviathan: Speed 40 feet and Swim 40 feet, Amphibious, Siege Monster, and Toxic Deluge, a Bonus Action expending a level 1+ slot for 2d4 Poison damage per slot level and the Poisoned condition in a 10-foot Emanation (Constitution save). Insectoid: Speed 40 feet and Fly 40 feet from Druid level 10, Flyby, Siege Monster, and Energizing Pollen, which expends a level 1+ slot to move half your Speed without provoking Opportunity Attacks while healing creatures you move within 5 feet of for 2d6 per slot level, once per creature per turn.",
                    Choice("titan_form", "Titan Form", "Choose the shape your Titan Form takes. You determine what it looks like.", 1, ChoiceKind.OPTION,
                        listOf(
                            ChoiceOption("behemoth", "Behemoth", "A towering land titan with a Climb Speed, Siege Monster, and a Radiant Incandescent Breath in a 60-foot Line. From Druid level 10 its Rampager Bonus Action expends a spell slot to charge through smaller enemies, knocking them Prone or dealing 1d10 Bludgeoning damage per slot level.", "Slashing Rend"),
                            ChoiceOption("leviathan", "Leviathan", "An amphibious deep-water titan with a Swim Speed and Siege Monster. From Druid level 10 its Toxic Deluge Bonus Action expends a spell slot to emit a miasma dealing 2d4 Poison damage per slot level and the Poisoned condition in a 10-foot Emanation.", "Bludgeoning Rend"),
                            ChoiceOption("insectoid", "Insectoid", "A chitinous titan that gains a Fly Speed and Flyby at Druid level 10, alongside Siege Monster. Its Energizing Pollen expends a spell slot to heal creatures it flies past for 2d6 per slot level.", "Piercing Rend"),
                        ), "Level 3")),
                f(6, "Dire Impact", "Elemental Rend: whenever you hit with your Titan Form's Rend attack, you can cause it to deal your choice of Acid, Cold, Fire, Lightning, or Thunder damage rather than its normal type. Shock Wave: once per turn, immediately after you move at least half your Speed, you can create a shock wave in a 10-foot Emanation originating from you. Each creature in it must succeed on a Constitution saving throw against your spell save DC or have the Prone condition."),
                f(10, "Primal Havoc", "Huge Size: you can become Huge when assuming your Titan Form if you're in a big enough space. Toughened Hide: immediately after you assume a Huge or larger Titan Form, you can expend a level 1+ spell slot; for the duration of the form you gain a bonus to your AC equal to half the expended slot's level (round up). Above It All: while you are Huge or larger in your Titan Form, Difficult Terrain caused by heavy snow, ice, rubble, or undergrowth doesn't cost you extra movement."),
                f(14, "Monstrous Appetite", "Gargantuan Size: you can become Gargantuan when assuming your Titan Form if you're in a big enough space. Grappling Rend: once per turn, while you are Huge or larger and hit a creature with your Titan Form's Rend attack, you can give the target the Grappled condition (escape DC equals your spell save DC); you can have only one target grappled this way at a time. Swallow: as a Bonus Action while you are Gargantuan, choose one Large or smaller creature Grappled by you. It makes a Strength saving throw against your spell save DC; on a failure you swallow it. A swallowed creature has the Blinded and Restrained conditions, has Total Cover against attacks outside your stomach, and takes Acid damage at the start of each of your turns equal to a number of d12s equal to your Wisdom modifier. You can hold a number of creatures equal to your Wisdom modifier (minimum of one) and must maintain Concentration to keep them; losing it or leaving your Titan Form regurgitates them all Prone within 10 feet of you."),
            ),
            book = VILLAIN, printing = P_VILLAIN_UPDATE),

        Subclass("hell_knight", "fighter", "Hell Knight",
            "A champion of archdevils who inflicts infernal wounds with a hell-forged weapon and fights with the tenacity of a devil.",
            listOf(
                f(3, "Diabolical Gift", "Devil's Sight: you can see normally in Dim Light and Darkness — both magical and nonmagical — within 120 feet of yourself. Devil's Talents: you know Infernal, the language of devils; if you already know it, you learn another language of your choice.",
                    Choice("hell_knight_skill", "Devil's Talents", "Choose a skill you gain proficiency in.", 1, ChoiceKind.SKILL,
                        ChoiceOptions.fromSkills(listOf(Skill.DECEPTION, Skill.PERFORMANCE, Skill.SLEIGHT_OF_HAND)), "Level 3")),
                f(3, "Hell-Forged Weapon", "When you take the Attack action, you can imbue each weapon you are holding with hellfire, transforming it into a Hell-Forged Weapon. It stays transformed until you use this feature again, you have the Unconscious condition, or the weapon is more than 5 feet away from you for 1 minute or more; you can also end the effect early (no action required). A Hell-Forged Weapon sheds Dim Light in a 5-foot radius, and whenever you deal damage with it, it can deal your choice of Cold, Fire, or Necrotic damage or its normal damage type (choose when you imbue the weapon).",
                    damageTypeChoice("hellfire_damage", "Hellfire", "Level 3", listOf("Cold", "Fire", "Necrotic"), changeableOnRest = true)),
                f(3, "Infernal Wound", "You have an Infernal Wound Die, which is a d6. When you hit a creature with your Hell-Forged Weapon, you can deal extra damage equal to one roll of that die, of the type you chose when you imbued the weapon, and you give the target an infernal wound if it doesn't already have one. While wounded this way the target takes damage of the chosen type equal to one roll of your Infernal Wound Die at the start of each of its turns. The wound lasts for 1 minute, until the target regains Hit Points, or until the target or a creature within 5 feet of it takes an action to stanch the wound. You can use this feature a number of times equal to your Constitution modifier (minimum of once), regaining all expended uses on a Short or Long Rest."),
                f(7, "Advanced Wounds", "When you roll your Infernal Wound Die, you can apply one of the following effects. If you roll a 6, the effect you choose has an additional Devil's Luck effect; once you use that, you can't do so again until the start of your next turn. Purulence of Minauros: caustic pus erupts from the wound, and each enemy in a 5-foot Emanation originating from the target takes Acid damage equal to your Constitution modifier, and the target has the Poisoned condition until the end of its next turn (Devil's Luck: each creature that takes this Acid damage has a -1 penalty to its AC until the end of your next turn). Rupture of Cania: the wound ruptures for Force damage equal to your Constitution modifier (Devil's Luck: the target subtracts 1d6 from its next saving throw before the end of your next turn). Stygian Gangrene: infernal rime spreads for Cold damage equal to your Constitution modifier, and the target can't take Reactions until the start of its next turn (Devil's Luck: the target's Speed is halved until the end of its next turn)."),
                f(7, "Infernal Equipment", "Infernal Resilience: whenever you finish a Short or Long Rest, choose Cold, Fire, or Necrotic damage; while wearing Heavy armor or wielding a Shield you have Resistance to that damage type until you choose a different one. Unholy Power: when you roll your Infernal Wound Die, you can treat a roll of 1 as a 6.",
                    damageTypeChoice("infernal_resilience", "Infernal Resilience", "Level 7", listOf("Cold", "Fire", "Necrotic"), changeableOnRest = true)),
                f(10, "Hellfire Surge", "When you use your Action Surge while holding a Hell-Forged Weapon, you erupt with hellfire in a 20-foot Emanation originating from you that lasts until the end of your next turn. Whenever a creature suffering an infernal wound starts its turn within the Emanation, it takes damage equal to two rolls of your Infernal Wound Die instead of one."),
                f(15, "Devil's Misfortune", "When a creature with an infernal wound hits you with an attack roll, you can take a Reaction to roll your Infernal Wound Die and reduce the damage taken by the number rolled. On a roll of 6, roll your Infernal Wound Die again (to a maximum of three rolls total) and reduce the damage by the total rolled. In addition, if the attack is a Critical Hit, it becomes a normal hit."),
                f(18, "Infernal Bargain", "When you roll a 6 on your Infernal Wound Die three or more times before the start of your next turn, you gain Heroic Inspiration. Infernal Inspiration: if a creature you can see within 120 feet of you rolls a d20 for a D20 Test, you can expend your Heroic Inspiration to force the target to reroll the d20. If the number rolled causes the target to succeed, you regain an expended use of Indomitable or Second Wind (your choice). If it causes the target to fail, you lose Hit Points equal to 3d6 plus your Fighter level."),
            ),
            book = VILLAIN, printing = P_VILLAIN_UPDATE),

        Subclass("demonic_sorcery", "sorcerer", "Demonic Sorcery",
            "A Sorcerer whose innate magic is a conduit to the infinite layers of the Abyss, warping their body and their surroundings in tandem.",
            listOf(
                f(3, "Abyssal Rupture", "When you use Innate Sorcery you create a rupture into the Abyss, which fills a 10-foot-radius Sphere centered on a point you can see within 30 feet of yourself with Abyssal energy. When you activate your Innate Sorcery and as a Bonus Action while it is active, you can choose one of the following options; while the rupture persists you can move its center to a point you can see within 30 feet of yourself at the start of each of your turns. Demonic Lash: make a melee spell attack against a target within 5 feet of the rupture; on a hit it takes 1d8 Slashing damage, and if it is Large or smaller you can pull it up to 10 feet toward the center of the Sphere. Terrifying Screams: each creature in the rupture must succeed on a Wisdom saving throw against your spell save DC or take 1d4 Psychic damage.",
                    Choice("abyssal_manifestation", "Abyssal Manifestation", "Choose how your connection to the Abyss shows itself when you channel your demonic power. This has no effect on your game statistics.", 1, ChoiceKind.OPTION,
                        listOf(
                            ChoiceOption("fissures", "Abyssal Fissures", "Abyssal fissures mar your flesh, revealing windows into a vast demonic realm."),
                            ChoiceOption("insects", "Writhing Insects", "Insects writhe beneath your skin and escape from your mouth, nose, and ears."),
                            ChoiceOption("scorched", "Scorched Skin", "Sheets of scorched skin peel from your body."),
                            ChoiceOption("bog", "Toxic Bog", "Your flesh bubbles and froths like a toxic bog."),
                            ChoiceOption("frostbitten", "Frostbitten Extremities", "Your fingers or other extremities discolor as if frostbitten."),
                            ChoiceOption("second_head", "A Second Head", "You grow a second head. This has no impact on your game statistics."),
                        ), "Level 3")),
                f(3, "Demonic Spells", "You always have these prepared once you reach the listed Sorcerer level: Bane, Dissonant Whispers, Spike Growth, and Web at 3; Bestow Curse and Dispel Magic at 5; Giant Insect and Hallucinatory Terrain at 7; and Contact Other Plane and Modify Memory at 9."),
                f(6, "Abyssal Realm", "When you spend at least 1 Sorcery Point as part of a Magic action or a Bonus Action on your turn, you can pull influence from the Abyss, creating a 10-foot Emanation originating from you or filling the Sphere of your Abyssal Rupture with magic from one of the following layers. If an effect requires a saving throw, the DC equals your spell save DC. Gaping Maw's Frenzy: designate a horizontal direction; each creature in the area that fails a Charisma saving throw must use as much of its movement as possible to move that way at the start of its next turn, taking the safest route. Maze of Azzatar: each creature in the area makes an Intelligence saving throw, and on a failure you gain the benefits of the Invisible condition against it until the start of your next turn. Slime Pits' Haze: each creature in the area makes a Constitution saving throw, and on a failure has your choice of the Charmed or Poisoned condition until the start of your next turn."),
                f(14, "Abyssal Conduit", "Rupture Expansion: your Abyssal Rupture is now a 30-foot-radius Sphere, and the area is Difficult Terrain for your enemies. Fiendish Servant: you always have the Summon Fiend spell prepared. When you cast it you can modify it so that it doesn't require Concentration; if you do, the spell's duration becomes 1 minute for that casting and you must choose Demon when you summon the Fiend. In addition, the Fiend has Advantage on attack rolls while within your Abyssal Rupture."),
                f(18, "Abyssal Explosion", "As a Magic action, you fill a 30-foot-radius Sphere with an explosion of Abyssal energy. Each creature in the Sphere makes a Constitution saving throw against your spell save DC. On a failed save, a creature takes 8d6 Force damage if it isn't a Fiend, and it has the Incapacitated condition until the start of your next turn. Once you use this feature you can't do so again until you finish a Long Rest, unless you spend 7 Sorcery Points (no action required) to restore your use of it."),
            ),
            book = VILLAIN, printing = P_VILLAIN_UPDATE),

        // ---------------------------------------- UA 2026: Villainous Options 2

        Subclass("path_of_lament", "barbarian", "Path of Lament",
            "A Barbarian who hones regret into a weapon, channelling unresolved grief into a wail that carries beyond the realm of the living.",
            listOf(
                f(3, "Banshee's Wail", "When you activate your Rage or as a Bonus Action while your Rage is active, you can let out a doleful wail. Each creature of your choice in a 30-foot Emanation originating from you makes a Constitution saving throw (DC 8 plus your Constitution modifier and Proficiency Bonus). On a failed save, a creature takes Psychic damage and has the Deafened condition for 1 minute. On a successful save, a creature takes half as much damage only. To determine the Psychic damage, roll a number of d12s equal to your Rage Damage bonus, and add them together. You can use this feature a number of times equal to your Constitution modifier (minimum of once). You regain all expended uses when you finish a Long Rest. You can also regain all uses by expending a use of your Rage (no action required).",
                    Choice("lament_origin", "Your Rage Stems from the Time…", "The root of your despair. Choose it or roll a d6. This shapes your character rather than your statistics.", 1, ChoiceKind.OPTION,
                        listOf(
                            ChoiceOption("loved_one", "A Lost Loved One Rose Again", "A lost loved one rose as an undead monster.", "1"),
                            ChoiceOption("companion", "Your Animal Companion Was Slain", "Enemies slew your animal companion.", "2"),
                            ChoiceOption("captors", "You Alone Escaped", "You — and you alone — escaped brutal captors.", "3"),
                            ChoiceOption("charred_remains", "Your Family's Charred Remains", "You found your family's charred remains.", "4"),
                            ChoiceOption("shipwreck", "A Devastating Shipwreck", "Your decision led to a devastating shipwreck.", "5"),
                            ChoiceOption("banished", "Banished from Home", "Your elders banished you from your home.", "6"),
                        ), "Level 3")),
                f(6, "Commune with the Dead", "You can cast the Speak with Dead spell, but only as a Ritual. Wisdom is your spellcasting ability for it."),
                f(6, "Horrifying Strike", "Once per turn when you hit a creature with a Strength-based attack roll while your Rage is active, you can attempt to horrify the target. The target must succeed on a Wisdom saving throw (DC 8 plus your Constitution modifier and Proficiency Bonus) or have the Frightened condition until the start of your next turn."),
                f(10, "Otherworldly Anguish", "You draw power from a sorrow so deep it extends beyond the boundaries of the realm of the living. Deathly Wail: if a target fails its saving throw against your Banshee's Wail and it has Hit Points equal to twice your Barbarian level or fewer, it drops to 0 Hit Points instead of taking damage. Impenetrable Sorrow: you can't be possessed. Resistance: you have Resistance to Cold and Necrotic damage while your Rage is active."),
                f(14, "Sorrow Form", "When you activate your Rage, you can empower yourself with undeath. You gain the benefits below for 1 minute or until you drop to 0 Hit Points. Once you use this feature, you can't do so again until you finish a Long Rest. Immunities: you have Immunity to the Charmed and Frightened conditions, and if you're Charmed or Frightened when you empower yourself, the condition ends on you; in addition, you can't gain Exhaustion levels. Life-Draining Strike: when a creature fails its saving throw against your Horrifying Strike, the creature takes 2d10 Necrotic damage, and you regain Hit Points equal to the Necrotic damage dealt. Undead: your creature type is Undead."),
            ),
            book = VILLAIN, printing = P_VILLAIN_2),

        Subclass("warrior_of_venom", "monk", "Warrior of Venom",
            "A Monk who pollutes their own internal reservoirs of power until a touch — or a single drop of blood — is as deadly as a viper's bite.",
            listOf(
                f(3, "Envenom Weapon", "At the start of your turn, you can expend 1 Focus Point to apply a toxin produced from your blood to one Monk weapon that you're holding. A creature that takes damage from the weapon is subjected to one of the toxin effects, chosen when you apply the toxin. Slowing Toxin: until the start of your next turn, the target's Speed is halved, it can't take Reactions, and it can take either an action or a Bonus Action on its turn, not both. Venom: the target takes Poison damage equal to two rolls of your Martial Arts die. The toxin retains potency for 1 minute or until a creature takes damage from the weapon."),
                f(3, "Potent Arsenal", "You gain a Poisoner's Kit, and you have proficiency with it. When creating a Basic Poison, you can do so over the course of 1 day (8 hours of work). Additionally, whenever you deal Poison damage with a Monk feature or a Monk weapon, you can change that damage type to Acid."),
                f(6, "Toxic Touch", "As a Magic action, you can expend 1 Focus Point to apply a potent toxin to a creature you touch. The target makes a Constitution saving throw. On a failed save, the target has the Poisoned condition for 1 minute, and while Poisoned it is affected by one of the following effects of your choice. Intoxicant: the target has the Charmed condition for the duration or until you or your allies deal damage to the target. Sedative: the creature falls asleep and has the Unconscious condition for the duration; another creature can use an action to shake it awake and remove the condition. Truth Serum: the target can't knowingly communicate a lie for the duration."),
                f(11, "Toxin Refiner", "Your body can filter poison. You gain Immunity to Poison damage. Whenever you are subjected to Poison damage, your Envenom Weapon options each deal extra Poison damage equal to one roll of your Martial Arts die, and you can't gain this benefit again until the end of your next turn. In addition, whenever you ingest a poison, you regain a number of Hit Points equal to one roll of your Martial Arts die."),
                f(11, "Toxic Blood", "Enemies draw your toxic blood at their own peril. Whenever a creature hits you with a melee attack roll, the attacker takes 1d6 Poison damage. If you are Bloodied, the attacker instead takes Poison damage equal to one roll of your Martial Arts die."),
                f(17, "Hallucinogenic Breath", "When you take the Attack action on your turn, you can expend 2 Focus Points and replace one of your attacks with an exhalation of hallucinogenic vapors at one creature you can see within 30 feet. The target must make a Constitution saving throw. On a failed save, the target takes Poison damage equal to three rolls of your Martial Arts die and has the Frightened condition for 1 minute or until the target takes damage. While Frightened, the target takes the Dash action and moves away from you by the safest route on each of its turns unless there is nowhere to move. On a successful save, a creature takes half as much damage only."),
            ),
            book = VILLAIN, printing = P_VILLAIN_2),

        Subclass("primordial_patron", "warlock", "Primordial Patron",
            "A Warlock whose pact draws on the Inner Planes, heralding the arrival of ancient elemental powers by seeding the world with elemental nodes.",
            listOf(
                f(3, "Elemental Node", "As a Magic action, you can create a 5-foot-radius Sphere of elemental magic centered on a point you can see within 60 feet of yourself. The magic of this elemental node resembles your chosen element. On later turns, you can take a Bonus Action to move the node up to 30 feet. When the node appears, each creature other than you in the node makes a Dexterity saving throw against your spell save DC, taking 1d6 damage of your chosen element's type on a failed save or half as much damage on a successful one. A creature also makes this save when the node moves into its space and when it enters the node or ends its turn there. A creature makes this save only once per turn. The node lasts for 1 minute, until you dismiss it (no action required), or until you use this feature to create another node. Once you use this feature, you can't use it again until you complete a Short or Long Rest unless you expend a Pact Magic spell slot (no action required) to restore your use of it. The node's damage increases by 1d6 when you reach Warlock levels 6 (2d6) and 14 (3d6).",
                    elementChoice()),
                f(3, "Elemental Spells", "The magic of your patron ensures you always have certain spells ready. You always have Chromatic Orb and Darkvision prepared at Warlock level 3, Elemental Weapon at 5, Summon Elemental at 7 — the spirit's element matches your chosen element — and Commune with Nature at 9, along with the spells corresponding to your chosen element. Air: Feather Fall and Shatter at 3, Fly at 5, Freedom of Movement at 7, Steel Wind Strike at 9. Earth: Entangle and Knock at 3, Plant Growth at 5, Vitriolic Sphere at 7, Wall of Stone at 9. Fire: Burning Hands and Heat Metal at 3, Fireball at 5, Wall of Fire at 7, Flame Strike at 9. Water: Alter Self and Ice Knife at 3, Water Walk at 5, Control Water at 7, Cone of Cold at 9."),
                f(6, "Elemental Haven", "Your Elemental Node shields you from harm. Elemental Protection: while you're within your node, you have a bonus to AC equal to your Charisma modifier (minimum of 1). Elemental Teleport: as a Bonus Action, you can teleport into your node or the nearest unoccupied space within 5 feet of it. You can use this benefit a number of times equal to your Charisma modifier (minimum of once), and you regain all expended uses when you finish a Long Rest."),
                f(10, "Primeval Protection", "Elemental Fortitude: you have Resistance to your chosen element's damage type. Additionally, while within your Elemental Node, you have Immunity to that damage type. Node Improvement: your Elemental Node is now a 10-foot-radius Sphere."),
                f(14, "Elemental Harbinger", "Your Elemental Node can usher in the mightiest of elementals. Elemental Vortex: whenever you expend a Pact Magic spell slot while you're within your Elemental Node, you can attempt to pull a creature into the node — one creature you choose within 30 feet of the node must succeed on a Strength saving throw or be pulled up to 15 feet toward the node's center. Node Improvement: your Elemental Node now lasts for up to 1 hour. Primordial Herald: while you're within your node's area, you can cast the Planar Ally spell without expending a spell slot, speaking the name of your patron when you do. Once you use this benefit, you can't use it again until you finish 2d4 Long Rests."),
            ),
            book = VILLAIN, printing = P_VILLAIN_2),

        // ---------------------------------------- UA 2025: Horror Subclasses

        Subclass("reanimator", "artificer", "Reanimator",
            "An Artificer who stitches servants from disparate corpses and turns the art of necromancy into a terrifying science.",
            listOf(
                f(3, "Reanimator Spells", "You always have these prepared once you reach the listed Artificer level: False Life, Spare the Dying, and Witch Bolt at 3; Blindness/Deafness and Enhance Ability at 5; Animate Dead and Lightning Bolt at 9; Blight and Death Ward at 13; and Antilife Shell and Raise Dead at 17."),
                f(3, "Jolt to Life", "When you cast Spare the Dying, you can modify the spell so that it sends a jolt of electricity through the target, reviving it. The target regains 1 Hit Point, and each creature in a 10-foot Emanation originating from the target makes a Dexterity saving throw against your spell save DC, taking Lightning damage equal to 1d4 plus half your Artificer level (round up) on a failed save or half as much on a success. You can modify the spell this way a number of times equal to your Intelligence modifier, regaining all expended uses when you finish a Long Rest."),
                f(3, "Reanimated Companion", "Using Tinker's Tools or another type of Artisan's Tools with which you have proficiency, you can take a Magic action to create a Reanimated Companion in an unoccupied space within 5 feet of you. It is a Small Undead with AC 10 plus your Intelligence modifier, Hit Points equal to 4 plus four times your Artificer level, Speed 30 feet, Resistance to Necrotic and Poison damage, Immunity to Lightning damage and the Charmed, Exhaustion, and Poisoned conditions, and Blindsight 60 feet. Death Burst: when it dies it explodes, and each creature in a 10-foot Emanation makes a Dexterity saving throw against your spell save DC, taking 2d6 Necrotic damage on a failure. Lightning Absorption: whenever it is subjected to Lightning damage it instead regains that many Hit Points. Dreadful Swipe: a melee attack using your spell attack modifier, reach 5 feet, for 1d4 plus 2 plus your Intelligence modifier Necrotic damage, and the target can't take Opportunity Attacks until the start of its next turn. It is Friendly to you and your allies, lasts until you finish a Long Rest or dismiss it, takes only the Dodge action unless you spend a Bonus Action to command it, and can't be recreated until you finish a Long Rest or expend a spell slot."),
                f(5, "Strange Modifications", "Whenever you create a Reanimated Companion it gains one of the following options of your choice. Arcane Conduit: you can cast spells as though you were in the companion's space, but you must use your own senses; once per turn, when you cast an Artificer spell from the Evocation or Necromancy schools and deal damage while your companion is within 120 feet of you, you can add your Intelligence modifier to one damage roll of that spell. Ferocity: when you command your companion to take the Dreadful Swipe action, it can use it twice."),
                f(9, "Improved Reanimation", "Whenever you create a Reanimated Companion it gains one more of the following options of your choice. Bloated: the companion becomes Large or Medium (your choice); whenever it hits a Large or smaller creature with its Dreadful Swipe, that creature can also be pushed up to 10 feet away, and you add your Intelligence modifier to the damage dealt by its Death Burst. Gaunt: its Speed increases to 45 feet and it gains a Climb Speed equal to its Speed, able to climb difficult surfaces including ceilings without an ability check; whenever a creature of your choice starts its turn within a 10-foot Emanation originating from your companion, it must succeed on a Wisdom saving throw against your spell save DC or have the Frightened condition until the start of its next turn. Moist: it gains a Swim Speed equal to its Speed, and whenever it is hit by an attack roll from a creature within 10 feet of it, the attacker takes Acid damage equal to your Intelligence modifier."),
                f(15, "Promethean Reanimation", "Facilitated Revival: when you cast Revivify or Raise Dead, the cost of any Material components needed for that spell is halved. Improved Companion: the damage of your Reanimated Companion's Death Burst increases to 4d6, and Necrotic damage dealt by your companion ignores Resistance. Life Transfer: when you take damage, you can take a Reaction to cause your Reanimated Companion to drop to 0 Hit Points. The companion immediately dies, triggering its Death Burst, and you regain a number of Hit Points equal to your Artificer level."),
            ),
            book = HORROR),

        Subclass("college_of_spirits", "bard", "College of Spirits",
            "A Bard who uses occult trappings to conjure legendary and long-dead spirits — capricious entities whose gifts are never entirely under the Bard's control.",
            listOf(
                f(3, "Channeler", "Guiding Whispers: you know the Guidance cantrip, and it has a range of 60 feet when you cast it. Spiritual Focus: you gain a Gaming Set (Playing Cards) and proficiency with it, and you can use the cards or one of the following items as a Spellcasting Focus for your Bard spells: an Arcane Focus (Crystal or Orb), a Candle, or an Ink Pen.",
                    Choice("spiritual_focus", "Spiritual Focus", "Choose the trappings you channel spirits through.", 1, ChoiceKind.OPTION,
                        listOf(
                            ChoiceOption("playing_cards", "Playing Cards", "The Gaming Set the feature grants you, dealt and read as the spirits move you."),
                            ChoiceOption("crystal", "Arcane Focus (Crystal or Orb)", "A scrying stone through which the departed make themselves seen."),
                            ChoiceOption("candle", "Candle", "A flame whose guttering marks the presence of something beyond the grave."),
                            ChoiceOption("ink_pen", "Ink Pen", "A pen that writes what the spirits dictate, whether or not you will it."),
                        ), "Level 3")),
                f(3, "Spirits from Beyond", "While holding a Spiritual Focus, you can take a Bonus Action to expend one use of your Bardic Inspiration and call forth the powers of a spirit. Roll the Bardic Inspiration die and refer to the Spirits from Beyond table to determine the spirit you channel, then choose one creature you can see within 30 feet of yourself as the spirit's target. The spirit immediately takes effect; if its effect requires a saving throw, the DC equals your Bard spell save DC. 1 Beloved: the target regains Hit Points equal to one roll of your Bardic Inspiration die plus your Charisma modifier. 2 Sharpshooter: the target takes Force damage equal to one roll plus your Charisma modifier. 3 Avenger: until the end of your next turn, any creature that hits the target with a melee attack roll takes Force damage equal to a roll of the die. 4 Renegade: the target can immediately take a Reaction to teleport up to 30 feet to an unoccupied space it can see. 5 Fortune Teller: the target has Advantage on D20 Tests until the start of your next turn. 6 Wayfarer: the target gains Temporary Hit Points equal to a roll plus your Bard level, and while it has them its Speed increases by 10 feet. 7 Trickster: Wisdom save or take Psychic damage equal to two rolls and have the Charmed condition until the start of your next turn (half damage on a success). 8 Shade: the target gains the Invisible condition until the end of its next turn or until it attacks, deals damage, or casts a spell; when the invisibility ends, each creature in a 5-foot Emanation from the target makes a Constitution save or takes Necrotic damage equal to two rolls. 9 Arsonist: Dexterity save for Fire damage equal to four rolls, half on a success. 10 Coward: the target and each creature of your choice in a 30-foot Emanation from it makes a Wisdom save or has the Frightened condition until the start of your next turn, with Speed halved and able to take either an action or a Bonus Action but not both. 11 Brute: each creature of your choice in a 30-foot Emanation from the target makes a Strength save, taking Thunder damage equal to three rolls and the Prone condition on a failure, half damage on a success. 12 Controlled Channeling: you determine the spirit's effect by choosing one of the other rows."),
                f(6, "Empowered Channeling", "Power from Beyond: once per turn, when you cast a Bard spell that deals damage or restores Hit Points, roll a d6 and gain a bonus to one of the spell's damage rolls or to the total Hit Points it restores equal to the number rolled. Spiritual Manifestation: you always have the Spirit Guardians spell prepared, and you can cast it once without a spell slot, regaining that use when you finish a Long Rest. Whenever you start casting the spell you can modify it so the spirits guard against worldly threats; when cast this way, you and allies within the spell's Emanation have Half Cover. Once you modify it this way you can't do so again until you finish a Short or Long Rest."),
                f(14, "Mystical Connection", "You gain mastery over the spirits you call forth. Whenever you roll on the Spirits from Beyond table, you can roll the die twice and choose which of the two effects to bestow."),
            ),
            book = HORROR),

        Subclass("grave_domain", "cleric", "Grave Domain",
            "A Cleric who guards the boundary between life and death, staving off the grave for those with work left to do and destroying the Undead who defy it.",
            listOf(
                f(3, "Circle of Mortality", "Pull of Death: once per turn, when you cast a spell or hit with an attack roll and deal damage to a Bloodied creature, that creature takes an extra 1d4 Necrotic damage. Return to Life: when you would normally roll one or more dice to restore Hit Points to a creature at 0 Hit Points with a spell or Channel Divinity, don't roll those dice; instead use the highest number possible for each die."),
                f(3, "Grave Domain Spells", "You always have these prepared once you reach the listed Cleric level: Bane, Chill Touch, Detect Evil and Good, Gentle Repose, and Ray of Enfeeblement at 3; Revivify and Vampiric Touch at 5; Blight and Dispel Evil and Good at 7; and Hold Monster and Raise Dead at 9."),
                f(3, "Path to the Grave", "As a Bonus Action you present your Holy Symbol and expend a use of Channel Divinity to curse one creature you can see within 30 feet of yourself until the start of your next turn. While cursed, the creature has Disadvantage on attack rolls and saving throws. When you or an ally you can see hits the cursed target with an attack roll, you can end the curse early (no action required) to make the attack deal extra Necrotic or Radiant damage (your choice) equal to 1d8 plus your Cleric level."),
                f(6, "Sentinel at Death's Door", "When you or a Bloodied creature you can see within 30 feet of yourself is hit with an attack roll, you can take a Reaction to halve that attack's damage. You can use this a number of times equal to your Wisdom modifier (minimum of once), regaining all expended uses when you finish a Long Rest."),
                f(17, "Divine Reaper", "Enhanced Necromancy: when you cast a spell of level 5 or lower from the Necromancy school that targets one creature, or a spell from the Grave Domain Spells table, you can expend a use of Channel Divinity to target a second creature within the spell's range. If the spell requires costly or consumed Material components, you must provide them for each target. Keeper of Souls: when an enemy dies within 60 feet of you, you or one creature you can see within 60 feet of yourself regains Hit Points equal to three times your Cleric level. You can't use this if you have the Incapacitated condition, and once you use it you can't use it again until you finish a Short or Long Rest."),
            ),
            book = HORROR),

        Subclass("hollow_warden", "ranger", "Hollow Warden",
            "A Ranger who venerates the ancient, bloodthirsty terrors of fallow soil and sharpened claws, transforming into a merciless guardian of dark and wild places.",
            listOf(
                f(3, "Hollow Warden Spells", "You always have these prepared once you reach the listed Ranger level: Wrathful Smite at 3; Spike Growth at 5; Phantom Steed at 9; Hallucinatory Terrain at 13; and Awaken at 17."),
                f(3, "Wrath of the Wild", "When you cast Hunter's Mark, you transform for the spell's duration. Ancient Armor: you gain a bonus to your AC equal to your Wisdom modifier (minimum of +1); your body is wreathed in rotten bark or beastly bristles. Unnerving Aura: when an enemy starts its turn within a 10-foot Emanation originating from you, it makes a Wisdom saving throw against your spell save DC; on a failed save it can take either an action or a Bonus Action on this turn, not both. Your shadow lengthens and twists around you, or you sprout unnatural growths like bloody antlers or putrid fangs.",
                    Choice("wrath_appearance", "Wrath of the Wild", "Choose how the transformation shows itself. This has no effect on your game statistics.", 1, ChoiceKind.OPTION,
                        listOf(
                            ChoiceOption("bark", "Rotten Bark", "Your body is wreathed in rotten bark, damp and blackened at the seams."),
                            ChoiceOption("bristles", "Beastly Bristles", "Coarse bristles push through your skin in overlapping plates."),
                            ChoiceOption("antlers", "Bloody Antlers", "You sprout unnatural growths — bloody antlers that catch the light wrong."),
                            ChoiceOption("fangs", "Putrid Fangs", "Your jaw distends around a second row of putrid fangs."),
                            ChoiceOption("shadow", "Lengthening Shadow", "Your shadow lengthens and twists around you, moving a beat behind."),
                        ), "Level 3")),
                f(7, "Hungering Might", "You gain a bonus to Constitution saving throws equal to your Wisdom modifier (minimum of +1). In addition, once per turn when you hit a creature with an attack roll while transformed using Wrath of the Wild, you regain a number of Hit Points equal to 1d10 plus your Wisdom modifier, provided you are Bloodied when you hit."),
                f(11, "Rot and Violence", "Eerie Aura: when a creature fails its saving throw against your Unnerving Aura, it also takes Necrotic, Poison, or Psychic damage (your choice) equal to your Ranger level, and this damage ignores Resistance. Strangling Roots: when you hit a creature with an attack roll using a weapon, you can activate the Sap or Slow mastery property in addition to a different mastery property you're using with that weapon."),
                f(15, "Ancient Endurance", "Persistent Hunt: if you drop to 0 Hit Points while transformed using Wrath of the Wild and don't die outright, you can expend a level 4+ spell slot (no action required); your Hit Points then instead change to an amount equal to five times the level of the spell slot expended. Timeless: you have Immunity to the Exhaustion condition."),
            ),
            book = HORROR),

        Subclass("phantom", "rogue", "Phantom",
            "A Rogue who walks the veil between life and death, shepherding opponents to the grave and stealing knowledge from the souls who have passed on.",
            listOf(
                f(3, "Wails from the Grave", "Immediately after you deal your Sneak Attack damage to a creature on your turn, you can target a second creature that you can see within 30 feet of the first creature. Roll half the number of Sneak Attack dice for your level (round up), and the second creature takes Necrotic damage equal to the roll's total as wails of the dead momentarily sound around them. You can use this a number of times equal to your Dexterity modifier (minimum of once), regaining all expended uses when you finish a Long Rest."),
                f(3, "Whispers of the Dead", "Whenever you finish a Short or Long Rest, you can choose one skill or tool proficiency that you lack and gain it, as a ghostly presence shares its knowledge with you. You lose this proficiency when you use this benefit again to choose a different proficiency.",
                    Choice("whispers_of_the_dead", "Whispers of the Dead", "Choose the skill a ghostly presence is lending you. You can change it whenever you rest.", 1, ChoiceKind.SKILL,
                        ChoiceOptions.fromSkills(Skill.ALL), "Level 3", changeableOnRest = true)),
                f(9, "Tokens of the Departed", "Whenever you finish a Long Rest, you gain two soul trinkets, each a Tiny object that lasts until you finish another Long Rest, at which point they disintegrate. If you ever move more than 30 feet from a trinket, it immediately teleports to you. The number rises to three at Rogue level 13 and four at 17. Death's Knell: when you deal Sneak Attack damage on your turn, you can expend one soul trinket and immediately use Wails from the Grave without expending a use of that feature; the trinket is destroyed. Life Essence: while you have at least one soul trinket, you have Advantage on Death Saving Throws and Constitution saving throws. Spirit Query: you can take a Magic action to expend one soul trinket and immediately cast the Augury spell, requiring no spell components and using Dexterity as the spellcasting modifier; the trinket is destroyed. Regaining Soul Trinkets: when a creature you can see within 30 feet of you dies, you can take a Reaction to regain one expended soul trinket."),
                f(9, "Voice of Death", "You can cast Speak with Dead once without a spell slot, requiring no spell components and using Dexterity as the spellcasting modifier. You regain the ability to cast it this way when you finish a Short or Long Rest."),
                f(13, "Ghost Walk", "As a Bonus Action you assume a spectral form for 10 minutes or until you end it (no action required). Flight: you gain a Fly Speed of 10 feet and can hover. Hazy Form: attack rolls have Disadvantage against you. Incorporeal Movement: you can move through occupied spaces as if they were Difficult Terrain, and if you end your turn in such a space you take 1d10 Force damage. Once you use this feature you can't use it again until you finish a Long Rest unless you expend and destroy one of your soul trinkets from Tokens of the Departed (no action required)."),
                f(17, "Death's Friend", "Death's Lament: when you use your Wails from the Grave feature, you can deal the Necrotic damage to both the first and the second creature. Draw of Death: when you roll Initiative, you regain one soul trinket for your Tokens of the Departed if you have none remaining."),
            ),
            book = HORROR),

        Subclass("shadow_sorcery", "sorcerer", "Shadow Sorcery",
            "A Sorcerer whose innate magic comes from the Shadowfell itself, commanding darkness, undeath, and woe with ease.",
            listOf(
                f(3, "Eyes of the Dark", "You have Darkvision with a range of 120 feet and Blindsight with a range of 10 feet. In addition, if a spell you cast creates an area of Darkness, you can see normally through that spell's Darkness."),
                f(3, "Shadow Spells", "You always have these prepared once you reach the listed Sorcerer level: Bane, Darkness, Inflict Wounds, and Pass without Trace at 3; Hunger of Hadar and Summon Undead at 5; Greater Invisibility and Phantasmal Killer at 7; and Contagion and Creation at 9."),
                f(6, "Spirits of Ill Omen", "You can cast Summon Undead without a Material component. You can also cast it once without expending a spell slot, regaining the ability to cast it this way when you finish a Long Rest. Whenever you start casting the spell, you can modify it so that it doesn't require Concentration; if you do so, the spell's duration becomes 1 minute for that casting, and the spell ends early if you cast it again."),
                f(14, "Shadow Walk", "While in Dim Light or Darkness, you can take a Bonus Action to teleport up to 120 feet to an unoccupied space you can see that is also in Dim Light or Darkness."),
                f(18, "Umbral Form", "As a Bonus Action you adopt a shadowy form for 1 minute, until you have the Incapacitated condition, or until you end the form (no action required). Incorporeal Movement: you can move through occupied spaces as if they were Difficult Terrain, taking 1d10 Force damage if you end your turn in such a space. Shadow Resilience: you have Resistance to all damage except Force and Radiant damage. Strength of the Grave: if you would drop to 0 Hit Points and not die outright, you can make a Charisma saving throw (DC 5 plus half the damage taken); on a success your Hit Points instead change to a number equal to three times your Sorcerer level. Once you use this feature you can't use it again until you finish a Long Rest unless you spend 6 Sorcery Points (no action required) to restore your use of it."),
            ),
            book = HORROR),

        Subclass("hexblade_patron", "warlock", "Hexblade Patron",
            "A Warlock bound to a sentient magic weapon and the cursed forces within its blade, which bestows malignant curses and punishing blows.",
            listOf(
                f(3, "Hexblade Spells", "You always have these prepared once you reach the listed Warlock level: Arcane Vigor, Hex, Magic Weapon, Shield, and Wrathful Smite at 3; Conjure Barrage and Dispel Magic at 5; Freedom of Movement and Staggering Smite at 7; and Animate Objects and Steel Wind Strike at 9."),
                f(3, "Hexblade Manifest", "Hexblade's Curse: you can cast Hex without expending a spell slot a number of times equal to your Charisma modifier (minimum of once), regaining all expended uses when you finish a Long Rest. When you cast Hex, a spectral weapon resembling your patron orbits the cursed target. Hexblade's Maneuvers: once per turn, when you hit a target cursed by your Hex with an attack roll, you can cause one of these additional effects. Draining Slash: Constitution saving throw against your spell save DC; on a failed save the target can't make Opportunity Attacks and its Speed is halved until the start of your next turn. Harrowing Blade: Wisdom saving throw; on a failed save, the next time the target makes an attack roll against a creature other than you before the start of your next turn, the target takes Necrotic damage equal to your Charisma modifier. Stymying Mark: the target has Disadvantage on the next saving throw it makes before the start of your next turn."),
                f(6, "Life Stealer", "Hungering Hex: whenever the target cursed by your Hex drops to 0 Hit Points, you regain Hit Points equal to 1d8 plus your Charisma modifier. Inevitable Blade: once per turn, if you make an attack roll against the target cursed by your Hex and miss, you can deal Necrotic damage to that creature equal to your Charisma modifier (minimum of 1 Necrotic damage)."),
                f(10, "Armor of Hexes", "When you take damage from the cursed target of your Hex, you can take a Reaction to reduce the damage taken by an amount equal to 2d8 plus your Charisma modifier. You can use this feature a number of times equal to your Charisma modifier, regaining all expended uses when you finish a Long Rest."),
                f(14, "Masterful Hex", "Accursed Critical: any attack roll you make against the target cursed by your Hex scores a Critical Hit on a roll of 19 or 20 on the d20. Infectious Hex: when you use one of your Hexblade's Maneuvers, you can target one additional creature within 30 feet of the cursed target, and that additional target takes 1d6 Necrotic damage. Resilient Hex: taking damage can't break your Concentration on Hex."),
            ),
            book = HORROR),

        Subclass("undead_patron", "warlock", "Undead Patron",
            "A Warlock bound to a lich, vampire, or other entity that has defied the cycle of life and death, and shares the profane knowledge of that defiance.",
            listOf(
                f(3, "Form of Dread", "As a Bonus Action you transform into an avatar of your patron's dreadful power for 1 minute, until you have the Incapacitated condition, or until you end the form (no action required). You can transform a number of times equal to your Charisma modifier (minimum of once), regaining all expended uses when you finish a Long Rest. Facsimile of Life: you gain Temporary Hit Points equal to 1d10 plus your Warlock level. Frightful Avatar: you have Immunity to the Frightened condition, and once per turn when you hit a creature with an attack roll you can force it to make a Wisdom saving throw against your spell save DC; on a failed save the target has the Frightened condition until the end of your next turn."),
                f(3, "Undead Spells", "You always have these prepared once you reach the listed Warlock level: Blindness/Deafness, False Life, Phantasmal Force, and Ray of Sickness at 3; Speak with Dead and Vampiric Touch at 5; Death Ward and Phantasmal Killer at 7; and Antilife Shell and Cloudkill at 9."),
                f(6, "Grave Touched", "Arcane Necrosis: whenever you cast a spell or hit a creature with an attack roll and deal Necrotic damage, the damage dealt ignores Resistance to Necrotic damage. Additionally, once per turn when you cast a spell that deals damage while using your Form of Dread, you can change that spell's damage type to Necrotic. Undead Endurance: you don't gain Exhaustion levels from dehydration, malnutrition, or suffocation. In addition, you don't need to sleep, and magic can't put you to sleep."),
                f(10, "Necrotic Husk", "Necrotic Resilience: you have Resistance to Necrotic damage, and while using your Form of Dread you have Immunity to Necrotic damage. Unholy Resuscitation: if you drop to 0 Hit Points and don't die outright, you can cause your body to erupt with deathly energy. Each creature of your choice in a 30-foot Emanation originating from you makes a Constitution saving throw against your spell save DC, taking Necrotic damage equal to 2d10 plus your Warlock level on a failure or half as much on a success. Your Hit Points then change to 10 times your Charisma modifier (minimum of 10 Hit Points), and you gain 1 Exhaustion level. Once you use this benefit you can't use it again until you finish a Short or Long Rest."),
                f(14, "Superior Dread", "While using your Form of Dread you gain the following. Flight: you have a Fly Speed equal to your Speed and can hover. Profane Casting: whenever you cast a Warlock spell from the Conjuration or Necromancy school, you cast it without any Verbal, Somatic, or Material components, except Material components that are consumed by the spell or that have a cost specified in the spell. Vitality Siphon: once per turn when you deal Necrotic damage to a creature, you regain Hit Points equal to your Charisma modifier (minimum of 1 Hit Point)."),
            ),
            book = HORROR),
    )

    fun forClass(classId: String): List<Subclass> = ALL.filter { it.classId == classId }

    fun byId(id: String): Subclass? = ALL.find { it.id == id }
}
