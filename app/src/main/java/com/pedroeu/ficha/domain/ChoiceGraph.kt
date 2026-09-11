package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.Sourcebook

/**
 * The one rule: a choice that has been answered may raise further choices, and those get
 * asked too — wherever the answering happened.
 *
 * This app has had the same bug reported under six different names. A Sage's Magic Initiate
 * granted two cantrips nobody was asked to pick. An Origin feat taken through a Human's
 * Versatile trait asked nothing further. Eldritch Invocations were added with no way to name
 * the cantrip they modify. Each was fixed where it was found, by writing another branch that
 * knew about that one feature, in the one screen it had been reported on.
 *
 * They were never six bugs. A [com.pedroeu.ficha.data.model.ChoiceOption] could not carry a
 * question of its own, so a question an option implied had to be hand-written somewhere that
 * knew the option by name — and there was no list of which options implied one, so every new
 * subclass, feat or invocation added another chance to forget. It was forgotten every time.
 *
 * The question now travels with the option, in
 * [com.pedroeu.ficha.data.model.ChoiceOption.grants], and this walks from what the player has
 * answered to everything those answers raise, repeatedly, until nothing new appears. It knows
 * nothing about invocations, feats or patrons, so content added later is asked about without
 * anyone remembering to wire it up.
 */
object ChoiceGraph {

    /**
     * [roots] plus every choice the answers to them raise, and so on to a fixed point.
     *
     * [answers] is choice id against the option ids picked for it. A choice nobody has
     * answered yet contributes nothing, which is what lets this be called with the whole
     * rulebook's worth of roots: an invocation not taken raises no question.
     *
     * [books] drops options from books the character isn't using. Options that are not book
     * content — a skill, a damage type, an ability score — carry no book and always stay.
     */
    fun expand(
        roots: List<Choice>,
        answers: Map<String, List<String>>,
        books: Set<Sourcebook> = Sourcebook.EVERYTHING,
    ): List<Choice> {
        val out = mutableListOf<Choice>()
        val seen = mutableSetOf<String>()
        var frontier = roots

        while (frontier.isNotEmpty()) {
            val next = mutableListOf<Choice>()
            frontier.forEach { choice ->
                if (!seen.add(choice.id)) return@forEach
                out += choice
                // Only what was actually picked asks anything further.
                answers[choice.id].orEmpty().forEach { pickedId ->
                    choice.options.find { it.id == pickedId }?.grants?.let { next += it }
                    // A choice that hands out a feat hands out that feat's own questions.
                    // Magic Initiate asks for two cantrips and a spell whether it arrived
                    // from a background, a species trait, an Ability Score Improvement or an
                    // Eldritch Invocation, and this is every one of those routes at once.
                    if (choice.kind == ChoiceKind.FEAT) {
                        next += com.pedroeu.ficha.data.content.FeatChoiceData
                            .choicesFor(pickedId, featName(pickedId))
                    }
                }
            }
            frontier = next.filterNot { it.id in seen }
        }

        return out.map { choice -> choice.limitedTo(books) }
    }

    /**
     * Only the choices the answers raised — the roots themselves left out.
     *
     * For a screen that already shows its own questions and needs the follow-ups beside them
     * rather than a second copy of the list it started from.
     */
    fun followUps(
        roots: List<Choice>,
        answers: Map<String, List<String>>,
        books: Set<Sourcebook> = Sourcebook.EVERYTHING,
    ): List<Choice> {
        val rootIds = roots.map { it.id }.toSet()
        return expand(roots, answers, books).filterNot { it.id in rootIds }
    }

    /**
     * Everything a character has been asked, roots and follow-ups alike.
     *
     * The roots are every choice the character's classes, subclasses, species, background and
     * feats put to them. Gathering all of them rather than only this level's is what lets an
     * invocation taken at level 2 still raise its cantrip question at level 11.
     */
    fun forCharacter(character: PlayerCharacter): List<Choice> =
        expand(
            roots = rootsFor(character),
            answers = ChoiceResolver.answers(character),
            books = character.enabledSources,
        )

    /** Every question the character's own classes and subclasses put to them. */
    fun rootsFor(character: PlayerCharacter): List<Choice> =
        ClassLevels.of(character).flatMap { entry ->
            val fromClass = com.pedroeu.ficha.data.content.ProgressionData
                .forClass(entry.classId)
                ?.features
                ?.filter { it.level <= entry.level }
                ?.flatMap { it.choices }
                .orEmpty()
            val fromSubclass = entry.subclassId
                ?.let { com.pedroeu.ficha.data.content.SubclassData.byId(it) }
                ?.features
                ?.filter { it.level <= entry.level }
                ?.flatMap { it.choices }
                .orEmpty()
            fromClass + fromSubclass
        }

    /**
     * The same choice with options from books the character isn't using removed.
     *
     * A choice whose every option would be dropped keeps them all: an empty list of options
     * is a question that cannot be answered, which blocks the flow it appears in. That only
     * happens when a book was turned off after the choice was built, and offering the option
     * is better than presenting a dead end.
     */
    private fun featName(featId: String): String =
        com.pedroeu.ficha.data.content.FeatData.byId(featId)?.name ?: featId

    private fun Choice.limitedTo(books: Set<Sourcebook>): Choice {
        if (options.isEmpty()) return this
        val kept = options.filter { it.book == null || it.book in books }
        return if (kept.isEmpty() || kept.size == options.size) this else copy(options = kept)
    }
}
