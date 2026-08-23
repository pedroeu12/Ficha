package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.ResourceOption

/**
 * The named abilities a limited-use pool pays for, with their full rules text.
 *
 * These are the ones the rules *grant* rather than ask you to pick — every Monk has Flurry of
 * Blows, Patient Defense, and Step of the Wind. Options the player chooses instead (Metamagic,
 * maneuvers, Arcane Shot) live on their [com.pedroeu.ficha.data.model.Choice] and are merged in
 * by [com.pedroeu.ficha.domain.CharacterResources].
 */
object ResourceOptionData {

    private fun option(
        id: String,
        name: String,
        cost: String,
        action: String,
        description: String,
        unlockLevel: Int = 1,
    ) = ResourceOption(
        id = id,
        name = name,
        cost = cost,
        actionType = action,
        description = description,
        unlockLevel = unlockLevel,
    )

    // ------------------------------------------------------------------ Monk

    private val MONK_FOCUS = listOf(
        option(
            "flurry_of_blows", "Flurry of Blows", "1 Focus Point", "Bonus Action",
            "You can expend 1 Focus Point to make two Unarmed Strikes as a Bonus Action. " +
                "The number of strikes increases to three when you reach Monk level 10.",
        ),
        option(
            "patient_defense", "Patient Defense", "1 Focus Point", "Bonus Action",
            "You can take the Disengage action as a Bonus Action. Alternatively, you can " +
                "expend 1 Focus Point to take both the Disengage and the Dodge actions as a " +
                "single Bonus Action.",
        ),
        option(
            "step_of_the_wind", "Step of the Wind", "1 Focus Point", "Bonus Action",
            "You can take the Dash action as a Bonus Action. Alternatively, you can expend " +
                "1 Focus Point to take both the Disengage and the Dash actions as a single " +
                "Bonus Action, and your jump distance is doubled for the turn.",
        ),
        option(
            "stunning_strike", "Stunning Strike", "1 Focus Point", "Part of an attack",
            "Once per turn when you hit a creature with a Monk weapon or an Unarmed Strike, " +
                "you can expend 1 Focus Point to attempt a stunning strike. The target must " +
                "make a Constitution saving throw against your Monk save DC. On a failed save, " +
                "it has the Stunned condition until the start of your next turn. On a " +
                "successful save, its Speed is halved until the start of your next turn, and " +
                "the next attack roll against it has Advantage.",
            unlockLevel = 5,
        ),
        option(
            "deflect_attacks", "Deflect Attacks", "None (Reaction)", "Reaction",
            "When an attack roll hits you and it deals Bludgeoning, Piercing, or Slashing " +
                "damage, you can take a Reaction to reduce the damage by 1d10 plus your " +
                "Dexterity modifier and your Monk level. If you reduce the damage to 0, you " +
                "can expend 1 Focus Point to redirect it: make a ranged attack with a Monk " +
                "weapon or an Unarmed Strike against a creature within 60 feet.",
            unlockLevel = 3,
        ),
    )

    private val MERCY_FOCUS = listOf(
        option(
            "hand_of_harm", "Hand of Harm", "1 Focus Point", "Part of an attack",
            "Once per turn when you hit a creature with an Unarmed Strike, you can expend 1 " +
                "Focus Point to deal extra Necrotic damage equal to one roll of your Martial " +
                "Arts die plus your Wisdom modifier.",
            unlockLevel = 3,
        ),
        option(
            "hand_of_healing", "Hand of Healing", "1 Focus Point", "Magic action",
            "As a Magic action, you can expend 1 Focus Point to touch a creature and restore " +
                "a number of Hit Points equal to a roll of your Martial Arts die plus your " +
                "Wisdom modifier. When you use Flurry of Blows, you can replace one of the " +
                "Unarmed Strikes with a use of this feature without spending a Focus Point.",
            unlockLevel = 3,
        ),
    )

