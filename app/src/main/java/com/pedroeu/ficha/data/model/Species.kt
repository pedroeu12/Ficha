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
    /**
     * The skills [bonusSkillChoiceCount] may be spent on. Empty means any skill, as the
     * Human's Skillful trait allows; the Elf's Keen Senses names three.
     */
    val bonusSkillOptions: List<Skill> = emptyList(),
    /**
     * True when the species hands out an Origin feat of the player's choice, as the Human's
     * Versatile trait does. The creation flow offers the same list a background draws from.
     */
    val grantsOriginFeat: Boolean = false,
    /** The book this comes from; the character's chosen books decide whether it is offered. */
    override val book: Sourcebook = Sourcebook.PHB,
) : FromSourcebook
