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
    /**
     * The book the option came from, where it came from one.
     *
     * Null for options that are not book content — a damage type, a skill, an ability score.
     * When a choice's options span several books the picker groups them under it, which is
     * the difference between choosing a cantrip from a list of nine and from a list of 34.
     */
    val book: Sourcebook? = null,
    /**
     * Levels needed in the choice's own class before this option may be taken.
     *
     * Zero for an option anyone can take from the start. A level 1 Warlock has five legal
     * Eldritch Invocations out of twenty-eight, and offering the other twenty-three lets them
     * build a character the rules do not allow.
     */
    val minLevel: Int = 0,
    /**
     * Other options from the same choice that must already be held.
     *
     * Eldritch Smite needs Pact of the Blade; Devouring Blade needs Thirsting Blade, which
     * needs Pact of the Blade in turn. Chains like that resolve on their own, because what is
     * ticked right now counts as held.
     */
    val requiresOptions: List<String> = emptyList(),
    /**
     * The book's own wording of the prerequisite, shown under the option.
     *
     * Some of them can't be checked — "a Warlock cantrip that deals damage via an attack
     * roll" depends on cantrips the player may not have picked yet — and printing the
     * condition is better than either hiding the option or pretending there isn't one.
     */
    val prerequisite: String = "",
    /**
     * The choices taking this option forces, asked as soon as it is taken.
     *
     * The rulebook writes plenty of options that are themselves a question — Agonizing Blast
     * names one of your cantrips, Pact of the Tome names five spells, Lessons of the First
     * Ones names an Origin feat. Until this field existed an option could not carry a
     * question of its own, so each of those was answered by a hand-written branch somewhere
     * else, keyed to the class that happened to raise it. That is why the same bug kept
     * coming back under a new name: every new piece of content needed someone to remember to
     * write another branch, and the list of things to remember was nowhere.
     *
     * Now the question travels with the option. [com.pedroeu.ficha.domain.ChoiceGraph] asks
     * it wherever the option is taken — during creation, during a level up, from Edit Mode —
     * without knowing anything about what the option is.
     *
     * Only choices made *when the option is gained* belong here. A decision the rules have
     * you make each time you use the feature is a
     * [com.pedroeu.ficha.data.content.PerUseChoice] instead.
     */
    val grants: List<Choice> = emptyList(),
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
    /**
     * True when the rules let you swap this selection during a rest — a Tattooed Warrior
     * reshaping a tattoo, a Fiend Warlock changing their resistance. Those are offered
     * again whenever the character rests.
     */
    val changeableOnRest: Boolean = false,
    /**
     * True when the rules let you revisit this every time you gain a level.
     *
     * The Primordial Patron's element is the case that brought this in: *"You can change
     * your chosen element — and your patron — whenever you gain a level."* That is not a
     * decision made once at level 3, so the level-up flow asks it again at every level, with
     * the current answer already ticked.
     */
    val changeableOnLevelUp: Boolean = false,
    /**
     * The limited-use pool these options are spent from, when there is one — Metamagic is
     * paid for with Sorcery Points, maneuvers with Superiority Dice. Lets the tracker list
     * the options the character actually picked, with their rules text.
     */
    val resourceId: String? = null,
    /**
     * The class whose levels [ChoiceOption.minLevel] is measured against.
     *
     * Empty where no option has a level prerequisite. Named rather than assumed, because a
     * multiclassed Warlock 5 / Fighter 6 has five Warlock levels and eleven character levels,
     * and the invocation list cares about the first number.
     */
    val prerequisiteClassId: String = "",
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
