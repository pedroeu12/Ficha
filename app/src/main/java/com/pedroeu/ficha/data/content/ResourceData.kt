package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Recharge
import com.pedroeu.ficha.data.model.ResourceDef

/**
 * Every pool of limited uses the character has, gathered from the class table, the subclass,
 * the species, and any feats. Anything the rules give a number of uses gets a tracker rather
 * than a sentence buried in a feature description.
 *
 * Maxima are the rules' defaults; Edit Mode can override any of them, and a player can add
 * their own resource for anything this doesn't model.
 */
object ResourceData {

    /** Inputs the derivation needs, so it stays free of the character model. */
    data class Context(
        val classId: String,
        val subclassId: String?,
        val speciesId: String,
        val lineageId: String?,
        val featIds: List<String>,
        val level: Int,
        val proficiencyBonus: Int,
        val abilityModifiers: Map<Ability, Int>,
    ) {
        fun mod(ability: Ability): Int = abilityModifiers[ability] ?: 0

        /** Several features scale with an ability modifier but never drop below one use. */
        fun modAtLeastOne(ability: Ability): Int = mod(ability).coerceAtLeast(1)
    }

    fun forContext(context: Context): List<ResourceDef> = buildList {
        addAll(classResources(context))
        addAll(subclassResources(context))
        addAll(speciesResources(context))
        addAll(featResources(context))
    }.filter { it.max > 0 }

    // ------------------------------------------------------------------ Classes

