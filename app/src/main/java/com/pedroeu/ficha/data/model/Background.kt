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
    /** The book this comes from; the character's chosen books decide whether it is offered. */
    override val book: Sourcebook = Sourcebook.PHB,
) : FromSourcebook
