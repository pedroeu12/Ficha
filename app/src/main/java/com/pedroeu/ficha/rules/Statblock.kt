package com.pedroeu.ficha.rules

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.PlayerCharacter
import kotlinx.serialization.Serializable

/** One thing a summoned creature can do, with its numbers left as formulas. */
data class StatblockAction(
    val name: String,
    val kind: ActionKind = ActionKind.ACTION,
    val description: String,
    /** "Melee Attack Roll" lines, whose bonus is usually the summoner's spell attack. */
    val toHit: Formula? = null,
    val damageDice: String = "",
    val damageBonus: Formula? = null,
    val damageType: String = "",
    val reach: String = "",
)

enum class ActionKind { ACTION, BONUS_ACTION, REACTION, TRAIT }

/**
 * A creature the character can summon.
 *
 * [armorClass] and [hitPoints] are formulas rather than numbers because a summon's are almost
 * never its own: a Steel Defender's Armor Class is "12 plus your Intelligence modifier", a
 * Vestige Companion has "4 + four times your Warlock level" Hit Points, and every 2024 Summon
 * spell scales with the slot spent. A printed card cannot say any of that; it has to be worked
 * out from whoever summoned it, which is the whole reason summons belong in the engine rather
 * than in a picture of a stat block.
 */
data class Statblock(
    val id: String,
    val name: String,
    val size: String,
    val creatureType: String,
    val armorClass: Formula,
    val hitPoints: Formula,
    val speed: String,
    val abilityScores: Map<Ability, Int>,
    val actions: List<StatblockAction> = emptyList(),
    val resistances: List<String> = emptyList(),
    val immunities: List<String> = emptyList(),
    val conditionImmunities: List<String> = emptyList(),
    val senses: String = "",
    val languages: String = "",
    /**
     * True when the creature adds the summoner's Proficiency Bonus to its checks and saves,
     * which most companions do and most spirits do not.
     */
    val sharesProficiencyBonus: Boolean = false,
    val notes: String = "",
) {
    fun armorClassFor(character: PlayerCharacter, spellLevel: Int, classId: String?): Int =
        FormulaEval.eval(armorClass, character, classId, spellLevel)

    fun hitPointsFor(character: PlayerCharacter, spellLevel: Int, classId: String?): Int =
        FormulaEval.eval(hitPoints, character, classId, spellLevel)

    fun modifier(ability: Ability): Int =
        ((abilityScores[ability] ?: 10) - 10).floorDiv(2)
}

/**
 * A creature currently on the table, with its own hit points and its own expended uses.
 *
 * Stored on the character rather than derived, because the whole point is that it changes: a
 * summon takes damage, spends its once-per-day trait, and is dismissed. Several can be out at
 * once — Animate Dead raises one more corpse each casting — so they are a list keyed by an
 * instance id rather than a single slot.
 */
@Serializable
data class ActiveSummon(
    val instanceId: String,
    val statblockId: String,
    /** The element that summoned it, so the sheet can say what to dismiss. */
    val sourceId: String,
    val sourceLabel: String = "",
    /** Renameable, because five wolves need telling apart. */
    val name: String = "",
    val currentHp: Int = 0,
    val maxHp: Int = 0,
    val tempHp: Int = 0,
    /** The slot level it was summoned with, which its numbers are worked out from. */
    val spellLevel: Int = 0,
    /** Its own limited uses, by the trait's name. */
    val spent: Map<String, Int> = emptyMap(),
    val notes: String = "",
    /** Set when the summoner is concentrating on it, so ending concentration ends it. */
    val concentration: Boolean = false,
) {
    val isBloodied: Boolean get() = maxHp > 0 && currentHp * 2 <= maxHp
    val isDown: Boolean get() = currentHp <= 0
}
