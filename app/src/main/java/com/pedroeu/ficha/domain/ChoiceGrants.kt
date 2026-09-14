package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.OptionSource
import com.pedroeu.ficha.data.model.Skill

/**
 * What an answer puts on the sheet, applied in one place and taken back in the same place.
 *
 * A skill picked through Skilled, a language from a feat, the three cantrips Pact of the Tome
 * names — each of these is an answer to a question *and* a thing the character now has. The
 * app used to turn the one into the other in three separate places: character creation, the
 * level-up flow, and Edit Mode's "add a feat", each with its own `when (choice.kind)` and each
 * missing a different case. Edit Mode never granted Expertise, nobody ever granted a language,
 * and a spell chosen inside a class feature reached nowhere at all. Removing a feat took the
 * feat and left everything it had granted behind, because no place knew what that was.
 *
 * Now there is one function for applying an answer and one for revoking it, and every route
 * an answer can arrive by goes through them. Changing an answer in Edit Mode revokes the old
 * one before applying the new, so the sheet never holds the skill you picked last week beside
 * the one you picked today.
 */
object ChoiceGrants {

    /** Everything [answers] to [choices] puts on the sheet. Safe to apply more than once. */
    fun apply(
        character: PlayerCharacter,
        choices: List<Choice>,
        answers: Map<String, List<String>>,
    ): PlayerCharacter = choices.fold(character) { pc, choice ->
        applyOne(pc, choice, answers[choice.id].orEmpty())
    }

    /** The reverse of [apply]: what those answers granted comes off. */
    fun revoke(
        character: PlayerCharacter,
        choices: List<Choice>,
        answers: Map<String, List<String>>,
    ): PlayerCharacter = choices.fold(character) { pc, choice ->
        revokeOne(pc, choice, answers[choice.id].orEmpty())
    }

    /**
     * Records a new answer to [choice], revoking what the old answer granted first.
     *
     * The one way an answer changes after the character exists — Edit Mode, and the rests
     * that let a pick be swapped — so that a changed answer is a changed sheet.
     */
    fun answer(
        character: PlayerCharacter,
        choice: Choice,
        level: Int,
        optionIds: List<String>,
    ): PlayerCharacter {
        val previous = ChoiceResolver.latestSelectionFor(character, choice.id)
        val cleared = revokeOne(character, choice, previous)
        val recorded = ChoiceResolver.withAnswer(cleared, choice.id, level, optionIds)
        return applyOne(recorded, choice, optionIds)
    }

    /**
     * Ticks or unticks one option, reading the current answer from the character rather than
     * from whatever a screen remembered.
     *
     * That last part is the fix for a whole class of Edit Mode bugs: a dialog that kept its
     * own copy of the answer computed the next one from a stale copy, so the second tap undid
     * the first. [unbounded] lifts the count for the one editor that deliberately lets a table
     * hand out more than the class table allows.
     */
    fun toggle(
        character: PlayerCharacter,
        choice: Choice,
        level: Int,
        optionId: String,
        unbounded: Boolean = false,
    ): PlayerCharacter {
        val current = ChoiceResolver.latestSelectionFor(character, choice.id)
        val limit = if (unbounded) Int.MAX_VALUE else choice.count
        return answer(character, choice, level, nextSelection(current, optionId, limit))
    }

    /**
     * The selection after one tap: off if it was on, on if there is room, and otherwise the
     * oldest pick gives way — a single-pick choice just switches.
     */
    fun nextSelection(current: List<String>, optionId: String, count: Int): List<String> = when {
        optionId in current -> current - optionId
        current.size < count -> current + optionId
        count == 1 -> listOf(optionId)
        count <= 0 -> current
        else -> current.drop(1) + optionId
    }

    // ------------------------------------------------------------------ One answer

