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
    ): ResolvedChoice {
        val ids = selectionsFor(character, choice.id, level.takeIf { it > 0 })
        return ResolvedChoice(
            choice = choice,
            selectedIds = ids,
            selectedNames = ids.map { nameFor(choice, it) },
            featureName = featureName,
            level = level,
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
                    feature.choices.map { resolve(character, it, feature.name, feature.level) }
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
                    feature.choices.map { resolve(character, it, feature.name, feature.level) }
                }
        }

    /**
     * Level-1 class options, which use the older [ClassChoice] shape. Only the feature
     * branches are meaningful here; cantrip picks already surface as known spells.
     */
    fun levelOneClassOptions(character: PlayerCharacter): List<Pair<String, String>> {
        val charClass = ClassData.byId(character.classId) ?: return emptyList()
        return charClass.choices
            .filterIsInstance<ClassChoice.FeatureOption>()
            .mapNotNull { choice ->
                val selectedId = character.classChoiceSelections[choice.id]?.firstOrNull()
                val option = choice.options.find { it.id == selectedId } ?: return@mapNotNull null
                choice.label to option.name
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
            classSelections = character.classChoiceSelections,
            backgroundId = character.backgroundId,
            originSelections = character.originChoiceSelections,
            extraFeatIds = character.featIds,
        ).map { choice -> resolve(character, choice, choice.label, 0) }

    /** Everything the character has decided, for the "your choices" view and rests. */
    fun all(character: PlayerCharacter): List<ResolvedChoice> =
        (classFeatureChoices(character) +
            subclassFeatureChoices(character) +
            originChoices(character))
            .distinctBy { it.choice.id to it.level }

    /** Choices the rules let you revisit when you rest. */
    fun restChangeable(character: PlayerCharacter): List<ResolvedChoice> =
        all(character).filter { it.choice.changeableOnRest }
}
