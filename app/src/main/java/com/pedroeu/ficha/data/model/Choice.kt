package com.pedroeu.ficha.data.model

/**
 * How a choice should be rendered and what its selections mean once applied.
 * The app's rule is that nothing is ever auto-picked on the player's behalf, so every grant
 * worded "of your choice" in the rulebook becomes one of these.
 */
enum class ChoiceKind {
    /** Generic named options, e.g. a Fighting Style or a subclass feature branch. */
    OPTION,
    /** Options are Skill.name values; selections become skill proficiencies. */
    SKILL,
    /** Options are Skill.name values; selections become expertise. */
    EXPERTISE,
    /** Options are tool names; selections become tool proficiencies. */
    TOOL,
    /** Options are spell ids from [com.pedroeu.ficha.data.content.SpellData]. */
    SPELL,
    /** Options are Ability.name values; selections raise those scores. */
    ABILITY_SCORE,
    /** Options are feat ids. */
    FEAT,
    /** Options are subclass ids. */
    SUBCLASS,
    /** Options are damage type names. */
    DAMAGE_TYPE,
    /** Options are language names. */
    LANGUAGE,
}

data class ChoiceOption(
    val id: String,
    val name: String,
    val description: String = "",
    /** Short right-aligned label, e.g. an ability abbreviation or spell school. */
    val supporting: String = "",
)

/**
 * One decision presented to the player. [count] selections must be made before the step
 * the choice belongs to can be completed.
 */
data class Choice(
    val id: String,
    val label: String,
    val prompt: String = "",
    val count: Int = 1,
    val kind: ChoiceKind = ChoiceKind.OPTION,
    val options: List<ChoiceOption> = emptyList(),
    /** Where this choice came from, shown as a caption, e.g. "Sage" or "Level 4". */
    val source: String = "",
)

/** Convenience builders for the common option shapes. */
object ChoiceOptions {

    fun fromSkills(skills: List<Skill>, exclude: Set<Skill> = emptySet()): List<ChoiceOption> =
        skills.filterNot { it in exclude }.map { skill ->
            ChoiceOption(
                id = skill.name,
                name = skill.displayName,
                supporting = skill.ability.abbreviation,
            )
        }

    fun fromAbilities(abilities: List<Ability> = Ability.ALL): List<ChoiceOption> =
        abilities.map { ability ->
            ChoiceOption(
                id = ability.name,
                name = ability.fullName,
                supporting = ability.abbreviation,
            )
        }

    fun fromStrings(values: List<String>): List<ChoiceOption> =
        values.map { ChoiceOption(id = it, name = it) }
}
