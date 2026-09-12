package com.pedroeu.ficha.rules

import com.pedroeu.ficha.data.model.Sourcebook

/** Where a rule came from, which decides whose level gates it and what caption it carries. */
sealed interface Source {
    val id: String

    data class Class(override val id: String) : Source
    data class Subclass(override val id: String, val classId: String) : Source
    data class Species(override val id: String) : Source
    data class Lineage(override val id: String, val speciesId: String) : Source
    data class Background(override val id: String) : Source
    data class Feat(override val id: String) : Source

    /** An option picked from a choice: an invocation, a Fighting Style, a mastery property. */
    data class Option(override val id: String, val choiceId: String, val owner: Source) : Source

    data class Item(override val id: String) : Source
    data class Spell(override val id: String) : Source

    /** A trait or action belonging to a summoned creature rather than the character. */
    data class Statblock(override val id: String) : Source

    /** Anything the player wrote themselves. */
    data class Custom(override val id: String) : Source

    /** The class whose levels gate this, where there is one. */
    val owningClassId: String?
        get() = when (this) {
            is Class -> id
            is Subclass -> classId
            is Option -> owner.owningClassId
            else -> null
        }
}

/**
 * When a rule applies.
 *
 * [levelScope] is the multiclass distinction written down rather than re-derived. It was an
 * `if (isClassGrant)` in one file, a `filter { it.level <= entry.level }` in another, and a
 * `prerequisiteClassId` string in a third — three spellings of one idea, and the places that
 * spelled it wrong are where a Warlock 5 / Fighter 6 was offered level 11 invocations.
 */
data class Gate(
    val level: Int = 1,
    val levelScope: LevelScope = LevelScope.OWNING_CLASS,
    /** Ids of elements that must already be held. Eldritch Smite needs Pact of the Blade. */
    val requires: List<String> = emptyList(),
    /** Answers that must have been given: choice id to option id. */
    val whenChosen: List<Pair<String, String>> = emptyList(),
    /** Wording the app cannot check, printed under the element rather than enforced. */
    val statedPrerequisite: String = "",
) {
    companion object {
        val ALWAYS = Gate(level = 0, levelScope = LevelScope.CHARACTER)
    }
}

/**
 * One piece of content, carrying every rule it implies.
 *
 * The point of the type is that there is only one of it. A feature's spells, its limited uses,
 * the questions it asks, the numbers it changes and the creatures it summons all hang off the
 * same object, so adding content is describing it once rather than remembering nine tables —
 * which is what the app used to require, and what nobody managed.
 */
data class RuleElement(
    val id: String,
    val name: String,
    val description: String = "",
    val source: Source,
    val gate: Gate = Gate(),
    val effects: List<Effect> = emptyList(),
    val book: Sourcebook? = null,
) {
    inline fun <reified T : Effect> effectsOf(): List<T> = effects.filterIsInstance<T>()
}
