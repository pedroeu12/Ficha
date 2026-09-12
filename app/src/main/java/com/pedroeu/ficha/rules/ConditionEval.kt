package com.pedroeu.ficha.rules

import com.pedroeu.ficha.data.content.EquipmentData
import com.pedroeu.ficha.data.model.ArmorCategory
import com.pedroeu.ficha.data.model.ArmorDef
import com.pedroeu.ficha.domain.PlayerCharacter

/**
 * Whether a [Condition] is met, judged from the sheet alone.
 *
 * The division that matters here is between conditions the sheet can *know* and conditions
 * only the table knows. What a character is wearing is written down, so "while you aren't
 * wearing armor" is a question with an answer. Whether they are Raging right now is not, and
 * a bonus applied on a guess is worse than a bonus the player adds themselves — so those
 * report [isComputable] false and never contribute a number. They are still worth carrying as
 * data, because a sheet can list them as "+2 while Raging" without pretending to apply them.
 */
object ConditionEval {

    /** True when the sheet holds enough to decide this without asking the player. */
    fun isComputable(condition: Condition): Boolean = when (condition) {
        Condition.Always,
        Condition.Unarmored,
        Condition.NoShield,
        Condition.WearingArmor,
        Condition.NotInHeavyArmor -> true

        is Condition.All -> condition.parts.all(::isComputable)

        // Momentary states. Real rules, but the sheet does not track the moment.
        Condition.WhileRaging,
        Condition.WhileWildShaped,
        Condition.NotIncapacitated,
        Condition.AtLowHitPoints -> false

        is Condition.Descriptive -> false
    }

    fun holds(condition: Condition, character: PlayerCharacter): Boolean = when (condition) {
        Condition.Always -> true
        Condition.Unarmored -> bodyArmor(character) == null
        Condition.WearingArmor -> bodyArmor(character) != null
        Condition.NoShield -> shieldBonus(character) == 0
        Condition.NotInHeavyArmor -> bodyArmor(character)?.category != ArmorCategory.HEAVY
        is Condition.All -> condition.parts.all { holds(it, character) }
        else -> false
    }

    /** How the condition reads on the sheet, next to the number it governs. */
    fun describe(condition: Condition): String = when (condition) {
        Condition.Always -> ""
        Condition.Unarmored -> "while not wearing armor"
        Condition.NoShield -> "while not wielding a Shield"
        Condition.WearingArmor -> "while wearing armor"
        Condition.NotInHeavyArmor -> "while not wearing Heavy armor"
        is Condition.All -> condition.parts.map(::describe).filter { it.isNotBlank() }
            .joinToString(" and ")
        Condition.WhileRaging -> "while Raging"
        Condition.WhileWildShaped -> "while in Wild Shape"
        Condition.NotIncapacitated -> "while not Incapacitated"
        Condition.AtLowHitPoints -> "at low Hit Points"
        is Condition.Descriptive -> condition.text
    }

    // ------------------------------------------------------------------ What is worn

    fun equippedArmor(character: PlayerCharacter): List<ArmorDef> =
        character.inventory
            .filter { it.equipped && it.armorDefId != null }
            .mapNotNull { EquipmentData.armorById(it.armorDefId!!) }

    /** The one piece of body armor worn, if any. A Shield is not body armor. */
    fun bodyArmor(character: PlayerCharacter): ArmorDef? =
        equippedArmor(character).firstOrNull { it.category != ArmorCategory.SHIELD }

    fun shieldBonus(character: PlayerCharacter): Int =
        equippedArmor(character).filter { it.category == ArmorCategory.SHIELD }.sumOf { it.baseAc }
}
