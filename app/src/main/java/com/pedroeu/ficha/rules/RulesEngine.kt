package com.pedroeu.ficha.rules

import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.PlayerCharacter

/**
 * One place that knows what a character's rules are.
 *
 * Every piece of content becomes a [RuleElement] carrying every rule it implies, this gathers
 * the ones whose [Gate] is satisfied, and the typed views below are what the rest of the app
 * reads. Nothing else walks the content tables; nothing else re-derives whether a level counts
 * against a class or the character.
 *
 * That last part is the whole argument for the engine. The app had nine tables keyed by the
 * same source ids with nothing joining them, and the same distinctions — this level or that
 * one, granted or offered, chosen now or chosen on use — spelled out separately in each. Every
 * bug class that kept coming back was one of those spellings being wrong in one place.
 */
object RulesEngine {

    /**
     * Every element in force, with the questions its answers raised.
     *
     * The walk is the same fixed point [com.pedroeu.ficha.domain.ChoiceGraph] does for
     * choices, widened to effects: an option the player picked contributes whatever it
     * carries, and if that includes another question the answer to *that* is followed too.
     * An option nobody picked contributes nothing, which is what lets this be handed the whole
     * rulebook without asking about invocations no one took.
     */
    fun elementsFor(character: PlayerCharacter): List<RuleElement> =
        cache.get(character, ::computeElements)

    /**
     * One entry, keyed on the character it was worked out for.
     *
     * The walk reads every table the character touches, and once the engine feeds a number the
     * sheet shows — Armor Class, maximum Hit Points — it is asked again on every recomposition.
     * A character is an immutable value that Compose hands back unchanged until something
     * edits it, so remembering the last answer is enough: an edit produces a new instance and
     * the entry misses. Single-entry rather than a map because one character is on screen.
     */
    private object cache {
        private var key: PlayerCharacter? = null
        private var value: List<RuleElement> = emptyList()

        @Synchronized
        fun get(
            character: PlayerCharacter,
            compute: (PlayerCharacter) -> List<RuleElement>,
        ): List<RuleElement> {
            if (key === character) return value
            val fresh = compute(character)
            key = character
            value = fresh
            return fresh
        }
    }

    private fun computeElements(character: PlayerCharacter): List<RuleElement> {
        val answers = ChoiceResolver.answers(character)
        val all = Adapters.all(character)

        val held = mutableListOf<RuleElement>()
        val heldIds = mutableSetOf<String>()
        var frontier = all.filter { satisfies(it.gate, character, answers, heldIds) }

        while (frontier.isNotEmpty()) {
            frontier.forEach { element ->
                if (heldIds.add(element.id)) held += element
            }
            // Options that were picked bring their own elements in behind them.
            val fromAnswers = held.flatMap { element ->
                element.effectsOf<Effect.AskOnGain>().flatMap { ask ->
                    answers[ask.choice.id].orEmpty().mapNotNull { pickedId ->
                        ask.choice.options.find { it.id == pickedId }
                            ?.let { option -> optionElement(element, ask.choice, option.id, option.grants) }
                    }
                }
            }
            frontier = (fromAnswers + all)
                .filterNot { it.id in heldIds }
                .filter { satisfies(it.gate, character, answers, heldIds) }
        }
        return held
    }

    /** An option the player picked, as an element of its own so its rules can be found. */
    private fun optionElement(
        parent: RuleElement,
        choice: Choice,
        optionId: String,
        grants: List<Choice>,
    ): RuleElement {
        val option = choice.options.find { it.id == optionId }
        return Adapters.withModifiers(RuleElement(
            id = "option:${choice.id}:$optionId",
            name = option?.name ?: optionId,
            description = option?.description.orEmpty(),
            source = Source.Option(optionId, choice.id, parent.source),
            gate = Gate.ALWAYS,
            // An option carries whatever it carries: the questions it raises, and anything
            // it adds to a summon someone else provides.
            effects = grants.map { Effect.AskOnGain(it) } +
                listOfNotNull(
                    com.pedroeu.ficha.data.content.SummonData.EXTENSIONS_BY_OPTION[optionId]
                ) +
                com.pedroeu.ficha.data.content.ModifierData.forOption(optionId),
            book = option?.book,
        ))
    }

