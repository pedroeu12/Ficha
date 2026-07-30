package com.pedroeu.ficha.data.model

enum class CasterType {
    /** No spellcasting from the base class. */
    NONE,

    /** Bard, Cleric, Druid, Sorcerer, Wizard: slots from level 1. */
    FULL,

    /** Paladin, Ranger: slots from level 2, progressing at half speed. */
    HALF,

    /**
     * Artificer: the half-caster table, except slots arrive at level 1 rather than level 2.
     * It is the only class in the game with this progression, which is why it can't reuse HALF.
     */
    ARTIFICER,

    /** Warlock: few slots, always at the highest level available, refreshed on a short rest. */
    PACT,
}

/** A feature the class table grants at a given level. */
data class ClassFeature(
    val level: Int,
    val name: String,
    val description: String,
    /** Decisions the player must make the moment this feature is gained. */
    val choices: List<Choice> = emptyList(),
)

data class ClassProgression(
    val classId: String,
    val casterType: CasterType,
    /** Level at which the subclass is chosen. Every 2024 class picks one at level 3. */
    val subclassLevel: Int,
    val subclassLabel: String,
    /** Levels granting an Ability Score Improvement (or a feat in its place). */
    val asiLevels: Set<Int>,
    /** Level granting an Epic Boon feat instead of a normal ASI. */
    val epicBoonLevel: Int = 19,
    val features: List<ClassFeature>,
    /** Character level -> total cantrips known. Sparse; the highest entry at or below applies. */
    val cantripsKnown: Map<Int, Int> = emptyMap(),
    /** Character level -> prepared (or known, for Warlock) spell count. Index 1..20. */
    val preparedSpells: List<Int> = emptyList(),
) {
    fun featuresAt(level: Int): List<ClassFeature> = features.filter { it.level == level }

    fun cantripsKnownAt(level: Int): Int =
        cantripsKnown.entries.filter { it.key <= level }.maxByOrNull { it.key }?.value ?: 0

    fun preparedSpellsAt(level: Int): Int =
        preparedSpells.getOrNull(level - 1) ?: 0

    fun grantsAsiAt(level: Int): Boolean = level in asiLevels

    fun grantsEpicBoonAt(level: Int): Boolean = level == epicBoonLevel
}

/** Spell slot rows, indexed by spell level 1..9. */
object SpellSlotTables {

    /** Bard, Cleric, Druid, Sorcerer, Wizard. Index 0 is character level 1. */
    val FULL: List<List<Int>> = listOf(
        listOf(2),
        listOf(3),
        listOf(4, 2),
        listOf(4, 3),
        listOf(4, 3, 2),
        listOf(4, 3, 3),
        listOf(4, 3, 3, 1),
        listOf(4, 3, 3, 2),
        listOf(4, 3, 3, 3, 1),
        listOf(4, 3, 3, 3, 2),
        listOf(4, 3, 3, 3, 2, 1),
        listOf(4, 3, 3, 3, 2, 1),
        listOf(4, 3, 3, 3, 2, 1, 1),
        listOf(4, 3, 3, 3, 2, 1, 1),
        listOf(4, 3, 3, 3, 2, 1, 1, 1),
        listOf(4, 3, 3, 3, 2, 1, 1, 1),
        listOf(4, 3, 3, 3, 2, 1, 1, 1, 1),
        listOf(4, 3, 3, 3, 3, 1, 1, 1, 1),
        listOf(4, 3, 3, 3, 3, 2, 1, 1, 1),
        listOf(4, 3, 3, 3, 3, 2, 2, 1, 1),
    )

    /** Paladin and Ranger. Level 1 has no slots. */
    val HALF: List<List<Int>> = listOf(
        emptyList(),
        listOf(2),
        listOf(3),
        listOf(3),
        listOf(4, 2),
        listOf(4, 2),
        listOf(4, 3),
        listOf(4, 3),
        listOf(4, 3, 2),
        listOf(4, 3, 2),
        listOf(4, 3, 3),
        listOf(4, 3, 3),
        listOf(4, 3, 3, 1),
        listOf(4, 3, 3, 1),
        listOf(4, 3, 3, 2),
        listOf(4, 3, 3, 2),
        listOf(4, 3, 3, 3, 1),
        listOf(4, 3, 3, 3, 1),
        listOf(4, 3, 3, 3, 2),
        listOf(4, 3, 3, 3, 2),
    )

    /** Artificer: identical to [HALF] from level 2 on, but level 1 already grants two slots. */
    val ARTIFICER: List<List<Int>> = listOf(listOf(2)) + HALF.drop(1)

    /** Warlock Pact Magic: slot count paired with the single level those slots are cast at. */
    val PACT: List<Pair<Int, Int>> = listOf(
        1 to 1, 2 to 1, 2 to 2, 2 to 2, 2 to 3,
        2 to 3, 2 to 4, 2 to 4, 2 to 5, 2 to 5,
        3 to 5, 3 to 5, 3 to 5, 3 to 5, 3 to 5,
        3 to 5, 4 to 5, 4 to 5, 4 to 5, 4 to 5,
    )

    /** Slots keyed by spell level for a character of [level] using [caster] progression. */
    fun slotsFor(caster: CasterType, level: Int): Map<Int, Int> {
        val index = (level - 1).coerceIn(0, 19)
        return when (caster) {
            CasterType.NONE -> emptyMap()
            CasterType.FULL -> FULL[index].mapIndexed { i, count -> (i + 1) to count }.toMap()
            CasterType.HALF -> HALF[index].mapIndexed { i, count -> (i + 1) to count }.toMap()
            CasterType.ARTIFICER ->
                ARTIFICER[index].mapIndexed { i, count -> (i + 1) to count }.toMap()
            CasterType.PACT -> {
                val (count, slotLevel) = PACT[index]
                mapOf(slotLevel to count)
            }
        }
    }

    /** Highest spell level the character can cast from slots. */
    fun maxSpellLevel(caster: CasterType, level: Int): Int =
        slotsFor(caster, level).keys.maxOrNull() ?: 0
}