    private val SHADOW_FOCUS = listOf(
        option(
            "shadow_arts_darkness", "Shadow Arts: Darkness", "1 Focus Point", "Magic action",
            "You can expend 1 Focus Point to cast the Darkness spell without spell " +
                "components. You can see within the spell's area, and you can move it 30 feet " +
                "as a Bonus Action on your turn.",
            unlockLevel = 3,
        ),
        option(
            "shadow_step", "Shadow Step", "1 Focus Point", "Bonus Action",
            "While entirely within Dim Light or Darkness, you can expend 1 Focus Point as a " +
                "Bonus Action to teleport up to 60 feet to an unoccupied space you can see " +
                "that is also in Dim Light or Darkness. You then have Advantage on the next " +
                "melee attack you make before the end of the turn.",
            unlockLevel = 6,
        ),
    )

    private val ELEMENTS_FOCUS = listOf(
        option(
            "elemental_attunement", "Elemental Attunement", "1 Focus Point", "Bonus Action",
            "As a Bonus Action, you can expend 1 Focus Point to channel an element for 10 " +
                "minutes. Your reach with Unarmed Strikes increases by 10 feet, those strikes " +
                "can deal your chosen elemental damage type instead of their normal type, and " +
                "once per turn a hit can push the target 10 feet or pull it 10 feet closer.",
            unlockLevel = 3,
        ),
    )

    private val OPEN_HAND_FOCUS = listOf(
        option(
            "open_hand_technique", "Open Hand Technique", "None (with Flurry)", "Part of Flurry of Blows",
            "Whenever you hit a creature with an attack granted by your Flurry of Blows, you " +
                "can impose one of the following effects on that target: Addle, so it can't " +
                "make Opportunity Attacks until the start of your next turn; Push, forcing a " +
                "Strength save or being pushed up to 15 feet away; or Topple, forcing a " +
                "Dexterity save or falling Prone.",
            unlockLevel = 3,
        ),
    )

    // ------------------------------------------------------------------ Sorcerer

    private val FONT_OF_MAGIC = listOf(
        option(
            "create_spell_slot", "Create Spell Slot", "2-7 Sorcery Points", "Bonus Action",
            "As a Bonus Action, you can transform unexpended Sorcery Points into one spell " +
                "slot. The created slots vanish when you finish a Long Rest. " +
                "Level 1 costs 2 points, level 2 costs 3, level 3 costs 5, level 4 costs 6, " +
                "and level 5 costs 7.",
            unlockLevel = 2,
        ),
        option(
            "convert_spell_slot", "Convert Spell Slot to Sorcery Points", "None", "Bonus Action",
            "As a Bonus Action, you can expend one spell slot to gain a number of Sorcery " +
                "Points equal to the slot's level.",
            unlockLevel = 2,
        ),
    )

    // ------------------------------------------------------------------ Cleric and Paladin

    private val CLERIC_CHANNEL_DIVINITY = listOf(
        option(
            "divine_spark", "Divine Spark", "1 use", "Magic action",
            "As a Magic action, you point your Holy Symbol and channel positive or negative " +
                "energy at a creature within 30 feet. Roll a number of d8s equal to half your " +
                "Cleric level (round up). You either restore Hit Points equal to that total " +
                "plus your Wisdom modifier, or force a Constitution saving throw for that much " +
                "Radiant or Necrotic damage, halved on a success.",
            unlockLevel = 2,
        ),
        option(
            "turn_undead", "Turn Undead", "1 use", "Magic action",
            "As a Magic action, you censure Undead. Each Undead within 30 feet that can see " +
                "or hear you makes a Wisdom saving throw against your spell save DC. On a " +
                "failed save it has the Frightened and Incapacitated conditions and must spend " +
                "its turns moving away from you, for 1 minute or until it takes any damage.",
            unlockLevel = 2,
        ),
    )

    private val PALADIN_CHANNEL_DIVINITY = listOf(
        option(
            "divine_sense", "Divine Sense", "1 use", "Bonus Action",
            "As a Bonus Action, you can sense the presence of strong evil and good. For 10 " +
                "minutes you know the location of any Celestial, Fiend, or Undead within 60 " +
                "feet that is not behind Total Cover, and you know its creature type.",
            unlockLevel = 3,
        ),
    )

    // ------------------------------------------------------------------ Fighter subclasses

