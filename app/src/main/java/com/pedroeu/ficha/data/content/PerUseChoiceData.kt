package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.ChoiceOption

/**
 * A decision the rules have you make at the moment you use a feature, not once and for all.
 *
 * These were being asked during character creation, which quietly turned "choose an effect
 * each time you activate the cannon" into "pick one effect forever". An Artillerist who
 * answered Flamethrower at level 3 was never asked again, and nothing on the sheet suggested
 * they could do anything else with it.
 *
 * They live in their own table rather than hanging off the feature, because their whole point
 * is that they don't belong to the creation flow. A feature listed here presents its options
 * where the ability is used — on its Uses tracker and beside the feature itself — and what the
 * player picks is recorded as *what is happening now*, changeable as often as the rules allow.
 */
data class PerUseChoice(
    val id: String,
    /** What the decision is called, e.g. "Activate Cannon". */
    val label: String,
    /** The question asked at the moment of use. */
    val prompt: String,
    val options: List<ChoiceOption>,
    /** The feature this belongs to, shown as a caption. */
    val source: String,
    /** The pool the feature draws on, when it has one, so its tracker can ask. */
    val resourceId: String = "",
    /** Gating: the subclass that grants it, if any. */
    val subclassId: String = "",
    /** Gating: the species that grants it, if any. */
    val speciesId: String = "",
    /** The level in the granting class — or the character's level, for a species — it needs. */
    val minLevel: Int = 1,
)

object PerUseChoiceData {

    /** "…damage of your choice", which several of these are and nothing more. */
    private fun damageTypes(types: List<String>): List<ChoiceOption> =
        types.map { ChoiceOption(it.lowercase(), it, "The strike or the aura deals or turns aside $it damage.") }


