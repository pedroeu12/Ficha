package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.Skill

/**
 * Flat numeric bonuses a source hands out just by existing.
 *
 * A Warforged's Integrated Protection is +1 to Armor Class, full stop — there's no action to
 * take and no condition to meet, so the sheet should simply add it rather than describe it in
 * a trait and leave the player to do the arithmetic. Everything of that shape lives here.
 *
 * Bonuses that only apply in some circumstances — a Barbarian's Unarmored Defense, a Monk's
 * speed while unarmoured — stay in the calculation that knows the circumstance.
 */
object PassiveBonusData {

    /** The stats a source can quietly add to. */
    enum class Target { ARMOR_CLASS, MAX_HIT_POINTS, SPEED, INITIATIVE, ALL_SAVES }

    /**
     * One bonus. [perLevel] multiplies by character level, for things like Tough or Dwarven
     * Toughness that grow as the character does.
     */
    data class Bonus(
        val target: Target,
        val amount: Int,
        val label: String,
        val perLevel: Boolean = false,
        /** A skill this applies to instead, when the target is a single skill check. */
        val skill: Skill? = null,
    )

    private val BY_SPECIES: Map<String, List<Bonus>> = mapOf(
        "warforged" to listOf(
            Bonus(Target.ARMOR_CLASS, 1, "Integrated Protection"),
        ),
        "dwarf" to listOf(
            Bonus(Target.MAX_HIT_POINTS, 1, "Dwarven Toughness", perLevel = true),
        ),
    )

    private val BY_LINEAGE: Map<String, List<Bonus>> = mapOf(
        // The Wood Elf's extra 5 feet of Speed is handled where base Speed is chosen.
    )

    private val BY_FEAT: Map<String, List<Bonus>> = mapOf(
        "tough" to listOf(
            Bonus(Target.MAX_HIT_POINTS, 2, "Tough", perLevel = true),
        ),
        "resilient" to listOf(
            // Resilient grants one save proficiency, which the proficiency system handles.
        ),
        "mark_of_passage" to listOf(
            Bonus(Target.SPEED, 5, "Courier's Speed"),
        ),
        "lucky" to emptyList(),
    )

    private val BY_CLASS_FEATURE: Map<String, List<Bonus>> = mapOf(
        // Nothing unconditional yet; class AC and Speed features depend on what's worn.
    )

    fun forSpecies(speciesId: String): List<Bonus> = BY_SPECIES[speciesId].orEmpty()

    fun forLineage(lineageId: String?): List<Bonus> =
        lineageId?.let { BY_LINEAGE[it] }.orEmpty()

    fun forFeat(featId: String): List<Bonus> = BY_FEAT[featId].orEmpty()

    fun forClass(classId: String): List<Bonus> = BY_CLASS_FEATURE[classId].orEmpty()

    /** Every id these tables are keyed by, so a test can check them against the real data. */
    fun sourceIds(): Set<String> =
        BY_SPECIES.keys + BY_LINEAGE.keys + BY_FEAT.keys + BY_CLASS_FEATURE.keys
}
