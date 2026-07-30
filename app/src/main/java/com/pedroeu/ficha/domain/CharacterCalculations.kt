package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.EquipmentData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.ArmorCategory
import com.pedroeu.ficha.data.model.CasterType
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.data.model.SpellSlotTables
import com.pedroeu.ficha.data.model.WeaponDef
import kotlin.math.floor

data class AttackLine(
    val name: String,
    val attackBonus: Int,
    val damage: String,
    val damageType: String,
    val notes: String,
    /** Where the line came from, so the sheet can group weapons apart from cantrips. */
    val source: AttackSource = AttackSource.WEAPON,
)

/**
 * Derived statistics for a character. Everything the sheet displays that isn't stored
 * directly is computed here so the stored model stays a record of player decisions only.
 *
 * Every public value runs through the same two-stage adjustment: the rules result is nudged
 * by the matching *bonus* map, then replaced entirely if an *override* is present. That keeps
 * Edit Mode from having to duplicate any of the underlying rules.
 */
object CharacterCalculations {

    fun modifier(score: Int): Int = floor((score - 10) / 2.0).toInt()

    fun formatModifier(mod: Int): String = if (mod >= 0) "+$mod" else "$mod"

    private fun statBonus(character: PlayerCharacter, stat: OverridableStat): Int =
        character.statBonuses[stat.name] ?: 0

    private fun statOverride(character: PlayerCharacter, stat: OverridableStat): Int? =
        character.statOverrides[stat.name]

    /** Applies the bonus map, then lets any override replace the result outright. */
    private fun adjust(character: PlayerCharacter, stat: OverridableStat, computed: Int): Int =
        statOverride(character, stat) ?: (computed + statBonus(character, stat))

    // ------------------------------------------------------------------ Core numbers

    fun proficiencyBonus(character: PlayerCharacter): Int =
        adjust(character, OverridableStat.PROFICIENCY_BONUS, 2 + (character.level - 1) / 4)

    /** Level-only form, for callers that have no character in hand. */
    fun proficiencyBonus(level: Int): Int = 2 + (level - 1) / 4

    /**
     * Base scores plus background bonuses, level-up improvements, and any Edit Mode bonus.
     * Species grants no ability bonuses in the 2024 rules.
     */
    fun finalAbilityScores(character: PlayerCharacter): Map<Ability, Int> =
        Ability.ALL.associateWith { ability ->
            character.abilityScoreOverrides[ability.name] ?: run {
                val base = character.baseAbilityScores[ability.name] ?: 10
                val background = character.backgroundAbilityBonuses[ability.name] ?: 0
                val improvements = character.abilityScoreImprovements[ability.name] ?: 0
                val manual = character.abilityScoreBonuses[ability.name] ?: 0
                base + background + improvements + manual
            }
        }

    fun abilityModifiers(character: PlayerCharacter): Map<Ability, Int> =
        finalAbilityScores(character).mapValues { (_, score) -> modifier(score) }

    // ------------------------------------------------------------------ Saves & skills

    fun isSavingThrowProficient(character: PlayerCharacter, ability: Ability): Boolean =
        character.saveProficiencyOverrides[ability.name]
            ?: (ClassData.byId(character.classId)?.savingThrows?.contains(ability) == true)

    fun savingThrowBonus(character: PlayerCharacter, ability: Ability): Int {
        character.saveOverrides[ability.name]?.let { return it }
        val mods = abilityModifiers(character)
        val proficient = isSavingThrowProficient(character, ability)
        val base = (mods[ability] ?: 0) +
            if (proficient) proficiencyBonus(character) else 0
        return base + (character.saveBonuses[ability.name] ?: 0)
    }

    fun skillBonus(character: PlayerCharacter, skill: Skill): Int {
        character.skillOverrides[skill.name]?.let { return it }
        val mods = abilityModifiers(character)
        val pb = proficiencyBonus(character)
        val abilityMod = mods[skill.ability] ?: 0
        val base = when {
            character.skillExpertise.contains(skill.name) -> abilityMod + pb * 2
            character.skillProficiencies.contains(skill.name) -> abilityMod + pb
            else -> abilityMod
        }
        return base + (character.skillBonuses[skill.name] ?: 0)
    }

