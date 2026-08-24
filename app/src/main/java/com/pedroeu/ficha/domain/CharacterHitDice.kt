package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.ClassData

/**
 * Hit Dice, one pool per class.
 *
 * A Fighter 5 / Wizard 3 has 5d10 and 3d6 — not eight of anything — and which of the two they
 * spend on a Short Rest is a real decision: the d10 heals more, the d6 is the one they can
 * afford to lose. The sheet used to show eight dice and roll a d10 for all of them, which
 * quietly handed a multiclassed character the best die in their build every time.
 *
 * Everything here works off [ClassLevels], so a single-class character has exactly one pool
 * and behaves as it always did.
 */
object CharacterHitDice {

    /** One class's Hit Dice: how big, how many, and how many are gone. */
    data class Pool(
        val classId: String,
        val className: String,
        val die: Int,
        val total: Int,
        val spent: Int,
    ) {
        val remaining: Int get() = (total - spent).coerceAtLeast(0)

        /** "5d10", the way a character sheet writes it. */
        val label: String get() = "${total}d$die"
    }

    /**
     * The character's pools, largest die first.
     *
     * Order is deliberate: the die you would reach for first is the one printed first, and a
     * rest that spends dice for you should spend the small ones. Ties keep class order so the
     * list doesn't reshuffle when two classes share a die.
     */
    fun pools(character: PlayerCharacter): List<Pool> {
        val classes = ClassLevels.of(character)
        val spentByClass = spentByClass(character, classes)

        return classes
            .map { entry ->
                val charClass = ClassData.byId(entry.classId)
                Pool(
                    classId = entry.classId,
                    className = charClass?.name ?: entry.classId,
                    die = charClass?.hitDie ?: 8,
                    total = entry.level,
                    spent = (spentByClass[entry.classId] ?: 0).coerceIn(0, entry.level),
                )
            }
            .sortedByDescending { it.die }
    }

    /**
     * Where the spent count comes from, per class.
     *
     * A sheet saved before this existed holds one number, and there is no record of which
     * class those dice came from — so they are taken off the pools in class order, which is
     * the same answer for the single-class character every one of those sheets is.
     */
    private fun spentByClass(
        character: PlayerCharacter,
        classes: List<ClassLevel>,
    ): Map<String, Int> {
        if (character.hitDiceSpentByClass.isNotEmpty()) return character.hitDiceSpentByClass

        var left = character.hitDiceSpent
        if (left <= 0) return emptyMap()
        return buildMap {
            classes.forEach { entry ->
                val take = minOf(left, entry.level)
                if (take > 0) put(entry.classId, take)
                left -= take
            }
        }
    }

    fun totalDice(character: PlayerCharacter): Int = pools(character).sumOf { it.total }

    fun remaining(character: PlayerCharacter): Int = pools(character).sumOf { it.remaining }

    fun spent(character: PlayerCharacter): Int = pools(character).sumOf { it.spent }

    /** Sets how many of one class's dice are spent, clamped to that class's levels. */
    fun withSpent(
        character: PlayerCharacter,
        classId: String,
        spent: Int,
    ): PlayerCharacter {
        val pool = pools(character).firstOrNull { it.classId == classId } ?: return character
        val current = pools(character).associate { it.classId to it.spent }
        return character.copy(
            hitDiceSpentByClass = current + (classId to spent.coerceIn(0, pool.total)),
            // Keep the flat count in step so anything still reading it agrees.
            hitDiceSpent = (current + (classId to spent.coerceIn(0, pool.total))).values.sum(),
        )
    }

    /** Spends one die from [classId], for a Short Rest. */
    fun spendOne(character: PlayerCharacter, classId: String): PlayerCharacter {
        val pool = pools(character).firstOrNull { it.classId == classId } ?: return character
        if (pool.remaining <= 0) return character
        return withSpent(character, classId, pool.spent + 1)
    }

    /**
     * Gives [count] dice back, smallest die first.
     *
     * A Long Rest returns half your total and says nothing about which, so the character keeps
     * the dice worth keeping: recovering a d6 before a d10 leaves the bigger die available.
     */
    fun withRecovered(character: PlayerCharacter, count: Int): PlayerCharacter {
        var left = count
        var updated = character
        pools(character).sortedBy { it.die }.forEach { pool ->
            if (left <= 0) return@forEach
            val give = minOf(left, pool.spent)
            if (give > 0) {
                updated = withSpent(updated, pool.classId, pool.spent - give)
                left -= give
            }
        }
        return updated
    }

    /** Every die back, for a full reset. */
    fun withAllRecovered(character: PlayerCharacter): PlayerCharacter =
        character.copy(hitDiceSpentByClass = emptyMap(), hitDiceSpent = 0)
}
