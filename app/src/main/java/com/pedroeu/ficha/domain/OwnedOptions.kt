package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind

/**
 * What the character already has, so no picker ever offers it twice.
 *
 * A skill from a background and the same skill from a class list are the same proficiency —
 * taking both wastes a pick and does nothing. The same is true of tools, languages, spells,
 * feats, and repeated feature options like Metamagic. This gathers everything already owned
 * so every list can grey out what would be a duplicate, whichever source granted it first.
 */
data class Owned(
    val skills: Set<String> = emptySet(),
    val expertise: Set<String> = emptySet(),
    val tools: Set<String> = emptySet(),
    val spells: Set<String> = emptySet(),
    /**
     * Spell names, because a class's cantrip list and the spell catalog use different ids for
     * the same spell — "wiz_mage_hand" against "mage_hand". Names are what actually match.
     */
    val spellNames: Set<String> = emptySet(),
    val languages: Set<String> = emptySet(),
    val feats: Set<String> = emptySet(),
    /** Feature options already taken, so the same Metamagic can't be picked twice. */
    val options: Set<String> = emptySet(),
)

object OwnedOptions {

    /** Everything a finished character already has. */
    fun of(character: PlayerCharacter): Owned = Owned(
        skills = character.skillProficiencies,
        expertise = character.skillExpertise,
        // Tool names vary in case and spacing between sources, so compare them loosely.
        tools = character.toolProficiencies.map { it.normalized() }.toSet(),
        spells = CharacterSpells.all(character).map { it.id }.toSet(),
        spellNames = CharacterSpells.all(character).map { it.name.normalized() }.toSet(),
        languages = character.languages.map { it.normalized() }.toSet(),
        feats = CharacterFeats.heldBy(character).toSet(),
        options = ChoiceResolver.all(character).flatMap { it.selectedIds }.toSet(),
    )

    /**
     * The options a picker should show as unavailable.
     *
     * [currentSelection] is what the player has already ticked inside this very choice, which
     * must stay enabled so they can untick it.
     */
    fun disabledFor(
        choice: Choice,
        owned: Owned,
        currentSelection: Set<String> = emptySet(),
        /**
         * Levels per class, for options that ask for one. A map rather than a character
         * because creation has no character yet and still knows the class is at level 1.
         * Empty means no level prerequisite is checked.
         */
        classLevels: Map<String, Int> = emptyMap(),
    ): Set<String> {
        val disabled = when (choice.kind) {
            ChoiceKind.SKILL -> choice.options.map { it.id }.filter { it in owned.skills }

            // Expertise needs a proficiency to build on, and doubling it does nothing.
            ChoiceKind.EXPERTISE -> choice.options.map { it.id }
                .filter { it !in owned.skills || it in owned.expertise }

            ChoiceKind.TOOL -> choice.options
                .filter { it.name.normalized() in owned.tools || it.id.normalized() in owned.tools }
                .map { it.id }

            ChoiceKind.SPELL -> choice.options
                .filter { it.id in owned.spells || it.name.normalized() in owned.spellNames }
                .map { it.id }

            ChoiceKind.LANGUAGE -> choice.options
                .filter { it.name.normalized() in owned.languages }
                .map { it.id }

            ChoiceKind.FEAT -> choice.options.map { it.id }.filter { it in owned.feats }

            // Repeatable feature grants — Metamagic, maneuvers, Eldritch Invocations — come
            // back at several levels, and the rules never let you take the same one twice.
            ChoiceKind.OPTION -> choice.options.map { it.id }.filter { it in owned.options }

            // Ability scores, damage types, and subclasses are either repeatable by design or
            // guarded elsewhere, so nothing is greyed out.
            ChoiceKind.ABILITY_SCORE, ChoiceKind.DAMAGE_TYPE, ChoiceKind.SUBCLASS -> emptyList()
        }

        // Never disable something the player just picked here; they need to be able to undo it.
        return (disabled.toSet() +
            unmetPrerequisites(choice, owned, currentSelection, classLevels)) - currentSelection
    }

    /**
     * Options the character does not yet qualify for.
     *
     * Two kinds, both from the Eldritch Invocations list and both real: a level in the
     * choice's own class, and another option from the same list. Chains resolve on their own
     * — Devouring Blade needs Thirsting Blade, which needs Pact of the Blade — because an
     * option ticked right now counts as held, so a player can build the chain in one sitting.
     *
     * Prerequisites the app cannot check are deliberately not enforced: "a Warlock cantrip
     * that deals damage" depends on cantrips that may not be chosen yet, and greying out
     * Agonizing Blast for a Warlock who is about to take Eldritch Blast would be worse than
     * printing the condition and trusting the table.
     */
    private fun unmetPrerequisites(
        choice: Choice,
        owned: Owned,
        currentSelection: Set<String>,
        classLevels: Map<String, Int>,
    ): Set<String> {
        val held = owned.options + currentSelection
        val level = choice.prerequisiteClassId
            .takeIf { it.isNotBlank() && classLevels.isNotEmpty() }
            ?.let { classLevels[it] ?: 0 }

        return choice.options.filter { option ->
            val tooEarly = level != null && option.minLevel > level
            val missingSupport = option.requiresOptions.any { it !in held }
            tooEarly || missingSupport
        }.map { it.id }.toSet()
    }

    /** Tool and language names are written inconsistently across sources, so loosen the match. */
    private fun String.normalized(): String =
        trim().lowercase().replace('’', '\'').removeSuffix("s")
}
