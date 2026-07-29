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
    /** True for point pools like Focus or Sorcery Points, which read better as a number. */
    val isPointPool: Boolean = false,
    /** True when the player wrote it, so the sheet can offer to delete it. */
    val isCustom: Boolean = false,
)
