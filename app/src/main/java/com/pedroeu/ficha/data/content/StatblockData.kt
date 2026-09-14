package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.rules.AbilityRef
import com.pedroeu.ficha.rules.ActionKind
import com.pedroeu.ficha.rules.Formula
import com.pedroeu.ficha.rules.LevelScope
import com.pedroeu.ficha.rules.Statblock
import com.pedroeu.ficha.rules.StatblockAction

/**
 * The creatures the rules let a character summon.
 *
 * Written as data with formulas rather than as printed cards, because almost none of these
 * numbers belong to the creature. A Bestial Spirit's Hit Points are "5 + 5 per spell level", a
 * Steel Defender's Armor Class is "12 plus your Intelligence modifier", and a Vestige
 * Companion's Hit Points are "4 + four times your Warlock level". The same spirit summoned by
 * a level 3 slot and a level 7 slot is two different creatures, and a sheet that showed one
 * number would be wrong for the other.
 *
 * Adding a creature here is the whole job of supporting a new summoning spell — the picker,
 * the tab, the hit point tracking and the dismissal are all general.
 */
object StatblockData {

    private fun scores(str: Int, dex: Int, con: Int, int: Int, wis: Int, cha: Int) = mapOf(
        Ability.STR to str, Ability.DEX to dex, Ability.CON to con,
        Ability.INT to int, Ability.WIS to wis, Ability.CHA to cha,
    )

    /** "Bonus equals your spell attack modifier", which most summoned spirits use. */
    private val SPELL_ATTACK: Formula = Formula.Flat(0)

    /** AC 11 plus the spell's level, the 2024 spirit pattern. */
    private fun spiritAc(base: Int) =
        Formula.Sum(listOf(Formula.Flat(base), Formula.SpellLevel))

    /** "X + Y per spell level", the 2024 spirit hit point pattern. */
    private fun spiritHp(base: Int, perLevel: Int) =
        Formula.Sum(listOf(Formula.Flat(base), Formula.PerSpellLevel(perLevel)))

    // ================================================================ Summon Beast

    private fun bestial(
        id: String, variant: String, speed: String, extra: StatblockAction?,
    ) = Statblock(
        id = id,
        name = "Bestial Spirit ($variant)",
        size = "Small",
        creatureType = "Beast",
        armorClass = spiritAc(11),
        hitPoints = spiritHp(5, 5),
        speed = speed,
        abilityScores = scores(18, 11, 16, 4, 14, 5),
        sharesProficiencyBonus = true,
        senses = "Darkvision 60 ft.",
        conditionImmunities = listOf("Charmed", "Frightened"),
        actions = listOfNotNull(
            StatblockAction(
                name = "Maul",
                description = "Melee Attack Roll: bonus equals your spell attack modifier, " +
                    "reach 5 ft. Hit: 1d8 + 4 plus the spell's level Piercing damage.",
                toHit = SPELL_ATTACK,
                damageDice = "1d8",
                damageBonus = Formula.Sum(listOf(Formula.Flat(4), Formula.SpellLevel)),
                damageType = "Piercing",
                reach = "5 ft.",
            ),
            StatblockAction(
                name = "Multiattack",
                kind = ActionKind.TRAIT,
                description = "The beast makes a number of Maul attacks equal to half this " +
                    "spell's level (round down).",
            ),
            extra,
        ),
    )

    private val BESTIAL = listOf(
        bestial("bestial_spirit_air", "Air", "Fly 60 ft.", null),
        bestial("bestial_spirit_land", "Land", "40 ft., Climb 40 ft.", null),
        bestial(
            "bestial_spirit_water", "Water", "20 ft., Swim 40 ft.",
            StatblockAction(
                name = "Water Breathing",
                kind = ActionKind.TRAIT,
                description = "The beast can breathe underwater.",
            ),
        ),
    )

    // ================================================================ Summon Undead

    private fun undead(id: String, variant: String, extra: StatblockAction) = Statblock(
        id = id,
        name = "Undead Spirit ($variant)",
        size = "Medium",
        creatureType = "Undead",
        armorClass = spiritAc(11),
        hitPoints = spiritHp(30, 10),
        speed = if (variant == "Ghostly") "30 ft., Fly 40 ft. (hover)" else "30 ft.",
        abilityScores = scores(12, 16, 15, 4, 10, 16),
        sharesProficiencyBonus = true,
        senses = "Darkvision 60 ft.",
        immunities = listOf("Necrotic", "Poison"),
        conditionImmunities = listOf("Exhaustion", "Frightened", "Paralyzed", "Poisoned"),
        actions = listOf(
            StatblockAction(
                name = "Multiattack",
                kind = ActionKind.TRAIT,
                description = "The undead makes a number of attacks equal to half this " +
                    "spell's level (round down).",
            ),
            extra,
        ),
    )

    private val UNDEAD = listOf(
        undead(
            "undead_spirit_ghostly", "Ghostly",
            StatblockAction(
                name = "Deathly Touch",
                description = "Melee Attack Roll: bonus equals your spell attack modifier, " +
                    "reach 5 ft. Hit: 1d8 + 3 plus the spell's level Necrotic damage, and " +
                    "the target has Disadvantage on its next attack roll before the start " +
                    "of your next turn.",
                toHit = SPELL_ATTACK,
                damageDice = "1d8",
                damageBonus = Formula.Sum(listOf(Formula.Flat(3), Formula.SpellLevel)),
                damageType = "Necrotic",
                reach = "5 ft.",
            ),
        ),
        undead(
            "undead_spirit_putrid", "Putrid",
            StatblockAction(
                name = "Rotting Claw",
                description = "Melee Attack Roll: bonus equals your spell attack modifier, " +
                    "reach 5 ft. Hit: 1d8 + 3 plus the spell's level Necrotic damage, and " +
                    "the target must succeed on a Constitution saving throw against your " +
                    "spell save DC or have the Poisoned condition until the end of its " +
                    "next turn.",
                toHit = SPELL_ATTACK,
                damageDice = "1d8",
                damageBonus = Formula.Sum(listOf(Formula.Flat(3), Formula.SpellLevel)),
                damageType = "Necrotic",
                reach = "5 ft.",
            ),
        ),
        undead(
            "undead_spirit_skeletal", "Skeletal",
            StatblockAction(
                name = "Grave Bolt",
                description = "Ranged Attack Roll: bonus equals your spell attack modifier, " +
                    "range 150 ft. Hit: 2d4 + 3 plus the spell's level Necrotic damage.",
                toHit = SPELL_ATTACK,
                damageDice = "2d4",
                damageBonus = Formula.Sum(listOf(Formula.Flat(3), Formula.SpellLevel)),
                damageType = "Necrotic",
                reach = "150 ft.",
            ),
        ),
    )

    // ================================================================ Summon Fey

    private fun fey(id: String, variant: String, extra: StatblockAction) = Statblock(
        id = id,
        name = "Fey Spirit ($variant)",
        size = "Medium",
        creatureType = "Fey",
        armorClass = spiritAc(12),
        hitPoints = spiritHp(30, 10),
        speed = "40 ft.",
        abilityScores = scores(13, 16, 14, 14, 11, 16),
        sharesProficiencyBonus = true,
        senses = "Darkvision 60 ft.",
        conditionImmunities = listOf("Charmed"),
        actions = listOf(
            StatblockAction(
                name = "Multiattack",
                kind = ActionKind.TRAIT,
                description = "The fey makes a number of Shortsword attacks equal to half " +
                    "this spell's level (round down).",
            ),
            StatblockAction(
                name = "Shortsword",
                description = "Melee Attack Roll: bonus equals your spell attack modifier, " +
                    "reach 5 ft. Hit: 2d6 + 3 plus the spell's level Force damage.",
                toHit = SPELL_ATTACK,
                damageDice = "2d6",
                damageBonus = Formula.Sum(listOf(Formula.Flat(3), Formula.SpellLevel)),
                damageType = "Force",
                reach = "5 ft.",
            ),
            extra,
        ),
    )

