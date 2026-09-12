package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.content.FeatChoiceData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind

/**
 * Every feat the character holds, however it arrived.
 *
 * A feat can reach a character by six routes: chosen at creation, granted by a background,
 * offered by a species trait, taken in place of an Ability Score Improvement, granted by
 * another feat, or — the one that was missed — handed over by an Eldritch Invocation.
 *
 * Only the first four ever reached [PlayerCharacter.featIds], and thirteen places read that
 * field directly. So a feat granted by an invocation had its *questions* asked, because the
 * choice walk follows a FEAT-kind answer into the feat's own choices, and then none of its
 * effects applied: no spells, no limited uses, no save DC, no ability score, no passive bonus.
 * Half a feat, which is worse than none, because the sheet looked like it had worked.
 *
 * This is the one answer to "which feats does this character have". Nothing should read
 * [PlayerCharacter.featIds] to decide what the character can do — that field is where feats
 * chosen outside a choice are *stored*, not the whole of what is held.
 */
object CharacterFeats {

    /**
     * Ids of every choice in the game that hands over a feat.
     *
     * Built from static content only — no character — so gathering a character's feats cannot
     * loop back through the choice walk that needs to know which feats they have.
     */
    private val FEAT_CHOICE_IDS: Set<String> by lazy {
        buildSet {
            fun walk(choices: List<Choice>) {
                choices.forEach { choice ->
                    if (choice.kind == ChoiceKind.FEAT) add(choice.id)
                    choice.options.forEach { walk(it.grants) }
                }
            }
            ProgressionData.ALL.forEach { p -> p.features.forEach { walk(it.choices) } }
            SubclassData.ALL.forEach { s -> s.features.forEach { walk(it.choices) } }
            SpeciesData.ALL.forEach { species ->
                species.traits.forEach { walk(it.choices) }
                species.lineageOptions.forEach { walk(it.choices) }
                // The Origin feat a Versatile species offers, whose id is built per species.
                if (species.grantsOriginFeat) add("species:${species.id}:origin_feat")
            }
            // The Origin feat the five open backgrounds leave to the player.
            BackgroundData.ALL.forEach { add("background:${it.id}:origin_feat") }
            // And a feat's own feat-granting choices, so a chain resolves.
            FeatData.ALL.forEach { walk(FeatChoiceData.choicesFor(it.id, it.name)) }
        }
    }

    /**
     * The feats in force, followed to a fixed point.
     *
     * A feat can grant a feat — a Human's Versatile trait offers one, and that one may be
     * Magic Initiate — so answering stops only when nothing new appears.
     */
    fun heldBy(character: PlayerCharacter): List<String> {
        val answers = ChoiceResolver.answers(character)
        val held = character.featIds.toMutableList()
        val seen = held.toMutableSet()

        var changed = true
        while (changed) {
            changed = false
            FEAT_CHOICE_IDS.forEach { choiceId ->
                answers[choiceId].orEmpty().forEach { featId ->
                    if (FeatData.byId(featId) != null && seen.add(featId)) {
                        held += featId
                        changed = true
                    }
                }
            }
            // A background's fixed feat, which is granted rather than chosen.
            val background = character.backgroundId.let { BackgroundData.byId(it) }
            if (background?.featChoice == null) {
                background?.featId?.takeIf { it.isNotBlank() && seen.add(it) }?.let {
                    held += it
                    changed = true
                }
            }
        }
        return held
    }

    /** True when the character holds [featId] by any route. */
    fun has(character: PlayerCharacter, featId: String): Boolean =
        featId in heldBy(character)
}
