package com.pedroeu.ficha.data.model

data class Trait(
    val name: String,
    val description: String,
    /**
     * What the trait asks the player to decide, if anything.
     *
     * A species trait could not ask anything until now, so the ones worded "of your choice"
     * were either answered through the single bonus-skill field on the species — which fits a
     * trait granting one skill and nothing else — or not asked at all. That left a Reborn with
     * no skill to pick and no resistance to name, a Warforged short a tool proficiency, and
     * five species whose magic never asked which ability casts it.
     *
     * Class and subclass features have carried their questions this way all along; this is the
     * same field, so [com.pedroeu.ficha.domain.ChoiceGraph] walks a trait exactly as it walks
     * a feature and an option inside one can raise further questions of its own.
     */
    val choices: List<Choice> = emptyList(),
)

/** A named sub-choice within a species, e.g. Elf's Elven Lineage or Dragonborn's Draconic Ancestry. */
data class LineageOption(
    val id: String,
    val name: String,
    val description: String,
    /**
     * What picking this lineage asks next.
     *
     * The High Elf's cantrip was the only one of these the app knew about, and it knew it
     * through a branch that named the elf and the lineage by hand. A lineage carries its own
     * question now, on the same footing as a trait or a class feature.
     */
    val choices: List<Choice> = emptyList(),
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