    private val FEY = listOf(
        fey(
            "fey_spirit_fuming", "Fuming",
            StatblockAction(
                name = "Fuming Glare",
                kind = ActionKind.BONUS_ACTION,
                description = "One creature the fey can see within 60 feet must succeed on a " +
                    "Wisdom saving throw against your spell save DC or have the Frightened " +
                    "condition until the end of the fey's next turn.",
            ),
        ),
        fey(
            "fey_spirit_mirthful", "Mirthful",
            StatblockAction(
                name = "Mirthful Glamour",
                kind = ActionKind.BONUS_ACTION,
                description = "One creature the fey can see within 60 feet must succeed on a " +
                    "Wisdom saving throw against your spell save DC or have the Charmed " +
                    "condition until the end of the fey's next turn.",
            ),
        ),
        fey(
            "fey_spirit_tricksy", "Tricksy",
            StatblockAction(
                name = "Tricksy Magic",
                kind = ActionKind.BONUS_ACTION,
                description = "The fey casts Entangle, requiring no spell components and " +
                    "using your spell save DC.",
            ),
        ),
    )

    // ================================================================ Summon Elemental

    private fun elemental(
        id: String, variant: String, damageType: String, speed: String, extra: StatblockAction,
    ) = Statblock(
        id = id,
        name = "Elemental Spirit ($variant)",
        size = "Medium",
        creatureType = "Elemental",
        armorClass = spiritAc(11),
        hitPoints = spiritHp(50, 10),
        speed = speed,
        abilityScores = scores(18, 15, 17, 4, 10, 16),
        sharesProficiencyBonus = true,
        senses = "Darkvision 60 ft.",
        immunities = listOf("Poison"),
        conditionImmunities = listOf("Exhaustion", "Paralyzed", "Petrified", "Poisoned", "Unconscious"),
        actions = listOf(
            StatblockAction(
                name = "Multiattack",
                kind = ActionKind.TRAIT,
                description = "The elemental makes a number of Slam attacks equal to half " +
                    "this spell's level (round down).",
            ),
            StatblockAction(
                name = "Slam",
                description = "Melee Attack Roll: bonus equals your spell attack modifier, " +
                    "reach 5 ft. Hit: 1d10 + 4 plus the spell's level $damageType damage.",
                toHit = SPELL_ATTACK,
                damageDice = "1d10",
                damageBonus = Formula.Sum(listOf(Formula.Flat(4), Formula.SpellLevel)),
                damageType = damageType,
                reach = "5 ft.",
            ),
            extra,
        ),
    )

    private val ELEMENTAL = listOf(
        elemental(
            "elemental_spirit_air", "Air", "Bludgeoning", "Fly 40 ft. (hover)",
            StatblockAction(
                name = "Air Form",
                kind = ActionKind.TRAIT,
                description = "The elemental can enter an enemy's space and stop there, and " +
                    "it can move through a space as narrow as 1 inch without squeezing.",
            ),
        ),
        elemental(
            "elemental_spirit_earth", "Earth", "Bludgeoning", "40 ft., Burrow 30 ft.",
            StatblockAction(
                name = "Earth Form",
                kind = ActionKind.TRAIT,
                description = "The elemental can burrow through nonmagical, unworked earth " +
                    "and stone without disturbing it.",
            ),
        ),
        elemental(
            "elemental_spirit_fire", "Fire", "Fire", "40 ft.",
            StatblockAction(
                name = "Fire Form",
                kind = ActionKind.TRAIT,
                description = "The elemental can move through a space as narrow as 1 inch " +
                    "without squeezing, and a creature that touches it takes 1d10 Fire damage.",
            ),
        ),
        elemental(
            "elemental_spirit_water", "Water", "Cold", "30 ft., Swim 40 ft.",
            StatblockAction(
                name = "Water Form",
                kind = ActionKind.TRAIT,
                description = "The elemental can enter an enemy's space and stop there, and " +
                    "it can move through a space as narrow as 1 inch without squeezing.",
            ),
        ),
    )

    // ================================================================ Companions

    /**
     * The Battle Smith's Steel Defender, whose every number comes from its Artificer.
     *
     * "Armor Class: 12 + your Intelligence modifier. Hit Points: 5 + five times your Artificer
     * level." This is the case the formulas exist for.
     */
    private val STEEL_DEFENDER = Statblock(
        id = "steel_defender",
        name = "Steel Defender",
        size = "Medium",
        creatureType = "Construct",
        armorClass = Formula.Sum(
            listOf(Formula.Flat(12), Formula.AbilityMod(AbilityRef.Fixed(Ability.INT)))
        ),
        hitPoints = Formula.Sum(
            listOf(Formula.Flat(5), Formula.PerLevel(5, LevelScope.OWNING_CLASS))
        ),
        speed = "40 ft.",
        abilityScores = scores(14, 12, 14, 4, 10, 6),
        sharesProficiencyBonus = true,
        immunities = listOf("Poison"),
        conditionImmunities = listOf("Charmed", "Exhaustion", "Poisoned"),
        senses = "Darkvision 60 ft., Passive Perception 10",
        languages = "Understands the languages you know",
        actions = listOf(
            StatblockAction(
                name = "Force-Empowered Rend",
                description = "Melee Attack Roll: bonus equals your spell attack modifier, " +
                    "reach 5 ft. Hit: 1d8 + 2 plus your Intelligence modifier Force damage.",
                toHit = SPELL_ATTACK,
                damageDice = "1d8",
                damageBonus = Formula.Sum(
                    listOf(Formula.Flat(2), Formula.AbilityMod(AbilityRef.Fixed(Ability.INT)))
                ),
                damageType = "Force",
                reach = "5 ft.",
            ),
            StatblockAction(
                name = "Repair (3/Day)",
                description = "The defender, or one Construct or object it can see within 5 " +
                    "feet, regains 2d8 plus your Intelligence modifier Hit Points.",
            ),
            StatblockAction(
                name = "Deflect Attack",
                kind = ActionKind.REACTION,
                description = "When a creature the defender can see within 5 feet makes an " +
                    "attack roll against a different creature, that roll has Disadvantage.",
            ),
        ),
    )

    /**
     * The Death Domain Vestige, which decides what it is and what that makes it resist.
     *
     * "Armor Class: 13 plus your Charisma modifier. Hit Points: 4 + four times your Warlock
     * level." Its damage type and Divine Power follow the form chosen in the Vestige Companion
     * feature, so there are three of it rather than one with a note.
     */
    private fun vestige(id: String, form: String, damageType: String, power: StatblockAction) =
        Statblock(
            id = id,
            name = "Vestige Companion ($form)",
            size = "Small",
            creatureType = form,
            armorClass = Formula.Sum(
                listOf(Formula.Flat(13), Formula.AbilityMod(AbilityRef.Fixed(Ability.CHA)))
            ),
            hitPoints = Formula.Sum(
                listOf(Formula.Flat(4), Formula.PerLevel(4, LevelScope.OWNING_CLASS))
            ),
            speed = "5 ft., Fly 30 ft. (hover)",
            abilityScores = scores(1, 14, 10, 15, 15, 16),
            sharesProficiencyBonus = true,
            resistances = listOf(damageType),
            conditionImmunities = listOf("Charmed", "Frightened", "Prone"),
            senses = "Passive Perception 12",
            languages = "Speaks the languages you know",
            actions = listOf(
                StatblockAction(
                    name = "Vestige's Strike",
                    description = "Melee or Ranged Attack Roll: bonus equals your spell " +
                        "attack modifier, reach 5 ft. or range 60 ft. Hit: 1d6 + 3 plus your " +
                        "Charisma modifier $damageType damage.",
                    toHit = SPELL_ATTACK,
                    damageDice = "1d6",
                    damageBonus = Formula.Sum(
                        listOf(Formula.Flat(3), Formula.AbilityMod(AbilityRef.Fixed(Ability.CHA)))
                    ),
                    damageType = damageType,
                    reach = "5 ft. or 60 ft.",
                ),
                power,
            ),
        )

