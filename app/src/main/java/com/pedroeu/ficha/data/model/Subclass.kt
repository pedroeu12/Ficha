package com.pedroeu.ficha.data.model

data class SubclassFeature(
    val level: Int,
    val name: String,
    val description: String,
    val choices: List<Choice> = emptyList(),
)

data class Subclass(
    val id: String,
    val classId: String,
    val name: String,
    val summary: String,
    val features: List<SubclassFeature>,
    /** Shown next to the name so playtest options are never mistaken for published ones. */
    val source: String = "Player's Handbook (2024)",
) {
    val isPlaytest: Boolean get() = source.contains("Unearthed Arcana")

    fun featuresAt(level: Int): List<SubclassFeature> = features.filter { it.level == level }
}
