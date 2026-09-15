package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceOption
import java.util.UUID

/**
 * The player's own answers, folded into the app's questions.
 *
 * Everything the sheet asks — which invocation, which feat, which Fighting Style, which
 * language — is a [Choice] with a list of options read out of the books. A table that plays
 * anything the books do not have had nowhere to put it: the closest option had to stand in,
 * and the difference lived in somebody's memory instead of on the sheet.
 *
 * So every question can also be answered with something written by hand, and there is exactly
 * one place that happens. Because the fold-in is done inside
 * [ChoiceOptionSources.resolve] — the single point every picker in the app already passes
 * through, on the sheet, in the level-up flow, in Edit Mode, on the phone and on the tablet —
 * a written option is not a feature of any screen. It is a property of the question, and no
 * screen had to be told.
 */
object CustomOptions {

    /** Marks an id as the player's own rather than the book's. */
    const val PREFIX = "custom:"

    /** A fresh id for something new, distinct from everything the books ship. */
    fun newId(): String = "$PREFIX${UUID.randomUUID()}"

    fun isCustom(optionId: String): Boolean = optionId.startsWith(PREFIX)

    /** What the player wrote for one question. */
    fun writtenFor(character: PlayerCharacter, choiceId: String): List<CustomOption> =
        character.customOptions.filter { it.choiceId == choiceId }

    /**
     * The question with the player's own work in it: rewrites applied, additions appended.
     *
     * A rewrite keeps the option's place in the list and everything the rules attached to it
     * — its level requirement, the options it depends on, the sub-questions it raises — and
     * changes only what it says. That is the honest reading of "write over Agonizing Blast":
     * the character still has the invocation, it just does something else now.
     */
    fun into(choice: Choice, written: List<CustomOption>): Choice {
        val mine = written.filter { it.choiceId == choice.id }
        if (mine.isEmpty()) return choice

        val byId = mine.associateBy { it.id }
        val rewritten = choice.options.map { option ->
            val rewrite = byId[option.id] ?: return@map option
            option.copy(
                name = rewrite.name.ifBlank { option.name },
                description = rewrite.description.ifBlank { option.description },
            )
        }
        val added = mine
            .filterNot { it.id in choice.options.map(ChoiceOption::id).toSet() }
            .map { ChoiceOption(id = it.id, name = it.name, description = it.description) }

        return choice.copy(options = rewritten + added)
    }

    /** The same, for a character. */
    fun into(choice: Choice, character: PlayerCharacter): Choice =
        into(choice, character.customOptions)

    /**
     * Writes one down, replacing any earlier version of the same one.
     *
     * A blank name is a deletion: an option with nothing written on it is not an option, and
     * making the empty form mean "take it back" saves a second gesture for undoing a mistake.
     */
    fun write(
        written: List<CustomOption>,
        option: CustomOption,
    ): List<CustomOption> {
        val without = written.filterNot { it.id == option.id && it.choiceId == option.choiceId }
        return if (option.name.isBlank()) without else without + option
    }

    fun write(character: PlayerCharacter, option: CustomOption): PlayerCharacter =
        character.copy(customOptions = write(character.customOptions, option))

    /**
     * Takes one back, and unpicks it if it was chosen.
     *
     * Deleting an option the character had selected would otherwise leave an answer pointing
     * at nothing: the sheet would show a blank line, and `SheetAudit` would rightly complain
     * that a stored answer is not among the options.
     */
    fun erase(character: PlayerCharacter, option: CustomOption): PlayerCharacter {
        val without = character.copy(
            customOptions = character.customOptions.filterNot {
                it.id == option.id && it.choiceId == option.choiceId
            },
        )
        // A rewrite is only text: taking it back restores the book's wording and the
        // character keeps the option, so nothing is unpicked.
        if (option.isRewrite) return without

        val chosen = ChoiceResolver.latestSelectionFor(without, option.choiceId)
        if (option.id !in chosen) return without
        return ChoiceResolver.withAnswer(
            character = without,
            choiceId = option.choiceId,
            level = levelOfAnswer(without, option.choiceId),
            optionIds = chosen - option.id,
        )
    }

    /** The level an answer is filed under, so rewriting it does not move it to another level. */
    private fun levelOfAnswer(character: PlayerCharacter, choiceId: String): Int =
        character.levelSelections.keys
            .filter { it.substringAfter(':') == choiceId }
            .mapNotNull { it.substringBefore(':').toIntOrNull() }
            .maxOrNull()
            ?: 0
}