    fun passivePerception(character: PlayerCharacter): Int =
        adjust(character, OverridableStat.PASSIVE_PERCEPTION, 10 + skillBonus(character, Skill.PERCEPTION))

    // ------------------------------------------------------------------ Movement & defense

    fun initiative(character: PlayerCharacter): Int {
        val dex = abilityModifiers(character)[Ability.DEX] ?: 0
        val alertBonus = if (character.featIds.contains("alert")) proficiencyBonus(character) else 0
        return adjust(character, OverridableStat.INITIATIVE, dex + alertBonus)
    }

    fun speed(character: PlayerCharacter): Int {
        val species = SpeciesData.byId(character.speciesId)
        // Wood Elf lineage raises base walking speed to 35.
        val base = if (character.lineageId == "wood_elf") 35 else species?.speed ?: 30

        val equippedArmor = character.inventory
            .filter { it.equipped && it.armorDefId != null }
            .mapNotNull { EquipmentData.armorById(it.armorDefId!!) }

        // Fast Movement stops applying in Heavy armor; Unarmored Movement needs no armor at all.
        val inHeavyArmor = equippedArmor.any { it.category == ArmorCategory.HEAVY }
        val barbarianFastMovement =
            if (character.classId == "barbarian" && character.level >= 5 && !inHeavyArmor) 10 else 0
        val monkUnarmoredMovement =
            if (character.classId == "monk" && equippedArmor.isEmpty()) monkSpeedBonus(character.level) else 0

        return adjust(character, OverridableStat.SPEED, base + barbarianFastMovement + monkUnarmoredMovement)
    }

    private fun monkSpeedBonus(level: Int): Int = when {
        level >= 18 -> 30
        level >= 14 -> 25
        level >= 10 -> 20
        level >= 6 -> 15
        level >= 2 -> 10
        else -> 0
    }

    fun size(character: PlayerCharacter): String =
        SpeciesData.byId(character.speciesId)?.size ?: "Medium"

    fun hitDie(character: PlayerCharacter): Int = ClassData.byId(character.classId)?.hitDie ?: 8

    fun maxHitPoints(character: PlayerCharacter): Int {
        val conMod = abilityModifiers(character)[Ability.CON] ?: 0
        val die = hitDie(character)

        // Level 1 always grants the full die. Later levels use whatever was rolled or taken
        // as the fixed average during level up, falling back to the average when unrecorded.
        val average = die / 2 + 1
        val laterLevels = (2..character.level).sumOf { level ->
            val recorded = character.hitPointsPerLevel.getOrNull(level - 2)
            (recorded ?: average) + conMod
        }
        val fromLevels = die + conMod + laterLevels

        val dwarvenToughness = if (character.speciesId == "dwarf") character.level else 0
        val toughFeat = if (character.featIds.contains("tough")) character.level * 2 else 0
        val computed = (fromLevels + dwarvenToughness + toughFeat).coerceAtLeast(1)
        return adjust(character, OverridableStat.MAX_HIT_POINTS, computed).coerceAtLeast(1)
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
        val cha = mods[Ability.CHA] ?: 0

        val equipped = character.inventory.filter { it.equipped && it.armorDefId != null }
            .mapNotNull { EquipmentData.armorById(it.armorDefId!!) }

        val shieldBonus = equipped.filter { it.category == ArmorCategory.SHIELD }.sumOf { it.baseAc }
        val bodyArmor = equipped.firstOrNull { it.category != ArmorCategory.SHIELD }

        val armoredAc = bodyArmor?.let { armor ->
            val dexPart = armor.maxDexBonus?.let { dex.coerceAtMost(it) } ?: dex
            armor.baseAc + dexPart
        }

        val wearingArmor = bodyArmor != null
        val unarmoredAc = when {
            character.classId == "barbarian" && !wearingArmor -> 10 + dex + con
            character.classId == "monk" && equipped.isEmpty() -> 10 + dex + wis
            character.subclassId == "draconic" && !wearingArmor -> 10 + dex + cha
            character.subclassId == "dance" && !wearingArmor -> 10 + dex + cha
            else -> null
        }

        val defaultAc = 10 + dex
        val best = listOfNotNull(armoredAc, unarmoredAc, defaultAc).max()

        // A Monk's Unarmored Defense requires no shield, so only add the shield when it applies.
        val monkUnarmoredWins =
            character.classId == "monk" && unarmoredAc != null && best == unarmoredAc
        val computed = if (monkUnarmoredWins) best else best + shieldBonus
        return adjust(character, OverridableStat.ARMOR_CLASS, computed)
    }

