package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.MulticlassData
import com.pedroeu.ficha.data.model.Ability

/** Whether a class can be taken, and why not when it can't. */
data class MulticlassOption(
    val classId: String,
    val className: String,
    val entry: MulticlassData.Entry,
    val allowed: Boolean,
    val reason: String,
    /** True when the character already has levels in this class. */
    val alreadyHas: Boolean,
)

/**
 * The rules around taking a level in a new class.
 *
 * Prerequisites gate *joining* a class, never continuing one. A Bard who rolled a 12 in
 * Charisma is still a Bard and still levels up as one; the 13 is only ever asked for at the
 * moment a second class is added. Getting that backwards left single-class characters unable
 * to advance at all, so [MulticlassOption.allowed] is unconditionally true for a class the
 * character already has levels in.
 *
 * For a genuinely new class both halves of the requirement matter: you need the scores for
 * the class you're joining *and* for every class you already have, since leaving a class has
 * the same bar as entering one. A Fighter with Strength 13 and Intelligence 10 can't become a
 * Wizard, and a Wizard with Intelligence 13 and Strength 10 can't become a Fighter.
 */
object Multiclassing {

    /** Every class the character could add a level in, with the ones they can't and why. */
    fun options(character: PlayerCharacter): List<MulticlassOption> {
        val scores = CharacterCalculations.finalAbilityScores(character)
        val existing = ClassLevels.of(character).map { it.classId }.toSet()
        val leaveBlockers = unmetForExistingClasses(character, scores)

        return ClassData.ALL.mapNotNull { charClass ->
            val entry = MulticlassData.forClass(charClass.id) ?: return@mapNotNull null
            val alreadyHas = charClass.id in existing
            val missing = unmet(entry, scores)

            val allowed = alreadyHas || (missing.isEmpty() && leaveBlockers.isEmpty())
            val reason = when {
                alreadyHas -> entry.prerequisiteLabel

                missing.isNotEmpty() ->
                    "Needs ${missing.joinToString(" and ") { "${it.fullName} ${MulticlassData.REQUIRED_SCORE}" }}"

                leaveBlockers.isNotEmpty() ->
                    "Your current class needs " +
                        leaveBlockers.joinToString(" and ") {
                            "${it.fullName} ${MulticlassData.REQUIRED_SCORE}"
                        }

                else -> entry.prerequisiteLabel
            }

            MulticlassOption(
                classId = charClass.id,
                className = charClass.name,
                entry = entry,
                allowed = allowed,
                reason = reason,
                alreadyHas = alreadyHas,
            )
        }
    }

    /** True when the character meets every requirement to add a level in [classId]. */
    fun canTake(character: PlayerCharacter, classId: String): Boolean =
        options(character).find { it.classId == classId }?.allowed == true

    /** Prerequisites of [entry] the character's scores don't meet. */
    private fun unmet(entry: MulticlassData.Entry, scores: Map<Ability, Int>): List<Ability> {
        val missingAll = entry.prerequisites
            .filter { (scores[it] ?: 0) < MulticlassData.REQUIRED_SCORE }

        // The Fighter accepts either score, so it only blocks when neither is high enough.
        val missingEither = if (
            entry.orPrerequisites.isNotEmpty() &&
            entry.orPrerequisites.none { (scores[it] ?: 0) >= MulticlassData.REQUIRED_SCORE }
        ) {
            entry.orPrerequisites
        } else {
            emptyList()
        }

        return missingAll + missingEither
    }

    /** Requirements the character fails for classes they already have, which block leaving. */
    private fun unmetForExistingClasses(
        character: PlayerCharacter,
        scores: Map<Ability, Int>,
    ): List<Ability> = ClassLevels.of(character)
        .mapNotNull { MulticlassData.forClass(it.classId) }
        .flatMap { unmet(it, scores) }
        .distinct()

    /**
     * Adds a level in [classId], creating the entry when it's a new class.
     *
     * The legacy single-class fields are kept in step so anything still reading them — and
     * anything already saved — carries on working.
     */
    fun withLevelIn(character: PlayerCharacter, classId: String): PlayerCharacter {
        val current = ClassLevels.of(character)
        val updated = if (current.any { it.classId == classId }) {
            current.map { if (it.classId == classId) it.copy(level = it.level + 1) else it }
        } else {
            current + ClassLevel(classId = classId, level = 1, isStarting = false)
        }

        return character.copy(
            classLevels = updated,
            level = updated.sumOf { it.level },
        )
    }

    /** Records the subclass chosen for one class, which each class picks on its own. */
    fun withSubclass(
        character: PlayerCharacter,
        classId: String,
        subclassId: String,
    ): PlayerCharacter {
        val updated = ClassLevels.of(character)
            .map { if (it.classId == classId) it.copy(subclassId = subclassId) else it }

        return character.copy(
            classLevels = updated,
            // Keep the legacy field pointing at the starting class's subclass.
            subclassId = if (classId == character.classId) subclassId else character.subclassId,
        )
    }

    /**
     * The proficiencies a multiclass level hands over — narrower than a starting character's,
     * and never including saving throws, which only the first class grants.
     */
    fun proficienciesGained(classId: String): MulticlassData.Entry? =
        MulticlassData.forClass(classId)
}
