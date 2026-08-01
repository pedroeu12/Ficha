package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.EquipmentData
import com.pedroeu.ficha.data.content.PassiveBonusData
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
    /**
     * The weapon mastery property this character can use with this weapon, if any. Blank
     * when the weapon has none or the character hasn't mastered it — the property is still
     * named in [notes] either way, but only a usable one gets its rules text.
     */
    val masteryProperty: String = "",
    val masteryDescription: String = "",
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
     * Base scores plus background bonuses, level-up improvements, the increases the
     * character's feats grant, and any Edit Mode bonus. Species grants no ability bonuses in
     * the 2024 rules.
     *
     * The feat increases are capped at 20 — or 30 for an Epic Boon, which is the one thing
     * that may push a score past the usual ceiling. An Edit Mode override skips all of it.
     */
    fun finalAbilityScores(character: PlayerCharacter): Map<Ability, Int> {
        val fromFeats = FeatBonuses.byAbility(character)
        return Ability.ALL.associateWith { ability ->
            character.abilityScoreOverrides[ability.name] ?: run {
                val base = character.baseAbilityScores[ability.name] ?: 10
                val background = character.backgroundAbilityBonuses[ability.name] ?: 0
                val improvements = character.abilityScoreImprovements[ability.name] ?: 0
                val manual = character.abilityScoreBonuses[ability.name] ?: 0
                val feats = fromFeats[ability] ?: 0
                val withoutFeats = base + background + improvements + manual
                (withoutFeats + feats)
                    .coerceAtMost(maxOf(FeatBonuses.capFor(character, ability), withoutFeats))
            }
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
        val passive = PassiveBonuses.totalFor(character, PassiveBonusData.Target.INITIATIVE)
        return adjust(character, OverridableStat.INITIATIVE, dex + alertBonus + passive)
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

        val passive = PassiveBonuses.totalFor(character, PassiveBonusData.Target.SPEED)
        return adjust(
            character,
            OverridableStat.SPEED,
            base + barbarianFastMovement + monkUnarmoredMovement + passive,
        )
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

        // Each class brings its own hit die, so a Fighter 5 / Wizard 3 rolls d10s for five
        // levels and d6s for three. The very first level of the starting class is maximised.
        val classes = ClassLevels.of(character)
        var levelsCounted = 0
        var fromLevels = 0
        classes.forEach { entry ->
            val die = ClassData.byId(entry.classId)?.hitDie ?: 8
            val average = die / 2 + 1
            repeat(entry.level) { indexInClass ->
                val isVeryFirstLevel = levelsCounted == 0
                val rolled = if (isVeryFirstLevel) {
                    die
                } else {
                    character.hitPointsPerLevel.getOrNull(levelsCounted - 1) ?: average
                }
                fromLevels += rolled + conMod
                levelsCounted++
            }
        }

        // Dwarven Toughness, the Tough feat, and anything else that quietly adds hit points.
        val passive = PassiveBonuses.totalFor(character, PassiveBonusData.Target.MAX_HIT_POINTS)
        val computed = (fromLevels + passive).coerceAtLeast(1)
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
        val withShield = if (monkUnarmoredWins) best else best + shieldBonus
        // A Warforged's plating and anything like it applies whatever the character wears.
        val computed = withShield +
            PassiveBonuses.totalFor(character, PassiveBonusData.Target.ARMOR_CLASS)
        return adjust(character, OverridableStat.ARMOR_CLASS, computed)
    }

    // ------------------------------------------------------------------ Spellcasting

    fun casterType(character: PlayerCharacter): CasterType =
        ProgressionData.forClass(character.classId)?.casterType ?: CasterType.NONE

    /**
     * Shared spell slots for however many classes the character has.
     *
     * A single-class character reads their own table, which is what the rules say and also
     * keeps the Warlock's Pact Magic intact. Anyone with levels in two or more casting
     * classes uses the combined caster level instead, where each class contributes its own
     * fraction of its levels.
     */
    private fun multiclassSlots(character: PlayerCharacter): Map<Int, Int> {
        val classes = ClassLevels.of(character)
        if (classes.size == 1) {
            return SpellSlotTables.slotsFor(casterType(character), character.level)
        }

        val casterLevel = ClassLevels.casterLevel(character)
        val shared = if (casterLevel > 0) {
            SpellSlotTables.slotsFor(CasterType.FULL, casterLevel)
        } else {
            emptyMap()
        }

        // Pact Magic is a separate pool, so a Warlock multiclass keeps both sets of slots.
        val pactLevel = ClassLevels.pactLevel(character)
        if (pactLevel == 0) return shared

        val pact = SpellSlotTables.slotsFor(CasterType.PACT, pactLevel)
        return (shared.keys + pact.keys).associateWith { level ->
            (shared[level] ?: 0) + (pact[level] ?: 0)
        }
    }

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
        val fromTable = multiclassSlots(character)
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

    /**
     * How many spells the character can have prepared, per the class table.
     *
     * Each class contributes what its own level allows, so a Cleric 5 / Wizard 3 prepares a
     * level 5 Cleric's spells plus a level 3 Wizard's rather than reading one table at
     * character level 8.
     */
    fun maxPreparedSpells(character: PlayerCharacter): Int {
        val total = ClassLevels.of(character).sumOf { entry ->
            ProgressionData.forClass(entry.classId)?.preparedSpellsAt(entry.level) ?: 0
        }
        return adjust(character, OverridableStat.MAX_PREPARED_SPELLS, total)
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
                // The mastery property sits alongside the ordinary properties, marked with
                // whether this character can actually use it.
                val masteryNote = CharacterMasteries.noteFor(character, weapon)
                val mastery = CharacterMasteries.forWeapon(character, weapon)
                AttackLine(
                    name = weapon.name,
                    attackBonus = attackBonus,
                    damage = "${weapon.damageDice} ${formatModifier(abilityMod)}",
                    damageType = weapon.damageType,
                    notes = (weapon.properties + masteryNote)
                        .filter { it.isNotBlank() }
                        .joinToString(", "),
                    source = AttackSource.WEAPON,
                    masteryProperty = mastery?.property.orEmpty(),
                    masteryDescription = mastery?.description.orEmpty(),
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
