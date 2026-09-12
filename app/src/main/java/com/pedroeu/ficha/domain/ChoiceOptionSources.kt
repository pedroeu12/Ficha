package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceOption
import com.pedroeu.ficha.data.model.ChoiceOptions
import com.pedroeu.ficha.data.model.OptionSource
import com.pedroeu.ficha.data.model.Skill

/**
 * Fills in the options for a choice whose list depends on the character.
 *
 * "Choose one of your known Warlock cantrips that deals damage" is not a list anybody can
 * write down in advance. Written as the whole Warlock cantrip list it stopped being that
 * question and became "learn a new cantrip" — a different feature, and one Agonizing Blast
 * does not offer. The same wording appears on Eldritch Spear, Repelling Blast, the Wizard's
 * Spell Mastery and Signature Spells, and on every Expertise choice in the game, which were
 * all offering eighteen skills where the rules offer the ones you are trained in.
 *
 * One resolver, applied where the choice walk runs, so no screen has to remember.
 */
object ChoiceOptionSources {

    /** The same choice with its options read from the character, where that is what it says. */
    fun resolve(choice: Choice, character: PlayerCharacter): Choice {
        val resolved = when (val source = choice.optionsFrom) {
            OptionSource.Declared -> return choice

            OptionSource.ProficientSkills -> ChoiceOptions.fromSkills(
                Skill.ALL.filter { it.name in character.skillProficiencies }
            )

            is OptionSource.KnownSpells -> knownSpells(character, source)
        }
        // A question with nothing to pick cannot be answered, and the level-up flow will not
        // move past one — so a character who genuinely has nothing eligible would be stuck.
        // Keeping the declared list is too generous by the rules and the lesser fault: a
        // flow that cannot be finished is worse than a list that offers too much. In practice
        // this never fires, because the rules gate these features behind having the thing.
        return if (resolved.isEmpty()) choice else choice.copy(options = resolved)
    }

    fun resolveAll(choices: List<Choice>, character: PlayerCharacter): List<Choice> =
        choices.map { resolve(it, character) }

    private fun knownSpells(
        character: PlayerCharacter,
        source: OptionSource.KnownSpells,
    ): List<ChoiceOption> =
        CharacterSpells.all(character)
            .mapNotNull { SpellData.byId(it.id) }
            .distinctBy { it.id }
            .filter { spell ->
                (source.level == null || spell.level == source.level) &&
                    (source.classId.isBlank() || source.classId in spell.classes) &&
                    (!source.needsDamage || spell.damage.isNotBlank()) &&
                    (!source.needsAttackRoll || spell.needsAttackRoll)
            }
            .sortedBy { it.name }
            .map { ChoiceOption(it.id, it.name, it.description, it.subtitle, it.book) }
}