    private val VESTIGES = listOf(
        vestige(
            "vestige_celestial", "Celestial", "Radiant",
            StatblockAction(
                name = "Healing Touch (1/Day)",
                kind = ActionKind.BONUS_ACTION,
                description = "The vestige touches another creature. The target regains 2d8 " +
                    "plus your Charisma modifier Hit Points and ends one condition on it: " +
                    "Blinded, Deafened, or Poisoned.",
            ),
        ),
        vestige(
            "vestige_fiend", "Fiend", "Fire",
            StatblockAction(
                name = "Fiendish Swap (1/Day)",
                kind = ActionKind.BONUS_ACTION,
                description = "If you and the vestige are within 60 feet of each other, you " +
                    "both teleport, swapping places.",
            ),
        ),
        vestige(
            "vestige_undead", "Undead", "Necrotic",
            StatblockAction(
                name = "Cursed Invocation (1/Day)",
                kind = ActionKind.BONUS_ACTION,
                description = "The vestige curses a creature it can see within 30 feet for 1 " +
                    "minute. While cursed, the target has Disadvantage on attack rolls " +
                    "against you and the vestige.",
            ),
        ),
    )

    // ================================================================ Fixed creatures

    private val SKELETON = Statblock(
        id = "skeleton",
        name = "Skeleton",
        size = "Medium",
        creatureType = "Undead",
        armorClass = Formula.Flat(14),
        hitPoints = Formula.Flat(13),
        speed = "30 ft.",
        abilityScores = scores(10, 16, 15, 6, 8, 5),
        immunities = listOf("Poison"),
        conditionImmunities = listOf("Exhaustion", "Poisoned"),
        senses = "Darkvision 60 ft.",
        resistances = listOf("Piercing"),
        actions = listOf(
            StatblockAction(
                name = "Shortsword",
                description = "Melee Attack Roll: +5, reach 5 ft. Hit: 1d6 + 3 Piercing damage.",
                damageDice = "1d6", damageBonus = Formula.Flat(3),
                damageType = "Piercing", reach = "5 ft.",
            ),
            StatblockAction(
                name = "Shortbow",
                description = "Ranged Attack Roll: +5, range 80/320 ft. Hit: 1d6 + 3 Piercing damage.",
                damageDice = "1d6", damageBonus = Formula.Flat(3),
                damageType = "Piercing", reach = "80/320 ft.",
            ),
        ),
    )

    private val ZOMBIE = Statblock(
        id = "zombie",
        name = "Zombie",
        size = "Medium",
        creatureType = "Undead",
        armorClass = Formula.Flat(8),
        hitPoints = Formula.Flat(15),
        speed = "20 ft.",
        abilityScores = scores(13, 6, 16, 3, 6, 5),
        immunities = listOf("Poison"),
        conditionImmunities = listOf("Exhaustion", "Poisoned"),
        senses = "Darkvision 60 ft.",
        actions = listOf(
            StatblockAction(
                name = "Slam",
                description = "Melee Attack Roll: +3, reach 5 ft. Hit: 1d6 + 1 Bludgeoning damage.",
                damageDice = "1d6", damageBonus = Formula.Flat(1),
                damageType = "Bludgeoning", reach = "5 ft.",
            ),
            StatblockAction(
                name = "Undead Fortitude",
                kind = ActionKind.TRAIT,
                description = "If damage reduces the zombie to 0 Hit Points, it makes a " +
                    "Constitution saving throw (DC 5 plus the damage taken) unless the " +
                    "damage is Radiant or from a Critical Hit. On a success, it drops to 1 " +
                    "Hit Point instead.",
            ),
        ),
    )

    private val OTHERWORLDLY_STEED = Statblock(
        id = "otherworldly_steed",
        name = "Otherworldly Steed",
        size = "Large",
        creatureType = "Celestial, Fey, or Fiend",
        armorClass = Formula.Sum(listOf(Formula.Flat(10), Formula.ProficiencyBonus())),
        hitPoints = Formula.Sum(
            listOf(Formula.Flat(5), Formula.PerLevel(5, LevelScope.CHARACTER))
        ),
        speed = "60 ft.",
        abilityScores = scores(18, 12, 16, 6, 13, 8),
        sharesProficiencyBonus = true,
        senses = "Darkvision 60 ft.",
        languages = "Understands the languages you know",
        actions = listOf(
            StatblockAction(
                name = "Slam",
                description = "Melee Attack Roll: bonus equals your spell attack modifier, " +
                    "reach 5 ft. Hit: 1d8 plus your spellcasting ability modifier damage of " +
                    "the steed's type.",
                toHit = SPELL_ATTACK,
                damageDice = "1d8",
                damageType = "Radiant, Psychic, or Necrotic",
                reach = "5 ft.",
            ),
            StatblockAction(
                name = "Otherworldly Bond",
                kind = ActionKind.TRAIT,
                description = "The steed adds your Proficiency Bonus to its ability checks, " +
                    "saving throws, and the damage of its Slam.",
            ),
        ),
    )

    /** Familiar forms, whose numbers are the ordinary Beast stat blocks. */
    private fun familiar(
        id: String, name: String, ac: Int, hp: Int, speed: String,
        str: Int, dex: Int, con: Int, int: Int, wis: Int, cha: Int,
        senses: String = "",
    ) = Statblock(
        id = id, name = name, size = "Tiny", creatureType = "Celestial, Fey, or Fiend",
        armorClass = Formula.Flat(ac), hitPoints = Formula.Flat(hp), speed = speed,
        abilityScores = scores(str, dex, con, int, wis, cha), senses = senses,
        notes = "A familiar can't attack, but it can take other actions as normal.",
    )

    private val FAMILIARS = listOf(
        familiar("familiar_bat", "Bat", 12, 1, "5 ft., Fly 30 ft.", 2, 15, 8, 2, 12, 4,
            "Blindsight 60 ft."),
        familiar("familiar_cat", "Cat", 12, 2, "40 ft., Climb 40 ft.", 3, 15, 10, 3, 12, 7),
        familiar("familiar_frog", "Frog", 11, 1, "20 ft., Swim 20 ft.", 1, 13, 8, 1, 8, 3,
            "Darkvision 30 ft."),
        familiar("familiar_hawk", "Hawk", 13, 1, "10 ft., Fly 60 ft.", 5, 16, 8, 2, 14, 6),
        familiar("familiar_lizard", "Lizard", 10, 2, "20 ft., Climb 20 ft.", 2, 11, 10, 1, 8, 3,
            "Darkvision 30 ft."),
        familiar("familiar_octopus", "Octopus", 12, 3, "5 ft., Swim 30 ft.", 4, 15, 11, 3, 10, 4,
            "Darkvision 30 ft."),
        familiar("familiar_owl", "Owl", 11, 1, "5 ft., Fly 60 ft.", 3, 13, 8, 2, 12, 7,
            "Darkvision 120 ft."),
        familiar("familiar_rat", "Rat", 10, 1, "30 ft., Climb 30 ft.", 2, 11, 9, 2, 10, 4,
            "Darkvision 30 ft."),
        familiar("familiar_raven", "Raven", 12, 2, "10 ft., Fly 50 ft.", 2, 14, 8, 5, 12, 6),
        familiar("familiar_spider", "Spider", 12, 1, "20 ft., Climb 20 ft.", 2, 14, 8, 1, 10, 2,
            "Darkvision 30 ft."),
        familiar("familiar_weasel", "Weasel", 13, 1, "30 ft., Climb 30 ft.", 3, 16, 8, 2, 12, 3),
    )


