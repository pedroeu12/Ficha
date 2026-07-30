package com.pedroeu.ficha.data.model

data class SpellDef(
    val id: String,
    val name: String,
    val level: Int,
    val school: String,
    val castingTime: String,
    val range: String,
    val components: String,
    val duration: String,
    val description: String,
    /** Class ids whose spell list includes this spell. */
    val classes: Set<String>,
    val ritual: Boolean = false,
    val concentration: Boolean = false,
    /**
     * Damage dice at the lowest level the spell is cast, e.g. "1d10". Present only for spells
     * the Attacks section can roll, which is how damage cantrips reach the combat tab.
     */
    val damage: String = "",
    val damageType: String = "",
    /** True when casting it means a spell attack roll rather than forcing a saving throw. */
    val needsAttackRoll: Boolean = false,
    /** The ability a target rolls to resist, for spells that force a save instead. */
    val saveAbility: Ability? = null,
    /** True for cantrips whose damage grows at character levels 5, 11, and 17. */
    val scalesWithLevel: Boolean = false,
) {
    val levelLabel: String
        get() = if (level == 0) "Cantrip" else "Level $level"

    val subtitle: String
        get() = buildString {
            append(if (level == 0) "Cantrip" else "Level $level")
            append(" • ")
            append(school)
            if (concentration) append(" • Concentration")
            if (ritual) append(" • Ritual")
        }
}