    private val PSI_WARRIOR_DICE = listOf(
        option(
            "protective_field", "Protective Field", "1 Psionic Energy Die", "Reaction",
            "When you or a creature you can see within 30 feet takes damage, you can take a " +
                "Reaction to expend one Psionic Energy Die and reduce that damage by the " +
                "number rolled plus your Intelligence modifier (minimum reduction of 1).",
            unlockLevel = 3,
        ),
        option(
            "psionic_strike", "Psionic Strike", "1 Psionic Energy Die", "Part of an attack",
            "Once per turn, when you hit a target within 30 feet with an attack using a " +
                "weapon, you can expend one Psionic Energy Die to deal extra Force damage " +
                "equal to the number rolled plus your Intelligence modifier.",
            unlockLevel = 3,
        ),
        option(
            "telekinetic_movement", "Telekinetic Movement", "1 Psionic Energy Die", "Magic action",
            "As a Magic action, you can move one Large or smaller object, or one willing " +
                "creature, up to 30 feet to an unoccupied space you can see. You can also do " +
                "this without expending a die once per Short or Long Rest.",
            unlockLevel = 3,
        ),
    )

    private val SOULKNIFE_DICE = listOf(
        option(
            "psi_bolstered_knack", "Psi-Bolstered Knack", "1 Psionic Energy Die", "None",
            "When you fail an ability check using a skill or tool with which you have " +
                "proficiency, you can expend one Psionic Energy Die and add the number rolled " +
                "to the check, potentially turning the failure into a success.",
            unlockLevel = 3,
        ),
        option(
            "psychic_whispers", "Psychic Whispers", "1 Psionic Energy Die", "Magic action",
            "As a Magic action, choose one or more creatures you can see, up to your " +
                "Proficiency Bonus, and establish telepathic communication with them for a " +
                "number of hours equal to the number rolled on the die. The first use after " +
                "each Long Rest costs no die.",
            unlockLevel = 3,
        ),
    )

    // ------------------------------------------------------------------ Barbarian

    private val RAGE_OPTIONS = listOf(
        option(
            "rage_damage", "Rage Damage", "Included", "While raging",
            "When you make an attack using Strength and deal damage, you add a bonus: +2 at " +
                "Barbarian level 1, +3 at level 9, and +4 at level 16.",
        ),
        option(
            "rage_resistance", "Rage Resistance", "Included", "While raging",
            "You have Resistance to Bludgeoning, Piercing, and Slashing damage for the " +
                "duration of the Rage.",
        ),
        option(
            "rage_advantage", "Strength Advantage", "Included", "While raging",
            "You have Advantage on Strength checks and Strength saving throws while raging.",
        ),
        option(
            "reckless_attack", "Reckless Attack", "None", "Part of the Attack action",
            "When you make your first attack roll on your turn, you can decide to attack " +
                "recklessly. Doing so gives you Advantage on Strength-based attack rolls this " +
                "turn, but attack rolls against you have Advantage until your next turn.",
            unlockLevel = 2,
        ),
    )

    // ------------------------------------------------------------------ Druid

    private val WILD_SHAPE_OPTIONS = listOf(
        option(
            "wild_shape_transform", "Transform", "1 use", "Bonus Action",
            "As a Bonus Action, you shape-shift into a Beast form you have learned. You keep " +
                "your mental ability scores, your personality, and your ability to speak if " +
                "the form allows it, and you gain the form's Speed, senses, and physical " +
                "ability scores. You revert when the duration ends, you take a Bonus Action to " +
                "do so, or you drop to 0 Hit Points.",
            unlockLevel = 2,
        ),
        option(
            "wild_companion", "Wild Companion", "1 use", "Magic action",
            "You can expend a use of Wild Shape to cast Find Familiar without material " +
                "components. The familiar is a Fey rather than its normal type and vanishes " +
                "after a number of hours equal to half your Druid level.",
            unlockLevel = 2,
        ),
    )

    // ------------------------------------------------------------------ Fighter

    private val SECOND_WIND_OPTIONS = listOf(
        option(
            "second_wind_heal", "Regain Hit Points", "1 use", "Bonus Action",
            "As a Bonus Action, you regain Hit Points equal to 1d10 plus your Fighter level.",
        ),
        option(
            "tactical_mind", "Tactical Mind", "1 use", "None",
            "When you fail an ability check, you can expend a use of Second Wind to add 1d10 " +
                "to the check. If that still fails, the use is not spent.",
            unlockLevel = 2,
        ),
        option(
            "tactical_shift", "Tactical Shift", "Included", "With Second Wind",
            "Whenever you activate Second Wind, you can move up to half your Speed without " +
                "provoking Opportunity Attacks.",
            unlockLevel = 5,
        ),
    )