    // ================================================================ Pact of the Chain

    /**
     * "Imp, Pseudodragon, Quasit, Skeleton, Slaad Tadpole, Sphinx of Wonder, Sprite, or
     * Venomous Snake."
     *
     * The eight forms Pact of the Chain adds to Find Familiar. Unlike the ordinary familiar
     * forms these can act — a Pact of the Chain familiar makes an attack of its own when the
     * Warlock forgoes one — so each carries its attack rather than the note that it has none.
     */
    private fun pactFamiliar(
        id: String, name: String, type: String, ac: Int, hp: Int, speed: String,
        str: Int, dex: Int, con: Int, int: Int, wis: Int, cha: Int,
        attack: StatblockAction,
        senses: String = "Darkvision 60 ft.",
        extra: List<StatblockAction> = emptyList(),
    ) = Statblock(
        id = id, name = name, size = "Tiny", creatureType = type,
        armorClass = Formula.Flat(ac), hitPoints = Formula.Flat(hp), speed = speed,
        abilityScores = scores(str, dex, con, int, wis, cha),
        senses = senses,
        actions = listOf(attack) + extra,
    )

    private val PACT_FAMILIARS = listOf(
        pactFamiliar(
            "pact_imp", "Imp", "Fiend (Devil)", 13, 21, "20 ft., Fly 40 ft.",
            6, 17, 13, 11, 12, 14,
            StatblockAction(
                name = "Sting",
                description = "Melee Attack Roll: +5, reach 5 ft. Hit: 1d4 + 3 Piercing " +
                    "damage plus 3d6 Poison damage.",
                damageDice = "1d4", damageBonus = Formula.Flat(3),
                damageType = "Piercing plus 3d6 Poison", reach = "5 ft.",
            ),
            extra = listOf(
                StatblockAction(
                    name = "Invisibility",
                    kind = ActionKind.TRAIT,
                    description = "The imp magically turns Invisible until it attacks, or " +
                        "until its Concentration ends.",
                ),
                StatblockAction(
                    name = "Shape-Shift",
                    kind = ActionKind.BONUS_ACTION,
                    description = "The imp shape-shifts into a Rat, a Raven, or a Spider, or " +
                        "back into its true form.",
                ),
            ),
        ),
        pactFamiliar(
            "pact_pseudodragon", "Pseudodragon", "Dragon", 14, 10, "15 ft., Fly 30 ft.",
            6, 15, 13, 10, 12, 10,
            StatblockAction(
                name = "Bite",
                description = "Melee Attack Roll: +4, reach 5 ft. Hit: 1d4 + 2 Piercing damage.",
                damageDice = "1d4", damageBonus = Formula.Flat(2),
                damageType = "Piercing", reach = "5 ft.",
            ),
            senses = "Blindsight 10 ft., Darkvision 60 ft.",
            extra = listOf(
                StatblockAction(
                    name = "Sting",
                    description = "Constitution saving throw DC 12. Failure: 2d4 Poison " +
                        "damage, and the target has the Poisoned condition for 1 hour.",
                ),
            ),
        ),
        pactFamiliar(
            "pact_quasit", "Quasit", "Fiend (Demon)", 13, 25, "40 ft.",
            5, 17, 14, 7, 10, 10,
            StatblockAction(
                name = "Claw",
                description = "Melee Attack Roll: +5, reach 5 ft. Hit: 1d4 + 3 Slashing " +
                    "damage, and the target has the Poisoned condition until the end of its " +
                    "next turn.",
                damageDice = "1d4", damageBonus = Formula.Flat(3),
                damageType = "Slashing", reach = "5 ft.",
            ),
            extra = listOf(
                StatblockAction(
                    name = "Scare (1/Day)",
                    description = "One creature within 20 feet makes a DC 10 Wisdom saving " +
                        "throw or has the Frightened condition for 1 minute.",
                ),
                StatblockAction(
                    name = "Shape-Shift",
                    kind = ActionKind.BONUS_ACTION,
                    description = "The quasit shape-shifts into a Bat, a Centipede, or a " +
                        "Toad, or back into its true form.",
                ),
            ),
        ),
        pactFamiliar(
            "pact_skeleton", "Skeleton", "Undead", 15, 13, "30 ft.",
            10, 16, 15, 6, 8, 5,
            StatblockAction(
                name = "Shortbow",
                description = "Ranged Attack Roll: +5, range 80/320 ft. Hit: 1d6 + 3 " +
                    "Piercing damage.",
                damageDice = "1d6", damageBonus = Formula.Flat(3),
                damageType = "Piercing", reach = "80/320 ft.",
            ),
        ),
        pactFamiliar(
            "pact_slaad_tadpole", "Slaad Tadpole", "Aberration", 12, 10, "30 ft.",
            7, 15, 10, 3, 5, 3,
            StatblockAction(
                name = "Bite",
                description = "Melee Attack Roll: +3, reach 5 ft. Hit: 1d4 + 1 Piercing damage.",
                damageDice = "1d4", damageBonus = Formula.Flat(1),
                damageType = "Piercing", reach = "5 ft.",
            ),
        ),
        pactFamiliar(
            "pact_sphinx_of_wonder", "Sphinx of Wonder", "Celestial", 13, 7, "20 ft., Fly 40 ft.",
            6, 17, 13, 15, 12, 11,
            StatblockAction(
                name = "Rend",
                description = "Melee Attack Roll: +5, reach 5 ft. Hit: 1d4 + 3 Slashing " +
                    "damage plus 2d6 Radiant damage.",
                damageDice = "1d4", damageBonus = Formula.Flat(3),
                damageType = "Slashing plus 2d6 Radiant", reach = "5 ft.",
            ),
            extra = listOf(
                StatblockAction(
                    name = "Magic Resistance",
                    kind = ActionKind.TRAIT,
                    description = "The sphinx has Advantage on saving throws against spells " +
                        "and other magical effects.",
                ),
            ),
        ),
        pactFamiliar(
            "pact_sprite", "Sprite", "Fey", 15, 10, "10 ft., Fly 40 ft.",
            3, 18, 10, 14, 13, 11,
            StatblockAction(
                name = "Shortbow",
                description = "Ranged Attack Roll: +6, range 40/160 ft. Hit: 1 Piercing " +
                    "damage, and the target has the Poisoned condition until the end of its " +
                    "next turn; a DC 10 Constitution save ends the condition early. On a " +
                    "failed save by 5 or more the target also falls Unconscious for 1 minute.",
                damageDice = "1", damageType = "Piercing", reach = "40/160 ft.",
            ),
            senses = "",
            extra = listOf(
                StatblockAction(
                    name = "Heart Sight",
                    description = "The sprite touches a creature and learns its emotional " +
                        "state and whether it is Evil.",
                ),
            ),
        ),
        pactFamiliar(
            "pact_venomous_snake", "Venomous Snake", "Beast", 12, 5, "30 ft., Swim 30 ft.",
            2, 15, 11, 1, 10, 3,
            StatblockAction(
                name = "Bite",
                description = "Melee Attack Roll: +4, reach 5 ft. Hit: 1 Piercing damage " +
                    "plus 2d4 Poison damage.",
                damageDice = "1", damageType = "Piercing plus 2d4 Poison", reach = "5 ft.",
            ),
            senses = "Blindsight 10 ft.",
        ),
    )

    /** The eight forms Pact of the Chain adds to Find Familiar. */
    val PACT_OF_THE_CHAIN_FORMS: List<String> = PACT_FAMILIARS.map { it.id }

