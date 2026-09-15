package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.PerUseChoiceData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.OptionSource
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.CharacterSummons
import com.pedroeu.ficha.domain.SummonEdits
import com.pedroeu.ficha.domain.ChoiceGrants
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.OwnedOptions
import com.pedroeu.ficha.domain.PerUseChoices
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.RestEngine
import com.pedroeu.ficha.rules.RulesEngine

/**
 * Everything that must be true of any character, checked against one.
 *
 * This exists because fixing the instance never fixed the class. The same bug kept coming back
 * under a new subclass: a question with no answerable options, an answer the sheet and the
 * rules read differently, a pool that promises uses and has no tracker. Each was reported,
 * fixed where it was reported, and left standing everywhere else — because nothing walked the
 * *rest* of the content looking for it.
 *
 * So the rule is: a bug worth reporting is worth writing down here as a thing that must never
 * be true, and then [EveryContentSweepTest] tries it against every subclass, species, lineage,
 * background and feat in the game. A new piece of content that repeats an old mistake fails
 * the build on the day it is added rather than on the day somebody plays it.
 *
 * Every check returns a complaint rather than throwing, so one run reports every problem the
 * content has instead of the first.
 */
object SheetAudit {

    /** Everything wrong with this character, each line naming the rule it breaks. */
    fun complaints(who: String, character: PlayerCharacter): List<String> = buildList {
        val say = { problem: String -> add("$who: $problem") }

        val resolved = ChoiceResolver.all(character)
        val answers = ChoiceResolver.answers(character)

        // -------------------------------------------------- Questions that can be answered
        resolved.forEach { row ->
            val choice = row.choice
            if (choice.options.isEmpty() && choice.optionsFrom == OptionSource.Declared) {
                say("${choice.id} is a question with no options, so nothing can answer it")
            }
            if (choice.count > choice.options.size && choice.options.isNotEmpty()) {
                say("${choice.id} asks for ${choice.count} of ${choice.options.size}")
            }
            if (choice.label.isBlank()) say("${choice.id} has no label")
            choice.options.forEach { option ->
                if (option.name.isBlank()) say("${choice.id} has an option with no name")
            }
            // The answer on the sheet and the answer the rules read are one answer.
            if (answers[choice.id].orEmpty() != row.selectedIds) {
                say(
                    "${choice.id}: the sheet shows ${row.selectedIds} and the rules read " +
                        "${answers[choice.id].orEmpty()}"
                )
            }
            // A stored answer names something the question offers.
            row.selectedIds.forEach { picked ->
                if (choice.options.none { it.id == picked }) {
                    say("${choice.id} holds the answer '$picked', which it does not offer")
                }
            }
        }

        // Two questions must not share an id: the answer to one would be read as the other's.
        resolved.groupBy { it.choice.id }
            .filterValues { rows -> rows.map { it.choice.label }.distinct().size > 1 }
            .forEach { (id, rows) ->
                say("$id is used by ${rows.map { it.choice.label }.distinct()}")
            }

        // -------------------------------------------------- What the player wrote themselves
        //
        // A written option that no question offers is invisible: the player typed it, the
        // sheet kept it, and it appears in no list — which reads as the app having thrown it
        // away. The question it names has to be one this character is actually asked.
        val asked = resolved.associateBy { it.choice.id }
        character.customOptions.forEach { own ->
            if (own.name.isBlank()) {
                say("a written option for ${own.choiceId} has no name, so nothing can show it")
            }
            val row = asked[own.choiceId]
            if (row == null) {
                say("${own.name} was written for ${own.choiceId}, a question this character is never asked")
                return@forEach
            }
            if (row.choice.options.none { it.id == own.id }) {
                say("${own.name} was written for ${own.choiceId} but is not in its list of options")
            }
        }

        // -------------------------------------------------- Spells
        val spells = CharacterSpells.all(character)
        spells.groupBy { it.id }.filterValues { it.size > 1 }.keys.forEach {
            say("the spell $it is on the list twice")
        }
        CharacterSpells.unresolvedGrants(character).forEach {
            say("a grant names '$it', which is not a spell")
        }
        val maxPrepared = CharacterCalculations.maxPreparedSpells(character)
        if (maxPrepared > 0 && CharacterSpells.preparedCount(character) > maxPrepared) {
            say(
                "prepares ${CharacterSpells.preparedCount(character)} spells of $maxPrepared " +
                    "allowed"
            )
        }

        // -------------------------------------------------- Limited uses
        CharacterResources.definitions(character).forEach { pool ->
            if (pool.name.isBlank()) say("${pool.id} is a pool with no name")
            if (pool.source.isBlank()) say("${pool.id} does not say what granted it")
            if (pool.max <= 0) say("${pool.id} has a maximum of ${pool.max}")
            pool.options.forEach { option ->
                if (option.name.isBlank()) say("${pool.id} has an unnamed option")
            }
        }
        CharacterResources.definitions(character).groupBy { it.id }
            .filterValues { it.size > 1 }.keys.forEach { say("two pools share the id $it") }

        // -------------------------------------------------- Chosen at the moment of use
        PerUseChoices.all(character).forEach { active ->
            val source = active.choice
            if (source.options.isEmpty()) say("${source.id} is an on-use choice with no options")
            val held = when {
                source.subclassId.isNotBlank() -> ClassLevels.hasSubclass(character, source.subclassId)
                source.speciesId.isNotBlank() -> character.speciesId == source.speciesId
                // A class can grant a decision of its own — the Rogue's Cunning Strike is the
                // first, chosen every time Sneak Attack lands.
                source.classId.isNotBlank() ->
                    ClassLevels.of(character).any { it.classId == source.classId }
                else -> false
            }
            if (!held) say("${source.id} is offered and its source is not held")
        }

        // -------------------------------------------------- Numbers the sheet prints
        if (CharacterCalculations.maxHitPoints(character) < 1) say("has no hit points")
        if (CharacterCalculations.armorClass(character) < 1) say("has no Armor Class")
        if (CharacterCalculations.speed(character) < 0) say("has a negative Speed")
        if (character.currentHitPoints > CharacterCalculations.maxHitPoints(character)) {
            say("has more hit points than its maximum")
        }
        CharacterResources.states(character).forEach {
            if (it.spent > it.def.max) say("${it.def.id} has spent more than it holds")
        }

        // -------------------------------------------------- What the engine says about it
        RulesEngine.elementsFor(character).groupBy { it.id }
            .filterValues { it.size > 1 }.keys.forEach { say("two rule elements share the id $it") }
        RulesEngine.elementsFor(character).forEach {
            if (it.name.isBlank()) say("${it.id} is a rule element with no name")
        }

        // -------------------------------------------------- Creatures it can call up
        CharacterSummons.available(character).forEach { summonable ->
            if (summonable.options.isEmpty()) {
                say("${summonable.summons.summonId} can be summoned and has no stat block")
            }
        }

        // Anything already on the table has to be describable. A creature whose rules cannot
        // be stated draws as an apology where a stat block should be, and the way to get one
        // is to delete a written creature that something was still an instance of.
        character.activeSummons.forEach { summon ->
            if (SummonEdits.resolve(character, summon) == null) {
                say("${summon.name.ifBlank { summon.statblockId }} is on the table with no rules behind it")
            }
            if (summon.maxHp <= 0) {
                say("${summon.name.ifBlank { summon.statblockId }} has no hit points")
            }
        }

        character.customStatblocks.forEach { creature ->
            if (creature.name.isBlank()) say("a written creature has no name")
            if (creature.hitPoints <= 0) say("${creature.name} was written with no hit points")
            creature.actions.filter { it.name.isBlank() }.forEach {
                say("${creature.name} has an action with no name")
            }
        }

        // -------------------------------------------------- A rest leaves it standing
        val rested = RestEngine.longRest(character).character
        if (CharacterCalculations.maxHitPoints(rested) != CharacterCalculations.maxHitPoints(character)) {
            say("a Long Rest changed its maximum hit points")
        }
        if (rested.currentHitPoints != CharacterCalculations.maxHitPoints(rested)) {
            say("a Long Rest did not heal it to full")
        }
        CharacterResources.states(rested)
            .filter { it.def.recharge.refilledBy(com.pedroeu.ficha.data.model.Recharge.LONG_REST) }
            .forEach { if (it.spent > 0) say("${it.def.id} survived a Long Rest still spent") }
    }

