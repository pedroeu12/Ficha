package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.data.model.Skill

/** A decision the character made, resolved to readable names for the sheet. */
data class ResolvedChoice(
    val choice: Choice,
    val selectedIds: List<String>,
    val selectedNames: List<String>,
    /** The feature the choice hangs off, e.g. "Beast Tattoos" or "Level 4". */
    val featureName: String,
    val level: Int,
    /**
     * The class the choice was earned in, empty for one that came from a species, a
     * background, or a feat.
     *
     * A multiclassed character reaches "Ability Score Improvement" once per class, and the
     * feature name alone can't tell the Fighter's from the Wizard's — which is how both ended
     * up listed under both.
     */
    val classId: String = "",
) {
    val isAnswered: Boolean get() = selectedIds.isNotEmpty()
    val summary: String get() = selectedNames.joinToString(", ")
}

/**
 * Selections are written into three different maps depending on where the choice came from,
 * and until now nothing read them back — so a Monk's chosen tattoos, a Battle Master's
 * maneuvers, and every Fighting Style taken after level 1 were saved but invisible.
 *
 * This resolves all three, for any feature, so the sheet can show what was actually picked.
 */
object ChoiceResolver {

    /** Level-up selections are keyed by the level they were made at. */
    private fun levelKey(level: Int, choiceId: String) = "$level:$choiceId"

    /**
     * Finds the stored selection for a choice, trying every map it could live in.
     * [level] narrows the level-up lookup; null searches all levels.
     */
    fun selectionsFor(
        character: PlayerCharacter,
        choiceId: String,
        level: Int? = null,
    ): List<String> {
        if (level != null) {
            character.levelSelections[levelKey(level, choiceId)]?.let { return it }
        } else {
            // A choice can recur at several levels (Battle Master maneuvers); gather them all.
            val fromLevels = character.levelSelections
                .filterKeys { it.substringAfter(':') == choiceId }
                .values
                .flatten()
            if (fromLevels.isNotEmpty()) return fromLevels.distinct()
        }
        character.classChoiceSelections[choiceId]?.let { return it }
        character.originChoiceSelections[choiceId]?.let { return it }
        return emptyList()
    }

    /**
     * Every question the character has answered, flattened to choice id against option ids.
     *
     * The three maps a selection can live in are an implementation detail of when it was
     * made, not of what it means, and a feature that raises a follow-up question shouldn't
     * have to know which one to look in. Where a question was answered at several levels the
     * newest answer wins, on the same reasoning as [latestSelectionFor].
     */
    fun answers(character: PlayerCharacter): Map<String, List<String>> {
        val flat = mutableMapOf<String, List<String>>()
        flat += character.classChoiceSelections
        flat += character.originChoiceSelections
        character.levelSelections.entries
            .sortedBy { it.key.substringBefore(':').toIntOrNull() ?: 0 }
            .forEach { (key, ids) -> flat[key.substringAfter(':')] = ids }
        return flat
    }

    /**
     * The answer given at the highest level, for a choice that restates its whole list.
     *
     * Weapon Mastery and the Artificer's arcane plans are asked again each time the count
     * grows, and each asking replaces the previous answer rather than adding to it. Unioning
     * every level's answer — which is what [selectionsFor] does, correctly, for a Battle
     * Master's maneuvers — would leave a character who changed their mind holding both the
     * old pick and the new one.
     */
    fun latestSelectionFor(character: PlayerCharacter, choiceId: String): List<String> {
        val fromLevels = character.levelSelections
            .filterKeys { it.substringAfter(':') == choiceId }
            .filterValues { it.isNotEmpty() }
            .maxByOrNull { it.key.substringBefore(':').toIntOrNull() ?: 0 }
        if (fromLevels != null) return fromLevels.value
        // Characters made before the choice was keyed by level fall back to the flat maps.
        return character.classChoiceSelections[choiceId]
            ?: character.originChoiceSelections[choiceId]
            ?: emptyList()
    }

    /** Turns stored option ids into names the sheet can print. */
    fun nameFor(choice: Choice, optionId: String): String {
        choice.options.find { it.id == optionId }?.let { return it.name }
        return when (choice.kind) {
            ChoiceKind.SPELL -> SpellData.byId(optionId)?.name ?: optionId
            ChoiceKind.SKILL, ChoiceKind.EXPERTISE ->
                Skill.ALL.find { it.name == optionId }?.displayName ?: optionId
            ChoiceKind.SUBCLASS -> SubclassData.byId(optionId)?.name ?: optionId
            else -> optionId
        }
    }

    private fun resolve(
        character: PlayerCharacter,
        choice: Choice,
        featureName: String,
        level: Int,
        classId: String = "",
    ): ResolvedChoice {
        val ids = selectionsFor(character, choice.id, level.takeIf { it > 0 })
        return ResolvedChoice(
            choice = choice,
            selectedIds = ids,
            selectedNames = ids.map { nameFor(choice, it) },
            featureName = featureName,
            level = level,
            classId = classId,
        )
    }