    // ================================================================ The catalogue

    // ================================================================ Summon Plant

    private fun plant(
        id: String, variant: String, acBase: Int, speed: String,
        vulnerabilities: List<String>, actions: List<StatblockAction>,
    ) = Statblock(
        id = id,
        name = "Plant Spirit ($variant)",
        size = "Large",
        creatureType = "Plant",
        armorClass = spiritAc(acBase),
        hitPoints = spiritHp(50, 10),
        speed = speed,
        abilityScores = scores(17, 13, 14, 10, 13, 10),
        sharesProficiencyBonus = true,
        senses = "Passive Perception 11",
        vulnerabilities = vulnerabilities,
        actions = actions + StatblockAction(
            name = "Multiattack",
            kind = ActionKind.TRAIT,
            description = "The spirit makes a number of attacks equal to half this spell's " +
                "level (round down).",
        ),
    )

    private val SLAM = StatblockAction(
        name = "Slam",
        description = "Melee Attack Roll: bonus equals your spell attack modifier, reach 5 ft. " +
            "Hit: 1d10 + 3 plus the spell's level Bludgeoning damage.",
        toHit = SPELL_ATTACK,
        damageDice = "1d10",
        damageBonus = Formula.Sum(listOf(Formula.Flat(3), Formula.SpellLevel)),
        damageType = "Bludgeoning",
        reach = "5 ft.",
    )

    private val PLANTS = listOf(
        // The Tree is the only one with the +2 to Armor Class, and the only one that burns.
        plant(
            "plant_spirit_tree", "Tree", 13, "40 ft.", listOf("Fire"),
            listOf(
                SLAM,
                StatblockAction(
                    name = "Siege Monster",
                    kind = ActionKind.TRAIT,
                    description = "The spirit deals double damage to objects and structures.",
                ),
            ),
        ),
        plant(
            "plant_spirit_fungus", "Fungus", 11, "40 ft.", listOf("Slashing"),
            listOf(
                StatblockAction(
                    name = "Spore Spray",
                    description = "Melee or Ranged Attack Roll: bonus equals your spell attack " +
                        "modifier, reach 5 ft. or range 30 ft. Hit: 1d4 plus the spell's level " +
                        "Poison damage, and the target has the Poisoned condition until the end " +
                        "of its next turn. If the target already has the Poisoned condition, it " +
                        "instead takes an extra 3d4 Poison damage.",
                    toHit = SPELL_ATTACK,
                    damageDice = "1d4",
                    damageBonus = Formula.SpellLevel,
                    damageType = "Poison",
                    reach = "5 ft. or range 30 ft.",
                ),
            ),
        ),
        plant(
            "plant_spirit_vine", "Vine", 11, "40 ft., Climb 40 ft.", listOf("Slashing"),
            listOf(
                SLAM,
                StatblockAction(
                    name = "Twist Away",
                    kind = ActionKind.BONUS_ACTION,
                    description = "The spirit takes the Dash or Disengage action.",
                ),
            ),
        ),
    )

    val PLANT_FORMS: List<String> = PLANTS.map { it.id }

    // ================================================================ Summon Dinosaur

    private fun dinosaur(
        id: String, variant: String, acBase: Int, actions: List<StatblockAction>,
    ) = Statblock(
        id = id,
        name = "Dinosaur Spirit ($variant)",
        size = "Huge",
        creatureType = "Beast (Dinosaur)",
        armorClass = spiritAc(acBase),
        hitPoints = spiritHp(60, 10),
        speed = "40 ft.",
        abilityScores = scores(21, 10, 15, 4, 12, 9),
        sharesProficiencyBonus = true,
        senses = "Passive Perception 11",
        actions = actions + listOf(
            StatblockAction(
                name = "Multiattack",
                kind = ActionKind.TRAIT,
                description = "The spirit makes a number of attacks equal to half this spell's " +
                    "level (round down).",
            ),
            StatblockAction(
                name = "Tough",
                kind = ActionKind.TRAIT,
                description = "Add half the spell's level (round down) to any Strength or " +
                    "Constitution saving throw the spirit makes.",
            ),
        ),
    )

    private val DINO_SLAM = StatblockAction(
        name = "Slam",
        description = "Melee Attack Roll: bonus equals your spell attack modifier, reach 10 ft. " +
            "Hit: 1d10 + 5 plus the spell's level Bludgeoning damage.",
        toHit = SPELL_ATTACK,
        damageDice = "1d10",
        damageBonus = Formula.Sum(listOf(Formula.Flat(5), Formula.SpellLevel)),
        damageType = "Bludgeoning",
        reach = "10 ft.",
    )

    private val DINOSAURS = listOf(
        dinosaur(
            "dinosaur_spirit_ankylosaur", "Ankylosaur", 13,
            listOf(
                DINO_SLAM,
                StatblockAction(
                    name = "Siege Monster",
                    kind = ActionKind.TRAIT,
                    description = "The spirit deals double damage to objects and structures.",
                ),
            ),
        ),
        dinosaur(
            "dinosaur_spirit_triceratops", "Triceratops", 11,
            listOf(
                StatblockAction(
                    name = "Gore",
                    description = "Melee Attack Roll: bonus equals your spell attack modifier, " +
                        "reach 5 ft. Hit: 1d10 + 5 plus the spell's level Piercing damage. If " +
                        "the target is a Huge or smaller creature and the spirit moved 20+ feet " +
                        "straight toward it immediately before the hit, the target takes an " +
                        "extra 1d10 Piercing damage and has the Prone condition.",
                    toHit = SPELL_ATTACK,
                    damageDice = "1d10",
                    damageBonus = Formula.Sum(listOf(Formula.Flat(5), Formula.SpellLevel)),
                    damageType = "Piercing",
                    reach = "5 ft.",
                ),
                DINO_SLAM,
            ),
        ),
        dinosaur(
            "dinosaur_spirit_tyrannosaur", "Tyrannosaur", 11,
            listOf(
                StatblockAction(
                    name = "Bite",
                    description = "Melee Attack Roll: bonus equals your spell attack modifier, " +
                        "reach 10 ft. Hit: 2d10 + 5 plus the spell's level Piercing damage. If " +
                        "the target is a Large or smaller creature, it has the Grappled " +
                        "condition (escape DC equal to your spell save DC), and it has the " +
                        "Restrained condition until the grapple ends.",
                    toHit = SPELL_ATTACK,
                    damageDice = "2d10",
                    damageBonus = Formula.Sum(listOf(Formula.Flat(5), Formula.SpellLevel)),
                    damageType = "Piercing",
                    reach = "10 ft.",
                ),
                DINO_SLAM,
            ),
        ),
    )

    val DINOSAUR_FORMS: List<String> = DINOSAURS.map { it.id }

    // ================================================================ Battle Familiar

    private fun battleFamiliar(
        id: String, variant: String, acBase: Int, hp: Int, speed: String,
        extra: List<StatblockAction>,
    ) = Statblock(
        id = id,
        name = "Battle Familiar ($variant)",
        size = "Medium",
        creatureType = "Celestial, Fey, or Fiend",
        armorClass = spiritAc(acBase),
        hitPoints = spiritHp(hp, 5),
        speed = speed,
        abilityScores = scores(16, 16, 12, 8, 13, 10),
        sharesProficiencyBonus = true,
        senses = "Passive Perception 11",
        conditionImmunities = listOf("Charmed", "Frightened"),
        actions = extra + listOf(
            StatblockAction(
                name = "Rend",
                description = "Melee Attack Roll: bonus equals your spell attack modifier, " +
                    "reach 5 ft. Hit: 1d8 + 3 plus the spell's level Force damage.",
                toHit = SPELL_ATTACK,
                damageDice = "1d8",
                damageBonus = Formula.Sum(listOf(Formula.Flat(3), Formula.SpellLevel)),
                damageType = "Force",
                reach = "5 ft.",
            ),
            StatblockAction(
                name = "Multiattack",
                kind = ActionKind.TRAIT,
                description = "The familiar makes a number of Rend attacks equal to half this " +
                    "spell's level (round down). It can replace one with Prowl if available.",
            ),
            StatblockAction(
                name = "Talented",
                kind = ActionKind.TRAIT,
                description = "Add half the spell's level (round down) to any ability check or " +
                    "saving throw the familiar makes.",
            ),
        ),
    )

