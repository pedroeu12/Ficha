package com.pedroeu.ficha.data.model

enum class Ability(val abbreviation: String, val fullName: String) {
    STR("STR", "Strength"),
    DEX("DEX", "Dexterity"),
    CON("CON", "Constitution"),
    INT("INT", "Intelligence"),
    WIS("WIS", "Wisdom"),
    CHA("CHA", "Charisma");

    companion object {
        val ALL: List<Ability> = entries
    }
}
