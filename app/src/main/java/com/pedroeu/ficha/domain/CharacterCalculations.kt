package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.EquipmentData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.ArmorCategory
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.data.model.WeaponDef
import kotlin.math.floor

data class AttackLine(
    val name: String,
    val attackBonus: Int,
    val damage: String,
    val damageType: String,
    val notes: String,
)

/**
 * Derived statistics for a character. Everything the sheet displays that isn't stored
 * directly is computed here so the stored model stays a record of player decisions only.
 */
object CharacterCalculations {

    fun modifier(score: Int): Int = floor((score - 10) / 2.0).toInt()

    fun formatModifier(mod: Int): String = if (mod >= 0) "+$mod" else "$mod"

    fun proficiencyBonus(level: Int): Int = 2 + (level - 1) / 4

    /** Base scores plus background bonuses. Species grants no ability bonuses in the 2024 rules. */
    fun finalAbilityScores(character: PlayerCharacter): Map<Ability, Int> =
        Ability.ALL.associateWith { ability ->
            val base = character.baseAbilityScores[ability.name] ?: 10
            val bg = character.backgroundAbilityBonuses[ability.name] ?: 0
            base + bg
        }

    fun abilityModifiers(character: PlayerCharacter): Map<Ability, Int> =
        finalAbilityScores(character).mapValues { (_, score) -> modifier(score) }

    fun savingThrowBonus(character: PlayerCharacter, ability: Ability): Int {
        val mods = abilityModifiers(character)
        val charClass = ClassData.byId(character.classId)
        val proficient = charClass?.savingThrows?.contains(ability) == true
        return (mods[ability] ?: 0) + if (proficient) proficiencyBonus(character.level) else 0
    }

    fun isSavingThrowProficient(character: PlayerCharacter, ability: Ability): Boolean =
        ClassData.byId(character.classId)?.savingThrows?.contains(ability) == true

    fun skillBonus(character: PlayerCharacter, skill: Skill): Int {
        val mods = abilityModifiers(character)
        val pb = proficiencyBonus(character.level)
        val base = mods[skill.ability] ?: 0
        return when {
            character.skillExpertise.contains(skill.name) -> base + pb * 2
            character.skillProficiencies.contains(skill.name) -> base + pb
            else -> base
        }
    }

    fun passivePerception(character: PlayerCharacter): Int =
        10 + skillBonus(character, Skill.PERCEPTION)

    fun initiative(character: PlayerCharacter): Int {
        val dex = abilityModifiers(character)[Ability.DEX] ?: 0
        val alertBonus = if (character.featIds.contains("alert")) proficiencyBonus(character.level) else 0
        return dex + alertBonus
    }

    fun speed(character: PlayerCharacter): Int {
        val species = SpeciesData.byId(character.speciesId) ?: return 30
        // Wood Elf lineage raises base walking speed to 35.
        return if (character.lineageId == "wood_elf") 35 else species.speed
    }

    fun size(character: PlayerCharacter): String =
        SpeciesData.byId(character.speciesId)?.size ?: "Medium"

    fun hitDie(character: PlayerCharacter): Int = ClassData.byId(character.classId)?.hitDie ?: 8

    fun maxHitPoints(character: PlayerCharacter): Int {
        val conMod = abilityModifiers(character)[Ability.CON] ?: 0
        val die = hitDie(character)
        // Level 1 grants the full die; later levels use the fixed average (die/2 + 1).
        val fromLevels = die + conMod + (character.level - 1) * ((die / 2 + 1) + conMod)
        val dwarvenToughness = if (character.speciesId == "dwarf") character.level else 0
        val toughFeat = if (character.featIds.contains("tough")) character.level * 2 else 0
        return (fromLevels + dwarvenToughness + toughFeat).coerceAtLeast(1)
    }