    // ------------------------------------------------------------------ Bard

    private val BARDIC_INSPIRATION_OPTIONS = listOf(
        option(
            "inspire", "Inspire an Ally", "1 use", "Bonus Action",
            "As a Bonus Action, you can inspire a creature other than yourself within 60 feet " +
                "that can hear you. It gains a Bardic Inspiration die, a d6, which lasts until " +
                "it is used or you finish a Long Rest. Within the next hour the creature can " +
                "roll the die and add it to one ability check, attack roll, or damage roll, " +
                "after seeing the roll but before knowing the outcome. The die grows to a d8 " +
                "at Bard level 5, a d10 at level 10, and a d12 at level 15.",
        ),
    )

    // ------------------------------------------------------------------ Paladin

    private val LAY_ON_HANDS_OPTIONS = listOf(
        option(
            "lay_on_hands_heal", "Restore Hit Points", "1+ points", "Bonus Action",
            "As a Bonus Action, you can touch a creature and draw from the pool to restore a " +
                "number of Hit Points to it, up to the number remaining in the pool.",
        ),
        option(
            "lay_on_hands_poison", "Cure Poison", "5 points", "Bonus Action",
            "You can expend 5 Hit Points from the pool to remove the Poisoned condition from " +
                "the creature you touch, instead of restoring Hit Points to it.",
        ),
    )

    // ------------------------------------------------------------------ Lookup

    /** Options granted automatically, keyed by the resource id they are spent from. */
    /** The Alchemist rolls on this table for each elixir, or picks from it by spending a slot. */
    private val EXPERIMENTAL_ELIXIR = listOf(
        option(
            "elixir_healing", "Healing", "1 elixir", "Bonus Action to drink",
            "The drinker regains a number of Hit Points equal to 2d8 plus your Intelligence " +
                "modifier. The healing increases by 1d8 at Artificer level 9 (3d8) and again " +
                "at level 15 (4d8).",
        ),
        option(
            "elixir_swiftness", "Swiftness", "1 elixir", "Bonus Action to drink",
            "The drinker's Speed increases by 10 feet for 1 hour. The bonus increases at " +
                "Artificer level 9 (15 feet) and again at level 15 (20 feet).",
        ),
        option(
            "elixir_resilience", "Resilience", "1 elixir", "Bonus Action to drink",
            "The drinker gains a +1 bonus to Armor Class for 10 minutes. The duration " +
                "increases at Artificer level 9 (1 hour) and again at level 15 (8 hours).",
        ),
        option(
            "elixir_boldness", "Boldness", "1 elixir", "Bonus Action to drink",
            "The drinker can roll 1d4 and add the number rolled to every attack roll and " +
                "saving throw it makes for the next minute. The duration increases at " +
                "Artificer level 9 (10 minutes) and again at level 15 (1 hour).",
        ),
        option(
            "elixir_flight", "Flight", "1 elixir", "Bonus Action to drink",
            "The drinker gains a Fly Speed of 10 feet for 10 minutes. The Fly Speed increases " +
                "at Artificer level 9 (20 feet) and again at level 15 (30 feet).",
        ),
        option(
            "elixir_transformation", "Transformation", "1 elixir", "Bonus Action to drink",
            "Rolled as a 6 on the Experimental Elixir table, this result lets you determine " +
                "the elixir's effect by choosing any one of the other rows instead.",
        ),
    )

