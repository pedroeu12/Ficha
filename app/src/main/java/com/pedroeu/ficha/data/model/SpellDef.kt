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