    private fun classResources(c: Context): List<ResourceDef> {
        val level = c.level
        return when (c.classId) {
            "barbarian" -> buildList {
                add(
                    ResourceDef(
                        id = "barbarian:rage",
                        name = "Rage",
                        max = when {
                            level >= 17 -> 6
                            level >= 12 -> 5
                            level >= 6 -> 4
                            level >= 3 -> 3
                            else -> 2
                        },
                        recharge = Recharge.LONG_REST,
                        source = "Barbarian",
                        notes = "You also regain one expended use when you finish a Short Rest.",
                    )
                )
            }

            "bard" -> buildList {
                add(
                    ResourceDef(
                        id = "bard:inspiration",
                        name = "Bardic Inspiration",
                        max = c.modAtLeastOne(Ability.CHA),
                        // Font of Inspiration moves it to a Short Rest at level 5.
                        recharge = if (level >= 5) Recharge.SHORT_REST else Recharge.LONG_REST,
                        source = "Bard",
                    )
                )
            }

            "cleric" -> buildList {
                if (level >= 2) add(
                    ResourceDef(
                        id = "cleric:channel_divinity",
                        name = "Channel Divinity",
                        max = when {
                            level >= 18 -> 4
                            level >= 6 -> 3
                            else -> 2
                        },
                        recharge = Recharge.SHORT_REST,
                        source = "Cleric",
                    )
                )
                if (level >= 10) add(
                    ResourceDef(
                        id = "cleric:divine_intervention",
                        name = "Divine Intervention",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Cleric",
                    )
                )
            }

            "druid" -> buildList {
                if (level >= 2) add(
                    ResourceDef(
                        id = "druid:wild_shape",
                        name = "Wild Shape",
                        max = when {
                            level >= 17 -> 4
                            level >= 6 -> 3
                            else -> 2
                        },
                        recharge = Recharge.SHORT_REST,
                        source = "Druid",
                    )
                )
            }

            "fighter" -> buildList {
                add(
                    ResourceDef(
                        id = "fighter:second_wind",
                        name = "Second Wind",
                        max = when {
                            level >= 10 -> 4
                            level >= 4 -> 3
                            else -> 2
                        },
                        recharge = Recharge.SHORT_REST,
                        source = "Fighter",
                    )
                )
                if (level >= 2) add(
                    ResourceDef(
                        id = "fighter:action_surge",
                        name = "Action Surge",
                        max = if (level >= 17) 2 else 1,
                        recharge = Recharge.SHORT_REST,
                        source = "Fighter",
                    )
                )
                if (level >= 9) add(
                    ResourceDef(
                        id = "fighter:indomitable",
                        name = "Indomitable",
                        max = when {
                            level >= 17 -> 3
                            level >= 13 -> 2
                            else -> 1
                        },
                        recharge = Recharge.LONG_REST,
                        source = "Fighter",
                    )
                )
            }

            "monk" -> buildList {
                if (level >= 2) {
                    add(
                        ResourceDef(
                            id = "monk:focus",
                            name = "Focus Points",
                            max = level,
                            recharge = Recharge.SHORT_REST,
                            source = "Monk",
                            isPointPool = true,
                        )
                    )
                    add(
                        ResourceDef(
                            id = "monk:uncanny_metabolism",
                            name = "Uncanny Metabolism",
                            max = 1,
                            recharge = Recharge.LONG_REST,
                            source = "Monk",
                        )
                    )
                }
            }

            "paladin" -> buildList {
                add(
                    ResourceDef(
                        id = "paladin:lay_on_hands",
                        name = "Lay On Hands",
                        max = level * 5,
                        recharge = Recharge.LONG_REST,
                        source = "Paladin",
                        notes = "A pool of hit points you can spend a few at a time.",
                        isPointPool = true,
                    )
                )
                if (level >= 3) add(
                    ResourceDef(
                        id = "paladin:channel_divinity",
                        name = "Channel Divinity",
                        max = if (level >= 11) 3 else 2,
                        recharge = Recharge.SHORT_REST,
                        source = "Paladin",
                    )
                )
                if (level >= 2) add(
                    ResourceDef(
                        id = "paladin:smite",
                        name = "Free Divine Smite",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Paladin's Smite",
                    )
                )
                if (level >= 5) add(
                    ResourceDef(
                        id = "paladin:find_steed",
                        name = "Free Find Steed",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Faithful Steed",
                    )
                )
            }

            "ranger" -> buildList {
                add(
                    ResourceDef(
                        id = "ranger:favored_enemy",
                        name = "Free Hunter's Mark",
                        max = when {
                            level >= 17 -> 6
                            level >= 13 -> 5
                            level >= 9 -> 4
                            level >= 5 -> 3
                            else -> 2
                        },
                        recharge = Recharge.LONG_REST,
                        source = "Favored Enemy",
                    )
                )
                if (level >= 14) add(
                    ResourceDef(
                        id = "ranger:natures_veil",
                        name = "Nature's Veil",
                        max = c.proficiencyBonus,
                        recharge = Recharge.LONG_REST,
                        source = "Ranger",
                    )
                )
            }

            "rogue" -> buildList {
                if (level >= 20) add(
                    ResourceDef(
                        id = "rogue:stroke_of_luck",
                        name = "Stroke of Luck",
                        max = 1,
                        recharge = Recharge.SHORT_REST,
                        source = "Rogue",
                    )
                )
            }

            "sorcerer" -> buildList {
                add(
                    ResourceDef(
                        id = "sorcerer:innate_sorcery",
                        name = "Innate Sorcery",
                        max = 2,
                        recharge = Recharge.LONG_REST,
                        source = "Sorcerer",
                    )
                )
                if (level >= 2) add(
                    ResourceDef(
                        id = "sorcerer:sorcery_points",
                        name = "Sorcery Points",
                        max = level,
                        recharge = Recharge.LONG_REST,
                        source = "Font of Magic",
                        isPointPool = true,
                        notes = "Sorcerous Restoration returns some on a Short Rest from level 5.",
                    )
                )
            }

            "warlock" -> buildList {
                if (level >= 2) add(
                    ResourceDef(
                        id = "warlock:magical_cunning",
                        name = "Magical Cunning",
                        max = if (level >= 20) 2 else 1,
                        recharge = Recharge.LONG_REST,
                        source = "Warlock",
                        notes = "Spend a minute to regain your Pact Magic spell slots.",
                    )
                )
                // Mystic Arcanum: one free casting at each of levels 11, 13, 15, and 17.
                listOf(11 to 6, 13 to 7, 15 to 8, 17 to 9).forEach { (unlock, spellLevel) ->
                    if (level >= unlock) add(
                        ResourceDef(
                            id = "warlock:arcanum_$spellLevel",
                            name = "Mystic Arcanum (Level $spellLevel)",
                            max = 1,
                            recharge = Recharge.LONG_REST,
                            source = "Warlock",
                        )
                    )
                }
            }

            "wizard" -> buildList {
                add(
                    ResourceDef(
                        id = "wizard:arcane_recovery",
                        name = "Arcane Recovery",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Wizard",
                        notes = "Recover spell slots totalling half your Wizard level on a Short Rest.",
                    )
                )
            }

            else -> emptyList()
        }
    }

