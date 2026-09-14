package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.PerUseChoice
import com.pedroeu.ficha.data.content.PerUseChoiceData
import com.pedroeu.ficha.data.model.ChoiceOption
import com.pedroeu.ficha.rules.RulesEngine

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

    /**
     * Every per-use choice this character has reached, in the order the table lists them.
     *
     * Which ones those are is the rules engine's answer: the element that carries the
     * question is gated on the level in the class that grants it, or the character's level
     * for a species, and this no longer re-derives that. A Druid 3 / Rogue 5 reaches Starry
     * Form and a Druid 1 / Rogue 7 does not, for the same reason every other feature does.
     */
    fun all(character: PlayerCharacter): List<ActiveChoice> =
        RulesEngine.choicesOnUse(character)
            // A pool's own on-use options are asked from the pool's tracker, not from here.
            .mapNotNull { applied -> PerUseChoiceData.byId(applied.effect.choice.id) }
            .distinctBy { it.id }
            .map { choice ->
                val selectedId = character.perUseChoices[choice.id]
                ActiveChoice(
                    choice = choice,
                    selected = choice.options.find { it.id == selectedId },
                )
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
