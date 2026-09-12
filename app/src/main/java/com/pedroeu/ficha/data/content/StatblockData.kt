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

    // ================================================================ The catalogue

    val ALL: List<Statblock> =
        BESTIAL + UNDEAD + FEY + ELEMENTAL + VESTIGES + FAMILIARS +
            listOf(STEEL_DEFENDER, SKELETON, ZOMBIE, OTHERWORLDLY_STEED)

    private val byId: Map<String, Statblock> = ALL.associateBy { it.id }

    fun byId(id: String): Statblock? = byId[id]

    fun idsMatching(ids: List<String>): List<Statblock> = ids.mapNotNull(::byId)
}
