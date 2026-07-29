package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.model.InventoryItem
import kotlinx.serialization.Serializable

@Serializable
data class Coins(
    val cp: Int = 0,
    val sp: Int = 0,
    val ep: Int = 0,
    val gp: Int = 0,
    val pp: Int = 0,
)

@Serializable
data class DeathSaves(
    val successes: Int = 0,
    val failures: Int = 0,
)

/** A spell the character knows or has prepared, flattened for storage. */
@Serializable
data class KnownSpell(
    val id: String,
    val name: String,
    val level: Int,
    val school: String,
    val description: String,
    val prepared: Boolean = true,
    /** Where the spell came from, e.g. "Wizard", "Magic Initiate (Wizard)", "High Elf". */
    val source: String = "",
)

/** An attack or action the player wrote themselves, kept alongside the derived weapon lines. */
@Serializable
data class CustomAttack(
    val id: String,
    val name: String,
    val damageDice: String = "",
    /** Free text so "+7", "DEX + PB", or "spell attack" all work. */
    val bonus: String = "",
    val damageType: String = "",
    val range: String = "",
    val notes: String = "",
)

/** A limited-use resource the player added by hand, for anything the rules engine misses. */
@Serializable
data class CustomResource(
    val id: String,
    val name: String,
    val max: Int,
    /** Matches [com.pedroeu.ficha.data.model.Recharge] by name. */
    val recharge: String,
    val notes: String = "",
)

/** A feature the player wrote themselves, shown on the Features tab. */
@Serializable
data class CustomFeature(
    val id: String,
    val name: String,
    val description: String = "",
    val source: String = "Custom",
)

/**
 * Stats the sheet normally derives from the rules but that Edit Mode can pin to a fixed value.
 * Stored by [name] so the map survives serialization.
 */
enum class OverridableStat(val label: String) {
    MAX_HIT_POINTS("Max HP"),
    ARMOR_CLASS("Armor Class"),
    INITIATIVE("Initiative"),
    SPEED("Speed"),
    PROFICIENCY_BONUS("Proficiency Bonus"),
    PASSIVE_PERCEPTION("Passive Perception"),
    SPELL_SAVE_DC("Spell Save DC"),
    SPELL_ATTACK_BONUS("Spell Attack Bonus"),
    MAX_PREPARED_SPELLS("Prepared Spells"),
    CANTRIPS_KNOWN("Cantrips Known"),
}

/**
 * A fully built character. Ability scores are stored as base (rolled/assigned) values;
 * species, background, and level-up bonuses are applied by the calculation layer so the
 * origin of each point stays visible on the sheet.
 *
 * Anything the rules compute can also be pinned or nudged by the player through Edit Mode.
 * Two mechanisms exist for that, and they compose: a *bonus* map adds to the rules result
 * (for narrative awards like "+2 Stealth from the DM"), while an *override* map replaces it
 * outright. Overrides always win.
 */
@Serializable
data class PlayerCharacter(
    val id: String,
    val name: String,
    val level: Int = 1,
    val experiencePoints: Int = 0,

    val speciesId: String,
    val lineageId: String? = null,
    val classId: String,
    val subclassId: String? = null,
    val backgroundId: String,

    /** Ability -> base score before any bonuses, keyed by Ability.name. */
    val baseAbilityScores: Map<String, Int>,
    /** Ability -> background bonus (+2/+1), keyed by Ability.name. */
    val backgroundAbilityBonuses: Map<String, Int> = emptyMap(),
    /** Ability -> points gained from Ability Score Improvements taken on level up. */
    val abilityScoreImprovements: Map<String, Int> = emptyMap(),
    /** Ability -> free-form bonus added in Edit Mode. */
    val abilityScoreBonuses: Map<String, Int> = emptyMap(),
    /** Ability -> score that replaces the computed total entirely. */
    val abilityScoreOverrides: Map<String, Int> = emptyMap(),

    /** Skill.name values the character is proficient in. */
    val skillProficiencies: Set<String> = emptySet(),
    /** Skill.name values with expertise (double proficiency bonus). */
    val skillExpertise: Set<String> = emptySet(),
    /** Skill.name -> flat bonus added on top of the computed skill modifier. */
    val skillBonuses: Map<String, Int> = emptyMap(),
    /** Skill.name -> total that replaces the computed skill modifier. */
    val skillOverrides: Map<String, Int> = emptyMap(),

    /** Ability.name -> proficient, overriding the class's default saving throws. */
    val saveProficiencyOverrides: Map<String, Boolean> = emptyMap(),
    /** Ability.name -> flat bonus added on top of the computed saving throw. */
    val saveBonuses: Map<String, Int> = emptyMap(),
    /** Ability.name -> total that replaces the computed saving throw. */
    val saveOverrides: Map<String, Int> = emptyMap(),

    /** OverridableStat.name -> pinned value. */
    val statOverrides: Map<String, Int> = emptyMap(),
    /** OverridableStat.name -> flat bonus added to the computed value. */
    val statBonuses: Map<String, Int> = emptyMap(),

    val toolProficiencies: List<String> = emptyList(),
    val armorTraining: List<String> = emptyList(),
    val weaponProficiencies: List<String> = emptyList(),
    val languages: List<String> = listOf("Common"),

    /** ClassChoice.id -> selected option id(s), for feature options and cantrip picks. */
    val classChoiceSelections: Map<String, List<String>> = emptyMap(),
    /** "<level>:<choiceId>" -> selected option id(s), recorded during level up. */
    val levelSelections: Map<String, List<String>> = emptyMap(),
    /** Choice id -> selection for grants that say "of your choice" outside the class table. */
    val originChoiceSelections: Map<String, List<String>> = emptyMap(),

    val featIds: List<String> = emptyList(),
    val knownSpells: List<KnownSpell> = emptyList(),
    val spellSlotsExpended: Map<String, Int> = emptyMap(),
    /** Spell level (as String) -> total slots, replacing the class table value. */
    val spellSlotOverrides: Map<String, Int> = emptyMap(),

    /** Attacks and actions written by the player. */
    val customAttacks: List<CustomAttack> = emptyList(),
    /** Extra features written by the player. */
    val customFeatures: List<CustomFeature> = emptyList(),
    /** Limited-use resources written by the player. */
    val customResources: List<CustomResource> = emptyList(),
    /** Resource id -> uses spent so far. */
    val resourceUses: Map<String, Int> = emptyMap(),
    /** Resource id -> maximum that replaces the derived one. */
    val resourceMaxOverrides: Map<String, Int> = emptyMap(),

    /**
     * Free text the player has rewritten, layered over the static rules content.
     * Keyed as "<scope>:<id>:name" or "<scope>:<id>:description" so a reset is per-field.
     */
    val textOverrides: Map<String, String> = emptyMap(),

    val currentHitPoints: Int = 0,
    val temporaryHitPoints: Int = 0,
    val hitDiceSpent: Int = 0,
    /** Hit points gained at each level after 1st, in level order. */
    val hitPointsPerLevel: List<Int> = emptyList(),
    val deathSaves: DeathSaves = DeathSaves(),
    val heroicInspiration: Boolean = false,

    val inventory: List<InventoryItem> = emptyList(),
    val coins: Coins = Coins(),

    val appearance: String = "",
    val backstory: String = "",
    val alignment: String = "",
    val notes: String = "",

    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