    // ------------------------------------------------------------------ Subclasses

    private fun subclassResources(c: Context): List<ResourceDef> {
        val subclass = c.subclassId ?: return emptyList()
        val level = c.level
        return when (subclass) {
            "battle_master" -> listOf(
                ResourceDef(
                    id = "battle_master:superiority",
                    name = "Superiority Dice",
                    max = when {
                        level >= 15 -> 6
                        level >= 7 -> 5
                        else -> 4
                    },
                    recharge = Recharge.SHORT_REST,
                    source = "Battle Master",
                )
            )

            "psi_warrior" -> listOf(
                ResourceDef(
                    id = "psi_warrior:psionic_energy",
                    name = "Psionic Energy Dice",
                    max = c.proficiencyBonus * 2,
                    recharge = Recharge.LONG_REST,
                    source = "Psi Warrior",
                    notes = "You regain one die on a Short Rest, and all of them on a Long Rest.",
                )
            )

            "soulknife" -> listOf(
                ResourceDef(
                    id = "soulknife:psionic_energy",
                    name = "Psionic Energy Dice",
                    max = c.proficiencyBonus * 2,
                    recharge = Recharge.LONG_REST,
                    source = "Soulknife",
                    notes = "You regain one die on a Short Rest, and all of them on a Long Rest.",
                )
            )

            "champion" -> if (level >= 10) listOf(
                ResourceDef(
                    id = "champion:heroic_warrior",
                    name = "Heroic Warrior",
                    max = 1,
                    recharge = Recharge.SHORT_REST,
                    source = "Champion",
                )
            ) else emptyList()

            "zealot" -> listOf(
                ResourceDef(
                    id = "zealot:warrior_of_the_gods",
                    name = "Warrior of the Gods (d12s)",
                    max = when {
                        level >= 17 -> 5
                        level >= 15 -> 4
                        level >= 9 -> 3
                        else -> 2
                    },
                    recharge = Recharge.LONG_REST,
                    source = "Path of the Zealot",
                    isPointPool = true,
                )
            ) + if (level >= 6) listOf(
                ResourceDef(
                    id = "zealot:fanatical_focus",
                    name = "Fanatical Focus",
                    max = 1,
                    recharge = Recharge.SPECIAL,
                    source = "Path of the Zealot",
                    notes = "Once per Rage.",
                )
            ) else emptyList()

            "berserker" -> if (level >= 14) listOf(
                ResourceDef(
                    id = "berserker:intimidating_presence",
                    name = "Intimidating Presence",
                    max = c.proficiencyBonus,
                    recharge = Recharge.LONG_REST,
                    source = "Path of the Berserker",
                )
            ) else emptyList()

            "celestial" -> listOf(
                ResourceDef(
                    id = "celestial:healing_light",
                    name = "Healing Light (d6s)",
                    max = level + 1,
                    recharge = Recharge.LONG_REST,
                    source = "Celestial Patron",
                    isPointPool = true,
                )
            )

            "fiend" -> if (level >= 6) listOf(
                ResourceDef(
                    id = "fiend:dark_ones_own_luck",
                    name = "Dark One's Own Luck",
                    max = c.proficiencyBonus,
                    recharge = Recharge.LONG_REST,
                    source = "Fiend Patron",
                )
            ) else emptyList()

            "archfey" -> listOf(
                ResourceDef(
                    id = "archfey:steps_of_the_fey",
                    name = "Steps of the Fey",
                    max = c.proficiencyBonus,
                    recharge = Recharge.LONG_REST,
                    source = "Archfey Patron",
                )
            )

            "great_old_one" -> if (level >= 6) listOf(
                ResourceDef(
                    id = "goo:clairvoyant_combatant",
                    name = "Clairvoyant Combatant",
                    max = 1,
                    recharge = Recharge.SHORT_REST,
                    source = "Great Old One Patron",
                )
            ) else emptyList()

            "wild_magic" -> listOf(
                ResourceDef(
                    id = "wild_magic:tides_of_chaos",
                    name = "Tides of Chaos",
                    max = 1,
                    recharge = Recharge.LONG_REST,
                    source = "Wild Magic Sorcery",
                )
            )

            "clockwork" -> listOf(
                ResourceDef(
                    id = "clockwork:restore_balance",
                    name = "Restore Balance",
                    max = c.proficiencyBonus,
                    recharge = Recharge.LONG_REST,
                    source = "Clockwork Sorcery",
                )
            )

            "diviner" -> listOf(
                ResourceDef(
                    id = "diviner:portent",
                    name = "Portent Dice",
                    max = if (level >= 14) 3 else 2,
                    recharge = Recharge.LONG_REST,
                    source = "Diviner",
                )
            )

            "abjurer" -> listOf(
                ResourceDef(
                    id = "abjurer:arcane_ward",
                    name = "Arcane Ward (hit points)",
                    max = level * 2 + c.mod(Ability.INT),
                    recharge = Recharge.LONG_REST,
                    source = "Abjurer",
                    isPointPool = true,
                )
            )

            "evoker" -> if (level >= 14) listOf(
                ResourceDef(
                    id = "evoker:overchannel",
                    name = "Overchannel",
                    max = 1,
                    recharge = Recharge.LONG_REST,
                    source = "Evoker",
                    notes = "Further uses in the same rest cost you necrotic damage.",
                )
            ) else emptyList()

            "illusionist" -> if (level >= 10) listOf(
                ResourceDef(
                    id = "illusionist:illusory_self",
                    name = "Illusory Self",
                    max = 1,
                    recharge = Recharge.SHORT_REST,
                    source = "Illusionist",
                )
            ) else emptyList()

            "arcane_archer" -> listOf(
                ResourceDef(
                    id = "arcane_archer:arcane_shot",
                    name = "Arcane Shot",
                    max = c.modAtLeastOne(Ability.INT),
                    recharge = Recharge.SHORT_REST,
                    source = "Arcane Archer",
                )
            ) + if (level >= 7) listOf(
                ResourceDef(
                    id = "arcane_archer:magical_ammunition",
                    name = "Magical Ammunition",
                    max = 1,
                    recharge = Recharge.SHORT_REST,
                    source = "Arcane Archer",
                )
            ) else emptyList()

            "conjurer" -> listOf(
                ResourceDef(
                    id = "conjurer:benign_transposition",
                    name = "Benign Transposition",
                    max = c.modAtLeastOne(Ability.INT),
                    // Distant Transposition moves it to a Short Rest at level 6.
                    recharge = if (level >= 6) Recharge.SHORT_REST else Recharge.LONG_REST,
                    source = "Conjurer",
                )
            )

            "enchanter" -> buildList {
                add(
                    ResourceDef(
                        id = "enchanter:hypnotic_presence",
                        name = "Hypnotic Presence",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Enchanter",
                        notes = "Also returns if you expend a level 1+ spell slot.",
                    )
                )
                if (level >= 6) add(
                    ResourceDef(
                        id = "enchanter:split_enchantment",
                        name = "Split Enchantment",
                        max = c.modAtLeastOne(Ability.INT),
                        recharge = Recharge.LONG_REST,
                        source = "Enchanter",
                    )
                )
                if (level >= 10) add(
                    ResourceDef(
                        id = "enchanter:instinctive_charm",
                        name = "Instinctive Charm",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Enchanter",
                    )
                )
            }

            "necromancer" -> buildList {
                if (level >= 6) add(
                    ResourceDef(
                        id = "necromancer:undead_thralls",
                        name = "Free Animate Dead",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Necromancer",
                    )
                )
            }

            "transmuter" -> buildList {
                add(
                    ResourceDef(
                        id = "transmuter:wondrous_alteration",
                        name = "Free Alter Self",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Transmuter",
                    )
                )
                if (level >= 6) add(
                    ResourceDef(
                        id = "transmuter:empowered_transmutation",
                        name = "Empowered Transmutation",
                        max = c.modAtLeastOne(Ability.INT),
                        recharge = Recharge.LONG_REST,
                        source = "Transmuter",
                    )
                )
                if (level >= 10) add(
                    ResourceDef(
                        id = "transmuter:shapechanger",
                        name = "Free Polymorph",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Transmuter",
                    )
                )
            }

            "mercy" -> listOf(
                ResourceDef(
                    id = "mercy:ultimate_mercy",
                    name = "Hand of Ultimate Mercy",
                    max = if (level >= 17) 1 else 0,
                    recharge = Recharge.LONG_REST,
                    source = "Warrior of Mercy",
                )
            )

            "shadow" -> if (level >= 17) listOf(
                ResourceDef(
                    id = "shadow:cloak_of_shadows",
                    name = "Cloak of Shadows",
                    max = 1,
                    recharge = Recharge.SHORT_REST,
                    source = "Warrior of Shadow",
                )
            ) else emptyList()

            "gloom_stalker" -> if (level >= 15) listOf(
                ResourceDef(
                    id = "gloom_stalker:shadowy_dodge",
                    name = "Shadowy Dodge",
                    max = c.proficiencyBonus,
                    recharge = Recharge.LONG_REST,
                    source = "Gloom Stalker",
                )
            ) else emptyList()

            "fey_wanderer" -> buildList {
                if (level >= 11) add(
                    ResourceDef(
                        id = "fey_wanderer:summon_fey",
                        name = "Free Summon Fey",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Fey Wanderer",
                    )
                )
                if (level >= 15) add(
                    ResourceDef(
                        id = "fey_wanderer:misty_wanderer",
                        name = "Free Misty Step",
                        max = c.modAtLeastOne(Ability.WIS),
                        recharge = Recharge.LONG_REST,
                        source = "Fey Wanderer",
                    )
                )
            }

            "ancients" -> if (level >= 15) listOf(
                ResourceDef(
                    id = "ancients:undying_sentinel",
                    name = "Undying Sentinel",
                    max = 1,
                    recharge = Recharge.LONG_REST,
                    source = "Oath of the Ancients",
                )
            ) else emptyList()

            "glory" -> if (level >= 15) listOf(
                ResourceDef(
                    id = "glory:glorious_defense",
                    name = "Glorious Defense",
                    max = c.modAtLeastOne(Ability.CHA),
                    recharge = Recharge.LONG_REST,
                    source = "Oath of Glory",
                )
            ) else emptyList()

            "land" -> if (level >= 6) listOf(
                ResourceDef(
                    id = "land:natural_recovery",
                    name = "Natural Recovery",
                    max = 1,
                    recharge = Recharge.LONG_REST,
                    source = "Circle of the Land",
                )
            ) else emptyList()

            "stars" -> if (level >= 6) listOf(
                ResourceDef(
                    id = "stars:cosmic_omen",
                    name = "Cosmic Omen",
                    max = c.proficiencyBonus,
                    recharge = Recharge.LONG_REST,
                    source = "Circle of the Stars",
                )
            ) else emptyList()

            "moon" -> if (level >= 10) listOf(
                ResourceDef(
                    id = "moon:moonlight_step",
                    name = "Moonlight Step",
                    max = c.modAtLeastOne(Ability.WIS),
                    recharge = Recharge.LONG_REST,
                    source = "Circle of the Moon",
                )
            ) else emptyList()

            "sea" -> listOf(
                ResourceDef(
                    id = "sea:wrath_of_the_sea",
                    name = "Wrath of the Sea",
                    max = c.modAtLeastOne(Ability.WIS),
                    recharge = Recharge.LONG_REST,
                    source = "Circle of the Sea",
                )
            )

            "trickery_domain" -> listOf(
                ResourceDef(
                    id = "trickery:blessing_of_the_trickster",
                    name = "Blessing of the Trickster",
                    max = c.proficiencyBonus,
                    recharge = Recharge.LONG_REST,
                    source = "Trickery Domain",
                )
            )

            "light_domain" -> listOf(
                ResourceDef(
                    id = "light:warding_flare",
                    name = "Warding Flare",
                    max = c.modAtLeastOne(Ability.WIS),
                    recharge = if (level >= 6) Recharge.SHORT_REST else Recharge.LONG_REST,
                    source = "Light Domain",
                )
            )

            "war_domain" -> listOf(
                ResourceDef(
                    id = "war:war_priest",
                    name = "War Priest",
                    max = c.modAtLeastOne(Ability.WIS),
                    recharge = Recharge.SHORT_REST,
                    source = "War Domain",
                )
            )

            "assassin" -> if (level >= 17) listOf(
                ResourceDef(
                    id = "assassin:death_strike",
                    name = "Death Strike",
                    max = 1,
                    recharge = Recharge.LONG_REST,
                    source = "Assassin",
                )
            ) else emptyList()

            "thief" -> if (level >= 17) listOf(
                ResourceDef(
                    id = "thief:thiefs_reflexes",
                    name = "Thief's Reflexes",
                    max = 1,
                    recharge = Recharge.LONG_REST,
                    source = "Thief",
                )
            ) else emptyList()

            "arcane_trickster" -> if (level >= 17) listOf(
                ResourceDef(
                    id = "arcane_trickster:spell_thief",
                    name = "Spell Thief",
                    max = 1,
                    recharge = Recharge.LONG_REST,
                    source = "Arcane Trickster",
                )
            ) else emptyList()

            "eldritch_knight" -> if (level >= 15) listOf(
                ResourceDef(
                    id = "eldritch_knight:arcane_charge",
                    name = "Arcane Charge",
                    max = 1,
                    recharge = Recharge.SHORT_REST,
                    source = "Eldritch Knight",
                )
            ) else emptyList()

            "world_tree" -> if (level >= 6) listOf(
                ResourceDef(
                    id = "world_tree:branches",
                    name = "Branches of the Tree",
                    max = c.proficiencyBonus,
                    recharge = Recharge.LONG_REST,
                    source = "Path of the World Tree",
                )
            ) else emptyList()

            "devotion" -> if (level >= 20) listOf(
                ResourceDef(
                    id = "devotion:holy_nimbus",
                    name = "Holy Nimbus",
                    max = 1,
                    recharge = Recharge.LONG_REST,
                    source = "Oath of Devotion",
                )
            ) else emptyList()

            "vengeance" -> if (level >= 20) listOf(
                ResourceDef(
                    id = "vengeance:avenging_angel",
                    name = "Avenging Angel",
                    max = 1,
                    recharge = Recharge.LONG_REST,
                    source = "Oath of Vengeance",
                )
            ) else emptyList()

            "beast_master" -> emptyList()

            else -> emptyList()
        }
    }

