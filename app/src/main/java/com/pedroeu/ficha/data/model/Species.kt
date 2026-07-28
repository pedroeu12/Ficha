package com.pedroeu.ficha.data.model

data class Trait(
    val name: String,
    val description: String,
)

/** A named sub-choice within a species, e.g. Elf's Elven Lineage or Dragonborn's Draconic Ancestry. */
data class LineageOption(
    val id: String,
    val name: String,
    val description: String,
)

data class Species(
    val id: String,
    val name: String,
    val size: String,
    val speed: Int,
    val darkvisionRange: Int,
    val summary: String,
    val traits: List<Trait>,
    val lineageChoiceLabel: String? = null,
    val lineageOptions: List<LineageOption> = emptyList(),
    val grantedSkills: List<Skill> = emptyList(),
    val bonusSkillChoiceCount: Int = 0,
)
