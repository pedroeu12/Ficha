package com.pedroeu.ficha.data.content

/**
 * What a feat asks of you before you can take it.
 *
 * Every one of these was already stated in the feat's own description and enforced nowhere,
 * which is how both Path of Villainy Initiate feats — level 4 feats, plainly labelled as such
 * — ended up sitting in the Origin list where a level 1 character could take one. The list a
 * feat lives in carries the level rule for the common cases (Origin feats at creation,
 * general feats in place of an Ability Score Improvement from level 4, Epic Boons at 19), and
 * this table carries everything beyond that.
 */
object FeatPrerequisiteData {

    /** A class feature a feat can demand, checked against what the character's classes give. */
    enum class RequiredFeature(val label: String) {
        WEAPON_MASTERY("the Weapon Mastery feature"),
        SPELLCASTING("the Spellcasting or Pact Magic feature"),
        MARTIAL_WEAPONS("proficiency with Martial weapons"),
    }

    /**
     * [anyOf] is satisfied by holding any one of the listed feats. [countFrom] is the
     * Ascension shape: "at least N other feats from this path".
     */
    data class Requirement(
        val minLevel: Int = 0,
        val anyOf: List<String> = emptyList(),
        val countFrom: Pair<Int, List<String>>? = null,
        val feature: RequiredFeature? = null,
    )

    private val DEATH_KNIGHT_PATH = listOf(
        "death_knight_initiate", "dread_authority", "harbinger_of_doom",
        "deathly_presence", "unholy_steed",
    )

    private val LICH_PATH = listOf(
        "lich_initiate", "arcane_restoration", "transfer_life", "undead_grasp",
    )

    /** Every dragonmark, which is what Potent Dragonmark builds on. */
    private val DRAGONMARKS = listOf(
        "aberrant_dragonmark", "mark_of_detection", "mark_of_finding", "mark_of_handling",
        "mark_of_healing", "mark_of_hospitality", "mark_of_making", "mark_of_passage",
        "mark_of_scribing", "mark_of_sentinel", "mark_of_shadow", "mark_of_storm",
        "mark_of_warding",
    )

    private val BY_FEAT: Map<String, Requirement> = mapOf(
        // ---------------------------------------------- Path of the Death Knight
        "death_knight_initiate" to Requirement(
            minLevel = 4,
            feature = RequiredFeature.WEAPON_MASTERY,
        ),
        "dread_authority" to Requirement(anyOf = listOf("death_knight_initiate")),
        "harbinger_of_doom" to Requirement(anyOf = listOf("death_knight_initiate")),
        "deathly_presence" to Requirement(
            minLevel = 8,
            anyOf = listOf("death_knight_initiate"),
        ),
        "unholy_steed" to Requirement(
            minLevel = 8,
            anyOf = listOf("death_knight_initiate"),
        ),
        "death_knight_ascension" to Requirement(
            minLevel = 12,
            countFrom = 2 to DEATH_KNIGHT_PATH,
        ),

        // ---------------------------------------------- Path of the Lich
        "lich_initiate" to Requirement(
            minLevel = 4,
            feature = RequiredFeature.SPELLCASTING,
        ),
        "arcane_restoration" to Requirement(anyOf = listOf("lich_initiate")),
        "transfer_life" to Requirement(anyOf = listOf("lich_initiate")),
        "undead_grasp" to Requirement(anyOf = listOf("lich_initiate")),
        "lich_ascension" to Requirement(
            minLevel = 12,
            countFrom = 2 to LICH_PATH,
        ),

        // ---------------------------------------------- Eberron dragonmarks
        "greater_aberrant_mark" to Requirement(anyOf = listOf("aberrant_dragonmark")),
        "greater_mark_of_detection" to Requirement(anyOf = listOf("mark_of_detection")),
        "greater_mark_of_finding" to Requirement(anyOf = listOf("mark_of_finding")),
        "greater_mark_of_handling" to Requirement(anyOf = listOf("mark_of_handling")),
        "greater_mark_of_healing" to Requirement(anyOf = listOf("mark_of_healing")),
        "greater_mark_of_hospitality" to Requirement(anyOf = listOf("mark_of_hospitality")),
        "greater_mark_of_making" to Requirement(anyOf = listOf("mark_of_making")),
        "greater_mark_of_passage" to Requirement(anyOf = listOf("mark_of_passage")),
        "greater_mark_of_scribing" to Requirement(anyOf = listOf("mark_of_scribing")),
        "greater_mark_of_sentinel" to Requirement(anyOf = listOf("mark_of_sentinel")),
        "greater_mark_of_shadow" to Requirement(anyOf = listOf("mark_of_shadow")),
        "greater_mark_of_storm" to Requirement(anyOf = listOf("mark_of_storm")),
        "greater_mark_of_warding" to Requirement(anyOf = listOf("mark_of_warding")),
        "potent_dragonmark" to Requirement(anyOf = DRAGONMARKS),

        // ---------------------------------------------- Heroes of Faerûn
        "dragonscarred" to Requirement(anyOf = listOf("cult_of_the_dragon_initiate")),
        "enclave_magic" to Requirement(anyOf = listOf("emerald_enclave_fledgling")),
        "harper_teamwork" to Requirement(anyOf = listOf("harper_agent")),
        "lordly_resolve" to Requirement(anyOf = listOf("lords_alliance_agent")),
        "orders_resilience" to Requirement(anyOf = listOf("tyro_of_the_gauntlet")),
        "purple_dragon_commandant" to Requirement(
            anyOf = listOf("purple_dragon_rook"),
            feature = RequiredFeature.MARTIAL_WEAPONS,
        ),
        "zhentarim_tactics" to Requirement(anyOf = listOf("zhentarim_ruffian")),
        // Spellfire Adept accepts either the feat or the ability to cast, so the feature is
        // checked only when the feat is missing; see FeatPrerequisites.
        "spellfire_adept" to Requirement(
            anyOf = listOf("spellfire_spark"),
            feature = RequiredFeature.SPELLCASTING,
        ),
        "boon_of_the_furious_storm" to Requirement(feature = RequiredFeature.SPELLCASTING),
    )

    fun forFeat(featId: String): Requirement? = BY_FEAT[featId]

    /** Feats where holding the named feat is an alternative to the feature, not an extra. */
    val FEATURE_OR_FEAT: Set<String> = setOf("spellfire_adept")

    /**
     * Every feat id this table is keyed by, including the ones it points at. A key naming a
     * feat that doesn't exist would gate on something unreachable, so a test checks it.
     */
    fun sourceIds(): Set<String> =
        BY_FEAT.keys +
            BY_FEAT.values.flatMap { it.anyOf } +
            BY_FEAT.values.flatMap { it.countFrom?.second.orEmpty() }
}