    // ------------------------------------------------------------------ Species

    private fun speciesResources(c: Context): List<ResourceDef> = when (c.speciesId) {
        "orc" -> listOf(
            ResourceDef(
                id = "orc:adrenaline_rush",
                name = "Adrenaline Rush",
                max = c.proficiencyBonus,
                recharge = Recharge.SHORT_REST,
                source = "Orc",
            ),
            ResourceDef(
                id = "orc:relentless_endurance",
                name = "Relentless Endurance",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Orc",
            ),
        )

        "dragonborn" -> listOf(
            ResourceDef(
                id = "dragonborn:breath_weapon",
                name = "Breath Weapon",
                max = c.proficiencyBonus,
                recharge = Recharge.LONG_REST,
                source = "Dragonborn",
            )
        )

        "aasimar" -> listOf(
            ResourceDef(
                id = "aasimar:healing_hands",
                name = "Healing Hands",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Aasimar",
            ),
            ResourceDef(
                id = "aasimar:celestial_revelation",
                name = "Celestial Revelation",
                max = if (c.level >= 3) 1 else 0,
                recharge = Recharge.LONG_REST,
                source = "Aasimar",
            ),
        )

        "goliath" -> listOf(
            ResourceDef(
                id = "goliath:giant_ancestry",
                name = "Giant Ancestry",
                // Cloud's Jaunt is the one that scales with the proficiency bonus.
                max = if (c.lineageId == "cloud") c.proficiencyBonus else 1,
                recharge = Recharge.LONG_REST,
                source = "Goliath",
            )
        )

        "human" -> listOf(
            ResourceDef(
                id = "human:determined",
                name = "Determined",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Human",
            )
        )

        "elf" -> emptyList()

        else -> emptyList()
    }