    /**
     * Best AC available from the character's equipped gear, falling back to the class's
     * Unarmored Defense when that yields more.
     */
    fun armorClass(character: PlayerCharacter): Int {
        val mods = abilityModifiers(character)
        val dex = mods[Ability.DEX] ?: 0
        val con = mods[Ability.CON] ?: 0
        val wis = mods[Ability.WIS] ?: 0

        val equipped = character.inventory.filter { it.equipped && it.armorDefId != null }
            .mapNotNull { EquipmentData.armorById(it.armorDefId!!) }

        val shieldBonus = equipped.filter { it.category == ArmorCategory.SHIELD }.sumOf { it.baseAc }
        val bodyArmor = equipped.firstOrNull { it.category != ArmorCategory.SHIELD }

        val armoredAc = bodyArmor?.let { armor ->
            val dexPart = armor.maxDexBonus?.let { dex.coerceAtMost(it) } ?: dex
            armor.baseAc + dexPart
        }

        val unarmoredAc = when (character.classId) {
            "barbarian" -> 10 + dex + con
            "monk" -> if (equipped.isEmpty()) 10 + dex + wis else null
            else -> null
        }

        val defaultAc = 10 + dex
        val best = listOfNotNull(armoredAc, unarmoredAc, defaultAc).max()

        // A monk's Unarmored Defense requires no shield, so only add the shield when it applies.
        val monkUnarmoredWins = character.classId == "monk" && unarmoredAc != null && best == unarmoredAc
        return if (monkUnarmoredWins) best else best + shieldBonus
    }

    fun spellcastingAbility(character: PlayerCharacter): Ability? =
        ClassData.byId(character.classId)?.spellcastingAbility

    fun spellSaveDc(character: PlayerCharacter): Int? {
        val ability = spellcastingAbility(character) ?: return null
        val mod = abilityModifiers(character)[ability] ?: 0
        return 8 + proficiencyBonus(character.level) + mod
    }

    fun spellAttackBonus(character: PlayerCharacter): Int? {
        val ability = spellcastingAbility(character) ?: return null
        val mod = abilityModifiers(character)[ability] ?: 0
        return proficiencyBonus(character.level) + mod
    }

    /** Level-1 spell slots by class; returns an empty map for non-casters. */
    fun spellSlots(character: PlayerCharacter): Map<Int, Int> {
        val charClass = ClassData.byId(character.classId) ?: return emptyMap()
        if (!charClass.isSpellcaster) return emptyMap()
        return when (charClass.id) {
            "warlock" -> mapOf(1 to 1)
            else -> mapOf(1 to 2)
        }
    }

    /** Attack lines for every weapon the character carries. */
    fun attacks(character: PlayerCharacter): List<AttackLine> {
        val mods = abilityModifiers(character)
        val pb = proficiencyBonus(character.level)
        val str = mods[Ability.STR] ?: 0
        val dex = mods[Ability.DEX] ?: 0

        return character.inventory
            .filter { it.weaponDefId != null }
            .mapNotNull { item -> EquipmentData.weaponById(item.weaponDefId!!) }
            .distinctBy { it.id }
            .map { weapon ->
                val abilityMod = if (weapon.usesDexOption && dex > str) dex else str
                val proficient = isProficientWithWeapon(character, weapon)
                val attackBonus = abilityMod + if (proficient) pb else 0
                AttackLine(
                    name = weapon.name,
                    attackBonus = attackBonus,
                    damage = "${weapon.damageDice} ${formatModifier(abilityMod)}",
                    damageType = weapon.damageType,
                    notes = weapon.properties.joinToString(", "),
                )
            }
    }

    private val SIMPLE_WEAPON_IDS = setOf(
        "club", "dagger", "greatclub", "handaxe", "javelin", "light_hammer", "mace",
        "quarterstaff", "sickle", "spear", "light_crossbow", "dart", "shortbow", "sling",
    )

    private fun isProficientWithWeapon(character: PlayerCharacter, weapon: WeaponDef): Boolean {
        val charClass = ClassData.byId(character.classId) ?: return false
        val profs = charClass.weaponProficiencies
        if (profs.contains("Martial")) return true
        if (profs.contains("Simple") && SIMPLE_WEAPON_IDS.contains(weapon.id)) return true
        // Remaining entries name individual weapons, e.g. "Shortswords" for a Monk.
        return profs.any { prof ->
            val normalized = prof.trimEnd('s').lowercase()
            weapon.name.lowercase() == normalized
        }
    }

    fun carriedWeight(character: PlayerCharacter): Double =
        character.inventory.sumOf { it.weightLb * it.quantity }
}