    private val BATTLE_FAMILIARS = listOf(
        // The Brute is the sturdier one: +2 Armor Class and ten more Hit Points.
        battleFamiliar("battle_familiar_brute", "Brute", 13, 30, "40 ft., Swim 30 ft.", emptyList()),
        battleFamiliar(
            "battle_familiar_flyer", "Flyer", 11, 20, "40 ft., Fly 30 ft. (hover), Swim 30 ft.",
            listOf(
                StatblockAction(
                    name = "Flyby",
                    kind = ActionKind.TRAIT,
                    description = "The familiar doesn't provoke an Opportunity Attack when it " +
                        "flies out of an enemy's reach.",
                ),
            ),
        ),
        battleFamiliar(
            "battle_familiar_stalker", "Stalker", 11, 20, "40 ft., Swim 30 ft.",
            listOf(
                StatblockAction(
                    name = "Prowl",
                    kind = ActionKind.BONUS_ACTION,
                    description = "The familiar moves up to half its Speed without provoking " +
                        "Opportunity Attacks. At the end of this movement, it can take the " +
                        "Hide action.",
                ),
            ),
        ),
    )

    val BATTLE_FAMILIAR_FORMS: List<String> = BATTLE_FAMILIARS.map { it.id }

    private val HEALING_TOUCH = StatblockAction(
        name = "Healing Touch (1/Day)",
        description = "The spirit touches another creature. The target regains Hit Points " +
            "equal to 2d8 + the spell's level.",
    )

    // ================================================================ The rest of the
    // 2024 Summon family, plus four older spells that print a stat block of their own.
    //
    // These were not missing by choice: Summon Beast, Undead, Fey and Elemental were written
    // first and the other five were never added, so a Wizard with Summon Fiend had a spell on
    // the sheet and nothing to put on the table. A test now reads the catalogue's own text and
    // fails the build for any spell whose creature has no stat block.

    private fun spirit(
        id: String, name: String, size: String, type: String,
        acBase: Int, hp: Int, hpPer: Int, speed: String,
        str: Int, dex: Int, con: Int, int: Int, wis: Int, cha: Int,
        actions: List<StatblockAction>,
        resistances: List<String> = emptyList(),
        immunities: List<String> = emptyList(),
        conditionImmunities: List<String> = emptyList(),
        senses: String = "",
        languages: String = "",
    ) = Statblock(
        id = id,
        name = name,
        size = size,
        creatureType = type,
        armorClass = spiritAc(acBase),
        hitPoints = spiritHp(hp, hpPer),
        speed = speed,
        abilityScores = scores(str, dex, con, int, wis, cha),
        sharesProficiencyBonus = true,
        resistances = resistances,
        immunities = immunities,
        conditionImmunities = conditionImmunities,
        senses = senses,
        languages = languages,
        actions = actions,
    )

    private fun multiattack(who: String) = StatblockAction(
        name = "Multiattack",
        kind = ActionKind.TRAIT,
        description = "The $who makes a number of attacks equal to half this spell's level " +
            "(round down).",
    )

    /** "Bonus equals your spell attack modifier … plus the spell's level." */
    private fun spiritAttack(
        name: String, dice: String, bonus: Int, type: String, reach: String, text: String,
    ) = StatblockAction(
        name = name,
        description = text,
        toHit = SPELL_ATTACK,
        damageDice = dice,
        damageBonus = Formula.Sum(listOf(Formula.Flat(bonus), Formula.SpellLevel)),
        damageType = type,
        reach = reach,
    )

    private val ABERRANT = listOf(
        spirit(
            "aberrant_spirit_beholderkin", "Aberrant Spirit (Beholderkin)", "Medium",
            "Aberration", 11, 40, 10, "30 ft., Fly 30 ft. (hover)",
            16, 10, 15, 16, 10, 6,
            listOf(
                spiritAttack(
                    "Eye Ray", "1d8", 3, "Psychic", "range 150 ft.",
                    "Ranged Attack Roll: bonus equals your spell attack modifier, range 150 ft. " +
                        "Hit: 1d8 + 3 + the spell's level Psychic damage.",
                ),
                multiattack("spirit"),
            ),
            immunities = listOf("Psychic"),
            senses = "Darkvision 60 ft., Passive Perception 10",
            languages = "Deep Speech, understands the languages you know",
        ),
        spirit(
            "aberrant_spirit_slaad", "Aberrant Spirit (Slaad)", "Medium", "Aberration",
            11, 40, 10, "30 ft.", 16, 10, 15, 16, 10, 6,
            listOf(
                spiritAttack(
                    "Claw", "1d10", 3, "Slashing", "5 ft.",
                    "Melee Attack Roll: bonus equals your spell attack modifier, reach 5 ft. " +
                        "Hit: 1d10 + 3 + the spell's level Slashing damage, and the target " +
                        "can't regain Hit Points until the start of the spirit's next turn.",
                ),
                multiattack("spirit"),
                StatblockAction(
                    name = "Regeneration",
                    kind = ActionKind.TRAIT,
                    description = "The spirit regains 5 Hit Points at the start of its turn if " +
                        "it has at least 1 Hit Point.",
                ),
            ),
            immunities = listOf("Psychic"),
            senses = "Darkvision 60 ft., Passive Perception 10",
            languages = "Deep Speech, understands the languages you know",
        ),
        spirit(
            "aberrant_spirit_mind_flayer", "Aberrant Spirit (Mind Flayer)", "Medium",
            "Aberration", 11, 40, 10, "30 ft.", 16, 10, 15, 16, 10, 6,
            listOf(
                spiritAttack(
                    "Psychic Slam", "1d8", 3, "Psychic", "5 ft.",
                    "Melee Attack Roll: bonus equals your spell attack modifier, reach 5 ft. " +
                        "Hit: 1d8 + 3 + the spell's level Psychic damage.",
                ),
                multiattack("spirit"),
                StatblockAction(
                    name = "Whispering Aura",
                    kind = ActionKind.TRAIT,
                    description = "At the start of each of the spirit's turns, it emits psionic " +
                        "energy if it doesn't have the Incapacitated condition. Wisdom Saving " +
                        "Throw: DC equals your spell save DC, each creature (other than you) " +
                        "within 5 feet of the spirit. Failure: 2d6 Psychic damage.",
                ),
            ),
            immunities = listOf("Psychic"),
            senses = "Darkvision 60 ft., Passive Perception 10",
            languages = "Deep Speech, understands the languages you know",
        ),
    )