    /** The Artillerist's cannon does one of three things each time it is activated. */
    private val ELDRITCH_CANNON = listOf(
        option(
            "cannon_flamethrower", "Flamethrower", "1 activation", "Bonus Action",
            "The cannon blasts fire in a 15-foot Cone. Each creature in that area makes a " +
                "Dexterity saving throw against your spell save DC, taking 2d8 Fire damage on " +
                "a failed save or half as much on a successful one. Flammable objects in the " +
                "Cone that aren't being worn or carried start burning. The damage increases by " +
                "1d8 at Artificer level 9.",
        ),
        option(
            "cannon_force_ballista", "Force Ballista", "1 activation", "Bonus Action",
            "Make a ranged spell attack originating from the cannon at one creature or object " +
                "within 120 feet of it. On a hit the target takes 2d8 Force damage, and a " +
                "creature is pushed up to 5 feet away from the cannon. The damage increases by " +
                "1d8 at Artificer level 9.",
        ),
        option(
            "cannon_protector", "Protector", "1 activation", "Bonus Action",
            "The cannon emits a burst of positive energy, granting itself and each creature of " +
                "your choice within 10 feet of it Temporary Hit Points equal to 1d8 plus your " +
                "Intelligence modifier (minimum of +1). The Temporary Hit Points increase by " +
                "1d8 at Artificer level 9.",
        ),
        option(
            "cannon_detonate", "Detonate", "The cannon itself", "Reaction",
            "When your cannon takes damage and you are within 60 feet of it, you can command " +
                "it to detonate. Doing so destroys the cannon and forces each creature within " +
                "20 feet of it to make a Dexterity saving throw against your spell save DC, " +
                "taking 3d10 Force damage on a failed save or half as much on a success.",
            unlockLevel = 9,
        ),
    )

    /** The Battle Smith chooses one of these each time they channel Arcane Jolt. */
    private val ARCANE_JOLT = listOf(
        option(
            "jolt_destructive", "Destructive Energy", "1 use", "No action",
            "The target takes an extra 2d6 Force damage. This increases to 4d6 at Artificer " +
                "level 15.",
        ),
        option(
            "jolt_restorative", "Restorative Energy", "1 use", "No action",
            "Choose one creature or object you can see within 30 feet of the target. Healing " +
                "energy flows into the chosen recipient, restoring 2d6 Hit Points to it. This " +
                "increases to 4d6 at Artificer level 15.",
        ),
    )

    private val BY_RESOURCE: Map<String, List<ResourceOption>> = mapOf(
        "alchemist:experimental_elixir" to EXPERIMENTAL_ELIXIR,
        "artillerist:eldritch_cannon" to ELDRITCH_CANNON,
        "battle_smith:arcane_jolt" to ARCANE_JOLT,
        "monk:focus" to MONK_FOCUS,
        "sorcerer:sorcery_points" to FONT_OF_MAGIC,
        "cleric:channel_divinity" to CLERIC_CHANNEL_DIVINITY,
        "paladin:channel_divinity" to PALADIN_CHANNEL_DIVINITY,
        "psi_warrior:psionic_energy" to PSI_WARRIOR_DICE,
        "soulknife:psionic_energy" to SOULKNIFE_DICE,
        "barbarian:rage" to RAGE_OPTIONS,
        "druid:wild_shape" to WILD_SHAPE_OPTIONS,
        "fighter:second_wind" to SECOND_WIND_OPTIONS,
        "bard:inspiration" to BARDIC_INSPIRATION_OPTIONS,
        "paladin:lay_on_hands" to LAY_ON_HANDS_OPTIONS,
    )

    /** Extra options a subclass adds to a resource its parent class already provides. */
    /**
     * The Warrior of Venom's uses for Focus Points.
     *
     * Both of these ask *which* toxin at the moment you apply it — that half lives in
     * [PerUseChoiceData], where the tracker can put the question in front of the player. What
     * is here is the cost and the action, which never change.
     */
    private val VENOM_FOCUS = listOf(
        option(
            "envenom_weapon", "Envenom Weapon", "1 Focus Point", "Start of your turn",
            "At the start of your turn, you can expend 1 Focus Point to apply a toxin " +
                "produced from your blood to one Monk weapon that you're holding. A creature " +
                "that takes damage from the weapon is subjected to the toxin effect you chose " +
                "when you applied it — Slowing Toxin or Venom. The toxin retains potency for " +
                "1 minute or until a creature takes damage from the weapon.",
            unlockLevel = 3,
        ),
        option(
            "toxic_touch", "Toxic Touch", "1 Focus Point", "Magic action",
            "As a Magic action, you can expend 1 Focus Point to apply a potent toxin to a " +
                "creature you touch. The target makes a Constitution saving throw against " +
                "your Monk save DC. On a failed save, the target has the Poisoned condition " +
                "for 1 minute and suffers the effect you chose — Intoxicant, Sedative, or " +
                "Truth Serum.",
            unlockLevel = 6,
        ),
        option(
            "hallucinogenic_breath", "Hallucinogenic Breath", "2 Focus Points", "Replaces one attack",
            "When you take the Attack action on your turn, you can expend 2 Focus Points and " +
                "replace one of your attacks with an exhalation of hallucinogenic vapors at " +
                "one creature you can see within 30 feet. The target must make a Constitution " +
                "saving throw. On a failed save, it takes Poison damage equal to three rolls " +
                "of your Martial Arts die and has the Frightened condition for 1 minute or " +
                "until it takes damage; while Frightened it takes the Dash action and moves " +
                "away from you by the safest route on each of its turns. On a successful " +
                "save, a creature takes half as much damage only.",
            unlockLevel = 17,
        ),
    )