    // ------------------------------------------------------------------ Spellcasting

    fun casterType(character: PlayerCharacter): CasterType =
        ProgressionData.forClass(character.classId)?.casterType ?: CasterType.NONE

    fun spellcastingAbility(character: PlayerCharacter): Ability? =
        ClassData.byId(character.classId)?.spellcastingAbility

    fun spellSaveDc(character: PlayerCharacter): Int? {
        val ability = spellcastingAbility(character) ?: return null
        val mod = abilityModifiers(character)[ability] ?: 0
        return adjust(character, OverridableStat.SPELL_SAVE_DC, 8 + proficiencyBonus(character) + mod)
    }

    fun spellAttackBonus(character: PlayerCharacter): Int? {
        val ability = spellcastingAbility(character) ?: return null
        val mod = abilityModifiers(character)[ability] ?: 0
        return adjust(character, OverridableStat.SPELL_ATTACK_BONUS, proficiencyBonus(character) + mod)
    }

    /** Spell slots by level, from the class table unless Edit Mode has pinned them. */
    fun spellSlots(character: PlayerCharacter): Map<Int, Int> {
        val fromTable = SpellSlotTables.slotsFor(casterType(character), character.level)
        if (character.spellSlotOverrides.isEmpty()) return fromTable

        val merged = fromTable.toMutableMap()
        character.spellSlotOverrides.forEach { (levelKey, total) ->
            val level = levelKey.toIntOrNull() ?: return@forEach
            if (total <= 0) merged.remove(level) else merged[level] = total
        }
        return merged.toSortedMap()
    }

    fun maxSpellLevel(character: PlayerCharacter): Int =
        spellSlots(character).keys.maxOrNull() ?: 0

    /** How many spells the character can have prepared, per the class table. */
    fun maxPreparedSpells(character: PlayerCharacter): Int {
        val progression = ProgressionData.forClass(character.classId) ?: return 0
        return adjust(
            character,
            OverridableStat.MAX_PREPARED_SPELLS,
            progression.preparedSpellsAt(character.level),
        )
    }

    fun maxCantripsKnown(character: PlayerCharacter): Int {
        val progression = ProgressionData.forClass(character.classId) ?: return 0
        return adjust(
            character,
            OverridableStat.CANTRIPS_KNOWN,
            progression.cantripsKnownAt(character.level),
        )
    }

    // ------------------------------------------------------------------ Attacks & load

    /** Attack lines for every weapon the character carries. */
    fun attacks(character: PlayerCharacter): List<AttackLine> {
        val mods = abilityModifiers(character)
        val pb = proficiencyBonus(character)
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
                    source = AttackSource.WEAPON,
                )
            }
    }

    private val SIMPLE_WEAPON_IDS = setOf(
        "club", "dagger", "greatclub", "handaxe", "javelin", "light_hammer", "mace",
        "quarterstaff", "sickle", "spear", "light_crossbow", "dart", "shortbow", "sling",
    )

    private fun isProficientWithWeapon(character: PlayerCharacter, weapon: WeaponDef): Boolean {
        val charClass = ClassData.byId(character.classId) ?: return false
        val profs = charClass.weaponProficiencies + character.weaponProficiencies
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

    /** True when any manual adjustment is in play, so the sheet can offer to reset them. */
    fun hasManualAdjustments(character: PlayerCharacter): Boolean =
        character.abilityScoreOverrides.isNotEmpty() ||
            character.abilityScoreBonuses.isNotEmpty() ||
            character.skillBonuses.isNotEmpty() ||
            character.skillOverrides.isNotEmpty() ||
            character.saveProficiencyOverrides.isNotEmpty() ||
            character.saveBonuses.isNotEmpty() ||
            character.saveOverrides.isNotEmpty() ||
            character.statOverrides.isNotEmpty() ||
            character.statBonuses.isNotEmpty() ||
            character.spellSlotOverrides.isNotEmpty()
}