    private val CELESTIAL = listOf(
        spirit(
            "celestial_spirit_avenger", "Celestial Spirit (Avenger)", "Large", "Celestial",
            11, 40, 10, "30 ft., Fly 40 ft.", 16, 14, 16, 10, 14, 16,
            listOf(
                spiritAttack(
                    "Radiant Bow", "2d6", 2, "Radiant", "range 600 ft.",
                    "Ranged Attack Roll: bonus equals your spell attack modifier, range 600 ft. " +
                        "Hit: 2d6 + 2 + the spell's level Radiant damage.",
                ),
                multiattack("spirit"),
                HEALING_TOUCH,
            ),
            resistances = listOf("Radiant"),
            conditionImmunities = listOf("Charmed", "Frightened"),
            senses = "Darkvision 60 ft., Passive Perception 12",
            languages = "Celestial, understands the languages you know",
        ),
        spirit(
            // The Defender is the one with the +2 to Armor Class.
            "celestial_spirit_defender", "Celestial Spirit (Defender)", "Large", "Celestial",
            13, 40, 10, "30 ft., Fly 40 ft.", 16, 14, 16, 10, 14, 16,
            listOf(
                spiritAttack(
                    "Radiant Mace", "1d10", 3, "Radiant", "5 ft.",
                    "Melee Attack Roll: bonus equals your spell attack modifier, reach 5 ft. " +
                        "Hit: 1d10 + 3 + the spell's level Radiant damage, and the spirit can " +
                        "choose itself or another creature it can see within 10 feet of the " +
                        "target. The chosen creature gains 1d10 Temporary Hit Points.",
                ),
                multiattack("spirit"),
                HEALING_TOUCH,
            ),
            resistances = listOf("Radiant"),
            conditionImmunities = listOf("Charmed", "Frightened"),
            senses = "Darkvision 60 ft., Passive Perception 12",
            languages = "Celestial, understands the languages you know",
        ),
    )

    private val CONSTRUCT_SLAM = spiritAttack(
        "Slam", "1d8", 4, "Bludgeoning", "5 ft.",
        "Melee Attack Roll: bonus equals your spell attack modifier, reach 5 ft. Hit: 1d8 + 4 " +
            "+ the spell's level Bludgeoning damage.",
    )

    private fun construct(id: String, variant: String, extra: StatblockAction?) = spirit(
        id, "Construct Spirit ($variant)", "Medium", "Construct",
        13, 40, 15, "30 ft.", 18, 10, 18, 14, 11, 5,
        listOfNotNull(CONSTRUCT_SLAM, multiattack("spirit"), extra),
        resistances = listOf("Poison"),
        conditionImmunities = listOf(
            "Charmed", "Exhaustion", "Frightened", "Paralyzed", "Poisoned",
        ),
        senses = "Darkvision 60 ft., Passive Perception 10",
        languages = "Understands the languages that you know",
    )

    private val CONSTRUCTS = listOf(
        construct(
            "construct_spirit_clay", "Clay",
            StatblockAction(
                name = "Berserk Lashing",
                kind = ActionKind.REACTION,
                description = "Trigger: the spirit takes damage from a creature. Response: the " +
                    "spirit makes a Slam attack against that creature if possible, or moves up " +
                    "to half its Speed toward it without provoking Opportunity Attacks.",
            ),
        ),
        construct(
            "construct_spirit_metal", "Metal",
            StatblockAction(
                name = "Heated Body",
                kind = ActionKind.TRAIT,
                description = "A creature that hits the spirit with a melee attack, or that " +
                    "starts its turn grappling it, takes 1d10 Fire damage.",
            ),
        ),
        construct(
            "construct_spirit_stone", "Stone",
            StatblockAction(
                name = "Stony Lethargy",
                kind = ActionKind.TRAIT,
                description = "When a creature starts its turn within 10 feet of the spirit, " +
                    "the spirit can target it. Wisdom Saving Throw: DC equals your spell save " +
                    "DC. Failure: until the start of its next turn, the target can't make " +
                    "Opportunity Attacks and its Speed is halved.",
            ),
        ),
    )

    private val DRACONIC_SPIRIT = spirit(
        "draconic_spirit", "Draconic Spirit", "Large", "Dragon",
        14, 50, 10, "30 ft., Fly 60 ft., Swim 30 ft.", 19, 14, 17, 10, 14, 14,
        listOf(
            spiritAttack(
                "Rend", "1d6", 4, "Piercing", "10 ft.",
                "Melee Attack Roll: bonus equals your spell attack modifier, reach 10 ft. " +
                    "Hit: 1d6 + 4 + the spell's level Piercing damage.",
            ),
            StatblockAction(
                name = "Breath Weapon",
                description = "Dexterity Saving Throw: DC equals your spell save DC, each " +
                    "creature in a 30-foot Cone. Failure: 2d6 damage of a type this spirit has " +
                    "Resistance to (your choice when you cast the spell). Success: half damage.",
            ),
            StatblockAction(
                name = "Multiattack",
                kind = ActionKind.TRAIT,
                description = "The spirit makes a number of Rend attacks equal to half this " +
                    "spell's level (round down), and it uses Breath Weapon.",
            ),
            StatblockAction(
                name = "Shared Resistances",
                kind = ActionKind.TRAIT,
                description = "When you summon the spirit, choose one of its Resistances. You " +
                    "have Resistance to the chosen damage type until the spell ends.",
            ),
        ),
        resistances = listOf("Acid", "Cold", "Fire", "Lightning", "Poison"),
        conditionImmunities = listOf("Charmed", "Frightened", "Poisoned"),
        senses = "Blindsight 30 ft., Darkvision 60 ft., Passive Perception 12",
        languages = "Draconic, understands the languages you know",
    )

    private fun fiend(
        id: String, variant: String, hp: Int, speed: String,
        attack: StatblockAction, extra: StatblockAction?,
    ) = spirit(
        id, "Fiendish Spirit ($variant)", "Large", "Fiend",
        12, hp, 15, speed, 13, 16, 15, 10, 10, 16,
        listOfNotNull(
            attack, multiattack("spirit"),
            StatblockAction(
                name = "Magic Resistance",
                kind = ActionKind.TRAIT,
                description = "The spirit has Advantage on saving throws against spells and " +
                    "other magical effects.",
            ),
            extra,
        ),
        resistances = listOf("Fire"),
        immunities = listOf("Poison"),
        conditionImmunities = listOf("Poisoned"),
        senses = "Darkvision 60 ft., Passive Perception 10",
        languages = "Abyssal, Infernal, Telepathy 60 ft.",
    )

    private val FIENDS = listOf(
        fiend(
            "fiendish_spirit_demon", "Demon", 50, "40 ft., Climb 40 ft.",
            spiritAttack(
                "Bite", "1d12", 3, "Necrotic", "5 ft.",
                "Melee Attack Roll: bonus equals your spell attack modifier, reach 5 ft. Hit: " +
                    "1d12 + 3 + the spell's level Necrotic damage.",
            ),
            StatblockAction(
                name = "Death Throes",
                kind = ActionKind.TRAIT,
                description = "When the spirit drops to 0 Hit Points or the spell ends, it " +
                    "explodes. Dexterity Saving Throw: DC equals your spell save DC, each " +
                    "creature in a 10-foot Emanation. Failure: 2d10 plus this spell's level " +
                    "Fire damage. Success: half damage.",
            ),
        ),
        fiend(
            "fiendish_spirit_devil", "Devil", 40, "40 ft., Fly 60 ft.",
            spiritAttack(
                "Fiery Strike", "2d6", 3, "Fire", "5 ft. or range 150 ft.",
                "Melee or Ranged Attack Roll: bonus equals your spell attack modifier, reach " +
                    "5 ft. or range 150 ft. Hit: 2d6 + 3 + the spell's level Fire damage.",
            ),
            StatblockAction(
                name = "Devil's Sight",
                kind = ActionKind.TRAIT,
                description = "Magical Darkness doesn't impede the spirit's Darkvision.",
            ),
        ),
        fiend(
            "fiendish_spirit_yugoloth", "Yugoloth", 60, "40 ft.",
            spiritAttack(
                "Claws", "1d8", 3, "Slashing", "5 ft.",
                "Melee Attack Roll: bonus equals your spell attack modifier, reach 5 ft. Hit: " +
                    "1d8 + 3 + the spell's level Slashing damage. Immediately after the attack " +
                    "hits or misses, the spirit can teleport up to 30 feet to an unoccupied " +
                    "space it can see.",
            ),
            null,
        ),
    )

