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
        feats = character.featIds.toSet(),
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
        return disabled.toSet() - currentSelection
    }

    /** Tool and language names are written inconsistently across sources, so loosen the match. */
    private fun String.normalized(): String =
        trim().lowercase().replace('’', '\'').removeSuffix("s")
}