    // ------------------------------------------------------------------ Feats

    private fun featResources(c: Context): List<ResourceDef> = c.featIds.mapNotNull { featId ->
        when (featId) {
            "lucky" -> ResourceDef(
                id = "feat:lucky",
                name = "Luck Points",
                max = c.proficiencyBonus,
                recharge = Recharge.LONG_REST,
                source = "Lucky",
                isPointPool = true,
            )

            "musician" -> ResourceDef(
                id = "feat:musician",
                name = "Musician Inspiration",
                max = c.proficiencyBonus,
                recharge = Recharge.SHORT_REST,
                source = "Musician",
            )

            "magic_initiate_cleric", "magic_initiate_druid", "magic_initiate_wizard" -> {
                val list = featId.removePrefix("magic_initiate_").replaceFirstChar { it.uppercase() }
                ResourceDef(
                    id = "feat:$featId",
                    name = "Magic Initiate Spell ($list)",
                    max = 1,
                    recharge = Recharge.LONG_REST,
                    source = FeatData.byId(featId)?.name ?: "Magic Initiate",
                    notes = "Cast the level 1 spell without expending a slot.",
                )
            }

            "fey_touched" -> ResourceDef(
                id = "feat:fey_touched",
                name = "Fey-Touched Spells",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Fey-Touched",
            )

            "shadow_touched" -> ResourceDef(
                id = "feat:shadow_touched",
                name = "Shadow-Touched Spells",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Shadow-Touched",
            )

            "healer" -> ResourceDef(
                id = "feat:healer",
                name = "Healer's Kit Uses",
                max = 10,
                recharge = Recharge.SPECIAL,
                source = "Healer",
                notes = "A Healer's Kit holds ten uses and is replaced, not rested.",
                isPointPool = true,
            )

            "boon_fate" -> ResourceDef(
                id = "feat:boon_fate",
                name = "Boon of Fate",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Boon of Fate",
            )

            "boon_recovery" -> ResourceDef(
                id = "feat:boon_recovery",
                name = "Boon of Recovery",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Boon of Recovery",
            )

            else -> null
        }
    }
}
