package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.model.Ability

/** Where an attack line came from, so the sheet can group and label them. */
enum class AttackSource {
    WEAPON,
    /** Unarmed Strike, and anything that upgrades it. */
    UNARMED,
    /** A weapon a feature conjures, e.g. an Armorer's Lightning Launcher. */
    FEATURE,
    /** A damage cantrip, rolled like any other attack. */
    CANTRIP,
    /** Written by the player. */
    CUSTOM,
}

/**
 * Everything the character can attack with, not just what they're carrying.
 *
 * A Warlock's Eldritch Blast and a Monk's Unarmed Strike are attacks in every sense that
 * matters at the table — they have a to-hit, damage dice, and a damage type — so they belong
 * in the same list as a longsword rather than buried in the spell or feature tabs.
 */
object CharacterAttacks {

    fun all(character: PlayerCharacter): List<AttackLine> =
        CharacterCalculations.attacks(character) +
            unarmed(character) +
            featureAttacks(character) +
            cantrips(character)

    // ------------------------------------------------------------------ Unarmed Strike

    /**
     * Every character can punch. The die and ability depend on what's been layered on top:
     * a Monk's Martial Arts die, Tavern Brawler's d4, or the plain 1 + Strength modifier.
     */
    fun unarmed(character: PlayerCharacter): List<AttackLine> {
        val mods = CharacterCalculations.abilityModifiers(character)
        val pb = CharacterCalculations.proficiencyBonus(character)
        val str = mods[Ability.STR] ?: 0
        val dex = mods[Ability.DEX] ?: 0
        val isMonk = character.classId == "monk"

        // Martial Arts lets a Monk use Dexterity and replaces the damage with a growing die.
        val abilityMod = if (isMonk && dex > str) dex else str
        val martialArtsDie = if (isMonk) martialArtsDie(character.level) else null
        val hasTavernBrawler = "tavern_brawler" in character.featIds

        val damage = when {
            martialArtsDie != null -> martialArtsDie
            hasTavernBrawler -> "1d4"
            else -> "1"
        }

        val notes = buildList {
            if (martialArtsDie != null) add("Martial Arts die")
            if (hasTavernBrawler) add("Tavern Brawler: reroll a 1 on the die")
            if (isMonk) add("Can use Dexterity")
            add("Or deal damage equal to the roll, shove, or grapple instead")
        }

        return listOf(
            AttackLine(
                name = "Unarmed Strike",
                attackBonus = abilityMod + pb,
                damage = "$damage ${CharacterCalculations.formatModifier(abilityMod)}",
                damageType = "Bludgeoning",
                notes = notes.joinToString(" • "),
                source = AttackSource.UNARMED,
            )
        )
    }

    /** The Monk's Martial Arts die, which grows at levels 5, 11, and 17. */
    private fun martialArtsDie(level: Int): String = when {
        level >= 17 -> "1d12"
        level >= 11 -> "1d10"
        level >= 5 -> "1d8"
        else -> "1d6"
    }

    // ------------------------------------------------------------------ Feature weapons

    /**
     * Weapons a feature conjures rather than the character carrying. The Armorer's models are
     * the clearest case: the armour itself is the weapon, and which one depends on the model.
     */
    private fun featureAttacks(character: PlayerCharacter): List<AttackLine> {
        val mods = CharacterCalculations.abilityModifiers(character)
        val pb = CharacterCalculations.proficiencyBonus(character)
        val level = character.level

        fun line(name: String, ability: Ability, damage: String, type: String, notes: String) =
            AttackLine(
                name = name,
                attackBonus = (mods[ability] ?: 0) + pb,
                damage = "$damage ${CharacterCalculations.formatModifier(mods[ability] ?: 0)}",
                damageType = type,
                notes = notes,
                source = AttackSource.FEATURE,
            )

        return buildList {
            if (character.subclassId == "armorer") {
                // Which model is active is a rest-changeable choice, so show the one picked.
                val model = ChoiceResolver.all(character)
                    .firstOrNull { it.choice.id == "armor_model" }
                    ?.selectedIds?.firstOrNull()
                val improved = if (level >= 9) " +1 to attack and damage" else ""
                when (model) {
                    "dreadnaught" -> add(
                        line(
                            "Force Demolisher", Ability.INT,
                            if (level >= 15) "2d6" else "1d10", "Force",
                            "Reach • Push or pull a smaller creature 10 feet$improved",
                        )
                    )
                    "guardian" -> add(
                        line(
                            "Thunder Pulse", Ability.INT,
                            if (level >= 15) "1d10" else "1d8", "Thunder",
                            "The target has Disadvantage on attacks against others$improved",
                        )
                    )
                    "infiltrator" -> add(
                        line(
                            "Lightning Launcher", Ability.INT,
                            if (level >= 15) "2d6" else "1d6", "Lightning",
                            "Range 90/300 • Extra 1d6 Lightning once per turn$improved",
                        )
                    )
                }
            }

            if (character.subclassId == "battle_smith" && level >= 3) {
                add(
                    line(
                        "Steel Defender: Force-Empowered Rend", Ability.INT, "1d8 + 2", "Force",
                        "Your companion's attack, using your spell attack modifier",
                    )
                )
            }

            if (character.subclassId == "soulknife" && level >= 3) {
                val die = when {
                    level >= 17 -> "1d12"
                    level >= 11 -> "1d10"
                    level >= 5 -> "1d8"
                    else -> "1d6"
                }
                add(
                    line(
                        "Psychic Blades", Ability.DEX, die, "Psychic",
                        "Finesse, Thrown (range 60 feet) • A second blade as a Bonus Action",
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------------ Damage cantrips

    /**
     * Damage cantrips the character knows, rolled with the DC or attack bonus of whichever
     * source granted them — a Monk's Magic Initiate cantrip uses that feat's ability, not the
     * Monk's Wisdom.
     */
    private fun cantrips(character: PlayerCharacter): List<AttackLine> {
        val dcs = CharacterDcs.all(character)
        val fallback = CharacterDcs.primary(character)

        return CharacterSpells.all(character)
            .filter { it.level == 0 }
            .mapNotNull { known -> SpellData.byId(known.id)?.let { known to it } }
            .filter { (_, spell) -> spell.damage.isNotBlank() }
            .map { (known, spell) ->
                // Match the cantrip's source to the DC that governs it where we can.
                val dc = dcs.firstOrNull { known.source.isNotBlank() && it.label == known.source }
                    ?: fallback

                val dice = scaledDamage(spell.damage, character.level, spell.scalesWithLevel)
                val notes = buildList {
                    if (spell.needsAttackRoll) {
                        add("Spell attack")
                    } else {
                        spell.saveAbility?.let { add("${it.fullName} save DC ${dc?.dc ?: "—"}") }
                    }
                    if (known.source.isNotBlank()) add(known.source)
                    add(spell.range)
                }

                AttackLine(
                    name = spell.name,
                    attackBonus = dc?.attackBonus ?: 0,
                    damage = dice,
                    damageType = spell.damageType,
                    notes = notes.joinToString(" • "),
                    source = AttackSource.CANTRIP,
                )
            }
    }

    /** Cantrip damage grows at character levels 5, 11, and 17. */
    internal fun scaledDamage(baseDice: String, level: Int, scales: Boolean): String {
        if (!scales) return baseDice
        val die = baseDice.substringAfter('d', "").toIntOrNull() ?: return baseDice
        val count = when {
            level >= 17 -> 4
            level >= 11 -> 3
            level >= 5 -> 2
            else -> 1
        }
        return "${count}d$die"
    }
}
