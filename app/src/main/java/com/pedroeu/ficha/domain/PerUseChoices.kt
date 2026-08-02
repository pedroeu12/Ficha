package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.PerUseChoice
import com.pedroeu.ficha.data.content.PerUseChoiceData
import com.pedroeu.ficha.data.model.ChoiceOption

/** A per-use choice the character has, together with what they currently have it set to. */
data class ActiveChoice(
    val choice: PerUseChoice,
    /** The option in effect right now, or null when nothing has been chosen since the rest. */
    val selected: ChoiceOption?,
) {
    val isSet: Boolean get() = selected != null
}

/**
 * The decisions the rules have you make when you use a feature, rather than once at creation.
 *
 * The distinction matters more than it sounds. A choice made at creation is part of who the
 * character is; a choice made at the moment of use is part of what they are doing this turn,
 * and freezing one into the other quietly removes two thirds of a feature. What is stored here
 * is only the answer to "what is happening now", which is why a rest clears it.
 */
object PerUseChoices {

    /** Every per-use choice this character has reached, in the order the table lists them. */
    fun all(character: PlayerCharacter): List<ActiveChoice> {
        val classes = ClassLevels.of(character)

        return PerUseChoiceData.ALL
            .filter { choice -> qualifies(character, classes, choice) }
            .map { choice ->
                val selectedId = character.perUseChoices[choice.id]
                ActiveChoice(
                    choice = choice,
                    selected = choice.options.find { it.id == selectedId },
                )
            }
    }

    private fun qualifies(
        character: PlayerCharacter,
        classes: List<ClassLevel>,
        choice: PerUseChoice,
    ): Boolean = when {
        // A subclass feature advances on the level in *that* class, so a Druid 3 / Rogue 5
        // reaches Starry Form and a Druid 1 / Rogue 7 does not.
        choice.subclassId.isNotBlank() -> classes.any {
            it.subclassId == choice.subclassId && it.level >= choice.minLevel
        }
        choice.speciesId.isNotBlank() ->
            character.speciesId == choice.speciesId && character.level >= choice.minLevel
        else -> false
    }

    /** The per-use choices attached to a limited-use pool, for its tracker to ask about. */
    fun forResource(character: PlayerCharacter, resourceId: String): List<ActiveChoice> =
        all(character).filter { it.choice.resourceId == resourceId }

    /** Records what the character is doing with the feature right now. */
    fun choose(
        character: PlayerCharacter,
        choiceId: String,
        optionId: String,
    ): PlayerCharacter {
        val choice = PerUseChoiceData.byId(choiceId) ?: return character
        if (choice.options.none { it.id == optionId }) return character
        return character.copy(perUseChoices = character.perUseChoices + (choiceId to optionId))
    }

    /** Puts the feature back to nothing in particular. */
    fun clear(character: PlayerCharacter, choiceId: String): PlayerCharacter =
        character.copy(perUseChoices = character.perUseChoices - choiceId)

    /**
     * Everything goes back to unset when the character rests.
     *
     * Every one of these lasts a minute or an hour at most, so nothing survives a rest — and
     * leaving yesterday's answer showing as current would be worse than showing none at all.
     */
    fun clearAll(character: PlayerCharacter): PlayerCharacter =
        if (character.perUseChoices.isEmpty()) character
        else character.copy(perUseChoices = emptyMap())
}