    /**
     * The character with every open question answered, the way a player would leave it.
     *
     * Most of what goes wrong only goes wrong once a choice is made — an invocation that
     * grants a spell, a subclass option that raises another question — so a sweep that only
     * looked at unanswered characters would miss the half of the content that matters.
     * Answering runs to a fixed point because an answer can raise a new question.
     */
    fun fullyAnswered(character: PlayerCharacter): PlayerCharacter {
        var pc = character
        repeat(8) {
            val open = ChoiceResolver.all(pc).filter { row ->
                row.selectedIds.size < row.choice.count && row.choice.options.isNotEmpty()
            }
            if (open.isEmpty()) return pc
            open.forEach { row ->
                val disabled = OwnedOptions.disabledFor(
                    choice = row.choice,
                    owned = OwnedOptions.of(pc),
                    currentSelection = row.selectedIds.toSet(),
                    classLevels = ClassLevels.levelMap(pc),
                )
                val pick = (row.selectedIds + row.choice.options
                    .filter { it.id !in disabled && it.id !in row.selectedIds }
                    .map { it.id })
                    .distinct()
                    .take(row.choice.count)
                if (pick.isNotEmpty()) {
                    pc = ChoiceGrants.answer(pc, row.choice, row.level, pick)
                }
            }
        }
        return pc
    }

