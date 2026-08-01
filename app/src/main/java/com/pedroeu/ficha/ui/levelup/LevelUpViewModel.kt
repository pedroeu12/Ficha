package com.pedroeu.ficha.ui.levelup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pedroeu.ficha.data.CharacterRepository
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.data.model.SpellDef
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.Multiclassing
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.PlayerCharacter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class LevelUpViewModel(
    private val repository: CharacterRepository,
    private val characterId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<LevelUpState?>(null)
    val state: StateFlow<LevelUpState?> = _state.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getById(characterId)?.let { character ->
                _state.value = LevelUpState(character = character)
            }
        }
    }

    private fun edit(transform: (LevelUpState) -> LevelUpState) {
        _state.update { it?.let(transform) }
    }

    // ------------------------------------------------------------------ Hit points

    fun setHitPointMethod(method: HitPointMethod) = edit { current ->
        current.copy(
            hitPointMethod = method,
            manualHitPoints = if (method == HitPointMethod.MANUAL) {
                current.manualHitPoints.coerceAtLeast(1)
            } else {
                current.manualHitPoints
            },
        )
    }

    fun rollHitPoints() = edit { current ->
        current.copy(rolledHitPoints = Random.nextInt(1, current.hitDie + 1))
    }

    fun setManualHitPoints(value: Int) = edit { current ->
        current.copy(manualHitPoints = value.coerceIn(1, current.hitDie))
    }

    // ------------------------------------------------------------------ Choices

    /**
     * Puts this level into a different class. Everything downstream — hit die, features,
     * subclass timing — reads from it, so the picks made so far are cleared.
     */
    fun selectLevellingClass(classId: String) = edit { current ->
        if (current.classId == classId) {
            current
        } else {
            current.copy(
                levellingClassId = classId,
                subclassId = null,
                selections = emptyMap(),
                newCantrips = emptyList(),
                newSpells = emptyList(),
                manualSpells = emptyList(),
                rolledHitPoints = null,
            )
        }
    }

    fun selectSubclass(id: String) = edit { it.copy(subclassId = id, selections = emptyMap()) }

    fun toggleSelection(choiceId: String, optionId: String, max: Int) = edit { current ->
        val selected = current.selections[choiceId].orEmpty()
        val next = when {
            selected.contains(optionId) -> selected - optionId
            selected.size < max -> selected + optionId
            max == 1 -> listOf(optionId)
            else -> selected.drop(1) + optionId
        }
        current.copy(selections = current.selections + (choiceId to next))
    }

    // ------------------------------------------------------------------ ASI and feats

    fun setAsiMode(mode: AsiMode) = edit {
        it.copy(asiMode = mode, asiPoints = emptyMap(), featId = null)
    }

    /**
     * Adds a point to an ability, respecting the shape of the chosen mode. Tapping an ability
     * that already holds points clears it, so a mistaken pick is easy to undo.
     */
    fun toggleAsiAbility(ability: Ability) = edit { current ->
        val points = current.asiPoints
        if (points.containsKey(ability)) return@edit current.copy(asiPoints = points - ability)

        val next = when (current.asiMode) {
            AsiMode.PLUS_TWO -> mapOf(ability to 2)
            AsiMode.PLUS_ONE_ONE ->
                if (points.size >= 2) points else points + (ability to 1)
            AsiMode.FEAT -> points
        }
        current.copy(asiPoints = next)
    }

    /** Changing the feat drops whatever was picked inside the old one. */
    fun selectFeat(id: String) = edit { current ->
        if (current.featId == id) return@edit current
        val staleChoiceIds = current.featChoices.map { it.id }.toSet()
        current.copy(
            featId = id,
            selections = current.selections.filterKeys { it !in staleChoiceIds },
        )
    }

    // ------------------------------------------------------------------ Spells

    fun toggleCantrip(spellId: String) = edit { current ->
        val selected = current.newCantrips
        val next = when {
            selected.contains(spellId) -> selected - spellId
            selected.size < current.cantripsToLearn -> selected + spellId
            current.cantripsToLearn == 1 -> listOf(spellId)
            else -> selected.drop(1) + spellId
        }
        current.copy(newCantrips = next)
    }

    fun toggleSpell(spellId: String) = edit { current ->
        val selected = current.newSpells
        val limit = (current.spellsToLearn - current.manualSpells.size).coerceAtLeast(0)
        val next = when {
            selected.contains(spellId) -> selected - spellId
            selected.size < limit -> selected + spellId
            limit == 1 -> listOf(spellId)
            limit == 0 -> selected
            else -> selected.drop(1) + spellId
        }
        current.copy(newSpells = next)
    }

    fun addManualSpell(name: String) = edit { current ->
        if (name.isBlank()) current
        else current.copy(manualSpells = current.manualSpells + name.trim())
    }

    fun removeManualSpell(name: String) = edit { current ->
        current.copy(manualSpells = current.manualSpells - name)
    }

    // ------------------------------------------------------------------ Navigation

    fun next() = edit { current ->
        val steps = current.steps
        val index = steps.indexOf(current.step)
        if (!current.canAdvance || index == steps.lastIndex) current
        else current.copy(requestedStep = steps[index + 1])
    }

    /** Returns false when already on the first step, so the caller can leave the flow. */
    fun back(): Boolean {
        val current = _state.value ?: return false
        val steps = current.steps
        val index = steps.indexOf(current.step)
        if (index <= 0) return false
        _state.value = current.copy(requestedStep = steps[index - 1])
        return true
    }

    // ------------------------------------------------------------------ Commit

    fun confirm() {
        val current = _state.value ?: return
        val updated = applyLevelUp(current)
        viewModelScope.launch {
            repository.save(updated)
            _finished.value = true
        }
    }

    private fun applyLevelUp(state: LevelUpState): PlayerCharacter {
        val character = state.character
        val allChoices = state.featureChoices

        // Selections are typed by their choice kind, so each lands in the right place.
        val newSkills = mutableSetOf<String>()
        val newExpertise = mutableSetOf<String>()
        val newTools = mutableListOf<String>()
        val choiceSelections = mutableMapOf<String, List<String>>()

        allChoices.forEach { choice ->
            val picked = state.selections[choice.id].orEmpty()
            if (picked.isEmpty()) return@forEach
            // Keyed by the level in the class that granted it, so a multiclass character's
            // picks stay attached to the right feature.
            choiceSelections["${state.targetClassLevel}:${choice.id}"] = picked

            when (choice.kind) {
                ChoiceKind.SKILL -> newSkills += picked.filter { id ->
                    Skill.ALL.any { it.name == id }
                }
                ChoiceKind.EXPERTISE -> newExpertise += picked.filter { id ->
                    Skill.ALL.any { it.name == id }
                }
                ChoiceKind.TOOL -> newTools += picked
                else -> Unit
            }
        }

        // What the feat taken this level asked for. These are keyed by the feat rather than
        // by the level, because a feat is taken once and its answers belong to it — that also
        // lets the sheet resolve them the same way as a feat gained at character creation.
        val featSelections = mutableMapOf<String, List<String>>()
        state.featChoices.forEach { choice ->
            val picked = state.selections[choice.id].orEmpty()
            if (picked.isEmpty()) return@forEach
            featSelections[choice.id] = picked

            when (choice.kind) {
                ChoiceKind.SKILL -> newSkills += picked.filter { id ->
                    Skill.ALL.any { it.name == id }
                }
                ChoiceKind.EXPERTISE -> newExpertise += picked.filter { id ->
                    Skill.ALL.any { it.name == id }
                }
                ChoiceKind.TOOL -> newTools += picked
                // An ability score increase is derived from this selection rather than
                // written in, so it stays right if the feat list is ever corrected.
                else -> Unit
            }
        }

        // A subclass feature can grant Expertise in a skill the character isn't proficient
        // with yet; taking the proficiency alongside it keeps the sheet consistent.
        newSkills += newExpertise

        val featName = state.featId?.let { FeatData.byId(it)?.name ?: it }.orEmpty()

        val learnedSpells = buildList {
            state.newCantrips.forEach { id ->
                SpellData.byId(id)?.let { add(it.toKnownSpell(state.className())) }
            }
            // A feat that teaches a spell — Fey-Touched, Spell Sniper, Magic Initiate taken
            // in place of an improvement — puts it on the list under the feat's name.
            state.featChoices
                .filter { it.kind == ChoiceKind.SPELL }
                .flatMap { featSelections[it.id].orEmpty() }
                .forEach { id -> SpellData.byId(id)?.let { add(it.toKnownSpell(featName)) } }
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
            skillProficiencies = character.skillProficiencies + newSkills,
            skillExpertise = character.skillExpertise + newExpertise,
            toolProficiencies = (
                character.toolProficiencies + newTools +
                    multiclassEntry?.toolProficiencies.orEmpty()
                ).distinct(),
            armorTraining = (
                character.armorTraining + multiclassEntry?.armorTraining.orEmpty()
                ).distinct(),
            weaponProficiencies = (
                character.weaponProficiencies + multiclassEntry?.weaponProficiencies.orEmpty()
                ).distinct(),
            abilityScoreImprovements = abilityImprovements,
            featIds = character.featIds + listOfNotNull(state.featId),
            knownSpells = (character.knownSpells + learnedSpells).distinctBy { it.id },
            levelSelections = character.levelSelections + choiceSelections,
            originChoiceSelections = character.originChoiceSelections + featSelections,
        ).let { updated ->
            // Record the subclass against its own class, so each class keeps its own.
            state.activeSubclassId
                ?.takeIf { state.gainsSubclass }
                ?.let { Multiclassing.withSubclass(updated, state.classId, it) }
                ?: updated
        }

        // Gaining a level raises max HP; current hit points rise by the same amount so the
        // character isn't suddenly wounded by levelling up.
        val hpGain = CharacterCalculations.maxHitPoints(leveled) -
            CharacterCalculations.maxHitPoints(character)
        return leveled.copy(
            currentHitPoints = (leveled.currentHitPoints + hpGain.coerceAtLeast(0))
                .coerceAtMost(CharacterCalculations.maxHitPoints(leveled))
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

    class Factory(
        private val repository: CharacterRepository,
        private val characterId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LevelUpViewModel(repository, characterId) as T
    }
}