    private val BY_SUBCLASS: Map<String, Pair<String, List<ResourceOption>>> = mapOf(
        "mercy" to ("monk:focus" to MERCY_FOCUS),
        "shadow" to ("monk:focus" to SHADOW_FOCUS),
        "elements" to ("monk:focus" to ELEMENTS_FOCUS),
        "open_hand" to ("monk:focus" to OPEN_HAND_FOCUS),
        "warrior_of_venom" to ("monk:focus" to VENOM_FOCUS),
    )

    /**
     * Everything granted for a resource at this level, including whatever the subclass adds.
     * Options above the character's level are left out.
     */
    fun forResource(resourceId: String, subclassId: String?, level: Int): List<ResourceOption> {
        val base = BY_RESOURCE[resourceId].orEmpty()
        val fromSubclass = subclassId
            ?.let { BY_SUBCLASS[it] }
            ?.takeIf { it.first == resourceId }
            ?.second
            .orEmpty()
        val curated = base + fromSubclass
        val derived = derivedFrom(resourceId, subclassId)
            // A curated entry wins: the Monk subclasses have hand-written text with the
            // Focus Point cost spelled out, which is better than the feature blurb.
            .filterNot { d -> curated.any { it.name.equals(d.name, ignoreCase = true) } }
        return (curated + derived).filter { it.unlockLevel <= level }
    }

    /**
     * The pools a subclass feature can say it spends, and the name it calls each one.
     *
     * Deriving these beats listing them: a subclass already carries the level, name and rules
     * text of every feature it grants, and repeating that in a second table is how fifteen
     * Paladin and Cleric options came to be missing from the Channel Divinity tracker while
     * the features themselves were on the sheet the whole time.
     */
    private val SPENT_BY: Map<String, Regex> = mapOf(
        "cleric:channel_divinity" to spendRegex("Channel Divinity"),
        "paladin:channel_divinity" to spendRegex("Channel Divinity"),
        "bard:inspiration" to spendRegex("Bardic Inspiration"),
        "monk:focus" to spendRegex("Focus Point"),
        "druid:wild_shape" to spendRegex("Wild Shape"),
        "sorcerer:sorcery_points" to spendRegex("Sorcery Point"),
        "battle_master:superiority" to spendRegex("[Ss]uperiority [Dd]i"),
        "barbarian:rage" to spendRegex("Rage"),
    )

    /**
     * Matches a feature that *spends* the pool, not one that merely mentions it.
     *
     * Most Barbarian subclass features say "While raging", which costs nothing; only the ones
     * that expend a use belong on a tracker.
     */
    private fun spendRegex(name: String) =
        Regex("""(?:[Ss]pend|[Ee]xpend(?:ing|s)?)[^.]{0,60}?$name""")

    private fun derivedFrom(resourceId: String, subclassId: String?): List<ResourceOption> {
        val spends = SPENT_BY[resourceId] ?: return emptyList()
        val subclass = subclassId?.let { SubclassData.byId(it) } ?: return emptyList()
        // The pool is named for the class that grants it, so a Cleric domain must not add
        // options to a Paladin's tracker just because both are called Channel Divinity.
        if (!resourceId.startsWith("${subclass.classId}:")) return emptyList()

        return subclass.features
            .filter { spends.containsMatchIn(it.description) }
            .map { feature ->
                ResourceOption(
                    id = "${subclass.id}:" + feature.name.lowercase().replace(Regex("[^a-z0-9]+"), "_"),
                    name = feature.name,
                    cost = "1 use",
                    description = feature.description,
                    unlockLevel = feature.level,
                )
            }
    }
}
