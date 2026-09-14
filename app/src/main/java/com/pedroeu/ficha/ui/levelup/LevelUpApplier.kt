package com.pedroeu.ficha.ui.levelup

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.model.SpellDef
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ChoiceGrants
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.Multiclassing
import com.pedroeu.ficha.domain.PlayerCharacter

/**
 * Turns a finished level-up into the character that results.
 *
 * A pure function of the state, kept apart from the view model so the whole flow — creation,
 * then this, then Edit Mode, then a rest — can be walked in a test without a database or a
 * main thread. What the level's answers grant goes through [ChoiceGrants], the same way every
 * other answer does, rather than through a switch of its own.
 */
internal object LevelUpApplier {

    fun apply(state: LevelUpState): PlayerCharacter {
        val character = state.character
        val allChoices = state.featureChoices

        // Keyed by the level in the class that granted it, so a multiclass character's picks
        // stay attached to the right feature.
        val choiceSelections = allChoices.mapNotNull { choice ->
            state.selections[choice.id]
                ?.takeIf { it.isNotEmpty() }
                ?.let { "${state.targetClassLevel}:${choice.id}" to it }
        }.toMap()

        // What the feat taken this level asked for. These are keyed by the feat rather than
        // by the level, because a feat is taken once and its answers belong to it — that also
        // lets the sheet resolve them the same way as a feat gained at character creation.
        val featSelections = state.featChoices.mapNotNull { choice ->
            state.selections[choice.id]?.takeIf { it.isNotEmpty() }?.let { choice.id to it }
        }.toMap()

        val learnedSpells = buildList {
            state.newCantrips.forEach { id ->
                SpellData.byId(id)?.let { add(it.toKnownSpell(state.className())) }
            }
            state.newSpells.forEach { id ->
                SpellData.byId(id)?.let { add(it.toKnownSpell(state.className())) }
            }
            state.manualSpells.forEach { name ->
                add(
                    KnownSpell(
                        id = "manual:${name.lowercase().replace(' ', '_')}",
                        name = name,
                        level = state.maxSpellLevel,
                        school = "",
                        description = "Added manually at level ${state.targetLevel}.",
                        source = state.className(),
                    )
                )
            }
        }

        val abilityImprovements = character.abilityScoreImprovements.toMutableMap()
        state.asiPoints.forEach { (ability, points) ->
            abilityImprovements[ability.name] = (abilityImprovements[ability.name] ?: 0) + points
        }

        // Taking the first level in a new class grants a reduced set of proficiencies —
        // never saving throws, which only the class you started with provides.
        val multiclassEntry = if (state.isNewClass) {
            Multiclassing.proficienciesGained(state.classId)
        } else {
            null
        }

        val withLevel = Multiclassing.withLevelIn(character, state.classId)
        val leveled = withLevel.copy(
            subclassId = if (state.classId == character.classId) {
                state.activeSubclassId
            } else {
                character.subclassId
            },
            hitPointsPerLevel = character.hitPointsPerLevel + state.hitPointsGained,
            toolProficiencies = (
                character.toolProficiencies + multiclassEntry?.toolProficiencies.orEmpty()
                ).distinct(),
            armorTraining = (
                character.armorTraining + multiclassEntry?.armorTraining.orEmpty()
                ).distinct(),
            weaponProficiencies = (
                character.weaponProficiencies + multiclassEntry?.weaponProficiencies.orEmpty()
                ).distinct(),
            abilityScoreImprovements = abilityImprovements,
            featIds = character.featIds + listOfNotNull(state.featId),
            knownSpells = (
                character.knownSpells
                    .filterNot { it.id == state.replacedSpellId || it.id == state.replacedCantripId } +
                    learnedSpells
                ).distinctBy { it.id },
            levelSelections = character.levelSelections + choiceSelections,
            originChoiceSelections = character.originChoiceSelections + featSelections,
        ).let { updated ->
            // Record the subclass against its own class, so each class keeps its own.
            state.activeSubclassId
                ?.takeIf { state.gainsSubclass }
                ?.let { Multiclassing.withSubclass(updated, state.classId, it) }
                ?: updated
        }

        // The skills, expertise, tools, languages and spells the level's answers hand over —
        // a feat's as much as a feature's — applied by the one function that knows how.
        val withAnswers = ChoiceGrants.apply(leveled, allChoices + state.featChoices, state.selections)
        // Two more spells in a Wizard's book is not two more prepared.
        val granted = CharacterSpells.withPreparedWithinLimit(withAnswers)

        // Gaining a level raises max HP; current hit points rise by the same amount so the
        // character isn't suddenly wounded by levelling up.
        val hpGain = CharacterCalculations.maxHitPoints(granted) -
            CharacterCalculations.maxHitPoints(character)
        return granted.copy(
            currentHitPoints = (granted.currentHitPoints + hpGain.coerceAtLeast(0))
                .coerceAtMost(CharacterCalculations.maxHitPoints(granted))
        )
    }

    /** The class the spells belong to, which is the one being levelled, not the first one. */
    private fun LevelUpState.className(): String =
        ClassData.byId(classId)?.name ?: classId.replaceFirstChar { it.uppercase() }

    private fun SpellDef.toKnownSpell(source: String) = KnownSpell(
        id = id,
        name = name,
        level = level,
        school = school,
        description = description,
        source = source,
    )
}
