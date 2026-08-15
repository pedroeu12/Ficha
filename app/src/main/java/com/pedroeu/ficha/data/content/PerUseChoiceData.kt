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

    val ALL: List<PerUseChoice> = listOf(

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
            source = "Improved Reanimation",
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
