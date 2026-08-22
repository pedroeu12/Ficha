package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.CasterType

/**
 * The character's classes and how many levels are in each.
 *
 * Almost every feature in the game advances on the level in its own class, not the character's
 * total. A Fighter 5 / Wizard 3 has Extra Attack and level 2 Wizard spells, not a level 8
 * version of either. Proficiency Bonus and the spell slot table are the two things that do
 * work off the total, and both are handled separately.
 *
 * A single-class character stores nothing here; [of] rebuilds the list from the legacy
 * fields, so saved characters keep working and only grow the list when they multiclass.
 */
object ClassLevels {

    /** Every class the character has levels in, starting class first. */
    fun of(character: PlayerCharacter): List<ClassLevel> =
        character.classLevels.ifEmpty {
            listOf(
                ClassLevel(
                    classId = character.classId,
                    level = character.level,
                    subclassId = character.subclassId,
                    isStarting = true,
                )
            )
        }.filter { it.level > 0 }

    /** Levels in one class, or zero if the character has none. */
    fun levelIn(character: PlayerCharacter, classId: String): Int =
        of(character).find { it.classId == classId }?.level ?: 0

    /** The subclass chosen for one class, which each class picks independently. */
    fun subclassIn(character: PlayerCharacter, classId: String): String? =
        of(character).find { it.classId == classId }?.subclassId

    /** Total levels across every class. This is what sets the Proficiency Bonus. */
    fun totalLevel(character: PlayerCharacter): Int =
        of(character).sumOf { it.level }.coerceAtLeast(1)

    /** True once the character has levels in more than one class. */
    fun isMulticlassed(character: PlayerCharacter): Boolean = of(character).size > 1

    /** The class taken at level 1, which is the one that granted the full proficiencies. */
    fun startingClass(character: PlayerCharacter): ClassLevel? =
        of(character).firstOrNull { it.isStarting } ?: of(character).firstOrNull()

    /** A short label for the sheet, e.g. "Fighter 5 / Wizard 3". */
    fun label(character: PlayerCharacter): String = of(character).joinToString(" / ") { entry ->
        "${ClassData.byId(entry.classId)?.name ?: entry.classId} ${entry.level}"
    }

    // ------------------------------------------------------------------ Spell slots

    /**
     * The level used to look up shared spell slots.
     *
     * Each class contributes a fraction of its levels: full casters all of them, half casters
     * half rounded down, third casters a third rounded down, and the Artificer half **rounded
     * up** — the one class whose fraction rounds the other way. Warlocks are left out
     * entirely, because Pact Magic is a separate pool that never merges with the rest.
     */
    fun casterLevel(character: PlayerCharacter): Int = of(character).sumOf { entry ->
        val caster = ProgressionData.forClass(entry.classId)?.casterType ?: CasterType.NONE
        when (caster) {
            CasterType.FULL -> entry.level
            CasterType.HALF -> entry.level / 2
            // The Artificer rounds up, so a single level already contributes one.
            CasterType.ARTIFICER -> (entry.level + 1) / 2
            // Pact Magic stands apart and is never folded into the shared table.
            CasterType.PACT -> 0
            // A third of their levels, rounded down. Reached through the subclass, because a
            // Fighter's own table has no caster type to read.
            CasterType.THIRD -> entry.level / 3
            CasterType.NONE -> thirdCasterLevel(entry)
        }
    }

    /** The same, for a class whose own table says NONE but whose subclass casts. */
    private fun thirdCasterLevel(entry: ClassLevel): Int {
        val fromSubclass = entry.subclassId?.let { SubclassData.byId(it)?.casterType }
        return if (fromSubclass == CasterType.THIRD) entry.level / 3 else 0
    }

    /** Warlock levels, which drive Pact Magic on their own. */
    fun pactLevel(character: PlayerCharacter): Int =
        of(character).filter { entry ->
            ProgressionData.forClass(entry.classId)?.casterType == CasterType.PACT
        }.sumOf { it.level }

    /** True when any class or subclass the character has grants spellcasting of some kind. */
    fun hasSpellcasting(character: PlayerCharacter): Boolean =
        casterLevel(character) > 0 || pactLevel(character) > 0

    // ------------------------------------------------------------------ Hit dice

    /** Hit dice by die size, since each class contributes its own. */
    fun hitDice(character: PlayerCharacter): Map<Int, Int> =
        of(character).groupBy { ClassData.byId(it.classId)?.hitDie ?: 8 }
            .mapValues { (_, entries) -> entries.sumOf { it.level } }
            .toSortedMap()

    /** A label for the sheet, e.g. "5d10 + 3d6". */
    fun hitDiceLabel(character: PlayerCharacter): String =
        hitDice(character).entries.joinToString(" + ") { (die, count) -> "${count}d$die" }
}