    private fun applyOne(pc: PlayerCharacter, choice: Choice, picked: List<String>): PlayerCharacter {
        if (picked.isEmpty()) return pc
        return when (choice.kind) {
            ChoiceKind.SKILL -> pc.copy(
                skillProficiencies = pc.skillProficiencies + picked.filter(::isSkill),
            )

            // Expertise on a skill you are not trained in is meaningless, so the training
            // comes with it — which is also what the level-up flow always did.
            ChoiceKind.EXPERTISE -> pc.copy(
                skillExpertise = pc.skillExpertise + picked.filter(::isSkill),
                skillProficiencies = pc.skillProficiencies + picked.filter(::isSkill),
            )

            ChoiceKind.TOOL -> pc.copy(
                toolProficiencies = (pc.toolProficiencies + picked).distinct(),
            )

            ChoiceKind.LANGUAGE -> pc.copy(languages = (pc.languages + picked).distinct())

            // A choice drawn from spells the character already has — "one of your known
            // cantrips" — names a spell to change, not a spell to learn.
            ChoiceKind.SPELL -> if (choice.optionsFrom != OptionSource.Declared) pc else {
                val already = pc.knownSpells.map { it.id }.toSet()
                val learned = picked
                    .filterNot { it in already }
                    .mapNotNull { id ->
                        SpellData.byId(id)?.let { spell ->
                            KnownSpell(
                                id = spell.id,
                                name = spell.name,
                                level = spell.level,
                                school = spell.school,
                                description = spell.description,
                                source = choice.source,
                            )
                        }
                    }
                if (learned.isEmpty()) pc else pc.copy(knownSpells = pc.knownSpells + learned)
            }

            // Feats, ability scores, feature options, damage types and subclasses are read
            // from the answer itself wherever they are needed; nothing is written down twice.
            ChoiceKind.OPTION, ChoiceKind.ABILITY_SCORE, ChoiceKind.FEAT,
            ChoiceKind.SUBCLASS, ChoiceKind.DAMAGE_TYPE -> pc
        }
    }

    private fun revokeOne(pc: PlayerCharacter, choice: Choice, picked: List<String>): PlayerCharacter {
        if (picked.isEmpty()) return pc
        return when (choice.kind) {
            // Losing the training takes the expertise built on it away too.
            ChoiceKind.SKILL -> pc.copy(
                skillProficiencies = pc.skillProficiencies - picked.toSet(),
                skillExpertise = pc.skillExpertise - picked.toSet(),
            )

            // Only the doubling goes: the training underneath may have come from anywhere.
            ChoiceKind.EXPERTISE -> pc.copy(skillExpertise = pc.skillExpertise - picked.toSet())

            ChoiceKind.TOOL -> pc.copy(
                toolProficiencies = pc.toolProficiencies.filterNot { held ->
                    picked.any { it.normalized() == held.normalized() }
                },
            )

            ChoiceKind.LANGUAGE -> pc.copy(
                languages = pc.languages.filterNot { held ->
                    picked.any { it.normalized() == held.normalized() }
                },
            )

            // Only the copy this choice wrote comes off. The same spell known through the
            // class list carries the class's name as its source and stays.
            ChoiceKind.SPELL -> if (choice.optionsFrom != OptionSource.Declared) pc else pc.copy(
                knownSpells = pc.knownSpells.filterNot { known ->
                    known.id in picked && known.source.equals(choice.source, ignoreCase = true)
                },
            )

            ChoiceKind.OPTION, ChoiceKind.ABILITY_SCORE, ChoiceKind.FEAT,
            ChoiceKind.SUBCLASS, ChoiceKind.DAMAGE_TYPE -> pc
        }
    }

    private fun isSkill(id: String): Boolean = Skill.ALL.any { it.name == id }

    private fun String.normalized(): String = trim().lowercase().removeSuffix("s")
}

/**
 * Taking a feat and giving it back, as two halves of one thing.
 *
 * Adding a feat records its answers and applies them through [ChoiceGrants]; removing it
 * revokes those same answers and forgets them, following the feat into any feat it granted.
 * Before this, removing a feat dropped its id and nothing else, so a sheet that had taken and
 * then un-taken Magic Initiate kept the three spells.
 */
object FeatEdits {

    fun add(
        character: PlayerCharacter,
        featId: String,
        selections: Map<String, List<String>> = emptyMap(),
    ): PlayerCharacter {
        if (featId in character.featIds) return character
        val choices = OriginChoices.forFeats(listOf(featId), selections)
        val recorded = character.copy(
            featIds = character.featIds + featId,
            originChoiceSelections = character.originChoiceSelections + selections,
        )
        return ChoiceGrants.apply(recorded, choices, selections)
    }

    fun remove(character: PlayerCharacter, featId: String): PlayerCharacter {
        if (featId !in character.featIds) return character
        val answers = ChoiceResolver.answers(character)
        val choices = OriginChoices.forFeats(listOf(featId), answers)
        val revoked = ChoiceGrants.revoke(character, choices, answers)
        val ids = choices.map { it.id }.toSet()
        return revoked.copy(
            featIds = revoked.featIds - featId,
            originChoiceSelections = revoked.originChoiceSelections - ids,
            classChoiceSelections = revoked.classChoiceSelections - ids,
            levelSelections = revoked.levelSelections.filterKeys { it.substringAfter(':') !in ids },
        )
    }
}
