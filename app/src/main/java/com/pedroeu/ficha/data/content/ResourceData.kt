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
        /** Levels in [classId], which is what class and subclass pools scale on. */
        val level: Int,
        val proficiencyBonus: Int,
        val abilityModifiers: Map<Ability, Int>,
        /**
         * Total character level. Species, lineage, and feat pools key off this rather than
         * off levels in one class — a Triton Fighter 3 / Wizard 2 is character level 5 and
         * has grown into every part of their heritage.
         */
        val characterLevel: Int = level,
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
                        spellId = "divine_smite",
                        name = "Free Divine Smite",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Paladin's Smite",
                        description = "You always have the Divine Smite spell prepared. " +
                            "You can cast it without expending a spell slot once, and you " +
                            "regain the ability to do so when you finish a Long Rest.",
                    )
                )
                if (level >= 5) add(
                    ResourceDef(
                        id = "paladin:find_steed",
                        spellId = "find_steed",
                        name = "Free Find Steed",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Faithful Steed",
                        description = "You always have the Find Steed spell prepared. You " +
                            "can also cast it without expending a spell slot once, and you " +
                            "regain the ability to do so when you finish a Long Rest.",
                    )
                )
            }

            "ranger" -> buildList {
                add(
                    ResourceDef(
                        id = "ranger:favored_enemy",
                        spellId = "hunters_mark",
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

            "artificer" -> buildList {
                add(
                    ResourceDef(
                        id = "artificer:tinkers_magic",
                        name = "Tinker's Magic",
                        max = c.modAtLeastOne(Ability.INT),
                        recharge = Recharge.LONG_REST,
                        source = "Artificer",
                    )
                )
                if (level >= 2) add(
                    ResourceDef(
                        id = "artificer:magic_items",
                        name = "Replicated Magic Items",
                        description = "When you finish a Long Rest with Tinker's Tools in " +
                            "hand, you can create magic items from the plans you know. You " +
                            "can attune to an item the instant you create it. An item made " +
                            "this way works like the real thing, except its magic isn't " +
                            "permanent: it vanishes 1d4 days after you die, or immediately if " +
                            "you replace the plan it was built from. If you exceed your " +
                            "maximum, the oldest item vanishes as the new one appears.",
                        max = when {
                            level >= 18 -> 6
                            level >= 14 -> 5
                            level >= 10 -> 4
                            level >= 6 -> 3
                            else -> 2
                        },
                        recharge = Recharge.SPECIAL,
                        source = "Artificer",
                        notes = "How many items from Replicate Magic Item you currently have. " +
                            "Creating a new one past your maximum makes the oldest vanish.",
                    )
                )
                if (level >= 6) add(
                    ResourceDef(
                        id = "artificer:drain_magic_item",
                        name = "Drain Magic Item",
                        description = "As a Bonus Action you can touch a magic item within 5 " +
                            "feet that you created with Replicate Magic Item and cause it to " +
                            "vanish, converting its magical energy into a spell slot. The slot " +
                            "is level 1 if the item is Common, or level 2 if it is Uncommon or " +
                            "Rare. Any slot created this way vanishes when you finish a Long Rest.",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Magic Item Tinker",
                    )
                )
                if (level >= 7) add(
                    ResourceDef(
                        id = "artificer:flash_of_genius",
                        name = "Flash of Genius",
                        max = c.modAtLeastOne(Ability.INT),
                        recharge = Recharge.LONG_REST,
                        source = "Artificer",
                        notes = when {
                            level >= 20 -> "You regain all uses on a Short Rest while attuned to at least one magic item."
                            level >= 14 -> "You regain one expended use whenever you finish a Short Rest."
                            else -> ""
                        },
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
                    spellId = "misty_step",
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
                        spellId = "animate_dead",
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
                        spellId = "alter_self",
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
                        spellId = "polymorph",
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
                    actionCost = "Magic action",
                    name = "Hand of Ultimate Mercy",
                    max = if (level >= 17) 1 else 0,
                    recharge = Recharge.LONG_REST,
                    source = "Warrior of Mercy",
                )
            )

            "shadow" -> if (level >= 17) listOf(
                ResourceDef(
                    id = "shadow:cloak_of_shadows",
                    spellId = "invisibility",
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
                        spellId = "summon_fey",
                        name = "Free Summon Fey",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Fey Wanderer",
                        description = "You always have the Summon Fey spell prepared. You " +
                            "can cast it without expending a spell slot once, and you " +
                            "regain the ability to do so when you finish a Long Rest.",
                    )
                )
                if (level >= 15) add(
                    ResourceDef(
                        id = "fey_wanderer:misty_wanderer",
                        spellId = "misty_step",
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
                    actionCost = "Bonus Action",
                    name = "Wrath of the Sea",
                    max = c.modAtLeastOne(Ability.WIS),
                    recharge = Recharge.LONG_REST,
                    source = "Circle of the Sea",
                )
            )

            "trickery_domain" -> listOf(
                ResourceDef(
                    id = "trickery:blessing_of_the_trickster",
                    actionCost = "Magic action",
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

            // One casting of Augury or Clairvoyance, then it is spent until a rest.
            "spiritual_guardian" -> if (level >= 10) listOf(
                ResourceDef(
                    id = "spiritual_guardian:consult_the_spirits",
                    name = "Consult the Spirits",
                    max = 1,
                    recharge = Recharge.SHORT_REST,
                    source = "Path of the Spiritual Guardian",
                    notes = "Casts Augury or Clairvoyance with Wisdom, no slot or Material components.",
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

            // Dread Lord is once per Long Rest, but a level 5 spell slot buys it back, so the
            // note says so rather than the pool silently being the only way to get it again.
            "oathbreaker" -> if (level >= 20) listOf(
                ResourceDef(
                    id = "oathbreaker:dread_lord",
                    name = "Dread Lord",
                    max = 1,
                    recharge = Recharge.LONG_REST,
                    source = "Oathbreaker",
                    notes = "You can also restore this by expending a level 5 spell slot.",
                )
            ) else emptyList()

            "devotion" -> if (level >= 20) listOf(
                ResourceDef(
                    id = "devotion:holy_nimbus",
                    actionCost = "Bonus Action",
                    name = "Holy Nimbus",
                    max = 1,
                    recharge = Recharge.LONG_REST,
                    source = "Oath of Devotion",
                )
            ) else emptyList()

            "vengeance" -> if (level >= 20) listOf(
                ResourceDef(
                    id = "vengeance:avenging_angel",
                    actionCost = "Bonus Action",
                    name = "Avenging Angel",
                    max = 1,
                    recharge = Recharge.LONG_REST,
                    source = "Oath of Vengeance",
                )
            ) else emptyList()

            "beast_master" -> emptyList()

            // ------------------------------------------- Artificer subclasses

            "alchemist" -> buildList {
                add(
                    ResourceDef(
                        id = "alchemist:experimental_elixir",
                        name = "Experimental Elixirs",
                        max = when {
                            level >= 15 -> 5
                            level >= 9 -> 4
                            level >= 5 -> 3
                            else -> 2
                        },
                        recharge = Recharge.LONG_REST,
                        source = "Alchemist",
                        notes = "You can also spend a spell slot to brew another, choosing its effect.",
                    )
                )
                if (level >= 9) add(
                    ResourceDef(
                        id = "alchemist:restorative_reagents",
                        spellId = "lesser_restoration",
                        name = "Restorative Reagents",
                        max = c.modAtLeastOne(Ability.INT),
                        recharge = Recharge.LONG_REST,
                        source = "Alchemist",
                        notes = "Free castings of Lesser Restoration through Alchemist's Supplies.",
                    )
                )
                if (level >= 15) add(
                    ResourceDef(
                        id = "alchemist:conjured_cauldron",
                        name = "Conjured Cauldron",
                        description = "You can cast Tasha's Bubbling Cauldron without " +
                            "expending a spell slot, without preparing the spell, and without " +
                            "Material components, provided you use Alchemist's Supplies as the " +
                            "Spellcasting Focus.",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Chemical Mastery",
                        notes = "A free casting of Tasha's Bubbling Cauldron.",
                    )
                )
            }

            "armorer" -> buildList {
                add(
                    ResourceDef(
                        id = "armorer:giant_stature",
                        name = "Giant Stature",
                        description = "As a Bonus Action you transform and enlarge your " +
                            "Dreadnaught armor for 1 minute. Your reach increases by 5 feet, " +
                            "and if you are smaller than Large you become Large along with " +
                            "anything you are wearing. At Artificer level 15 your reach " +
                            "increases by 10 feet instead, you can become Large or Huge, and " +
                            "you have Advantage on Strength checks and Strength saving throws " +
                            "for the duration.",
                        max = c.modAtLeastOne(Ability.INT),
                        recharge = Recharge.LONG_REST,
                        source = "Dreadnaught Armor",
                        notes = "Only applies while your Arcane Armor uses the Dreadnaught model.",
                    )
                )
                if (level >= 15) add(
                    ResourceDef(
                        id = "armorer:perfected_armor",
                        name = "Perfected Armor",
                        max = c.modAtLeastOne(Ability.INT),
                        recharge = Recharge.LONG_REST,
                        source = "Perfected Armor",
                        notes = "The Guardian's pulling Reaction and the Infiltrator's Bonus " +
                            "Action flight both draw on this pool.",
                    )
                )
            }

            "artillerist" -> listOf(
                ResourceDef(
                    id = "artillerist:eldritch_cannon",
                    name = "Eldritch Cannon",
                    max = if (level >= 15) 2 else 1,
                    recharge = Recharge.LONG_REST,
                    source = "Artillerist",
                    notes = "You can also expend a spell slot to build another cannon.",
                )
            )

            "battle_smith" -> if (level >= 9) listOf(
                ResourceDef(
                    id = "battle_smith:arcane_jolt",
                    name = "Arcane Jolt",
                    max = c.modAtLeastOne(Ability.INT),
                    recharge = Recharge.LONG_REST,
                    source = "Battle Smith",
                    notes = "No more than once per turn.",
                )
            ) else emptyList()

            "cartographer" -> buildList {
                add(
                    ResourceDef(
                        id = "cartographer:illuminated_cartography",
                        spellId = "faerie_fire",
                        name = "Illuminated Cartography",
                        description = "You can cast Faerie Fire without expending a spell " +
                            "slot, outlining the affected creatures as if in ink. Your Guided " +
                            "Precision and Superior Atlas features both key off creatures " +
                            "affected by it.",
                        max = c.modAtLeastOne(Ability.INT),
                        recharge = Recharge.LONG_REST,
                        source = "Cartographer",
                        notes = "Free castings of Faerie Fire.",
                    )
                )
                if (level >= 15) add(
                    ResourceDef(
                        id = "cartographer:unerring_path",
                        spellId = "find_the_path",
                        name = "Unerring Path",
                        description = "While you are one of the map holders for your " +
                            "Adventurer's Atlas, you can cast Find the Path without expending " +
                            "a spell slot, without preparing the spell, and without needing " +
                            "spell components.",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Superior Atlas",
                        notes = "A free casting of Find the Path.",
                    )
                )
            }

            // ------------------------------------------- Heroes of Faerûn subclasses

            "college_of_the_moon" -> if (level >= 6) listOf(
                ResourceDef(
                    id = "moon:blessing_of_moonlight",
                    name = "Blessing of Moonlight",
                    max = 1,
                    recharge = Recharge.LONG_REST,
                    source = "College of the Moon",
                    notes = "Modifies one casting of Moonbeam.",
                )
            ) else emptyList()

            "knowledge_domain" -> if (level >= 17) listOf(
                ResourceDef(
                    id = "knowledge:divine_foreknowledge",
                    name = "Divine Foreknowledge",
                    max = 1,
                    recharge = Recharge.LONG_REST,
                    source = "Knowledge Domain",
                    notes = "You can also restore it by expending a level 6+ spell slot.",
                )
            ) else emptyList()

            "banneret" -> listOf(
                ResourceDef(
                    id = "banneret:group_recovery",
                    name = "Group Recovery",
                    max = 1,
                    recharge = Recharge.SHORT_REST,
                    source = "Banneret",
                )
            )

            "noble_genies" -> buildList {
                if (level >= 15) add(
                    ResourceDef(
                        id = "noble_genies:elemental_rebuke",
                        name = "Elemental Rebuke",
                        max = c.modAtLeastOne(Ability.CHA),
                        recharge = Recharge.LONG_REST,
                        source = "Oath of the Noble Genies",
                    )
                )
                if (level >= 20) add(
                    ResourceDef(
                        id = "noble_genies:noble_scion",
                        name = "Noble Scion",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Oath of the Noble Genies",
                        notes = "You can also restore it by expending a level 5 spell slot.",
                    )
                )
            }

            "winter_walker" -> buildList {
                if (level >= 7) add(
                    ResourceDef(
                        id = "winter_walker:fortifying_soul",
                        name = "Fortifying Soul",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Winter Walker",
                    )
                )
                if (level >= 11) add(
                    ResourceDef(
                        id = "winter_walker:chilling_retribution",
                        name = "Chilling Retribution",
                        max = c.modAtLeastOne(Ability.WIS),
                        recharge = Recharge.LONG_REST,
                        source = "Winter Walker",
                    )
                )
                if (level >= 15) add(
                    ResourceDef(
                        id = "winter_walker:frozen_haunt",
                        name = "Frozen Haunt",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Winter Walker",
                        notes = "You can also use it again by expending a level 4+ spell slot.",
                    )
                )
            }

            "scion_of_the_three" -> listOf(
                ResourceDef(
                    id = "scion_three:bloodthirst",
                    name = "Bloodthirst",
                    max = c.modAtLeastOne(Ability.INT),
                    recharge = Recharge.LONG_REST,
                    source = "Scion of the Three",
                    notes = if (level >= 17) {
                        "You regain one expended use whenever you finish a Short Rest."
                    } else {
                        ""
                    },
                )
            )

            "spellfire_sorcery" -> if (level >= 18) listOf(
                ResourceDef(
                    id = "spellfire:crown_of_spellfire",
                    name = "Crown of Spellfire",
                    max = 1,
                    recharge = Recharge.LONG_REST,
                    source = "Spellfire Sorcery",
                    notes = "You can also restore it by spending 5 Sorcery Points.",
                )
            ) else emptyList()

            "bladesinger" -> listOf(
                ResourceDef(
                    id = "bladesinger:bladesong",
                    name = "Bladesong",
                    max = c.modAtLeastOne(Ability.INT),
                    recharge = Recharge.LONG_REST,
                    source = "Bladesinger",
                    notes = "You also regain one expended use when you use Arcane Recovery.",
                )
            )

            // ------------------------------------------ UA 2026: Villainous Options

            "pestilence_domain" -> buildList {
                if (level >= 6) add(
                    ResourceDef(
                        id = "pestilence_domain:virulent_burst",
                        name = "Virulent Burst",
                        max = c.modAtLeastOne(Ability.WIS),
                        recharge = Recharge.LONG_REST,
                        source = "Pestilence Domain",
                    )
                )
                if (level >= 17) add(
                    ResourceDef(
                        id = "pestilence_domain:vermin_form",
                        name = "Vermin Form",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Pestilence Domain",
                        notes = "Or expend a level 5+ spell slot to restore it.",
                    )
                )
            }

            "hell_knight" -> buildList {
                if (level >= 3) add(
                    ResourceDef(
                        id = "hell_knight:infernal_wound",
                        name = "Infernal Wound",
                        max = c.modAtLeastOne(Ability.CON),
                        recharge = Recharge.SHORT_REST,
                        source = "Hell Knight",
                        notes = "Your Infernal Wound Die is a d6.",
                    )
                )
            }

            // ------------------------------------------ UA 2026: Villainous Options 2

            "path_of_lament" -> buildList {
                if (level >= 3) add(
                    ResourceDef(
                        id = "path_of_lament:banshees_wail",
                        name = "Banshee's Wail",
                        max = c.modAtLeastOne(Ability.CON),
                        recharge = Recharge.LONG_REST,
                        source = "Path of Lament",
                        notes = "You can also regain all uses by expending a use of your " +
                            "Rage (no action required).",
                    )
                )
                if (level >= 14) add(
                    ResourceDef(
                        id = "path_of_lament:sorrow_form",
                        name = "Sorrow Form",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Path of Lament",
                        notes = "Activated when you enter your Rage; lasts 1 minute or until " +
                            "you drop to 0 Hit Points.",
                    )
                )
            }

            "primordial_patron" -> buildList {
                if (level >= 3) add(
                    ResourceDef(
                        id = "primordial_patron:elemental_node",
                        name = "Elemental Node",
                        max = 1,
                        recharge = Recharge.SHORT_REST,
                        source = "Primordial Patron",
                        notes = "Or expend a Pact Magic spell slot (no action required) to " +
                            "restore your use of it.",
                    )
                )
                if (level >= 6) add(
                    ResourceDef(
                        id = "primordial_patron:elemental_teleport",
                        name = "Elemental Teleport",
                        max = c.modAtLeastOne(Ability.CHA),
                        recharge = Recharge.LONG_REST,
                        source = "Elemental Haven",
                        description = "As a Bonus Action, you can teleport into your " +
                            "Elemental Node or the nearest unoccupied space within 5 feet " +
                            "of it.",
                    )
                )
                if (level >= 14) add(
                    ResourceDef(
                        id = "primordial_patron:primordial_herald",
                        name = "Primordial Herald",
                        max = 1,
                        // Not a rest at all: 2d4 of them. Tracked as a use the player clears
                        // by hand, because no rest the app knows about should refill it.
                        recharge = Recharge.SPECIAL,
                        source = "Elemental Harbinger",
                        description = "While you're within your node's area, you can cast " +
                            "the Planar Ally spell without expending a spell slot, speaking " +
                            "the name of your patron when you do.",
                        notes = "Once used, you can't do so again until you finish 2d4 Long " +
                            "Rests — roll them and clear this by hand when they are done.",
                    )
                )
            }

            // ------------------------------------------ UA 2025: Horror Subclasses

            "reanimator" -> buildList {
                if (level >= 3) {
                    add(
                        ResourceDef(
                            id = "reanimator:jolt_to_life",
                            name = "Jolt to Life",
                            max = c.modAtLeastOne(Ability.INT),
                            recharge = Recharge.LONG_REST,
                            source = "Reanimator",
                        )
                    )
                    add(
                        ResourceDef(
                            id = "reanimator:companion",
                            name = "Reanimated Companion",
                            max = 1,
                            recharge = Recharge.LONG_REST,
                            source = "Reanimator",
                            notes = "Or expend a spell slot to create another.",
                        )
                    )
                }
            }

            "grave_domain" -> buildList {
                if (level >= 6) add(
                    ResourceDef(
                        id = "grave_domain:sentinel",
                        name = "Sentinel at Death's Door",
                        max = c.modAtLeastOne(Ability.WIS),
                        recharge = Recharge.LONG_REST,
                        source = "Grave Domain",
                    )
                )
                if (level >= 17) add(
                    ResourceDef(
                        id = "grave_domain:keeper_of_souls",
                        name = "Keeper of Souls",
                        max = 1,
                        recharge = Recharge.SHORT_REST,
                        source = "Grave Domain",
                        description = "When an enemy dies within 60 feet of you, you or " +
                            "one creature you can see within 60 feet of yourself regains " +
                            "Hit Points equal to three times your Cleric level. You can't " +
                            "use this if you have the Incapacitated condition.",
                    )
                )
            }

            "phantom" -> buildList {
                if (level >= 3) add(
                    ResourceDef(
                        id = "phantom:wails",
                        name = "Wails from the Grave",
                        max = c.modAtLeastOne(Ability.DEX),
                        recharge = Recharge.LONG_REST,
                        source = "Phantom",
                    )
                )
                if (level >= 9) add(
                    ResourceDef(
                        id = "phantom:soul_trinkets",
                        name = "Soul Trinkets",
                        max = when {
                            level >= 17 -> 4
                            level >= 13 -> 3
                            else -> 2
                        },
                        recharge = Recharge.LONG_REST,
                        source = "Phantom",
                        isPointPool = true,
                        description = "Tiny objects holding the echoes of the dead. Spend " +
                            "one to use Wails from the Grave without expending a use of " +
                            "that feature, or to cast Augury as a Magic action. While you " +
                            "hold at least one, you have Advantage on Death Saving Throws " +
                            "and Constitution saving throws.",
                        notes = "Regain one as a Reaction when a creature dies within 30 feet.",
                    )
                )
                if (level >= 13) add(
                    ResourceDef(
                        id = "phantom:ghost_walk",
                        name = "Ghost Walk",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Phantom",
                        notes = "Or destroy a soul trinket to restore it.",
                    )
                )
            }

            "shadow_sorcery" -> buildList {
                if (level >= 6) add(
                    ResourceDef(
                        id = "shadow_sorcery:spirits_of_ill_omen",
                        spellId = "summon_undead",
                        name = "Summon Undead (free casting)",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Shadow Sorcery",
                    )
                )
                if (level >= 18) add(
                    ResourceDef(
                        id = "shadow_sorcery:umbral_form",
                        name = "Umbral Form",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Shadow Sorcery",
                        notes = "Or spend 6 Sorcery Points to restore it.",
                    )
                )
            }

            "hexblade_patron" -> buildList {
                if (level >= 3) add(
                    ResourceDef(
                        id = "hexblade_patron:curse",
                        spellId = "hex",
                        name = "Hexblade's Curse",
                        max = c.modAtLeastOne(Ability.CHA),
                        recharge = Recharge.LONG_REST,
                        source = "Hexblade Patron",
                        description = "Cast Hex without expending a spell slot. When you " +
                            "do, a spectral weapon resembling your patron orbits the " +
                            "cursed target.",
                    )
                )
                if (level >= 10) add(
                    ResourceDef(
                        id = "hexblade_patron:armor_of_hexes",
                        name = "Armor of Hexes",
                        max = c.modAtLeastOne(Ability.CHA),
                        recharge = Recharge.LONG_REST,
                        source = "Hexblade Patron",
                    )
                )
            }

            "undead_patron" -> buildList {
                if (level >= 3) add(
                    ResourceDef(
                        id = "undead_patron:form_of_dread",
                        name = "Form of Dread",
                        max = c.modAtLeastOne(Ability.CHA),
                        recharge = Recharge.LONG_REST,
                        source = "Undead Patron",
                    )
                )
                if (level >= 10) add(
                    ResourceDef(
                        id = "undead_patron:unholy_resuscitation",
                        name = "Unholy Resuscitation",
                        max = 1,
                        recharge = Recharge.SHORT_REST,
                        source = "Undead Patron",
                        description = "If you drop to 0 Hit Points and don't die outright, " +
                            "your body erupts with deathly energy. Each creature of your " +
                            "choice in a 30-foot Emanation makes a Constitution saving " +
                            "throw, taking 2d10 plus your Warlock level Necrotic damage on " +
                            "a failure. Your Hit Points then change to 10 times your " +
                            "Charisma modifier, and you gain 1 Exhaustion level.",
                    )
                )
            }

            "college_of_spirits" -> buildList {
                if (level >= 6) add(
                    ResourceDef(
                        id = "college_of_spirits:spiritual_manifestation",
                        spellId = "spirit_guardians",
                        name = "Spirit Guardians (free casting)",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "College of Spirits",
                    )
                )
            }

            "hollow_warden" -> emptyList()

            // ------------------------------------------ Pre-existing gaps the audit found

            "demonic_sorcery" -> buildList {
                if (level >= 18) add(
                    ResourceDef(
                        id = "demonic_sorcery:abyssal_explosion",
                        name = "Abyssal Explosion",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Demonic Sorcery",
                        description = "Fill a 30-foot-radius Sphere with Abyssal energy. " +
                            "Each creature there makes a Constitution saving throw, taking " +
                            "8d6 Force damage if it isn't a Fiend and gaining the " +
                            "Incapacitated condition until the start of your next turn.",
                        notes = "Or spend 7 Sorcery Points to restore it.",
                    )
                )
            }

            "draconic" -> buildList {
                if (level >= 18) add(
                    ResourceDef(
                        id = "draconic:dragon_companion",
                        spellId = "summon_dragon",
                        name = "Summon Dragon (free casting)",
                        max = 1,
                        recharge = Recharge.LONG_REST,
                        source = "Draconic Sorcery",
                        description = "Cast Summon Dragon without expending a spell slot, " +
                            "and command the dragon more freely than the spell allows.",
                    )
                )
            }

            else -> emptyList()
        }
    }

    // ------------------------------------------------------------------ Species

    /** A pool of Proficiency Bonus uses that comes back on a Long Rest. */
    private fun pbPerLongRest(c: Context, speciesId: String, name: String, source: String) =
        ResourceDef(
            id = "$speciesId:${name.lowercase().replace(" ", "_")}",
            name = name,
            max = c.proficiencyBonus,
            recharge = Recharge.LONG_REST,
            source = source,
        )

    private fun speciesResources(c: Context): List<ResourceDef> = when (c.speciesId) {
        // Imported species whose trait text promises a Proficiency Bonus of uses per Long
        // Rest. Same shape for all four, so they share one branch.
        "boggart" -> listOf(pbPerLongRest(c, "boggart", "Fury of the Small", "Boggart"))
        "dhampir" -> listOf(pbPerLongRest(c, "dhampir", "Vampiric Bite", "Dhampir"))
        "lupin" -> listOf(pbPerLongRest(c, "lupin", "Howl", "Lupin"))
        "reborn" -> listOf(
            pbPerLongRest(c, "reborn", "Knowledge from a Past Life", "Reborn"),
        )

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
                // Every Giant Ancestry benefit is Proficiency Bonus uses, not one.
                max = c.proficiencyBonus,
                recharge = Recharge.LONG_REST,
                source = "Goliath",
            )
        )

        "human" -> listOf(
            ResourceDef(
                id = "human:resourceful",
                name = "Heroic Inspiration",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Human",
                description = "Resourceful: you gain Heroic Inspiration whenever you finish " +
                    "a Long Rest.",
            )
        )

        "dwarf" -> listOf(
            ResourceDef(
                id = "dwarf:stonecunning",
                name = "Stonecunning",
                max = c.proficiencyBonus,
                recharge = Recharge.LONG_REST,
                source = "Dwarf",
                description = "As a Bonus Action you gain Tremorsense with a range of 60 feet " +
                    "for 10 minutes, provided you are on a stone surface.",
            )
        )

        "elf" -> emptyList()

        "shifter" -> listOf(
            ResourceDef(
                id = "shifter:shifting",
                name = "Shifting",
                max = c.proficiencyBonus,
                recharge = Recharge.LONG_REST,
                source = "Shifter",
            )
        )

        "khoravar" -> listOf(
            ResourceDef(
                id = "khoravar:lethargy_resilience",
                name = "Lethargy Resilience",
                max = 1,
                recharge = Recharge.SPECIAL,
                source = "Khoravar",
                notes = "Returns after 1d4 Long Rests rather than on a fixed schedule.",
            )
        )

        else -> emptyList()
    }

    // ------------------------------------------------------------------ Feats

    /**
     * The ability a feat's "your spellcasting ability" refers to. Feats that say this are
     * taken by casters, so the class's own ability is the right answer; a feat on a class
     * with no spellcasting falls back to Charisma, which is what the Paths of Villainy use.
     */
    private fun spellcastingAbilityFor(c: Context): Ability =
        ClassData.byId(c.classId)?.spellcastingAbility ?: Ability.CHA

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

            // Every dragonmark grants at least one free casting per Long Rest. The Greater
            // version of a mark upgrades that pool rather than adding a second one.
            "mark_of_detection", "mark_of_finding", "mark_of_handling", "mark_of_healing",
            "mark_of_hospitality", "mark_of_making", "mark_of_passage", "mark_of_scribing",
            "mark_of_sentinel", "mark_of_shadow", "mark_of_storm", "mark_of_warding",
            -> {
                val markName = FeatData.byId(featId)?.name ?: "Dragonmark"
                // Greater Mark of Healing turns its single free Cure Wounds into several.
                val improvedHealing =
                    featId == "mark_of_healing" && "greater_mark_of_healing" in c.featIds
                ResourceDef(
                    id = "feat:$featId",
                    name = "$markName Spells",
                    max = if (improvedHealing) c.proficiencyBonus else 1,
                    recharge = Recharge.LONG_REST,
                    source = markName,
                    notes = "Free castings of the spells your mark grants.",
                )
            }

            "aberrant_dragonmark" -> ResourceDef(
                id = "feat:aberrant_dragonmark",
                name = "Aberrant Fortitude",
                max = 1,
                recharge = if ("greater_aberrant_mark" in c.featIds) {
                    Recharge.SHORT_REST
                } else {
                    Recharge.LONG_REST
                },
                source = "Aberrant Dragonmark",
                notes = "Your free casting of the mark's level 1 spell also returns on a rest.",
            )

            "greater_mark_of_handling" -> ResourceDef(
                id = "feat:subdue_animal",
                name = "Subdue Animal",
                max = c.proficiencyBonus,
                recharge = Recharge.LONG_REST,
                source = "Greater Mark of Handling",
            )

            // Imported feats whose text promises a per-rest allowance. Grouped by shape, since
            // the only thing that differs between them is the size of the pool and the rest
            // that refills it.
            "sharp_eye", "vampire_s_plaything", "gathered_whispers", "living_shadow",
            "mist_walker", "symbiotic_being", "bloodlust", "infernal_bulwark",
            -> ResourceDef(
                id = "feat:$featId",
                name = FeatData.byId(featId)?.name ?: featId,
                max = c.proficiencyBonus,
                recharge = Recharge.LONG_REST,
                source = FeatData.byId(featId)?.name ?: featId,
            )

            // Guarded Mind: turn a failed Int, Wis or Cha save into a success.
            "mage_slayer" -> ResourceDef(
                id = "feat:mage_slayer",
                name = "Guarded Mind",
                max = 1,
                recharge = Recharge.SHORT_REST,
                source = "Mage Slayer",
            )

            "tireless_reveler" -> ResourceDef(
                id = "feat:tireless_reveler",
                name = "Tireless Reveler",
                max = c.proficiencyBonus,
                recharge = Recharge.SHORT_REST,
                source = "Tireless Reveler",
            )

            "vampire_hunter", "delicious_pain", "love_bites", "putrefy", "rebuke",
            -> ResourceDef(
                id = "feat:$featId",
                name = FeatData.byId(featId)?.name ?: featId,
                max = 1,
                recharge = Recharge.SHORT_REST,
                source = FeatData.byId(featId)?.name ?: featId,
            )

            "light_bringer", "fey_pact", "fey_tormentor" -> ResourceDef(
                id = "feat:$featId",
                name = FeatData.byId(featId)?.name ?: featId,
                max = 1,
                recharge = Recharge.LONG_REST,
                source = FeatData.byId(featId)?.name ?: featId,
            )

            "greater_mark_of_warding" -> ResourceDef(
                id = "feat:improved_warding",
                name = "Improved Warding",
                max = c.proficiencyBonus,
                recharge = Recharge.LONG_REST,
                source = "Greater Mark of Warding",
            )

            "greater_aberrant_mark" -> ResourceDef(
                id = "feat:mark_of_inspiration",
                name = "Mark of Inspiration",
                max = c.proficiencyBonus,
                recharge = Recharge.LONG_REST,
                source = "Greater Aberrant Mark",
            )

            "potent_dragonmark" -> ResourceDef(
                id = "feat:dragonmark_slot",
                name = "Dragonmark Spell Slot",
                max = 1,
                recharge = Recharge.SHORT_REST,
                source = "Potent Dragonmark",
                notes = "Level equals half your level, rounded up, to a maximum of level 5.",
            )

            // ------------------------------------------------ Heroes of Faerûn feats

            "cult_of_the_dragon_initiate" -> ResourceDef(
                id = "feat:inspired_by_fear",
                name = "Inspired by Fear",
                max = 1,
                recharge = Recharge.SHORT_REST,
                source = "Cult of the Dragon Initiate",
            )

            "emerald_enclave_fledgling" -> ResourceDef(
                id = "feat:speak_with_animals",
                name = "Speak with Animals",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Emerald Enclave Fledgling",
                notes = "A free casting; you can also cast it with spell slots.",
            )

            "purple_dragon_rook" -> ResourceDef(
                id = "feat:rallying_cry",
                name = "Rallying Cry",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Purple Dragon Rook",
            )

            "spellfire_spark" -> ResourceDef(
                id = "feat:spellfire_flame",
                name = "Spellfire Flame",
                max = c.proficiencyBonus,
                recharge = Recharge.LONG_REST,
                source = "Spellfire Spark",
                notes = "Bonus Action castings of Sacred Flame.",
            )

            "fairy_trickster" -> ResourceDef(
                id = "feat:flustering_strike",
                name = "Flustering Strike",
                max = c.proficiencyBonus,
                recharge = Recharge.LONG_REST,
                source = "Fairy Trickster",
            )

            "genie_magic" -> ResourceDef(
                id = "feat:wish_magic",
                name = "Wish Magic",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Genie Magic",
            )

            "mythal_touched" -> ResourceDef(
                id = "feat:mythal_ward",
                name = "Mythal Ward",
                max = c.proficiencyBonus,
                recharge = Recharge.LONG_REST,
                source = "Mythal Touched",
            )

            "lordly_resolve" -> ResourceDef(
                id = "feat:standard_bearer",
                name = "Standard Bearer",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Lordly Resolve",
            )

            "purple_dragon_commandant" -> ResourceDef(
                id = "feat:encourage_ally",
                name = "Encourage Ally",
                max = c.proficiencyBonus,
                recharge = Recharge.LONG_REST,
                source = "Purple Dragon Commandant",
            )

            "enclave_magic" -> ResourceDef(
                id = "feat:beast_sense",
                name = "Beast Sense",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Enclave Magic",
                notes = "A free casting that needs no Concentration.",
            )

            "boon_of_siberys" -> ResourceDef(
                id = "feat:boon_of_siberys",
                name = "Siberys Mark Spell",
                max = 1,
                recharge = Recharge.SHORT_REST,
                source = "Boon of Siberys",
            )

            "boon_of_exquisite_radiance" -> ResourceDef(
                id = "feat:powerful_radiance",
                name = "Powerful Radiance",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Boon of Exquisite Radiance",
            )

            "boon_of_fluid_forms" -> ResourceDef(
                id = "feat:shapechanger",
                name = "Shapechanger",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Boon of Fluid Forms",
            )

            "boon_of_revelry" -> ResourceDef(
                id = "feat:inspire_dance",
                name = "Inspire Dance",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Boon of Revelry",
            )

            "boon_of_terror" -> ResourceDef(
                id = "feat:flee_fools",
                name = "Flee, Fools!",
                max = 1,
                recharge = Recharge.SHORT_REST,
                source = "Boon of Terror",
            )

            "boon_of_the_soul_drinker" -> ResourceDef(
                id = "feat:siphon_life",
                name = "Siphon Life",
                max = 1,
                recharge = Recharge.SHORT_REST,
                source = "Boon of the Soul Drinker",
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

            // ------------------------------------------ Paths of Villainy

            // Death Points are the currency the whole Death Knight path spends: every feat
            // on it casts its spell by expending them rather than a spell slot.
            "death_knight_initiate" -> ResourceDef(
                id = "feat:death_points",
                name = "Death Points",
                max = c.proficiencyBonus,
                recharge = Recharge.LONG_REST,
                source = "Path of the Death Knight",
                isPointPool = true,
                description = "Spend Death Points to cast the spells your Path of the Death " +
                    "Knight feats grant without expending a spell slot. Death Knight " +
                    "Ascension can also spend 1 to 5 of them at once on a Hellfire Orb.",
            )

            "arcane_restoration" -> ResourceDef(
                id = "feat:arcane_restoration",
                name = "Essence Rejuvenation",
                max = 1,
                recharge = Recharge.SHORT_REST,
                source = "Arcane Restoration",
                description = "When you use Soul Siphon to consume a soul, recover one or " +
                    "more expended spell slots with a combined level of no more than 4.",
            )

            "lich_ascension" -> ResourceDef(
                id = "feat:lich_ascension",
                name = "Frightening Gaze",
                max = c.modAtLeastOne(spellcastingAbilityFor(c)),
                recharge = Recharge.LONG_REST,
                source = "Lich Ascension",
                description = "Cast Fear without expending a spell slot, using the " +
                    "spellcasting ability you chose when you took this feat.",
            )

            // ------------------------------------------ Feats whose text limits a use

            "telepathic" -> ResourceDef(
                id = "feat:telepathic",
                name = "Detect Thoughts (free casting)",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Telepathic",
                description = "Cast Detect Thoughts without expending a spell slot, using " +
                    "the ability score this feat increased.",
            )

            "boon_of_the_bandit_king" -> ResourceDef(
                id = "feat:boon_of_the_bandit_king",
                name = "Dastardly Charm",
                max = 1,
                recharge = Recharge.SHORT_REST,
                source = "Boon of the Bandit King",
                description = "When you succeed on a check to pick a pocket, you can cause " +
                    "the target to willingly part with the item and have the Charmed " +
                    "condition for 1 minute or until it takes damage.",
            )

            "greater_mark_of_hospitality" -> ResourceDef(
                id = "feat:greater_mark_of_hospitality",
                name = "Inspired Hospitality",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Greater Mark of Hospitality",
                description = "Modify Purify Food and Drink so each creature of your choice " +
                    "within 30 feet has its Exhaustion reduced by 1 and gains Temporary Hit " +
                    "Points equal to your Proficiency Bonus plus your spellcasting modifier.",
            )

            "greater_mark_of_scribing" -> ResourceDef(
                id = "feat:greater_mark_of_scribing",
                name = "Inspired Scribing",
                max = 1,
                recharge = Recharge.LONG_REST,
                source = "Greater Mark of Scribing",
                description = "Modify Comprehend Languages to cover up to three willing " +
                    "creatures within 30 feet; for the duration you and they can communicate " +
                    "telepathically within 1 mile.",
            )

            else -> null
        }
    }
}
