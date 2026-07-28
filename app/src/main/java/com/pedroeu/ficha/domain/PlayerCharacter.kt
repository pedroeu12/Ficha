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
)

/**
 * A fully built character. Ability scores are stored as base (rolled/assigned) values;
 * species and background bonuses are applied by [finalAbilityScores] so the origin of each
 * point stays visible on the sheet.
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
    val subclassName: String = "",
    val backgroundId: String,

    /** Ability -> base score before any bonuses, keyed by Ability.name. */
    val baseAbilityScores: Map<String, Int>,
    /** Ability -> background bonus (+2/+1), keyed by Ability.name. */
    val backgroundAbilityBonuses: Map<String, Int> = emptyMap(),

    /** Skill.name values the character is proficient in. */
    val skillProficiencies: Set<String> = emptySet(),
    /** Skill.name values with expertise (double proficiency bonus). */
    val skillExpertise: Set<String> = emptySet(),
    val toolProficiencies: List<String> = emptyList(),
    val languages: List<String> = listOf("Common"),

    /** ClassChoice.id -> selected option id(s), for feature options and cantrip picks. */
    val classChoiceSelections: Map<String, List<String>> = emptyMap(),

    val featIds: List<String> = emptyList(),
    val knownSpells: List<KnownSpell> = emptyList(),
    val spellSlotsExpended: Map<String, Int> = emptyMap(),

    val currentHitPoints: Int = 0,
    val temporaryHitPoints: Int = 0,
    val hitDiceSpent: Int = 0,
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
