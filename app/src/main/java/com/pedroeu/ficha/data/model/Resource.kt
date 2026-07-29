package com.pedroeu.ficha.data.model

/** When a limited-use resource comes back. */
enum class Recharge(val label: String, val shortLabel: String) {
    /** Returns on a Short Rest, and therefore also on a Long Rest. */
    SHORT_REST("Short or Long Rest", "Short"),

    /** Returns only on a Long Rest. */
    LONG_REST("Long Rest", "Long"),

    /** Comes back some other way — a kill, a crit, dawn. Rests leave it alone. */
    SPECIAL("Special", "Special");

    /** True when finishing a rest of [other] kind refills this resource. */
    fun refilledBy(other: Recharge): Boolean = when (this) {
        SHORT_REST -> other == SHORT_REST || other == LONG_REST
        LONG_REST -> other == LONG_REST
        SPECIAL -> false
    }
}

/**
 * One named thing a resource can be spent on — Flurry of Blows for Focus Points, Careful
 * Spell for Sorcery Points, Trip Attack for Superiority Dice.
 *
 * These carry their own rules text so the sheet can answer "what does this actually do?"
 * rather than only listing the name next to a number of remaining uses.
 */
data class ResourceOption(
    val id: String,
    val name: String,
    /** What it costs, e.g. "1 Focus Point" or "1-3 Sorcery Points". */
    val cost: String = "",
    /** The action it takes, e.g. "Bonus Action" or "Magic action". */
    val actionType: String = "",
    /** Full rules text, not a one-line summary. */
    val description: String,
    /** Class level at which it becomes available. */
    val unlockLevel: Int = 1,
    /** True when the player picked this rather than being granted it automatically. */
    val isChosen: Boolean = false,
) {
    /** The caption shown next to the name, e.g. "Bonus Action • 1 Focus Point". */
    val subtitle: String
        get() = listOf(actionType, cost).filter { it.isNotBlank() }.joinToString(" • ")
}

/**
 * A pool of limited uses shown on the sheet with a tracker. Derived from the character's
 * class, subclass, species, and feats, or written by the player.
 */
data class ResourceDef(
    val id: String,
    val name: String,
    val max: Int,
    val recharge: Recharge,
    /** Where it came from, e.g. "Monk", "Path of the Berserker", "Lucky". */
    val source: String,
    val notes: String = "",
    /**
     * The feature's own rules text, so a tracker can answer "what is this for?". Filled in
     * from the feature the pool belongs to rather than written twice.
     */
    val description: String = "",
    /** True for point pools like Focus or Sorcery Points, which read better as a number. */
    val isPointPool: Boolean = false,
    /** True when the player wrote it, so the sheet can offer to delete it. */
    val isCustom: Boolean = false,
    /** Abilities this pool pays for, each with its own rules text. */
    val options: List<ResourceOption> = emptyList(),
)
