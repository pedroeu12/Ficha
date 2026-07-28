package com.pedroeu.ficha.data.model

data class Background(
    val id: String,
    val name: String,
    val summary: String,
    /** Exactly 3 abilities the player distributes +2/+1/+1 across when picking this background. */
    val abilityOptions: List<Ability>,
    val skillProficiencies: List<Skill>,
    val toolProficiency: String,
    val featId: String,
    val equipment: List<String>,
    val startingGold: Int,
)
