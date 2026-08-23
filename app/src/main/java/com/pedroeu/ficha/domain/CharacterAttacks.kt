package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SubclassData
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

    /**
     * The attack and damage a hand-written attack works out to.
     *
     * A player who typed a bonus keeps it — that field exists so "DEX + PB" or "spell attack"
     * can be written when the maths doesn't fit. Everything else is derived from the ability
     * chosen, whether proficiency applies, and the magic bonus, so a +1 Longsword picked from
     * three dropdowns comes out right without anyone doing the addition.
     */
    fun customAttackNumbers(
        character: PlayerCharacter,
        attack: CustomAttack,
    ): Pair<String, String> {
        if (attack.bonus.isNotBlank()) return attack.bonus to attack.damageDice

        val ability = Ability.ALL.firstOrNull { it.name == attack.abilityName }
        val abilityMod = ability?.let { CharacterCalculations.abilityModifiers(character)[it] } ?: 0
        val proficiency =
            if (attack.proficient) CharacterCalculations.proficiencyBonus(character) else 0

        val toHit = abilityMod + proficiency + attack.magicBonus
        val damageMod = abilityMod + attack.magicBonus

        val damage = buildString {
            append(attack.damageDice)
            if (damageMod != 0) {
                append(if (damageMod > 0) " + $damageMod" else " - ${-damageMod}")
            }
        }
        return CharacterCalculations.formatModifier(toHit) to damage
    }

    /**
     * Every attack on the sheet, in the order the player put them, with whatever they
     * retyped, and without the ones they took off.
     *
     * A hand-written attack is one of these like any other: same shape, same edits, same
     * place in the order. The only thing that separates it is that deleting it works, where
     * a derived line can only be hidden — it is rebuilt from the inventory every time.
     */
    fun all(character: PlayerCharacter): List<AttackLine> {
        val derived = CharacterCalculations.attacks(character) +
            unarmed(character) +
            featureAttacks(character) +
            cantrips(character)

        val custom = character.customAttacks.map { attack ->
            val (toHit, damage) = customAttackNumbers(character, attack)
            AttackLine(
                id = attack.id,
                name = attack.name,
                attackBonus = 0,
                bonusLabel = toHit,
                damage = damage,
                damageType = attack.damageType,
                notes = listOf(attack.range, attack.notes)
                    .filter { it.isNotBlank() }
                    .joinToString(" • "),
                source = AttackSource.WEAPON,
                isCustom = true,
            )
        }

        val edited = (derived + custom)
            .filterNot { it.id in character.hiddenAttackIds }
            .map { applyOverrides(character, it) }

        // Named ids first in the order the player set; anything new keeps its natural place
        // at the end rather than disappearing into an order it was never added to.
        val position = character.attackOrder.withIndex().associate { (i, id) -> id to i }
        return edited.sortedBy { position[it.id] ?: (position.size + edited.indexOf(it)) }
    }

    /** Text the player typed over a line, from Edit Mode. */
    private fun applyOverrides(character: PlayerCharacter, line: AttackLine): AttackLine {
        fun override(field: String) = character.textOverrides["attack:${line.id}:$field"]
        return line.copy(
            name = override("name") ?: line.name,
            bonusLabel = override("bonus") ?: line.bonusLabel,
            damage = override("damage") ?: line.damage,
            damageType = override("damageType") ?: line.damageType,
            notes = override("notes") ?: line.notes,
        )
    }

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
                id = "unarmed",
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
                id = "feature:" + name.lowercase().replace(Regex("[^a-z0-9]+"), "_"),
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

            // A feature that forces a save rather than rolling to hit still belongs here —
            // it is damage the player rolls on their turn. Written the same way a save
            // cantrip is: the DC in the notes, and the source's attack bonus on the line so
            // the number beside it means something.
            fun saveLine(
                name: String,
                dcId: String,
                damage: String,
                type: String,
                saveAbility: Ability,
                notes: String,
            ) {
                val dc = CharacterDcs.all(character).firstOrNull { it.id == dcId }
                add(
                    AttackLine(
                        id = "feature:$dcId",
                        name = name,
                        attackBonus = dc?.attackBonus ?: 0,
                        damage = damage,
                        damageType = type,
                        notes = "${saveAbility.fullName} save DC ${dc?.dc ?: "—"} • $notes",
                        source = AttackSource.FEATURE,
                    )
                )
            }

            val classLevel = { classId: String -> ClassLevels.levelIn(character, classId) }

            if (character.subclassId == "path_of_lament" && classLevel("barbarian") >= 3) {
                // The wail's dice are the Rage Damage bonus, which is the one number on the
                // Barbarian table that grows without a feature announcing it.
                val rageDamage = when {
                    classLevel("barbarian") >= 16 -> 4
                    classLevel("barbarian") >= 9 -> 3
                    else -> 2
                }
                saveLine(
                    "Banshee's Wail", "subclass:path_of_lament",
                    "${rageDamage}d12", "Psychic", Ability.CON,
                    "30-foot Emanation • Deafened 1 minute on a failure • Half on a success",
                )
            }

            if (character.subclassId == "warrior_of_venom" && classLevel("monk") >= 17) {
                saveLine(
                    "Hallucinogenic Breath", "feature:monk",
                    "3 × Martial Arts die", "Poison", Ability.CON,
                    "2 Focus Points • Replaces one attack • 30 feet • Frightened 1 minute",
                )
            }

            if (character.subclassId == "primordial_patron" && classLevel("warlock") >= 3) {
                val warlockLevel = classLevel("warlock")
                val dice = when {
                    warlockLevel >= 14 -> "3d6"
                    warlockLevel >= 6 -> "2d6"
                    else -> "1d6"
                }
                val element = ChoiceResolver
                    .latestSelectionFor(character, SubclassData.ELEMENT_CHOICE_ID)
                    .firstOrNull()
                saveLine(
                    "Elemental Node", "class:warlock", dice,
                    when (element) {
                        "air" -> "Thunder"
                        "earth" -> "Acid"
                        "water" -> "Cold"
                        "fire" -> "Fire"
                        // No element chosen yet, so no damage type to claim.
                        else -> "—"
                    },
                    Ability.DEX,
                    "Magic action • Saved against on entering, ending a turn there, or the " +
                        "node moving in • Once per turn",
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
                    id = "spell:${spell.id}",
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