    /**
     * Every choice attached to class-table features the character has reached.
     *
     * Features advance on the level in their own class, so a Fighter 5 / Wizard 3 sees a
     * level 5 Fighter's choices and a level 3 Wizard's, not level 8 of either.
     */
    fun classFeatureChoices(character: PlayerCharacter): List<ResolvedChoice> =
        ClassLevels.of(character).flatMap { entry ->
            val progression = ProgressionData.forClass(entry.classId)
                ?: return@flatMap emptyList()
            progression.features
                .filter { it.level <= entry.level }
                .flatMap { feature ->
                    feature.choices.map {
                        resolve(character, it, feature.name, feature.level, entry.classId)
                    }
                }
        }

    /** Every choice attached to subclass features the character has reached. */
    fun subclassFeatureChoices(character: PlayerCharacter): List<ResolvedChoice> =
        ClassLevels.of(character).flatMap { entry ->
            val subclass = entry.subclassId?.let { SubclassData.byId(it) }
                ?: return@flatMap emptyList()
            subclass.features
                .filter { it.level <= entry.level }
                .flatMap { feature ->
                    feature.choices.map {
                        resolve(character, it, feature.name, feature.level, entry.classId)
                    }
                }
        }

    /**
     * Level-1 class options, which use the older [ClassChoice] shape. Only the feature
     * branches are meaningful here; cantrip picks already surface as known spells.
     */
    fun levelOneClassOptions(
        character: PlayerCharacter,
        /** One class's options, or every class's when left out. */
        classId: String? = null,
    ): List<Pair<String, String>> {
        val ids = classId?.let { listOf(it) } ?: ClassLevels.of(character).map { it.classId }
        return ids.flatMap { id ->
            val charClass = ClassData.byId(id) ?: return@flatMap emptyList()
            charClass.choices
                .filterIsInstance<ClassChoice.FeatureOption>()
                .mapNotNull { choice ->
                    val selectedId = character.classChoiceSelections[choice.id]?.firstOrNull()
                    val option = choice.options.find { it.id == selectedId }
                        ?: return@mapNotNull null
                    choice.label to option.name
                }
        }
    }

    /**
     * Choices from species, background, and every feat the character holds — including ones
     * taken in place of an Ability Score Improvement, which have their own sub-choices and
     * used to appear nowhere at all.
     */
    fun originChoices(character: PlayerCharacter): List<ResolvedChoice> =
        OriginChoices.all(
            speciesId = character.speciesId,
            lineageId = character.lineageId,
            classId = character.classId,
            // Everything answered, not only the creation-time map: an invocation picked at
            // level 5 raises its own damage-type question, and it lives in levelSelections.
            classSelections = answers(character),
            backgroundId = character.backgroundId,
            originSelections = character.originChoiceSelections,
            extraFeatIds = character.featIds,
            books = character.enabledSources,
        ).map { choice -> resolve(character, choice, choice.label, 0) }

    /** Everything the character has decided, for the "your choices" view and rests. */
    fun all(character: PlayerCharacter): List<ResolvedChoice> =
        (classFeatureChoices(character) +
            subclassFeatureChoices(character) +
            originChoices(character))
            .distinctBy { it.choice.id to it.level }

    /**
     * The "pick some from a pool" features, gathered for Edit Mode to work on directly.
     *
     * Eldritch Invocations, Artificer plans, Fighting Styles, Weapon Masteries, Metamagic,
     * Battle Master maneuvers — all the same shape: a list the class hands you and a number
     * you may hold from it. They are normally decided during creation or a level up and only
     * revisited through the feature that granted them, which is a long way to walk when what
     * you want is to swap one invocation. This is the short way.
     *
     * A subclass isn't one of these — choosing it opens a whole flow of its own — and neither
     * is a choice with nothing to weigh, where the pool is no bigger than the pick.
     */
    fun poolChoices(character: PlayerCharacter): List<ResolvedChoice> =
        all(character)
            .filter { it.choice.kind != ChoiceKind.SUBCLASS }
            .filter { it.choice.options.size > it.choice.count }
            .groupBy { it.choice.id }
            // A choice asked at several levels is one decision; its newest form is current.
            .map { (_, versions) -> versions.maxBy { it.level } }
            .sortedWith(compareBy({ it.featureName }, { it.choice.label }))

    /**
     * Choices the rules let you revisit every time you gain a level.
     *
     * The Primordial Patron's element is the whole of it so far: it is picked at level 3 and
     * then offered again at 4, at 5, and at every level after — so it can't be found by
     * looking at what the new level grants, which is where the level-up flow otherwise looks.
     * Asked here so it is re-offered from the level it was first granted onward, with the
     * current answer standing until the player changes it.
     */
    fun levelUpChangeable(character: PlayerCharacter): List<ResolvedChoice> =
        (classFeatureChoices(character) + subclassFeatureChoices(character))
            .filter { it.choice.changeableOnLevelUp }
            .distinctBy { it.choice.id }

    /**
     * Choices the rules let you revisit when you rest.
     *
     * A choice asked again at several levels — Weapon Mastery, which restates the whole list
     * each time the count grows — is one decision, so only its current form is offered.
     */
    fun restChangeable(character: PlayerCharacter): List<ResolvedChoice> =
        all(character)
            .filter { it.choice.changeableOnRest }
            .groupBy { it.choice.id }
            .map { (_, versions) -> versions.maxBy { it.level } }
}
