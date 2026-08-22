package com.pedroeu.ficha.data.model

/** How hard a magic item is to come by, which also sets what the Artificer can replicate. */
enum class ItemRarity(val label: String) {
    COMMON("Common"),
    UNCOMMON("Uncommon"),
    RARE("Rare"),
    VERY_RARE("Very Rare"),
    LEGENDARY("Legendary"),
    ARTIFACT("Artifact"),
}

/**
 * One magic item from the rulebook.
 *
 * These serve two purposes: a DM can hand one to a player, who adds it straight to their
 * inventory, and the Artificer draws their Replicate Magic Item plans from the same list, so
 * the two never drift apart.
 */
data class MagicItem(
    val id: String,
    val name: String,
    val rarity: ItemRarity,
    /** The item's category line, e.g. "Wondrous Item", "Weapon (any sword)", "Armor (Shield)". */
    val kind: String,
    val description: String,
    val requiresAttunement: Boolean = false,
    /** Qualifier on the attunement, e.g. "by a Spellcaster". Empty when anyone can attune. */
    val attunementNote: String = "",
    val weightLb: Double = 0.0,
    /**
     * The Artificer level at which this becomes available as a Replicate Magic Item plan, or
     * null for items the feature can't reproduce.
     */
    val artificerPlanLevel: Int? = null,
    /**
     * The bonus this item gives to attack and damage rolls made with it, for the +N weapons
     * and the ones that quietly include a +1. Written out rather than read back out of the
     * description, so the attack line and the rules text can't drift apart.
     */
    val attackBonus: Int = 0,
    /** The bonus this item gives to Armor Class while worn, wielded, or attuned. */
    val acBonus: Int = 0,
    /** The book this comes from; the character's chosen books decide whether it is offered. */
    override val book: Sourcebook = Sourcebook.DMG,
) : FromSourcebook {
    /** The line printed under the name, e.g. "Wondrous Item, Rare (Requires Attunement)". */
    val subtitle: String
        get() = buildString {
            append(kind)
            append(", ")
            append(rarity.label)
            if (requiresAttunement) {
                append(" (Requires Attunement")
                if (attunementNote.isNotBlank()) append(" $attunementNote")
                append(")")
            }
        }
}