    private fun satisfies(
        gate: Gate,
        character: PlayerCharacter,
        answers: Map<String, List<String>>,
        heldIds: Set<String>,
    ): Boolean {
        if (gate.requires.any { it !in heldIds }) return false
        if (gate.whenChosen.any { (choiceId, optionId) ->
                optionId !in answers[choiceId].orEmpty()
            }
        ) return false
        return true
    }

    /**
     * Every effect in force, each tagged with the element it came from.
     *
     * A gate is checked here rather than in [elementsFor] because an element's *level* depends
     * on which class it belongs to, and the answer differs per class for a multiclassed
     * character. Keeping the two apart is what stops a Cleric 3 / Fighter 9 from being handed
     * a level 9 domain list.
     */
    fun effectsFor(character: PlayerCharacter): List<Applied<Effect>> =
        elementsFor(character)
            .filter { reachedLevel(it, character) }
            .flatMap { element -> element.effects.map { Applied(it, element) } }

    private fun reachedLevel(element: RuleElement, character: PlayerCharacter): Boolean {
        val level = FormulaEval.levelFor(
            character,
            element.gate.levelScope,
            element.source.owningClassId,
        )
        return level >= element.gate.level
    }

    // ================================================================ Typed views

    /** Everything a UI could show as one of these, without knowing what produced it. */
    inline fun <reified T : Effect> view(character: PlayerCharacter): List<Applied<T>> =
        effectsFor(character).mapNotNull { applied ->
            (applied.effect as? T)?.let { Applied(it, applied.element) }
        }

    fun choicesOnGain(character: PlayerCharacter): List<Applied<Effect.AskOnGain>> =
        view(character)

    fun choicesOnUse(character: PlayerCharacter): List<Applied<Effect.AskOnUse>> =
        view(character)

    fun grantedSpells(character: PlayerCharacter): List<Applied<Effect.GrantSpell>> =
        view(character)

    fun pools(character: PlayerCharacter): List<Applied<Effect.LimitedUses>> =
        view(character)

    fun saveDcs(character: PlayerCharacter): List<Applied<Effect.ProvidesSaveDc>> =
        view(character)

    fun summons(character: PlayerCharacter): List<Applied<Effect.Summons>> =
        view(character)

    /**
     * The total a stat gains from every rule that touches it, conditions honoured.
     *
     * A condition the sheet cannot decide never contributes: [Condition.Descriptive] is the
     * marker for a rule written as prose, and "while Raging" is a real rule the sheet has no
     * way to know is true right now. Applying a number the engine cannot justify is worse than
     * applying none — the player can add it themselves, but they cannot find one that is
     * silently already in the total.
     */
    fun statBonus(character: PlayerCharacter, target: StatTarget): Int =
        statSources(character, target)
            .filter { ConditionEval.holds(it.effect.condition, character) }
            .sumOf {
                FormulaEval.eval(it.effect.amount, character, it.element.source.owningClassId)
            }

    /** The same, itemised, for a sheet that shows where a number came from. */
    fun statSources(
        character: PlayerCharacter,
        target: StatTarget,
    ): List<Applied<Effect.ModifyStat>> =
        view<Effect.ModifyStat>(character).filter { it.effect.target == target }

    /**
     * The best base value on offer for a stat, or [default] when nothing beats it.
     *
     * Unarmored Defense and everything shaped like it: several candidates, each with its own
     * condition, and the rules take the highest of the ones that apply. Worth being a single
     * function because the alternative — the one the app had — was a `when` block that had to
     * be extended by hand for every new source and silently answered for none of them.
     */
    fun statBase(character: PlayerCharacter, target: StatTarget, default: Int): Int =
        (baseSources(character, target)
            .filter { ConditionEval.holds(it.effect.condition, character) }
            .map { FormulaEval.eval(it.effect.amount, character, it.element.source.owningClassId) }
            + default)
            .max()

    fun baseSources(
        character: PlayerCharacter,
        target: StatTarget,
    ): List<Applied<Effect.SetStatBase>> =
        view<Effect.SetStatBase>(character).filter { it.effect.target == target }

    /** How many uses a pool has, worked out from its formula. */
    fun poolMax(character: PlayerCharacter, poolId: String): Int =
        pools(character)
            .firstOrNull { it.effect.poolId == poolId }
            ?.let { FormulaEval.eval(it.effect.max, character, it.element.source.owningClassId) }
            ?: 0

    /** Classes the character has levels in, for adapters and views that need the split. */
    fun classesOf(character: PlayerCharacter) = ClassLevels.of(character)
}