    val ALL: List<PerUseChoice> = listOf(

        // ---------------------------------------------------------------- Barbarian
        PerUseChoice(
            id = "wild_heart:rage_of_the_wilds",
            label = "Rage of the Wilds",
            prompt = "Which animal answers this Rage? You choose again every time you rage.",
            source = "Rage of the Wilds",
            resourceId = "barbarian:rage",
            subclassId = "wild_heart",
            minLevel = 3,
            options = listOf(
                ChoiceOption(
                    "bear", "Bear",
                    "While your Rage is active, you have Resistance to every damage type except Force, Necrotic, Psychic, and Radiant.", "While raging",
                ),
                ChoiceOption(
                    "eagle", "Eagle",
                    "When you activate your Rage you can Disengage and Dash as part of that Bonus Action, and take both as a Bonus Action while it lasts.", "While raging",
                ),
                ChoiceOption(
                    "wolf", "Wolf",
                    "While your Rage is active, your allies have Advantage on attack rolls against any enemy of yours within 5 feet of you.", "While raging",
                ),
            ),
        ),
        PerUseChoice(
            id = "wild_heart:power_of_the_wilds",
            label = "Power of the Wilds",
            prompt = "And which power? A second choice on the same Rage, made again every time you rage.",
            source = "Power of the Wilds",
            resourceId = "barbarian:rage",
            subclassId = "wild_heart",
            minLevel = 14,
            options = listOf(
                ChoiceOption(
                    "falcon", "Falcon",
                    "While your Rage is active, you have a Fly Speed equal to your Speed if you aren't wearing any armor.", "While raging",
                ),
                ChoiceOption(
                    "lion", "Lion",
                    "While your Rage is active, every enemy within 5 feet of you has Disadvantage on attack rolls against targets other than you or another Barbarian who has this feature.", "While raging",
                ),
                ChoiceOption(
                    "ram", "Ram",
                    "While your Rage is active, you can cause a Large or smaller creature you hit to have the Prone condition.", "While raging",
                ),
            ),
        ),

        // ---------------------------------------------------------------- Cleric
        PerUseChoice(
            id = "arcana_domain:modify_magic",
            label = "Modify Magic",
            prompt = "How is this spell changed? You choose again each time you spend a use.",
            source = "Modify Magic",
            resourceId = "cleric:channel_divinity",
            subclassId = "arcana_domain",
            minLevel = 3,
            options = listOf(
                ChoiceOption(
                    "fortifying", "Fortifying Spell",
                    "One target of the spell gains Temporary Hit Points equal to your Wisdom modifier plus the spell's level.",
                ),
                ChoiceOption(
                    "piercing", "Piercing Spell",
                    "The spell ignores Resistance to the damage it deals.",
                ),
                ChoiceOption(
                    "reaching", "Reaching Spell",
                    "The spell's range increases by 30 feet, or its touch range becomes 30 feet.",
                ),
            ),
        ),

        // ---------------------------------------------------------------- Monk
        PerUseChoice(
            id = "elements:elemental_strikes",
            label = "Elemental Strikes",
            prompt = "Which element rides this strike? You choose again on every hit while " +
                "your Elemental Attunement is active.",
            source = "Elemental Attunement",
            subclassId = "elements",
            minLevel = 3,
            options = damageTypes(listOf("Acid", "Cold", "Fire", "Lightning", "Thunder")),
        ),
        PerUseChoice(
            id = "elements:elemental_epitome",
            label = "Elemental Epitome",
            prompt = "Which damage are you resisting? You choose again at the start of each " +
                "of your turns.",
            source = "Elemental Epitome",
            subclassId = "elements",
            minLevel = 17,
            options = damageTypes(listOf("Acid", "Cold", "Fire", "Lightning", "Thunder")),
        ),
        PerUseChoice(
            id = "open_hand:open_hand_technique",
            label = "Open Hand Technique",
            prompt = "What does this strike do? You choose again on every hit from your " +
                "Flurry of Blows.",
            source = "Open Hand Technique",
            subclassId = "open_hand",
            minLevel = 3,
            options = listOf(
                ChoiceOption(
                    "addle", "Addle",
                    "The target can't make Opportunity Attacks until the start of your next turn.", "On a hit",
                ),
                ChoiceOption(
                    "push", "Push",
                    "The target must succeed on a Strength saving throw or be pushed up to 15 feet away from you.", "On a hit",
                ),
                ChoiceOption(
                    "topple", "Topple",
                    "The target must succeed on a Dexterity saving throw or have the Prone condition.", "On a hit",
                ),
            ),
        ),

        // ---------------------------------------------------------------- Paladin
        PerUseChoice(
            id = "noble_genies:elemental_shielding",
            label = "Aura of Elemental Shielding",
            prompt = "Which damage does the aura turn aside? You choose again at the start of " +
                "each of your turns, no action required.",
            source = "Aura of Elemental Shielding",
            subclassId = "noble_genies",
            minLevel = 7,
            options = damageTypes(listOf("Acid", "Cold", "Fire", "Lightning", "Thunder")),
        ),

        // ---------------------------------------------------------------- Ranger
        PerUseChoice(
            id = "gloom_stalker:stalkers_flurry",
            label = "Stalker's Flurry",
            prompt = "Which extra effect rides this Dreadful Strike? You choose again each time.",
            source = "Stalker's Flurry",
            resourceId = "gloom_stalker:dreadful_strike",
            subclassId = "gloom_stalker",
            minLevel = 11,
            options = listOf(
                ChoiceOption(
                    "sudden_strike", "Sudden Strike",
                    "Make another attack with the same weapon against a different creature within reach or range.",
                ),
                ChoiceOption(
                    "mote_of_darkness", "Mote of Darkness",
                    "A 5-foot-radius Sphere of Darkness fills the target's space, and the target has Disadvantage on attack rolls while in it.",
                ),
            ),
        ),

        // ---------------------------------------------------------------- Sorcerer
        PerUseChoice(
            id = "aberrant:revelation_in_flesh",
            label = "Revelation in Flesh",
            prompt = "Which alteration is showing? One per Sorcery Point spent, and chosen " +
                "again each time you alter yourself.",
            source = "Revelation in Flesh",
            resourceId = "sorcerer:sorcery_points",
            subclassId = "aberrant",
            minLevel = 14,
            options = listOf(
                ChoiceOption(
                    "aquatic", "Aquatic Adaptation",
                    "You gain a Swim Speed equal to twice your Speed and can breathe underwater.",
                ),
                ChoiceOption(
                    "glistening", "Glistening Flesh",
                    "Your body is covered in mucus that lets you move across difficult terrain without the extra cost, and you cannot be Grappled or Restrained.",
                ),
                ChoiceOption(
                    "perception", "Perception of the Beyond",
                    "You gain Darkvision with a range of 60 feet and Blindsight with a range of 10 feet.",
                ),
                ChoiceOption(
                    "wormlike", "Wormlike Movement",
                    "Your body becomes slithering and boneless; you can move through a space as narrow as 1 inch and gain a Climb Speed equal to your Speed.",
                ),
            ),
        ),
        PerUseChoice(
            id = "demonic_sorcery:abyssal_rupture",
            label = "Abyssal Rupture",
            prompt = "What does the rupture do? You choose when you activate Innate Sorcery, " +
                "and again as a Bonus Action while it lasts.",
            source = "Abyssal Rupture",
            resourceId = "sorcerer:innate_sorcery",
            subclassId = "demonic_sorcery",
            minLevel = 3,
            options = listOf(
                ChoiceOption(
                    "demonic_lash", "Demonic Lash",
                    "Make a melee spell attack against a target within 5 feet of the rupture; on a hit it takes 1d8 Slashing damage, and a Large or smaller target can be pulled up to 10 feet toward the centre.",
                ),
                ChoiceOption(
                    "terrifying_screams", "Terrifying Screams",
                    "Each creature in the rupture must succeed on a Wisdom saving throw against your spell save DC or take 1d4 Psychic damage.",
                ),
            ),
        ),

        // ---------------------------------------------------------------- Warlock
        PerUseChoice(
            id = "archfey:steps_of_the_fey",
            label = "Steps of the Fey",
            prompt = "Which step is this? You choose again every time you cast Misty Step.",
            source = "Steps of the Fey",
            resourceId = "archfey:steps_of_the_fey",
            subclassId = "archfey",
            minLevel = 3,
            options = listOf(
                ChoiceOption(
                    "refreshing", "Refreshing Step",
                    "Immediately after you teleport, you or one creature you can see within 10 feet of yourself gains Temporary Hit Points equal to 1d10 plus your Charisma modifier.",
                ),
                ChoiceOption(
                    "taunting", "Taunting Step",
                    "Each creature of your choice within 5 feet of the space you left must succeed on a Wisdom saving throw against your spell save DC or have Disadvantage on attack rolls against creatures other than you until the start of your next turn.",
                ),
            ),
        ),

        // ---------------------------------------------------------------- Wizard
        PerUseChoice(
            id = "diviner:third_eye",
            label = "The Third Eye",
            prompt = "Which sense do you open? It lasts until you start a rest, and you choose " +
                "again the next time.",
            source = "The Third Eye",
            resourceId = "diviner:third_eye",
            subclassId = "diviner",
            minLevel = 10,
            options = listOf(
                ChoiceOption(
                    "darkvision", "Darkvision",
                    "You gain Darkvision with a range of 120 feet.",
                ),
                ChoiceOption(
                    "comprehension", "Greater Comprehension",
                    "You can read any language, however it is written or spoken.",
                ),
                ChoiceOption(
                    "see_invisibility", "See Invisibility",
                    "You can cast See Invisibility without expending a spell slot.",
                ),
            ),
        ),

        // ---------------------------------------------------------------- Fighter
        PerUseChoice(
            id = "hell_knight:hell_forged_weapon",
            label = "Hell-Forged Weapon",
            prompt = "Which hellfire is in the blade? You choose again each time you imbue " +
                "the weapon.",
            source = "Hell-Forged Weapon",
            subclassId = "hell_knight",
            minLevel = 3,
            options = damageTypes(listOf("Cold", "Fire", "Necrotic")),
        ),

        PerUseChoice(
            id = "spiritual_guardian:spiritual_protectors",
            label = "Spiritual Protectors",
            prompt = "What do the spirits do on this hit? You choose again on every hit you " +
                "land while your Rage is active.",
            source = "Spiritual Protectors",
            // No pool. "While your Rage is active, when you hit a creature … it suffers one of
            // the following effects of your choice" — the choice belongs to the hit and costs
            // nothing. Hanging it off the Rage tracker asked it once, at the moment you raged,
            // which turned three effects per hit into one effect per Rage.
            subclassId = "spiritual_guardian",
            minLevel = 3,
            options = listOf(
                ChoiceOption(
                    "distract", "Distract",
                    "Until the start of your next turn, the target has Disadvantage on " +
                        "attack rolls against targets other than you or another Barbarian " +
                        "who has this feature.",
                    "On a hit",
                ),
                ChoiceOption(
                    "protect", "Protect",
                    "Until the end of the target's next turn, the next time it hits a " +
                        "creature other than you with an attack roll, that creature has " +
                        "Resistance to the damage dealt by the attack.",
                    "On a hit",
                ),
                ChoiceOption(
                    "strike", "Strike",
                    "The target takes an extra 1d6 damage, which can be Acid, Cold, Fire, " +
                        "Force, Lightning, or Thunder damage (your choice).",
                    "On a hit",
                ),
            ),
        ),

        // ---------------------------------------------------------------- Artificer
        PerUseChoice(
            id = "artillerist:cannon_activation",
            label = "Activate Cannon",
            prompt = "What is the cannon doing? You choose again each time you activate it.",
            source = "Eldritch Cannon",
            resourceId = "artillerist:eldritch_cannon",
            subclassId = "artillerist",
            minLevel = 3,
            options = listOf(
                ChoiceOption(
                    "flamethrower", "Flamethrower",
                    "The cannon exhales fire in an adjacent 15-foot Cone that you designate. " +
                        "Each creature in that area makes a Dexterity saving throw against " +
                        "your spell save DC, taking 2d8 Fire damage on a failed save or half " +
                        "as much damage on a successful one. The fire ignites any flammable " +
                        "objects in the area that aren't being worn or carried.",
                    "Bonus Action",
                ),
                ChoiceOption(
                    "force_ballista", "Force Ballista",
                    "Make a ranged spell attack, originating from the cannon, at one creature " +
                        "or object within 120 feet of it. On a hit, the target takes 2d8 Force " +
                        "damage, and if the target is a creature, it is pushed up to 5 feet " +
                        "away from the cannon.",
                    "Bonus Action",
                ),
                ChoiceOption(
                    "protector", "Protector",
                    "The cannon emits a burst of positive energy that grants itself and each " +
                        "creature of your choice within 10 feet of it a number of Temporary " +
                        "Hit Points equal to 1d8 plus your Intelligence modifier (minimum of " +
                        "one Temporary Hit Point).",
                    "Bonus Action",
                ),
            ),
        ),
        PerUseChoice(
            id = "battle_smith:arcane_jolt",
            label = "Arcane Jolt",
            prompt = "Which energy do you channel? You choose again each time.",
            source = "Arcane Jolt",
            resourceId = "battle_smith:arcane_jolt",
            subclassId = "battle_smith",
            minLevel = 9,
            options = listOf(
                ChoiceOption(
                    "destructive", "Destructive Energy",
                    "The target takes an extra 2d6 Force damage. This increases to 4d6 at " +
                        "Artificer level 15.",
                    "1 use",
                ),
                ChoiceOption(
                    "restorative", "Restorative Energy",
                    "Choose one creature or object you can see within 30 feet of the target. " +
                        "Healing flows into it, restoring 2d6 Hit Points. This increases to " +
                        "4d6 at Artificer level 15.",
                    "1 use",
                ),
            ),
        ),

        PerUseChoice(
            id = "reanimator:modification",
            label = "Strange Modifications",
            prompt = "Which modification does this companion have? You choose again whenever " +
                "you create a new one.",
            source = "Strange Modifications",
            resourceId = "reanimator:companion",
            subclassId = "reanimator",
            minLevel = 5,
            options = listOf(
                ChoiceOption(
                    "arcane_conduit", "Arcane Conduit",
                    "You can cast spells as though you were in the companion's space, but you " +
                        "must use your own senses. Once per turn, when you cast an Artificer " +
                        "spell from the Evocation or Necromancy schools and deal damage while " +
                        "your companion is within 120 feet of you, you can add your " +
                        "Intelligence modifier to one damage roll of that spell.",
                ),
                ChoiceOption(
                    "ferocity", "Ferocity",
                    "When you command your companion to take the Dreadful Swipe action, the " +
                        "companion can use it twice.",
                ),
            ),
        ),
        PerUseChoice(
            id = "reanimator:improved_modification",
            label = "Improved Reanimation",
            prompt = "Which further modification does this companion have? You choose " +
                "again whenever you create a new one.",
            source = "Macabre Modifications",
            resourceId = "reanimator:companion",
            subclassId = "reanimator",
            minLevel = 9,
            options = listOf(
                ChoiceOption(
                    "bloated", "Bloated",
                    "The companion becomes Large or Medium (your choice). Whenever it hits a " +
                        "Large or smaller creature with the Dreadful Swipe action, that " +
                        "creature can also be pushed up to 10 feet away from the companion. " +
                        "Additionally, you can add your Intelligence modifier to the damage " +
                        "dealt by the companion's Death Burst.",
                ),
                ChoiceOption(
                    "gaunt", "Gaunt",
                    "The companion's Speed increases to 45 feet, and it gains a Climb Speed " +
                        "equal to its Speed, able to climb difficult surfaces including " +
                        "ceilings without an ability check. Whenever a creature of your choice " +
                        "starts its turn within a 10-foot Emanation originating from your " +
                        "companion, the creature must succeed on a Wisdom saving throw against " +
                        "your spell save DC or have the Frightened condition until the start " +
                        "of its next turn.",
                ),
                ChoiceOption(
                    "moist", "Moist",
                    "The companion gains a Swim Speed equal to its Speed. In addition, " +
                        "whenever it is hit by an attack roll from a creature within 10 feet " +
                        "of it, the attacker takes Acid damage equal to your Intelligence " +
                        "modifier.",
                ),
            ),
        ),

        // ---------------------------------------------------------------- Druid
        PerUseChoice(
            id = "stars:starry_form",
            label = "Starry Form",
            prompt = "Which constellation do you take on? You choose each time you assume the form.",
            source = "Starry Form",
            resourceId = "druid:wild_shape",
            subclassId = "stars",
            minLevel = 3,
            options = listOf(
                ChoiceOption(
                    "archer", "Archer",
                    "A luminous bow appears in your hand. Immediately after you assume the " +
                        "form, and as a Bonus Action on your later turns, you can make a " +
                        "ranged spell attack against one creature within 60 feet of yourself, " +
                        "dealing 1d8 plus your Wisdom modifier Radiant damage on a hit.",
                    "Bonus Action",
                ),
                ChoiceOption(
                    "chalice", "Chalice",
                    "A shimmering cup appears in your hand. Immediately after you assume the " +
                        "form, and whenever you cast a spell that restores Hit Points, you or " +
                        "another creature you can see within 30 feet of yourself regains Hit " +
                        "Points equal to 1d8 plus your Wisdom modifier.",
                    "On a healing spell",
                ),
                ChoiceOption(
                    "dragon", "Dragon",
                    "A star-filled dragon's head appears above yours. When you make an " +
                        "Intelligence or Wisdom check or a Constitution saving throw to " +
                        "maintain Concentration, treat a roll of 9 or lower on the d20 as a 10.",
                    "Passive",
                ),
            ),
        ),

        // ---------------------------------------------------------------- Paladin
        PerUseChoice(
            id = "noble_genies:elemental_smite",
            label = "Elemental Smite",
            prompt = "Whose power do you invoke? You choose again each time.",
            source = "Elemental Smite",
            resourceId = "paladin:channel_divinity",
            subclassId = "noble_genies",
            minLevel = 3,
            options = listOf(
                ChoiceOption(
                    "dao", "Dao's Crush",
                    "Earth rises up around the target of your Divine Smite. The target has " +
                        "the Grappled condition, with an escape DC equal to your spell save " +
                        "DC. While Grappled this way, the target also has the Restrained " +
                        "condition.",
                    "1 Channel Divinity",
                ),
                ChoiceOption(
                    "djinni", "Djinni's Escape",
                    "You teleport to an unoccupied space you can see within 30 feet of " +
                        "yourself and take on a semi-incorporeal form until the end of your " +
                        "next turn. While in this form you have Resistance to Bludgeoning, " +
                        "Piercing, and Slashing damage, and Immunity to the Grappled, Prone, " +
                        "and Restrained conditions.",
                    "1 Channel Divinity",
                ),
                ChoiceOption(
                    "efreeti", "Efreeti's Fury",
                    "The target of your Divine Smite takes an extra 2d4 Fire damage, and fire " +
                        "jumps from the target to another creature you can see within 30 feet " +
                        "of yourself. That second creature also takes 2d4 Fire damage.",
                    "1 Channel Divinity",
                ),
                ChoiceOption(
                    "marid", "Marid's Surge",
                    "The target of your Divine Smite and each creature of your choice in a " +
                        "10-foot Emanation originating from you make a Strength saving throw " +
                        "against your spell save DC. On a failed save, a creature is pushed 15 " +
                        "feet straight away from you and has the Prone condition.",
                    "1 Channel Divinity",
                ),
            ),
        ),

        // ---------------------------------------------------------------- Sorcerer
        PerUseChoice(
            id = "spellfire_sorcery:spellfire_burst",
            label = "Spellfire Burst",
            prompt = "Which effect do you unleash? You choose again each time you burst.",
            source = "Spellfire Burst",
            resourceId = "sorcerer:sorcery_points",
            subclassId = "spellfire_sorcery",
            minLevel = 3,
            options = listOf(
                ChoiceOption(
                    "bolstering_flames", "Bolstering Flames",
                    "You or one creature you can see within 30 feet of yourself gains " +
                        "Temporary Hit Points equal to 1d4 plus your Charisma modifier. At " +
                        "Sorcerer level 14 you add your Sorcerer level to these Temporary Hit " +
                        "Points.",
                    "No extra cost",
                ),
                ChoiceOption(
                    "radiant_fire", "Radiant Fire",
                    "One creature you can see within 30 feet of yourself takes 1d4 Fire or " +
                        "Radiant damage (your choice). At Sorcerer level 14 this damage " +
                        "increases to 1d8.",
                    "No extra cost",
                ),
            ),
        ),
        PerUseChoice(
            id = "demonic_sorcery:abyssal_layer",
            label = "Abyssal Realm",
            prompt = "Which layer of the Abyss do you draw on? You choose again each time.",
            source = "Abyssal Realm",
            resourceId = "sorcerer:sorcery_points",
            subclassId = "demonic_sorcery",
            minLevel = 6,
            options = listOf(
                ChoiceOption(
                    "gaping_maw", "Gaping Maw's Frenzy",
                    "Designate a direction that is horizontal to you. Each creature in the " +
                        "area that fails a Charisma saving throw must use as much of its " +
                        "movement as possible to move in that direction at the start of its " +
                        "next turn, taking the safest route.",
                    "Charisma save",
                ),
                ChoiceOption(
                    "maze_of_azzatar", "Maze of Azzatar",
                    "Each creature in the area makes an Intelligence saving throw. On a failed " +
                        "save, you gain the benefits of the Invisible condition against the " +
                        "target until the start of your next turn.",
                    "Intelligence save",
                ),
                ChoiceOption(
                    "slime_pits", "Slime Pits' Haze",
                    "Each creature in the area makes a Constitution saving throw. On a failed " +
                        "save, the target has your choice of the Charmed or Poisoned condition " +
                        "until the start of your next turn.",
                    "Constitution save",
                ),
            ),
        ),

        // ---------------------------------------------------------------- Fighter
        PerUseChoice(
            id = "hell_knight:advanced_wound",
            label = "Advanced Wounds",
            prompt = "Which effect does the wound take? You choose again each time you roll the die.",
            source = "Advanced Wounds",
            // Rolling the Infernal Wound Die isn't itself a use of the pool, so this records
            // what the wound is doing without spending anything.
            subclassId = "hell_knight",
            minLevel = 7,
            options = listOf(
                ChoiceOption(
                    "purulence", "Purulence of Minauros",
                    "Caustic pus erupts from the wound. Each enemy in a 5-foot Emanation " +
                        "originating from the target takes Acid damage equal to your " +
                        "Constitution modifier, and the target has the Poisoned condition " +
                        "until the end of its next turn. Devil's Luck: each creature that " +
                        "takes this Acid damage has a -1 penalty to its AC until the end of " +
                        "your next turn.",
                    "Acid",
                ),
                ChoiceOption(
                    "rupture", "Rupture of Cania",
                    "The wound ruptures with a spurt of arcane energy. The target takes Force " +
                        "damage equal to your Constitution modifier. Devil's Luck: the target " +
                        "subtracts 1d6 from the next saving throw it makes before the end of " +
                        "your next turn.",
                    "Force",
                ),
                ChoiceOption(
                    "gangrene", "Stygian Gangrene",
                    "Infernal rime spreads from the wound. The target takes Cold damage equal " +
                        "to your Constitution modifier and can't take Reactions until the " +
                        "start of its next turn. Devil's Luck: the target's Speed is halved " +
                        "until the end of its next turn.",
                    "Cold",
                ),
            ),
        ),

        // ---------------------------------------------------------------- Warlock
        PerUseChoice(
            id = "hexblade_patron:maneuver",
            label = "Hexblade's Maneuvers",
            prompt = "Which effect do you cause? You choose again each time you hit the cursed target.",
            source = "Hexblade Manifest",
            // Once per turn on a hit, which costs nothing from the Curse pool.
            subclassId = "hexblade_patron",
            minLevel = 3,
            options = listOf(
                ChoiceOption(
                    "draining_slash", "Draining Slash",
                    "The target makes a Constitution saving throw against your spell save DC. " +
                        "On a failed save, the target can't make Opportunity Attacks and its " +
                        "Speed is halved until the start of your next turn.",
                    "Constitution save",
                ),
                ChoiceOption(
                    "harrowing_blade", "Harrowing Blade",
                    "The target makes a Wisdom saving throw against your spell save DC. On a " +
                        "failed save, the next time the target makes an attack roll against a " +
                        "creature other than you before the start of your next turn, the " +
                        "target takes Necrotic damage equal to your Charisma modifier.",
                    "Wisdom save",
                ),
                ChoiceOption(
                    "stymying_mark", "Stymying Mark",
                    "The target has Disadvantage on the next saving throw it makes before the " +
                        "start of your next turn.",
                    "No save",
                ),
            ),
        ),

        // ---------------------------------------------------------------- Aasimar
        PerUseChoice(
            id = "duskling:inner_magic",
            label = "Inner Magic",
            prompt = "Which benefit is your inner magic holding? Chosen on a Long Rest and " +
                "switched again on a Bonus Action, as often as your Proficiency Bonus allows.",
            source = "Inner Magic",
            resourceId = "duskling:inner_magic",
            speciesId = "duskling",
            options = listOf(
                ChoiceOption(
                    "duskling_ardor", "Ardor",
                    "You have Advantage on Charisma checks and on saving throws to avoid or " +
                        "end the Frightened condition.",
                ),
                ChoiceOption(
                    "duskling_mobility", "Mobility",
                    "Your Speed increases by 10 feet, and climbing and swimming don't cost " +
                        "you extra movement.",
                ),
                ChoiceOption(
                    "duskling_vigor", "Vigor",
                    "Temporary Hit Points equal to your Proficiency Bonus, and Advantage on " +
                        "Strength (Athletics) and Dexterity (Acrobatics) checks.",
                ),
            ),
        ),
        PerUseChoice(
            id = "dhampir:vampiric_bite",
            label = "Vampiric Bite",
            prompt = "What does the bite take from them? You choose again with every bite.",
            source = "Vampiric Bite",
            resourceId = "dhampir:vampiric_bite",
            speciesId = "dhampir",
            options = listOf(
                ChoiceOption(
                    "heal", "Regain Hit Points",
                    "You regain Hit Points equal to the Piercing damage the bite dealt.",
                    "On a bite",
                ),
                ChoiceOption(
                    "bonus", "Empowering Bonus",
                    "You gain a bonus equal to the Piercing damage dealt to the next ability " +
                        "check or attack roll you make.",
                    "On a bite",
                ),
            ),
        ),
        PerUseChoice(
            id = "aasimar:celestial_revelation",
            label = "Celestial Revelation",
            prompt = "Which revelation do you take on? You choose each time you transform.",
            source = "Celestial Revelation",
            resourceId = "aasimar:celestial_revelation",
            speciesId = "aasimar",
            minLevel = 3,
            options = listOf(
                ChoiceOption(
                    "necrotic_shroud", "Necrotic Shroud",
                    "Your eyes briefly become pools of darkness, and ghostly, flightless wings " +
                        "sprout from your back. Each creature of your choice within 10 feet of " +
                        "you must succeed on a Charisma saving throw (DC 8 plus your Charisma " +
                        "modifier and Proficiency Bonus) or have the Frightened condition " +
                        "until the end of your next turn. Once per turn while the " +
                        "transformation lasts, you can deal extra Necrotic damage equal to " +
                        "your Proficiency Bonus when you hit with an attack roll.",
                    "1 minute",
                ),
                ChoiceOption(
                    "radiant_consumption", "Radiant Consumption",
                    "Searing light temporarily radiates from your eyes and mouth. For the " +
                        "duration you shed Bright Light in a 10-foot radius and Dim Light for " +
                        "an additional 10 feet, and at the end of each of your turns each " +
                        "creature within 10 feet of you takes Radiant damage equal to your " +
                        "Proficiency Bonus. Once per turn you can deal extra Radiant damage " +
                        "equal to your Proficiency Bonus when you hit with an attack roll.",
                    "1 minute",
                ),
                ChoiceOption(
                    "radiant_soul", "Radiant Soul",
                    "Two luminous, spectral wings sprout from your back. For the duration you " +
                        "have a Fly Speed equal to your Speed, and once per turn you can deal " +
                        "extra Radiant damage equal to your Proficiency Bonus when you hit " +
                        "with an attack roll.",
                    "1 minute",
                ),
            ),
        ),

        // ---------------------------------------------------------------- Warrior of Venom
        PerUseChoice(
            id = "warrior_of_venom:envenom_weapon",
            label = "Envenom Weapon",
            prompt = "Which toxin do you draw? You choose again each time you apply one.",
            source = "Envenom Weapon",
            resourceId = "monk:focus",
            subclassId = "warrior_of_venom",
            minLevel = 3,
            options = listOf(
                ChoiceOption(
                    "slowing", "Slowing Toxin",
                    "Until the start of your next turn, the target's Speed is halved; it " +
                        "can't take Reactions; and it can take either an action or a Bonus " +
                        "Action on its turn, not both.",
                    "1 Focus Point",
                ),
                ChoiceOption(
                    "venom", "Venom",
                    "The target takes Poison damage equal to two rolls of your Martial Arts " +
                        "die. From Monk level 11, whenever you have been subjected to Poison " +
                        "damage, this deals a further roll of your Martial Arts die.",
                    "1 Focus Point",
                ),
            ),
        ),
        PerUseChoice(
            id = "warrior_of_venom:toxic_touch",
            label = "Toxic Touch",
            prompt = "Which toxin is this? You choose again each time you use it.",
            source = "Toxic Touch",
            resourceId = "monk:focus",
            subclassId = "warrior_of_venom",
            minLevel = 6,
            options = listOf(
                ChoiceOption(
                    "intoxicant", "Intoxicant",
                    "The target has the Charmed condition for the duration or until you or " +
                        "your allies deal damage to the target.",
                    "1 Focus Point",
                ),
                ChoiceOption(
                    "sedative", "Sedative",
                    "The creature falls asleep and has the Unconscious condition for the " +
                        "duration. Another creature can use an action to shake it awake and " +
                        "remove the condition.",
                    "1 Focus Point",
                ),
                ChoiceOption(
                    "truth_serum", "Truth Serum",
                    "The target can't knowingly communicate a lie for the duration.",
                    "1 Focus Point",
                ),
            ),
        ),
    )

    private val byId: Map<String, PerUseChoice> = ALL.associateBy { it.id }

    fun byId(id: String): PerUseChoice? = byId[id]

    /** The subclasses and species this table names, so a test can check every one is real. */
    fun sourceIds(): List<Pair<String, String>> = ALL.flatMap { choice ->
        buildList {
            if (choice.subclassId.isNotBlank()) add("subclass" to choice.subclassId)
            if (choice.speciesId.isNotBlank()) add("species" to choice.speciesId)
            if (choice.resourceId.isNotBlank()) add("resource" to choice.resourceId)
        }
    }
}