    /**
     * A character of one class and subclass as the creation wizard would have left it.
     *
     * The level 1 skill proficiencies matter more than they look: Expertise is offered from
     * the skills you are trained in, so a character built without any is asked to pick two of
     * a list of none — a question with no answer, and one no real character ever meets.
     */
    fun character(
        classId: String,
        level: Int,
        subclassId: String? = null,
        speciesId: String = "human",
        lineageId: String? = null,
        featIds: List<String> = emptyList(),
        classLevels: List<com.pedroeu.ficha.domain.ClassLevel> = emptyList(),
    ): PlayerCharacter {
        val skills = ClassData.byId(classId)?.choices
            .orEmpty()
            .filterIsInstance<com.pedroeu.ficha.data.model.ClassChoice.SkillProficiencyChoice>()
            .flatMap { it.options.take(it.count) }
            .map { it.name }
            .toSet()
        val base = PlayerCharacter(
            id = "sweep", name = "Sweep", speciesId = speciesId, lineageId = lineageId,
            classId = classId, subclassId = subclassId, backgroundId = "soldier",
            level = level, featIds = featIds, classLevels = classLevels,
            skillProficiencies = skills,
            baseAbilityScores = com.pedroeu.ficha.data.model.Ability.ALL
                .associate { it.name to 15 },
        )
        return base.copy(currentHitPoints = CharacterCalculations.maxHitPoints(base))
    }

    /** Every class the app ships, for a sweep that wants all of them. */
    fun everyClassId(): List<String> = ClassData.ALL.map { it.id }

    /** Spell ids the catalogue knows, for checks that need a real spell. */
    fun isRealSpell(id: String): Boolean = SpellData.byId(id) != null

    /** On-use choices, for a sweep that wants to reach every one of them. */
    fun everyPerUseSource(): List<Pair<String, String>> = PerUseChoiceData.sourceIds()

    /** True when the choice is one the app expects a player to answer from a list. */
    fun isAnswerable(kind: ChoiceKind): Boolean = kind != ChoiceKind.SUBCLASS
}