    private fun insect(id: String, variant: String, speed: String, extra: StatblockAction?) =
        spirit(
            id, "Giant Insect ($variant)", "Large", "Beast",
            11, 30, 10, speed, 17, 13, 15, 4, 14, 3,
            listOfNotNull(
                spiritAttack(
                    "Poison Jab", "1d6", 3, "Piercing", "10 ft.",
                    "Melee Attack Roll: bonus equals your spell attack modifier, reach 10 ft. " +
                        "Hit: 1d6 + 3 plus the spell's level Piercing damage plus 1d4 Poison " +
                        "damage.",
                ),
                multiattack("insect"),
                StatblockAction(
                    name = "Spider Climb",
                    kind = ActionKind.TRAIT,
                    description = "The insect can climb difficult surfaces, including along " +
                        "ceilings, without needing to make an ability check.",
                ),
                extra,
            ),
            senses = "Darkvision 60 ft., Passive Perception 12",
            languages = "Understands the languages you know",
        )

    private val INSECTS = listOf(
        insect(
            "giant_insect_centipede", "Centipede", "40 ft., Climb 40 ft.",
            StatblockAction(
                name = "Venomous Spew",
                kind = ActionKind.BONUS_ACTION,
                description = "Constitution Saving Throw: your spell save DC, one creature the " +
                    "insect can see within 10 feet. Failure: the target has the Poisoned " +
                    "condition until the start of the insect's next turn.",
            ),
        ),
        insect(
            "giant_insect_spider", "Spider", "40 ft., Climb 40 ft.",
            spiritAttack(
                "Web Bolt", "1d10", 3, "Bludgeoning", "range 60 ft.",
                "Ranged Attack Roll: bonus equals your spell attack modifier, range 60 ft. " +
                    "Hit: 1d10 + 3 plus the spell's level Bludgeoning damage, and the target's " +
                    "Speed is reduced to 0 until the start of the insect's next turn.",
            ),
        ),
        insect("giant_insect_wasp", "Wasp", "40 ft., Climb 40 ft., Fly 40 ft.", null),
    )

    private val HOMUNCULUS = Statblock(
        id = "homunculus_servant",
        name = "Homunculus Servant",
        size = "Tiny",
        creatureType = "Construct",
        armorClass = Formula.Flat(13),
        hitPoints = spiritHp(0, 5),
        speed = "20 ft., Fly 30 ft.",
        abilityScores = scores(4, 15, 12, 10, 10, 7),
        sharesProficiencyBonus = true,
        immunities = listOf("Poison"),
        conditionImmunities = listOf("Exhaustion", "Poisoned"),
        senses = "Darkvision 60 ft., Passive Perception 10",
        languages = "Telepathy 1 mile (works only with you)",
        actions = listOf(
            spiritAttack(
                "Force Strike", "1d6", 0, "Force", "5 ft. or range 30 ft.",
                "Melee or Ranged Attack Roll: bonus equals your spell attack modifier, reach " +
                    "5 ft. or range 30 ft. Hit: 1d6 plus the spell's level Force damage.",
            ),
            StatblockAction(
                name = "Channel Magic",
                kind = ActionKind.REACTION,
                description = "Trigger: you cast a spell with a range of touch while the " +
                    "homunculus is within 120 feet of you. Response: the homunculus delivers " +
                    "the spell through its touch.",
            ),
            StatblockAction(
                name = "Evasion",
                kind = ActionKind.TRAIT,
                description = "On a Dexterity save for half damage the homunculus takes none " +
                    "on a success and half on a failure. Not while Incapacitated.",
            ),
            StatblockAction(
                name = "Magic Bond",
                kind = ActionKind.TRAIT,
                description = "Add the spell's level to any ability check or saving throw the " +
                    "homunculus makes.",
            ),
        ),
    )

    private fun animatedObject(id: String, size: String, hp: Int, damage: String) = Statblock(
        id = id,
        name = "Animated Object ($size)",
        size = size,
        creatureType = "Construct",
        armorClass = Formula.Flat(15),
        hitPoints = Formula.Flat(hp),
        speed = "30 ft.",
        abilityScores = scores(16, 10, 10, 3, 3, 1),
        sharesProficiencyBonus = true,
        immunities = listOf("Poison", "Psychic"),
        conditionImmunities = listOf(
            "Charmed", "Exhaustion", "Frightened", "Paralyzed", "Poisoned",
        ),
        senses = "Blindsight 30 ft., Passive Perception 6",
        languages = "Understands the languages that you know",
        actions = listOf(
            StatblockAction(
                name = "Slam",
                description = "Melee Attack Roll: bonus equals your spell attack modifier, " +
                    "reach 5 ft. Hit: $damage Force damage.",
                toHit = SPELL_ATTACK,
                damageDice = damage.substringBefore(" +"),
                damageType = "Force",
                reach = "5 ft.",
            ),
        ),
    )

    private val ANIMATED_OBJECTS = listOf(
        animatedObject("animated_object_medium", "Medium", 10, "1d4 + 3"),
        animatedObject("animated_object_large", "Large", 20, "2d6 + 3"),
        animatedObject("animated_object_huge", "Huge", 40, "2d12 + 3"),
    )

    private val PHANTOM_STEED = Statblock(
        id = "phantom_steed",
        name = "Phantom Steed",
        size = "Large",
        creatureType = "Beast (Riding Horse)",
        armorClass = Formula.Flat(11),
        hitPoints = Formula.Flat(11),
        // "except it has a Speed of 100 feet and can travel 13 miles in an hour."
        speed = "100 ft.",
        abilityScores = scores(16, 10, 11, 2, 11, 7),
        senses = "Passive Perception 10",
        notes = "Fades when the spell ends, giving the rider 1 minute to dismount. The spell " +
            "ends early if the steed takes any damage.",
        actions = listOf(
            StatblockAction(
                name = "Hooves",
                description = "Melee Attack Roll: +4, reach 5 ft. Hit: 1d6 + 3 Bludgeoning " +
                    "damage.",
                toHit = Formula.Flat(4),
                damageDice = "1d6",
                damageBonus = Formula.Flat(3),
                damageType = "Bludgeoning",
                reach = "5 ft.",
            ),
        ),
    )

    val ABERRANT_FORMS: List<String> = ABERRANT.map { it.id }
    val CELESTIAL_FORMS: List<String> = CELESTIAL.map { it.id }
    val CONSTRUCT_FORMS: List<String> = CONSTRUCTS.map { it.id }
    val FIENDISH_FORMS: List<String> = FIENDS.map { it.id }
    val GIANT_INSECT_FORMS: List<String> = INSECTS.map { it.id }
    val ANIMATED_OBJECT_FORMS: List<String> = ANIMATED_OBJECTS.map { it.id }

    val ALL: List<Statblock> =
        BESTIAL + UNDEAD + FEY + ELEMENTAL + VESTIGES + FAMILIARS + PACT_FAMILIARS +
            PLANTS + DINOSAURS + BATTLE_FAMILIARS +
            ABERRANT + CELESTIAL + CONSTRUCTS + FIENDS + INSECTS + ANIMATED_OBJECTS +
            listOf(
                STEEL_DEFENDER, SKELETON, ZOMBIE, OTHERWORLDLY_STEED,
                DRACONIC_SPIRIT, HOMUNCULUS, PHANTOM_STEED,
            )


    private val byId: Map<String, Statblock> = ALL.associateBy { it.id }

    fun byId(id: String): Statblock? = byId[id]

    fun idsMatching(ids: List<String>): List<Statblock> = ids.mapNotNull(::byId)
}
